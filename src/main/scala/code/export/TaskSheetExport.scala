package code.export

import code.model.User
import code.service.ReportService
import code.util.TaskSheetUtils._
import com.github.nscala_time.time.Imports._
import com.norbitltd.spoiwo.model._
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions.XlsxSheet
import net.liftweb.common.Box
import net.liftweb.http.S
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.joda.time.DateTimeConstants.{SATURDAY, SUNDAY}
import org.joda.time.{DateTimeFieldType, ReadablePartial}

import scala.util.Try

object TaskSheetExport {

  def workbook(interval: Interval, scale: LocalDate => ReadablePartial, user: Box[User]): (XSSFWorkbook, String) = {
    val taskSheet = ReportService.taskSheetData(interval, scale, user)

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
            Row(left(t.name) :: (ds.map { d =>
              value(duration(taskSheet, d, t).minutes).withDefaultStyle(formatCell(d))
            } :+ summary(sumByTasks(taskSheet)(t).minutes)))
          } :+
            Row(sumFooter :: (ds.map { d => footer(sumByDates(taskSheet)(d).minutes) } :+ footer(sum(taskSheet).minutes)))
        },
      columns = autoSizedColumns(range(columnWith)),
      mergedRegions = List(CellRange(0 -> 0, columnWith)),
      paneAction = FreezePane(1, 2),
      style = styles.default
    ).convertAsXlsx()

    (xlsx, s"tasksheet_${fullTitle.toLowerCase.replace(" ", "")}.xlsx")
  }

  def formatCell[D <: ReadablePartial](d: D): Option[CellStyle] =
    Try(d.get(DateTimeFieldType.dayOfWeek())).toOption flatMap { i =>
      if (i == SATURDAY || i == SUNDAY) Some(styles.weekend) else None
    }

  def taskTitle: Cell = Cell(value = S.?("export.tasksheet.project_identifier"), style = styles.header.withHorizontalAlignment(styles.left.horizontalAlignment.get))

  def sumTitle: Cell = header(S.?("export.tasksheet.sum"))

  def sumFooter: Cell = Cell(value = S.?("export.tasksheet.sum"), style = styles.footer.withHorizontalAlignment(styles.left.horizontalAlignment.get))

  def main[T: CellValueType](t: T): Cell = Cell(value = t, style = styles.main)

  def header[T: CellValueType](t: T): Cell = Cell(value = t, style = styles.header)

  def left[T: CellValueType](t: T): Cell = Cell(value = t, style = styles.left)

  def value[T: CellValueType](t: T): Cell = Cell(value = t, style = styles.default)

  def summary[T: CellValueType](t: T): Cell = Cell(value = t, style = styles.summary)

  def footer[T: CellValueType](t: T): Cell = Cell(value = t, style = styles.footer)

  def autoSizedColumns(range: Range): List[Column] = (for (i <- range) yield Column(index = i, autoSized = true)).toList

  def range(p: (Int, Int)): Range = p match { case (start, end) => start to end }

  object styles {
    import com.norbitltd.spoiwo.model.Color.{LightGrey, LightYellow}
    import com.norbitltd.spoiwo.model.enums.CellBorderStyle.{Medium, None, Thin}
    import com.norbitltd.spoiwo.model.enums.CellFill.Solid
    import com.norbitltd.spoiwo.model.enums.CellHorizontalAlignment.{Center, Left, Right}
    import com.norbitltd.spoiwo.model.{CellStyle, _}

    lazy val default: CellStyle = CellStyle(
      font = Font(bold = true),
      horizontalAlignment = Right,
      borders = CellBorders(None)
    )

    lazy val main: CellStyle = default.withHorizontalAlignment(Center)

    lazy val header: CellStyle = default.withBorders(CellBorders().withBottomStyle(Medium))

    lazy val left: CellStyle = default.withHorizontalAlignment(Left)

    lazy val footer: CellStyle = default.withBorders(CellBorders().withTopStyle(Thin))

    lazy val weekend: CellStyle = default.withFillPattern(Solid).withFillForegroundColor(LightGrey)

    lazy val summary: CellStyle = default.withFillPattern(Solid).withFillForegroundColor(LightYellow)
  }

}
