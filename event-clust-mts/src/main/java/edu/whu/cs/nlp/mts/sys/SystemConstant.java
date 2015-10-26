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

    /**
     * 系统默认字符编码
     */
    public final static Charset     DEFAULT_CHARSET                 = Charset.forName("UTF-8");

    /**
     * 统一换行符，采用linux风格
     */
    public final static String      LINE_SPLITER                    = "\n";

    /**
     * 一个单词中所有属性的连接符，用于打印时进行组织
     */
    public final static String      WORD_ATTRBUTE_CONNECTOR         = "__";

    /**
     * 经过句子切分之后的文本所存放的目录
     */
    public final static String      DIR_TEXT                        = "seg_text";

    /**
     * 经过指代消解之后的文本所存放的目录名
     */
    public final static String      DIR_CR_TEXT                     = "cr_seg_text";

    /**
     * 经过指代消解之后的文本详细信息存放的目录名
     */
    public final static String      DIR_CR_TEXT_DETAIL              = "cr_seg_text_detail";

    /**
     * 依存分析结果存放目录名
     */
    public final static String      DIR_PARSE                       = "parsed";

    /**依存分析结果简版存放目录*/
    public static final String      DIR_PARSE_SIMPLE                = "parsed_simplify";

    /**
     * 存放事件抽取结果的目录名
     */
    public final static String      DIR_EVENTS                      = "events";

    /**
     * 采用精简表示形式的事件所存放的目录
     */
    public final static String      DIR_SIMPLIFY_EVENT              = "events_simplify";

    /**
     * node文件存放目录名称
     */
    public final static String      DIR_NODES                       = "nodes";

    /**
     * edge文件存放目录名称
     */
    public final static String      DIR_EDGES                       = "edges";

    /**
     * 词性标注结果存放目录名
     */
    public final static String      DIR_TAGGED                      = "tagged";

    /**
     * 事件聚类结果存放文件名
     */
    public final static String      DIR_CW_RESULT                   = "cw_result";

    /**
     * 抽取的子句存放文件名
     */
    public final static String      DIR_EXTRACTED_SETENCES          = "extracted_sentences";

    /**
     * 口哨算法预处理结果存放路径
     */
    public final static String      DIR_CW                          = "cw";

    /**
     * 句子压缩结果存放的文件夹
     */
    public final static String      DIR_COMPRESS                    = "compress";

    /**
     * 事件中词之间的连接符
     */
    public final static String      WORD_CONNECTOR                  = "#";

    /**
     * 事件中文件名的左右分割符
     */
    public final static String      FILENAME_REST_LEFT              = "[$",
            FILENAME_REST_RIGHT = "$]";

    /**
     * 选择非最大相似度的权值
     */
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
     * 人称指代
     *//*
     * public final static Set<String> PERSON_PRONOUN2 = new HashSet<String>()
     * {
     *
     * private static final long serialVersionUID = -1988404852361670496L;
     *
     * { add("he"); add("she"); add("it"); add("they"); add("who"); } };
     */

    /**
     * 人称指代
     */
    public final static Set<String> POS_PERSON_PRONOUN              = new HashSet<String>() {
        private static final long serialVersionUID = 3536875708378397981L;

        {
            add("PRP");
        }
    };

    /**
     * 人称代词 + 所有格代词
     */
    public final static Set<String> POS_PRONOUN                     = new HashSet<String>() {
        private static final long serialVersionUID = 3536875708378397981L;

        {
            add("PRP");
            add("PRP$");
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
            //add("ccomp");  // ccomp不靠谱
            add("xcomp");
        }
    };

}
