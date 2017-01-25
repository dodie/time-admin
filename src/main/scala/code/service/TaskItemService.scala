package code
package service

import code.commons.TimeUtils
import code.commons.TimeUtils.{dayEndInMs, intervalFrom}
import code.model.mixin.HierarchicalItem
import code.model.{Project, Task, TaskItem, User}
import code.service.HierarchicalItemService.path
import com.github.nscala_time.time.Imports._
import net.liftweb.common.Box.box2Option
import net.liftweb.common.{Box, Full}
import net.liftweb.mapper.{By, _}
import org.joda.time.{DateTime, Interval, ReadablePartial}

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

/**
 * TaskItem data handling and conversion service.
 * Reponsible for task item handling.
 *
 * @author David Csakvari
 */
object TaskItemService {

  /**
   * Returns the last option for the given day.
   */
  def getLastTaskItemForDay(offset: Int) = {
    getTaskItems(TimeUtils.offsetToDailyInterval(offset), identity).lastOption
  }

  def alwaysTrue[T <: Mapper[T]]: QueryParam[T] = BySql[T]("1=1", IHaveValidatedThisSQL("suliatis", "2016-11-10"))

  /**
   * Returns a sequence with the task item entries on the given day.
   * The ordering is determined by the item's start time.
   */
  def getTaskItems[D <: ReadablePartial](interval: Interval, scale: LocalDate => D, user: Box[User] = User.currentUser): List[TaskItemWithDuration] = {
    lazy val projects = Project.findAll

    def toTimeline(taskItems: List[TaskItem]): List[TaskItemWithDuration] = {
      val taskItemDtos = new ListBuffer[TaskItemWithDuration]

      if (taskItems.nonEmpty) {
        val trimmedItems =
          taskItems
            .map(item =>
              if (item.start.get < interval.startMillis)
                TaskItem.create
                  .user(item.user.get)
                  .task(item.task.get)
                  .start(interval.startMillis)
              else
                item)

        val cap =
          if (taskItems.last.task.get != 0 && interval.endMillis < TimeUtils.currentTime) {
            List(TaskItem.create
              .user(taskItems.last.user.get)
              .task(0)
              .start(interval.endMillis - 1))
          } else {
            List()
          }

        val allTaskItems = splitTasksInInterval(trimmedItems ::: cap, interval, scale)

        var previousTaskStart: Long =
          if (allTaskItems.last.task.get == 0) {
            allTaskItems.last.start.get
          } else {
            TimeUtils.currentTime
          }

        for (taskItem <- allTaskItems.reverse) {
          val start = taskItem.start.get
          //compensate lost millisecond at the end of the day
          val duration = (if (previousTaskStart == dayEndInMs(previousTaskStart)) previousTaskStart + 1 else previousTaskStart) - start
          previousTaskStart = start
          taskItemDtos += TaskItemWithDuration(taskItem, Duration.millis(duration), taskItem.task.obj map (t => path(List(t), t.parent.box, projects)) getOrElse Nil)
        }
      }

      taskItemDtos.reverse.toList.filter(item => item.duration.getMillis != 0L || item.taskItem.task.get == 0)
    }

    val taskItemsForPeriod = TaskItem.findAll(
        OrderBy(TaskItem.start, Ascending),
        user.map(u => By(TaskItem.user, u)).getOrElse(alwaysTrue),
        By_<(TaskItem.start, interval.endMillis),
        By_>=(TaskItem.start, interval.startMillis),
        PreCache(TaskItem.task)
      )

    val users = user.map(List(_)).getOrElse(User.findAll)

    val lastPartTaskItemBeforePeriodThatMightCount: List[TaskItem] =
      users
        .flatMap(u =>
          TaskItem.findAll(OrderBy(TaskItem.start, Descending),
            MaxRows(1),
            By(TaskItem.user, u),
            By_<(TaskItem.start, interval.startMillis))
          .filter(_.task != 0))

    val taskItems = lastPartTaskItemBeforePeriodThatMightCount ::: taskItemsForPeriod

    val list = taskItems.groupBy(_.user.get).flatMap(userItems => toTimeline(userItems._2).dropWhile(_.taskItem.task.get == 0)).toList

    if (list.isEmpty) {
      // if the result is empty, then return a list that contains only a Pause item
      List(TaskItemWithDuration(TaskItem.create.user(user).start(interval.startMillis + 1), 0 millis, Nil))
    } else {
      list
    }
  }

