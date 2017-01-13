package code.test.utils

import java.util.concurrent.atomic.AtomicReference

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec, Matchers}

/**
  * Created by suliatis on 1/12/17.
  */
trait BaseSuite extends FunSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {
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
}
