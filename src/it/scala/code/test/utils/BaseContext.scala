package code.test.utils

import java.util.concurrent.atomic.AtomicReference

import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

/**
  * Created by suliatis on 1/12/17.
  */
trait BaseContext extends FunSpec with Matchers with BeforeAndAfter {
  private val givenFunctionAtomic = new AtomicReference[Option[() => Any]](None)

  before {
    println("before")
    Db.init
    givenFunctionAtomic.get match {
      case Some(f) => f()
      case None =>
    }
  }

  protected def given(f: => Any): Unit = {
    givenFunctionAtomic.compareAndSet(None, Some(() => f))
  }

  after {
    println("after")
    Db.clear
  }
}
