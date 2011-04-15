package gnat.filter.nei;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import banner.BannerProperties;
import banner.Sentence;
import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;

import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextRepository;

public class BANNERValidationFilter implements Filter {
	private Tokenizer tokenizer;
	private PostProcessor postProcessor;
	private CRFTagger tagger;

	public BANNERValidationFilter() {
		String propsFile = ISGNProperties.getProperty("bannerProps");
		File modelFile = new File(ISGNProperties.getProperty("bannerModel"));

		BannerProperties properties = BannerProperties.load(propsFile);
		this.tokenizer = properties.getTokenizer();
		try {
			this.tagger = CRFTagger.load(modelFile, properties.getLemmatiser(), properties.getPosTagger());
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
		this.postProcessor = properties.getPostProcessor();
	}

	@Override
	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository) {
		for (Text text : context.getTexts()){
			Set<RecognizedEntity> ents = context.getRecognizedEntitiesInText(text);

			Map<String,List<Mention>> sentenceMentions = new HashMap<String,List<Mention>>();

			for (RecognizedEntity e : ents){
				String sent = e.getText().getSentenceAround(e.getBegin());

				int sentStart = e.getText().getPlainText().indexOf(sent);
				if (sentStart != -1){
					if (!sentenceMentions.containsKey(sent)){
						Sentence sentence = new Sentence(sent);
						tokenizer.tokenize(sentence);
						tagger.tag(sentence);
						if (postProcessor != null)
							postProcessor.postProcess(sentence);

						List<banner.tagging.Mention> mentions = sentence.getMentions();
						sentenceMentions.put(sent, mentions);
					}

					List<Mention> mentions = sentenceMentions.get(sent);

					boolean matchesBannerMention = false;
					
					int s1 = e.getBegin();
					int e1 = e.getEnd()+1;
					
					for (Mention m : mentions){
						int s2 = m.getStartChar() + sentStart;
						int e2 = m.getEndChar() + sentStart;
						
//						System.out.println("\t" + s2 + "\t" + e2 + "\t" + m.getStart() + "\t" + m.getEnd() + "\t" + m.getText());

						if ((s1 >= s2 && s1 < e2) || (s2 >= s1 && s2 < e1)){
							matchesBannerMention = true;
//							System.out.println("\t" + m.getText());
							break;
						}						
					}
					
					if (!matchesBannerMention){
						context.removeRecognizedEntity(e);
					}
				}
			}
		}
	}
}