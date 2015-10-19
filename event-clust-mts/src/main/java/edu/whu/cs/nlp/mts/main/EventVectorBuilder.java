package edu.whu.cs.nlp.mts.main;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.whu.cs.nlp.mts.clust.EventVectorBuilderThread;

/**
 * 事件向量生成
 * @author Apache_xiaochao
 *
 */
public class EventVectorBuilder {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        //获取线程数
        final int threadNum = 4;
        final String textDir = "/home/eventChain/mts_dir/duc07.results.data/testdata/duc2007_testdocs/main_pret";
        final String workDir = "/home/eventChain/mts_dir";
        final String cacheName = "db_cache_vec";
        final int dimension = 300;
        final String datasource = "localhost-3306-user_vec";
        final File dirFile = new File(textDir);
        final String[] dirs = dirFile.list();
        final ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (final String dir : dirs) {
            executorService.execute(
                    new EventVectorBuilderThread(
                            textDir + "/" + dir, workDir, cacheName, dimension, datasource));
        }
        executorService.shutdown();
    }

}
