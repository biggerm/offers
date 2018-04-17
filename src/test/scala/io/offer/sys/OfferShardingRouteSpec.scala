package io.offer.sys

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone, UUID}

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.stream.ActorMaterializer
import io.offers.http._
import io.offers.sys.{OfferManager, OfferService}
import org.json4s.jackson.Serialization.writePretty
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.util.Try

class OfferShardingRouteSpec extends FlatSpec with Matchers with JsonProtocol with ScalatestRouteTest {

  val manager = new OfferManager()(system, system.dispatcher)
  val service = new OfferService()(ValidationRejectionHandler(),
                                  system.dispatcher,
                                  system: ActorSystem,
                                  materializer: ActorMaterializer,
                                  manager: OfferManager)

  implicit val timeout = RouteTestTimeout(5.seconds)

  override def testConfigSource =
    """
      |akka {
      |
      |  remote {
      |    log-remote-lifecycle-events = off
      |    netty.tcp {
      |      hostname = "127.0.0.1"
      |      port = 0
      |    }
      |  }
      |
      |  cluster {
      |    seed-nodes = []
      |  }
      |
      |  actor {
      |    provider = "cluster"
      |
      |    debug {
      |    # enable function of Actor.loggable(), which is to log any received message
      |    # at DEBUG level, see the “Testing Actor Systems” section of the Akka
      |    # Documentation at http://akka.io/docs
      |    receive = on
      |
      |    # enable DEBUG logging of all AutoReceiveMessages (Kill, PoisonPill et.c.)
      |    autoreceive = off
      |
      |    # enable DEBUG logging of actor lifecycle changes
      |    lifecycle = on
      |
      |    # enable DEBUG logging of all LoggingFSMs for events, transitions and timers
      |    fsm = on
      |
      |    # enable DEBUG logging of subscription changes on the eventStream
      |    event-stream = off
      |
      |    # enable DEBUG logging of unhandled messages
      |    unhandled = on
      |
      |    # enable WARN logging of misconfigured routers
      |    router-misconfiguration = on
      |  }
      |  }
      |}
    """.stripMargin

  val cluster = Cluster(system)
  cluster.join(cluster.selfAddress)

  val isoFormatter = {
    val UTC = TimeZone.getTimeZone("UTC")
    val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    f.setTimeZone(UTC)
    f
  }

  val offerRoutes = service.routes

  "The Offer service" should "return a new Offer for POST requests to the offer path" in {

    val json =
      s"""|{
        |  "description": "Some great new offer",
        |  "currency": "GBP",
        |  "amount": "125.50",
        |  "expiry": "2018-06-30T12:30:25Z"
        |}
      """.stripMargin
    val request = HttpEntity(ContentTypes.`application/json`, json)

    Post("/offers", request) ~> offerRoutes ~> check {

      status shouldEqual StatusCodes.Created
      val offer = responseAs[OfferStatusJS]
      assert(offer.state == "ENABLED")
      assert(offer.id.nonEmpty)

      offer.data match {
        case Some(ojs: OfferJS) =>
          assert(ojs.expiry.equals(isoFormatter.parse("2018-06-30T12:30:25Z")))
          assert(ojs.description == "Some great new offer")
          assert(ojs.currency == "GBP")
          assert(ojs.amount == "125.50")
        case None => fail("No data returned")
      }

    }
  }

  "The Offer service" should "return a new Offer that can then be cancelled" in {

    val json =
      s"""|{
          |  "description": "Some great new offer",
          |  "currency": "GBP",
          |  "amount": "125.50",
          |  "expiry": "2018-06-30T12:30:25Z"
          |}
      """.stripMargin
    val request = HttpEntity(ContentTypes.`application/json`, json)

    var uuid: UUID = null

    Post("/offers", request) ~> offerRoutes ~> check {

      status shouldEqual StatusCodes.Created
      val offer = responseAs[OfferStatusJS]
      assert(offer.state == "ENABLED")
      assert(offer.id.nonEmpty)
      uuid = Try(UUID.fromString(offer.id)).toOption match {
        case Some(id) => id
        case None => fail(s"UUID was corrupted: ${offer.id}")
      }

      offer.data match {
        case Some(ojs: OfferJS) =>
          assert(ojs.expiry.equals(isoFormatter.parse("2018-06-30T12:30:25Z")))
          assert(ojs.description == "Some great new offer")
          assert(ojs.currency == "GBP")
          assert(ojs.amount == "125.50")

        case None => fail("No data returned")
      }

    }

    Delete(s"/offers/${uuid.toString}") ~> offerRoutes ~> check {
      status shouldEqual StatusCodes.OK
      val offer = responseAs[OfferStatusJS]
      assert(offer.state == "CANCELLED")
      assert(offer.id.nonEmpty)
      Try(UUID.fromString(offer.id)).toOption match {
        case Some(id) if uuid == id => succeed
        case None => fail(s"UUID was corrupted: ${offer.id}")
      }

      offer.data match {
        case Some(ojs: OfferJS) =>
          assert(ojs.expiry.equals(isoFormatter.parse("2018-06-30T12:30:25Z")))
          assert(ojs.description == "Some great new offer")
          assert(ojs.currency == "GBP")
          assert(ojs.amount == "125.50")

        case None => fail("No data returned")
      }
    }
  }

