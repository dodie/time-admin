package code
package service

import java.util.Date

import code.commons.TimeUtils
import code.model.{Task, User}
import code.commons.TimeUtils.offsetToDailyInterval
import code.service.TaskItemService.{IntervalQuery, getTaskItems}
import code.service.UserPreferenceNames.{timesheetLeaveAdditionalTime, timesheetLeaveOfftime}
import code.service.UserPreferenceService.getUserPreference
import code.util.ListToReducedMap._
import com.github.nscala_time.time.Imports._
import net.liftweb.common._
import net.liftweb.http.S
import net.liftweb.mapper.By
import org.joda.time.{DateTime, Duration, Interval, LocalDate, _}

import scala.collection.immutable.Seq
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
    val removeOfftimeFromLeaveTime = getUserPreference(timesheetLeaveOfftime).toBoolean
    val additionalLeaveTime = getUserPreference(timesheetLeaveAdditionalTime).toLong * 1000 * 60

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
  def getTimesheetData(i: IntervalQuery): List[(String,String,String,Double)] = {
    (for {
      (d, ts) <- getTaskItems(i).sortBy(_.taskItem.start.get).groupBy(t => new LocalDate(t.taskItem.start.get))
      if trim(ts).nonEmpty
    } yield {
      val breaks = calculateTimeRemovalFromLeaveTime(ts.find(_.task.isEmpty).map(_.duration).foldLeft(Duration.ZERO)(_ + _).getMillis)

      val last = ts.lastOption
      val first = ts.headOption

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
          val date = new Date(last.get.taskItem.start.get - breaks)
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

      (d.getDayOfMonth.toString, transform(arrive), transform(leave), sum)
    }).toList.sortBy(_._1)
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
   * Removes the Pause tasks from the begining and the end of the sequence.
   */
  def trim(in: Seq[TaskItemWithDuration]): Seq[TaskItemWithDuration] = {
    in.dropWhile(_.taskItem.task.get == 0).reverse.dropWhile(_.taskItem.task.get == 0).reverse
  }
}
