package io.offers

import java.time.Instant
import java.util.Date

import io.offers.fsm.OfferFSM.{OfferData, OfferState}

package object fsm {

  sealed trait OfferMessage

  case object CancelOffer extends OfferMessage

  case object QueryOffer extends OfferMessage

  case class OfferStatus(id: String, state: OfferState, data: OfferData) extends OfferMessage

  case class EnableOffer(description: String, price: Price, expiry: Date) extends OfferMessage {
    require(description.nonEmpty, "Offer description must not be empty")
    require(price.currency.nonEmpty, "Offer currency cannot be empty")
    require(price.amount > BigDecimal(0), "Offer price must be greater than zero")
    require(expiry.toInstant.isAfter(Instant.now()), "Offer expiry date must be in the future")
  }


  case object PassivateOffer extends OfferMessage

  case class OfferError(id: String, msg: String) extends OfferMessage


}