  def splitTasksInInterval[D <: ReadablePartial](ts: List[TaskItem], i: Interval, f: LocalDate => D): List[TaskItem] = {
    def pause(t: TaskItem, step: Period): TaskItem =
      TaskItem.create.user(t.user.get).task(0).start(nextStart(t, step) - 1)

    def nextItem(t: TaskItem, step: Period): TaskItem =
      TaskItem.create.user(t.user.get).task(t.task.get).start(nextStart(t, step))

    def nextStart(t: TaskItem, step: Period): Long = {
      val interval = intervalFrom(f(new LocalDate(t.start.get)))
      if (interval.startMillis < t.start.get) t.start.get + new Interval(t.start, interval.endMillis).toDurationMillis
      else new DateTime(t.start.get).plus(step).getMillis
    }

    def loop(zs: List[TaskItem], ts: List[TaskItem], step: Period): List[TaskItem] = ts match {
      case h1 :: h2 :: t =>
        if (new Interval(h1.start.get, h2.start.get).toDuration.getMillis > step.toDurationFrom(new DateTime(h1.start.get)).getMillis)
          loop(pause(h1, step):: h1 :: zs, nextItem(h1, step) :: h2 :: t, step)
        else loop(h1 :: zs, h2 :: t, step)
      case h :: Nil => h :: zs
    }

    loop(Nil, ts, intervalFrom(f(i.start.toLocalDate)).toPeriod).reverse
  }

  /**
   * Inserts a task item for the current user.
   * If future date is given, it will be converted to the current time.
   * If the new item collides with the user's other item (exact same time is given (to minutes)), the previous one will be deleted.
   *
   * The time value always converted to whole minutes.
   */
  def insertTaskItem(taskId: Long, time: Long) = {
    // calculate insert time
    val insertTime = math.min(time, TimeUtils.currentTime)

    // if the new item collides with the user's other item, the previous one will be deleted
    TaskItem.findAll(By(TaskItem.user, User.currentUser.get), By(TaskItem.start, insertTime)).foreach(_.delete_!)

    // create item
    TaskItem.create.task(taskId).user(User.currentUser.get).start(insertTime).save
  }

  /**
   * Updates or splits the given task item for the current user.
   *
   * Calculates valid time based on
   * the given day start, end, current time
   * and the previous and next task item.
   *
   * If the time is earlier than the day start or the previous task item, it will be converted to be greater than those.
   * If the time is later than the day end (or the current time, if offset is 0) or the next task item, it will be converted to be less than those.
   * The previous and next task item's start time calculated with one minute padding, the modified or splitted task item can't replace any an old one.
   *
   * The time value always converted to whole minutes.
   */
  def editTaskItem(taskItemId: Long, taskId: Long, time: Long, split: Boolean = false) = {
    // offset value that represents the given day
    val offset = math.abs(TimeUtils.getOffset(time)) * (-1)

    // calculate upper and lower time bounds
    val minTime = TimeUtils.currentDayStartInMs(offset)
    val maxTime = math.min(TimeUtils.currentDayEndInMs(offset), TimeUtils.currentTime)

    val taskItem = TaskItem.find(By(TaskItem.id, taskItemId), By(TaskItem.user, User.currentUser.get)).get
    val previousTaskItemTime = {
      val item = if (split) {
        Full(taskItem)
      } else {
        previousTaskItem(taskItem)
      }

      if (!item.isEmpty) {
        item.get.start.get + (1000L * 60) // one minute padding
      } else {
        minTime
      }
    }
    val nextTaskItemTime = {
      val item = nextTaskItem(taskItem)
      if (!item.isEmpty) {
        item.get.start.get - (1000L * 60) // one minute padding
      } else {
        maxTime
      }
    }

    val lowerBound = math.max(minTime, previousTaskItemTime)
    val upperBound = math.min(maxTime, nextTaskItemTime)

    // calculate valid time based on the bounds
    val newTime = TimeUtils.chopToMinute({
      if (time < lowerBound) {
        lowerBound
      } else if (upperBound < time) {
        upperBound
      } else {
        time
      }
    })

    // update or split current task, based on mode
    if (split) {
      TaskItem.create.task(taskId).user(User.currentUser.get).start(newTime).save
    } else {
      taskItem.start(newTime).task(taskId).save
    }
  }

