package code.snippet


import code.model.User
import com.github.nscala_time.time.Imports._
import net.liftweb.common.Box
import net.liftweb.http.S
import net.liftweb.util.ControlHelpers.tryo
import org.joda.time.ReadablePartial
import org.joda.time.LocalDate

object Params {

  def parseInterval(s: S): Box[(Interval, LocalDate => ReadablePartial)] = tryo {
    for {
      start <- s.param("intervalStart").map(d => YearMonth.parse(d))
      end <- s.param("intervalEnd").map(d => YearMonth.parse(d))
    } yield between(start, end)
  } flatMap identity

  private def between(start: YearMonth, end: YearMonth): (Interval, LocalDate => ReadablePartial) =
    if (start.year == end.year && start.monthOfYear == end.monthOfYear) (start.toInterval, identity)
    else (new Interval(start.toInterval.start, end.toInterval.end), d => new YearMonth(d))

  def thisMonth(): (Interval, LocalDate => ReadablePartial) = (YearMonth.now().toInterval, identity)

  def parseUser(s: S): Box[User] = S.param("user").map(_.toLong).flatMap(User.findByKey)
}
