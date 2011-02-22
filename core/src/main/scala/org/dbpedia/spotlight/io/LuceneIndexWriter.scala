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

package org.dbpedia.spotlight.io

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.lucene.index.{BaseIndexer, OccurrenceContextIndexer, MergedOccurrencesContextIndexer}

/**
 * Writes Lucene index.
 */


object LuceneIndexWriter
{
    private val LOG = LogFactory.getLog(this.getClass)

    def writeLuceneIndex(indexer : OccurrenceContextIndexer, occSource : OccurrenceSource)
    {
        var indexDisplay = 0
        LOG.info("Indexing with " + indexer.getClass + " in Lucene ...")

        for (occ <- occSource.view)
        {
            indexer.add(occ)

            indexDisplay += 1
            if (indexDisplay % 10000 == 0) {
                LOG.debug("  indexed " + indexDisplay + " occurrences")
            }
        }
        indexer.close  // important

        LOG.info("Finished: indexed " + indexDisplay + " occurrences")
    }

}