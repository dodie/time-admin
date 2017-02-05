package code
package export

import java.io._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

import scala.collection.JavaConversions._
import org.apache.poi.hssf.usermodel._
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Cell
import code.commons.TimeUtils
import code.model._
import net.liftweb.util.Props
import net.liftweb.http.S
import code.service.ReportService
import code.service.TaskItemService.IntervalQuery
import org.joda.time.YearMonth

/**
 * Excel export features.
 */
object ExcelExport {
  val templatePath = Props.get("export.excel.timesheet_template").openOrThrowException("No Excel template defined for Timesheets!")

  /**
   * Timesheet Excel export.
   */
  def exportTimesheet(user: User, offset: Int) = {
    var fos: ByteArrayOutputStream = null;
    var array: Array[Byte] = null
    try {

      // template
      val fs = new POIFSFileSystem(new FileInputStream(templatePath))
      val workbook = new HSSFWorkbook(fs, true)

      // parameters
      val userName = user.lastName + " " + user.firstName
      val monthText = TimeUtils.currentYear(offset) + " " + TimeUtils.monthNumberToText(TimeUtils.currentMonth(offset))
      val dates = ReportService.getTimesheetData(IntervalQuery(new YearMonth(TimeUtils.currentYear(offset), TimeUtils.currentMonth(offset) + 1).toInterval))

      // spreadsheet to export
      val sheet = workbook.getSheet("Timesheet")

      /**
       * Finds and returns the first cell in the sheet that contains the given text.
       */
      def findCell(text: String): Option[Cell] = {
        for (row <- sheet.rowIterator(); cell <- row) {
          if (cell.getCellType() == 1 && text.equals(cell.getStringCellValue())) {
            return Some(cell)
          }
        }
        None
      }

      // localize texts
      findCell("{ta_text_date}") foreach { cell => cell.setCellValue(S.?("timesheet.date")) }
      findCell("{ta_text_arrival}") foreach { cell => cell.setCellValue(S.?("timesheet.arrival")) }
      findCell("{ta_text_leave}") foreach { cell => cell.setCellValue(S.?("timesheet.leave")) }
      findCell("{ta_text_time_sum_hour}") foreach { cell => cell.setCellValue(S.?("timesheet.time_sum_hour")) }

      // insert data
      findCell("{ta_name}") foreach { cell => cell.setCellValue(userName) }
      findCell("{ta_month}") foreach { cell => cell.setCellValue(monthText) }
      for (i <- 1 to 31) {
        val data = if (dates.size >= i) dates(i - 1) else (null, null, null, null)
        findCell("{ta_date_" + i + "}") foreach { cell => cell.setCellValue(data._1) }
        findCell("{ta_arrive_" + i + "}") foreach { cell => cell.setCellValue(data._2) }
        findCell("{ta_leave_" + i + "}") foreach { cell => cell.setCellValue(data._3) }
      }

      // write sheet
      fos = new ByteArrayOutputStream()
      workbook.write(fos)
    } catch {
      case e: Exception => e.printStackTrace
    } finally {
      if (fos != null) {
        try {
          fos.flush();
          array = fos.toByteArray
          fos.close();
        } catch {
          case e: IOException => e.printStackTrace
        }
      }
    }

    val contentStream = new ByteArrayInputStream(array)
    val name = "timesheet_" + TimeUtils.currentYear(offset.toInt) + "-" + (TimeUtils.currentMonth(offset.toInt) + 1) + ".xls";

    (contentStream, name)
  }
}
