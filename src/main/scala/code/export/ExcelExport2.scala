package code.export

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import code.model.User
import code.service.ReportService
import code.util.TaskSheetUtils
import code.util.TaskSheetUtils._
import com.norbitltd.spoiwo.model.enums.CellHorizontalAlignment.{Center, Right}
import com.norbitltd.spoiwo.model.{CellStyle, _}
import com.norbitltd.spoiwo.model.enums.{CellBorderStyle, CellFill, CellHorizontalAlignment}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions.XlsxSheet
import net.liftweb.http.S
import org.joda.time.{DateTime, ReadablePartial}
import com.github.nscala_time.time.Imports._
import net.liftweb.common.Box

/**
  * Created by suliatis on 23/11/16.
  */
object ExcelExport2 {

  def tasksheet(interval: Interval, scale: LocalDate => ReadablePartial, user: Box[User]): (InputStream, String) = {
    val taskSheet = ReportService.taskSheetData(interval, d => d, User.currentUser)

    val ds = dates(taskSheet)
    val ts = tasks(taskSheet)

    val userName = user.map(u => s"${u.lastName} ${u.firstName} ").getOrElse("")
    val fullTitle = userName + title(interval, scale)

    val workbook = Sheet(
      name = fullTitle,
      rows =
        Row(main(fullTitle)) ::
        Row(taskTitle :: (ds.map(d => header(d.toString)) :+ sumTitle)) :: {
          ts.map { t =>
            Row(Cell(t.name) :: (ds.map { d => value(duration(taskSheet, d, t).minutes) } :+ value(sumByTasks(taskSheet)(t).minutes)))
          } :+
          Row(sumFooter :: (ds.map { d => footer(sumByDates(taskSheet)(d).minutes) } :+ footer(sum(taskSheet).minutes)))
        },
      mergedRegions = List(CellRange(0 -> 0, 0 -> (ds.length + 1)))
    ).convertAsXlsx()

    val contentStream = using(new ByteArrayOutputStream()) { out =>
      workbook.write(out)
      out.flush()
      new ByteArrayInputStream(out.toByteArray)
    }

    (contentStream, s"tasksheet_${fullTitle.toLowerCase.replace(" ", "")}.xls")
  }


  def taskTitle: Cell = header(S.?("export.tasksheet.project_identifier"))

  def sumTitle: Cell = header(S.?("export.tasksheet.sum"))

  def sumFooter: Cell = footer(S.?("export.tasksheet.sum"))

  def main[T: CellValueType](t: T): Cell = Cell(value = t, style = bold.withHorizontalAlignment(Center))

  def header[T: CellValueType](t: T): Cell = Cell(value = t, style = bold.withBorders(CellBorders().withBottomStyle(CellBorderStyle.Medium)))

  def value[T: CellValueType](t: T): Cell = Cell(value = t, style = bold)

  def footer[T: CellValueType](t: T): Cell = Cell(value = t, style = bold.withBorders(CellBorders().withTopStyle(CellBorderStyle.Thin)))

  def using[A, B <: {def close(): Unit}] (closeable: B) (f: B => A): A = try { f(closeable) } finally { closeable.close() }

  private lazy val weekend = CellStyle(font = Font(bold = true), fillPattern = CellFill.Solid, fillForegroundColor = Color.LightGreen)

  private lazy val bold: CellStyle = CellStyle(font = Font(bold = true))

  private lazy val centeredBold = bold.withHorizontalAlignment(horizontalAlignment = Center)

  private lazy val header = bold.withBorders(CellBorders().withStyle(CellBorderStyle.Thin))

  private lazy val weekendHeader = weekend.defaultWith(header)

  private lazy val footer = header
}
