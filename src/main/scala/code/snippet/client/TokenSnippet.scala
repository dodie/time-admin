package code
package snippet

import scala.xml.NodeSeq

import net.liftweb.util.Helpers.StringToCssBindPromoter
import code.model.ExtSession
import code.model.User
import net.liftweb.mapper.By
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.Reload

class TokenSnippet {
  
	private def user = User.currentUser.openOrThrowException("Current user must be defined!")

  def list(in: NodeSeq): NodeSeq = {
    val sessions = ExtSession.findAll(By(ExtSession.userId, user.userIdAsString))
    
    ".token" #> sessions.map { session =>
      ".token-cookieId *" #> session.cookieId.get &
      ".token-expiration *" #> session.expiration.get &
      ".token-remove [onClick]" #> SHtml.onEvent(s => {
        session.delete_!
        Reload
      })
    } apply in
    
  }
  
  def add(in: NodeSeq): NodeSeq = {
    "input [onClick]" #> SHtml.onEvent(s => {
      ExtSession.create.userId(user.userIdAsString).saveMe
      Reload
    }) apply in
  }

}
