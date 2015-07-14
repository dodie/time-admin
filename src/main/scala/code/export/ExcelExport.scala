package code
package export

import java.io._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import scala.collection.JavaConversions._
import scala.util.Sorting
import org.apache.poi.hssf.usermodel._
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellReference
import org.joda.time.DateTime
import code.commons.TimeUtils
import code.model._
import code.service._
import net.liftweb.util.Props
import net.liftweb.http.S
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.util.CellRangeAddress
import code.service.ReportService
import java.util.Date

/**
 * Excel export features.
 */
object ExcelExport {

  /**
   * Timesheet Excel export.
   */
  def exportTimesheet(userId: Long, offset: Int) = {
    var fos: ByteArrayOutputStream = null;
    var array: Array[Byte] = null
    try {
      // template
      val templatePath = Props.get("export.excel.timesheet_template").get
      val fs = new POIFSFileSystem(new FileInputStream(templatePath))
      val workbook = new HSSFWorkbook(fs, true)

      // parameters
      val user = User.findByKey(userId).get
      val userName = user.lastName + " " + user.firstName
      val monthText = TimeUtils.currentYear(offset) + " " + TimeUtils.monthNumberToText(TimeUtils.currentMonth(offset))
      val dates = ReportService.getTimesheetData(offset)

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

    new ByteArrayInputStream(array)
  }

  /**
   * Tasksheet Excel export.
   */
  def exportTasksheet(blank: Boolean, offset: Int) = {
    var fos: ByteArrayOutputStream = null
    var array: Array[Byte] = null
    try {
      // Initialize workbook
      val workbook = new HSSFWorkbook
      val wsheet = workbook.createSheet("Tasksheet")
      val cYear = TimeUtils.currentYear(offset)
      val cMonth = TimeUtils.currentMonth(offset)
      val monthText = cYear + ". " + TimeUtils.monthNumberToText(cMonth)
      var dt = new DateTime(cYear, cMonth + 1, 1, 0, 0, 0, 0)
      val monthLength = TimeUtils.getLastDayOfMonth(dt)

      // Initialize styles
      val boldFont = workbook.createFont()
      boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD)

      val weekendCellStyle = workbook.createCellStyle()
      weekendCellStyle.setFont(boldFont)
      weekendCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND)
      weekendCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex())

      val borderType = CellStyle.BORDER_THIN

      val boldCellStyle = workbook.createCellStyle()
      boldCellStyle.setFont(boldFont);

      val weekendHeaderCellStyle = workbook.createCellStyle()
      weekendHeaderCellStyle.cloneStyleFrom(weekendCellStyle)
      weekendHeaderCellStyle.setBorderBottom(borderType)

      val centeredBoldCellStyle = workbook.createCellStyle()
      centeredBoldCellStyle.cloneStyleFrom(boldCellStyle)
      centeredBoldCellStyle.setAlignment(CellStyle.ALIGN_CENTER)

      val footerCellStyle = workbook.createCellStyle()
      footerCellStyle.cloneStyleFrom(boldCellStyle)
      footerCellStyle.setBorderTop(borderType)

      val headerCellStyle = workbook.createCellStyle()
      headerCellStyle.cloneStyleFrom(boldCellStyle)
      headerCellStyle.setBorderBottom(borderType)

      // Create headers
      var rowNum = 0
      val firstHeaderRow = wsheet.createRow(rowNum)
      val monthTextCell = firstHeaderRow.createCell(2)
      monthTextCell.setCellStyle(centeredBoldCellStyle)
      monthTextCell.setCellValue(monthText)
      wsheet.addMergedRegion(new CellRangeAddress(0, 0, 2, monthLength + 1))
      rowNum += 1

      val secondHeaderRow = wsheet.createRow(rowNum)
      val paCell = secondHeaderRow.createCell(0)
      paCell.setCellStyle(headerCellStyle)
      paCell.setCellValue(S.?("export.tasksheet.project_identifier"))
      val statusCell = secondHeaderRow.createCell(1)
      statusCell.setCellStyle(headerCellStyle)
      statusCell.setCellValue(S.?("export.tasksheet.active"))
      for (i <- 2 to (monthLength + 1)) {
        val dayCell = secondHeaderRow.createCell(i)
        dayCell.setCellValue(String.valueOf(i - 1))
        if (TimeUtils.isWeekend(dt)) {
          dayCell.setCellStyle(weekendHeaderCellStyle)
        } else {
          dayCell.setCellStyle(headerCellStyle)
        }
        dt = dt.plusDays(1)
      }
      val headerSumTextCell = secondHeaderRow.createCell(monthLength + 2)
      headerSumTextCell.setCellStyle(headerCellStyle)
      headerSumTextCell.setCellValue(S.?("export.tasksheet.sum"))
      rowNum += 1

      // Create taskitem rows
      val taskList = TaskService.getTaskArray(activeOnly = false)

      if (!taskList.isEmpty) {
        val taskMatrix = ReportService.getTasksheetData(offset);
        for (std <- taskList.toSeq) {
          val itemRow = wsheet.createRow(rowNum)
          val taskNameCell = itemRow.createCell(0)
          taskNameCell.setCellValue(std.getFullName())
          val activityCell = itemRow.createCell(1)
          activityCell.setCellType(Cell.CELL_TYPE_NUMERIC)
          activityCell.setCellValue(if (std.task.active.get) 1D else 0D)
          dt = new DateTime(cYear, cMonth + 1, 1, 0, 0, 0, 0)
          for (i <- 2 to (monthLength + 1)) {
            val dayCell = itemRow.createCell(i)

            if (!blank) {
              val dayKey = i - 1
              val taskKey = std.task.id.get

              if (taskMatrix.containsKey(dayKey) && taskMatrix(dayKey).contains(taskKey)) {
                val duration = taskMatrix(dayKey)(taskKey)
                if (0 < duration) {
                  dayCell.setCellValue(duration / 60000)
                }
              }
            }

            if (TimeUtils.isWeekend(dt)) {
              dayCell.setCellStyle(weekendCellStyle)
            }
            dt = dt.plusDays(1)
          }

          val sumCell = itemRow.createCell(monthLength + 2)
          sumCell.setCellType(Cell.CELL_TYPE_FORMULA)
          sumCell.setCellStyle(boldCellStyle)
          rowNum += 1
          sumCell.setCellFormula("SUM(" + CellReference.convertNumToColString(2) + rowNum + ":" + CellReference.convertNumToColString(monthLength + 1) + rowNum + ")")
        }
      }

      // Create summary row
      val summaryRow = wsheet.createRow(rowNum)
      val summaryTextCell = summaryRow.createCell(0)
      summaryTextCell.setCellStyle(footerCellStyle)
      summaryTextCell.setCellValue(S.?("export.tasksheet.sum"))
      wsheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 1))
      for (i <- 2 to (monthLength + 2)) {
        val sumCell = summaryRow.createCell(i)
        sumCell.setCellStyle(footerCellStyle)
        sumCell.setCellType(Cell.CELL_TYPE_FORMULA)
        val colName = CellReference.convertNumToColString(i)
        sumCell.setCellFormula("SUM(" + colName + "3:" + colName + "" + (rowNum) + ")")
      }

      // Create sheet freeze
      wsheet.createFreezePane(2, 2)

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
    new ByteArrayInputStream(array)
  }
}
