package code
package service

import java.util.Date

import scala.collection.mutable.ListBuffer
import scala.util.Sorting
import org.joda.time.DateTime
import code.commons.TimeUtils
import code.model.TaskItem
import code.model.User
import code.model.Project
import net.liftweb.common.Box.box2Option
import net.liftweb.common.{Box, Full}
import net.liftweb.mapper.MappedField.mapToType
import net.liftweb.mapper._

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
    getTaskItemsForDay(offset).lastOption
  }

  def alwaysTrue[T <: Mapper[T]]: QueryParam[T] = BySql[T]("1=1", IHaveValidatedThisSQL("suliatis", "2016-11-10"))

  /**
   * Returns a sequence with the task item entries on the given day.
   * The ordering is determined by the item's start time.
   */
  def getTaskItemsForDay(offset: Int, user: Box[User] = User.currentUser): List[TaskItemWithDuration] = {
    /**
     * Takes a list of consecutive TaskItems and converts them to a TaskItemWithDuration.
     * The function calculates the durations of the task items.
     */
    def taskItemsToTaskItemDtos(taskItems: List[TaskItem]): List[TaskItemWithDuration] = { // TODO: call for each user separately
      val taskItemDtos = new ListBuffer[TaskItemWithDuration]

      if (!taskItems.isEmpty) {
        val cappedTaskItems =
          if (taskItems.last.task.get != 0 && TimeUtils.currentDayEndInMs(offset) < TimeUtils.currentTime) {
            taskItems ::: List(TaskItem.create.user(taskItems.last.user.get).start(TimeUtils.currentDayEndInMs(offset) - 1))
          } else {
            taskItems
          }

        var previousTaskStart: Long =
          if (cappedTaskItems.last.task.get == 0) {
            cappedTaskItems.last.start.get
          } else {
            TimeUtils.currentTime
          }

        for (taskItem <- cappedTaskItems.reverse) {
          val duration = previousTaskStart - taskItem.start.get
          previousTaskStart = taskItem.start.get
          taskItemDtos += TaskItemWithDuration(taskItem, duration)
        }
      }

      taskItemDtos.reverse.toList
    }

    // task items for the given day
    var list = taskItemsToTaskItemDtos(
      TaskItem.findAll(OrderBy(TaskItem.start, Ascending),
        user.map(u => By(TaskItem.user, u)).getOrElse(alwaysTrue),
        By_<(TaskItem.start, TimeUtils.currentDayEndInMs(offset)),
        By_>=(TaskItem.start, TimeUtils.currentDayStartInMs(offset))))

    // if there there isn't a task item at the start of the period, we have to check the last task item before the period
    if (!list.exists(_.taskItem.start.get == TimeUtils.currentDayStartInMs(offset))) {
      // last task item before the period
      val lastItem = taskItemsToTaskItemDtos(
        TaskItem.findAll(OrderBy(TaskItem.start, Descending),
          MaxRows(1),
          user.map(u => By(TaskItem.user, u)).getOrElse(alwaysTrue),
          By_<(TaskItem.start, TimeUtils.currentDayStartInMs(offset))))
      // if there is a task item before the period, and it is not Pause, then it will be truncated to the given period, and will count in the result
      if (!lastItem.isEmpty && lastItem.head.taskItem.id != 0) {
        val dto = TaskItemWithDuration(
          user.map(u => TaskItem.create.user(u).task(lastItem.head.taskItem.task.get).start(TimeUtils.currentDayStartInMs(offset)))
              .getOrElse(TaskItem.create.task(lastItem.head.taskItem.task.get).start(TimeUtils.currentDayStartInMs(offset))),
          {
            if (list.isEmpty) (TimeUtils.currentTime - (TimeUtils.currentDayStartInMs(offset) + 1))
            else (list.head.taskItem.start.get - (TimeUtils.currentDayStartInMs(offset) + 1))
          })

        list = List(dto) ::: list
      }
    }

    // remove starting Pause items
    list = list.dropWhile(_.taskItem.task.get == 0)

    if (list.isEmpty) {
      // if the result is empty, then return a list that contains only a Pause item
      List(TaskItemWithDuration(TaskItem.create.user(user).start(TimeUtils.currentDayStartInMs(offset) + 1), 0))
    } else {
      list
    }
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
case class TaskItemWithDuration(taskItem: TaskItem, var duration: Long) {
  val task = TaskService.getTask(taskItem.task.get)
  val project = if (!task.isEmpty) Some(Project.findByKey(task.get.parent.get).get) else None

  val taskName = if (!task.isEmpty) Some(task.get.name.get) else None
  val projectName = if (!project.isEmpty) Some(ProjectService.getDisplayName(project.get)) else None

  val timeString = TimeUtils.format(TimeUtils.TIME_FORMAT, taskItem.start.get)

  def durationInMinutes = (duration / 60D / 1000).toLong
}


