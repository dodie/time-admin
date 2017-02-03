package code
package model

import net.liftweb.mapper._

object Task extends Task with LongKeyedMetaMapper[Task]

class Task extends LongKeyedMapper[Task] {
  def getSingleton = Task
  def primaryKeyField = id
  object id extends MappedLongIndex[Task](this)
  object parent extends MappedLongForeignKey[Task, Task](this, Task)
  object name extends MappedString[Task](this, 140)
  object description extends MappedString[Task](this, 300)
  object active extends MappedBoolean[Task](this)
  object color extends MappedString[Task](this, 140)
  object specifiable extends MappedBoolean[Task](this)
  object selectable extends MappedBoolean[Task](this)
}
