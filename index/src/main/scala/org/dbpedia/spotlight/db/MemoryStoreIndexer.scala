package org.dbpedia.spotlight.db

import gnu.trove.TObjectIntHashMap
import java.io.File
import collection.mutable.ListBuffer
import memory.{MemoryStore, MemorySurfaceFormStore}
import org.dbpedia.spotlight.model.{SurfaceFormIndexer, SurfaceForm}
import java.util.Map
import org.apache.commons.lang.NotImplementedException
import scala.collection.JavaConversions._

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemoryStoreIndexer(val baseDir: File)
  extends SurfaceFormIndexer {

  //SURFACE FORMS

  lazy val sfStore = new MemorySurfaceFormStore()

  def addSurfaceForm(sf: SurfaceForm, count: Int) {
    throw new NotImplementedException()
  }

  def addSurfaceForms(sfCount: Map[SurfaceForm, Int]) {
    addSurfaceForms(sfCount.toIterator)
  }

  def addSurfaceForms(sfCount: Iterator[Pair[SurfaceForm, Int]]) {
    var i = 1

    val supportForID = ListBuffer[Int]()
    supportForID += 0
    val idForString = new TObjectIntHashMap()

    sfCount foreach {
      case (sf, count) => {
        idForString.put(sf.name, i)
        supportForID += count

        i += 1
      }
    }
    sfStore.idForString = idForString
    sfStore.supportForID = supportForID.toArray
    MemoryStore.dump[MemorySurfaceFormStore](sfStore, new File(baseDir, "sf.mem"))

  }


}