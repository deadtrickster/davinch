package davinch

import info.ephyra.answerselection.AnswerSelection
import info.ephyra.answerselection.filters.AnswerPatternFilter
import info.ephyra.answerselection.filters.AnswerTypeFilter
import info.ephyra.answerselection.filters.DuplicateFilter
import info.ephyra.answerselection.filters.FactoidSubsetFilter
import info.ephyra.answerselection.filters.FactoidsFromPredicatesFilter
import info.ephyra.answerselection.filters.PredicateExtractionFilter
import info.ephyra.answerselection.filters.QuestionKeywordsFilter
import info.ephyra.answerselection.filters.ScoreCombinationFilter
import info.ephyra.answerselection.filters.ScoreNormalizationFilter
import info.ephyra.answerselection.filters.ScoreSorterFilter
import info.ephyra.answerselection.filters.StopwordFilter
import info.ephyra.answerselection.filters.TruncationFilter
import info.ephyra.io.Logger
import info.ephyra.io.MsgPrinter
import info.ephyra.nlp.LingPipe
import info.ephyra.nlp.NETagger
import info.ephyra.nlp.OpenNLP
import info.ephyra.nlp.SnowballStemmer
import info.ephyra.nlp.StanfordNeTagger
import info.ephyra.nlp.StanfordParser
import info.ephyra.nlp.indices.FunctionWords
import info.ephyra.nlp.indices.IrregularVerbs
import info.ephyra.nlp.indices.Prepositions
import info.ephyra.nlp.indices.WordFrequencies
import info.ephyra.nlp.semantics.ontologies.Ontology
import info.ephyra.nlp.semantics.ontologies.WordNet
import info.ephyra.querygeneration.QueryGeneration
import info.ephyra.querygeneration.generators.BagOfTermsG
import info.ephyra.querygeneration.generators.BagOfWordsG
import info.ephyra.querygeneration.generators.PredicateG
import info.ephyra.querygeneration.generators.QuestionInterpretationG
import info.ephyra.querygeneration.generators.QuestionReformulationG
import info.ephyra.questionanalysis.QuestionAnalysis
import info.ephyra.questionanalysis.QuestionInterpreter
import info.ephyra.search.Search
import info.ephyra.search.searchers.IndriKM
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext

/**
 * @author Hitoshi Wada
 */
@SpringBootApplication
class MainApplication {

    public static final String NORMALIZER =
            "res/scorenormalization/classifiers/" +
                    "AdaBoost70_" +
                    "Score+Extractors_" +
                    "TREC10+TREC11+TREC12+TREC13+TREC14+TREC15+TREC8+TREC9" +
                    ".serialized";

    public static void main(String[] args) {

        Logger.setLogfile("log/OpenEphyra")
        Logger.enableLogging(true)

        init()

        initFactoid()

        ApplicationContext ctx = SpringApplication.run(MainApplication.class, args);

        System.out.println("Let's inspect the beans provided by Spring Boot:");

//        String[] beanNames = ctx.getBeanDefinitionNames();
//        Arrays.sort(beanNames);
//        for (String beanName : beanNames) {
//            System.out.println(beanName);
//        }
    }

