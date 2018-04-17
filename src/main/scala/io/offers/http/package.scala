package io.offers

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

import io.offers.fsm.OfferFSM._
import io.offers.fsm.{OfferFSM, OfferStatus}

package object http {

  case class OfferIdJS(shardId: String, entityId: String)

  case class OfferJS(description:String, currency:String, amount:String, expiry: Date)

  case class PatchOfferJS(state: String)

  object OfferStatusJS{
    def apply(os: OfferStatus): OfferStatusJS = {
      os.data match {
        case sd: StandardOfferData =>
          OfferStatusJS(os.id, OfferFSM.stateToString(os.state),
            Some(OfferJS(sd.description,
                          sd.price.currency,
                          sd.price.amount.toString(),
                          sd.expiry)))

        case NoOfferData => OfferStatusJS(os.id, OfferFSM.stateToString(os.state),None)
      }
    }
  }

  case class OfferStatusJS( id: String, state: String, data: Option[OfferJS])
}
