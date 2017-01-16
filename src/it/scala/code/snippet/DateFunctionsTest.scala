package code.snippet

import code.snippet.mixin.DateFunctions
import code.test.utils.BaseSuite
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.S

class DateFunctionsTest extends BaseSuite with DateFunctions {
  val anyUrl = "http://foo.com/test/this"

  it("Get offset from days") { withS(get(anyUrl + "?offset=-1"), Empty) {
    offsetInDays shouldBe -1
    S.getSessionAttribute("offset") shouldBe Full("-1")
  }}

  it("Get non negative offset from days") { withS(get(anyUrl + "?offset=1"), Empty) {
    offsetInDays shouldBe 0
    S.getSessionAttribute("offset") shouldBe Full("1")

  }}

  val past = "2015-06-17"
  val future = "2025-06-17"

  it("Get offset from iso date (past)") { withS(get(anyUrl + "?date=" + past), Empty) {
    offsetInDays should be < 0
    S.getSessionAttribute("offset") shouldBe a [Full[_]]
  }}

  it("Get offset from iso date (future)") { withS(get(anyUrl + "?date=" + future), Empty) {
    offsetInDays shouldBe 0
    S.getSessionAttribute("offset") shouldBe a [Full[_]]

  }}

  it("Get offset from days session attribute") { withS (get(anyUrl), Empty) {
    S.setSessionAttribute("offset", "-1")
    offsetInDays shouldBe -1
  }}

  it("Get non negative offset from days session attribute") { withS (get(anyUrl), Empty) {
    S.setSessionAttribute("offset", "1")
    offsetInDays shouldBe 0
  }}
}
