package org.dbpedia.spotlight.evaluation

import org.apache.lucene.analysis.{StopAnalyzer, Analyzer}
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.io.{LuceneIndexWriter, FileOccurrenceSource}
import org.dbpedia.spotlight.lucene.index.MergedOccurrencesContextIndexer
import org.apache.commons.logging.LogFactory
import java.io.{PrintStream, File}
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.search.{Similarity, DefaultSimilarity}
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.disambiguate._
import org.dbpedia.spotlight.lucene.similarity._

import  org.dbpedia.spotlight.evaluation.Profiling._
import org.apache.lucene.store.{NIOFSDirectory, Directory, FSDirectory}
import org.dbpedia.spotlight.disambiguate.Disambiguator

/**
 * This class is evolving to be the main disambiguation class that takes parameters for which dataset to run.
 *
 * @author pablomendes
 */
object EvaluateDisambiguationOnly
{

    private val LOG = LogFactory.getLog(this.getClass)

    def createMergedDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity) : Disambiguator = {
        val directory = FSDirectory.open(new File(outputFileName+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        createMergedDisambiguator(outputFileName, analyzer, similarity, directory)
    }

    def createMergedDisambiguator(outputFileName: String, analyzer: Analyzer, similarity: Similarity, directory: Directory) : Disambiguator = {
        ensureExists(directory)
        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        //val isfLuceneManager = new LuceneManager.BufferedMerging(new RAMDirectory())

//        val queryTimeAnalyzer = new QueryAutoStopWordAnalyzer(Version.LUCENE_29, analyzer);
        val queryTimeAnalyzer = analyzer;

        luceneManager.setContextAnalyzer(queryTimeAnalyzer);
        luceneManager.setContextSimilarity(similarity);
        //------------ ICF DISAMBIGUATOR
        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);

        //timed(printTime("Adding auto stopwords took ")) {
        //  queryTimeAnalyzer.addStopWords(contextSearcher.getIndexReader, DBpediaResourceField.CONTEXT.toString, 0.5f);
        //}

        timed(printTime("Warm up took ")) {
          //contextSearcher.warmUp(10000);
        }

        LOG.info("Number of entries in merged resource index ("+contextSearcher.getClass()+"): "+ contextSearcher.getNumberOfEntries());
        // The Disambiguator chooses the best URI for a surface form
        new MergedOccurrencesDisambiguator(contextSearcher)
    }

    def getICFSnowballDisambiguator(outputFileName: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new InvCandFreqSimilarity();
        val directory =  LuceneManager.pickDirectory(new File(outputFileName+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        createMergedDisambiguator(outputFileName, analyzer, similarity, directory)
    }

  def getICF2Disambiguator(outputFileName: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      val directory = LuceneManager.pickDirectory(new File(outputFileName+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val cache = new JCSTermCache(new LuceneManager.BufferedMerging(directory));
      val similarity : Similarity = new CachedInvCandFreqSimilarity(cache);
      createMergedDisambiguator(outputFileName, analyzer, similarity, directory)
  }

  def getNewDisambiguator(outputFileName: String) : Disambiguator = {
      val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      val directory = LuceneManager.pickDirectory(new File(outputFileName+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
      val cache = new JCSTermCache(new LuceneManager.BufferedMerging(directory));
      val similarity : Similarity = new NewSimilarity(cache);
      createMergedDisambiguator(outputFileName, analyzer, similarity, directory)
  }


    def getICFStandardDisambiguator(outputFileName: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new InvCandFreqSimilarity();
        val directory = FSDirectory.open(new File(outputFileName+"."+analyzer.getClass.getSimpleName+".DefaultSimilarity"));
        createMergedDisambiguator(outputFileName, analyzer, similarity, directory)
    }

    def getDefaultSnowballDisambiguator(outputFileName: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity();
        createMergedDisambiguator(outputFileName, analyzer, similarity)
    }

    def getDefaultStandardDisambiguator(outputFileName: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new DefaultSimilarity();
        createMergedDisambiguator(outputFileName, analyzer, similarity)
    }

    def getSweetSpotSnowballDisambiguator(outputFileName: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new SweetSpotSimilarity()
        createMergedDisambiguator(outputFileName, analyzer, similarity)
    }

    def getSweetSpotStandardDisambiguator(outputFileName: String) : Disambiguator = {
        val analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        val similarity : Similarity = new SweetSpotSimilarity()
        createMergedDisambiguator(outputFileName, analyzer, similarity)
    }


    // the next two use an own disambiguator, while the two before just use a different similarity class

//    def getDefaultScorePlusPriorSnowballDisambiguator(outputFileName: String) : Disambiguator = {
//        var analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//        var similarity : Similarity = new DefaultSimilarity();
//        val directory = FSDirectory.open(new File(outputFileName+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
//
//        ensureExists(directory)
//        val luceneManager = new LuceneManager.BufferedMerging(directory)
//        //val isfLuceneManager = new LuceneManager.BufferedMerging(new RAMDirectory())
//        luceneManager.setContextAnalyzer(analyzer);
//        luceneManager.setContextSimilarity(similarity);
//        //------------ ICF DISAMBIGUATOR
//        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);
//        LOG.info("Number of entries in merged resource index ("+contextSearcher.getClass()+"): "+ contextSearcher.getNumberOfEntries());
//        // The Disambiguator chooses the best URI for a surface form
//        new MergedPlusPriorDisambiguator(contextSearcher)
//    }







    def getICFIDFSnowballDisambiguator(outputFileName: String) : Disambiguator = {
        var analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        var similarity : Similarity = new ICFIDFSimilarity();
        createMergedDisambiguator(outputFileName, analyzer, similarity, FSDirectory.open(new File(outputFileName+"."+analyzer.getClass.getSimpleName+".InvSenseFreqSimilarity")))
    }

    def getICFIDFStandardDisambiguator(outputFileName: String) : Disambiguator = {
        var analyzer : Analyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer(Version.LUCENE_29, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        var similarity : Similarity = new ICFIDFSimilarity();
        createMergedDisambiguator(outputFileName, analyzer, similarity, FSDirectory.open(new File(outputFileName+"."+analyzer.getClass.getSimpleName+".InvSenseFreqSimilarity")))
    }

    def getPriorDisambiguator(outputFileName: String) : Disambiguator = {
        var analyzer : Analyzer = new org.apache.lucene.analysis.snowball.SnowballAnalyzer(Version.LUCENE_29, "English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        var similarity : Similarity = new DefaultSimilarity
        val directory = new NIOFSDirectory(new File(outputFileName+"."+analyzer.getClass.getSimpleName+"."+similarity.getClass.getSimpleName));
        //val luceneManager = new LuceneManager.BufferedMerging(directory)
        val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(directory)
        luceneManager.setContextAnalyzer(analyzer);
        luceneManager.setContextSimilarity(similarity);
        val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager);
        LOG.info("Number of entries in merged resource index ("+contextSearcher.getClass()+"): "+ contextSearcher.getNumberOfEntries());
        new LucenePriorDisambiguator(contextSearcher)
    }


    def ensureExists(directory: Directory)() {
      return true;
        // Lucene Indexer - Needs an index in disk to be used in disambiguation
//        if (!directory.getFile.exists)
//        {
////                LOG.info("Index directory does not exist. Will index.");
////                index(trainingFile, isfLuceneManager);
//            System.err.println("Index directory does not exist. "+directory.getFile);
//            exit();
//        } else {
//            LOG.info("Index directory exists ("+directory.getFile+"). Will not reindex.");
//        }
    }

    def index(trainingFile: File, luceneManager: LuceneManager.BufferedMerging) {
            val trainSource = FileOccurrenceSource.fromFile(trainingFile)
            val vectorBuilder = new MergedOccurrencesContextIndexer(luceneManager);
            LuceneIndexWriter.writeLuceneIndex(vectorBuilder, trainSource)
            LOG.info("Number of entries indexed: "+vectorBuilder.numEntriesProcessed);
    }

    def exists(fileName: String) {
            if (!new File(fileName).exists) {
                System.err.println("Important directory does not exist. "+fileName);
                exit();
            }
    }

    def main(args : Array[String])
        {
            //val baseDir = "e:/dbpa/data/Person/";
            //val baseDir = "e:/dbpa/data/All-50-50/";
            //val baseDir = "e:/dbpa/data/RelaxedApple/";
            //val baseDir = "e:/dbpa/data/StrictApple/";
            //var baseDir: String = "C:\\Users\\Pablo\\workspace\\dbpa\\data\\Company\\"

            val indexDir: String = "/home/pablo/web/DisambigIndex.singleSFs-plusTypes"; // args(0)
            //val indexDir: String = "/home/pablo/web/DisambigIndex.allSFsAllowed.plusTypes-plusSFs";
            //val indexDir: String = "/home/pablo/web/DisambigIndex.restrictedSFs.plusTypes-plusSFs"

            //val indexDir: String = "/home/pablo/web/DisambigIndex.allSFsAllowed.plusTypes-plusSFs.SnowballAnalyzer.DefaultSimilarity"
            val testFileName: String = "/home/pablo/eval/cucerzan/cucerzan-occs.tsv"; // "/home/pablo/data/split.train-98.amb/wikipediaTest.amb.tsv" // args(1)
            val resultsFileName: String = "/home/pablo/eval/cucerzan/disambiguation.results"; //"/home/pablo/data/split.train-98.amb/disambiguation.results"

            val out = new PrintStream(resultsFileName, "UTF-8");

          //exists(indexDir);
          exists(testFileName);
          //exists(resultsFileName);

            val osName : String = System.getProperty("os.name");
            LOG.info("Your operating system is: "+osName);

//            val trainingFile = new File(baseDir+"wikipediaTraining.50.amb.tsv")
//            if (!trainingFile.exists) {
//                System.err.println("Training file does not exist. "+trainingFile);
//                exit();
//            }


//            val trainingFileName = baseDir+"wikipediaAppleTraining.50.amb.tsv"
//            val luceneIndexFileName = baseDir+"2.9.3/MergedIndex.wikipediaAppleTraining.a50"
//            val testFileName = baseDir+"wikipediaAppleTest.50.amb.tsv"

            // For merged disambiguators
            //val outputFileName = baseDir+"/2.9.3/Index.wikipediaTraining.Merged";


            val disSet = Set(// Snowball analyzer
                                //getDefaultSnowballDisambiguator(indexDir),
                                //getNewDisambiguator(indexDir),
                                getICF2Disambiguator(indexDir)
                                //getICFSnowballDisambiguator(indexDir)
                                //getSweetSpotSnowballDisambiguator(indexDir)
                                //getICFWithPriorSnowballDisambiguator(indexDir),
                                //getICFIDFSnowballDisambiguator(indexDir),

                                //getDefaultScorePlusPriorSnowballDisambiguator(indexDir)
//                                getDefaultScorePlusConditionalSnowballDisambiguator(indexDir),
//                                getProbPlusPriorSnowballDisambiguator(indexDir),
//                                getProbPlusConditionalSnowballDisambiguator(indexDir)

                             // Standard analyzer
                                //getDefaultStandardDisambiguator(outputFileName),
                                //getICFStandardDisambiguator(outputFileName),
                                //getSweetSpotStandardDisambiguator(outputFileName),
                                //getICFWithPriorStandardDisambiguator(outputFileName),
                                //getICFIDFStandardDisambiguator(outputFileName),
                                //getICFIDFStandardDisambiguator,

                             // no analyzer
                                //getPriorDisambiguator(indexDir)
            )

            // Read some text to test.
            val testSource = FileOccurrenceSource.fromFile(new File(testFileName))

            //      // Now with most common amongst top K most similar contexts
            //      var disambiguator2 : Disambiguator = new MergedOccurrencesDisambiguator(resourceSearcher, new RankingStrategy.MostCommonAmongTopK(contextSearcher))
            //      // Now with most common URI by usage (prior) -- reading from file
            //      //var disambiguator3 : Disambiguator = new PriorDisambiguator(surrogateSearcher, new DataLoader(new DataLoader.TSVParser(), new File(resourcePriorsFileName)));
            //      // Now with most common URI by usage (prior) -- reading from lucene
            //      var disambiguator4 : Disambiguator = new LucenePriorDisambiguator(resourceSearcher);


            //        LOG.info("=================================");

          //testSource.view(10000,15000)
          val evaluator = new DisambiguationEvaluator(testSource, disSet, out);
          evaluator.evaluate()
          out.close();


        }

}