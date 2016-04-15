package cz.cvut.fit;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.bg.PixelBoundryBackground;
import com.kennycason.kumo.font.KumoFont;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.SerialController;
import gate.gui.FeatureMapEditorDialog;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Milan Dojchinovski
 *         <milan (at) dojchinovski (dot) mk>
 *         Twitter: @m1ci
 *         www: http://dojchinovski.mk
 */
public class GateClient  {

    // corpus pipeline
    private static SerialAnalyserController annotationPipeline = null;

    // whether the GATE is initialised
    private static boolean isGateInitilised = false;
    private RunGateListener runGateListener;

    ShutdownStreamListener listener;
    ProcessingResource documentResetPR;
    ProcessingResource annotationSetTransfer;
    ProcessingResource tweetLanguageIdentificator;
    ProcessingResource EmoticonsGazetteer;
    ProcessingResource twitterTokenizer;
    ProcessingResource hashtagTokenizer;
    ProcessingResource annieGazettier;
    ProcessingResource annieSentenceSplitter;
    ProcessingResource tweetNormalizer;
    ProcessingResource stanfordPOSTagger;
    ProcessingResource annieNETranscuder;

    FeatureMap transducerFeatureMap;
    Corpus corpus;

    public GateClient(ShutdownStreamListener listener, RunGateListener runGateListener) {

        this.listener = listener;
        this.runGateListener = runGateListener;

        if (!isGateInitilised) {
            initialiseGate();
        }

        try {
            documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");

            annotationSetTransfer = (ProcessingResource) Factory.createResource("gate.creole.annotransfer.AnnotationSetTransfer");

            FeatureMap params = Factory.newFeatureMap();
            URL u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/lang_id/default-names.conf");
            params.put("configURL", u);
            tweetLanguageIdentificator = (ProcessingResource) Factory.createResource("org.knallgrau.utils.textcat.LanguageIdentifier", params);

            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/emoticons/lists.def");
            params.clear();
            params.put("caseSensitive", new Boolean(true));
            params.put("encoding", "UTF-8");
            params.put("listsURL", u);
            params.put("gazetteerFeatureSeparator", " ");
            EmoticonsGazetteer = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", params);

            params.clear();
            params.put("encoding", "UTF-8");
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/tokeniser/DefaultTokeniser.rules");
            params.put("tokeniserRulesURL", u);
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/tokeniser/twitter+English.jape");
            params.put("transducerGrammarURL", u);
            twitterTokenizer = (ProcessingResource) Factory.createResource("gate.twitter.tokenizer.TokenizerEN", params);

            params.clear();
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/hashtag/gazetteer/lists.def");
            params.put("gazetteerURL", u);
            hashtagTokenizer = (ProcessingResource) Factory.createResource("gate.twitter.HashtagTokenizer", params);

            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/ANNIE/resources/gazetteer/lists.def");
            params.clear();
            params.put("caseSensitive", new Boolean(true));
            params.put("encoding", "UTF-8");
            params.put("listsURL", u);
            params.put("gazetteerFeatureSeparator", ":");
            annieGazettier = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", params);

            params.clear();
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/ANNIE/resources/sentenceSplitter/gazetteer/lists.def");
            params.put("gazetteerListsURL", u);
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/ANNIE/resources/sentenceSplitter/grammar/main.jape");
            params.put("transducerURL", u);
            params.put("encoding", "UTF-8");
            annieSentenceSplitter = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter", params);

            params.clear();
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/normaliser/orth.en.csv");
            params.put("orthURL", u);
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/normaliser/english.jaspell");
            params.put("dictURL", u);
            tweetNormalizer = (ProcessingResource) Factory.createResource("gate.twitter.Normaliser", params);

            params.clear();
            u = new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/Twitter/resources/pos/gate-EN-twitter.model");
            params.put("modelFile", u);
            stanfordPOSTagger = (ProcessingResource) Factory.createResource("gate.stanford.Tagger", params);

            params.clear();
            params.put("encoding", "UTF-8");
            params.put("grammarURL", new URL("file:/C:/Program%20Files/GATE_Developer_8.1/plugins/ANNIE/resources/NE/main-twitter.jape"));
            annieNETranscuder = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);


            // create corpus pipeline
            annotationPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

            // add the processing resources (modules) to the pipeline
            annotationPipeline.add(documentResetPR);
            annotationPipeline.add(annotationSetTransfer);
            annotationPipeline.add(tweetLanguageIdentificator);
            annotationPipeline.add(EmoticonsGazetteer);
            annotationPipeline.add(twitterTokenizer);
            annotationPipeline.add(hashtagTokenizer);
            annotationPipeline.add(annieGazettier);
            annotationPipeline.add(annieSentenceSplitter);
            annotationPipeline.add(tweetNormalizer);
            annotationPipeline.add(stanfordPOSTagger);
            annotationPipeline.add(annieNETranscuder);

            corpus = Factory.newCorpus("");

        } catch (GateException ex) {
            System.out.println(ex);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    private void initialiseGate() {

        try {
            // set GATE home folder
            // Eg. /Applications/GATE_Developer_7.0
            File gateHomeFile = new File("C:\\Program Files\\GATE_Developer_8.1");
            Gate.setGateHome(gateHomeFile);

            // set GATE plugins folder
            // Eg. /Applications/GATE_Developer_7.0/plugins
            File pluginsHome = new File("C:\\Program Files\\GATE_Developer_8.1\\plugins");
            Gate.setPluginsHome(pluginsHome);

//            // set user config file (optional)
//            // Eg. /Applications/GATE_Developer_7.0/user.xml
//            Gate.setUserConfigFile(new File("/Applications/GATE_Developer_7.0", "user.xml"));

            // initialise the GATE library
            Gate.init();

            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);

            Gate.getCreoleRegister().registerDirectories(
                    new File(Gate.getPluginsHome(), "Tools").toURL()
            );

            Gate.getCreoleRegister().registerDirectories(
                    new File(Gate.getPluginsHome(), "Language_Identification").toURL()
            );

            Gate.getCreoleRegister().registerDirectories(
                    new File(Gate.getPluginsHome(), "Twitter").toURL()
            );

            // flag that GATE was successfuly initialised
            isGateInitilised = true;

        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (GateException ex) {
            System.out.println(ex);
        }
    }

    public void run() throws ResourceInstantiationException, ExecutionException, InvalidOffsetException, IOException {

        // set the corpus to the pipeline
        annotationPipeline.setCorpus(corpus);

        //run the pipeline
        annotationPipeline.execute();

        ArrayList<String> hashtagList = new ArrayList<>();

        // loop through the documents in the corpus
        for (int i = 0; i < corpus.size(); i++) {

            Document doc = corpus.get(i);

            // get the default annotation set
            AnnotationSet as_default = doc.getAnnotations();

            FeatureMap futureMap = null;
            // get all Token annotations
            AnnotationSet hashtags = as_default.get("Hashtag", futureMap);

            ArrayList tokenAnnotations = new ArrayList(hashtags);

            // looop through the Hashtag annotations
            for (int j = 0; j < tokenAnnotations.size(); ++j) {

                // get a hashtag annotation
                Annotation hashtag = (Annotation) tokenAnnotations.get(j);

                // get the underlying string for the Token
                Node isaStart = hashtag.getStartNode();
                Node isaEnd = hashtag.getEndNode();
                String underlyingString = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                System.out.println("Hashtag: " + underlyingString);
                hashtagList.add(underlyingString);
                // get the features of the hashtag
//                FeatureMap annFM = hashtag.getFeatures();

                // get the value of the "string" feature
//                String value = (String)annFM.get((Object)"string");
//                System.out.println("Token: " + value);
            }
        }

        ArrayList<String> stopwords =  new ArrayList<>();
        stopwords.add("fap");
        stopwords.add("ipad");
        final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
        frequencyAnalyzer.setWordFrequenciesToReturn(300);
        frequencyAnalyzer.setMinWordLength(4);
        frequencyAnalyzer.setStopWords(stopwords);

        final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(hashtagList);
        final Dimension dimension = new Dimension(600, 600);
        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        wordCloud.setPadding(2);
        wordCloud.setBackground(new CircleBackground(300))    ;
        wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
        wordCloud.setFontScalar(new LinearFontScalar(10, 40));
        wordCloud.setKumoFont(new KumoFont(new Font("Consolas", Font.PLAIN, 10)));
        wordCloud.build(wordFrequencies);
        wordCloud.writeToFile("C:\\Users\\George\\Desktop\\word_cloud.png");

        return;
    }

    private static InputStream getInputStream(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    public void addDocument(String text) {
        Document doc = null;
        try {
            doc = Factory.newDocument(text);
            corpus.add(doc);
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }
    }

    public int getCorpusSize() {
        return corpus.size();
    }


}