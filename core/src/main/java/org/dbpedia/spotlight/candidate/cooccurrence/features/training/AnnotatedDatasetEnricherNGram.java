package org.dbpedia.spotlight.candidate.cooccurrence.features.training;

import org.dbpedia.spotlight.candidate.cooccurrence.CandidateUtil;
import org.dbpedia.spotlight.candidate.cooccurrence.features.AttributesNGram;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterPattern;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.json.JSONException;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates training data for an ngram classifier.
 *
 * {@see AnnotatedDatasetEnricher}
 *
 * @author Joachim Daiber
 */
public class AnnotatedDatasetEnricherNGram extends AnnotatedDatasetEnricher {

	public AnnotatedDatasetEnricherNGram(SpotlightConfiguration configuration) throws ConfigurationException, IOException, InitializationException {
		super();

		FilterTermsize filterTermsize = new FilterTermsize(FilterTermsize.Termsize.unigram);
		FilterPattern filterPattern = new FilterPattern();
		filterTermsize.inverse();

		filters.add(filterTermsize);
		filters.add(filterPattern);
		header = new Instances("NgramTraining", buildAttributeList(), buildAttributeList().size());

		OccurrenceDataProviderSQL.initialize(configuration.getSpotterConfiguration());
		dataProvider  = OccurrenceDataProviderSQL.getInstance();

	}

	@Override
	public Instance buildInstance(OccurrenceInstance trainingInstance, OccurrenceDataProvider dataProvider) {
		DenseInstance instance = new DenseInstance(buildAttributeList().size());
		instance.setDataset(header);
		header.attribute(AttributesNGram.i(AttributesNGram.ends_with)).setWeight(10);
		return CandidateUtil.buildInstanceNGram(trainingInstance, dataProvider, instance, true);
	}

	@Override
	protected ArrayList<Attribute> buildAttributeList() {

		return AttributesNGram.attributeList;

	}

	public static void main(String[] args) throws ConfigurationException, IOException, JSONException, InitializationException {

		AnnotatedDatasetEnricherNGram annotatedDatasetEnricherNGram = new AnnotatedDatasetEnricherNGram(new SpotlightConfiguration("conf/server.properties"));

		OccurrenceDataset trainingData = new OccurrenceDataset(new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/second_run.json"), OccurrenceDataset.Format.JSON);
		annotatedDatasetEnricherNGram.writeDataset(trainingData, new File("/Users/jodaiber/Desktop/NgramTraining.xrff"));

	}



}
