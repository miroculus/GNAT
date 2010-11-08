/*
 * Created on 27.09.2004
 *
 */
package gnat.alignment;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An extension of the global alignment algorithm defined in the parent class. 
 * 
 * @author plake
 *
 */
public class LocalAlignment extends Alignment{

	
	private float restartValue = 0;
	private float[][] table;
	
	public int[] startOfPhrase1 = new int[10]; //jh
	public int[] startOfPhrase2 = new int[10];
	
	/** **/
	//Calendar cal;
	long traceback_starttime;
	int ctraceback;

	/** Stores the max. number of tracebacks needed for a proper result. Useful when multiple alignment
	 * are executed using the same instance of this class.*/
	public int maxtracebacks = 0; // max observed: 79260  // aborted after 190101,180610,189984

	/** An alignment can be assigned a timeout value. After this is reached,
	 * the alignment is aborted and align(Phrase, Phrase) returns Float.NaN. Default: 2000.
	 */
	public int maxtimeinmillis = 2000;
	
	
	public int getPosition1 () {
	    return this.startOfPhrase1[0];
	}
	
	public int getPosition2 () {
	    return this.startOfPhrase2[0];
	}
	
	
	/**
	 * 
	 * 
	 * */
	public LocalAlignment (SubstitutionMatrix sm){
		super(sm);
		this.restartValue = 0;
		maxtracebacks = 0;

	}
	
	/**
	 * 
	 * 
	 * */
	public LocalAlignment(SubstitutionMatrix sm, float restartValue){
		super(sm);
		this.restartValue = restartValue;
		maxtracebacks = 0;

	}
	

	/**
	 * Aligns two phrases and returns the alignment score.<br>
	 * When the max. time (maxtimeinmillis) is reached, alignment is aborted
	 * and align() returns Float.NaN.
	 */
	@SuppressWarnings("unchecked")
	public float align(Phrase p1, Phrase p2){			
		
		table = makeTable(p1, p2);
		int[][][] pointer = makePointer(table, p1, p2);

		//int[] highCell = new int[2];
		LinkedList<int[]> highCells  = new LinkedList<int[]>();
		float val = Float.NEGATIVE_INFINITY;
		for(int x=0;x<table.length;x++){
			for(int y=0;y<table[x].length;y++){
				
//				if(table[x][y]==restartValue){
//					pointer[x][y][0] = 0;
//					pointer[x][y][1] = 0;
//					pointer[x][y][2] = 0;
//					pointer[x][y][3] = 0;
//					pointer[x][y][4] = 0;
//					pointer[x][y][5] = 0;
//				}
				
				if(table[x][y]>val){					
					val = table[x][y];
					highCells.clear();
					highCells.add(new int[]{x, y});					
				}else if(table[x][y]==val){
					highCells.add(new int[]{x, y});
				}
			}
		}
		
		//System.err.println("Best high cells have value: "+val);//jh

		//
		traceback_starttime = System.currentTimeMillis();
		ctraceback = 0;
		int counter = 1;

		
		alignments = new LinkedList();
		Iterator it = highCells.iterator();
//		if(highCells.size()>1){
//			System.out.println(highCells.size());
//		}
		
		try {
			while(it.hasNext()){
				int[] cell = (int[]) it.next();
				alignments.addAll(
						traceback(table, pointer, cell[0], cell[1], new LinkedList(), new Phrase(), new Phrase()) );
				counter++;
			}
			

			if (ctraceback > maxtracebacks)
				maxtracebacks = ctraceback;
			
			//System.err.println();
			//System.err.println("# LA.align(): Needed " + ctraceback + " tracebacks");
						
			Phrase[] firstAlignment = (Phrase[])alignments.get(0);
			return getScore(firstAlignment[0], firstAlignment[1]);

		} catch (NullPointerException noe) {
			//System.err.println("# Aborted after " + ctraceback + " tracebacks");
			return Float.NaN;
		}
	}
	
	/**
	 * 
	 * 
	 * */
	public void setRestartValue(float value){
		restartValue = value;
	}
	
	/**
	 * 
	 * 
	 * */
	public float getRestartValue(){
		return restartValue;
	}
	
