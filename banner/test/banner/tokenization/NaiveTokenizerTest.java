/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tokenization;

import java.util.List;

import org.junit.Test;

import banner.Sentence;
import banner.tokenization.NaiveTokenizer;
import banner.tokenization.Token;
import banner.util.TrieTest;

import junit.framework.JUnit4TestAdapter;
import static org.junit.Assert.assertEquals;

public class NaiveTokenizerTest {

	@Test
	public void testSimple() {
		NaiveTokenizer tokenizer = new NaiveTokenizer();
		Sentence sentence = new Sentence("AA 12 - AA12 AA-12 (12AA) 12.-.");
		tokenizer.tokenize(sentence);
		List<Token> tokens = sentence.getTokens();

		assertEquals(0, tokens.get(0).getStart());
		assertEquals(2, tokens.get(0).getEnd());
		assertEquals("AA", tokens.get(0).getText());

		assertEquals(3, tokens.get(1).getStart());
		assertEquals(5, tokens.get(1).getEnd());
		assertEquals("12", tokens.get(1).getText());

		assertEquals(6, tokens.get(2).getStart());
		assertEquals(7, tokens.get(2).getEnd());
		assertEquals("-", tokens.get(2).getText());

		assertEquals(8, tokens.get(3).getStart());
		assertEquals(10, tokens.get(3).getEnd());
		assertEquals("AA", tokens.get(3).getText());

		assertEquals(10, tokens.get(4).getStart());
		assertEquals(12, tokens.get(4).getEnd());
		assertEquals("12", tokens.get(4).getText());

		assertEquals(13, tokens.get(5).getStart());
		assertEquals(15, tokens.get(5).getEnd());
		assertEquals("AA", tokens.get(5).getText());

		assertEquals(15, tokens.get(6).getStart());
		assertEquals(16, tokens.get(6).getEnd());
		assertEquals("-", tokens.get(6).getText());

		assertEquals(16, tokens.get(7).getStart());
		assertEquals(18, tokens.get(7).getEnd());
		assertEquals("12", tokens.get(7).getText());

		assertEquals(19, tokens.get(8).getStart());
		assertEquals(20, tokens.get(8).getEnd());
		assertEquals("(", tokens.get(8).getText());

		assertEquals(20, tokens.get(9).getStart());
		assertEquals(22, tokens.get(9).getEnd());
		assertEquals("12", tokens.get(9).getText());

		assertEquals(22, tokens.get(10).getStart());
		assertEquals(24, tokens.get(10).getEnd());
		assertEquals("AA", tokens.get(10).getText());

		assertEquals(24, tokens.get(11).getStart());
		assertEquals(25, tokens.get(11).getEnd());
		assertEquals(")", tokens.get(11).getText());

		assertEquals(26, tokens.get(12).getStart());
		assertEquals(28, tokens.get(12).getEnd());
		assertEquals("12", tokens.get(12).getText());

		assertEquals(28, tokens.get(13).getStart());
		assertEquals(29, tokens.get(13).getEnd());
		assertEquals(".", tokens.get(13).getText());

		assertEquals(29, tokens.get(14).getStart());
		assertEquals(30, tokens.get(14).getEnd());
		assertEquals("-", tokens.get(14).getText());

		assertEquals(30, tokens.get(15).getStart());
		assertEquals(31, tokens.get(15).getEnd());
		assertEquals(".", tokens.get(15).getText());
	}

	/**
	 * JUnit3 test adapter, this will allow the junit 4 test to be run under the
	 * current version of ant
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TrieTest.class);
	}
}
