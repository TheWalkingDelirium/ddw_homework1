package cz.cvut.fit;

import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import twitter4j.*;

import java.io.IOException;

/**
 * Created by George on 15-Apr-16.
 */
public class TwitterHashtagAnalyzer implements ShutdownStreamListener, RunGateListener, StatusListener {
    TwitterCrawler crawler;
    GateClient client;

    public TwitterHashtagAnalyzer() {
        crawler = new TwitterCrawler();
        client = new GateClient(this, this);
    }

    public void analyze(String query) {
        try {
            crawler.authorize();
        } catch (TwitterException e) {
            e.printStackTrace();
            return;
        }

        crawler.setStream(this, query);
    }

    @Override
    public void runTextProcessing() {
    }

    @Override
    public void closeStream() {
    }

    @Override
    public void onStatus(Status status) {

        client.addDocument(status.getText());

        if (client.getCorpusSize() == 300) {
            runGateClient();
        }
    }

    private void runGateClient() {

        try {
            client.run();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        crawler.closeStream();


    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

    }

    @Override
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {

    }

    @Override
    public void onScrubGeo(long userId, long upToStatusId) {

    }

    @Override
    public void onStallWarning(StallWarning warning) {

    }

    @Override
    public void onException(Exception ex) {

    }
}
