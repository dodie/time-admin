package code.snippet

import scala.xml.NodeSeq
import code.model.User
import code.service.UserService.nonAdmin
import code.service.{ReportService, UserService}
import code.snippet.Params.{parseInterval, parseUser, thisMonth}
import code.snippet.mixin.DateFunctions
import code.util.TaskSheetUtils
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import net.liftweb.http.S
import com.github.nscala_time.time.Imports._
import net.liftweb.common.Box
import net.liftweb.util.CssSel
import org.joda.time.ReadablePartial
import code.util.TaskSheetUtils._


/**
 * Tasksheet displaying component.
 * @author David Csakvari
 */
class TasksheetSnippet extends DateFunctions {

  /**
   * Tasksheet download link.
   */
  def tasksheetExportLink(in: NodeSeq): NodeSeq = {
    ("a [href]" #> s"/export/tasksheet?intervalStart=${S.param("intervalStart").getOrElse(LocalDate.now().toString)}&intervalEnd=${S.param("intervalEnd").getOrElse(LocalDate.now().toString)}${S.param("user").map("&user=" + _).getOrElse("")}").apply(in)
  }

  def title(in: NodeSeq): NodeSeq = {
    val (interval, scale) = parseInterval(S) getOrElse thisMonth()
    <span>{TaskSheetUtils.title(interval, scale)}</span>
  }

  def tasksheet(in: NodeSeq): NodeSeq = {
    val (interval, scale) = parseInterval(S) getOrElse thisMonth
    val user = User.currentUser filter nonAdmin or parseUser(S)

    renderTaskSheet(interval, scale, user)(in)
  }

  def renderTaskSheet[D <: ReadablePartial](i: Interval, f: LocalDate => D, u: Box[User]): CssSel = {
    val taskSheet = ReportService.taskSheetData(i, f, u)

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
