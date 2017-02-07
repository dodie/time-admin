package code.test.utils

import java.util.concurrent.atomic.AtomicReference

import code.model.{Task, User}
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

  def traverse(ps: entity*): List[Task] = {
    def traverse_(pa: Task, ps: List[entity]): List[Task] =
      ps flatMap { t =>
        val h = t.asEntity.parent(pa)
        h :: traverse_(h, t.children)
      }

    ps flatMap { t =>
      val h = t.asEntity
      h :: traverse_(h, t.children)
    } toList
  }

  abstract class entity(name: String, active: Boolean, ts: List[entity]) {
    def children: List[entity] = ts

    def asEntity: Task
  }

  case class project(name: String, active: Boolean, ts: List[entity]) extends entity(name, active, ts) {
    def asEntity: Task = Task.create.name(name).active(active).selectable(false)
  }

  object project {
    def apply(name: String): project = project(name, active = true, Nil)
    def apply(name: String, active: Boolean): project = project(name, active, Nil)
    def apply(name: String, ts: List[entity]): project = project(name, active = true, ts)
    def apply(name: String, ts: entity*): project = project(name, active = true, ts.toList)
    def apply(name: String, active: Boolean, ts: entity*): project = project(name, active, ts.toList)
  }

  case class task(name: String, active: Boolean, ts: List[entity]) extends entity(name, active, ts) {
    def asEntity: Task = Task.create.name(name).active(active).selectable(true)
  }

  object task {
    def apply(name: String): task = task(name, active = true, Nil)
    def apply(name: String, active: Boolean): task = task(name, active, Nil)
    def apply(name: String, ts: List[entity]): task = task(name, active = true, ts)
    def apply(name: String, ts: entity*): task = task(name, active = true, ts.toList)
    def apply(name: String, active: Boolean, ts: entity*): task = task(name, active, ts.toList)
  }
}
