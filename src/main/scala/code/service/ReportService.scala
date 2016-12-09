package code
package service

import java.util.Date
import java.text.Collator
import java.text.DecimalFormat

import scala.collection.mutable.ListBuffer
import net.liftweb.common._
import org.joda.time.{LocalDate, DateTime, Duration, _}
import code.commons.TimeUtils
import net.liftweb.http.S
import code.model.{Project, User}
import code.service.TaskItemService.getTaskItems
import com.github.nscala_time.time.Imports._

import scala.collection.immutable.{IndexedSeq, Seq}
import code.util.ListToFoldedMap._

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
   * and returns data that can be used in timesheets.
   * @return a sequence of (dayOfMonth: String, arriveTime: String, leaveTime: String) tuples. Arrive and leave time strings are in hh:mm format.
   */
  def getTimesheetData(offset: Int): IndexedSeq[(String, String, String, String)] = {
    val formatter = new DecimalFormat("#.#")

    // deduct offtime from leaving time based on user preferences
    val removeOfftimeFromLeaveTime = UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveOfftime).toBoolean
    val additionalLeaveTime = UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime).toLong * 1000 * 60

    // calculate first and last days of the month
    val monthStartOffset = TimeUtils.currentMonthStartInOffset(offset) + offset
    val monthEndOffset = TimeUtils.currentMonthEndInOffset(offset) + offset

    // for all days, get all taskitems and produce data touples
    (for (offset <- monthStartOffset until monthEndOffset + 1) yield {
      val taskItemsForDay = getTaskItems(new LocalDate().minusDays(offset).toInterval)

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

      if (trim(taskItemsForDay).nonEmpty && (offset <= 0)) {
        val last = taskItemsForDay.lastOption
        val first = taskItemsForDay.headOption

        val day = (math.abs(monthStartOffset) - math.abs(offset) + 1).toString

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
          case (Right(arriveTime), Right(leaveTime)) => formatter.format((leaveTime - arriveTime) / (1000D * 60D * 60D))
          case _ => "-"
        }

        (day, transform(arrive), transform(leave), sum)
      } else {
        null
      }
    }).filter(_ != null)
  }

  type TaskSheet[D <: ReadablePartial] = Map[D, Map[TaskSheetItem,Duration]]

  def taskSheetData[D <: ReadablePartial](u: Box[User], i: Interval, f: LocalDate => D): TaskSheet[D] =
    dates(i, f).map(d => (f(d), activeTaskItems(intervalFrom(f(d)), u).map(t => taskSheetItemWithDuration(t))))
      .reducedMap(Nil: List[(TaskSheetItem,Duration)])(_ ::: _)
      .mapValues(_.reducedMap(Duration.ZERO)(_ + _))

  def dates[D <: ReadablePartial](i: Interval, f: LocalDate => D): List[D] =
    {
      if (i.contains(DateTime.now())) 0 to i.withEnd(DateTime.now()).toPeriod(PeriodType.days).getDays
      else 0 until i.toPeriod(PeriodType.days).getDays

    }.map(i.start.toLocalDate.plusDays(_)).toSet.map(f).toList

  def intervalFrom[D <: ReadablePartial](d: D): Interval = d match {
    case d: {def toInterval(): Interval} => d.toInterval
  }

  def activeTaskItems(i: Interval, u: Box[User]): List[TaskItemWithDuration] =
    getTaskItems(i, u) filter (_.project.exists(_.active.get))

  def dateWithTask[D <: ReadablePartial](f: LocalDate => D)(t: TaskItemWithDuration): (D, List[TaskItemWithDuration]) =
    (f(new DateTime(t.taskItem.start.get).toLocalDate), List(t))

  def taskSheetItemWithDuration(t: TaskItemWithDuration): (TaskSheetItem, Duration) =
    (TaskSheetItem(t), new Duration(t.duration))

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
      var dur: Long = 0
      for (taskItemDto <- aggregated._2) {
        dur = dur + taskItemDto.duration
      }

      val task = TaskService.getTask(aggregated._2.head.taskItem.task.get)
      val taskName: String = task match {
        case Full(t) => t.name.get
        case _ => S.?("task.pause")
      }

      val project = task match {
        case Full(t) => Project.findByKey(t.parent.get)
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

      aggregatedDatas.append(AggregatedTaskItemData(aggregated._1, rootProjectId, dur, projectName, taskName, task.isEmpty))
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
  def collator = Collator.getInstance(S.locale);
  def compare(that: AggregatedTaskItemData) = collator.compare(projectName + taskName, that.projectName + that.taskName)
  def durationInMinutes = (duration / 60D / 1000).toLong
}
