package miroculus;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.client.Run;
import gnat.filter.nei.AlignmentFilter;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.filter.nei.LeftRightContextFilter;
import gnat.filter.nei.MultiSpeciesDisambiguationFilter;
import gnat.filter.nei.NameValidationFilter;
import gnat.filter.nei.RecognizedEntityUnifier;
import gnat.filter.nei.SpeciesFrequencyFilter;
import gnat.filter.nei.StopWordFilter;
import gnat.filter.nei.UnambiguousMatchFilter;
import gnat.filter.nei.UnspecificNameFilter;
import gnat.filter.ner.DefaultSpeciesRecognitionFilter;
import gnat.filter.ner.RunAllGeneDictionaries;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.Text;
import gnat.representation.TextRepository;
import gnat.utils.AlignmentHelper;

public class SocketAnnotate implements Runnable {
	

	
	private Socket connection;
	private int ID;
	private Run run;
	
	public SocketAnnotate(Socket connection, int ID) {
		this.connection = connection;
		this.ID = ID;
		System.out.println("running process "+ this.ID);
		run = new Run();
		run.verbosity = 0;
		
		ConstantsNei.setOutputLevel(run.verbosity);
		
		// setting input in blank
		run.setTextRepository(new TextRepository());
		
		// the text must set a setpmid as in RUN:102
		
		
		//Preparing the filters
		// pre processing 
		run.addFilter(new NameRangeExpander()); // affects the text object...
		
		// NER filters
		run.addFilter(new DefaultSpeciesRecognitionFilter()); // affects the text object
		// default species NER: spots human, mouse, rat, yeast, and fly only
		
		// this block of code must run after set a text
		String assumeSpecies = ISGNProperties.get("assumeSpecies");
		if (assumeSpecies != null && assumeSpecies.length() > 0) {
			String[] species = assumeSpecies.split("[\\;\\,]\\s*");
			for (String spec: species) {
				if (!spec.matches("\\d+")) continue;
				int tax = Integer.parseInt(spec);
				for (Text text : run.getTextRepository().getTexts())
					text.addTaxonId(tax);
			}
		}
		// end
		
		// construct a dictionary filter for human
		RunAllGeneDictionaries afewDictionaryFilters = new RunAllGeneDictionaries();
		afewDictionaryFilters.setLimitToTaxons(9606);
		run.addFilter(afewDictionaryFilters);
		

		// NER filters:
		run.addFilter(new RecognizedEntityUnifier());

		run.addFilter(new ImmediateContextFilter());

		run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));

		run.addFilter(new ImmediateContextFilter());
		
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.DATABASE));

		run.addFilter(new StopWordFilter(ISGNProperties.get("stopWords")));

		run.addFilter(new UnambiguousMatchFilter());

		run.addFilter(new UnspecificNameFilter());

		run.addFilter(new AlignmentFilter(AlignmentHelper.globalAlignment, 0.7f));

		run.addFilter(new NameValidationFilter());

		run.addFilter(new SpeciesFrequencyFilter());
		
		// Final disambiguation filter
		run.addFilter(new MultiSpeciesDisambiguationFilter(
				Integer.parseInt(ISGNProperties.get("disambiguationThreshold")),
				Integer.parseInt(ISGNProperties.get("maxIdsForCandidatePrediction"))));
		
		// Mark everything that "survived" until here as OK, will be reported in output
		// Only for high-recall runs
		String tuning = ISGNProperties.get("tuning");
		if (tuning != null && tuning.equalsIgnoreCase("recall"))
			run.addFilter(new IdentifyAllFilter());
	}
	
	public static void main(String[] args) {

		
		int port = 20051;
		int count = 0;
		try {
			ServerSocket socket1 = new ServerSocket(port);
			System.out.println("MultipleSocketServer Initialized on "+port);
			while (true) {
				Runnable runnable = new SocketAnnotate(socket1.accept(), ++count);
				Thread thread = new Thread(runnable);
				thread.start();
			}
		} catch (Exception e) {
			System.out.println("105. exception "+ e);
		}
		
		
		
	}

	@Override
	public void run() {
		StringBuffer process = new StringBuffer();
		int counter = 0;
		String eseTextp = "";
		boolean running = true;
		try {
			InputStream is = connection.getInputStream();
			while(running){

				int character;
				process = new StringBuffer();
				process.ensureCapacity(1000000);
				
				while ((character = is.read()) != 13) {
					process.append((char) character);
				}
				counter = 1;
				
				eseTextp = process.toString();
				counter = 2;
				String[] format = eseTextp.split("></-/><");

				if("end".equals(format[0])){
					System.out.println("closing socket");
					running = false;
					continue;
				}
				counter = 3;
				run.context.clear();
				run.getTextRepository().clear();
				
				Text text = new Text(format[0], format[2]);
				text.setPMID(Integer.parseInt(format[0]));

				run.getTextRepository().addText(text);
				
				counter = 4;
				/* ENDS */
				run.runFilters();
				
				counter = 5;				
				List<String> result = run.context.getIdentifiedGeneList_SortedByTextAndId();
				StringBuffer response = new StringBuffer(String.join("\n", result));
				response.append((char)13);
				
				BufferedOutputStream os = new BufferedOutputStream(
						connection.getOutputStream());
				counter = 6;
				OutputStreamWriter osw = new OutputStreamWriter(os, "US-ASCII");
				counter = 7;
				osw.write(response.toString());
				counter = 8;
				osw.flush();
				//System.out.println(result);				
			}

		} catch (Exception e) {
			System.out.println("333 "+ counter + ". " +e);
			System.out.println(eseTextp);
			e.printStackTrace();
		} finally {

			try {
				connection.close();
				//System.out.println("cerrando "+ID);
			} catch (IOException e) {
				System.out.println("302" + e);
			}
		}
	}

}
