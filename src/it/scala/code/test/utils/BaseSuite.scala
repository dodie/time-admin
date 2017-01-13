package code.test.utils

import java.util.concurrent.atomic.AtomicReference

import code.model.User
import net.liftweb.common.{Box, Empty}
import net.liftweb.http.{LiftSession, S}
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

  def withS(test: => Any): Unit = {
    S.initIfUninitted(session) {
      test
    }
  }

  def withS(u: Box[User] = Empty)(test: => Any): Unit = {
    S.initIfUninitted(session) {
      u.foreach(User.logUserIn)
      test
    }
  }
}