	/**
	 * 
	 * 
	 * */
	public float[][] makeTable(Phrase p1, Phrase p2){
		float[][] table = new float[p1.length() + 1][p2.length() + 1];

		w1 = p1.getWords();
		w2 = p2.getWords();

		// calculate base conditions
		table[0][0] = 0;
		for (int i = 0; i < table.length - 1; i++) {			
			table[i+1][0] = 0;
		}
		for (int j = 0; j < table[0].length - 1; j++) {			
			table[0][j+1] = 0;
		}

		// calculate subtable
		for (int i = 1; i < table.length; i++) {
			for (int j = 1; j < table[i].length; j++) {
				//table[i][j] = Math.max(table[i - 1][j] + weight(w1[i - 1]), 	table[i][j - 1] + weight(w2[j - 1]));
				table[i][j] =
					Math.max(table[i - 1][j] + 
						sm.getScore(
								w1[i - 1],
								Alignment.WORDGAP), 
						table[i][j - 1] +
						sm.getScore(
								w2[j - 1],
								Alignment.WORDGAP));
				//table[i][j] = Math.max(table[i][j], table[i - 1][j - 1] + weight(w1[i - 1], w2[j - 1]));
				table[i][j] = Math.max(table[i][j], table[i - 1][j - 1] + sm.getScore(w1[i - 1], w2[j - 1]));
				table[i][j] = Math.max(restartValue, table[i][j]);
			}
		}

		//printTable(table);
		return table;
	}
	
	/**
	 * Returns a list of aligned phrases. All traceback paths are considered by recursively calling this method, whenever a cell on the path points to multiple neighbors. 
	 * 
	 * */
	protected List traceback(
		float[][] table,
		int[][][] pointer,
		int i,
		int j,
		List<Phrase[]> list,
		Phrase alignment1,
		Phrase alignment2) {

		//		if (list.size() >= 100000) {
		//			System.out.println("Alignment.traceback(): " + list.size());
		//			//return list;
		//		}
		
		ctraceback++;
		if (System.currentTimeMillis() - traceback_starttime > maxtimeinmillis) {
			System.err.println(" LA.traceback(): stopping after " + (System.currentTimeMillis() - traceback_starttime) + "ms.");
			return null;
		}
		

		while (table[i][j]>restartValue) {

			//			System.out.print("now in cell: "+i+", "+j);

			boolean target1 = false;
			boolean target2 = false;
			boolean target3 = false;

			int ni = pointer[i][j][4];
			int nj = pointer[i][j][5];

			if (ni == i - 1 && nj == j - 1) {
				target1 = true;
			}

			ni = pointer[i][j][0];
			nj = pointer[i][j][1];

			if (ni == i && nj == j - 1) {
				if (target1) {
					Phrase newa1 = new Phrase(alignment1.getWordList());
					Phrase newa2 = new Phrase(alignment2.getWordList());
					newa1.append(WORDGAP);
					newa2.append(w2[j - 1]);
					traceback(table, pointer, ni, nj, list, newa1, newa2);
				} else {
					target2 = true;
				}
			}

			ni = pointer[i][j][2];
			nj = pointer[i][j][3];

			if (ni == i - 1 && nj == j) {
				if (target1 || target2) {
					Phrase newa1 = new Phrase(alignment1.getWordList());
					Phrase newa2 = new Phrase(alignment2.getWordList());
					newa1.append(w1[i - 1]);
					newa2.append(WORDGAP);
					traceback(table, pointer, ni, nj, list, newa1, newa2);
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

		//System.err.println("Ended with i="+i+" and j="+j);//jh, to get position of consensus
		this.startOfPhrase1[0] = i;
		this.startOfPhrase2[0] = j;
		
		list.add(new Phrase[] { alignment1.reverse(), alignment2.reverse()});

		return list;
	}

	/**
	 * Returns the an array of length two, containing two aligned phrases. Only one traceback path is considered.
	 * 
	 * */
	protected Phrase[] singleTraceback(float[][] table, int[][][] pointer, int i, int j) {
		//List l = new LinkedList();
		Phrase alignment1 = new Phrase();
		Phrase alignment2 = new Phrase();
		while (table[i][j]>restartValue) {

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

		//l.add(new Phrase[] { alignment1.reverse(), alignment2.reverse()});
		return new Phrase[]{alignment1.reverse(), alignment2.reverse()};
		//return l;
	}
		
}
