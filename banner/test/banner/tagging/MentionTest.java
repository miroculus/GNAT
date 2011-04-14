/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging;

import static org.junit.Assert.assertEquals;

import java.util.List;

import junit.framework.JUnit4TestAdapter;

import org.junit.Test;

import banner.Sentence;
import banner.tagging.Mention;
import banner.tagging.MentionType;
import banner.tokenization.NaiveTokenizer;
import banner.tokenization.Token;


public class MentionTest {

	@Test
	public void testNo1() {
		NaiveTokenizer tokenizer = new NaiveTokenizer();
		Sentence sentence = new Sentence(
				"What [GENES] are involved in the melanogenesis of human lung cancers?");
		tokenizer.tokenize(sentence);
		List<Token> sentenceTokens = sentence.getTokens();
		assertEquals(14, sentenceTokens.size());
		assertEquals(0, sentence.getMentions().size());
		Mention mention = new Mention(sentence, MentionType.getType("GENE"), 2, 3);
		List<Token> mentionTokens = mention.getTokens();
		assertEquals(1, mentionTokens.size());
		assertEquals(sentenceTokens.get(2), mentionTokens.get(0));
		assertEquals("GENES", mention.getText());
	}

	/**
	 * JUnit3 test adapter, this will allow the junit 4 test to be run under the
	 * current version of ant
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(DictionaryTaggerTest.class);
	}

}
