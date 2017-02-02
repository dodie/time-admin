package code
package service

import java.text.Collator
import java.util.Date

import code.commons.TimeUtils
import code.model.{Task, User}
import code.service.TaskItemService.{IntervalQuery, getTaskItems}
import code.util.ListToReducedMap._
import com.github.nscala_time.time.Imports._
import net.liftweb.common._
import net.liftweb.http.S
import org.joda.time.{DateTime, Duration, Interval, _}
import net.liftweb.mapper.By

import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

/**
 * Reponsible for creating report data.
 *
 * @author David Csakvari
 */
object ReportService {

  /**
   * Calculates the number of milliseconds to be subtracted from leave time for a day,
   * based on user preferences and the given offtime.
   */
  def calculateTimeRemovalFromLeaveTime(offtime: Long): Long = {
    val removeOfftimeFromLeaveTime = UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveOfftime).toBoolean
    val additionalLeaveTime = UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime).toLong * 1000 * 60

    (if (removeOfftimeFromLeaveTime) {
      offtime
    } else {
      0L
    }) - additionalLeaveTime
  }

  /**
   * Processes the TaskItems in the given month defined by the offset (in days) from the current day,
   * and returns data that can be used in time sheets.
   * @return a sequence of (dayOfMonth: String, arriveTime: String, leaveTime: String) tuples. Arrive and leave time strings are in hh:mm format.
   */
  def getTimesheetData(offset: Int): List[(String,String,String,Double)] = {

    // calculate first and last days of the month
    val monthStartOffset = TimeUtils.currentMonthStartInOffset(offset) + offset
    val monthEndOffset = TimeUtils.currentMonthEndInOffset(offset) + offset

    // for all days, get all task items and produce data tuples
    (for (currentOffset <- monthStartOffset until monthEndOffset + 1) yield {
      val taskItemsForDay = getTaskItems(IntervalQuery(TimeUtils.offsetToDailyInterval(currentOffset)))

      val offtimeToRemoveFromLeaveTime = {
        val aggregatedArray = createAggregatedDatas(taskItemsForDay)
        val pause = aggregatedArray.find(_.taskId == 0)
        val pauseTime = if (pause.isEmpty) {
          0L
        } else {
          pause.get.duration
        }
        calculateTimeRemovalFromLeaveTime(pauseTime)
      }

      if (trim(taskItemsForDay).nonEmpty && (currentOffset <= 0)) {
        val last = taskItemsForDay.lastOption
        val first = taskItemsForDay.headOption

        val day = (math.abs(monthStartOffset) - math.abs(currentOffset) + 1).toString

        val arrive = if (first.isEmpty) {
          Left("-")
        } else {
          val date = new Date(first.get.taskItem.start.get)
          Right(date.getTime)
        }

        val leave = if (last.isEmpty) {
          Left("-")
        } else {
          if (last.get.taskItem.task.get == 0) {
            val date = new Date(last.get.taskItem.start.get - offtimeToRemoveFromLeaveTime)
            Right(date.getTime)
          } else {
            Left("...")
          }
        }

        def transform(e: Either[String, Long]) = e match {
          case Right(time) => TimeUtils.format(TimeUtils.TIME_FORMAT, time)
          case Left(err) => err
        }

        val sum = (arrive, leave) match {
          case (Right(arriveTime), Right(leaveTime)) => (leaveTime - arriveTime) / (1000D * 60D * 60D)
          case _ => 0.0d
        }

        (day, transform(arrive), transform(leave), sum)
      } else {
        null
      }
    }).filter(_ != null).toList
  }

  type TaskSheet = Map[ReadablePartial, Map[TaskSheetItem,Duration]]

  def taskSheetData(i: IntervalQuery, u: Box[User]): TaskSheet = {
    val ps = Task.findAll(By(Task.selectable, false))

    val ds = dates(i.interval, i.scale).map(d => d -> (Nil: List[TaskItemWithDuration])).toMap

    (ds ++ taskItemsExceptPause(i, u).groupBy(t => i.scale(new LocalDate(t.taskItem.start.get))))
      .mapValues(_.map(taskSheetItemWithDuration(_, ps)).leftReducedMap(Duration.ZERO)(_ + _))
  }

  def dates(i: Interval, f: LocalDate => ReadablePartial): List[ReadablePartial] = days(i).map(f).distinct

  def days(i: Interval): List[LocalDate] =
    {
      if (i.contains(DateTime.now())) 0 to i.withEnd(DateTime.now()).toPeriod(PeriodType.days).getDays
      else 0 until i.toPeriod(PeriodType.days).getDays

    } map (i.start.toLocalDate.plusDays(_)) toList

  def taskItemsExceptPause(i: IntervalQuery, u: Box[User]): List[TaskItemWithDuration] =
    getTaskItems(i, u) filter (_.taskName != "")

  def taskSheetItemWithDuration(t: TaskItemWithDuration, ps: List[Task]): (TaskSheetItem, Duration) =
    (TaskSheetItem(t.task map (_.id.get) getOrElse 0L, t.fullName), new Duration(t.duration))

  /**
   * Aggregates the given TaskItem DTOs.
   * Every task in the input set represented by exactly one AggregatedTaskItemData
   * witch duration is the sum of the corresponding TaskItemWithDuration durations.
   * @param taskItemsToGroup input TaskItemWithDuration sequence
   * @return array of AggregatedTaskItemData
   */
  def createAggregatedDatas(taskItemsToGroup: Seq[TaskItemWithDuration]): Array[AggregatedTaskItemData] = {
    val aggregatedDatas = new ListBuffer[AggregatedTaskItemData]

    for (aggregated <- trim(taskItemsToGroup).groupBy(_.taskItem.task.get)) {
      val duration = aggregated._2.foldLeft(Duration.ZERO)(_ + _.duration)

      val task = TaskService.getTask(aggregated._2.head.taskItem.task.get)
      val taskName: String = task match {
        case Full(t) => t.name.get
        case _ => S.?("task.pause")
      }

      val project = task match {
        case Full(t) => Task.findByKey(t.parent.get)
        case _ => Empty
      }

      val projectName: String = project match {
        case Full(p) => ProjectService.getDisplayName(p)
        case _ => ""
      }

      val rootProjectId: Long = project match {
        case Full(p) => ProjectService.getRootProject(p).id.get
        case _ => -1
      }

      aggregatedDatas.append(AggregatedTaskItemData(aggregated._1, rootProjectId, duration.getMillis, projectName, taskName, task.isEmpty))
    }

    aggregatedDatas.toList.sortBy((t: AggregatedTaskItemData) => t.projectName + t.taskName).toArray
    //Sorting.quickSort(data)
    //data.sorted
  }

  /**
   * Removes the Pause tasks from the begining and the end of the sequence.
   */
  def trim(in: Seq[TaskItemWithDuration]): Seq[TaskItemWithDuration] = {
    in.dropWhile(_.taskItem.task.get == 0).reverse.dropWhile(_.taskItem.task.get == 0).reverse
  }
}

/**
 * TaskItem DTO that represents aggregated task items of a task for a given period.
 */
case class AggregatedTaskItemData(taskId: Long, rootProjectId: Long, duration: Long, projectName: String, taskName: String, isPause: Boolean = false) extends Ordered[AggregatedTaskItemData] {
  def collator: Collator = Collator.getInstance(S.locale)
  def compare(that: AggregatedTaskItemData): Int = collator.compare(projectName + taskName, that.projectName + that.taskName)
  def durationInMinutes: Long = (duration / 60D / 1000).toLong
}
