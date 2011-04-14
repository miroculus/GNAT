The two main functions of this software are (1) to train a model using existing labeled data and store the model in a file, and (2) use a model trained previously to find the mentions in previously unseen text. A good example of training is the class bc2.TrainModel, though here is an outline of the code needed for doing it:

// ***** Training example 

// Get the properties information from file
BannerProperties properties = BannerProperties.load(new FileReader(properties filename));
Tokenizer tokenizer = properties.getTokenizer();
// Load in the sentence text
List<String> sentenceTextList = /* a list of the sentences from the training data, exactly one sentence per string */
List<Sentence> sentences = new ArrayList<Sentence>();
for (String sentenceText : sentenceTextList)
{
    Sentence sentence = new Sentence(sentenceText);
    tokenizer.tokenize(sentence);
    // Add each mention in the sentence to the Sentence object using a call to sentence.addMention(new Mention(sentence, MentionType.getType(type), start, end));
    sentences.add(sentence);
}
// Train the model
CRFTagger tagger = CRFTagger.train(sentences, properties.getOrder(), properties.isUseFeatureInduction(), properties.getTagFormat(), properties.getTextDirection(), properties.getLemmatiser(), properties.getPosTagger(), properties.isUseNumericNormalization());
// Output the model to file
tagger.write(new File(model filename));

// ***** End training example

The main things to note here are that your text has to be broken into sentences, and that you'll need to add the mentions yourself. Keep in mind that training the model takes a significant amount of time: training on the 15000 sentences in the BioCreative II gene mention training set requires 12-18 hours.

A good example of using an existing model for labeling is the class bc2.TestModel, though here is an outline of the code needed to do it:

// ***** Testing example

// Get the properties information from file
BannerProperties properties = BannerProperties.load(new FileReader(properties filename));
Tokenizer tokenizer = properties.getTokenizer();
CRFTagger tagger = CRFTagger.load(new File(model filename), properties.getLemmatiser(), properties.getPosTagger());
ParenthesisPostProcessor postProcessor = properties.getPostProcessor();
// For each sentence to be labeled
{
    Sentence sentence = new Sentence(sentenceText);
    tokenizer.tokenize(sentence);
    tagger.tag(sentence);
    if (postProcessor != null)
        postProcessor.postProcess(sentence2);
    System.out.println(sentence.getTrainingText(properties.getTagFormat()));
}

// ***** End testing example

Execution is much faster - labeling 5000 the sentences from the BioCreative II gene mention test set requires approximately 10-20 minutes.

Installation should consist of simply unzipping the download files, though you must have Java 6 installed. Both compiling the source and execution will require the jar files in the libs directory. Also, the "lemmatiserDataDirectory" entry and the "posTaggerDataDirectory" in the properties file need to be correct, though they will not require modification if you execute from the parent of the "nlpdata" folder.

