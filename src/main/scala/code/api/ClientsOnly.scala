package code.api

import net.liftweb.common._
import net.liftweb.http.LiftRules._
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.{ S, JsonResponse, LiftResponse, Req }
import net.liftweb.json.JsonDSL._

import code.model.User

object ClientsOnly {

  class ClientsOnlyDispatchPF(handler: DispatchPF)(errorHandler: (Req) => () => Box[LiftResponse]) extends DispatchPF {

    def isDefinedAt(req: Req): Boolean = handler.isDefinedAt(req)

    def apply(req: Req): () => Box[LiftResponse] = clientsOnly(req)

    def clientsOnly(req: Req, now: Long = System.currentTimeMillis): () => Box[LiftResponse] = {
      if (User.currentUser.isEmpty) {
        errorHandler(req)
      } else {
        handler(req)
      }
    }
    
  }

}

trait ClientsOnly extends Loggable {

  self: RestHelper =>

  import ClientsOnly._

  def errorHandler(req: Req): () => Box[LiftResponse] = {
    logger.info(s"API authentication failure for path: ${req.path.wholePath.mkString("/")}")
    JsonResponse(
      ("error" -> "Authentication failure."),
      403)
  }

  def clientsOnly(handler: DispatchPF): DispatchPF = {
    new ClientsOnlyDispatchPF(handler)(errorHandler)
  }

}