package code.snippet

import scala.xml.NodeSeq

import code.service.ReportService
import code.snippet.mixin.DateFunctions
import net.liftweb.util.BindHelpers.strToCssBindPromoter

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
    if (!timesheetData.isEmpty) {
      (
        ".item *" #> timesheetData.map(
          row => {
            ".day *" #> row._1 &
              ".from *" #> row._2 &
              ".to *" #> row._3 &
              ".sum *" #> row._4
          }
        )
      ).apply(in)
    } else {
      <lift:embed what="no_data"/>
    }
  }
}
