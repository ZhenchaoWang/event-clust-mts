package edu.whu.cs.nlp.mts.clust;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.cs.nlp.mts.domain.Event;
import edu.whu.cs.nlp.mts.sys.SystemConstant;
import edu.whu.cs.nlp.mts.utils.EhCacheUtil;

/**
 * 向量操作相关类
 * @author Apache_xiaochao
 *
 */
public class VectorOperation implements SystemConstant{

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EhCacheUtil ehCacheUtil;
    private final int dimension;  //向量维度

    public VectorOperation(String cacheName, int dimension, String datasource) {
        super();
        this.dimension = dimension;
        this.ehCacheUtil = new EhCacheUtil(cacheName, dimension, datasource);
    }

    /**
     * 判断事件所属类型
     * @param event
     * @return 3：主-谓-宾；2：主-谓；1：谓-宾；0：未知事件
     */
    private int eventType(Event event){
        int type = 0;
        if(event.getLeftWord() != null
                && event.getMiddleWord() != null
                && event.getRightWord() != null){
            type = 3;
        } else if(event.getLeftWord() != null
                && event.getMiddleWord() != null){
            type = 2;
        } else if(event.getMiddleWord() != null
                && event.getRightWord() != null){
            type = 1;
        }
        return type;
    }

    /**
     * 将事件转化成向量
     * @param event
     * @param wordVec
     * @return
     */
    @Deprecated
    public double[] event2Vec(Event event, Map<String, Float[]> wordVec){
        double[] eventVec = null;
        if(event != null && wordVec != null){
            if(eventType(event) == 3){
                final Float[] vec_left = wordVec.get(event.getLeftWord().getLemma());
                final Float[] vec_middle = wordVec.get(event.getMiddleWord().getLemma());
                final Float[] vec_right = wordVec.get(event.getRightWord().getLemma());
                if(vec_left != null
                        && vec_middle != null && vec_right != null){
                    final double[][] kronecker_left_right = new double[this.dimension][this.dimension];  //存储主语为宾语的克罗内克积
                    //计算主语和宾语的克罗内卡积
                    for(int i = 0 ; i < this.dimension; ++i){
                        for(int j = 0; j < this.dimension; ++j){
                            kronecker_left_right[i][j] = vec_left[j] * vec_right[i];
                        }
                    }
                    //将得到的克罗内卡积与谓语作矩阵乘法
                    eventVec = new double[this.dimension];
                    for(int i = 0; i < this.dimension; ++i){
                        double product = 0;
                        for(int j = 0; j < this.dimension; ++j){
                            product += vec_middle[j] * kronecker_left_right[j][i];
                        }
                        eventVec[i] = product;
                    }

                }else{
                    //对于词向量模型中不存在的词，暂时不予考虑
                }
            } else {
                //暂时不考虑三元组以外的事件类型
            }
        }
        return eventVec;
    }

    /**
     * 根据时间中三个词的已知向量，来计算对应事件的向量
     * @param vecs_left
     * @param vecs_middle
     * @param vecs_right
     * @return
     */
    public List<Double[]> event2Vecs(
            List<Float[]> vecs_left, List<Float[]> vecs_middle, List<Float[]> vecs_right){
        final List<Double[]> eventVecs = new ArrayList<Double[]>();
        for (final Float[] f_v_left : vecs_left) {
            for (final Float[] f_v_middle : vecs_middle) {
                for (final Float[] f_v_right : vecs_right) {
                    final double[][] kronecker_left_right = new double[this.dimension][this.dimension];  //存储主语为宾语的克罗内克积
                    //计算主语和宾语的克罗内卡积
                    for(int i = 0 ; i < this.dimension; ++i){
                        for(int j = 0; j < this.dimension; ++j){
                            kronecker_left_right[i][j] = f_v_right[i] * f_v_left[j];
                        }
                    }
                    //将得到的克罗内卡积与谓语作矩阵乘法
                    final Double[] eventVec = new Double[this.dimension];
                    for(int i = 0; i < this.dimension; ++i){
                        double product = 0;
                        for(int j = 0; j < this.dimension; ++j){
                            product += f_v_middle[j] * kronecker_left_right[j][i];
                        }
                        eventVec[i] = product;
                    }
                    eventVecs.add(eventVec);
                }
            }
        }
        return eventVecs;
    }

