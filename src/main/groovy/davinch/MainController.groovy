package davinch

import info.ephyra.answerselection.AnswerSelection
import info.ephyra.io.Logger
import info.ephyra.io.MsgPrinter
import info.ephyra.querygeneration.Query
import info.ephyra.querygeneration.QueryGeneration
import info.ephyra.questionanalysis.AnalyzedQuestion
import info.ephyra.questionanalysis.QuestionAnalysis
import info.ephyra.questionanalysis.QuestionNormalizer
import info.ephyra.search.Result
import info.ephyra.search.Search
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse

@RestController
public class MainController {

    /** Factoid question type. */
    protected static final String FACTOID = "FACTOID";
    /** List question type. */
    protected static final String LIST = "LIST";

    /** Maximum number of factoid answers. */
    protected static final int FACTOID_MAX_ANSWERS = 1;
    /** Absolute threshold for factoid answer scores. */
    protected static final float FACTOID_ABS_THRESH = 0;
    /** Relative threshold for list answer scores (fraction of top score). */
    protected static final float LIST_REL_THRESH = 0.1f;

    /** Serialized classifier for score normalization. */
    public static final String NORMALIZER =
            "res/scorenormalization/classifiers/" +
                    "AdaBoost70_" +
                    "Score+Extractors_" +
                    "TREC10+TREC11+TREC12+TREC13+TREC14+TREC15+TREC8+TREC9" +
                    ".serialized";

    /** The directory of Ephyra, required when Ephyra is used as an API. */
    protected String dir;

    @RequestMapping("/{query}")
    public String index(@PathVariable String query) {

        System.out.println("Query str: " + query);

        if (!query) {
//            response.setContentType("text/html;charset=utf-8");
//            response.setStatus(HttpServletResponse.SC_OK);
//            baseRequest.setHandled(true);
            return "sorry!";
        }

        String question = URLDecoder.decode(query, "UTF-8");

        // response
//        res.setContentType("text/html;charset=utf-8");
//        res.setStatus(HttpServletResponse.SC_OK);
//        baseRequest.setHandled(true);

        // determine question type and extract question string
        String type;
        if (question.matches("(?i)" + FACTOID + ":.*+")) {
            // factoid question
            type = FACTOID;
            question = question.split(":", 2)[1].trim();
        } else if (question.matches("(?i)" + LIST + ":.*+")) {
            // list question
            type = LIST;
            question = question.split(":", 2)[1].trim();
        } else {
            // question type unspecified
            type = FACTOID;  // default type
        }

        // ask question
        Result[] results = new Result[0];
        if (type.equals(FACTOID)) {
            Logger.logFactoidStart(question);
            results = askFactoid(question, FACTOID_MAX_ANSWERS,
                    FACTOID_ABS_THRESH);
            Logger.logResults(results);
            Logger.logFactoidEnd();
        } else if (type.equals(LIST)) {
            Logger.logListStart(question);
            results = askList(question, LIST_REL_THRESH);
            Logger.logResults(results);
            Logger.logListEnd();
        }

        String answer = results ? results[0].getAnswer() : null
        return answer ?: "Sorry, I cannot answer your question."

    }

    public Result[] askFactoid(String question, int maxAnswers, float absThresh) {
        // initialize pipeline
//        initFactoid(); // do in MainApplication class

        // analyze question
        MsgPrinter.printAnalyzingQuestion();
        AnalyzedQuestion aq = QuestionAnalysis.analyze(question);

        // get answers
        Result[] results = runPipeline(aq, maxAnswers, absThresh);

        return results;
    }

    protected Result[] runPipeline(AnalyzedQuestion aq, int maxAnswers, float absThresh) {
        // query generation
        MsgPrinter.printGeneratingQueries()
        Query[] queries = QueryGeneration.getQueries(aq)

        // search
        MsgPrinter.printSearching()
        Result[] results = Search.doSearch(queries)

        // answer selection
        MsgPrinter.printSelectingAnswers()
        results = AnswerSelection.getResults(results, maxAnswers, absThresh)

        results
    }

    public Result[] askList(String question, float relThresh) {
        question = QuestionNormalizer.transformList(question);

        Result[] results = askFactoid(question, Integer.MAX_VALUE, 0)

        // get results with a score of at least relThresh * top score
        ArrayList<Result> confident = new ArrayList<Result>();
        if (results.length > 0) {
            float topScore = results[0].getScore();

            for (Result result : results)
                if (result.getScore() >= relThresh * topScore)
                    confident.add(result)
        }

        return confident.toArray(new Result[confident.size()])
    }
}
