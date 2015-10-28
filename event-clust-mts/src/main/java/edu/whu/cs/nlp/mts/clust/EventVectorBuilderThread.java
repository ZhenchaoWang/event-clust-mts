package edu.whu.cs.nlp.mts.clust;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.whu.cs.nlp.mts.domain.EventWithWord;
import edu.whu.cs.nlp.mts.domain.EventToId;
import edu.whu.cs.nlp.mts.sys.SystemConstant;
import edu.whu.cs.nlp.mts.utils.CommonUtil;
import edu.whu.cs.nlp.mts.utils.FileUtil;

/**
 * 事件向量生成线程
 * @author Apache_xiaochao
 *
 */
public class EventVectorBuilderThread implements Runnable, SystemConstant{

    private final Logger log = Logger.getLogger(this.getClass());

    private final String topicDir;
    private final String saveBaseDir;
    private final VectorOperation vo;

    public EventVectorBuilderThread(
            String topicDir, String saveBaseDir, String cacheName, int dimension, String datasource) {
        super();
        this.topicDir = topicDir;
        this.saveBaseDir = saveBaseDir;
        this.vo = new VectorOperation(cacheName, dimension, datasource);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        final DecimalFormat df = new DecimalFormat("#0.000");
        final String topicName = this.topicDir.substring(this.topicDir.lastIndexOf("/"));
        this.log.info(Thread.currentThread().getId() +  " is calculating event vectors, dir:" + this.topicDir);
        int num = 0; // 事件编号
        final List<EventToId> event_id_list = new ArrayList<EventToId>(); // 存放所有事件及其对应的序号
        final File f_event_files = new File(this.topicDir + "/" + DIR_EVENTS);
        final String[] filenames = f_event_files.list();
        final Map<Integer, List<Double[]>> eventVecsMap = new TreeMap<Integer, List<Double[]>>();  //记录每一个事件对应的向量集合，一个事件可能有多个向量
        final Map<Integer, String> eventMap = new TreeMap<Integer, String>();  //记录事件与编号之间的对应的关系
        if (filenames != null && filenames.length > 0) {
            for (final String filename : filenames) {
                try {
                    // 加载当前文件中的事件集合
                    final List<EventWithWord> eventsInFile =
                            FileUtil.loadEvents(this.topicDir + "/" + DIR_EVENTS + "/" + filename);
                    //对事件进行编号
                    for (final EventWithWord event : eventsInFile) {
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
                                eventMap.put(num, event.toShortString());
                            }
                        } catch (final SQLException e) {
                            this.log.error("计算事件向量出错！", e);
                            //e.printStackTrace();
                        }
                        ++num;
                    }
                } catch (final IOException e) {
                    this.log.error("操作文件出错：" + this.topicDir + "/" + DIR_EVENTS + "/" + filename, e);
                    //e.printStackTrace();
                }
            }
        }

        //将事件，及其对应的向量保存
        final StringBuilder sb_event = new StringBuilder();
        final StringBuilder sb_event_vecs = new StringBuilder();
        int serial = 0;
        for (int i = 1; i <= num; ++i) {
            if(eventMap.get(i) != null
                    && eventVecsMap.get(i) != null && eventVecsMap.get(i).size() > 0){
                sb_event.append((serial ++) + "\t" + (i - 1) + "\t" + eventMap.get(i) + LINE_SPLITER);
                final StringBuilder sb_tmp = new StringBuilder();
                for (final Double d : eventVecsMap.get(i).get(0)) {
                    sb_tmp.append(df.format(d) + "\t");
                }
                sb_event_vecs.append(sb_tmp.toString().trim() + LINE_SPLITER);
            }
        }
        try {
            FileUtil.write(
                    this.saveBaseDir + "/vectors/events/" + topicName,
                    CommonUtil.cutLastLineSpliter(sb_event.toString()), DEFAULT_CHARSET);
            FileUtil.write(
                    this.saveBaseDir + "/vectors/vecs/" + topicName,
                    CommonUtil.cutLastLineSpliter(sb_event_vecs.toString()), DEFAULT_CHARSET);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            this.log.error("事件向量存储错误！", e);
            //e.printStackTrace();
        }
    }

}
