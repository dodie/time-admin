package code
package model

import net.liftweb.mapper._

object Role extends Role with LongKeyedMetaMapper[Role]

class Role extends LongKeyedMapper[Role] with ManyToMany {
  def getSingleton = Role

  def primaryKeyField = id
  object id extends MappedLongIndex(this)
  object name extends MappedString(this, 140)
  object users extends MappedManyToMany(UserRoles, UserRoles.role, UserRoles.user, User)
}
