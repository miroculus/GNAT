/*
 * Created on 14.09.2004
 *
 */
package gnat.alignment;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * This class implements the global alignment algorithm by Needleman-Wunsch.<br>
 * <br>
 * global Alignment: Needleman-Wunsch(1970), Sellers(1974)<br>
 * local Alignment:  Smith-Waterman(1981)<br>
 *
 *
 * @author Conrad Plake, Joerg Hakenberg
 *
 */

public class Alignment {

    public int[] startOfPhrase1 = new int[10];
    public int[] startOfPhrase2 = new int[10];
	public int getPosition1 () {
	    return this.startOfPhrase1[0];
	}

	public int getPosition2 () {
	    return this.startOfPhrase2[0];
	}

	public static Word WORDGAP = new Word("-", "-");

	protected List alignments;

	protected Word[] w1, w2;

	//protected int GAPWEIGHT = -1;

	protected SubstitutionMatrix sm;

	/**
	 * 	Creates a new alignment object.
	 *
	 * */
	public Alignment() {
	}

	/**
	 * 	Creates a new alignment object.
	 *
	 * */
	public Alignment(SubstitutionMatrix sm) {
		this.sm = sm;
	}

	/**
	 * Creates a new alignment object for two phrases.
	 *
	 * */
	public Alignment(SubstitutionMatrix sm, Phrase p1, Phrase p2) {
		this.sm = sm;
		align(p1, p2);
	}

	/**
	 * Returns the substitution matrix.
	 * */
	public SubstitutionMatrix getSubstitutionsmatrix(){
		return sm;
	}

	/**
	 * Returns the dynamic programming table for two phrases.
	 *
	 * */
	public float[][] makeTable(Phrase p1, Phrase p2) {
		float[][] table = new float[p1.length() + 1][p2.length() + 1];

		w1 = p1.getWords();
		w2 = p2.getWords();

		// calculate base conditions
		table[0][0] = 0;
		for (int i = 0; i < table.length - 1; i++) {
			//table[i + 1][0] = table[i][0] + weight(w1[i]);
			table[i + 1][0] = table[i][0] + sm.getScore(w1[i], Alignment.WORDGAP);
			//table[i+1][0] = 0;
		}
		for (int j = 0; j < table[0].length - 1; j++) {
			//table[0][j + 1] = table[0][j] + weight(w2[j]);
			table[0][j + 1] = table[0][j] +  sm.getScore(w2[j], Alignment.WORDGAP);
			//table[0][j+1] = 0;
		}

		// calculate subtable
		for (int i = 1; i < table.length; i++) {
			for (int j = 1; j < table[i].length; j++) {
				//table[i][j] =	Math.max(	table[i - 1][j] + weight(w1[i - 1]), 	table[i][j - 1] + weight(w2[j - 1]));
				table[i][j] =	Math.max(	table[i - 1][j] + sm.getScore(w1[i - 1], Alignment.WORDGAP), 	table[i][j - 1] + sm.getScore(w2[j - 1], Alignment.WORDGAP));
				//table[i][j] =	Math.max(	table[i][j], 	table[i - 1][j - 1] + weight(w1[i - 1], w2[j - 1]));
				table[i][j] =	Math.max(	table[i][j], 	table[i - 1][j - 1] + sm.getScore(w1[i - 1], w2[j - 1]));
			}
		}

		//printTable(table);
		return table;
	}

