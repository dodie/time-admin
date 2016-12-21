package code.service

import code.model.mixin.HierarchicalItem
import net.liftweb.common.Box

object HierarchicalItemService {
  def path(z: List[HierarchicalItem[_]], pid: Box[Long], ps: List[HierarchicalItem[_]]): List[HierarchicalItem[_]] =
    (for {
      id <- pid
      p <- ps find (_.id.get == id)
    } yield path(p :: z, p.parent.box, ps)) getOrElse z
}
