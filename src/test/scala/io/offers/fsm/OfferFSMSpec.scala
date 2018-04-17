package io.offers.fsm

import java.time.Instant
import java.util.Date

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestFSMRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.offers.fsm.OfferFSM.{Cancelled, Disabled, Enabled, Expired, NoOfferData, StandardOfferData}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class OfferFSMSpec extends TestKit(ActorSystem("OFFER"))
  with FlatSpecLike
  with Matchers
  with MockFactory
  with BeforeAndAfterAll {

  val config = ConfigFactory.parseString(
    """
      |
      |akka.actor.debug {
      |  # enable function of Actor.loggable(), which is to log any received message
      |  # at DEBUG level, see the “Testing Actor Systems” section of the Akka
      |  # Documentation at http://akka.io/docs
      |  receive = off
      |
      |  # enable DEBUG logging of all AutoReceiveMessages (Kill, PoisonPill etc.)
      |  autoreceive = on
      |
      |  # enable DEBUG logging of actor lifecycle changes
      |  lifecycle = on
      |
      |  # enable DEBUG logging of all LoggingFSMs for events, transitions and timers
      |  fsm = on
      |
      |  # enable DEBUG logging of subscription changes on the eventStream
      |  event-stream = off
      |
      |  # enable DEBUG logging of unhandled messages
      |  unhandled = on
      |
      |  # enable WARN logging of misconfigured routers
      |  router-misconfiguration = off
      |}
    """.stripMargin)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  def expireIn(duration: FiniteDuration): Date = {
    val millis = duration.toMillis
    new Date(System.currentTimeMillis() + millis)
  }

  "OfferFSM" should "Initialise to a new offer with no data" in {
    val fsm = TestFSMRef(new OfferFSM())

    val fsmRef: TestActorRef[OfferFSM] = fsm

    awaitCond(fsm.stateName == Disabled, 5.seconds, 200.millis)
    assert(fsm.stateData == NoOfferData)
  }

  "OfferFSM" should "Initialise to a new offer and return status on query" in {
    val fsm = TestFSMRef(new OfferFSM())

    val fsmRef: TestActorRef[OfferFSM] = fsm

    val probe = TestProbe()
    probe.send(fsmRef, QueryOffer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Disabled, NoOfferData) => m
    }

    awaitCond(fsm.stateName == Disabled, 5.seconds, 200.millis)
    assert(fsm.stateData == NoOfferData)
  }

  "OfferFSM" should "Initialise to a new offer with data and return status on query" in {
    val fsm = TestFSMRef(new OfferFSM())

    val fsmRef: TestActorRef[OfferFSM] = fsm

    val probe = TestProbe()
    probe.send(fsmRef, QueryOffer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Disabled, NoOfferData) => m
    }

    awaitCond(fsm.stateName == Disabled, 5.seconds, 200.millis)
    assert(fsm.stateData == NoOfferData)

    val offer = EnableOffer("A great new offer", Price("GBP", "12.50"), expireIn(5.seconds))
    probe.send(fsmRef, offer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Enabled, data: StandardOfferData) =>
        assert(data.description == offer.description)
        assert(data.price == offer.price)
        assert(data.expiry == offer.expiry)
        m
    }

    awaitCond(fsm.stateName == Enabled, 5.seconds, 200.millis)
    fsm.stateData match {
      case data: StandardOfferData =>
        assert(data.description == offer.description)
        assert(data.price == offer.price)
        assert(data.expiry == offer.expiry)
      case x => fail(s"unexpected data in fsm: ${x}")
    }
    assert(fsmRef.underlyingActor.isTimerActive(OfferFSM.ExpiryTimer))

  }

  "OfferFSM" should "Initialise to a new offer with data and cancel correctly" in {
    val fsm = TestFSMRef(new OfferFSM())

    val fsmRef: TestActorRef[OfferFSM] = fsm

    val probe = TestProbe()
    probe.send(fsmRef, QueryOffer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Disabled, NoOfferData) => m
    }

    awaitCond(fsm.stateName == Disabled, 5.seconds, 200.millis)
    assert(fsm.stateData == NoOfferData)

    val offer = EnableOffer("A great new offer", Price("GBP", "12.50"), expireIn(5.seconds))

    probe.send(fsmRef, offer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Enabled, data: StandardOfferData) =>
        assert(data.description == offer.description)
        assert(data.price == offer.price)
        assert(data.expiry == offer.expiry)
        m
    }

    awaitCond(fsm.stateName == Enabled, 5.seconds, 200.millis)
    fsm.stateData match {
      case data: StandardOfferData =>
        assert(data.description == offer.description)
        assert(data.price == offer.price)
        assert(data.expiry == offer.expiry)
      case x => fail(s"unexpected data in fsm: ${x}")
    }
    assert(fsmRef.underlyingActor.isTimerActive(OfferFSM.ExpiryTimer))

    probe.send(fsmRef, CancelOffer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Cancelled, data: StandardOfferData) =>
        assert(data.description == offer.description)
        assert(data.price == offer.price)
        assert(data.expiry == offer.expiry)
        m
    }
    awaitCond(fsm.stateName == Cancelled, 5.seconds, 200.millis)
    assert(fsmRef.underlyingActor.isTimerActive(OfferFSM.ExpiryTimer) == false)


  }

  "OfferFSM" should "Initialise to a new offer and expire correctly" in {
    val fsm = TestFSMRef(new OfferFSM())

    val fsmRef: TestActorRef[OfferFSM] = fsm

    val probe = TestProbe()
    probe.send(fsmRef, QueryOffer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Disabled, NoOfferData) => m
    }

    awaitCond(fsm.stateName == Disabled, 5.seconds, 200.millis)
    assert(fsm.stateData == NoOfferData)

    val offer = EnableOffer("A great new offer", Price("GBP", "12.50"), expireIn(5.seconds))

    probe.send(fsmRef, offer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Enabled, data: StandardOfferData) =>
        assert(data.description == offer.description)
        assert(data.price == offer.price)
        assert(data.expiry == offer.expiry)
        m
    }

    awaitCond(fsm.stateName == Enabled, 5.seconds, 200.millis)
    fsm.stateData match {
      case data: StandardOfferData =>
        assert(data.description == offer.description)
        assert(data.price == offer.price)
        assert(data.expiry == offer.expiry)
      case x => fail(s"unexpected data in fsm: ${x}")
    }
    assert(fsmRef.underlyingActor.isTimerActive(OfferFSM.ExpiryTimer))

    awaitCond(fsm.stateName == Expired, 7.seconds, 200.millis)

  }

  "OfferFSM" should "Initialise with no offer and fail to cancel" in {
    val fsm = TestFSMRef(new OfferFSM())

    val fsmRef: TestActorRef[OfferFSM] = fsm

    val probe = TestProbe()
    probe.send(fsmRef, QueryOffer)
    probe.expectMsgPF[OfferStatus](5.seconds, "OfferStatus") {
      case m @ OfferStatus(_, Disabled, NoOfferData) => m
    }

    awaitCond(fsm.stateName == Disabled, 5.seconds, 200.millis)
    assert(fsm.stateData == NoOfferData)

    probe.send(fsmRef, CancelOffer)
    probe.expectMsgPF[OfferError](5.seconds, "OfferError") {
      case m @ OfferError(id, msg) =>
        m
    }
    awaitCond(fsm.stateName == Disabled, 5.seconds, 200.millis)
    assert(fsmRef.underlyingActor.isTimerActive(OfferFSM.ExpiryTimer) == false)


  }
}