    /**
     * 将事件转化成向量
     * @param event
     * @return
     * @throws SQLException
     */
    public List<Double[]> event2Vecs(Event event) throws SQLException{
        List<Double[]> eventVecs = new ArrayList<Double[]>();
        if(event != null){
            //创建一个值全为1的词向量
            final Float[] all_1_vec = new Float[this.dimension];
            Arrays.fill(all_1_vec, 1f);

            if(eventType(event) == 3){
                //主-谓-宾结构
                final List<Float[]> vecs_left = this.ehCacheUtil.getVec(event.getLeftWord());
                final List<Float[]> vecs_middle = this.ehCacheUtil.getVec(event.getMiddleWord());
                final List<Float[]> vecs_right = this.ehCacheUtil.getVec(event.getRightWord());
                if(vecs_left.size() > 0 && vecs_middle.size() > 0 && vecs_right.size() > 0){
                    eventVecs = event2Vecs(vecs_left, vecs_middle, vecs_right);
                }else{
                    this.log.warn("当前事件中存在未知的词向量：" + event);
                }
            } else if(eventType(event) == 2){
                //主-谓，将宾语的向量全部用1代替
                final List<Float[]> vecs_left = this.ehCacheUtil.getVec(event.getLeftWord());
                final List<Float[]> vecs_middle = this.ehCacheUtil.getVec(event.getMiddleWord());
                final List<Float[]> vecs_right = new ArrayList<Float[]>();
                vecs_right.add(all_1_vec);
                if(vecs_left.size() > 0 && vecs_middle.size() > 0 && vecs_right.size() > 0){
                    eventVecs = event2Vecs(vecs_left, vecs_middle, vecs_right);
                }else{
                    this.log.warn("当前事件中存在未知的词向量：" + event);
                }
            } else if(eventType(event) == 1){
                //谓-宾，将主语的向量全部用1代替
                final List<Float[]> vecs_left = new ArrayList<Float[]>();
                vecs_left.add(all_1_vec);
                final List<Float[]> vecs_middle = this.ehCacheUtil.getVec(event.getMiddleWord());
                final List<Float[]> vecs_right = this.ehCacheUtil.getVec(event.getRightWord());
                if(vecs_left.size() > 0 && vecs_middle.size() > 0 && vecs_right.size() > 0){
                    eventVecs = event2Vecs(vecs_left, vecs_middle, vecs_right);
                }else{
                    this.log.warn("当前事件中存在未知的词向量：" + event);
                }
            }else{
                this.log.info("不支持该事件类型：" + event);
            }
        }
        return eventVecs;
    }

    /**
     * 计算两个事件之间的近似度
     * 策略三：
     * 采用“Experimental Support for a Categorical Compositional Distributional Model of Meaning.pdf”中的方法
     * 将模型全部载入内存
     * @param event1
     * @param event2
     * @return
     */
    @Deprecated
    public double eventsApproximationDegree(
            Event event1, Event event2, Map<String, Float[]> wordVec){
        double approx = 0;  //默认以最大值来表示两个事件之间的最大值
        if(event1 != null && event2 != null && wordVec != null){
            //计算得到两个事件的向量
            final double[] event_vec_1 = event2Vec(event1, wordVec);
            final double[] event_vec_2 = event2Vec(event2, wordVec);
            if(event_vec_1 != null & event_vec_2 != null){
                //利用向量余弦值来计算事件之间的相似度
                double scalar = 0;  //两个向量的内积
                double module_1 = 0, module_2 = 0;  //向量vec_1和vec_2的模
                for(int i = 0; i < this.dimension; ++i){
                    scalar += event_vec_1[i] * event_vec_2[i];
                    module_1 += event_vec_1[i] * event_vec_1[i];
                    module_2 += event_vec_2[i] * event_vec_2[i];
                }
                if(module_1 > 0 && module_2 > 0) {
                    approx = scalar / (Math.sqrt(module_1) * Math.sqrt(module_2));
                }
            }else{
                return Double.MAX_VALUE;
            }
        }
        return approx;
    }

