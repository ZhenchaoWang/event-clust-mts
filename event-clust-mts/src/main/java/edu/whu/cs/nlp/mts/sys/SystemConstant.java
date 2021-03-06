package edu.whu.cs.nlp.mts.sys;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * 系统常量
 *
 * @author Apache_xiaochao
 *
 */
public interface SystemConstant {

    /** 系统默认字符编码 */
    public final static Charset     DEFAULT_CHARSET                 = Charset.forName("UTF-8");

    /** 统一换行符，采用linux风格 */
    public final static String      LINE_SPLITER                    = "\n";

    /** 一个单词中所有属性的连接符，用于打印时进行组织 */
    public final static String      WORD_ATTRBUTE_CONNECTOR         = "__";

    /** 目录名：原文本 */
    public final static String      DIR_TEXT                        = "text";

    /** 目录名：句子切分 */
    public final static String      DIR_SEG_TEXT                    = "text_seg";

    /** 目录名：句子切分（详细） */
    public final static String      DIR_SEGDETAIL_TEXT              = "text_seg-detail";

    /** 目录名：依存分析 */
    public final static String      DIR_PARSE_TEXT                  = "text_parse";

    /** 目录名：依存分析（简版） */
    public static final String      DIR_PARSESIMPLIFY               = "text_parse-simplify";

    /** 目录名：事件抽取 */
    public final static String      DIR_EVENTS                      = "events";

    /** 目录名：事件抽取（简版） */
    public final static String      DIR_EVENTSSIMPLIFY              = "events-simplify";

    /** 目录名：指代消解 */
    public static final String      DIR_CR_EVENTS                   = "events_cr";

    /** 目录名：指代消解（简版） */
    public static final String      DIR_CR_EVENTSSIMPLIFY           = "events_cr-simplify";

    /** 目录名：事件修复 */
    public static final String      DIR_CR_RP_EVENTS                = "events_cr_rp";

    /** 目录名：事件修复（简版） */
    public static final String      DIR_CR_RP_EVENTSSIMPLIFY        = "events_cr_rp-simplify";

    /** 目录名：短语扩充 */
    public static final String      DIR_CR_RP_PE_EVENTS             = "events_cr_rp_pe";

    /** 目录名：短语扩充（简版） */
    public static final String      DIR_CR_RP_PE_EVENTSSIMPLIFY     = "events_cr_rp_pe-simplify";

    /** 目录名：事件过滤 */
    public static final String      DIR_CR_RP_PE_EF_EVENTS          = "events_cr_rp_pe_ef";

    /** 目录名：事件过滤（简版） */
    public static final String      DIR_CR_RP_PE_EF_EVENTSSIMPLIFY  = "events_cr_rp_pe_ef-simplify";

    /** 目录名：node文件 */
    public final static String      DIR_NODES                       = "nodes";

    /** 目录名：edge文件 */
    public final static String      DIR_EDGES                       = "edges";

    /** 目录名：词性标注 */
    public final static String      DIR_TAGGED                      = "tagged";

    /** 目录名：事件聚类 */
    public final static String      DIR_EVENTS_CLUST                = "events-clust";

    /** 目录名：子句抽取 */
    public final static String      DIR_SUB_SENTENCES_EXTRACTED     = "sub-sentences";

    /** 目录名：口哨算法预处理 */
    public final static String      DIR_CW_PRETREAT                 = "cw_pretreat";

    /** 目录名：多语句压缩 */
    public final static String      DIR_SENTENCES_COMPRESSION       = "sentences-compression";

    /** 目录名：chunk处理得到的短语集合（简版） */
    public static final String      DIR_CHUNKSIMPILY                = "chunk-simpily";

    /** 事件中词之间的连接符 */
    public final static String      WORD_CONNECTOR_IN_EVENTS        = "#";

    /** 事件所在文件名标记符：左 */
    public final static String      FILENAME_REST_LEFT              = "[$";

    /** 事件所在文件名标记符：右 */
    public final static String      FILENAME_REST_RIGHT             = "$]";

    /** 选择非最大相似度的权值 */
    public final static int         VARIATION_WEIGHT                = 80;

