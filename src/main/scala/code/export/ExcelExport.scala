package code
package export

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, _}

import code.model._
import code.service.ReportService
import code.service.TaskItemService.IntervalQuery
import code.util.I18n
import net.liftweb.http.S
import net.liftweb.util.Props
import org.apache.poi.hssf.usermodel._
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Cell
import org.joda.time.{LocalDate, YearMonth}
import net.liftweb.common.Box

import scala.collection.JavaConversions._

/**
 * Excel export features.
 */
object ExcelExport {
  val templatePath = Props.get("export.excel.timesheet_template").openOrThrowException("No Excel template defined for Timesheets!")

  /**
   * Timesheet Excel export.
   */
  def exportTimesheet(userBox: Box[User], offset: Int) = {
    val user = userBox.openOrThrowException("No user found!")
    var fos: ByteArrayOutputStream = null;
    var array: Array[Byte] = null
    val yearMonth = new YearMonth(LocalDate.now().plusDays(offset))
    try {

      // template
      val fs = new POIFSFileSystem(new FileInputStream(templatePath))
      val workbook = new HSSFWorkbook(fs, true)

      // parameters
      val userName = user.lastName + " " + user.firstName

      val monthText = I18n.Dates.printLongForm(yearMonth, S.locale)
      val dates = ReportService.getTimesheetData(IntervalQuery(yearMonth.toInterval), userBox)

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
        findCell("{ta_date_" + i + "}") foreach { cell => cell.setCellValue(if (data._1 != null) data._1.toString else null) }
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
    val name = s"timesheet_${yearMonth.toString}.xls";

    (contentStream, name)
  }
}
