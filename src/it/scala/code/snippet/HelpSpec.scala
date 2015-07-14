package code.snippet

import code.model.User
import net.liftweb.http.{ S, LiftSession }
import net.liftweb.mapper.BaseMetaMapper
import net.liftweb.util._
import net.liftweb.common._
import org.scalatest.FlatSpec
import org.scalatest._

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

