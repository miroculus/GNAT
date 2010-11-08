package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;
import gnat.utils.FileHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filters entities basesd on known false positives from a training set.
 * 
 * @author Conrad
 */
public class StrictFPFilter implements Filter {

	private List<String[]> FPs;
	private List<String[]> nonFPs;

	private Map<String, Integer> fpCount;
	private Map<String, Integer> nonFPCount;


	/**
	 */
	public StrictFPFilter(String fpNames, String nonFPNames) {
		FPs = FileHelper.readFileIntoList(fpNames, "\t");
		nonFPs = FileHelper.readFileIntoList(nonFPNames, "\t");

		fpCount = new HashMap<String, Integer>();
		for (String[] name : FPs) {
			Integer count = fpCount.get(name[1]);
			if(count==null){
				count = new Integer(0);
			}
			count++;
			fpCount.put(name[1], count);
        }

		nonFPCount = new HashMap<String, Integer>();
		for (String[] name : nonFPs) {
			Integer count = nonFPCount.get(name[1]);
			if(count==null){
				count = new Integer(0);
			}
			count++;
			nonFPCount.put(name[1], count);
        }
	}


	/**
	 *
	 * */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		for (RecognizedEntity recognizedEntity : context.getUnidentifiedEntities()) {
			String name = recognizedEntity.getName();

			Integer fpNameCount = fpCount.get(name);
			if(fpNameCount == null){
				fpNameCount = new Integer(0);
			}

			Integer nonFPNameCount = nonFPCount.get(name);
			if(nonFPNameCount == null){
				nonFPNameCount = new Integer(0);
			}

			if(fpNameCount > nonFPNameCount){
				context.removeRecognizedEntity(recognizedEntity);
			}
		}

	}
}