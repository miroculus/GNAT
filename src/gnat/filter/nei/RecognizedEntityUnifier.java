package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRange;
import gnat.representation.TextRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges recognized entities in a text to a single entity when they overlap or are adjacent to each other.
 * 
 * @author Conrad
 */
public class RecognizedEntityUnifier implements Filter {

	/**
	 * Merges recognized entities in a text to a single entity when they overlap or are adjacent to each other.
	 * */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Map<String, Set<RecognizedEntity>> textIdToGeneNames = context.getRecognizedEntitiesAsMap();

		for (Text text : context.getTexts()) {
			Set<RecognizedEntity> entitiesForText = textIdToGeneNames.get(text.getID());
			if(entitiesForText!=null){

				// sort entities according to their text range start position
				List<RecognizedEntity> entityList = Context.sortRecognizedEntities(entitiesForText);

				for(int i=0;i<entityList.size();i++){
					RecognizedEntity entity = entityList.get(i);

					//System.out.println("**** Starting with name '"+entity.getName()+"' for text="+text.getID());

					TextRange newEntityRange = new TextRange(entity.getBegin(), entity.getEnd());;
					Set<String> newEntityIds = new HashSet<String>();
					newEntityIds.addAll(context.getIdCandidates(entity));

					i = prolongEntityName(context, text, entityList, i, newEntityRange, newEntityIds);

					String newName = text.getPlainText().substring(newEntityRange.getBegin(), newEntityRange.getEnd()+1);
					RecognizedEntity newEntity = new RecognizedEntity(text, new TextAnnotation(newEntityRange, newName));
					context.addRecognizedEntity(newEntity, newEntityIds);
					//System.out.println("**** Added new name '"+newName+"' for text="+text.getID()+" sarting at "+newEntity.getBegin());
				}

				// remove old entities
				for (RecognizedEntity entity : entityList) {
					context.removeRecognizedEntity(entity);
                }
			}
		}
	}

	
	/**
	 * Tries to find overlapping or adjacent entities and merges them to a new, prolonged entity.
	 * Entities that have a range contained in the current range also get merged.
	 * The new set of candidate IDs for a prolonged entity is the union of all IDs merged together.
	 *
	 * Returns the index position of an entity, according to the successor list, that cannot be merged.
	 * */
	private int prolongEntityName (Context context, Text text, List<RecognizedEntity> entitySuccessors, int currentIndex, TextRange currentTextRange, Set<String> currentIds){

		if(currentIndex+1 < entitySuccessors.size()){
			RecognizedEntity next = entitySuccessors.get(currentIndex+1);
			boolean consumedNext = false;
			if(currentTextRange.equals(next.getTextRange())){
				currentIds.addAll(context.getIdCandidates(next));
				consumedNext = true;
				//System.out.println("Consumed "+next.getName());
			}
			else if(currentTextRange.contains(next.getTextRange())){
				consumedNext = true;
				//System.out.println("Consumed "+next.getName());
			}
			else if(next.getBegin()==currentTextRange.getBegin()){	// next contains current
				currentIds.clear();
				currentIds.addAll(context.getIdCandidates(next));
				currentTextRange.setEnd(next.getEnd());
				consumedNext = true;
				//System.out.println("Prolonged "+next.getName());
			}
			else if(next.getBegin()<=currentTextRange.getEnd()){
				currentIds.addAll(context.getIdCandidates(next));
				currentTextRange.setEnd(next.getEnd());
				consumedNext = true;
				//System.out.println("Prolonged "+next.getName());
			}
			else if(entitiesAreAdjacent(text, currentTextRange, next.getTextRange())){
				Set<String> overlapIds = cut(currentIds, context.getIdCandidates(next));
				if(overlapIds.size()>0){
					currentTextRange.setEnd(next.getEnd());
					consumedNext = true;
					//System.out.println("Prolonged "+next.getName());
				}
			}

			if(consumedNext){
				currentIndex = prolongEntityName(context, text, entitySuccessors, currentIndex+1, currentTextRange, currentIds);
			}
		}

		return currentIndex;
	}

	/**
	 * Tests if two text ranges are adjacent, i.e. they are separated by a whitespace character.
	 *
	 * */
	private boolean entitiesAreAdjacent(Text text, TextRange leftRange, TextRange rightRange){
		boolean adjacent = false;

		if(leftRange.getEnd()+2 == rightRange.getBegin()){
			char charInBetween = text.getPlainText().charAt(leftRange.getEnd()+1);
			if(charInBetween==' '){
				adjacent = true;
			}
		}

		return adjacent;
	}

	/**
	 * Returns the overlap of two sets of strings.
	 * */
	@SuppressWarnings("unused")
    private Set<String> cut(Set<String> setA, Set<String> setB){
		Set<String> overlapSet = new HashSet<String>();
		for (String string : setA) {
			if(setB.contains(string)){
				overlapSet.add(string);
			}
        }
		return overlapSet;
	}
}
