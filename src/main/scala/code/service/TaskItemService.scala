package code
package service

import code.commons.TimeUtils
import code.commons.TimeUtils.dayEndInMs
import code.model.{Task, TaskItem, User}
import code.service.TaskService.path
import com.github.nscala_time.time.Imports._
import net.liftweb.common.Box.box2Option
import net.liftweb.common.{Box, Full}
import net.liftweb.mapper.{By, _}
import org.joda.time.{DateTime, Interval, LocalDate, ReadablePartial}

import scala.collection.mutable.ListBuffer
import scala.language.{postfixOps, reflectiveCalls}

/**
 * TaskItem data handling and conversion service.
 * Reponsible for task item handling.
 *
 * @author David Csakvari
 */
object TaskItemService {

  def alwaysTrue[T <: Mapper[T]]: QueryParam[T] = BySql[T]("1=1", IHaveValidatedThisSQL("suliatis", "2016-11-10"))

  /**
   * Returns a sequence with the task item entries on the given interval.
   * The ordering is determined by the item's start time.
   */
  def getTaskItems(query: IntervalQuery, user: Box[User], removeForgottenLastItem: Boolean = false): List[TaskItemWithDuration] = {
    lazy val allTasks = Task.findAll

    def toTimeline(taskItems: List[TaskItem]): List[TaskItemWithDuration] = {
      val taskItemDtos = new ListBuffer[TaskItemWithDuration]

      if (taskItems.nonEmpty) {
        val trimmedItems =
          taskItems
            .map(item =>
              if (item.start.get < query.interval.startMillis)
                TaskItem.create
                  .user(item.user.get)
                  .task(item.task.get)
                  .start(query.interval.startMillis)
              else
                item)

        val cap =
          if (taskItems.last.task.get != 0 && query.interval.endMillis < TimeUtils.currentTime) {
            List(TaskItem.create
              .user(taskItems.last.user.get)
              .task(0)
              .start(query.interval.endMillis - 1))
          } else {
            List()
          }

        val allTaskItems = split(trimmedItems ::: cap, query.stepInterval)

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
          taskItemDtos += TaskItemWithDuration(taskItem, taskItem.task.obj map (t => path(Nil, t.parent.box, allTasks)) getOrElse Nil, Duration.millis(duration))
        }
      }

      taskItemDtos.reverse.toList.filter(item => item.duration.getMillis != 0L || item.taskItem.task.get == 0)
    }

    val taskItemsForPeriod = TaskItem.findAll(
        OrderBy(TaskItem.start, Ascending),
        user.map(u => By(TaskItem.user, u)).getOrElse(alwaysTrue),
        By_<(TaskItem.start, query.interval.endMillis),
        By_>=(TaskItem.start, query.interval.startMillis),
        PreCache(TaskItem.task)
      )

    val users = user.map(List(_)).getOrElse(User.findAll)

    val lastPartTaskItemBeforePeriodThatMightCount: List[TaskItem] =
      users
        .flatMap(u =>
          TaskItem.findAll(OrderBy(TaskItem.start, Descending),
            MaxRows(1),
            By(TaskItem.user, u),
            By_<(TaskItem.start, query.interval.startMillis))
          .filter(_.task != 0))

    val taskItems = lastPartTaskItemBeforePeriodThatMightCount ::: taskItemsForPeriod

    val list = taskItems.groupBy(_.user.get).flatMap(userItems => {

      /*
       * Check final item in taskItemsForPeriod to see if that is the users last entry to the DB.
       * If so, and it's older than 24 h assume that the user forgot to administer times spent and
       * change the offending item to a pause at the end.
       */
      if (removeForgottenLastItem) {
        val lastEntry = TaskItem.findAll(OrderBy(TaskItem.start, Descending),
          MaxRows(1),
          By(TaskItem.user, userItems._1)).last

        if (userItems._2.last == lastEntry && userItems._2.last.start.get < TimeUtils.currentTime - 86400000) {
          userItems._2.last.task(0)
        }
      }

      toTimeline(userItems._2).dropWhile(_.taskItem.task.get == 0)
    }).toList

