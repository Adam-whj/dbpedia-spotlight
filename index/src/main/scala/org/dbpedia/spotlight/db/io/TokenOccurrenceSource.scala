package org.dbpedia.spotlight.db.io

import java.io.{InputStream, FileInputStream, File}
import io.Source
import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.dbpedia.spotlight.db.model.{ResourceStore, TokenStore}
import org.apache.commons.logging.LogFactory
import scala.Predef._
import scala.Array


/**
 * @author Joachim Daiber
 *
 *
 *
 */

object TokenOccurrenceSource {

  private val LOG = LogFactory.getLog(this.getClass)

  def fromPigInputStream(tokenInputStream: InputStream, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore): Iterator[Triple[DBpediaResource, Array[Token], Array[Int]]] = {

    var i = 0
    plainTokenOccurrenceSource(tokenInputStream) map {
      case (wikiurl: String, tokens: Array[String], counts: Array[Int]) => {
        i += 1
        if (i % 10000 == 0)
          LOG.info("Read context for %d resources...".format(i))

        Triple(
          resStore.getResourceByName(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl)),
          tokens.map{ token => tokenStore.getToken(token) },
          counts
        )
      }
    }

  }

  def fromPigFile(tokenFile: File, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore) = fromPigInputStream(new FileInputStream(tokenFile), tokenStore, wikipediaToDBpediaClosure, resStore)


  def plainTokenOccurrenceSource(tokenInputStream: InputStream): Iterator[Triple[String, Array[String], Array[Int]]] = {
    Source.fromInputStream(tokenInputStream) getLines() filter(!_.equals("")) map {
      line: String => {
        val Array(wikiurl, tokens) = line.trim().split('\t')
          var tokensA = Array[String]()
          var countsA = Array[Int]()

          tokens.tail.init.split("[()]").filter(pair => !pair.equals(",") && !pair.equals("")).map {
            pair: String => {
              val i = pair.lastIndexOf(',')
              tokensA :+= pair.take(i)
              countsA :+= pair.drop(i+1).toInt
            }
          }
          Triple(wikiurl, tokensA, countsA)
      }
    }
  }
}
