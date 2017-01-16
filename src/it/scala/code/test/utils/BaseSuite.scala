package code.test.utils

import java.util.concurrent.atomic.AtomicReference
import javax.servlet.http.HttpServletRequest

import code.model.User
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http.{LiftSession, Req, S}
import net.liftweb.mocks.MockHttpServletRequest
import net.liftweb.mockweb.MockWeb
import net.liftweb.util.StringHelpers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec, Matchers}

/**
  * Created by suliatis on 1/12/17.
  */
trait BaseSuite extends FunSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {
  private val session : LiftSession = new LiftSession("", StringHelpers.randomString(20), Empty)
  private val givenFunctionAtomic = new AtomicReference[Option[() => Any]](None)

  override def beforeAll(): Unit = {
    Db.init
  }

  before {
    Db.clear
    givenFunctionAtomic.get match {
      case Some(f) => f()
      case None =>
    }
  }

  protected def given(f: => Any): Unit = {
    givenFunctionAtomic.compareAndSet(None, Some(() => f))
  }

  after {
    Db.clear
  }

  def withS(execution: => Any): Unit = S.initIfUninitted(session) { execution }

  def withS(req: Box[MockHttpServletRequest] = Empty, user: Box[User])(execution: => Any): Any =
    req map { r => MockWeb.testS(r, Full(session)) {
      user.foreach(User.logUserIn)
      execution
    }} getOrElse { S.initIfUninitted(session) {
      user.foreach(User.logUserIn)
      execution
    }
  }

  def get(uri: String): Box[MockHttpServletRequest] = Full(new MockHttpServletRequest(uri, ""))
}
