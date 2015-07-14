package code
package model

import net.liftweb.mapper._

object UserRoles extends UserRoles with LongKeyedMetaMapper[UserRoles]

class UserRoles extends LongKeyedMapper[UserRoles] with IdPK {
  def getSingleton = UserRoles
  object user extends MappedLongForeignKey(this, User)
  object role extends MappedLongForeignKey(this, Role)
}