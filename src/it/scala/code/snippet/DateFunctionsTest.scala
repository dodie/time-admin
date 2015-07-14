package code.snippet

import code.snippet.mixin.DateFunctions
import net.liftweb.common.{ Empty, Full }
import net.liftweb.http.S
import net.liftweb.mockweb.WebSpec

class DateFunctionsTest extends WebSpec with DateFunctions {
  val anyUrl = "http://foo.com/test/this"

  "Get offset from days" withSFor (anyUrl + "?offset=-1") in {
    (offsetInDays must_== -1) and (S.getSessionAttribute("offset") must_!= Full(-1))
  }
  "Get non negative offset from days" withSFor (anyUrl + "?offset=1") in {
    (offsetInDays must_== 0) and (S.getSessionAttribute("offset") must_!= Full(1))
  }

  val past = "2015-06-17"
  val future = "2025-06-17"

  "Get offset from iso date" withSFor (anyUrl + "?date=" + past) in {
    (offsetInDays must lessThan(0)) and (S.getSessionAttribute("offset") must_!= Empty)
  }
  "Get offset from iso date" withSFor (anyUrl + "?date=" + future) in {
    (offsetInDays must equalTo(0)) and (S.getSessionAttribute("offset") must_!= Empty)
  }

  "Get offset from days session attribute" withSFor (anyUrl) in {
    S.setSessionAttribute("offset", -1.toString)
    offsetInDays must_== -1
  }
  "Get non negative offset from days session attribute" withSFor (anyUrl) in {
    S.setSessionAttribute("offset", 1.toString)
    offsetInDays must_== 0
  }

}
