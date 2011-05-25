package org.dbpedia.spotlight.candidate.cooccurrence.features.training;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;

/**
 * TrainingDataGenerator Tester.
 *
 * @author jodaiber

 */
public class DatasetGeneratorTest extends TestCase {

	private DatasetGenerator trainingDataGenerator;

	public DatasetGeneratorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
		LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());
		trainingDataGenerator = new DatasetGeneratorUnigram();

    }

    public void tearDown() throws Exception {
        super.tearDown();
        
    }
	

    /**
     *
     * Method: findExampleSentenceGeneric(String surfaceForm)
     *
     */
    public void testFindExampleSentenceGeneric() throws Exception {
		OccurrenceInstance occurrenceInstance
				= trainingDataGenerator.findExampleSentenceGeneric("review");

		assertEquals("review", occurrenceInstance.getSurfaceForm());
	}

    /**
     *
     * Method: findExampleSentenceWikipedia(String surfaceForm, DBpediaResource dbpediaResource)
     *
     */
    public void testFindExampleSentenceWikipedia() throws Exception {

		OccurrenceInstance exampleSentenceWikipedia
				= trainingDataGenerator.findExampleSentenceWikipedia(new SurfaceForm("Berlin"), new DBpediaResource("Berlin"));

		assertEquals("Berlin", exampleSentenceWikipedia.getAnnotationURI());

    }



    public static Test suite() {
        return new TestSuite(DatasetGeneratorTest.class);
    }
}
