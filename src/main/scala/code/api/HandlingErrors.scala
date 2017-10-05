package code.api

import net.liftweb.common._
import net.liftweb.http.LiftRules._
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.{ S, JsonResponse, LiftResponse, Req }
import net.liftweb.json.JsonDSL._

import code.model.User

object HandlingErrors {

  class HandlingErrorsDispatchPF(handler: DispatchPF)(errorHandler: (Req, Throwable) => () => Box[LiftResponse]) extends DispatchPF {

    def isDefinedAt(req: Req): Boolean = handler.isDefinedAt(req)

    def apply(req: Req): () => Box[LiftResponse] = handlingErrors(req)

    def handlingErrors(req: Req, now: Long = System.currentTimeMillis): () => Box[LiftResponse] = {
      try {
        handler(req)
      } catch {
        case t : Throwable => errorHandler(req, t)
      }
    }
    
  }

}

trait HandlingErrors extends Loggable {

  self: RestHelper =>

  import HandlingErrors._

  def errorHandler(req: Req, t: Throwable): () => Box[LiftResponse] = {
    logger.error(s"API failure for path: ${req.path.wholePath.mkString("/")}", t)
    JsonResponse(
      ("error" -> "Unknown error."),
      500)
  }

  def handlingErrors(handler: DispatchPF): DispatchPF = {
    new HandlingErrorsDispatchPF(handler)(errorHandler)
  }

}