    /**
     * 句子数阈值，当类别中的句子数要大于等于该阈值才能进入压缩
     */
    public final static int         MIN_SENTENCE_COUNT_FOR_COMPRESS = 3;

    /**
     * 最大摘要总词数
     */
    public final static int         MAX_SUMMARY_WORDS_COUNT         = 250;

    /**
     * 人称代词 + 所有格代词
     */
    public final static Set<String> POS_PRP                         = new HashSet<String>() {
        private static final long serialVersionUID = 3536875708378397981L;

        {
            add("PRP");
            add("PRP$");
            add("WP");
            add("WP$");
        }
    };

    /**
     * 指示代词集合，后续根据实际情况进行补充
     */
    public final static Set<String> DEMONSTRACTIVE_PRONOUN          = new HashSet<String>() {

        private static final long serialVersionUID = -1988404852361670496L;

        {
            add("i");
            add("you");
            add("he");
            add("she");
            add("it");
            add("we");
            add("they");
            add("me");
            add("him");
            add("her");
            add("us");
            add("them");
            add("this");
            add("that");
            add("these");
            add("those");
            add("who");
            add("which");
            add("what");
        }
    };

    /**
     * 不希望被指代的词集合，后续根据实际情况进行补充
     */
    public final static Set<String> EXCEPTED_DEMONSTRACTIVE_PRONOUN = new HashSet<String>() {

        private static final long serialVersionUID = -1988404852361670496L;

        {
            add("i");
            add("you");
            add("he");
            add("she");
            add("it");
            add("we");
            add("they");
            add("me");
            add("him");
            add("her");
            add("us");
            add("them");
            add("its");
            add("this");
            add("that");
            add("these");
            add("those");
            add("my");
            add("your");
            add("his");
            add("their");
            add("who");
            add("which");
            add("what");
        }
    };

    /**
     * 词性标签-名词
     */
    public final static Set<String> POS_NOUN                        = new HashSet<String>() {
        private static final long serialVersionUID = -4215344365700028825L;

        {
            add("NN");
            add("NNS");
            add("NNP");
            add("NNPS");
        }
    };

    /**
     * 词性标签-动词
     */
    public final static Set<String> POS_VERB                        = new HashSet<String>() {
        private static final long serialVersionUID = 92436997464208966L;

        {
            add("VB");
            add("VBD");
            add("VBG");
            add("VBN");
            add("VBP");
            add("VBZ");
        }
    };

    /**
     * 词性标签-副词
     */
    public final static Set<String> POS_ADVERB                      = new HashSet<String>() {
        private static final long serialVersionUID = -4717718652903957444L;

        {
            add("RB");
            add("RBR");
            add("RBS");
        }
    };

    /**
     * 词性标签-形容词
     */
    public final static Set<String> POS_ADJ                         = new HashSet<String>() {
        private static final long serialVersionUID = 1739698370056824950L;

        {
            add("JJ");
            add("JJR");
            add("JJS");
        }
    };

    /**
     * 依存关系：施事
     */
    public final static Set<String> DEPENDENCY_AGENT                = new HashSet<String>() {
        private static final long serialVersionUID = -4819853309587426759L;

        {
            add("nsubj");
            add("xsubj");
            add("csubj");
            add("agent");
        }
    };

    /**
     * 依存关系：受事
     */
    public final static Set<String> DEPENDENCY_OBJECT               = new HashSet<String>() {
        private static final long serialVersionUID = -4819853309587426759L;

        {
            add("dobj");
            add("nsubjpass");
            add("acomp");
            add("xcomp");
        }
    };

    /** 序列化文件后缀 */
    public static final String      SUFFIX_SERIALIZE_FILE           = ".obj";

    /** 目录名：序列化的文件 */
    public static final String      DIR_SERIALIZE_EVENTS            = "serializable-events";

    /** 词向量维度 */
    public static final Integer     DIMENSION                       = 300;

    /** 停用词列表 */
    public static final Set<String> STOPWORDS                       = ResourceLoader.loadStopwords("stopwords-en-default.txt");

}
