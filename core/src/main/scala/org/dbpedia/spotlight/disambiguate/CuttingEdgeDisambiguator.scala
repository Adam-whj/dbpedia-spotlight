package org.dbpedia.spotlight.disambiguate

/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import java.io.File
import org.dbpedia.spotlight.lucene.similarity._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.exceptions.{SearchException, InputException}
import org.apache.lucene.search.Explanation
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.lucene.disambiguate.MixedWeightsDisambiguator
import scalaj.collection.Imports._

/**
 * Implementation used for evaluation runs. Will change as new scores/implementations come in.
 *
 * @author pablomendes
 */
class CuttingEdgeDisambiguator(val factory: SpotlightFactory) extends Disambiguator with ParagraphDisambiguator {

    private val LOG = LogFactory.getLog(this.getClass)

    LOG.info("Initializing disambiguator object ...")

//    val indexDir = new File(configuration.getContextIndexDirectory)
//
//    // Disambiguator
//    val dir = LuceneManager.pickDirectory(indexDir)
//
//    //val luceneManager = new LuceneManager(dir)                              // use this if surface forms in the index are case-sensitive
//    val luceneManager = new LuceneManager.CaseSensitiveSurfaceForms(dir)  // use this if all surface forms in the index are lower-cased
//    val cache = JCSTermCache.getInstance(luceneManager, configuration.getMaxCacheSize);
//    luceneManager.setContextSimilarity(new CachedInvCandFreqSimilarity(cache))        // set most successful Similarity
//    luceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)

    val disambiguator : Disambiguator = new MixedWeightsDisambiguator(factory.searcher, new LinearRegressionMixture())

    //TODO fix MultiThreading
    //val disambiguator : Disambiguator = new MultiThreadedDisambiguatorWrapper(new MixedWeightsDisambiguator(contextSearcher, new LinearRegressionMixture()))

    LOG.info("Done.")

    def disambiguate(sfOccurrence: SurfaceFormOccurrence): DBpediaResourceOccurrence = {
        disambiguator.disambiguate(sfOccurrence)
    }

    @throws(classOf[InputException])
    def disambiguate(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.disambiguate(sfOccurrences)
    }
    /**
     * Method for ParagraphDisambiguator
     */
    def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
        paragraph.occurrences.foldRight(List[DBpediaResourceOccurrence]())((o,acc) => disambiguate(o) :: acc)
    }

    def bestK(sfOccurrence: SurfaceFormOccurrence, k: Int): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.bestK(sfOccurrence, k)
    }
    /**
     * Method for ParagraphDisambiguator
     */
    def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {
        paragraph.occurrences.foldRight(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())((o,acc) => {
            try {
                acc + (o -> bestK(o,k).asScala.toList)
            } catch {
                case e: Exception => {
                    LOG.debug("Exception: "+e.getMessage)
                    //e.printStackTrace()
                    acc
                }
            }
        })
    }

    def name() : String = {
        "CuttingEdge:"+disambiguator.name
    }

    def ambiguity(sf : SurfaceForm) : Int = {
        disambiguator.ambiguity(sf)
    }

    def support(resource : DBpediaResource) : Int = {
        disambiguator.support(resource)
    }

    def spotProbability(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[SurfaceFormOccurrence] = {
        disambiguator.spotProbability(sfOccurrences)
    }

    @throws(classOf[SearchException])
    def explain(goldStandardOccurrence: DBpediaResourceOccurrence, nExplanations: Int) : java.util.List[Explanation] = {
        disambiguator.explain(goldStandardOccurrence, nExplanations)
    }

    def contextTermsNumber(resource : DBpediaResource) : Int = {
        disambiguator.contextTermsNumber(resource)
    }

    def averageIdf(context : Text) : Double = {
        disambiguator.averageIdf(context)
    }

}