  "The Offer service" should "return a new Offer that can then be queried, and wait for expiry" in {

    val expiryDate = new Date(System.currentTimeMillis() + 5000)
    val expireInFive = isoFormatter.format(expiryDate)

    val json =
      s"""|{
          |  "description": "Some great new offer",
          |  "currency": "GBP",
          |  "amount": "125.50",
          |  "expiry": "$expireInFive"
          |}
      """.stripMargin
    val request = HttpEntity(ContentTypes.`application/json`, json)

    var uuid: UUID = null

    Post("/offers", request) ~> offerRoutes ~> check {

      status shouldEqual StatusCodes.Created
      val offer = responseAs[OfferStatusJS]
      assert(offer.state == "ENABLED")
      assert(offer.id.nonEmpty)
      uuid = Try(UUID.fromString(offer.id)).toOption match {
        case Some(id) => id
        case None => fail(s"UUID was corrupted: ${offer.id}")
      }

      offer.data match {
        case Some(ojs: OfferJS) =>
          assert(ojs.expiry.equals(isoFormatter.parse(expireInFive)))
          assert(ojs.description == "Some great new offer")
          assert(ojs.currency == "GBP")
          assert(ojs.amount == "125.50")

        case None => fail("No data returned")
      }

    }

    Thread.sleep(6000)

    Get(s"/offers/${uuid.toString}") ~> offerRoutes ~> check {
      status shouldEqual StatusCodes.OK
      val offer = responseAs[OfferStatusJS]
      assert(offer.state == "EXPIRED")
      assert(offer.id.nonEmpty)
      Try(UUID.fromString(offer.id)).toOption match {
        case Some(id) if uuid == id => succeed
        case None => fail(s"UUID was corrupted: ${offer.id}")
      }

      offer.data match {
        case Some(ojs: OfferJS) =>
          assert(ojs.expiry.equals(isoFormatter.parse(expireInFive)))
          assert(ojs.description == "Some great new offer")
          assert(ojs.currency == "GBP")
          assert(ojs.amount == "125.50")

        case None => fail("No data returned")
      }
    }


  }

  "The Offer service" should "return a new Offer and list all the offers" in {

    val expiryDate = new Date(System.currentTimeMillis() + 5000)
    val expireInFive = isoFormatter.format(expiryDate)

    val json =
      s"""|{
          |  "description": "Some great new offer",
          |  "currency": "GBP",
          |  "amount": "125.50",
          |  "expiry": "$expireInFive"
          |}
      """.stripMargin
    val request = HttpEntity(ContentTypes.`application/json`, json)

    var uuid: UUID = null

    Post("/offers", request) ~> offerRoutes ~> check {

      status shouldEqual StatusCodes.Created
      val offer = responseAs[OfferStatusJS]
      assert(offer.state == "ENABLED")
      assert(offer.id.nonEmpty)
      uuid = Try(UUID.fromString(offer.id)).toOption match {
        case Some(id) => id
        case None => fail(s"UUID was corrupted: ${offer.id}")
      }

      offer.data match {
        case Some(ojs: OfferJS) =>
          assert(ojs.expiry.equals(isoFormatter.parse(expireInFive)))
          assert(ojs.description == "Some great new offer")
          assert(ojs.currency == "GBP")
          assert(ojs.amount == "125.50")

        case None => fail("No data returned")
      }

    }

    Get(s"/offers") ~> offerRoutes ~> check {
      status shouldEqual StatusCodes.OK
      val offerIds = responseAs[List[OfferIdJS]]
      assert(offerIds.size == 4)
      println("OfferIds:\n" + writePretty(offerIds))
    }


  }

}


