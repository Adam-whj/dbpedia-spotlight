package org.dbpedia.spotlight.lucene.search;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Similarity;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneFeatureVector;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.similarity.CachedSimilarity;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.model.vsm.FeatureVector;
import org.dbpedia.spotlight.string.ContextExtractor;

import java.io.IOException;
import java.util.*;

/**
 * Contains a unified index of (surface form, uri, context)
 * - Allows one step disambiguation (faster than two steps: getSurrogate, then Disambiguate)
 * - Allows prior disambiguator to get a quick "default sense" based on the most popular URI for a given surface form
 * @author pablomendes
 */
public class MergedOccurrencesContextSearcher extends BaseSearcher implements ContextSearcher {

    String[] onlyUri = {LuceneManager.DBpediaResourceField.URI.toString()};
    private String[] onlyUriAndTypes = {LuceneManager.DBpediaResourceField.URI.toString(),
                                        LuceneManager.DBpediaResourceField.TYPE.toString()};

    public MergedOccurrencesContextSearcher(LuceneManager lucene) throws IOException {
        super(lucene);
        mSearcher.setSimilarity(lucene.contextSimilarity());
    }

    public Similarity getSimilarity() {
        return mLucene.contextSimilarity();
    }
    /**
      * MIXED ALL IN ONE DISAMBIGUATOR IMPLEMENTATION
      * Implementation to perform two steps at once.
      * Performs a SurrogateIndex#get(SurfaceForm) followed by a DBpediaResourceIndex#get(DBpediaResource)
      * @param
      * @return
      * @throws org.dbpedia.spotlight.exceptions.SearchException
      */
    public List<FeatureVector> getSurrogates(SurfaceForm sf) throws SearchException {
        //LOG.debug("Retrieving surrogates for surface form: "+sf);

        // search index for surface form
        List<FeatureVector> surrogates = new ArrayList<FeatureVector>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(sf))) {
            int docNo = hit.doc;

            DBpediaResource resource = getDBpediaResource(docNo);
            TermFreqVector vector = getVector(docNo);
            surrogates.add(new LuceneFeatureVector(resource, vector));
        }

        //LOG.debug(surrogates.size()+" surrogates found.");

        // return set of surrogates
        return surrogates;
    }

    /**
     * How many concepts co-occurred with this surface form in some paragraph, with the surface form annotated or unnanotated.
     * @param sf
     * @return
     * @throws SearchException
     */
    public long getConceptNeighborhoodCount(SurfaceForm sf) throws SearchException {
        long ctxFreq = 0;
        Similarity similarity = getSimilarity();
        if (similarity instanceof CachedSimilarity)
            ctxFreq =((CachedSimilarity) similarity).getTermCache().getPromiscuity(mReader, sf); // quick cached response
        else {
            ScoreDoc[] hits = getHits(mLucene.getMustQuery(new Text(sf.name())));    // without caching we need to search
            ctxFreq = hits.length;
        }
        LOG.trace("Context Frequency (Promiscuity) for "+sf+"="+ctxFreq);
        return ctxFreq;
    }

    /**
     * How many occurrences contain this surface form
     * @param sf
     * @return
     * @throws SearchException
     */
    public long getContextFrequency(SurfaceForm sf) throws SearchException {
        long ctxFreq = 0;
        Query q = mLucene.getQuery(new Text(sf.name()));
        Set<Term> qTerms = new HashSet<Term>();
        q.extractTerms(qTerms);
        ScoreDoc[] hits = getHits(mLucene.getMustQuery(qTerms));
        for (ScoreDoc d: hits) {
            TermFreqVector vector = null;
            try {
                vector = mReader.getTermFreqVector(d.doc, LuceneManager.DBpediaResourceField.CONTEXT.toString());
            } catch (IOException e) {
                throw new SearchException("Error getting term frequency vector. ", e);
            }
            if (vector!=null) {
                int freq = Integer.MAX_VALUE; // set to max value because will be used in Math.min below
                // get min
                for (Term t : qTerms) {
                    int[] freqs = vector.getTermFrequencies();
                    String[] terms = vector.getTerms();
                    int pos = Arrays.binarySearch(terms,t.text());
                    int termFreq = freqs[pos];
                    freq = Math.min(freq, termFreq); // estimate probability of phrase by the upper bound which is the minimum freq of individual term.
                }
                ctxFreq += freq;
            }
        }
        LOG.trace("Context Frequency (Promiscuity) for "+sf+"="+ctxFreq);
        return ctxFreq;
    }


    public long getFrequency(SurfaceForm sf) throws SearchException {
        long ctxFreq = 0;
        Query q = mLucene.getQuery(sf);
        ScoreDoc[] hits = getHits(q);
        for (ScoreDoc d: hits) {
            int docNo = d.doc;
            String[] onlySf = {LuceneManager.DBpediaResourceField.SURFACE_FORM.toString()};
            //Surface Form field does not store frequency vector, but indexes surface forms multiple times.
            Document doc = getDocument(docNo, new MapFieldSelector(onlySf));
            String[] sfList = doc.getValues(LuceneManager.DBpediaResourceField.SURFACE_FORM.toString());

            //Need to filter sfList to keep only the elements that are equal to the sf (caution with lowercase)
            int i=0;
            for (String sfName: sfList) {
                if (sfName.toLowerCase().equals(sf.name().toLowerCase())) //TODO un-hardcode lowercase! :)
                    ctxFreq++;
                i++;
            }
        }
        LOG.trace("Frequency for "+sf+"="+ctxFreq);
        return ctxFreq;
    }


    public List<Document> getFullDocuments(SurfaceForm sf, int k) {
        //LOG.trace("Retrieving documents for surface form: "+sf);

        // search index for surface form
        List<Document> documents = new ArrayList<Document>();

        // get documents containing the surface form
        ScoreDoc[] hits;
        try {
            hits = getHits(mLucene.getQuery(sf), k);
        } catch (SearchException e) {
            LOG.debug(e);
            return documents;
        }

        // Iterate through the results:
        int i = 0;
        for (ScoreDoc hit : hits) {
            try {
                if (i==k) break;
                documents.add(getFullDocument(hit.doc));  //TODO can gain some speedup by lazy loading context
                i++;
            } catch (SearchException e) {
                LOG.debug(e);
            }
        }
        //LOG.debug(documents.size()+" documents found.");

        // return set of surrogates
        return documents;
    }

    public List<Document> getDocuments(DBpediaResource res, FieldSelector fieldSelector) throws SearchException {
        //LOG.trace("Retrieving documents for surface form: "+res);

        // search index for surface form
        List<Document> documents = new ArrayList<Document>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(res))) {
            documents.add(getDocument(hit.doc, fieldSelector));
        }
        //LOG.debug(documents.size()+" documents found.");

        // return set of surrogates
        return documents;
    }

    public List<Document> getDocuments(SurfaceForm sf, FieldSelector fieldSelector) throws SearchException {
        //LOG.trace("Retrieving documents for surface form: "+res);

        // search index for surface form
        List<Document> documents = new ArrayList<Document>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(sf))) {
            documents.add(getDocument(hit.doc, fieldSelector));
        }
        //LOG.debug(documents.size()+" documents found.");

        // return set of surrogates
        return documents;
    }


    ContextExtractor contextExtractor = new ContextExtractor();
    public ScoreDoc[] getHits(SurfaceForm sf, Text context) throws SearchException {
        //FIXME
        //HACK
        //TODO We need a better fix for the too many clauses lucene error.
        // Sometimes we get that error because of funny chars (encoding)
        Text narrowContext = context;
//        int queryEstimatedSize = context.text().split("\\W+").length;
//        if (queryEstimatedSize > 1000) { //too many clauses is 1024
//            narrowContext = contextExtractor.narrowContext(sf, context);
//        }
        ScoreDoc[] hits = getHits(mLucene.getQuery(sf, narrowContext));

        return hits;
    }

    public ScoreDoc[] getHits(DBpediaResource resource) throws SearchException {
        return getHits(mLucene.getQuery(resource));        
    }

    public int getAmbiguity(SurfaceForm sf) throws SearchException {
        ScoreDoc[] hits = getHits(mLucene.getQuery(sf));
        LOG.trace("Ambiguity for "+sf+"="+hits.length);
        return hits.length;
    }

    public int getNumberOfOccurrences(DBpediaResource res) throws SearchException {
        FieldSelector fieldSelector = new MapFieldSelector(onlyUri);
        int numUris = 0;
        for (Document doc: getDocuments(res, fieldSelector)) {
            String[] fields = doc.getValues(LuceneManager.DBpediaResourceField.URI.toString());
            numUris += fields.length;
        }
        return numUris;
    }

    /**
     *
     * @param dbpediaResource
     * @param topN
     * @return
     */
    public Map<String, Integer> getContextWords(DBpediaResource dbpediaResource, int topN) throws SearchException {
        Map<String,Integer> termFreqMap = new HashMap<String,Integer>();
        ScoreDoc[] docs = getHits(dbpediaResource);
        //TODO Create an exception DuplicateResourceException
        LOG.error(String.format("Resource %s has more than one document in  the index. Maybe index corrupted?", dbpediaResource));
        // Will accept multiple docs for a resource and get the overall top terms
        try {
            for (ScoreDoc d: docs) {
                TermFreqVector vector = mReader.getTermFreqVector(d.doc, LuceneManager.DBpediaResourceField.CONTEXT.toString());
                int[] freqs = vector.getTermFrequencies();
                String[] terms = vector.getTerms();
                for (int i=0; i<vector.size(); i++) {
                    termFreqMap.put(terms[i],freqs[i]);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Ordering descOrder = new Ordering<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String,Integer> left, Map.Entry<String,Integer> right) {
                return Ints.compare(right.getValue(), left.getValue());
            }
        };
        List<Map.Entry<String,Integer>> sorted = descOrder.sortedCopy(termFreqMap.entrySet());

        return termFreqMap;
    }



    @Override
    public List<FeatureVector> get(DBpediaResource resource) throws SearchException {
        List<FeatureVector> vectors = new ArrayList<FeatureVector>();

        // Iterate through the results:
        for (ScoreDoc hit : getHits(mLucene.getQuery(resource))) {
            int docNo = hit.doc;
            TermFreqVector vector = getVector(docNo);
            vectors.add(new LuceneFeatureVector(resource, vector));
        }
        LOG.trace(vectors.size()+" vectors found.");

        // return set of vectors
        return vectors;
    }

    // Returns the first URI that can be found in the document number docNo
    public DBpediaResource getDBpediaResource(int docNo) throws SearchException { //TODO why is this overriding BaseSearcher? can merge?

        FieldSelector fieldSelector = new MapFieldSelector(onlyUriAndTypes);

        LOG.debug("Getting document number "+docNo+"...");
        Document document = getDocument(docNo, fieldSelector);
        String uri = document.get(LuceneManager.DBpediaResourceField.URI.toString());
        if (uri==null)
            throw new SearchException("Cannot find URI for document "+document);

        LOG.debug("Setting URI, types and support...");
        DBpediaResource resource = new DBpediaResource(uri);
        resource.setTypes( getDBpediaTypes(document) );
        resource.setSupport( getSupport(document) ); //TODO this can be optimized for time performance by adding a support field. (search for the most likely URI then becomes a bit more complicated)

        //LOG.debug("uri:"+uri);
        return resource;
    }

    // Returns the number of URIs in document number docNo:
    // represents the number of times the URI was seen in the training data
    public int getSupport(Document document) throws SearchException {
         //TODO can this be optimized for time performance by adding a support field?
        return document.getFields(LuceneManager.DBpediaResourceField.URI.toString()).length;  // number of URI fields in this document;
    }

    // Returns a list of DBpediaTypes that are registered in the index in document number docNo.
    // Duplicates are not removed.
    // CAUTION: sorting is not guaranteed! (but should be fine (Max thinks) if an order was given when indexing (typically from least to most specific)
    public List<DBpediaType> getDBpediaTypes(Document document) throws SearchException {
        String[] types = document.getValues(LuceneManager.DBpediaResourceField.TYPE.toString());
        List<DBpediaType> typesList = new ArrayList<DBpediaType>();
        for (String t : types) {
            typesList.add(new DBpediaType(t));
        }
        return typesList;
    }

    /**
     * Generates explanations for how a given SurfaceFormOccurrence has been disambiguated into a DBpediaResourceOccurrence
     * @param goldStandardOccurrence
     * @param nExplanations
     * @return a list of explanations
     * @throws SearchException
     */
    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException {
        List<Explanation> explanations = new ArrayList<Explanation>();
        int i = 0;
        // We know that the disambiguate should have found the URI in goldStandardOccurrence
        int shouldHaveFound = getHits(goldStandardOccurrence.resource())[0].doc;
        LOG.debug("Explanation: ");
        LOG.debug("\t"+goldStandardOccurrence.resource()+" has doc no. = "+shouldHaveFound);
        //Search the index for the surface form in goldStandardOccurrence
        //for (ScoreDoc hit : getHits() { // query for a surface form
            Query actuallyFound = mLucene.getQuery(goldStandardOccurrence.surfaceForm(), goldStandardOccurrence.context()); // for each of the vectors found, we'll give an explanation
            try {
                LOG.debug("\tSurface Form Occurrence looks like: "+actuallyFound.toString());
                //Returns an Explanation that describes how doc(actuallyFound) scored against weight/query (shouldHaveFound).
                explanations.add(mSearcher.explain(actuallyFound, shouldHaveFound));
            } catch (IOException e) {
                throw new SearchException("Error generating explanation for occurrence "+goldStandardOccurrence, e);
            }
            i++;
            //if (i >= nExplanations) break;
        //}
        return explanations;
    }

    public long getNumberOfResources() throws IOException {
        return mSearcher.maxDoc();
//        long total = 0;
//        try {
//            total = mSearcher.maxDoc();
//        } catch (IOException e) {
//            LOG.error("Error reading number of resources. "+e.getMessage());
//        }
//        return total;
    }
}
