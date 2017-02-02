package code.service

import code.model.Task
import net.liftweb.common.Box

object HierarchicalItemService {
  def path(z: List[Task], pid: Box[Long], ps: List[Task]): List[Task] =
    (for {
      id <- pid
      p <- ps find (_.id.get == id)
    } yield path(p :: z, p.parent.box, ps)) getOrElse z
}
