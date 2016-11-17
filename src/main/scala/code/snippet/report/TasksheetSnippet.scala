package code.snippet

import scala.xml.NodeSeq
import code.model.User
import code.service.ReportService
import code.snippet.mixin.DateFunctions
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import net.liftweb.http.S
import com.github.nscala_time.time.Imports._
import net.liftweb.common.Box
import net.liftweb.util.CssSel
import org.joda.time.ReadablePartial
import code.util.TaskSheetUtils._

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

  def tasksheetSummaryExportLink(in: NodeSeq): NodeSeq = {
    ("a [href]" #> s"/export/tasksheetSummary?intervalStart=${S.param("intervalStart").getOrElse(LocalDate.now().toString)}&intervalEnd=${S.param("intervalEnd").getOrElse(LocalDate.now().toString)}&user=${S.param("user").getOrElse("-1").toLong}").apply(in)
  }

  def tasksheet(in: NodeSeq): NodeSeq = {
    val date = S.param("date").or(S.getSessionAttribute("date")).map(s => DateTime.parse(s))
    date.foreach(d => S.setSessionAttribute("date", d.toString))

    val interval = new YearMonth(date.getOrElse(DateTime.now())).toInterval

    renderTaskSheet(interval, d => d, User.currentUser)(in)
  }

  def tasksheetSummary(in: NodeSeq): NodeSeq = {
    val interval = try {
      val intervalStart = S.param("intervalStart").map(s => DateTime.parse(s))
      val intervalEnd = S.param("intervalEnd").map(s => DateTime.parse(s))

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

  def renderTaskSheet[D <: ReadablePartial](i: Interval, f: LocalDate => D, u: Box[User]): CssSel = {
    val taskSheet = ReportService.taskSheetData(u, i, f)

    ".dayHeader" #> dates(taskSheet).map(d => ".dayHeader *" #> dayOf(d).map(_.toString).getOrElse(d.toString)) &
        ".TaskRow" #> tasks(taskSheet).map { t =>
          ".taskFullName *" #> t.name & ".taskFullName [title]" #> t.name &
            ".dailyData" #> dates(taskSheet)
              .map(d => ".dailyData *" #> formattedDurationInMinutes(taskSheet, d, t) & formatData(i, d)) &
            ".taskSum *" #> sumByTasks(taskSheet)(t).minutes
        } &
        ".dailySum" #> dates(taskSheet).map(d => ".dailySum *" #> sumByDates(taskSheet)(d).minutes) &
        ".totalSum *" #> sum(taskSheet).minutes & ".totalSum [title]" #> sum(taskSheet).hours
  }

  def formatData[D <: ReadablePartial](i: Interval, d: D): CssSel =
    ".dailyData [class]" #> Some(d)
      .filter(hasDayFieldType)
      .flatMap(d => mapToDateTime(i, d))
      .filter(isWeekend)
      .map(_ => "colWeekend")
      .getOrElse("colWeekday")
}