    if (list.isEmpty) {
      // if the result is empty, then return a list that contains only a Pause item
      List(TaskItemWithDuration(TaskItem.create.user(user).start(query.interval.startMillis + 1), Nil, 0 millis))
    } else {
      list
    }
  }

  def split(ts: List[TaskItem], i: StepInterval): List[TaskItem] = {
    def nextStep(instant: Long, i: StepInterval): StepInterval =
      if (i.step.contains(instant)) i
      else nextStep(instant, i.next)

    def pause(t: TaskItem, i: StepInterval): TaskItem =
      TaskItem.create.user(t.user.get).task(0).start(i.step.endMillis - 1L)

    def task(t: TaskItem, i: StepInterval): TaskItem =
      TaskItem.create.user(t.user.get).task(t.task.get).start(i.step.endMillis)

    def loop(zs: List[TaskItem], ts: List[TaskItem], i: StepInterval): List[TaskItem] = ts match {
      case t1 :: t2 :: ts =>
        val s = nextStep(t1.start.get, i)
        if (t1.task.box.isEmpty || s.step.contains(new Interval(t1.start.get, t2.start.get))) loop(t1 :: zs, t2 :: ts, i)
        else loop(pause(t1, s) :: t1 :: zs, task(t1, s) :: t2 :: ts, i)
      case t :: Nil =>  (t :: zs).reverse
      case Nil => zs.reverse
    }

    loop(Nil, ts, i)
  }

  case class IntervalQuery(interval: Interval, scale: LocalDate => ReadablePartial) {
    lazy val stepInterval: StepInterval = StepInterval(interval, scale)
  }

  object IntervalQuery {
    def apply(interval: Interval): IntervalQuery = IntervalQuery(interval, identity)

    def between(start: YearMonth, end: YearMonth): IntervalQuery =
      if (start.year == end.year && start.monthOfYear == end.monthOfYear) oneMonth(start)
      else IntervalQuery(start.toInterval.start to end.toInterval.end, d => new YearMonth(d))

    def oneMonth(start: YearMonth): IntervalQuery = IntervalQuery(start.toInterval)

    def thisMonth(): IntervalQuery = IntervalQuery(YearMonth.now().toInterval)
  }

  case class StepInterval(step: Interval, period: Period) {

    def next: StepInterval = StepInterval(new Interval(step.start + period, step.end + period), period)
  }

  object StepInterval {

    def apply(interval: Interval, scale: LocalDate => ReadablePartial): StepInterval = {
      lazy val step: Interval = intervalFrom(scale(new LocalDate(interval.start)))
      lazy val period: Period = step.toPeriod
      new StepInterval(step, period)
    }

    private def intervalFrom(d: ReadablePartial): Interval = d match {
      case d: { def toInterval: Interval } => d.toInterval
    }
  }

  /**
   * Inserts a task item for the current user.
   * If future date is given, it will be converted to the current time.
   * If the new item collides with the user's other item (exact same time is given (to minutes)), the previous one will be deleted.
   *
   * The time value always converted to whole minutes.
   */
  def insertTaskItem(taskId: Long, time: Long, user: Box[User] = User.currentUser): TaskItem = {

    if (taskId != -1L && Task.find(By(Task.id, taskId)).isEmpty) {
      throw new IllegalArgumentException("No task found with ID: " + taskId);
    }

    // calculate insert time
    val insertTime = math.min(time, TimeUtils.currentTime)

    // if the new item collides with the user's other item, the previous one will be deleted
    TaskItem.findAll(By(TaskItem.user, user.openOrThrowException("Current user must be defined!")), By(TaskItem.start, insertTime)).foreach(_.delete_!)

    // create item
    val taskItem = TaskItem.create.task(taskId).user(user.openOrThrowException("Current user must be defined!")).start(insertTime)
    taskItem.save
    taskItem
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
  def editTaskItem(taskItemId: Long, taskId: Long, time: Long, split: Boolean = false, user: Box[User] = User.currentUser): Boolean = {

    if (taskId != -1L && Task.find(By(Task.id, taskId)).isEmpty) {
      throw new IllegalArgumentException("No task found with ID: " + taskId);
    }

    // offset value that represents the given day
    val offset = math.abs(TimeUtils.getOffset(time)) * (-1)

    // calculate upper and lower time bounds
    val minTime = TimeUtils.currentDayStartInMs(offset)
    val maxTime = math.min(TimeUtils.currentDayEndInMs(offset), TimeUtils.currentTime)

    val taskItem = TaskItem.find(By(TaskItem.id, taskItemId), By(TaskItem.user, user.openOrThrowException("Current user must be defined!"))).openOrThrowException("Task item must be defined!")
    val previousTaskItemTime = {
      val item = if (split) {
        Full(taskItem)
      } else {
        previousTaskItem(taskItem, user.openOrThrowException("Current user must be defined!"))
      }

      if (!item.isEmpty) {
        item.openOrThrowException("Task item must be defined!").start.get + (1000L * 60) // one minute padding
      } else {
        minTime
      }
    }
    val nextTaskItemTime = {
      val item = nextTaskItem(taskItem, user.openOrThrowException("Current user must be defined!"))
      if (!item.isEmpty) {
        item.openOrThrowException("Task item must be defined!").start.get - (1000L * 60) // one minute padding
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
      TaskItem.create.task(taskId).user(user.openOrThrowException("Current user must be defined!")).start(newTime).save
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
  def appendTaskItem(taskId: Long, time: Long): Boolean = {
    def getLastTaskItemForDay(offset: Int): Option[TaskItemWithDuration] = {
      getTaskItems(IntervalQuery(TimeUtils.offsetToDailyInterval(offset)), User.currentUser).lastOption
    }
  
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
    TaskItem.findAll(By(TaskItem.user, User.currentUser.openOrThrowException("Current user must be defined!")), By(TaskItem.start, insertTime)).foreach(_.delete_!)
    TaskItem.create.task(taskId).user(User.currentUser.openOrThrowException("Current user must be defined!")).start(insertTime).save
  }

  /**
   * Deletes task item with the given id for current user.
   */
  def deleteTaskItem(id: Long, user: Box[User] = User.currentUser): Boolean = {
    TaskItem.find(By(TaskItem.id, id), By(TaskItem.user, user.openOrThrowException("Current user must be defined!"))).openOrThrowException("Task item must be defined!").delete_!
  }

  /**
   * Normalizes the user's task items for the given day:
   * 	- merges task item A and B (by deleting B), if A has the same task as B, and B directly follows A
   *  - deletes a Pause task item, if there are no non-Pause tasks before that
   */
  def normalizeTaskItems(offset: Int): Unit = {
    // get all task items for the given day
    val taskItemsForDay = TaskItem.findAll(OrderBy(TaskItem.start, Ascending),
      By(TaskItem.user, User.currentUser.openOrThrowException("Current user must be defined!")),
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
      TaskItem.findByKey(taskItemId).openOrThrowException("Task item must be defined!").delete_!
    }
  }

  /**
   * Returns the previous task item relative to the given task item.
   * Note: it can return task item from another day.
   */
  private def previousTaskItem(taskItem: TaskItem, user: User) = {
    TaskItem.find(MaxRows(1),
      OrderBy(TaskItem.start, Descending),
      By(TaskItem.user, user),
      By_<=(TaskItem.start, taskItem.start.get),
      NotBy(TaskItem.id, taskItem.id.get))
  }

  /**
   * Returns the next task item relative to the given task item.
   * Note: it can return task item from another day.
   */
  private def nextTaskItem(taskItem: TaskItem, user: User) = {
    TaskItem.find(MaxRows(1),
      OrderBy(TaskItem.start, Ascending),
      By(TaskItem.user, user),
      By_>=(TaskItem.start, taskItem.start.get),
      NotBy(TaskItem.id, taskItem.id.get))
  }

}

/**
 * TaskItem wrapper/DTO, witch contains the duration value of the given entry as it is usually needed.
 * The duration can be derived from the entry's and the following entry's start time.
 */
case class TaskItemWithDuration(taskItem: TaskItem, path: List[Task], duration: Duration) extends TaskDto[TaskItemWithDuration](taskItem.task.obj, path) {
  lazy val task: Box[Task] = taskItem.task.obj

  lazy val localTime: LocalTime = new DateTime(taskItem.start.get).toLocalTime
}
