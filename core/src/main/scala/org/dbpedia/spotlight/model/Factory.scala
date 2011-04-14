package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.string.ModifiedWikiUtil
import org.apache.lucene.document.Document
import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import java.io.File
import org.apache.lucene.search.Similarity
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.apache.lucene.store.Directory
import org.dbpedia.spotlight.disambiguate.mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.lucene.disambiguate.MixedWeightsDisambiguator
import org.dbpedia.spotlight.annotate.DefaultAnnotator
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter
import org.dbpedia.spotlight.candidate.{CommonWordFilter, SpotSelector}
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters

/**
 * Class containing methods to create model objects in many different ways
 *
 * @author pablomendes
 */
object Factory {

    def createSurfaceFormFromDBpediaResourceURI(resource: DBpediaResource) = {
        val name = ModifiedWikiUtil.cleanPageTitle(resource.uri)
        val surfaceForm = new SurfaceForm(name);
        surfaceForm;
    }

    def createDBpediaResourceOccurrenceFromDocument(doc : Document) {
        createDBpediaResourceOccurrenceFromDocument(doc, -1)
    }
    def createDBpediaResourceOccurrenceFromDocument(doc : Document, id: Int) = {
        // getField: If multiple fields exists with this name, this method returns the first value added.
        var resource = new DBpediaResource(doc.getField(LuceneManager.DBpediaResourceField.URI.toString).stringValue)
        var context = new Text(doc.getFields(LuceneManager.DBpediaResourceField.CONTEXT.toString).mkString("\n"))

        new DBpediaResourceOccurrence(
            resource,
            createSurfaceFormFromDBpediaResourceURI(resource), // this is sort of the "official" surface form, since it's the cleaned up title
            context,
            -1,
            Provenance.Wikipedia // Ideally grab this from index, if we have sources other than Wikipedia
            )
    }

}

class LuceneFactory(val configuration: SpotlightConfiguration,
                    val analyzer: Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET)
                    ) {

    val directory : Directory = LuceneManager.pickDirectory(new File(configuration.getIndexDirectory))
    val luceneManager : LuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
    val similarity : Similarity = new CachedInvCandFreqSimilarity(new JCSTermCache(luceneManager))

    luceneManager.setContextAnalyzer(analyzer);
    luceneManager.setContextSimilarity(similarity);

    val searcher = new MergedOccurrencesContextSearcher(luceneManager);

    def disambiguator() = {
        val mixture = new LinearRegressionMixture
        new MixedWeightsDisambiguator(searcher,mixture);
    }

    def spotter() ={
        new LingPipeSpotter(new File(configuration.getSpotterFile));
    }

    def spotSelector() ={
        new CommonWordFilter(configuration.getCommonWordsFile)
    }

    def annotator() ={
        new DefaultAnnotator(spotter(),spotSelector(),disambiguator())
    }

    def filter() ={
        new CombineAllAnnotationFilters(configuration)
    }

}