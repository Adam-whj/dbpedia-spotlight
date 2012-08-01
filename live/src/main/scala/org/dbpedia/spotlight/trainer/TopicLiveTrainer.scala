package org.dbpedia.spotlight.trainer

import org.dbpedia.spotlight.topic.{WekaMultiLabelClassifier, TopicalClassifier}
import org.dbpedia.spotlight.model._
import org.apache.commons.logging.LogFactory
import scala.collection.mutable._
import xml.XML
import java.io.{FileWriter, PrintWriter, File}

object TopicLiveTrainer {

  /*main(Array("/home/dirk/workspace/dbpedia-spotlight/conf/server.properties",
    "/home/dirk/workspace/dbpedia-spotlight/index/src/main/resources/topic_descriptions.xml", "en", "10000"))*/

  /**
   * example: "/home/dirk/workspace/dbpedia-spotlight/conf/server.properties","/home/dirk/workspace/dbpedia-spotlight/index/src/main/resources/topic_descriptions.xml","en","10000"
   * @param args topical classifier configuration file (like server.properties), topic descriptions (in resources folder), language tag ("en"), rss feed update interval (in ms)
   */
  def main(args: Array[String]) {
    val topicInfos = TopicInformation.fromDescriptionFile(args(1))

    val config = new TopicalClassificationConfiguration(args(0))
    val classifier = config.getClassifier
    val trainer = new TopicLiveTrainer(classifier)
    val languageTag = args(2)

    val wikiUpdateConfig = getClass.getClassLoader.getResource("pedia_uima_harvester/descriptors/CPE/HTTPCR_parser_wst_category_externalConsumer_CPE.xml").getPath

    val configXml = XML.loadFile(wikiUpdateConfig)
    val port = ((configXml \\ "nameValuePair").find(node => (node \\ "name").head.text.equals("Ports")).get \\ "integer").text.toInt

    WikipediaUpdateFeed.startFeed(
      languageTag + ".wikipedia", "localhost", port, languageTag + "wikipediaorgtest", languageTag,
      getClass.getClassLoader.getResource("pedia_uima_harvester/resources/articlefilter/redirects.list").getPath,
      getClass.getClassLoader.getResource("pedia_uima_harvester/resources/articlefilter/nonarticle_titles_prefixes.list").getPath,
      wikiUpdateConfig)

    //subscribe to wiki feed (wait until it is registered)
    var wikiFeeds = FeedRegistry.getFeeds[(DBpediaResource, Set[DBpediaResource], Set[DBpediaCategory], Text)]
    while (wikiFeeds.isEmpty) {
      Thread.sleep(1000)
      wikiFeeds = FeedRegistry.getFeeds[(DBpediaResource, Set[DBpediaResource], Set[DBpediaCategory], Text)]
    }

    topicInfos.foreach(info => {
      if (info.rssFeeds.size > 0) {
        new RssTopicFeed(info.topic, info.rssFeeds.toArray, args(3).toLong)
      }
    })

    //subscribe to rss feeds
    trainer.subscribeToAll
  }
}

/**
 * This class represents a trainer for a topical classifier. It basically consumes different Feeds that are topically
 * annotated, which are for now Feeds of type: (Topic, RssItem), (Set[Topic], Text), (Topic, Text).
 *
 * @param classifier classifier to update and save after training stopped
 * @param evalFile if defined, file which will contain training evaluations
 * @param evalInterval interval (measured in number of updates) between evaluations
 */
class TopicLiveTrainer(val classifier: TopicalClassifier, minimalConfidence:Double = 0.8, evalFile: File = null, evalInterval:Int = 100) {
  private val LOG = LogFactory.getLog(getClass)
  private val multiLabel = classifier.isInstanceOf[WekaMultiLabelClassifier]
  private var evalWriter:PrintWriter = null
  if (evalFile!=null)
    evalWriter = new PrintWriter(new FileWriter(evalFile,true))

  private val rssFeedListener = new FeedListener[(Topic, RssItem)] {
    protected override def update(item: (Topic, RssItem)) {
      LOG.debug("Updating topical classifier from RSS...")
      updateModel(Map(item._1->1), item._2.description)
    }
  }

  private val topicSetTextFeedListener = new FeedListener[(Map[Topic,Double],Text)] {
    protected override def update(item: (Map[Topic,Double],Text)) {
      LOG.debug("Updating topical classifier...")
      updateModel(item._1,item._2)
    }
  }

  private val topicTextFeedListener = new FeedListener[(Topic, Text)] {
    protected override def update(item: (Topic, Text)) {
      LOG.debug("Updating topical classifier...")
      updateModel(Map(item._1->1),item._2)
    }
  }

  private var updateCtr = 0
  private var meanSquaredError = 0.0

  private def updateModel(topics:Map[Topic,Double], text:Text) {
    updateCtr += 1
    val predictions = classifier.getPredictions(text)

    if (evalWriter!=null) {
      meanSquaredError += predictions.foldLeft(0.0)((sum,prediction) =>
        sum + math.pow( topics.getOrElse(prediction._1, 0.0) - prediction._2, 2))

      if (updateCtr%evalInterval==0) {
        evalWriter.println("===== Evaluation of interval "+(updateCtr-evalInterval)+" - "+updateCtr+" =====")
        evalWriter.println("mean squared error: "+(meanSquaredError/evalInterval))
        evalWriter.println("last topics: "+topics.toList.sortBy(-_._2).take(4).foldLeft("")(
          (string,topic) => string+" ("+topic._2.toString.substring(0,math.min(4,topic._2.toString.length))+","+topic._1.getName+")"))
        evalWriter.println("for text: "+text.text.substring(0,math.min(200,text.text.length))+"...")
        evalWriter.println("last predictions:"+predictions.toList.sortBy(-_._2).take(4).foldLeft("")(
          (string,prediction) => string+" ("+prediction._2.toString.substring(0,math.min(4,prediction._2.toString.length))+","+prediction._1.getName+")"))
        evalWriter.flush()
        meanSquaredError = 0.0
      }
    }
    if (evalWriter!=null)
      evalWriter.print("last trained topics: ")
    predictions.foreach{ case (topic,prediction) =>
      if (prediction > minimalConfidence)
        if (topics.getOrElse(topic,0.0) < minimalConfidence) {
          if (evalWriter!=null)
            evalWriter.print(" not_"+topic.getName)
          classifier.update(text,new Topic(WekaMultiLabelClassifier.NEGATIVE_TOPIC_PREFIX+topic.getName))
        }
        else {
          if (evalWriter!=null)
            evalWriter.print(" "+topic.getName)
          classifier.update(text,topic)
        }
    }
    if (evalWriter!=null)
      evalWriter.println
  }

  def subscribeToAll {
    rssFeedListener.subscribeToAllFeeds
    topicSetTextFeedListener.subscribeToAllFeeds
    topicTextFeedListener.subscribeToAllFeeds
  }

  def stopTraining {
    rssFeedListener.unSubscribeToAllFeeds
    topicSetTextFeedListener.unSubscribeToAllFeeds
    topicTextFeedListener.unSubscribeToAllFeeds
    if (evalWriter!=null)
      evalWriter.close()
  }

  def saveModel {
    classifier.persist
  }
}
