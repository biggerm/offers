package io.offers.sys

import java.util.{Date, UUID}

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.ShardRegion.{CurrentShardRegionState, GetShardRegionState, ShardState}
import akka.pattern._
import akka.util.Timeout
import io.offers.fsm._
import io.offers.http.{OfferIdJS, OfferJS}
import io.offers.sys.OfferSharding.OfferShardMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._


class OfferManager(implicit system: ActorSystem, ec: ExecutionContext) {

  val offerSharding: ActorRef = OfferSharding.init(system)
  implicit val timeout = Timeout(5 seconds)

  /**
    * Create an Offer on the system
    * @param details
    * @param uuid
    * @return
    */
  def createOffer(details: OfferJS, uuid: UUID): Future[Either[String, OfferStatus]] = {
    val offer = EnableOffer(
      details.description,
      Price(details.currency, details.amount),
      details.expiry)

    offerSharding.ask(OfferShardMessage(uuid.toString, offer)).map {
      case os: OfferStatus => Right(os)
      case other => Left(s"Unexpected message response: $other")
    }
  }

  /**
    * Cancel an Offer on the system. An error is returne if cancellation is not possible.
    * @param uuid
    * @return
    */
  def cancelOffer(uuid: UUID): Future[Either[String, OfferStatus]] = {
    offerSharding.ask(OfferShardMessage(uuid.toString, CancelOffer)).map {
      case os: OfferStatus => Right(os)
      case other => Left(s"Unexpected message response: $other")
    }
  }

  /**
    * Query an Offer using the supplied UUID
    * @param uuid
    * @return
    */
  def queryOffer(uuid: UUID): Future[Either[String, OfferStatus]] = {
    offerSharding.ask(OfferShardMessage(uuid.toString, QueryOffer)).map {
      case os: OfferStatus => Right(os)
      case other => Left(s"Unexpected message response: $other")
    }
  }

  /**
    * List all Offer IDs on the system (across all shards)
    *
    * This ultimately will not scale as a query tool, but is useful for admin function.
    * A better solution for user querying would be to use ElasticSearch to make the Offers
    * searchable by keyword and price. The OfferFSM actors could update their state
    * and search records based on state transitions, expiry or cancellation.
    * @return
    */
  def listOffers(): Future[Either[String, List[OfferIdJS]]] = {
    offerSharding.ask(GetShardRegionState).map {
      case CurrentShardRegionState(shards: Set[ShardState]) =>
        Right(shards.foldLeft(List.empty[OfferIdJS]){ (a, ss) =>
          a ++ ss.entityIds.toList.map{ eid => OfferIdJS(ss.shardId, eid.toString)}
        })

      case other => Left(s"Unexpected message response: $other")
    }
  }
}
