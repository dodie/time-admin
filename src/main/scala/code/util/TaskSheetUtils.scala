package code.util

import code.service.ReportService._
import code.service.TaskSheetItem
import com.github.nscala_time.time.Imports._
import code.util.ListToFoldedMap._
import org.joda.time.{DateTimeConstants, DateTimeFieldType, ReadablePartial}

import scala.util.Try

object TaskSheetUtils {

  def duration[D <: ReadablePartial](ts: TaskSheet[D], d: D, t: TaskSheetItem): Duration =
    ts.get(d).flatMap(_.get(t)).getOrElse(Duration.millis(0))

  def dates[D <: ReadablePartial](ts: TaskSheet[D]): List[D] =
    ts.keys.toList.sorted

  def sumByDates[D <: ReadablePartial](ts: TaskSheet[D]): Map[D, Duration] =
    ts.mapValues(m => m.values.foldLeft(Duration.millis(0))(_ + _))

  def tasks[D <: ReadablePartial](ts: TaskSheet[D]): List[TaskSheetItem] =
    ts.values.flatMap(_.keySet).toSet.toList.sortBy((t: TaskSheetItem) => t.name)

  def sumByTasks[D <: ReadablePartial](ts: TaskSheet[D]): Map[TaskSheetItem, Duration] =
    ts.values.flatMap(_.toList).toList.foldedMap(Duration.millis(0))(_ + _)

  def sum[D <: ReadablePartial](ts: TaskSheet[D]): Duration =
    sumByDates(ts).values.foldLeft(Duration.millis(0))(_ + _)

  def mapToDateTime[D <: ReadablePartial](i: Interval, d: D): Option[DateTime] =
    List(d.toDateTime(i.start), d.toDateTime(i.end)).find(i.contains(_))

  def isWeekend(d: DateTime): Boolean =
    d.getDayOfWeek == DateTimeConstants.SATURDAY || d.getDayOfWeek == DateTimeConstants.SUNDAY

  def hasDayFieldType[RD <: ReadablePartial](d: RD): Boolean =
    d.isSupported(DateTimeFieldType.dayOfWeek()) || d.isSupported(DateTimeFieldType.dayOfMonth()) || d.isSupported(DateTimeFieldType.dayOfYear())

  def dayOf[RD <: ReadablePartial](d: RD): Try[Int] =
    Try(d.get(DateTimeFieldType.dayOfMonth()))
      .orElse(Try(d.get(DateTimeFieldType.dayOfYear())))
      .orElse(Try(d.get(DateTimeFieldType.dayOfWeek())))

  def title(interval: Interval, scale: LocalDate => ReadablePartial): String = {
    val now = LocalDate.now()
    if (scale(now) == now) s"${interval.start.getYear}.${interval.start.getMonthOfYear}"
    else f"${interval.start.getYear}.${interval.start.getMonthOfYear}%02d - ${interval.end.minusDays(1).getYear}.${interval.end.minusDays(1).getMonthOfYear}%02d"
  }
}
