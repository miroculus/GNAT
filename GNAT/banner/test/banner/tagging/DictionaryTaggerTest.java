/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging;

import java.util.List;

import junit.framework.JUnit4TestAdapter;

import banner.Sentence;
import banner.tagging.DictionaryTagger;
import banner.tagging.Mention;
import banner.tagging.MentionType;
import banner.tokenization.NaiveTokenizer;

import org.junit.Test;



import static org.junit.Assert.assertEquals;

public class DictionaryTaggerTest {

	@Test
	public void testNo1() {
		NaiveTokenizer tokenizer = new NaiveTokenizer();
		DictionaryTagger dictionaryTagger = new DictionaryTagger(tokenizer,
				true);
		assertEquals(0, dictionaryTagger.size());
		dictionaryTagger.add("GENES", MentionType.getType("GENE"));
		assertEquals(1, dictionaryTagger.size());
		dictionaryTagger.add("melanogenesis", MentionType.getType("BIOP"));
		assertEquals(2, dictionaryTagger.size());
		dictionaryTagger.add("human", MentionType.getType("ORGM"));
		assertEquals(3, dictionaryTagger.size());
		dictionaryTagger.add("lung cancers", MentionType.getType("DISE"));
		assertEquals(4, dictionaryTagger.size());
		Sentence sentence = new Sentence(
				"What [GENES] are involved in the melanogenesis of human lung cancers?");
		tokenizer.tokenize(sentence);
		assertEquals(14, sentence.getTokens().size());
		assertEquals(0, sentence.getMentions().size());
		dictionaryTagger.tag(sentence);
		List<Mention> mentions = sentence.getMentions();
		assertEquals(4, mentions.size());
		assertEquals(1, mentions.get(0).getTokens().size());
		assertEquals(1, mentions.get(1).getTokens().size());
		assertEquals(1, mentions.get(2).getTokens().size());
		assertEquals(2, mentions.get(3).getTokens().size());
	}

	@Test
	public void testNo2() {
		NaiveTokenizer tokenizer = new NaiveTokenizer();
		DictionaryTagger dictionaryTagger = new DictionaryTagger(tokenizer,
				true);
		assertEquals(0, dictionaryTagger.size());
		dictionaryTagger.add("GENES", MentionType.getType("GENE"));
		assertEquals(1, dictionaryTagger.size());
		dictionaryTagger.add("involved in", MentionType.getType("ACTION")); // Sentence does
		// not include
		// this text
		dictionaryTagger.add("axon guidance", MentionType.getType("BIOP"));
		dictionaryTagger.add("C.elegans", MentionType.getType("ORGM"));
		Sentence sentence = new Sentence(
				"What [GENES] are involved axon guidance in C.elegans?");
		tokenizer.tokenize(sentence);
		assertEquals(13, sentence.getTokens().size());
		assertEquals(0, sentence.getMentions().size());
		dictionaryTagger.tag(sentence);
		List<Mention> mentions = sentence.getMentions();
		assertEquals(3, mentions.size());
		assertEquals(1, mentions.get(0).getTokens().size());
		assertEquals(2, mentions.get(1).getTokens().size());
		assertEquals(3, mentions.get(2).getTokens().size());

	}

	/**
	 * JUnit3 test adapter, this will allow the junit 4 test to be run under the
	 * current version of ant
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(DictionaryTaggerTest.class);
	}

}
