package code
package model

import net.liftweb.mapper._

object TaskItem extends TaskItem with LongKeyedMetaMapper[TaskItem]

class TaskItem extends LongKeyedMapper[TaskItem] {
  def getSingleton = TaskItem

  def primaryKeyField = id
  object id extends MappedLongIndex(this)
  object task extends MappedLongForeignKey(this, Task)
  object user extends MappedLongForeignKey(this, User)
  object start extends MappedLong(this)
}
