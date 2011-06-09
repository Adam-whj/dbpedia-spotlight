package org.dbpedia.spotlight.candidate.cooccurrence.weka.ukwac;

import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.weka.InstanceBuilderUnigram;

/**
 * @author Joachim Daiber
 */
public class InstanceBuilderUnigramUKWAC extends InstanceBuilderUnigram {
	
	public InstanceBuilderUnigramUKWAC(OccurrenceDataProvider dataProvider) {
		super(dataProvider);

		this.unigramCorpusMax 	= 20000;
		this.unigramWebMin 		= 60000;
		this.bigramLeftWebMin 	= 20;
		this.bigramRightWebMin 	= 20;
		this.trigramLeftWebMin 	= 40;
		this.trigramMiddleWebMin= 40;
		this.trigramRightWebMin = 40;

	}
	
}
