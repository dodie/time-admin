package code
package model

import net.liftweb.mapper._
import code.model.mixin.HierarchicalItem

object Task extends Task with LongKeyedMetaMapper[Task]

class Task extends LongKeyedMapper[Task] with HierarchicalItem[Task] {
  def getSingleton = Task
}
