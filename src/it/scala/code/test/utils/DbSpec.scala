package code.test.utils

import org.specs2.mutable.BeforeAfter

@deprecated(message = "Move to scalatest and BaseSuite where possible.")
trait DbSpec extends BeforeAfter {
  override def before: Unit = {
    Db.init
  }

  override def after: Unit = {
    Db.clear
  }
}
