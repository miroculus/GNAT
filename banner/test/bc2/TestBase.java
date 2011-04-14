/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package bc2;

import org.junit.Test;

import junit.framework.JUnit4TestAdapter;
import banner.tagging.DictionaryTaggerTest;
import static org.junit.Assert.assertEquals;


public class TestBase {

	@Test
	public void testConvertFullIndex2NonWSIndex()
	{
		String str = "ABC  123";
		assertEquals(0, Base.convertFullIndex2NonWSIndex(str, 0));
		assertEquals(1, Base.convertFullIndex2NonWSIndex(str, 1));
		assertEquals(2, Base.convertFullIndex2NonWSIndex(str, 2));
		assertEquals(-1, Base.convertFullIndex2NonWSIndex(str, 3));
		assertEquals(-1, Base.convertFullIndex2NonWSIndex(str, 4));
		assertEquals(3, Base.convertFullIndex2NonWSIndex(str, 5));
		assertEquals(4, Base.convertFullIndex2NonWSIndex(str, 6));
		assertEquals(5, Base.convertFullIndex2NonWSIndex(str, 7));
		str = " ABC 123 ";
		assertEquals(-1, Base.convertFullIndex2NonWSIndex(str, 0));
		assertEquals(0, Base.convertFullIndex2NonWSIndex(str, 1));
		assertEquals(1, Base.convertFullIndex2NonWSIndex(str, 2));
		assertEquals(2, Base.convertFullIndex2NonWSIndex(str, 3));
		assertEquals(-1, Base.convertFullIndex2NonWSIndex(str, 4));
		assertEquals(3, Base.convertFullIndex2NonWSIndex(str, 5));
		assertEquals(4, Base.convertFullIndex2NonWSIndex(str, 6));
		assertEquals(5, Base.convertFullIndex2NonWSIndex(str, 7));
		assertEquals(-1, Base.convertFullIndex2NonWSIndex(str, 8));
	}
	
	@Test
	public void testConvertNonWSIndex2FullIndex()
	{
		String str = "ABC  123";
		assertEquals(0, Base.convertNonWSIndex2FullIndex(str, 0));
		assertEquals(1, Base.convertNonWSIndex2FullIndex(str, 1));
		assertEquals(2, Base.convertNonWSIndex2FullIndex(str, 2));
		assertEquals(5, Base.convertNonWSIndex2FullIndex(str, 3));
		assertEquals(6, Base.convertNonWSIndex2FullIndex(str, 4));
		assertEquals(7, Base.convertNonWSIndex2FullIndex(str, 5));
		str = " ABC 123 ";
		assertEquals(1, Base.convertNonWSIndex2FullIndex(str, 0));
		assertEquals(2, Base.convertNonWSIndex2FullIndex(str, 1));
		assertEquals(3, Base.convertNonWSIndex2FullIndex(str, 2));
		assertEquals(5, Base.convertNonWSIndex2FullIndex(str, 3));
		assertEquals(6, Base.convertNonWSIndex2FullIndex(str, 4));
		assertEquals(7, Base.convertNonWSIndex2FullIndex(str, 5));
	}
	
	/**
	 * JUnit3 test adapter, this will allow the junit 4 test to be run under the
	 * current version of ant
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(DictionaryTaggerTest.class);
	}

}
