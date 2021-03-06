package cz.cvut.fit;

import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.util.List;

/**
 * Created by George on 13-Apr-16.
 */
public class TwitterCrawler implements ShutdownStreamListener{
    private Twitter twitter;
    private TwitterStream stream;

    public TwitterCrawler() {

    }

    public void authorize() throws TwitterException {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey("E8avhNRUoUUKxL6vS9GKcKaFf");
        builder.setOAuthConsumerSecret("ZoVZgGmkyU64fblUazOEjYoumqiJaOtqcPd7sCFEPlv0a5SIe9");
        builder.setOAuthAccessToken("440022981-D5jExYSETLEoJbQgV8vLXO3VVDBt9eUOUN9QwgKM");
        builder.setOAuthAccessTokenSecret("WKyFWQoee5znACt3vqEZW2l2n6aSAn0XebHY2O1KfWsGm");
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        this.twitter = factory.getInstance();
        return;
    }


    public void setStream(StatusListener listener, String queryString) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey("E8avhNRUoUUKxL6vS9GKcKaFf");
        builder.setOAuthConsumerSecret("ZoVZgGmkyU64fblUazOEjYoumqiJaOtqcPd7sCFEPlv0a5SIe9");
        builder.setOAuthAccessToken("440022981-D5jExYSETLEoJbQgV8vLXO3VVDBt9eUOUN9QwgKM");
        builder.setOAuthAccessTokenSecret("WKyFWQoee5znACt3vqEZW2l2n6aSAn0XebHY2O1KfWsGm");
        Configuration configuration = builder.build();

        stream = new TwitterStreamFactory(configuration).getInstance();
        stream.addListener(listener);

        if (!queryString.isEmpty()) {
            FilterQuery query = new FilterQuery(queryString);
            query.language("en");
            stream.filter(query);
        } else {
            stream.sample();
        }
    }

    @Override
    public void closeStream() {
        stream.clearListeners();
        stream.shutdown();
    }
}
