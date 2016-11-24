package code.export

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import code.commons.TimeUtils
import code.model.User
import code.service.ReportService
import code.util.TaskSheetUtils.{tasks, _}
import com.norbitltd.spoiwo.model.enums.CellHorizontalAlignment.Center
import com.norbitltd.spoiwo.model.{CellStyle, _}
import com.norbitltd.spoiwo.model.enums.{CellBorderStyle, CellFill}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions.XlsxSheet
import net.liftweb.http.S
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import code.util.DateTimeWithLocalizedMonthNames._
import org.apache.poi.ss.util.CellReference

/**
  * Created by suliatis on 23/11/16.
  */
object ExcelExport2 {

  def tasksheet(user: User, offset: Int): (InputStream, String) = {
    val date = new DateTime(TimeUtils.currentDayStartInMs(offset))

    val interval = date.monthOfYear.toInterval
    val taskSheet = ReportService.taskSheetData(User.currentUser, interval, d => d)

    val ds = dates(taskSheet)
    val ts = tasks(taskSheet)

    val colls = Stream.from(0).map(CellReference.convertNumToColString)

    val title = Row(Cell(value = s"${date.getYear}. ${date.getMonthNameOfYear}"))
    val heading = Row(Cell(value = tasksHeader, style = header) :: (ds.map(d => Cell(d.toString, style = header)) :+ Cell("sum")))

    val content = ts.zip(Stream.from(3)).map { case (t, row) =>
      Row(Cell(t.name) :: (ds.map { d => Cell(durationInMinutes(taskSheet, d, t)) } :+ Cell(s"=SUM(${colls(1)}$row:${colls(ds.length)}$row)")))
    }

    val footer = Row(Cell("sum") :: (ds.map { d => Cell(sumByDates(taskSheet)(d).minutes) } :+ Cell(s"=SUM(${colls(1)}${ts.length+2}:${colls(ds.length)}${ts.length+2})")))

    val regions = List(CellRange(0 -> 0, 0 -> (ds.length + 1)))

    val workbook = Sheet(rows = title :: heading :: (content :+ footer), mergedRegions = regions).convertAsXlsx()

    val contentStream = using(new ByteArrayOutputStream()) { out =>
      workbook.write(out)
      out.flush()
      new ByteArrayInputStream(out.toByteArray)
    }
    val fileName = s"tasksheet_${date.getYear}-${date.getMonthOfYear}_${user.firstName.toLowerCase + user.lastName.toLowerCase}.xls"
    (contentStream, fileName)
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
