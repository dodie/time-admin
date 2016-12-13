package code.export

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import code.model.User
import code.service.ReportService
import code.util.TaskSheetUtils._
import com.norbitltd.spoiwo.model.enums.CellHorizontalAlignment.Center
import com.norbitltd.spoiwo.model.{CellStyle, _}
import com.norbitltd.spoiwo.model.enums.{CellBorderStyle, CellFill}
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

    val main = Row(Cell(value = fullTitle))
    val heading = Row(Cell(value = tasksHeader, style = header) :: (ds.map(d => Cell(d.toString, style = header)) :+ Cell("sum")))

    val content = ts.map { t =>
      Row(Cell(t.name) :: (ds.map { d => Cell(durationInMinutes(taskSheet, d, t)) } :+ Cell(sumByTasks(taskSheet)(t).minutes)))
    }

    val footer = Row(Cell("sum") :: (ds.map { d => Cell(sumByDates(taskSheet)(d).minutes) } :+ Cell(sum(taskSheet).minutes)))

    val regions = List(CellRange(0 -> 0, 0 -> (ds.length + 1)))

    val workbook = Sheet(rows = main :: heading :: (content :+ footer), mergedRegions = regions).convertAsXlsx()

    val contentStream = using(new ByteArrayOutputStream()) { out =>
      workbook.write(out)
      out.flush()
      new ByteArrayInputStream(out.toByteArray)
    }

    (contentStream, s"tasksheet_${fullTitle.toLowerCase.replace(" ", "")}.xls")
  }

  def using[A, B <: {def close(): Unit}] (closeable: B) (f: B => A): A = try { f(closeable) } finally { closeable.close() }

  private lazy val tasksHeader = S.?("export.tasksheet.project_identifier")

  private lazy val weekend = CellStyle(font = Font(bold = true), fillPattern = CellFill.Solid, fillForegroundColor = Color.LightGreen)

  private lazy val bold: CellStyle = CellStyle(font = Font(bold = true))

  private lazy val centeredBold = bold.withHorizontalAlignment(horizontalAlignment = Center)

  private lazy val header = bold.withBorders(CellBorders().withStyle(CellBorderStyle.Thin))

  private lazy val weekendHeader = weekend.defaultWith(header)

  private lazy val footer = header
}
