package io.offers.http

import java.text.SimpleDateFormat
import java.util.TimeZone

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, jackson}

trait JsonProtocol extends Json4sSupport {

  implicit val serialization = jackson.Serialization

  implicit val formats = new DefaultFormats {
    val UTC = TimeZone.getTimeZone("UTC")
    val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    f.setTimeZone(UTC)
    override def dateFormatter = f
  }
}

object JsonProtocol extends JsonProtocol
