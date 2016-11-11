package code.snippet

import scala.xml.NodeSeq
import code.model.User
import code.service.ReportService.TaskSheet
import code.service.{ReportService, TaskSheetItem}
import code.snippet.mixin.DateFunctions
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import net.liftweb.http.S
import com.github.nscala_time.time.Imports._
import code.util.ListToFoldedMap._
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.util.CssSel
import org.joda.time.{DateTimeConstants, DateTimeFieldType, ReadablePartial}

import scala.util.Try

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

  def tasksheet(in: NodeSeq): NodeSeq = {
    val date = S.param("date").or(S.getSessionAttribute("date").flatMap(tryParseDate))
    date.foreach(d => S.setSessionAttribute("date", d.toString))

    val interval = new YearMonth(date.getOrElse(DateTime.now())).toInterval

    renderTaskSheet(interval, d => d, User.currentUser)(in)
  }

  def tasksheetSummary(in: NodeSeq): NodeSeq = {
    val interval = try {
      val intervalStart = S.param("intervalStart").flatMap(tryParseDate)
      val intervalEnd = S.param("intervalEnd").flatMap(tryParseDate)

      new Interval(
        new YearMonth(intervalStart.getOrElse(DateTime.now())).toInterval.start,
        new YearMonth(intervalEnd.getOrElse(DateTime.now())).toInterval.end
      )
    } catch {
      case e: Exception => new Interval(
          new YearMonth(DateTime.now()).toInterval.start,
          new YearMonth(DateTime.now()).toInterval.end
        )
    }

    val userId = S.param("user").getOrElse("-1").toLong
    val user = User.findByKey(userId)

    renderTaskSheet(interval, d => new YearMonth(d), user)(in)
  }

  def tryParseDate(s: String): Box[DateTime] = Try(DateTime.parse(s)).map(d => Full(d)).getOrElse(Empty)

  def renderTaskSheet[D <: ReadablePartial](i: Interval, f: LocalDate => D, u: Box[User]): CssSel = {
    val taskSheet = ReportService.taskSheetData(u, i, f)

    ".dayHeader" #> dates(taskSheet).map(d => ".dayHeader *" #> d.toString) &
        ".TaskRow" #> tasks(taskSheet).map { t =>
          ".taskFullName *" #> t.name & ".taskFullName [title]" #> t.name &
            ".dailyData" #> dates(taskSheet)
              .map(d => ".dailyData *" #> formattedDurationInMinutes(taskSheet, d, t) & formatData(i, d)) &
            ".taskSum *" #> sumByTasks(taskSheet)(t).minutes
        } &
        ".dailySum" #> dates(taskSheet).map(d => ".dailySum *" #> sumByDates(taskSheet)(d).minutes) &
        ".totalSum *" #> sum(taskSheet).minutes & ".totalSum [title]" #> sum(taskSheet).hours
  }

  def formattedDurationInMinutes[D <: ReadablePartial](ts: TaskSheet[D], d: D, t: TaskSheetItem): String =
    ts.get(d).flatMap(_.get(t)).map(_.minutes.toString).getOrElse("")

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

  def formatData[D <: ReadablePartial](i: Interval, d: D): CssSel =
    ".dailyData [class]" #> Some(d)
      .filter(hasDayFieldType)
      .flatMap(d => mapToDateTime(i, d))
      .filter(isWeekend)
      .map(_ => "colWeekend")
      .getOrElse("colWeekday")

  def mapToDateTime[D <: ReadablePartial](i: Interval, d: D): Option[DateTime] =
    List(d.toDateTime(i.start), d.toDateTime(i.end)).find(i.contains(_))

  def isWeekend(d: DateTime): Boolean =
    d.getDayOfWeek == DateTimeConstants.SATURDAY || d.getDayOfWeek == DateTimeConstants.SUNDAY

  def hasDayFieldType[RD <: ReadablePartial](d: RD): Boolean =
    d.isSupported(DateTimeFieldType.dayOfWeek()) || d.isSupported(DateTimeFieldType.dayOfMonth()) || d.isSupported(DateTimeFieldType.dayOfYear())
}
