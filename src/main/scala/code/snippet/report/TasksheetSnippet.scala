package code.snippet

import scala.xml.NodeSeq
import code.model.User
import code.service.{ReportService, TaskSheetItem}
import code.snippet.mixin.DateFunctions
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import net.liftweb.http.S
import com.github.nscala_time.time.Imports._
import code.util.ListToFoldedMap._
import net.liftweb.util.CssSel
import org.joda.time.{DateTimeConstants, DateTimeFieldType, ReadablePartial}

/**
 * Tasksheet displaying component.
 * @author David Csakvari
 */
class TasksheetSnippet extends DateFunctions {

  /**
   * Blank tasksheet download link.
   */
  def blankTasksheetExportLink(in: NodeSeq): NodeSeq = {
    (
      "a [href]" #> ("/export/tasksheet/blank/" + offsetInDays)
    ).apply(in)
  }

  /**
   * Tasksheet download link.
   */
  def tasksheetExportLink(in: NodeSeq): NodeSeq = {
    (
      "a [href]" #> ("/export/tasksheet/" + offsetInDays)
    ).apply(in)
  }

  /**
   * Taskshet display.
   */
  def tasksheet(in: NodeSeq): NodeSeq = {
    val interval = new YearMonth(S.param("date").map(s => DateTime.parse(s)).getOrElse(DateTime.now())).toInterval
    val taskSheet = ReportService.taskSheetData(
      User.currentUser.get,
      interval,
      d => new MonthDay(d)
    )

    (
      ".dayHeader" #> dates(taskSheet).map(d => ".dayHeader *" #> d.get(d.getFieldTypes.last)) &
        ".TaskRow" #> tasks(taskSheet).map { t =>
        ".taskFullName *" #> t.name & ".taskFullName [title]" #> t.name &
        ".dailyData" #> dates(taskSheet)
          .map(d => ".dailyData *" #> formattedDurationInMinutes(taskSheet, d, t) & formatData(interval, d)) &
        ".taskSum *" #> sumByTasks(taskSheet)(t).minutes
      } &
      ".dailySum" #> dates(taskSheet).map(d => ".dailySum *" #> sumByDates(taskSheet)(d).minutes) &
      ".totalSum *" #> sum(taskSheet).minutes & ".totalSum [title]" #> sum(taskSheet).hours
    )(in)
  }

  def formattedDurationInMinutes[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]], d: RD, t: TaskSheetItem): String =
    ts.get(d).flatMap(_.get(t)).map(_.minutes.toString).getOrElse("")

  def dates[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): List[RD] =
    ts.keys.toList.sorted

  def sumByDates[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): Map[RD, Duration] =
    ts.mapValues(m => m.values.foldLeft(Duration.millis(0))(_ + _))

  def tasks[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): List[TaskSheetItem] =
    ts.values.flatMap(_.keySet).toSet.toList.sortBy((t: TaskSheetItem) => t.name)

  def sumByTasks[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): Map[TaskSheetItem, Duration] =
    ts.values.flatMap(_.toList).toList.foldedMap(Duration.millis(0))(_ + _)

  def sum[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): Duration =
    sumByDates(ts).values.foldLeft(Duration.millis(0))(_ + _)

  def formatData[RD <: ReadablePartial](i: Interval, d: RD): CssSel =
    ".dailyData [class]" #> Some(d)
      .filter(hasDayFieldType)
      .flatMap(d => mapToDateTime(i, d))
      .filter(isWeekend)
      .map(_ => "colWeekend")
      .getOrElse("colWeekday")

  def mapToDateTime[RD <: ReadablePartial](i: Interval, d: RD): Option[DateTime] =
    List(d.toDateTime(i.start), d.toDateTime(i.end)).find(i.contains(_))

  def isWeekend(d: DateTime): Boolean =
    d.getDayOfWeek == DateTimeConstants.SATURDAY || d.getDayOfWeek == DateTimeConstants.SUNDAY

  def hasDayFieldType[RD <: ReadablePartial](d: RD): Boolean =
    d.isSupported(DateTimeFieldType.dayOfWeek()) || d.isSupported(DateTimeFieldType.dayOfMonth()) || d.isSupported(DateTimeFieldType.dayOfYear())
}
