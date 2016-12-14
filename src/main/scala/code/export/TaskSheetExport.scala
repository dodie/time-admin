package code.export

import code.model.User
import code.service.ReportService
import code.util.TaskSheetUtils
import code.util.TaskSheetUtils._
import com.norbitltd.spoiwo.model.enums.CellHorizontalAlignment.Center
import com.norbitltd.spoiwo.model.{CellStyle, _}
import com.norbitltd.spoiwo.model.enums.CellFill
import net.liftweb.http.S
import org.joda.time.ReadablePartial
import com.github.nscala_time.time.Imports._
import com.norbitltd.spoiwo.model.enums.CellBorderStyle.{Medium, Thin}
import net.liftweb.common.Box
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions.XlsxSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object TaskSheetExport {

  def workbook(interval: Interval, scale: LocalDate => ReadablePartial, user: Box[User]): (XSSFWorkbook, String) = {
    val taskSheet = ReportService.taskSheetData(interval, scale, User.currentUser)

    val ds = dates(taskSheet)
    val ts = tasks(taskSheet)

    val userName = user.map(u => s"${u.lastName} ${u.firstName} ").getOrElse("")
    val fullTitle = userName + title(interval, scale)

    val columnWith = 0 -> (ds.length + 1)

    val xlsx = Sheet(
      name = fullTitle,
      rows =
        Row(main(fullTitle)) ::
          Row(taskTitle :: (ds.map(d => header(d.toString)) :+ sumTitle)) :: {
          ts.map { t =>
            Row(Cell(t.name) :: (ds.map { d =>
              value(duration(taskSheet, d, t).minutes).withStyle(if (isWeekend(interval, d)) weekend else bold)
            } :+ value(sumByTasks(taskSheet)(t).minutes)))
          } :+
            Row(sumFooter :: (ds.map { d => footer(sumByDates(taskSheet)(d).minutes) } :+ footer(sum(taskSheet).minutes)))
        },
      columns = autoSizedColumns(range(columnWith)),
      mergedRegions = List(CellRange(0 -> 0, columnWith)),
      paneAction = FreezePane(1, 2)
    ).convertAsXlsx()

    (xlsx, s"tasksheet_${fullTitle.toLowerCase.replace(" ", "")}.xlsx")
  }

  def taskTitle: Cell = header(S.?("export.tasksheet.project_identifier"))

  def sumTitle: Cell = header(S.?("export.tasksheet.sum"))

  def sumFooter: Cell = footer(S.?("export.tasksheet.sum"))

  def main[T: CellValueType](t: T): Cell = Cell(value = t, style = bold.withHorizontalAlignment(Center))

  def header[T: CellValueType](t: T): Cell = Cell(value = t, style = bold.withBorders(CellBorders().withBottomStyle(Medium)))

  def value[T: CellValueType](t: T): Cell = Cell(value = t, style = bold)

  def footer[T: CellValueType](t: T): Cell = Cell(value = t, style = bold.withBorders(CellBorders().withTopStyle(Thin)))

  def autoSizedColumns(range: Range): List[Column] = (for (i <- range) yield Column(index = i, autoSized = true)).toList

  def range(p: (Int, Int)): Range = p match { case (start, end) => start to end }

  def isWeekend(i: Interval, d: ReadablePartial): Boolean =
    Some(d).filter(hasDayFieldType).flatMap(mapToDateTime(i, _)).exists(TaskSheetUtils.isWeekend)

  private lazy val weekend = CellStyle(font = Font(bold = true), fillPattern = CellFill.Solid, fillForegroundColor = Color(221, 221, 221))

  private lazy val bold: CellStyle = CellStyle(font = Font(bold = true))

}
