package io.offers.fsm

object Price {
  def apply(currency: String, amount: String): Price = Price(currency, BigDecimal(amount))
}

case class Price(currency: String, amount: BigDecimal)