    /**
     * 计算两个事件之间的近似度
     * 策略三：
     * 采用“Experimental Support for a Categorical Compositional Distributional Model of Meaning.pdf”中的方法
     * 采用数据库+缓存框架
     * @param event1
     * @param event2
     * @return
     * @throws SQLException
     */
    public double eventsApproximationDegree(Event event1, Event event2) throws SQLException{
        double approx = 0;  //默认以最大值来表示两个事件之间的最大值
        if(event1 != null && event2 != null){
            //计算得到两个事件的向量
            final List<Double[]> event_vecs_1 = event2Vecs(event1);
            final List<Double[]> event_vecs_2 = event2Vecs(event2);
            if(event_vecs_1.size() > 0 && event_vecs_2.size() > 0){
                final Random random = new Random(System.currentTimeMillis());
                final int r_value = random.nextInt(100);
                if(r_value >= VARIATION_WEIGHT){
                    //随机选择一个值作为相似度
                    Double[] event_vec_1 = null;
                    Double[] event_vec_2 = null;
                    int times = 0;
                    while((event_vec_1 = event_vecs_1.get(random.nextInt(event_vecs_1.size()))) == null){
                        if(++times >= event_vecs_1.size()){
                            return Double.MAX_VALUE;
                        }
                    }
                    times = 0;
                    while((event_vec_2 = event_vecs_2.get(random.nextInt(event_vecs_2.size()))) == null){
                        if(++times >= event_vecs_2.size()){
                            return Double.MAX_VALUE;
                        }
                    }
                    //利用向量余弦值来计算事件之间的相似度
                    double scalar = 0;  //两个向量的内积
                    double module_1 = 0, module_2 = 0;  //向量vec_1和vec_2的模
                    for(int i = 0; i < this.dimension; ++i){
                        scalar += event_vec_1[i] * event_vec_2[i];
                        module_1 += event_vec_1[i] * event_vec_1[i];
                        module_2 += event_vec_2[i] * event_vec_2[i];
                    }
                    if(module_1 > 0 && module_2 > 0) {
                        approx = scalar / (Math.sqrt(module_1) * Math.sqrt(module_2));
                    }
                }else{
                    //选择最大值作为相似度
                    double max_approxs = 0;  //记录计算过程中的最大相似度
                    double approxTmp = max_approxs;
                    for (final Double[] event_vec_1 : event_vecs_1) {
                        for (final Double[] event_vec_2 : event_vecs_2) {
                            //利用向量余弦值来计算事件之间的相似度
                            double scalar = 0;  //两个向量的内积
                            double module_1 = 0, module_2 = 0;  //向量vec_1和vec_2的模
                            for(int i = 0; i < this.dimension; ++i){
                                scalar += event_vec_1[i] * event_vec_2[i];
                                module_1 += event_vec_1[i] * event_vec_1[i];
                                module_2 += event_vec_2[i] * event_vec_2[i];
                            }
                            if(module_1 > 0 && module_2 > 0) {
                                approxTmp = scalar / (Math.sqrt(module_1) * Math.sqrt(module_2));
                                if(approxTmp > max_approxs){
                                    max_approxs = approxTmp;
                                }
                                //approxs.add(scalar / (Math.sqrt(module_1) * Math.sqrt(module_2)));
                            }
                        }
                    }
                    approx = max_approxs;
                }
            }
        }
        return approx;
    }

    /**
     * 计算两个事件之间的近似度
     * 策略三：
     * 采用“Experimental Support for a Categorical Compositional Distributional Model of Meaning.pdf”中的方法
     * 采用数据库+缓存框架
     * @param event_vecs_1
     * @param event_vecs_2
     * @return
     * @throws SQLException
     */
    public double eventsApproximationDegree(
            List<Double[]> event_vecs_1, List<Double[]> event_vecs_2) throws SQLException{
        double approx = 0;  //默认以最大值来表示两个事件之间的最大值
        if(event_vecs_1 == null || event_vecs_2 == null){
            return approx;
        }
        //计算得到两个事件的向量
        if(event_vecs_1.size() > 0 && event_vecs_2.size() > 0){
            final Random random = new Random(System.currentTimeMillis());
            final int r_value = random.nextInt(100);
            if(r_value >= VARIATION_WEIGHT){
                //随机选择一个值作为相似度
                Double[] event_vec_1 = null;
                Double[] event_vec_2 = null;
                int times = 0;
                while((event_vec_1 = event_vecs_1.get(random.nextInt(event_vecs_1.size()))) == null){
                    if(++times >= event_vecs_1.size()){
                        return Double.MAX_VALUE;
                    }
                }
                times = 0;
                while((event_vec_2 = event_vecs_2.get(random.nextInt(event_vecs_2.size()))) == null){
                    if(++times >= event_vecs_2.size()){
                        return Double.MAX_VALUE;
                    }
                }
                //利用向量余弦值来计算事件之间的相似度
                double scalar = 0;  //两个向量的内积
                double module_1 = 0, module_2 = 0;  //向量vec_1和vec_2的模
                for(int i = 0; i < this.dimension; ++i){
                    scalar += event_vec_1[i] * event_vec_2[i];
                    module_1 += event_vec_1[i] * event_vec_1[i];
                    module_2 += event_vec_2[i] * event_vec_2[i];
                }
                if(module_1 > 0 && module_2 > 0) {
                    approx = scalar / (Math.sqrt(module_1) * Math.sqrt(module_2));
                }
            }else{
                //选择最大值作为相似度
                double max_approxs = 0;  //记录计算过程中的最大相似度
                double approxTmp = max_approxs;
                for (final Double[] event_vec_1 : event_vecs_1) {
                    for (final Double[] event_vec_2 : event_vecs_2) {
                        //利用向量余弦值来计算事件之间的相似度
                        double scalar = 0;  //两个向量的内积
                        double module_1 = 0, module_2 = 0;  //向量vec_1和vec_2的模
                        for(int i = 0; i < this.dimension; ++i){
                            scalar += event_vec_1[i] * event_vec_2[i];
                            module_1 += event_vec_1[i] * event_vec_1[i];
                            module_2 += event_vec_2[i] * event_vec_2[i];
                        }
                        if(module_1 > 0 && module_2 > 0) {
                            approxTmp = scalar / (Math.sqrt(module_1) * Math.sqrt(module_2));
                            if(approxTmp > max_approxs){
                                max_approxs = approxTmp;
                            }
                            //approxs.add(scalar / (Math.sqrt(module_1) * Math.sqrt(module_2)));
                        }
                    }
                }
                approx = max_approxs;
            }
        }
        return approx;
    }

}
