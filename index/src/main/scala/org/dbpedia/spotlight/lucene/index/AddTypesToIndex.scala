/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.io.{TypeAdder, FileOccurrenceSource}
import org.dbpedia.spotlight.model.{DBpediaType, DBpediaResource}
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.util.{IndexingConfiguration, TypesLoader}

/**
 * Reads file instance_types_en.nt from DBpedia in order to add Ontology Resource Types to the index.
 *
 * Usage:
 *
 *
 * @author maxjakob, Joachim Daiber (allows reading from TSV: used for DBpedia+Schema+Freebase types)
 */
object AddTypesToIndex {

    def loadTypes(instanceTypesFileName: String) = {
        instanceTypesFileName.endsWith(".tsv") match {
          case true  => TypesLoader.getTypesMapFromTSV_java(new File(instanceTypesFileName))
          case false => TypesLoader.getTypesMap_java(new FileInputStream(instanceTypesFileName))
        }
    }

    def main(args : Array[String]) {
      val indexingConfigFileName = args(0)

      val config = new IndexingConfiguration(indexingConfigFileName)
      val sourceIndexFileName = config.get("org.dbpedia.spotlight.index.dir")

      val indexDir = new File(sourceIndexFileName)
      var listOfIndexes = Seq[String](sourceIndexFileName)

      if (indexDir.listFiles().foldLeft(false)( _ || _.isDirectory )) {
        listOfIndexes = indexDir.listFiles().filter( _.isDirectory ).map(_.getAbsolutePath).toSeq
      }

      val instanceTypesFileName = config.get("org.dbpedia.spotlight.data.instanceTypes")
      val typesMap = loadTypes(instanceTypesFileName)

      for (sourceIndexFileName <- listOfIndexes) {
        val targetIndexFileName = sourceIndexFileName+"-withTypes"
        val typesIndexer = new IndexEnricher(sourceIndexFileName,targetIndexFileName, config)

        typesIndexer.enrichWithTypes(typesMap)
        typesIndexer.close
      }
    }

}
