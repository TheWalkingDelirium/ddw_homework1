package cz.cvut.fit;

import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import twitter4j.*;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        TwitterHashtagAnalyzer analyzer = new TwitterHashtagAnalyzer();
        analyzer.analyze("");
    }

}
