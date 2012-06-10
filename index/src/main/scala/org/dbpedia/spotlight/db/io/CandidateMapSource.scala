package org.dbpedia.spotlight.db.io

import io.Source
import scala.Predef._
import org.dbpedia.spotlight.model._
import java.io.{File, FileInputStream, InputStream}
import org.dbpedia.spotlight.db.model.{SurfaceFormStore, ResourceStore}
import java.lang.String
import collection.mutable.HashSet
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, DBpediaResourceNotFoundException, ItemNotFoundException}


/**
 * @author Joachim Daiber
 *
 */

/**
 * Represents a source of DBpediaResources
 */

object CandidateMapSource {

  def fromTSVInputStream(
    candmap: InputStream,
    resourceStore: ResourceStore,
    surfaceFormStore: SurfaceFormStore
    ): java.util.Map[Candidate, Int] = {

    val candidateMap = new java.util.HashMap[Candidate, Int]()

    val uriNotFound = HashSet[String]()
    val sfNotFound = HashSet[String]()

    Source.fromInputStream(candmap).getLines() foreach {
      line: String => {
        try {
          val s1 = line.trim().split("\t")
          val s2 = s1(0).split(" ")
          val sf = s1(1)
          val count = s2(0)
          val uri = new DBpediaResource(s2(1)).uri

          candidateMap.put(
            Candidate(surfaceFormStore.getSurfaceForm(sf), resourceStore.getResourceByName(uri)),
            count.toInt
          )
        } catch {
          case e: ArrayIndexOutOfBoundsException => System.err.println("WARNING: Could not read line.")
          case e: DBpediaResourceNotFoundException => uriNotFound += line
          case e: SurfaceFormNotFoundException => sfNotFound += line
        }
      }
    }

    System.err.println("WARNING: URI for %d candidate definitions not found!".format(uriNotFound.size) )
    System.err.println("WARNING: SF for %d candidate definitions not found!".format(sfNotFound.size) )

    candidateMap
  }

  def fromTSVFile(
    candmap: File,
    resStore: ResourceStore,
    sfStore: SurfaceFormStore
    ): java.util.Map[Candidate, Int] = {
    fromTSVInputStream(new FileInputStream(candmap), resStore, sfStore)
  }

}