    private static void init(){

        String dir = System.getProperty("user.dir") + "/";
        println dir

        MsgPrinter.printInitializing();

        // create tokenizer
        MsgPrinter.printStatusMsg("Creating tokenizer...");
        if (!OpenNLP.createTokenizer(dir +
                "res/nlp/tokenizer/opennlp/EnglishTok.bin.gz"))
            MsgPrinter.printErrorMsg("Could not create tokenizer.");

        // create sentence detector
        MsgPrinter.printStatusMsg("Creating sentence detector...");
        if (!OpenNLP.createSentenceDetector(dir +
                "res/nlp/sentencedetector/opennlp/EnglishSD.bin.gz"))
            MsgPrinter.printErrorMsg("Could not create sentence detector.");
        LingPipe.createSentenceDetector();

        // create stemmer
        MsgPrinter.printStatusMsg("Creating stemmer...");
        SnowballStemmer.create();

        // create part of speech tagger
        MsgPrinter.printStatusMsg("Creating POS tagger...");
        if (!OpenNLP.createPosTagger(
                dir + "res/nlp/postagger/opennlp/tag.bin.gz",
                dir + "res/nlp/postagger/opennlp/tagdict"))
            MsgPrinter.printErrorMsg("Could not create OpenNLP POS tagger.");

        // create chunker
        MsgPrinter.printStatusMsg("Creating chunker...");
        if (!OpenNLP.createChunker(dir +
                "res/nlp/phrasechunker/opennlp/EnglishChunk.bin.gz"))
            MsgPrinter.printErrorMsg("Could not create chunker.");

        // create syntactic parser
        MsgPrinter.printStatusMsg("Creating syntactic parser...");
        try {
            StanfordParser.initialize();
        } catch (Exception e) {
            MsgPrinter.printErrorMsg("Could not create Stanford parser.");
        }

        // create named entity taggers
        MsgPrinter.printStatusMsg("Creating NE taggers...");
        NETagger.loadListTaggers(dir + "res/nlp/netagger/lists/");
        NETagger.loadRegExTaggers(dir + "res/nlp/netagger/patterns.lst");
        MsgPrinter.printStatusMsg("  ...loading models");
        if (!StanfordNeTagger.isInitialized() && !StanfordNeTagger.init())
            MsgPrinter.printErrorMsg("Could not create Stanford NE tagger.");
        MsgPrinter.printStatusMsg("  ...done");

        // create WordNet dictionary
        MsgPrinter.printStatusMsg("Creating WordNet dictionary...");
        if (!WordNet.initialize(dir +
                "res/ontologies/wordnet/file_properties.xml"))
            MsgPrinter.printErrorMsg("Could not create WordNet dictionary.");

        // load function words (numbers are excluded)
        MsgPrinter.printStatusMsg("Loading function verbs...");
        if (!FunctionWords.loadIndex(dir +
                "res/indices/functionwords_nonumbers"))
            MsgPrinter.printErrorMsg("Could not load function words.");

        // load prepositions
        MsgPrinter.printStatusMsg("Loading prepositions...");
        if (!Prepositions.loadIndex(dir +
                "res/indices/prepositions"))
            MsgPrinter.printErrorMsg("Could not load prepositions.");

        // load irregular verbs
        MsgPrinter.printStatusMsg("Loading irregular verbs...");
        if (!IrregularVerbs.loadVerbs(dir + "res/indices/irregularverbs"))
            MsgPrinter.printErrorMsg("Could not load irregular verbs.");

        // load word frequencies
        MsgPrinter.printStatusMsg("Loading word frequencies...");
        if (!WordFrequencies.loadIndex(dir + "res/indices/wordfrequencies"))
            MsgPrinter.printErrorMsg("Could not load word frequencies.");

        // load query reformulators
        MsgPrinter.printStatusMsg("Loading query reformulators...");
        if (!QuestionReformulationG.loadReformulators(dir +
                "res/reformulations/"))
            MsgPrinter.printErrorMsg("Could not load query reformulators.");

        // load question patterns
        MsgPrinter.printStatusMsg("Loading question patterns...");
        if (!QuestionInterpreter.loadPatterns(dir +
                "res/patternlearning/questionpatterns/"))
            MsgPrinter.printErrorMsg("Could not load question patterns.");

        // load answer patterns
        MsgPrinter.printStatusMsg("Loading answer patterns...");
        if (!AnswerPatternFilter.loadPatterns(dir +
                "res/patternlearning/answerpatterns/"))
            MsgPrinter.printErrorMsg("Could not load answer patterns.");

    }

    private static void initFactoid() {
        // question analysis
        Ontology wordNet = new WordNet();
        // - dictionaries for term extraction
        QuestionAnalysis.clearDictionaries();
        QuestionAnalysis.addDictionary(wordNet);
        // - ontologies for term expansion
        QuestionAnalysis.clearOntologies();
        QuestionAnalysis.addOntology(wordNet);

        // query generation
        QueryGeneration.clearQueryGenerators();
        QueryGeneration.addQueryGenerator(new BagOfWordsG());
        QueryGeneration.addQueryGenerator(new BagOfTermsG());
        QueryGeneration.addQueryGenerator(new PredicateG());
        QueryGeneration.addQueryGenerator(new QuestionInterpretationG());
        QueryGeneration.addQueryGenerator(new QuestionReformulationG());

        // search
        // - knowledge miners for unstructured knowledge sources
        Search.clearKnowledgeMiners();

        for (String[] indriIndices : IndriKM.getIndriIndices())
            Search.addKnowledgeMiner(new IndriKM(indriIndices, false));

        // - knowledge annotators for (semi-)structured knowledge sources
        Search.clearKnowledgeAnnotators();
        /* Search.addKnowledgeAnnotator(new WikipediaKA("list.txt")); */

        // answer extraction and selection
        // (the filters are applied in this order)
        AnswerSelection.clearFilters();
        // - answer extraction filters
        AnswerSelection.addFilter(new AnswerTypeFilter());
        AnswerSelection.addFilter(new AnswerPatternFilter());
        AnswerSelection.addFilter(new PredicateExtractionFilter());
        AnswerSelection.addFilter(new FactoidsFromPredicatesFilter());
        AnswerSelection.addFilter(new TruncationFilter());
        // - answer selection filters
        AnswerSelection.addFilter(new StopwordFilter());
        AnswerSelection.addFilter(new QuestionKeywordsFilter());
        AnswerSelection.addFilter(new ScoreNormalizationFilter(NORMALIZER));
        AnswerSelection.addFilter(new ScoreCombinationFilter());
        AnswerSelection.addFilter(new FactoidSubsetFilter());
        AnswerSelection.addFilter(new DuplicateFilter());
        AnswerSelection.addFilter(new ScoreSorterFilter());
    }
}
