package edu.whu.cs.nlp.mts.sys;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * stanford coreNlp对象初始化类
 *
 * @author Apache_xiaochao
 *
 */
public class CoreNlpObject {

    private volatile static StanfordCoreNLP pipeline; // stanford coref need

    private CoreNlpObject() {

    }

    public static StanfordCoreNLP getPipeLine() {
        if (pipeline == null) {
            synchronized (CoreNlpObject.class) {
                if (pipeline == null) {
                    final Properties props = new Properties();
                    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
                    pipeline = new StanfordCoreNLP(props);
                }
            }
        }
        return pipeline;
    }

}
