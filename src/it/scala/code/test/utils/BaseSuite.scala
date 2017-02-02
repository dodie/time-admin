package code.test.utils

import java.util.concurrent.atomic.AtomicReference

import code.model.{Project, Task, User}
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http.{LiftSession, S}
import net.liftweb.mocks.MockHttpServletRequest
import net.liftweb.mockweb.MockWeb
import net.liftweb.util.StringHelpers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec, Matchers}

import scala.language.postfixOps

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

  def traverse(ps: project*): List[Project] = {
    def traverse_(pa: Project, ps: List[project]): List[Project] =
      ps flatMap { case project(n, b, ps) =>
        val h = Project.create.name(n).active(b).parent(pa)
        h :: traverse_(h, ps)
      }

    ps flatMap { case project(n, b, ps) =>
      val h = Project.create.name(n).active(b)
      h :: traverse_(h, ps)
    } toList
  }

  case class project(n: String, b: Boolean, ps: List[project])

  object project {
    def apply(n: String): project = project(n, true, Nil)
    def apply(n: String, b: Boolean): project = project(n, b, Nil)
    def apply(n: String, ps: List[project]): project = project(n, true, ps)
    def apply(n: String, ps: project*): project = project(n, true, ps.toList)
    def apply(n: String, b: Boolean, ps: project*): project = project(n, b, ps.toList)
  }

  def list(ts: task*): List[Task] =
    ts map { case task(n, b, p) => Task.create.name(n).parent(p).active(b) } toList

  case class task(n: String, b: Boolean, p: Project)

  object task {
    def apply(n: String, p: Project): task = task(n, true, p)
  }
}
