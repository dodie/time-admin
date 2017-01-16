package code.snippet

import net.liftweb.common._
import net.liftweb.http.{LiftSession, S}
import net.liftweb.util._
import org.scalatest.{FlatSpec, _}

class HelpSpec extends FlatSpec with Matchers {

  val session: LiftSession = new LiftSession("", StringHelpers.randomString(20), Empty)

  override def withFixture(test: NoArgTest) = {

    S.initIfUninitted(session) {
      test()
    }
  }

  "Help snippet" must "produce a QuickHelp" in {
    val xml = <anything>help.localization.key</anything>
    val snippet = new Help()
    val output = snippet.quickHelp(xml)

    output should not be null
    (output \ "@class").toString should be("QuickHelp")
    (output \ "@data-hint").toString should not have length(0)
  }

}

