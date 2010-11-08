// ©2008 Transinsight GmbH - www.transinsight.com - All rights reserved.
package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;
import gnat.utils.FileHelper;

import java.util.Set;

/**
 * This filter removes all recognized entities that use a stop word as their name.
 *
 * @author Conrad
 */
public class StopWordFilter implements Filter {

	private Set<String> stopwords;

	/**
	 *
	 */
	public StopWordFilter (String stopwordFile){
		stopwords = FileHelper.readFileIntoSet(stopwordFile, false, true);
	}

	
	/**
	 * 
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		
		for (RecognizedEntity recognizedEntity : context.getRecognizedEntities()) {
			if(stopwords.contains(recognizedEntity.getName())){
				context.removeEntitiesHavingName(recognizedEntity.getName(), recognizedEntity.getText());
			}
		}
	}

}
