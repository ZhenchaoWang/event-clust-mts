package edu.whu.cs.nlp.mts.sys;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

/**
 * 资源加载器
 *
 * @author ZhenchaoWang 2015-11-3 10:20:26
 *
 */
public class ResourceLoader {

    private static Logger log = Logger.getLogger(ResourceLoader.class);

    /**
     * 加载停用词列表，可以同时指定多个文件
     *
     * @param filenames
     * @return
     */
    public static Set<String> loadStopwords(String... filenames) {
        Set<String> stopwords = new HashSet<String>();
        for (String filename : filenames) {
            try {
                LineIterator iterator = null;
                try{
                    log.info("loading stopwords...");
                    iterator = FileUtils.lineIterator(
                            new File(ResourceLoader.class.getClassLoader().getResource(filename).toURI()), "UTF-8");

                    while(iterator.hasNext()) {
                        stopwords.add(iterator.nextLine());
                    }
                    log.info("load stopwords finished, count:" + stopwords.size());
                } finally {
                    if(iterator != null) {
                        iterator.close();
                    }
                }
            } catch (IOException | URISyntaxException e) {
                log.error("load stopwords error!", e);
            }
        }
        return stopwords;
    }

    public static void main(String[] args) {
        Set<String> set = ResourceLoader.loadStopwords("stopwords-en-default.txt", "stopwords-en-mysql.txt");
        int num = 0;
        for (String str : set) {
            System.out.println((++num) + "\t" + str);
        }
    }

}
