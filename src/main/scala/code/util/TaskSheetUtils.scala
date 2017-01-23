package code.util

import code.service.ReportService._
import code.service.TaskSheetItem
import code.util.ListToReducedMap._
import com.github.nscala_time.time.Imports._
import org.joda.time.ReadablePartial

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
    ts.values.flatMap(_.toList).toList.leftReducedMap(Duration.millis(0))(_ + _)

  def sum[D <: ReadablePartial](ts: TaskSheet[D]): Duration =
    sumByDates(ts).values.foldLeft(Duration.millis(0))(_ + _)

  def title(interval: Interval, scale: LocalDate => ReadablePartial): String = {
    val now = LocalDate.now()
    if (scale(now) == now) f"${interval.start.getYear}.${interval.start.getMonthOfYear}%02d"
    else f"${interval.start.getYear}.${interval.start.getMonthOfYear}%02d - ${interval.end.minusDays(1).getYear}.${interval.end.minusDays(1).getMonthOfYear}%02d"
  }
}
