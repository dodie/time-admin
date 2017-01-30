package code.snippet

import code.model.User
import code.service.TaskItemService.IntervalQuery
import com.github.nscala_time.time.Imports._
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http.S
import net.liftweb.util.ControlHelpers.tryo

import scala.language.postfixOps

object Params {

  def parseMonths(): Box[List[YearMonth]] =
    S.param("interval") map { _.split(";") map YearMonth.parse toList }

  def parseInterval: Box[IntervalQuery] = tryo {
    parseMonths() flatMap {
      case start :: end :: _ => Full(IntervalQuery.between(start, end))
      case start :: _ => Full(IntervalQuery.oneMonth(start))
      case _ => Empty
    }
  } flatMap identity

  def parseUser(): Box[User] =
    S.param("user") map (_.toLong) or (User.currentUser map (_.id.get)) flatMap User.findByKey
}
