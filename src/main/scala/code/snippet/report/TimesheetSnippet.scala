package code.snippet

import code.commons.TimeUtils

import net.liftweb.http.S
import scala.xml.NodeSeq
import code.service.ReportService
import code.service.TaskItemService.IntervalQuery
import code.snippet.mixin.DateFunctions
import net.liftweb.util.Helpers._
import org.joda.time.YearMonth

import scala.util.Try

/**
 * Timesheet displaying component.
 * @author David Csakvari
 */
class TimesheetSnippet extends DateFunctions {

  /**
   * Tasksheet download link.
   */
  def timesheetExportLink(in: NodeSeq): NodeSeq = {
    (
      "a [href]" #> ("/export/timesheet/" + offsetInDays)
    ).apply(in)
  }

  /**
   * Timesheet display.
   */
  def timesheet(in: NodeSeq): NodeSeq = {
    def decimalsToLocale(value: String): String = {
      if (S.locale.toString == "hu")
        value.replace(".", ",")
      else
        value
    }

    val timesheetData = ReportService.getTimesheetData(IntervalQuery(new YearMonth(TimeUtils.currentYear(offsetInDays), TimeUtils.currentMonth(offsetInDays) + 1).toInterval))
    if (timesheetData.nonEmpty) {
      (
        ".item *" #> timesheetData.map(
          row => {
            ".day *" #> row._1 &
              ".from *" #> row._2 &
              ".to *" #> row._3 &
              ".sum *" #> decimalsToLocale(f"${row._4}%1.1f")
          }
        ) &
        ".sumtotal *" #> decimalsToLocale(f"${timesheetData.map(row => Try(row._4.toDouble).getOrElse(0.0d)).sum}%1.1f")
      ).apply(in)
    } else {
      <lift:embed what="no_data"/>
    }
  }
}
