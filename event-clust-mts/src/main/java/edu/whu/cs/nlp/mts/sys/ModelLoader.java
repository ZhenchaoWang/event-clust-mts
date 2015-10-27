package edu.whu.cs.nlp.mts.sys;

import java.io.IOException;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.InvalidFormatException;

/**
 * 模型加载器<br>
 * 单例模式，线程安全
 *
 * @author Apache_xiaochao
 *
 */
public class ModelLoader {

    private volatile static StanfordCoreNLP pipeline; // stanford coref need
    private volatile static ChunkerModel chunkerModel;  // open nlp chunk need

    private ModelLoader() {

    }

    /**
     * 加载stanford指代消解模型<br>
     * 单例模式，线程安全
     *
     * @return
     */
    public static StanfordCoreNLP getPipeLine() {
        if (pipeline == null) {
            synchronized (ModelLoader.class) {
                if (pipeline == null) {
                    Properties props = new Properties();
                    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
                    pipeline = new StanfordCoreNLP(props);
                }
            }
        }
        return pipeline;
    }

    /**
     * 获取open nlp chunk模型<br>
     * 单例模式，线程安全
     *
     * @return
     * @throws IOException
     * @throws InvalidFormatException
     */
    public static ChunkerModel getChunkerModel() throws IOException {
        if(chunkerModel == null) {
            synchronized (ModelLoader.class) {
                if(chunkerModel == null) {
                    chunkerModel = new ChunkerModel(ModelLoader.class.getClassLoader().getResource("en-chunker.bin"));
                }
            }
        }
        return chunkerModel;
    }

}
