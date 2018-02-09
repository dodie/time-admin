package code
package service

import code.commons.TimeUtils
import code.model.{Task, User}
import code.service.TaskItemService.{IntervalQuery, getTaskItems}
import code.util.ListToReducedMap._
import com.github.nscala_time.time.Imports._
import net.liftweb.common._
import net.liftweb.mapper.By
import org.joda.time.{DateTime, Duration, Interval, LocalDate, _}

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
  def calculateTimeRemovalFromLeaveTime(offtime: Long): Long =
    User.currentUser filter (_.subtractBreaks.get) map (_ => offtime) getOrElse 0L

  /**
   * Processes the TaskItems in the given month defined by the offset (in days) from the current day,
   * and returns data that can be used in time sheets.
   * @return a sequence of (dayOfMonth: String, arriveTime: String, leaveTime: String) tuples. Arrive and leave time strings are in hh:mm format.
   */
  def getTimesheetData(i: IntervalQuery): List[(Int,String,String,Double)] = {
    (for {
      (d, ts) <- getTaskItems(i) groupBy startDate mapValues (_ sortBy startDate)
      if trim(ts).nonEmpty
    } yield {
      val breaks = calculateTimeRemovalFromLeaveTime {
        trim(ts) filter (_.task.isEmpty) map (_.duration.getMillis) sum
      }

      val first = ts.headOption
      val last = ts.lastOption

      val arrive = if (first.isEmpty) {
        Left("-")
      } else {
        Right(first.get.taskItem.start.get)
      }

      val leave = if (last.isEmpty) {
        Left("-")
      } else {
        if (last.get.taskItem.task.get == 0) {
          Right(last.get.taskItem.start.get - breaks)
        } else {
          Left("...")
        }
      }

      def transform(e: Either[String, Long]) = e match {
        case Right(time) => new LocalTime(time).toString(TimeUtils.TIME_FORMAT)
        case Left(err) => err
      }

      val sum = (arrive, leave) match {
        case (Right(arriveTime), Right(leaveTime)) => (leaveTime - arriveTime) / (1000.0d * 60.0d * 60.0d)
        case _ => 0.0d
      }

      (d.getDayOfMonth, transform(arrive), transform(leave), sum)
    }).toList.sortBy(_._1)
  }

  def startDate(t: TaskItemWithDuration): LocalDate = new LocalDate(t.taskItem.start.get)

  type TaskSheet = Map[ReadablePartial, Map[TaskSheetItem,Duration]]

  def taskSheetData(i: IntervalQuery, u: Box[User], taskFilter: String = ""): TaskSheet = {
    val ds = dates(i.interval, i.scale).map(d => d -> (Nil: List[TaskItemWithDuration])).toMap

    (ds ++ taskItemsExceptPause(i, u, taskFilter).groupBy(t => i.scale(new LocalDate(t.taskItem.start.get))))
      .mapValues(_.map(taskSheetItemWithDuration(_)).leftReducedMap(Duration.ZERO)(_ + _))
  }

  def getCollaborators(user: User) = {
    val interval = IntervalQuery(new Interval(IntervalQuery.thisMonth().interval.start.minusMonths(3), IntervalQuery.thisMonth().interval.end), d => new YearMonth(d))
    val collaborators = for (otherUser <- User.findAll if user != otherUser) yield (otherUser, collaboration(interval, user, otherUser))
    collaborators.sortWith((item1, item2) => {
      item1._2.toList.map(item => item._2.plus(item._3).getMillis).sum > item2._2.toList.map(item => item._2.plus(item._3).getMillis).sum
    })
  }

  def collaboration(i: IntervalQuery, u1: User, u2: User) = {
    val tasksheet1 = taskSheetData(i, Full(u1))
    val tasksheet2 = taskSheetData(i, Full(u2))

    val commonTasks = for (
        (interval1, items1) <- tasksheet1;
        (interval2, items2) <- tasksheet2;
        i1 <- items1;
        i2 <- items2

        if interval1 == interval2 && i1._1.name == i2._1.name
    ) yield (i1._1, i1._2, i2._2)

    commonTasks
      .groupBy(_._1)
      .mapValues(i => i.reduce((acc, i2) => (i2._1, i2._2.plus(acc._2), i2._3.plus(acc._3))))
      .values
      .toList
      .sortWith((item1, item2) => (item1._2.plus(item1._3)).compareTo((item2._2.plus(item2._3))) > 0)
  }

  def dates(i: Interval, f: LocalDate => ReadablePartial): List[ReadablePartial] = days(i).map(f).distinct

  def days(i: Interval): List[LocalDate] =
    {
      if (i.contains(DateTime.now())) 0 to i.withEnd(DateTime.now()).toPeriod(PeriodType.days).getDays
      else 0 until i.toPeriod(PeriodType.days).getDays

    } map (i.start.toLocalDate.plusDays(_)) toList

  def taskItemsExceptPause(i: IntervalQuery, u: Box[User], taskFilter: String): List[TaskItemWithDuration] =
    getTaskItems(i, u) filter (t => (if (taskFilter == "") t.taskName != "" else t.taskName != "" && t.fullName.contains(taskFilter)))

  def taskSheetItemWithDuration(t: TaskItemWithDuration): (TaskSheetItem, Duration) =
    (TaskSheetItem(t.task map (_.id.get) getOrElse 0L, t.fullName), new Duration(t.duration))

  /**
   * Removes the Pause tasks from the begining and the end of the sequence.
   */
  def trim(in: List[TaskItemWithDuration]): List[TaskItemWithDuration] = {
    in.dropWhile(_.taskItem.task.get == 0).reverse.dropWhile(_.taskItem.task.get == 0).reverse
  }
}
