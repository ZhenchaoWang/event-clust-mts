package edu.whu.cs.nlp.mts.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.cs.nlp.mts.clust.CalculateSimilarityThread;
import edu.whu.cs.nlp.mts.clust.ClusterByChineseWhispers;
import edu.whu.cs.nlp.mts.domain.CWRunParam;
import edu.whu.cs.nlp.mts.extract.EventsExtractBasedOnGraph;
import edu.whu.cs.nlp.mts.sys.SystemConstant;

/**
 * 驱动类
 * @author ZhenchaoWang 2015-10-20 10:55:06
 *
 */
public class MTSBOEC implements SystemConstant{

    private static Logger log = LoggerFactory.getLogger(MTSBOEC.class);

    public static void main(String[] args) {

        if(args.length == 0){
            System.err.println("请指定配置文件！");
            return;
        }

        final String propFilePath = args[0];  //配置文件所在路径

        /*
         * 加载配置文件
         */
        final Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propFilePath));
        } catch (final IOException e) {
            log.error("load properties failed!", e);
            return;
        }

        //获取线程数
        final int threadNum = Integer.parseInt(properties.getProperty("threadNum", "2"));
        final String textDir = properties.getProperty("textDir");
        final String workDir = properties.getProperty("workDir");

        /**
         * 执行事件抽取操作
         */
        if("y".equalsIgnoreCase(properties.getProperty("isExtractEvent"))){
            log.info("正在进行事件提取..." + textDir);
            final File dirFile = new File(textDir);
            final String[] dirs = dirFile.list();
            final List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
            for (final String dir : dirs) {
                tasks.add(new EventsExtractBasedOnGraph(textDir + "/" + dir));
            }
            //执行完成之前，主线程阻塞
            if(tasks != null && tasks.size() > 0){
                final ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
                try {
                    final List<Future<Boolean>> futures = executorService.invokeAll(tasks);
                    if(futures != null){
                        for (final Future<Boolean> future : futures) {
                            future.get();
                        }
                    }

                } catch (InterruptedException | ExecutionException e) {
                    log.error("事件抽取任务组异常", e);
                    //e.printStackTrace();
                } finally{
                    executorService.shutdown();
                }
            }

        }else{
            log.info("不启用事件抽取！");
        }

        /**
         * 计算事件之间的相似度
         */
        final int nThreadSimiarity =
                Integer.parseInt(properties.getProperty("nThreadSimiarity"));  //计算事件相似度的线程数量
        if("y".equalsIgnoreCase(properties.getProperty("isCalculateSimilarity"))){
            log.info("正在计算事件相似度...");
            final String cacheName = properties.getProperty("cacheName");
            final int dimension = Integer.parseInt(properties.getProperty("dimension", "300"));
            final String datasource = properties.getProperty("datasource");
            final File dirFile = new File(textDir);
            final String[] dirs = dirFile.list();
            final List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
            for (final String dir : dirs) {
                tasks.add(
                        new CalculateSimilarityThread(
                                textDir + "/" + dir, workDir, cacheName, dimension, datasource));
            }
            if(tasks != null && tasks.size() > 0){
                final ExecutorService executorService = Executors.newFixedThreadPool(nThreadSimiarity);
                try {
                    final List<Future<Boolean>> futures = executorService.invokeAll(tasks);
                    if(futures != null){
                        for (final Future<Boolean> future : futures) {
                            future.get();
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("相似度计算任务组异常", e);
                    //e.printStackTrace();
                }finally{
                    executorService.shutdown();
                }
            }
        }else{
            log.info("不启用事件相似度计算!");
        }

        /**
         * 对事件进行聚类，同时按类别抽取时间所在子句
         */
        if("y".equalsIgnoreCase(properties.getProperty("isEventCluster"))){
            log.info("正在进行事件聚类和子句抽取...");
            final String nodesDir = workDir + "/" + DIR_NODES;
            final String edgeDir = workDir + "/" + DIR_EDGES;
            final String clustResultDir = workDir + "/" + DIR_CW_RESULT;
            final String sentencesSaveDir = workDir + "/" + DIR_EXTRACTED_SETENCES;
            final String moduleFilePath = workDir + "/en-pos-maxent.bin";
            final String dictPath = properties.getProperty("dictPath");
            final float edgeSelectedWeight = 3.2f;  //边阈值增加权重
            boolean isPret = true;
            boolean isClust = true;
            if("n".equalsIgnoreCase(properties.getProperty("isPret"))){
                isPret = false;
            }
            if("n".equalsIgnoreCase(properties.getProperty("isClust"))){
                isClust = false;
            }
            final ClusterByChineseWhispers cluster =
                    new ClusterByChineseWhispers(
                            nodesDir, edgeDir, clustResultDir, textDir,
                            sentencesSaveDir, moduleFilePath, threadNum, edgeSelectedWeight, isPret, isClust, dictPath);
            try {
                //对事件进行聚类
                //构建口哨算法运行参数
                final CWRunParam cwRunParam = new CWRunParam();
                cwRunParam.setJarPath(properties.getProperty("cwjarPath"));
                cwRunParam.setKeepClassRate(0.0f);
                cwRunParam.setMutation_rate(0.21f);
                cwRunParam.setIterationCount(100);
                cluster.doCluster(cwRunParam);
                //获取事件对应的句子
                cluster.clusterSentencesByEvents();
            } catch (IOException | InterruptedException e) {
                log.error("事件聚类出错！", e);
                //e.printStackTrace();
            }

        }else{
            log.info("不启用事件聚类和子句抽取!");
        }

    }

}
