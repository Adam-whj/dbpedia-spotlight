/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.lucene.LuceneManager;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * We overrode hashCode and equals in our Scala classes. We want to make sure that it works in Java fine.
 *
 * @author pablomendes
 */
public class EqualityTests {

    DBpediaResource r1 = new DBpediaResource("DBpedia");
    DBpediaResource r2 = new DBpediaResource("DBpedia");
    DBpediaResource r3 = new DBpediaResource("http://dbpedia.org/resource/DBpedia");

    @Test
    public void testDBpediaResourceEquals() {
        assertEquals(r1,r2);
        assertEquals(r1,r3);
    }

    @Test
    public void testDBpediaResourceHashCode() {
        HashSet<DBpediaResource> set = new HashSet<DBpediaResource>();
        set.add(r1);
        set.add(r2);
        set.add(r3);
        assertEquals(1, set.size());
    }

}
