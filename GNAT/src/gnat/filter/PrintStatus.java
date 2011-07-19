package gnat.filter;

import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.IdentifiedGene;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * This filter does nothing other than print the current status: all recognized, identified, and still
 * unidentified gene mentions, to STDOUT.
 * <br>
 * <br>
 * Change the PrintStream {@link #out} if you want to print to someplace else.
 * 
 * 
 * @author hakenbej
 *
 */

public class PrintStatus implements Filter {
	
	public PrintStream out = System.out;

	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		
		out.append("Status:\n");
		//out.append("TextRepository has " + textRepository.size() + " texts.\n");
		//out.append("GeneRepository has " + geneRepository.size() + " genes.\n");
		out.append("RecognizedEntities:\n");
		for (RecognizedEntity re: context.getRecognizedEntities()) {
			out.append("R\t" + re.getText().getPMID()+"\t" + re.getName()
					+ "\t" + re.getBegin() + "-" + re.getEnd()
					+ "\t" + context.getIdentificationStatus(re).getIdCandidates()
					+ "\n");
		}
		out.append("IdentifiedGenes:\n");
		Iterator<IdentifiedGene> it = context.getIdentifiedGenesIterator();
		while (it.hasNext()) {
			IdentifiedGene gene = it.next();
			out.append("I\t" + gene.getRecognizedEntity().getText().PMID
					+ "\t" + gene.getName()
					+ "\t" + gene.getRecognizedEntity().getBegin() + "-" + gene.getRecognizedEntity().getEnd()
					+ "\t" + gene.getGene().getID()
					+ "\t" + gene.getGene().getTaxon()
					+ "\n");
		}
		out.append("UnidentifiedGenes:\n");
		for (RecognizedEntity re: context.getUnidentifiedEntities()) {
			out.append("U\t" + re.getText().getPMID()+"\t" + re.getName()
					+ "\t" + re.getBegin() + "-" + re.getEnd()
					+ "\t" + context.getIdentificationStatus(re).getIdCandidates()
					+ "\n");
		}
		out.append("--------------------\n");
		
	}
	
}