	/**
	 *	Returns a 3-dim array of integers, denoting pointers between cells in the given table.<BR>
	 * Example: pointers from cell (i,j):<BR>
	 * LEFT pointer:<BR>
	 * i = pointers[i][j][0]<BR>
	 * j-1 = pointers[i][j][1]<BR>
	 * UP pointer:<BR>
	 * i-1 = pointers[i][j][2]<BR>
	 * j = pointers[i][j][3]<BR>
	 * LEFT/UP pointer:<BR>
	 * i-1 = pointers[i][j][4]<BR>
	 * j-1 = pointers[i][j][5]<BR>
	 * */
	public int[][][] makePointer(float[][] table, Phrase p1, Phrase p2) {
		int c = 0;
		// make pointers
		int[][][] pointer = new int[p1.length() + 1][p2.length() + 1][6];
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < table[i].length; j++) {

				if (j > 0
					//&& table[i][j] == table[i][j - 1] + weight(w2[j - 1])) {
					&& table[i][j] == table[i][j - 1] + sm.getScore(w2[j - 1], Alignment.WORDGAP)) {
					pointer[i][j][0] = i;
					pointer[i][j][1] = j - 1;
					c++;
				}
				if (i > 0
					//&& table[i][j] == table[i - 1][j] + weight(w1[i - 1])) {
					&& table[i][j] == table[i - 1][j] + sm.getScore(w1[i - 1], Alignment.WORDGAP)) {
					pointer[i][j][2] = i - 1;
					pointer[i][j][3] = j;
					c++;
				}
				if (j > 0
					&& i > 0
					&& table[i][j]
						//== table[i - 1][j - 1] + weight(w1[i - 1], w2[j - 1])) {
						== table[i - 1][j - 1] + sm.getScore(w1[i - 1], w2[j - 1])) {
					pointer[i][j][4] = i - 1;
					pointer[i][j][5] = j - 1;
					c++;
				}
			}
		}
		//System.out.println("align(): #pointers="+c);
		return pointer;
	}

	/**
	 * Aligns two phrases and returns the maximum score.
	 *
	 * */
	public float align(Phrase p1, Phrase p2) {
	    //System.out.println("%Starting to align.");
	    alignments =
			traceback(
				makePointer(makeTable(p1, p2), p1, p2),
				p1.length(),
				p2.length(),
				new LinkedList<Phrase[]>(),
				new Phrase(),
				new Phrase());

		Phrase[] firstAlignment = (Phrase[]) alignments.get(0);
		//float score = ;//jh, tried to save mem by setting firstAlign to null, using a float only
		//firstAlignment = null; //jh
		//System.out.println("%Done with align.");
		return getScore(firstAlignment[0], firstAlignment[1]);
	}

	/**
	 * Aligns two phrases and returns the maximum score.
	 *
	 * */
	public float alignWithLimitedGaps(Phrase p1, Phrase p2, int maxGaps) {
	    //System.out.println("%Starting to align.");
	    alignments =
			gapLimitedTraceback(
				makePointer(makeTable(p1, p2), p1, p2),
				p1.length(),
				p2.length(),
				new LinkedList<Phrase[]>(),
				new Phrase(),
				new Phrase(),
				0,
				maxGaps);

		Phrase[] firstAlignment = (Phrase[]) alignments.get(0);
		//float score = ;//jh, tried to save mem by setting firstAlign to null, using a float only
		//firstAlignment = null; //jh
		//System.out.println("%Done with align.");
		return getScore(firstAlignment[0], firstAlignment[1]);
	}

	/**
	 * Returns a list of aligned phrases. All traceback paths are considered by recursively calling this method, whenever a cell on the path points to multiple neighbors.
	 *
	 * */
	protected List traceback(
		int[][][] pointer,
		int i,
		int j,
		List<Phrase[]> list,
		Phrase alignment1,
		Phrase alignment2) {

	    try {
		//		if (list.size() >= 100000) {
		//			System.out.println("Alignment.traceback(): " + list.size());
		//			//return list;
		//		}
	    //System.out.println("%Starting traceback.");
	    Phrase newa1;
	    Phrase newa2;
	    boolean target1 = false;
	    boolean target2 = false;
	    boolean target3 = false;
	    int ni;
	    int nj;

		while (i > 0 || j > 0) {

			//			System.out.print("now in cell: "+i+", "+j);

			target1 = false;
			target2 = false;
			target3 = false;

			ni = pointer[i][j][4];
			nj = pointer[i][j][5];

			if (ni == i - 1 && nj == j - 1) {
				target1 = true;
			}

			ni = pointer[i][j][0];
			nj = pointer[i][j][1];

			if (ni == i && nj == j - 1) {
				if (target1) {
					newa1 = new Phrase(alignment1.getWordList()); //jh: init above
					newa2 = new Phrase(alignment2.getWordList());
					newa1.append(WORDGAP);
					newa2.append(w2[j - 1]);
					traceback(pointer, ni, nj, list, newa1, newa2);
				} else {
					target2 = true;
				}
			}

			ni = pointer[i][j][2];
			nj = pointer[i][j][3];

			if (ni == i - 1 && nj == j) {
				if (target1 || target2) {
					newa1 = new Phrase(alignment1.getWordList());//jh
					newa2 = new Phrase(alignment2.getWordList());
					newa1.append(w1[i - 1]);
					newa2.append(WORDGAP);
					traceback(pointer, ni, nj, list, newa1, newa2);
				} else {
					target3 = true;
				}
			}

			if (target1) {
				alignment1.append(w1[i - 1]);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][4];
				nj = pointer[i][j][5];
			}
			if (target2) {
				alignment1.append(WORDGAP);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][0];
				nj = pointer[i][j][1];
			}
			if (target3) {
				alignment1.append(w1[i - 1]);
				alignment2.append(WORDGAP);
				ni = pointer[i][j][2];
				nj = pointer[i][j][3];
			}

			i = ni;
			j = nj;
			//System.out.println("  going to: "+i+", "+j);
		}

		newa1 = null;
		newa2 = null;

		list.add(new Phrase[] { alignment1.reverse(), alignment2.reverse()});

	    } catch (OutOfMemoryError oe) {
	        oe.printStackTrace();
	        System.out.println("OME in traceback routine.");
	    }

		//System.out.println("%Done with traceback.");


		return list;
	}

	/**
	 * Returns a list of aligned phrases. All traceback paths are considered by recursively calling this method, whenever a cell on the path points to multiple neighbors.
	 *
	 * */
	protected List gapLimitedTraceback(
		int[][][] pointer,
		int i,
		int j,
		List<Phrase[]> list,
		Phrase alignment1,
		Phrase alignment2,
		int gapCount,
		int maxGaps) {

		if(gapCount>maxGaps){
			return list;
		}

	    try {
	    Phrase newa1;
	    Phrase newa2;
	    boolean target1 = false;
	    boolean target2 = false;
	    boolean target3 = false;
	    int ni;
	    int nj;

		while (i > 0 || j > 0) {

			target1 = false;
			target2 = false;
			target3 = false;

			ni = pointer[i][j][4];
			nj = pointer[i][j][5];

			if (ni == i - 1 && nj == j - 1) {
				target1 = true;
			}

			ni = pointer[i][j][0];
			nj = pointer[i][j][1];

			if (ni == i && nj == j - 1) {
				if (target1) {
					newa1 = new Phrase(alignment1.getWordList()); //jh: init above
					newa2 = new Phrase(alignment2.getWordList());
					newa1.append(WORDGAP);
					newa2.append(w2[j - 1]);
					gapLimitedTraceback(pointer, ni, nj, list, newa1, newa2, gapCount+1, maxGaps);
				} else {
					target2 = true;
				}
			}

			ni = pointer[i][j][2];
			nj = pointer[i][j][3];

			if (ni == i - 1 && nj == j) {
				if (target1 || target2) {
					newa1 = new Phrase(alignment1.getWordList());//jh
					newa2 = new Phrase(alignment2.getWordList());
					newa1.append(w1[i - 1]);
					newa2.append(WORDGAP);
					gapLimitedTraceback(pointer, ni, nj, list, newa1, newa2, gapCount+1, maxGaps);
				} else {
					target3 = true;
				}
			}

			if (target1) {
				alignment1.append(w1[i - 1]);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][4];
				nj = pointer[i][j][5];
			}
			if (target2) {
				alignment1.append(WORDGAP);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][0];
				nj = pointer[i][j][1];
				gapCount++;
			}
			if (target3) {
				alignment1.append(w1[i - 1]);
				alignment2.append(WORDGAP);
				ni = pointer[i][j][2];
				nj = pointer[i][j][3];
				gapCount++;
			}

			i = ni;
			j = nj;
		}

		newa1 = null;
		newa2 = null;

		list.add(new Phrase[] { alignment1.reverse(), alignment2.reverse()});

	    } catch (OutOfMemoryError oe) {
	        oe.printStackTrace();
	        System.err.println("OME in traceback routine.");
	    }


		return list;
	}

	/**
	 * Returns an array of length two, containing two aligned phrases. Only one traceback path is considered.
	 *
	 * */
	protected Phrase[] singleTraceback(int[][][] pointer, int i, int j) {
		//List l = new LinkedList();
		Phrase alignment1 = new Phrase();
		Phrase alignment2 = new Phrase();
		while (i > 0 || j > 0) {

			int ni = pointer[i][j][0];
			int nj = pointer[i][j][1];

			boolean target1 = false;
			boolean target2 = false;
			boolean target3 = false;

			if (ni == i && nj == j - 1) {
				target1 = true;
			}

			ni = pointer[i][j][2];
			nj = pointer[i][j][3];

			if (ni == i - 1 && nj == j) {
				target2 = true;
			}

			ni = pointer[i][j][4];
			nj = pointer[i][j][5];

			if (ni == i - 1 && nj == j - 1) {
				target3 = true;
			}

			if (target1) {
				alignment1.append(WORDGAP);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][0];
				nj = pointer[i][j][1];
			} else if (target2) {
				alignment1.append(w1[i - 1]);
				alignment2.append(WORDGAP);
				ni = pointer[i][j][2];
				nj = pointer[i][j][3];
			} else if (target3) {
				alignment1.append(w1[i - 1]);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][4];
				nj = pointer[i][j][5];
			}

			i = ni;
			j = nj;
		}

		return new Phrase[]{alignment1.reverse(), alignment2.reverse()};
	}

	/**
	 * Returns an array of length two, containing two aligned phrases. Only one traceback path is considered.
	 *
	 * */
	protected Phrase[] gapLimitedSingleTraceback(int[][][] pointer, int i, int j, int maxGaps) {
		//List l = new LinkedList();
		int gapCount = 0;
		Phrase alignment1 = new Phrase();
		Phrase alignment2 = new Phrase();
		while (i > 0 || j > 0) {

			int ni = pointer[i][j][0];
			int nj = pointer[i][j][1];

			boolean target1 = false;
			boolean target2 = false;
			boolean target3 = false;

			if (ni == i && nj == j - 1) {
				target1 = true;
			}

			ni = pointer[i][j][2];
			nj = pointer[i][j][3];

			if (ni == i - 1 && nj == j) {
				target2 = true;
			}

			ni = pointer[i][j][4];
			nj = pointer[i][j][5];

			if (ni == i - 1 && nj == j - 1) {
				target3 = true;
			}

			if (target3) {
				alignment1.append(w1[i - 1]);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][4];
				nj = pointer[i][j][5];
			}
			else if (target2) {
				alignment1.append(w1[i - 1]);
				alignment2.append(WORDGAP);
				ni = pointer[i][j][2];
				nj = pointer[i][j][3];
				gapCount++;
			}
			else if (target1) {
				alignment1.append(WORDGAP);
				alignment2.append(w2[j - 1]);
				ni = pointer[i][j][0];
				nj = pointer[i][j][1];
				gapCount++;
			}

			if(gapCount > maxGaps){
				return null;
			}

			i = ni;
			j = nj;
		}

		return new Phrase[]{alignment1.reverse(), alignment2.reverse()};
	}


	/**
	 * Prints out values of a given table to the console. Useful for debugging.
	 *
	 * */
	public void printTable(float[][] table) {
		for (int s1 = 0; s1 < table.length; s1++) {
			for (int s2 = 0; s2 < table[s1].length; s2++) {
				System.err.print(table[s1][s2] + "   ");
			}
			System.err.println();
		}
	}

	/**
	 * Prints out values of a given pointer-table (see makePointer) to the console. Useful for debugging.
	 *
	 * */
	public void printPointer(int[][][] table) {
		for (int s1 = 0; s1 < table.length; s1++) {
			for (int s2 = 0; s2 < table[s1].length; s2++) {
				System.err.print("[");
				for (int i = 0; i < table[s1][s2].length - 1; i += 2) {
					System.err.print(
						"("
							+ table[s1][s2][i]
							+ ","
							+ table[s1][s2][i
							+ 1]
							+ ")");
				}
				System.err.print("]");
			}
			System.err.println();
		}
	}

	/**
	 * Prints out values of cell c1,c2 of a given pointer-table (see makePointer) to the console. Useful for debugging.
	 *
	 * */
	protected void printPointer(int[][][] table, int c1, int c2) {

		System.out.print("[");
		for (int i = 0; i < table[c1][c2].length - 1; i += 2) {
			System.out.print(
				"(" + table[c1][c2][i] + "," + table[c1][c2][i + 1] + ")");
		}
		System.err.print("]");
		System.err.println();
	}

	/**
	 * Returns the weight or score for the alignment of two phrases. <BR>
	 * Unlike in method align, only one traceback path is processed to compute the score.
	 *
	 * */
	public float getAlignmentScore(Phrase p1, Phrase p2) {
		Phrase[] firstAlignment =
			singleTraceback(
				makePointer(makeTable(p1, p2), p1, p2),
				p1.length(),
				p2.length());
		return getScore(firstAlignment[0], firstAlignment[1]);
	}

	/**
	 * Returns the weight or score for the alignment of two phrases. <BR>
	 * Unlike in method align, only one traceback path is processed to compute the score.
	 *
	 * */
	public Float getGapLimitedAlignmentScore(Phrase p1, Phrase p2, int maxGaps) {
		Phrase[] firstAlignment =
			gapLimitedSingleTraceback(
				makePointer(makeTable(p1, p2), p1, p2),
				p1.length(),
				p2.length(),
				maxGaps);
		if(firstAlignment==null){
			return null;
		}
		return getScore(firstAlignment[0], firstAlignment[1]);
	}

	/**
	 * Returns the weight or score between to phrases. <BR>
	 * The phrases are assumed to be aligned to each other and thus are of equal length and
	 * may contain gaps.
	 *
	 * */
	public float getScore(Phrase p1, Phrase p2) {
		float w = 0;
		Word[] w1 = p1.getWords();
		Word[] w2 = p2.getWords();
		for (int i = 0; i < w1.length && i < w2.length; i++) {
			w += sm.getScore(w1[i], w2[i]);
		}
		return w;
	}


	/**
	 * Returns a list of alignments for two phrases last computed by method align.<BR>
	 * The list may contain more than one element, because there may be multiple optimal alignment paths.<BR>
	 * Every element in the returned list is an array of phrases with length of two, which contains two aligned phrases.
	 *
	 * */
	public List getAlignments() {
		return alignments;
	}
}