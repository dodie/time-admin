package code.test.utils

import code.model._
import net.liftweb.common._
import net.liftweb.db.StandardDBVendor
import net.liftweb.http.LiftRules
import net.liftweb.mapper
import net.liftweb.mapper.{DB, Schemifier}
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification._

trait DbSpec extends BeforeAfter {
  override def before: Unit = {
    Db.init

    given
  }

  def given: Unit = {}

  override def after: Unit = {
    Db.clear
  }
}

trait DbSpecEach extends BeforeAfterEach {
  override def before: Unit = {
    Db.init

    given
  }

  def given: Unit = {}

  override def after: Unit = {
    Db.clear
  }
}

trait DbAroundEach extends AroundEach {

  override def around[R: AsResult](r: => R): Result = {
    Db.init
    given
    try AsResult(r)
    finally Db.clear
  }

  def given: Unit = {}
}

trait DbSpecification extends Specification {
  override def map(fs: => Fragments): Fragments = step(Db.init) ^ step(given) ^ fs ^ step(Db.clear)

  def given: Unit = {}
}

object Db {

  def init: Unit = {

    val vendor = new StandardDBVendor(
      "org.h2.Driver",
      "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
      Empty, Empty
    )
    LiftRules.unloadHooks.append(vendor.closeAllConnections_!)
    DB.defineConnectionManager(mapper.DefaultConnectionIdentifier, vendor)

    Schemifier.schemify(true, Schemifier.infoF _, User)
    Schemifier.schemify(true, Schemifier.infoF _, Project)
    Schemifier.schemify(true, Schemifier.infoF _, Role)
    Schemifier.schemify(true, Schemifier.infoF _, Task)
    Schemifier.schemify(true, Schemifier.infoF _, TaskItem)
    Schemifier.schemify(true, Schemifier.infoF _, UserPreference)
    Schemifier.schemify(true, Schemifier.infoF _, UserRoles)
    Schemifier.schemify(true, Schemifier.infoF _, ExtSession)
  }

  def clear: Unit = {
    User.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
    Project.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
    Role.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
    Task.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
    TaskItem.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
    UserPreference.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
    UserRoles.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
    ExtSession.bulkDelete_!!(mapper.DefaultConnectionIdentifier)
  }
}
