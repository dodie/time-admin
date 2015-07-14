package code
package model

import net.liftweb.mapper._

object UserPreference extends UserPreference with LongKeyedMetaMapper[UserPreference]

class UserPreference extends LongKeyedMapper[UserPreference] {
  def getSingleton = UserPreference

  def primaryKeyField = id
  object id extends MappedLongIndex(this)
  object user extends MappedLongForeignKey(this, User)
  object key extends MappedString(this, 300)
  object value extends MappedString(this, 300)
}

