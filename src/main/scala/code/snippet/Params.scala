package code.snippet

import code.model.User
import com.github.nscala_time.time.Imports._
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http.S
import net.liftweb.util.ControlHelpers.tryo
import org.joda.time.ReadablePartial
import org.joda.time.LocalDate

object Params {

  def parseMonths(): Box[List[YearMonth]] =
    S.param("interval") map { _.split(";") map YearMonth.parse toList }

  def parseInterval(): Box[(Interval, LocalDate => ReadablePartial)] = tryo {
    parseMonths() flatMap {
      case start :: end :: _ => Full(between(start, end))
      case start :: _ => Full(oneMonth(start))
      case _ => Empty
    }
  } flatMap identity

  private def between(start: YearMonth, end: YearMonth): (Interval, LocalDate => ReadablePartial) =
    if (start.year == end.year && start.monthOfYear == end.monthOfYear) oneMonth(start)
    else new Interval(start.toInterval.start, end.toInterval.end) -> (d => new YearMonth(d))

  private def oneMonth(start: YearMonth): (Interval, LocalDate => ReadablePartial) =
    start.toInterval -> identity

  def thisMonth(): (Interval, LocalDate => ReadablePartial) = (YearMonth.now().toInterval, identity)

  def parseUser(): Box[User] =
    S.param("user") map (_.toLong) or (User.currentUser map (_.id.get)) flatMap User.findByKey
}
