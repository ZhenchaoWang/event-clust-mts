package edu.whu.cs.nlp.mts.clust;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import edu.whu.cs.nlp.mts.domain.Event;
import edu.whu.cs.nlp.mts.domain.EventToId;
import edu.whu.cs.nlp.mts.sys.SystemConstant;
import edu.whu.cs.nlp.mts.utils.CommonUtil;
import edu.whu.cs.nlp.mts.utils.FileUtil;

/**
 * 计算事件之间的相似度
 *
 * @author Apache_xiaochao
 */
public class CalculateSimilarityThread implements Callable<Boolean>, SystemConstant {

    private static Logger log = Logger.getLogger(CalculateSimilarityThread.class);

    private final String topicDir;
    private final String saveBaseDir;
    private final VectorOperation vo;

    public CalculateSimilarityThread(
            String topicDir, String saveBaseDir, String cacheName, int dimension, String datasource) {
        super();
        this.topicDir = topicDir;
        this.saveBaseDir = saveBaseDir;
        this.vo = new VectorOperation(cacheName, dimension, datasource);
    }

    @Override
    public Boolean call() {
        boolean success = false;
        final DecimalFormat df = new DecimalFormat("#0.0000000000");
        final String topicName = this.topicDir.substring(this.topicDir.lastIndexOf("/"));
        log.info(Thread.currentThread().getId() +  " is running, dir:" + this.topicDir);
        int num = 0; // 事件编号
        final List<EventToId> event_id_list = new ArrayList<EventToId>(); // 存放所有事件及其对应的序号
        final File f_event_files = new File(this.topicDir + "/" + DIR_EVENTS);
        final String[] filenames = f_event_files.list();
        final Map<Integer, List<Double[]>> eventVecsMap = new TreeMap<Integer, List<Double[]>>();  //记录每一个事件对应的向量集合，一个事件可能有多个向量
        if (filenames != null && filenames.length > 0) {
            for (final String filename : filenames) {
                try {
                    // 加载当前文件中的事件集合
                    final List<Event> eventsInFile =
                            FileUtil.loadEvents(this.topicDir + "/" + DIR_EVENTS + "/" + filename);
                    //对事件进行编号
                    for (final Event event : eventsInFile) {
                        //对事件进行编号，然后封装成对象存储
                        final EventToId event2Id = new EventToId();
                        event2Id.setEvent(event);
                        event2Id.setNum(num);
                        event_id_list.add(event2Id);
                        //计算当前事件对应的向量集合
                        try {
                            final List<Double[]> vecs = this.vo.event2Vecs(event);
                            if(vecs != null && vecs.size() > 0){
                                eventVecsMap.put(num, vecs);
                            }
                        } catch (final SQLException e) {
                            log.error("计算事件向量出错！", e);
                            //e.printStackTrace();
                        }
                        ++num;
                    }
                } catch (final IOException e) {
                    log.error("操作文件出错：" + this.topicDir + "/" + DIR_EVENTS + "/" + filename, e);
                    //e.printStackTrace();
                }
            }
        }

        //将编号的事件保存
        if (event_id_list.size() > 0) {
            // 将事件及其序号信息，写入文件
            final StringBuilder sb_nodes = new StringBuilder();
            for (final EventToId event2Id : event_id_list) {
                sb_nodes.append(event2Id.getNum() + "\t" + event2Id.getEvent().toString() + LINE_SPLITER);
            }
            final String str_nodes = CommonUtil.cutLastLineSpliter(sb_nodes.toString());
            try {
                FileUtil.write(this.saveBaseDir + "/" + DIR_NODES + "/" + topicName + ".node", str_nodes, DEFAULT_CHARSET);
            } catch (final IOException e) {
                log.error("写文件出错：" + this.saveBaseDir + "/" + DIR_NODES + "/" + topicName + ".node", e);
                //e.printStackTrace();
            }
        }

        // 将上面的事件集合转化成用map进行存储
        final Map<Integer, Event> eventMap = new TreeMap<Integer, Event>();
        for (final EventToId event2Id : event_id_list) {
            eventMap.put(event2Id.getNum(), event2Id.getEvent());
        }

        BufferedWriter bw = null;
        final String savePath = this.saveBaseDir + "/" + DIR_EDGES + "/" + topicName + ".edge";
        try {
            //如果存放文件路径不存在，则创建
            final File file = new File(savePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            bw = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(savePath), DEFAULT_CHARSET));

            // 计算事件之间的相似度，并保存成文件
            for (int i = 1; i <= num; ++i) {
                for (int j = i + 1; j <= num; ++j) {
                    double approx;
                    try {
                        approx = this.vo.eventsApproximationDegree(eventVecsMap.get(i), eventVecsMap.get(j));
                        if (approx > 0 && approx <= 1) {
                            // 当前节点之间有边
                            bw.write(i + "\t" + j + "\t" + df.format(approx) + LINE_SPLITER);
                            bw.write(j + "\t" + i + "\t" + df.format(approx) + LINE_SPLITER);
                        }
                    } catch (SQLException | IOException e) {
                        log.error("计算事件相似出错，事件1：" + eventMap.get(i) + "， 事件2：" + eventMap.get(j), e);
                        //e.printStackTrace();
                    }
                }
                bw.flush();
            }
            success = true;
        } catch (final IOException e) {
            log.error("", e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (final IOException e) {
                    log.error("", e);
                    //e.printStackTrace();
                }
            }
        }
        return success;
    }

}
