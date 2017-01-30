package code.snippet

import scala.xml.NodeSeq
import code.service.ReportService
import code.snippet.mixin.DateFunctions
import net.liftweb.util.Helpers._

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
    val timesheetData = ReportService.getTimesheetData(offsetInDays)
    if (timesheetData.nonEmpty) {
      (
        ".item *" #> timesheetData.map(
          row => {
            ".day *" #> row._1 &
              ".from *" #> row._2 &
              ".to *" #> row._3 &
              ".sum *" #> f"${row._4}%1.1f"
          }
        ) &
        ".sumtotal *" #> f"${timesheetData.map(row => Try(row._4.toDouble).getOrElse(0.0d)).sum}%1.1f"
      ).apply(in)
    } else {
      <lift:embed what="no_data"/>
    }
  }
}
