package code.snippet

import code.model.User
import net.liftweb.common._
import net.liftweb.http.{LiftSession, S}
import net.liftweb.util._
import org.scalatest.{FlatSpec, _}

class UsersSnippetSpec extends FlatSpec with Matchers {

  val session: LiftSession = new LiftSession("", StringHelpers.randomString(20), Empty)
  val testUserName = StringHelpers.randomString(10)

  override def withFixture(test: NoArgTest) = {
    import bootstrap.liftweb._
    val boot = new Boot
    boot.boot

    S.initIfUninitted(session) {
      val user: User = User.create
      user.firstName(testUserName)
      user.lastName(testUserName)
      user.save
      User.logUserIn(user)
      test()
    }
  }

  "Users snippet" must "render current user nice name" in {
    val xml = <div class="lift:usersSnippet.loginBox">
                Hi
                <span class="ActualUserName"></span>
                <span class="ActualUserEmail"></span>
                !
              </div>

    val snippet = new UsersSnippet()
    val output = snippet.currentUser(xml)

    val actualUserNode = output \ "span" filter (n => (n \ "@class").text == "ActualUserName")
    actualUserNode.text should include(testUserName)
  }

}

