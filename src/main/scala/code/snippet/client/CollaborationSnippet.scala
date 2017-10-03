package code
package snippet

import scala.xml.NodeSeq
import net.liftweb.util.Helpers.StringToCssBindPromoter
import code.model.User
import code.service.ReportService

class CollaborationSnippet {
  
	private def user = User.currentUser.openOrThrowException("Current user must be defined!")

  def list(in: NodeSeq): NodeSeq = {
    val collaborators = ReportService.getCollaborators(user)
    
    ".collaborator" #> collaborators.map { collaborator =>
      ".collaborator-name *" #> collaborator._1.niceName &
      ".collaboration-tasks *" #> collaborator._2.map { task => 
        ".name *" #> task._1.name &
        ".duration *" #> task._2.plus(task._3).toStandardMinutes().getMinutes
      }
    } apply in
    
  }

}
