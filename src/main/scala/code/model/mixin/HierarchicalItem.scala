package code
package model
package mixin

import net.liftweb.mapper._

trait HierarchicalItem[T <: Mapper[T]] {
  self: T =>
  def primaryKeyField = id
  object id extends MappedLongIndex[T](this)
  object parent extends MappedLongForeignKey[T, Project](this, Project)
  object name extends MappedString[T](this, 140)
  object description extends MappedString[T](this, 300)
  object active extends MappedBoolean[T](this)
  object color extends MappedString[T](this, 140)
}
