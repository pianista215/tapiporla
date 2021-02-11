package com.tapiporla.microservices.wallet.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.sksamuel.elastic4s.jackson.JacksonSupport
import com.sksamuel.elastic4s.{HitReader, Indexable}
import com.sksamuel.exts.Logging

object CustomElasticJackson {

  val mapper = JacksonSupport.mapper.registerModule(new JodaModule())

  object Implicits extends Logging {

    implicit def JacksonJsonIndexable[T](implicit mapper: ObjectMapper = mapper): Indexable[T] =
      com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits.JacksonJsonIndexable(mapper)

    implicit def JacksonJsonHitReader[T](implicit mapper: ObjectMapper = mapper,
                                         manifest: Manifest[T]): HitReader[T] =
      com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits.JacksonJsonHitReader(mapper, manifest)
  }

}
