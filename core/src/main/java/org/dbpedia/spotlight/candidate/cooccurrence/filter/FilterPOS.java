package org.dbpedia.spotlight.candidate.cooccurrence.filter;


import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.TaggedToken;

import java.util.List;

/**
 * Part-of-Speech Filter.
 *
 * This file presumes the part-of-speech tag set of the Brown Corpus
 * ({@see http://icame.uib.no/brown/bcm.html#bc6}).
 *
 * @author Joachim Daiber
 */

public class FilterPOS extends Filter {

	/**
	 * Part of Speech pattern for valid unigram candidates.
	 *
	 * Candidates are any nouns (except adverbial nouns like "today" [nr])
	 */
	final static String UNIGRAM_POS_WHITELIST_PATTERN = "(n[^r]*|fw.*)";


	/**
	 * Check if a unigram occurrence of a surface form is on the POS blacklist.
	 *
	 * @param surfaceFormOccurrence
	 * @return
	 */

	private boolean isOnUnigramBlacklist(SurfaceFormOccurrence surfaceFormOccurrence) {

		List<TaggedToken> taggedTokens = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider()
				.getTaggedTokens(surfaceFormOccurrence);

		if(taggedTokens.size() != 1) {
			return false;
		} else {
			return isOnUnigramBlacklist(taggedTokens.get(0).getPOSTag());
		}
	}

	public boolean isOnUnigramBlacklist(String posTag) {
		if(posTag == null)
			return false;
		else
			return !(posTag.matches(UNIGRAM_POS_WHITELIST_PATTERN) && !posTag.equals("nil"));
	}


	@Override
	public boolean applies(SurfaceFormOccurrence surfaceFormOccurrence) {

		switch (FilterTermsize.getTermSize(surfaceFormOccurrence.surfaceForm())) {

			case unigram:
				return !isOnUnigramBlacklist(surfaceFormOccurrence);
			default:
				return false;

		}

	}

	@Override
	public String name() {
		return "Filter for non-term part-of-speech patterns.";
	}


}
