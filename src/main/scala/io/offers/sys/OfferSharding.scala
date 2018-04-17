package io.offers.sys

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import io.offers.fsm.OfferFSM

object OfferSharding {

  case class OfferShardMessage(id: String, msg: Any)

  val regionName = "offers"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case OfferShardMessage(id: String, payload) ⇒ (id, payload)
  }

  val numberOfShards = 10

  val extractShardId: ShardRegion.ExtractShardId = {
    case OfferShardMessage(id: String, payload) ⇒ (id.hashCode % numberOfShards).toString
  }

  def init(system: ActorSystem): ActorRef = ClusterSharding(system).start(
    typeName = regionName,
    entityProps = OfferFSM.props(),
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId)
}
