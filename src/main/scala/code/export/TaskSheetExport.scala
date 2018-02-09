package code.export

import code.model.User
import code.service.ReportService
import code.service.TaskItemService.IntervalQuery
import code.util.{I18n, ISO}
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

  def workbook(i: IntervalQuery, user: Box[User], dimension: String, taskFilter: String): (XSSFWorkbook, String) = {
    val taskSheet = ReportService.taskSheetData(i, user, taskFilter)

    val ds = dates(taskSheet)
    val ts = tasks(taskSheet)

    def toDimension(minutes: Long): Double = {
      if (dimension == "minutes") {
        minutes
      } else if (dimension == "hours") {
        minutes / 60.0d
      } else if (dimension == "manDays") {
         (minutes / 60.0d) / 8.0d
      } else {
        throw new RuntimeException("Unknown dimension.")
      }
    }

    val userName = user.map(u => s"${u.lastName} ${u.firstName} ").getOrElse("")
    val fullTitle = userName + I18n.Dates.printLongForm(i.interval, S.locale)

    val columnWith = 0 -> (ds.length + 1)

    val xlsx = Sheet(
      name = fullTitle,
      rows =
        Row(main(fullTitle)) ::
          Row(taskTitle :: (ds.map(d => header(d.toString)) :+ sumTitle)) :: {
          ts.map { t =>
            Row(left(t.name) :: (ds.map { d =>
              value(toDimension(duration(taskSheet, d, t).minutes)).withDefaultStyle(formatCell(d))
            } :+ summary(toDimension(sumByTasks(taskSheet)(t).minutes))))
          } :+
            Row(sumFooter :: (ds.map { d => footer(toDimension(sumByDates(taskSheet)(d).minutes)) } :+ footer(toDimension(sum(taskSheet).minutes))))
        },
      columns = autoSizedColumns(range(columnWith)),
      mergedRegions = List(CellRange(0 -> 0, columnWith)),
      paneAction = FreezePane(1, 2),
      style = styles.default
    ).convertAsXlsx()


    (xlsx, s"tasksheet_${(userName + ISO.Dates.print(i.interval)).toLowerCase.replace(" ", "")}.xlsx")
  }

  def formatCell(d: ReadablePartial): Option[CellStyle] =
    Try(d.get(DateTimeFieldType.dayOfWeek())).toOption flatMap { i =>
      if (i == SATURDAY || i == SUNDAY) Some(styles.weekend) else None
    }

  def taskTitle: Cell = Cell(value = S.?("export.tasksheet.identifier"), style = styles.header.withHorizontalAlignment(styles.left.horizontalAlignment.get))

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
