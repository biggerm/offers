package io.offers.sys

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.{RejectionHandler, Route, RouteResult}
import akka.http.scaladsl.server.RouteResult._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import io.offers.http.ValidationRejectionHandler
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings.RoutingSettings
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}

object OfferSystem extends StrictLogging {

  def main(args: Array[String]): Unit = {
    implicit val config = ConfigFactory.load()
    implicit val system = ActorSystem("OfferSystem")
    implicit val cluster = Cluster(system)
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val handler: RejectionHandler = ValidationRejectionHandler()
    implicit val materializer = ActorMaterializer()
    val interface = "0.0.0.0"
    val port = 8080

    cluster.join(cluster.selfAddress)

    cluster.registerOnMemberUp {
      implicit val manager = new OfferManager()
      val service = new OfferService()
      val routes = service.routes

      val bindingFuture: Future[ServerBinding] =
        Http(system).bindAndHandle(routes, interface, port)

      bindingFuture.failed.foreach { ex =>
        logger.error(s"Failed to bind to $interface:$port", ex)
      }
    }
  }
}
