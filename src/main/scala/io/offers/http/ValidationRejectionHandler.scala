package io.offers.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.heikoseeberger.akkahttpjson4s.Json4sSupport.ShouldWritePretty
import org.json4s.{jackson, _}

object ValidationRejectionHandler extends Json4sSupport {

  def makeJsonMsg(s: String): String = {
    s.replace('\n', '|').replace('"',''').trim.filter(_ >= ' ')
  }

  case class RejectionError(error: String)

  implicit val serialization = jackson.Serialization
  implicit val formats = jackson.Serialization.formats(NoTypeHints)
  implicit val prettyPrint = ShouldWritePretty.False

  def apply(): RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case MalformedRequestContentRejection(msg, e) =>
          complete(BadRequest -> RejectionError(makeJsonMsg(s"parsing failed: $msg : ${e.getMessage}")))
      }
      .handle {
        case ValidationRejection(msg, _) =>
          complete(BadRequest -> RejectionError(makeJsonMsg(msg)))
      }
      .result()
}
