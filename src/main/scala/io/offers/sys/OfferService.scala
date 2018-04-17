package io.offers.sys

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import io.offers.fsm.OfferStatus
import io.offers.http.{OfferIdJS, OfferJS, OfferStatusJS, PatchOfferJS}
import io.offers.http.JsonProtocol._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class OfferService(implicit handler: RejectionHandler,
                   ec: ExecutionContext,
                   system: ActorSystem,
                   materializer: ActorMaterializer,
                   offerManager: OfferManager) {

  val offersPath = "offers"

  val routes =
    pathPrefix(offersPath){
      handleRejections(handler) {
        pathEndOrSingleSlash {
          get {
            onComplete(offerManager.listOffers()) {
              case Success(Right(ol: List[OfferIdJS])) => complete(OK -> ol)
              case Success(Left(msg: String)) => complete(PreconditionFailed -> msg)
              case Failure(ex) => complete(InternalServerError -> ex.getMessage)
            }
          } ~
          post {
            entity(as[OfferJS]) { offer =>
              onComplete(offerManager.createOffer(offer, UUID.randomUUID())) {
                case Success(Right(os: OfferStatus)) => complete(Created -> OfferStatusJS(os))
                case Success(Left(msg: String)) => complete(PreconditionFailed -> msg)
                case Failure(ex) => complete(InternalServerError -> ex.getMessage)
              }
            }

          }
        } ~
        path(JavaUUID) { uuid =>
          get {
            onComplete(offerManager.queryOffer(uuid)) {
              case Success(Right(os: OfferStatus)) => complete(OK -> OfferStatusJS(os))
              case Success(Left(msg: String)) => complete(PreconditionFailed -> msg)
              case Failure(ex) => complete(InternalServerError -> ex.getMessage)
            }
          } ~
          delete {
            onComplete(offerManager.cancelOffer(uuid)) {
              case Success(Right(os: OfferStatus)) => complete(OK -> OfferStatusJS(os))
              case Success(Left(msg: String)) => complete(PreconditionFailed -> msg)
              case Failure(ex) => complete(InternalServerError -> ex.getMessage)
            }
          }
        }
      }
    }
}
