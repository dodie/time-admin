package code.util

import code.service.ReportService._
import code.service.TaskSheetItem
import code.util.ListToReducedMap._
import com.github.nscala_time.time.Imports._
import org.joda.time.ReadablePartial

object TaskSheetUtils {

  def duration(ts: TaskSheet, d: ReadablePartial, t: TaskSheetItem): Duration =
    ts.get(d).flatMap(_.get(t)).getOrElse(Duration.millis(0))

  def dates(ts: TaskSheet): List[ReadablePartial] =
    ts.keys.toList.sorted

  def sumByDates(ts: TaskSheet): Map[ReadablePartial, Duration] =
    ts.mapValues(m => m.values.foldLeft(Duration.millis(0))(_ + _))

  def tasks(ts: TaskSheet): List[TaskSheetItem] =
    ts.values.flatMap(_.keySet).toSet.toList.sortBy((t: TaskSheetItem) => t.name)

  def sumByTasks(ts: TaskSheet): Map[TaskSheetItem, Duration] =
    ts.values.flatMap(_.toList).toList.leftReducedMap(Duration.millis(0))(_ + _)

  def sum(ts: TaskSheet): Duration =
    sumByDates(ts).values.foldLeft(Duration.millis(0))(_ + _)
}