  /**
   * Appends task item for current user for the current day.
   *
   * Calculate valid item time based on
   * the time parameter (desired insertion time)
   * the day start time
   * the current time
   * the previous task item start time.
   *
   * A task item can be appended to today only, older time values will be converted to midnight.
   * A task item can't be appended to the future. Future times will be converted to the current time.
   * A task item can't be appended before the previous task item,
   * older values will be converted to the exact time of the previous task item, and the previous item will be removed.
   *
   * The time value always converted to whole minutes.
   */
  def appendTaskItem(taskId: Long, time: Long) = {
    val now = TimeUtils.currentTime
    val dayStart = TimeUtils.currentDayStartInMs(0)
    val actualTaskItemStart = {
      val actualTaskItem = getLastTaskItemForDay(0)
      if (actualTaskItem.isEmpty) {
        dayStart
      } else {
        actualTaskItem.get.taskItem.start.get
      }
    }

    // calculated insert time
    val insertTime = TimeUtils.chopToMinute(
      math.max(
        math.max(
          math.min(
            time,
            now),
          dayStart),
        actualTaskItemStart))

    // if the new item collides with the user's other item, the previous one will be deleted
    TaskItem.findAll(By(TaskItem.user, User.currentUser.get), By(TaskItem.start, insertTime)).foreach(_.delete_!)
    TaskItem.create.task(taskId).user(User.currentUser.get).start(insertTime).save
  }

  /**
   * Deletes task item with the given id for current user.
   */
  def deleteTaskItem(id: Long) = {
    TaskItem.find(By(TaskItem.id, id), By(TaskItem.user, User.currentUser.get)).get.delete_!
  }

  /**
   * Normalizes the user's task items for the given day:
   * 	- merges task item A and B (by deleting B), if A has the same task as B, and B directly follows A
   *  - deletes a Pause task item, if there are no non-Pause tasks before that
   */
  def normalizeTaskItems(offset: Int) = {
    // get all task items for the given day
    val taskItemsForDay = TaskItem.findAll(OrderBy(TaskItem.start, Ascending),
      By(TaskItem.user, User.currentUser.get),
      By_<(TaskItem.start, TimeUtils.currentDayEndInMs(offset)),
      By_>(TaskItem.start, TimeUtils.currentDayStartInMs(offset)))

    // mark task items to delete
    val taskItemsToDelete = new ListBuffer[Long]
    var prevTask: TaskItem = null
    for (taskItem <- taskItemsForDay) {

      if (prevTask != null && prevTask.task.get == taskItem.task.get) { /* found two task item that follow one another */

        taskItemsToDelete.append(taskItem.id.get)
      } else {
        prevTask = taskItem
      }
    }

    // delete task items
    for (taskItemId <- taskItemsToDelete) {
      TaskItem.findByKey(taskItemId).get.delete_!
    }
  }

  /**
   * Returns the previous task item relative to the given task item.
   * Note: it can return task item from another day.
   */
  private def previousTaskItem(taskItem: TaskItem) = {
    TaskItem.find(MaxRows(1),
      OrderBy(TaskItem.start, Descending),
      By(TaskItem.user, User.currentUser.get),
      By_<=(TaskItem.start, taskItem.start.get),
      NotBy(TaskItem.id, taskItem.id.get))
  }

  /**
   * Returns the next task item relative to the given task item.
   * Note: it can return task item from another day.
   */
  private def nextTaskItem(taskItem: TaskItem) = {
    TaskItem.find(MaxRows(1),
      OrderBy(TaskItem.start, Ascending),
      By(TaskItem.user, User.currentUser.get),
      By_>=(TaskItem.start, taskItem.start.get),
      NotBy(TaskItem.id, taskItem.id.get))
  }

}

/**
 * TaskItem wrapper/DTO, witch contains the duration value of the given entry as it is usually needed.
 * The duration can be derived from the entry's and the following entry's start time.
 */
case class TaskItemWithDuration(taskItem: TaskItem, duration: Duration, path: List[HierarchicalItem[_]]) {
  lazy val task: Box[Task] = taskItem.task.obj

  lazy val taskName: String = task map (_.name.get) getOrElse ""
  lazy val projectName: String = init(path) map (_.name.get) mkString "-"
  lazy val fullName: String = path map (_.name) mkString "-"

  lazy val color: Color = Color.get(
    task map (_.name.get) getOrElse "",
    init(path) map (_.name.get) mkString "-",
    task.exists(_.active.get)
  )

  lazy val baseColor: Color = path.headOption map (_.color.get) flatMap {
    s => Option(s) filter (c => c.nonEmpty && c.length == 7)
  } map Color.parse getOrElse Color.transparent

  lazy val localTime: LocalTime = new DateTime(taskItem.start.get).toLocalTime

  private def init[A](as: List[A]): List[A] = as match {
    case Nil => Nil
    case as => as.init
  }
}