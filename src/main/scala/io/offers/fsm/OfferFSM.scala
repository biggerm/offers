package io.offers.fsm

import java.util.Date

import akka.actor.{FSM, LoggingFSM, Props}
import io.offers.fsm.OfferFSM._
import scala.concurrent.duration._

object OfferFSM {

  sealed trait OfferState

  case object Disabled extends OfferState
  case object Enabled extends OfferState
  case object Cancelled extends OfferState
  case object Expired extends OfferState

  val stringToStatePF: PartialFunction[String, OfferState] = {
    case "DISABLED" => Disabled
    case "ENABLED" => Enabled
    case "CANCELLED" => Cancelled
    case "EXPIRED" => Expired
  }

  val stateToStringPF: PartialFunction[OfferState, String] = {
    case Disabled => "DISABLED"
    case Enabled => "ENABLED"
    case Cancelled => "CANCELLED"
    case Expired => "EXPIRED"
  }

  implicit def stringToState(s: String): OfferState = stringToStatePF(s.toUpperCase)
  implicit def stateToString(s: OfferState): String = stateToStringPF(s)

  implicit def stringToOptionState(s: String): Option[OfferState] = stringToStatePF.lift(s.toUpperCase)
  implicit def stateToOptionString(s: OfferState): Option[String] = stateToStringPF.lift(s)



  sealed trait OfferData
  case object NoOfferData extends OfferData
  case class StandardOfferData(description: String, price: Price, expiry: Date) extends OfferData

  def props(): Props = Props(new OfferFSM())

  val ExpiryTimer = "ExpiryTimer"
  case object ExpireNow

}

class OfferFSM extends FSM[OfferState, OfferData] with LoggingFSM[OfferState, OfferData] {

  val offerId: String = self.path.name
  override def logDepth = 20

  def durationTo(date: Date): FiniteDuration = {
    (date.getTime - System.currentTimeMillis()).millis
  }

  startWith(Disabled, NoOfferData)

  when(Disabled, stateTimeout = 1.minute){
    case Event(StateTimeout, NoOfferData) =>
      stop(FSM.Failure(s"Stopped OfferFSM[$offerId] No offer data received"), stateData)

    case Event(em: EnableOffer, NoOfferData) =>
      val data = StandardOfferData(em.description,em.price, em.expiry)
      setTimer(ExpiryTimer, ExpireNow, durationTo(em.expiry), false)
      goto(Enabled) using(data) replying OfferStatus(offerId, Enabled, data)

    case Event(CancelOffer, _) =>
      stay replying OfferError(offerId, "Offer does not yet exist")
  }

  when(Enabled){
    case Event(ExpireNow, _) =>
      cancelTimer(ExpiryTimer)
      goto(Expired)

    case Event(CancelOffer, _) =>
      cancelTimer(ExpiryTimer)
      goto(Cancelled) replying OfferStatus(offerId, Cancelled, stateData)
  }

  when (Cancelled){
    case Event(CancelOffer, _) =>
      cancelTimer(ExpiryTimer)
      stay replying OfferStatus(offerId, Cancelled, stateData)
  }

  when(Expired){
    case Event(CancelOffer, _) =>
      stay replying OfferError(offerId, "Cannot cancel expired offer")
  }

  whenUnhandled {
    case Event(QueryOffer, _) =>
      stay replying OfferStatus(offerId, stateName, stateData)

    case Event(m, _) =>
      stay replying(OfferError(offerId, s"Unexpected message: [$m] for Offer: ${OfferStatus(offerId, stateName, stateData)}"))

  }


  initialize()
}
