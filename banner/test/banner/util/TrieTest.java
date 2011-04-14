/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.util;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import banner.util.Trie;


import junit.framework.JUnit4TestAdapter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrieTest {

	@Test
	public void testSimple() {
		Trie<String, String> t = new Trie<String, String>();
		assertEquals(null, t.getValue());
		assertEquals(null, t.getChild((String) null));
		assertEquals(0, t.size());

		List<String> keys = new ArrayList<String>();

		keys.add("1");
		assertEquals(null, t.add(keys, "1*"));
		assertEquals("1*", t.getValue(keys));
		assertTrue(t.getChild("1") != null);
		assertTrue(t.getChild("1").getChild("2") == null);
		assertTrue(t.getChild("1a") == null);
		assertEquals(1, t.size());

		keys.add("2");
		assertEquals(null, t.add(keys, "2*"));
		assertEquals("2*", t.getValue(keys));
		assertTrue(t.getChild("1") != null);
		assertTrue(t.getChild("1").getChild("2") != null);
		assertTrue(t.getChild("1").getChild("2").getChild("3") == null);
		assertTrue(t.getChild("1a") == null);
		assertEquals(2, t.size());

		keys.add("3");
		assertEquals(null, t.add(keys, "3*"));
		assertEquals("3*", t.getValue(keys));
		assertTrue(t.getChild("1") != null);
		assertTrue(t.getChild("1").getChild("2") != null);
		assertTrue(t.getChild("1").getChild("2").getChild("3") != null);
		assertTrue(t.getChild("1a") == null);
		assertEquals(3, t.size());

		keys.clear();

		keys.add("1a");
		assertEquals(null, t.add(keys, "1a*"));
		assertEquals("1a*", t.getValue(keys));
		assertTrue(t.getChild("1") != null);
		assertTrue(t.getChild("1").getChild("2") != null);
		assertTrue(t.getChild("1").getChild("2").getChild("3") != null);
		assertTrue(t.getChild("1a") != null);
		assertTrue(t.getChild("1a").getChild("2a") == null);
		assertEquals(4, t.size());
	}

	/**
	 * JUnit3 test adapter, this will allow the junit 4 test to be run under the
	 * current version of ant
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TrieTest.class);
	}

}
