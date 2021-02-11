package com.tapiporla.microservices.wallet.model

import org.joda.time.DateTime

case class User(
               email: String,
               password: String,
               accessTokens: Seq[Token]
               )


case class Token(
                id: String,
                expiration: DateTime
                )
