package gnat.evaluation;

import gnat.client.nei.GenePubMedScorer;
import gnat.database.go.GOAccess;
import gnat.representation.Gene;
import gnat.representation.GeneFactory;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextFactory;
import gnat.representation.TextRepository;
import gnat.utils.MathHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

public class TestCase {

	TextRepository textrep = new TextRepository();
	TextFactory textFactory = new TextFactory();
	Text mytext = new Text("-1");

	GeneRepository generep = new GeneRepository();

	GenePubMedScorer genePubMedScorer = new GenePubMedScorer(new GOAccess(), "data/go2go.object");

	LinkedList<String> ids2check = new LinkedList<String>();

	public HashMap<String, Boolean> annotations = new HashMap<String, Boolean>();

	/**
	 *
	 * @param pmid
	 * @param genes
	 * @param annotations
	 */
	public TestCase (String textfile, String[] genes, boolean[] annotations) {
		GeneFactory.dataDirectory = "data/entrezgenes/";

		mytext = TextFactory.loadTextFromFile(textfile);
		textrep.addText(mytext);

		ids2check.clear();
		Collections.addAll(ids2check, genes);
		//generep.setGenes( GeneFactory.loadGenesFromDirectory(genes) );

		for (int a = 0; a < annotations.length; a++) {
			this.annotations.put(genes[a], annotations[a]);
		}
	}


	/**
	 *
	 * @return
	 */
	public boolean test () {
		System.err.println("# Testing PubMed " + mytext.ID);

		for (Gene gene: generep.getGenes(ids2check)) {
			//if (!gene.isValid()) continue;

			float score = genePubMedScorer.getScore(gene, mytext, "unknown");

			String id = gene.ID;
			while (id.length() < 5)
				id = " " + id;

			System.err.print("Gene " + id + " ("
					+ MathHelper.round2(gene.getContextModel().innerCoherence) + "):\t");
			if (annotations.get(gene.ID).booleanValue() == true)
				System.err.print("+");
			else
				System.err.print("-");

			System.err.print(" " + MathHelper.round2(score));

			System.err.print("\t["
				+ MathHelper.round2((score / gene.getContextModel().innerCoherence))
				+ ", " + MathHelper.round2((score * gene.getContextModel().innerCoherence))
				+ ", " + MathHelper.round2((score / (1.0f - gene.getContextModel().innerCoherence)))
				+ ", " + MathHelper.round2((score * (1.0f - gene.getContextModel().innerCoherence)))
				+ ", " + MathHelper.round2((score + ((1.0f - gene.getContextModel().innerCoherence) / 10.0f)))
				+ ", " + MathHelper.round2((score - (gene.getContextModel().innerCoherence / 10.0f)))
				+ ", " + MathHelper.round2(gene.getContextModel().innerCoherence - score)
				+ "]");

			System.err.println();

		}
		return true;
	}

}
