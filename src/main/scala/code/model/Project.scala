package code
package model

import net.liftweb.mapper._
import code.model.mixin.HierarchicalItem

object Project extends Project with LongKeyedMetaMapper[Project]

class Project extends LongKeyedMapper[Project] with HierarchicalItem[Project] {
  def getSingleton = Project
}
