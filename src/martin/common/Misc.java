package martin.common;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class Misc {

	/**
	 * @param arr
	 * @param separator
	 * @return a string of all string representations of the objects in arr, separated by separator
	 */
	public static String implode(Object[] arr, String separator){
		if (arr.length == 0)
			return "";

		StringBuffer sb = new StringBuffer();

		sb.append(arr[0].toString());

		for (int i = 1; i < arr.length; i++)
			sb.append(separator + arr[i].toString());

		return sb.toString();
	}

	/**
	 * @param set
	 * @param separator
	 * @return a string of all string representations of the objects in set, separated by separator
	 */
	public static String implode(Set<String> set, String separator){
		if (set.size() == 0)
			return "";

		StringBuffer sb = null;


		for (String s : set){
			if (sb == null)
				sb = new StringBuffer(s);
			else
				sb.append(separator + s);
		}

		if (sb != null)
			return sb.toString();
		else
			return null;
	}

	/**
	 * 
	 * @param list
	 * @return list, sorted
	 */
	public static ArrayList sort(List list){
		Object[] arr = new Object[list.size()];
		int c = 0;
		for (Object o : list)
			arr[c++] = o;
		Arrays.sort(arr);
		ArrayList res = new ArrayList(arr.length);
		for (Object o : arr)
			res.add(o);
		return res;			
	}

	public static String downloadURL(URL url){
		StringBuffer sb = new StringBuffer();

		try{
			InputStream s = url.openStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(s));

			String line;
			while ((line = r.readLine()) != null)
				sb.append(line + "\n");

			s.close();

		} catch (Exception e){
			System.err.println(e.toString());
			e.printStackTrace();
			System.exit(-1);
		}

		return sb.toString();
	}

	public static double[][] loadCSV(File file){
		ArrayList<double[]> retres = new ArrayList<double[]>();

		try{
			BufferedReader inStream = new BufferedReader(new FileReader(file));


			String line = inStream.readLine();

			while (line != null){
				String[] lines = line.split(",");
				double[] vals = new double[lines.length];

				for (int i = 0; i < lines.length; i++)
					vals[i] = Double.parseDouble(lines[i]);

				retres.add(vals);

				line = inStream.readLine();
			}

			inStream.close();
		} catch (FileNotFoundException e){
			System.err.println("Could not find file " + file.getAbsolutePath());
			System.exit(-1);
		} catch (IOException e){
			System.err.println("IO exception with file " + file.getAbsolutePath());
			System.exit(-1);
		}

		return retres.toArray(new double[0][0]);
	}

	/**
	 * rounds a double value to a given number of digits
	 * @param data
	 * @param numFractionDigits
	 * @return data, rounded
	 */
	public static double round(double data, int numFractionDigits){
		data = ((double)((int) (data*Math.pow(10, numFractionDigits))))/Math.pow(10, numFractionDigits);

		return data;
	}

	/**
	 * Pads integer data with leading zeros until a desired length is reached. This is useful for e.g. dates, where 2009-07-30 looks better than 2009-7-30.
	 * @param data
	 * @param desiredLength
	 * @return a string of length desiredLength consisting of data and a number of leading zeros
	 */
	public static String addzeros(int data, int desiredLength){
		String retres = ""+Math.abs(data);

		if (data<0)
			desiredLength--;

		if (desiredLength < retres.length())
			throw new IllegalStateException("desiredLength < length of data");

		retres = replicateChar('0', desiredLength-retres.length()) + retres;

		if (data<0)
			retres = '-'+retres;

		return retres;
	}

	/**
	 * will try to detect whether a text file is in utf-8 or windows-1252 format
	 * @param instream
	 * @return the name for the encoding used in the stream
	 */
	public static String detectEncoding(BufferedReader instream){
		int utf = 0;
		int win = 0;

		try{
			int readchar = instream.read();

			while (readchar != -1){
				if (readchar == 13){
					int next = instream.read();
					if (next == 10)
						win++;
				}
				if (readchar == 10)
					utf++;

				readchar = instream.read();
			}
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}

		if (utf>win)
			return "utf-8";
		if (win>utf)
			return "windows-1252";

		return "";
	}

	public static String replicateChar(char c, int n){
		String str = new String();

		for (int i = 0; i < n; i++)
			str += c;

		return str;
	}

	public static Set<String> loadStringSetFromFile(File file) {
		if (file == null)
			return null;

		Set<String> res = new HashSet<String>();
		StreamIterator fi = new StreamIterator(file);
		for (String s : fi)
			if (!s.startsWith("#"))
				res.add(s);

		return res;
	}

	public static Map<String,String> loadMap(File file){
		return loadMap(file, false);
	}
	
	public static Map<String, Set<String>> loadMapSet(File file, boolean reverse) {
		if (file == null)
			return null;

		Map<String,Set<String>> res = new HashMap<String,Set<String>>();
		StreamIterator data = new StreamIterator(file);
		Pattern p = Pattern.compile("\t");
		
		for (String s : data){
			String[] fs = p.split(s);
			
			if (fs.length != 2){
				System.err.println("The line '" + s + "' in file " + file.getAbsolutePath() + " must contain two fields, separated by a tab-character.");
				System.exit(-1);
			}

			String k = reverse ? fs[1] : fs[0];
			String v = reverse ? fs[0] : fs[1];

			if (!res.containsKey(k))
				res.put(k, new HashSet<String>(4));
			
			res.get(k).add(v);
		}
		return res;
	}

	public static Map<String, String> loadMap(File file, boolean reverse) {
		if (file == null)
			return null;

		Map<String,String> res = new HashMap<String,String>();
		StreamIterator data = new StreamIterator(file);
		Pattern p = Pattern.compile("\t");
		
		for (String s : data){
			String[] fs = p.split(s);
			if (fs.length != 2){
				System.err.println("The line '" + s + "' in file " + file.getAbsolutePath() + " must contain two fields, separated by a tab-character.");
				System.exit(-1);
			}
			if (!reverse && res.containsKey(fs[0])){
				System.err.println("The key " + fs[0] + " in file " + file.getAbsolutePath() + " must only appear once.");
				System.exit(-1);
			}
			if (reverse && res.containsKey(fs[1])){
				System.err.println("The (reversed) key " + fs[1] + " in file " + file.getAbsolutePath() + " must only appear once.");
				System.exit(-1);
			}

			if (reverse)
				res.put(fs[1], fs[0]);
			else
				res.put(fs[0], fs[1]);
		}
		return res;
	}

	public static void writeFile(File file, String text) {
		try{
			BufferedWriter outStream = new BufferedWriter(new FileWriter(file));
			outStream.write(text);
			outStream.close();
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static String loadFile(File file) {
		StreamIterator c = new StreamIterator(file);
		StringBuffer sb = new StringBuffer();
		for (String s : c)
			sb.append(s + "\n");
		return sb.toString();
	}
}
