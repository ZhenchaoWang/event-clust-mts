package edu.whu.cs.nlp.mts.extract;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.whu.cs.nlp.mts.domain.CoreferenceElement;
import edu.whu.cs.nlp.mts.domain.Event;
import edu.whu.cs.nlp.mts.domain.EventWithPhrase;
import edu.whu.cs.nlp.mts.domain.EventWithWord;
import edu.whu.cs.nlp.mts.domain.ParseItem;
import edu.whu.cs.nlp.mts.domain.Word;
import edu.whu.cs.nlp.mts.nlp.StanfordNLPTools;
import edu.whu.cs.nlp.mts.sys.ModelLoader;
import edu.whu.cs.nlp.mts.sys.SystemConstant;
import edu.whu.cs.nlp.mts.utils.CommonUtil;
import edu.whu.cs.nlp.mts.utils.Encipher;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.Span;

/**
 * 基于依存关系来构建词图，在词图的基础上基于规则进行事件抽取<br>
 * 规则如下：<br>
 * 1.先进行事件抽取，再进行指代消解
 *
 * @version 2.0
 * @author ZhenchaoWang 2015-10-26 19:38:22
 *
 */
public class EventsExtractBasedOnGraphV2 implements SystemConstant, Callable<Boolean>{

    private final Logger log = Logger.getLogger(this.getClass());

    /**获取切分后的句子集合的Key*/
    public static final String SEGED_TEXT = "SEGED_TEXT";

    /**获取切分后的句子详细信息的key*/
    public static final String SEGED_TEXT_DETAIL = "SEGED_TEXT_DETAIL";

    /**获取仅包含词性的文本信息*/
    public static final String SEGED_TEXT_POS = "SEGED_TEXT_POS";

    /**获取所有词的对象信息*/
    public static final String WORDS = "WORDS";

    /**获取所有词的依存关系集合*/
    public static final String PARSED_ITEMS = "PARSED_ITEMS";

    private final StanfordCoreNLP pipeline;
    /**输入文件所在目录*/
    private final String textDir;

    /**
     * 构造函数
     * @param textDir 输入文件目录
     */
    public EventsExtractBasedOnGraphV2(String textDir) {
        super();
        this.textDir = textDir;
        this.pipeline = ModelLoader.getPipeLine();
    }

    @Override
    public Boolean call() throws Exception {

        boolean success = true;

        // 获取指定文件夹下的所有文件
        Collection<File> files = FileUtils.listFiles(FileUtils.getFile(this.textDir), null, false);
        //Pretreatment pretreatment = new Pretreatment();  // 预处理器

        for (File file : files) {
            //加载文件
            String absolutePath = file.getAbsolutePath();  // 当前处理的文件的绝对路径
            String parentPath = file.getParentFile().getAbsolutePath();  // 文件所属文件夹的路径

            this.log.info(Thread.currentThread().getId() + "正在操作文件：" + absolutePath);
            try {
                // 加载正文
                String text = FileUtils.readFileToString(file, DEFAULT_CHARSET);

                // 对文本进行句子切分和指代消解
                /*final Map<String, String> preTreatResult = pretreatment.coreferenceResolution(text);
                FileUtil.write(parentPath + "/" + DIR_TEXT + "/" + file.getName(), preTreatResult.get(Pretreatment.KEY_SEG_TEXT), DEFAULT_CHARSET);
                text = preTreatResult.get(Pretreatment.KEY_CR_TEXT);*/

                // 利用stanford的nlp核心工具进行处理
                Map<String, Object> coreNlpResults =  this.coreNlpOperate(text);

                //获取句子切分后的文本
                String segedtext = (String) coreNlpResults.get(SEGED_TEXT);
                /*中间结果记录：记录句子切分后的文本*/
                FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_CR_TEXT, file.getName()), segedtext, DEFAULT_CHARSET);

                //获取句子切分后的文本详细信息
                String segedTextDetail = (String) coreNlpResults.get(SEGED_TEXT_DETAIL);
                /*中间结果记录：记录句子切分后的文本详细信息*/
                FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_CR_TEXT_DETAIL, file.getName()), segedTextDetail, DEFAULT_CHARSET);

                //获取句子切分后的带有词性的文本信息
                String segedTextPOS = (String) coreNlpResults.get(SEGED_TEXT_POS);
                /*中间结果记录：记录句子切分后的带有词性的文本信息*/
                FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_CR_TEXT_DETAIL + "/pos", file.getName()), segedTextPOS, DEFAULT_CHARSET);

                //获取对句子中单词进行对象化后的文本
                @SuppressWarnings("unchecked")
                List<List<Word>> words = (List<List<Word>>) coreNlpResults.get(WORDS);

                StringBuilder sb_words_pos = new StringBuilder();
                for (List<Word> list : words) {
                    StringBuilder sb_words = new StringBuilder();
                    StringBuilder sb_pos = new StringBuilder();
                    for (Word word : list) {
                        sb_words.append(word.getName() + " ");
                        sb_pos.append(word.getPos() + " ");
                    }
                    sb_words_pos.append(sb_words.toString().trim() + LINE_SPLITER);
                    sb_words_pos.append(sb_pos.toString().trim() + LINE_SPLITER);
                }
                /*中间结果记录：词和词性分开按行存储*/
                FileUtils.writeStringToFile(
                        FileUtils.getFile(parentPath + "/" + DIR_CR_TEXT_DETAIL + "/pos2/", file.getName()),
                        CommonUtil.cutLastLineSpliter(sb_words_pos.toString()), DEFAULT_CHARSET);

                //获取依存分析结果
                @SuppressWarnings("unchecked")
                List<List<ParseItem>> parseItemList = (List<List<ParseItem>>) coreNlpResults.get(PARSED_ITEMS);
                FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_PARSE, file.getName()), CommonUtil.lists2String(parseItemList), DEFAULT_CHARSET);

                /*中间结果记录：记录依存分析简版结果*/
                StringBuilder simplifyParsedResult = new StringBuilder();
                for (List<ParseItem> parseItems : parseItemList) {
                    for (ParseItem parseItem : parseItems)
                        simplifyParsedResult.append(parseItem.toShortString() + "\t");
                    simplifyParsedResult.append(LINE_SPLITER);
                }
                FileUtils.writeStringToFile(
                        FileUtils.getFile(parentPath + "/" + DIR_PARSE_SIMPLE, file.getName()),
                        CommonUtil.cutLastLineSpliter(simplifyParsedResult.toString()),
                        DEFAULT_CHARSET);

                // 对当前文本进行事件抽取
                Map<Integer, List<EventWithWord>> events = this.extract(parseItemList, words, file.getName());
                /*中间结果记录：保存事件抽取结果*/
                StringBuilder sb_events = new StringBuilder();
                StringBuilder sb_simplify_events = new StringBuilder();
                for (Entry<Integer, List<EventWithWord>> entry : events.entrySet()) {
                    String eventsInSentence = CommonUtil.list2String(entry.getValue());
                    sb_events.append(entry.getKey() + "\t" + eventsInSentence + LINE_SPLITER);
                    sb_simplify_events.append(entry.getKey() + "\t" + this.getSimpilyEvents(entry.getValue()) + LINE_SPLITER);
                }
                FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_EVENTS, file.getName()), CommonUtil.cutLastLineSpliter(sb_events.toString()), DEFAULT_CHARSET);
                FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_SIMPLIFY_EVENT, file.getName()), CommonUtil.cutLastLineSpliter(sb_simplify_events.toString()), DEFAULT_CHARSET);

                /**
                 * 指代消解
                 */
                Map<String, CoreferenceElement> crChains = StanfordNLPTools.cr(text);
                /*存放指代消解之后的事件，按行组织*/
                Map<Integer, List<EventWithPhrase>> eventsAfterCR = new TreeMap<Integer, List<EventWithPhrase>>();
                try {
                    for (Entry<Integer, List<EventWithWord>> entry : events.entrySet()) {
                        Integer sentNum = entry.getKey();
                        List<EventWithWord> eventsInSentence = entry.getValue();
                        List<EventWithPhrase> eventWithPhraseInSentence = new ArrayList<EventWithPhrase>();
                        for (EventWithWord event : eventsInSentence) {
                            Word leftWord = event.getLeftWord();
                            Word negWord = event.getNegWord();
                            Word middleWord = event.getMiddleWord();
                            Word rightWord = event.getRightWord();
                            List<Word> leftPhrase = this.changeToCorefWord(leftWord, crChains, words);
                            if(CollectionUtils.isEmpty(leftPhrase) && leftWord != null) {
                                leftPhrase.add(leftWord);
                            }
                            List<Word> rightPhrase = this.changeToCorefWord(rightWord, crChains, words);
                            if(CollectionUtils.isEmpty(rightPhrase) && rightWord != null) {
                                rightPhrase.add(rightWord);
                            }
                            List<Word> middlePhrase = new ArrayList<Word>();
                            if(negWord != null) {
                                middlePhrase.add(negWord);
                            }
                            middlePhrase.add(middleWord);
                            eventWithPhraseInSentence.add(new EventWithPhrase(leftPhrase, middlePhrase, rightPhrase, event.getFilename()));
                        }
                        eventsAfterCR.put(sentNum, eventWithPhraseInSentence);
                    }

                    /*中间结果记录：保存指代消解之后的事件*/
                    StringBuilder sb_events_phrase = new StringBuilder();
                    StringBuilder sb_simplify_events_phrase = new StringBuilder();
                    for (Entry<Integer, List<EventWithPhrase>> entry : eventsAfterCR.entrySet()) {
                        sb_events_phrase.append(entry.getKey() + "\t" + CommonUtil.list2String(entry.getValue()) + LINE_SPLITER);
                        sb_simplify_events_phrase.append(entry.getKey() + "\t" + this.getSimpilyEvents(entry.getValue()) + LINE_SPLITER);
                    }
                    FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_CR_EVENTS, file.getName()), CommonUtil.cutLastLineSpliter(sb_events_phrase.toString()), DEFAULT_CHARSET);
                    FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_CR_SIMPLIFY_EVENT, file.getName()), CommonUtil.cutLastLineSpliter(sb_simplify_events_phrase.toString()), DEFAULT_CHARSET);

                } catch (Throwable e) {

                    this.log.error("coreference resolution error", e);
                    throw e;

                }

                /**
                 * 事件修复
                 */
                // TODO 事件修复 2015-10-28 16:58:46

                /**
                 * 短语扩充
                 */
                /*经过指代消解和短语扩充之后的事件*/
                Map<Integer, List<EventWithPhrase>> eventsAfterCRAndPE = new TreeMap<Integer, List<EventWithPhrase>>();
                try {

                    for (Entry<Integer, List<EventWithPhrase>> entry : eventsAfterCR.entrySet()) {
                        Integer sentNum = entry.getKey();
                        List<EventWithPhrase> eventWithPhrases = this.phraseExpansion(entry.getValue(), words.get(sentNum - 1));
                        eventsAfterCRAndPE.put(sentNum, eventWithPhrases);
                    }

                    /*中间结果记录：保存经过指代消解和短语扩充之后的事件*/
                    StringBuilder sb_events_phrase = new StringBuilder();
                    StringBuilder sb_simplify_events_phrase = new StringBuilder();
                    for (Entry<Integer, List<EventWithPhrase>> entry : eventsAfterCR.entrySet()) {
                        sb_events_phrase.append(entry.getKey() + "\t" + CommonUtil.list2String(entry.getValue()) + LINE_SPLITER);
                        sb_simplify_events_phrase.append(entry.getKey() + "\t" + this.getSimpilyEvents(entry.getValue()) + LINE_SPLITER);
                    }
                    FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_CR_PE_EVENTS, file.getName()), CommonUtil.cutLastLineSpliter(sb_events_phrase.toString()), DEFAULT_CHARSET);
                    FileUtils.writeStringToFile(FileUtils.getFile(parentPath + "/" + DIR_CR_PE_SIMPLIFY_EVENT, file.getName()), CommonUtil.cutLastLineSpliter(sb_simplify_events_phrase.toString()), DEFAULT_CHARSET);

                } catch (Throwable e) {

                    this.log.error("exspand word to phrase error!", e);

                }

                // TODO 对事件进行过滤操作 2015-10-27 21:49:27

            } catch (IOException e) {

                this.log.error("文件读或写失败：" + file.getAbsolutePath(), e);

            }
        }
        return success;
    }

    /**
     * 将输入单词转换成相应的指代的词
     *
     * @param inWord
     * @param crChains
     * @param words
     * @return
     */
    private List<Word> changeToCorefWord(Word inWord, Map<String, CoreferenceElement> crChains, List<List<Word>> words) {

        List<Word> phrase = new ArrayList<Word>();

        if(inWord == null || MapUtils.isEmpty(crChains)) {
            return phrase;
        }

        try {

            String key = Encipher.MD5(inWord.getName() + inWord.getSentenceNum() + inWord.getNumInLine() + (inWord.getNumInLine() + 1));
            CoreferenceElement corefElement = crChains.get(key);
            if(corefElement != null) {
                CoreferenceElement ref = corefElement.getRef();
                //System.out.println(key + "\t" + inWord.getName() + "\t" + inWord.getSentenceNum() + "\t" + inWord.getNumInLine() + "\t" + (inWord.getNumInLine() + 1) + "\t->\t" + ref.getElement());
                List<Word> wordsInSent = words.get(ref.getSentNum() - 1);
                for(int i = ref.getStartIndex(); i < ref.getEndIndex(); i++) {
                    phrase.add(wordsInSent.get(i));
                }

            }

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {

            this.log.error("MD5 encode error!", e);

        }

        return phrase;

    }

    /**
     * 利用stanford的nlp处理工具coreNlp对传入的正文进行处理，主要包括：<br>
     * 句子切分；词性标注，命名实体识别，依存分析等
     * key如下：<br>
     * SEGED_TEXT：切分后的句子集合，类型List<String><br>
     * SEGED_TEXT_DETAIL：切分后的句子详细信息，类型List<String><br>
     * WORDS:所有词的对象信息，按行组织，类型：List<List<Word>><br>
     * PARSED_ITEMS：所有词的依存关系集合，按行组织，类型：List<List<ParseItem>><br>
     *
     * @param text 输入文本
     * @return 结果信息全部存在一个map集合中返回，通过key来获取
     *
     */
    public Map<String, Object> coreNlpOperate(final String text) {

        Map<String, Object> coreNlpResults = new HashMap<String, Object>();

        Annotation document = new Annotation(text);
        this.pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        /*按照文章的组织结果，将每个词用对象进行存储*/
        List<List<Word>>  wordsList = new ArrayList<List<Word>>();
        /*存放正文中所有的依存关系*/
        List<List<ParseItem>> parseItemList = new ArrayList<List<ParseItem>>();
        /*用来存放经过按行切分后的正文*/
        StringBuilder textAfterSSeg = new StringBuilder();
        /*仅包含词性的文本*/
        StringBuilder textWithPOS = new StringBuilder();
        /*相对于上面的区别在于每个词都带有详细信息*/
        StringBuilder textAfterSsegDetail = new StringBuilder();

        //获取句子中词的详细信息，并封装成对象
        for(int i = 0; i < sentences.size(); ++i){

            List<Word> words = new ArrayList<Word>();  //存放一行中所有词的对象信息

            //构建一个Root词对象，保证与依存分析中的词顺序统一
            Word root = new Word();
            root.setName("Root");
            root.setLemma("root");
            root.setPos("PUNT");
            root.setNer("O");
            root.setNumInLine(0);
            root.setSentenceNum(i + 1);
            words.add(0, root);

            CoreMap sentence = sentences.get(i);
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

                // 构建词对象
                Word word = new Word();
                word.setName(token.get(TextAnnotation.class));
                word.setLemma(token.get(LemmaAnnotation.class));
                word.setPos(token.get(PartOfSpeechAnnotation.class));
                word.setNer(token.get(NamedEntityTagAnnotation.class));
                word.setSentenceNum((i + 1));
                word.setNumInLine(token.index());
                words.add(word);

            }

            // 缓存每个句子的处理结果
            wordsList.add(words);
            textAfterSSeg.append(sentence.toString() + LINE_SPLITER);
            textAfterSsegDetail.append(this.words2Sentence(words) + LINE_SPLITER);
            textWithPOS.append(this.words2SentenceSimply(words) + LINE_SPLITER);

            // 获取依存依存分析结果，构建依存对象对儿
            SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            List<TypedDependency> typedDependencies = (List<TypedDependency>) dependencies.typedDependencies();
            List<ParseItem> parseItems = new ArrayList<ParseItem>();  //存放一行中的依存信息
            for (TypedDependency typedDependency : typedDependencies) {
                // 依存关系单元
                Word leftWord = words.get(typedDependency.gov().index());
                Word rightWord = words.get(typedDependency.dep().index());
                // 构建依存关系单元
                ParseItem parseItem = new ParseItem();
                parseItem.setDependencyType(typedDependency.reln().getShortName());
                parseItem.setLeftWord(leftWord);
                parseItem.setRightWord(rightWord);
                parseItems.add(parseItem);
            }
            parseItemList.add(parseItems);
        }

        //缓存处理的结果，用于返回
        coreNlpResults.put(SEGED_TEXT, CommonUtil.cutLastLineSpliter(textAfterSSeg.toString()));
        coreNlpResults.put(SEGED_TEXT_DETAIL, CommonUtil.cutLastLineSpliter(textAfterSsegDetail.toString()));
        coreNlpResults.put(SEGED_TEXT_POS, CommonUtil.cutLastLineSpliter(textWithPOS.toString()));
        coreNlpResults.put(WORDS, wordsList);
        coreNlpResults.put(PARSED_ITEMS, parseItemList);

        return coreNlpResults;

    }



    /**
     * 构建依存关系词图，以依存关系作为边
     * @param parseItems
     * @param wordsCount
     * @return
     */
    public String[][] buildWordGraph(List<ParseItem> parseItems, int wordsCount){

        String[][] edges = new String[wordsCount][wordsCount];  // 存放图的边信息
        for (ParseItem parseItem : parseItems)
            edges[parseItem.getLeftWord().getNumInLine()][parseItem.getRightWord().getNumInLine()] = parseItem.getDependencyType();

        return edges;

    }

    /**
     * 事件抽取函数<br>
     * 先构建词图，然后基于词图来进行事件抽取
     *
     * @param parsedList
     * @param words
     * @param filename
     * @return
     */
    public Map<Integer, List<EventWithWord>> extract(List<List<ParseItem>> parsedList, List<List<Word>> words, String filename){

        Map<Integer, List<EventWithWord>> events = new HashMap<Integer, List<EventWithWord>>();

        if(CollectionUtils.isNotEmpty(parsedList) && CollectionUtils.isNotEmpty(words))
            for (int k = 0; k < parsedList.size(); ++k) {
                /*当前处理单位：句子*/

                List<ParseItem> parseItems = parsedList.get(k);
                List<Word> wordsInSentence = words.get(k);
                int wordsCount = wordsInSentence.size();
                // 以依存关系为边构建当前句子的依存关系词图
                String[][] edges = this.buildWordGraph(parseItems, wordsCount);
                // 临时打印
                /*for (String[] edge : edges) {
                    for (String str : edge) {
                        System.out.print(str + "\t");
                    }
                    System.out.println();
                }
                Scanner sc = new Scanner(System.in);
                sc.nextLine();*/
                List<EventWithWord> eventsInSentence = new ArrayList<EventWithWord>();  // 用于存储从当前句子中抽取到的事件
                // 构建事件
                for(int i = 0; i < wordsCount; ++i){
                    /*当前处理单位：词*/

                    List<Integer> agents = new ArrayList<Integer>();
                    List<Integer> objects = new ArrayList<Integer>();
                    Word copWord = null;  // cop关系
                    Word prepWord = null; // 前缀词
                    Word negWord = null;  // 否定词

                    for(int j = 0; j < wordsCount; ++j){

                        if(DEPENDENCY_AGENT.contains(edges[i][j]))
                            // 施事
                            agents.add(j);

                        if(DEPENDENCY_OBJECT.contains(edges[i][j]))
                            // 受事
                            objects.add(j);

                        // 缓存cop关系
                        if("cop".equals(edges[i][j]))
                            copWord = wordsInSentence.get(j);

                        // 缓存prep关系
                        if("prep".equals(edges[i][j]) || "prepc".equals(edges[i][j])){
                            prepWord = wordsInSentence.get(j);
                            if(!POS_PRONOUN.contains(prepWord.getPos()) && "O".equals(prepWord.getNer()))
                                // 如果不是名词或命名实体，则过滤掉
                                prepWord = null;
                        }

                        // 缓存neg关系
                        if("neg".equals(edges[i][j]))
                            negWord = wordsInSentence.get(j);

                    }

                    Word middleWord = wordsInSentence.get(i);

                    if(CollectionUtils.isNotEmpty(agents) && CollectionUtils.isNotEmpty(objects))
                        for (Integer agent : agents) {

                            Word leftWord = wordsInSentence.get(agent);

                            for (Integer object : objects) {

                                Word rightWord = wordsInSentence.get(object);

                                eventsInSentence.add(new EventWithWord(leftWord, negWord, middleWord, rightWord, filename));

                            }

                        }
                    else if(CollectionUtils.isNotEmpty(agents)){
                        /**
                         * 宾语缺失
                         */

                        //从当前词语往后寻找最近的命名实体或名词来作为宾语，效果下降，暂时屏蔽
                        Word subjWord = null;
                        /*for(int n = middleWord.getNumInLine() + 1; n < wordsInSentence.size(); ++n){
                            Word tmpWord = wordsInSentence.get(n);
                            if(POS_NOUN.contains(tmpWord.getPos()) || !"O".equals(tmpWord.getNer())){
                                subjWord = tmpWord;
                                break;
                            }
                        }*/

                        for (Integer agent : agents) {

                            Word leftWord = wordsInSentence.get(agent);

                            if(copWord != null)
                                /**
                                 * 如果存在依存关系cop，则用cop关系将二元事件补全为三元事件
                                 */
                                eventsInSentence.add(new EventWithWord(leftWord,negWord, copWord, middleWord, filename));
                            else
                                /**
                                 * 不存在cop关系的词
                                 */
                                eventsInSentence.add(new EventWithWord(leftWord, negWord, middleWord, subjWord == null ? null : subjWord, filename));
                        }

                    }else if(CollectionUtils.isNotEmpty(objects)) {
                        /**
                         * 主语缺失
                         */

                        //从当前词语往前寻找最近的命名实体或名词来作为主语，效果下降，暂时屏蔽
                        Word objWord = null;
                        /*for(int n = middleWord.getNumInLine() - 1; n > 0; --n){
                            Word tmpWord = wordsInSentence.get(n);
                            if(POS_NOUN.contains(tmpWord.getPos()) || !"O".equals(tmpWord.getNer())){
                                objWord = tmpWord;
                                break;
                            }

                        }*/

                        for (Integer object : objects) {

                            Word rightWord = wordsInSentence.get(object);
                            if(prepWord != null)
                                /**
                                 * 用前缀词做补全主语
                                 */
                                eventsInSentence.add(new EventWithWord(prepWord, negWord, middleWord, rightWord, filename));
                            else
                                /**
                                 * 不存在符合要求的前缀词
                                 */
                                eventsInSentence.add(new EventWithWord(objWord == null ? null : objWord, negWord, middleWord, rightWord, filename));
                        }
                    }
                }
                events.put(k + 1, eventsInSentence);
            }
        return events;
    }

    /**
     * 将事件中的词扩充成短语<br>
     * 按行处理
     *
     * @param events 由词构成的事件
     * @param words 当前句子中的词语
     * @return
     * @throws Exception
     */
    private List<EventWithPhrase> phraseExpansion(List<EventWithPhrase> events, List<Word> words) throws Exception {
        List<EventWithPhrase> eventsInSentence = new ArrayList<EventWithPhrase>();
        try {
            int wordsCount = words.size();
            String[] toks = new String[wordsCount - 1];
            String[] tags = new String[wordsCount - 1];
            for(int i = 1; i < words.size(); i++) {
                toks[i - 1] = words.get(i).getName();
                tags[i - 1] = words.get(i).getPos();
            }
            // 采用open nlp进行chunk
            ChunkerModel chunkerModel = ModelLoader.getChunkerModel();
            ChunkerME chunkerME = new ChunkerME(chunkerModel);
            Span[] spans = chunkerME.chunkAsSpans(toks, tags);

            for (EventWithPhrase event : events) {
                List<Word> leftPhrase = event.getLeftPhrases();
                List<Word> rightPhrase = event.getRightPhrases();

                if(leftPhrase.size() == 1 || rightPhrase.size() == 1) {
                    /**
                     * 只处理单词数为1的主语和宾语
                     */
                    if(leftPhrase.size() == 1) {
                        Word leftWord = leftPhrase.get(0);
                        for (Span span : spans) {
                            if(span.getStart() <= leftWord.getNumInLine()
                                    && leftWord.getNumInLine() <= span.getEnd()
                                    && (span.getEnd() - span.getStart()) > 1) {
                                leftPhrase.clear();
                                for (int i = span.getStart(); i < span.getEnd(); i++) {
                                    leftPhrase.add(words.get(i + 1));
                                }
                                break;
                            }
                        }
                    }
                    if(rightPhrase.size() == 1) {
                        Word rightWord = rightPhrase.get(0);
                        for (Span span : spans) {
                            if(span.getStart() <= rightWord.getNumInLine()
                                    && rightWord.getNumInLine() <= span.getEnd()
                                    && (span.getEnd() - span.getStart()) > 1) {
                                rightPhrase.clear();
                                for (int i = span.getStart(); i < span.getEnd(); i++) {
                                    rightPhrase.add(words.get(i + 1));
                                }
                                break;
                            }
                        }
                    }

                    eventsInSentence.add(new EventWithPhrase(leftPhrase, event.getRightPhrases(), rightPhrase, event.getFilename()));

                } else {

                    eventsInSentence.add(event);

                }

            }

        } catch (IOException e) {
            this.log.error("Load chunk model error!", e);
            throw new Exception(e);
        }
        return eventsInSentence;
    }

    /**
     * 事件完善
     *
     * @param eventWithPhrases
     * @param words
     * @return
     */
    private List<EventWithPhrase> eventRepair(List<EventWithPhrase> eventWithPhrases, List<Word> words) {

        List<EventWithPhrase> result = new ArrayList<EventWithPhrase>();

        /**
         * 1.如果两个事件，一个是三元事件，一个是二元事件，<br>
         *   如果三元事件的宾语是二元事件的谓语，同时该词不是名词和命名实体，<br>
         *   则将这两个事件合为一个事件
         */

        for(int i = 0; i < eventWithPhrases.size(); i++) {
            for(int j = 0; j < eventWithPhrases.size(); j++) {

            }
        }

        /**
         * 2.如果一个事件的主语或者谓语是量词，<br>
         *   则将该词替换成向前向后距离最近（不超过标点范围）的名词或命名实体
         */

        /**
         * 3.如果一个事件缺失主语或者宾语，<br>
         *   则向前向后找最近（不超过标点范围）的名词或命名实体进行补全
         */

        return result;
    }

    /**
     * 事件过滤函数，对于不符合要求的事件，返回null
     * @param event_in
     * @return
     */
    private EventWithWord eventFilter(EventWithWord event_in){
        EventWithWord event = null;
        if(event_in != null){
            /*
             * 对事件进行过滤，过滤规则：
             * 对于三元组事件：
             * 1.如果谓词不是英文单词，则返回null
             * 2.对于三元组事件，如果主语或者宾语有一个不为单词，则将其替换为二元组事件，如果满足二元组事件要求，就将得到的二元组事件返回，否则返回null
             * 对于二元组事件：
             * 1.如果谓词不是单词，则返回null
             * 2.如果主语或者宾语不是单词，则返回null
             */
            final Pattern pattern_include = Pattern.compile("[a-zA-Z0-9$]+");  //必须包含的项
            final Pattern pattern_exclude = Pattern.compile("[&']");  //不能包含的字符
            if(event_in.getMiddleWord() == null
                    || !pattern_include.matcher(event_in.getMiddleWord().getLemma()).find()
                    || pattern_exclude.matcher(event_in.getMiddleWord().getLemma()).find())
                //谓语不是单词
                event = null;
            else if(event_in.getLeftWord() != null
                    && event_in.getRightWord() != null){
                //当前为三元组事件
                if(pattern_include.matcher(event_in.getLeftWord().getLemma()).find()
                        && pattern_include.matcher(event_in.getRightWord().getLemma()).find()
                        && !pattern_exclude.matcher(event_in.getLeftWord().getLemma()).find()
                        && !pattern_exclude.matcher(event_in.getRightWord().getLemma()).find())
                    event = event_in;
                else if(pattern_include.matcher(event_in.getLeftWord().getLemma()).find()
                        && !pattern_exclude.matcher(event_in.getLeftWord().getLemma()).find())
                    //将当前三元事件降级为二元事件
                    event = new EventWithWord(event_in.getLeftWord(), event_in.getMiddleWord(), null, event_in.getFilename());
                else if(pattern_include.matcher(event_in.getRightWord().getLemma()).find()
                        && !pattern_exclude.matcher(event_in.getRightWord().getLemma()).find())
                    event = new EventWithWord(null, event_in.getMiddleWord(), event_in.getRightWord(), event_in.getFilename());
                else
                    event = null;
            } else //当前为二元组事件
                if(event_in.getLeftWord() != null
                && pattern_include.matcher(event_in.getLeftWord().getLemma()).find()
                && !pattern_exclude.matcher(event_in.getLeftWord().getLemma()).find())
                    event = event_in;
                else if(event_in.getRightWord() != null
                        && pattern_include.matcher(event_in.getRightWord().getLemma()).find()
                        && !pattern_exclude.matcher(event_in.getRightWord().getLemma()).find())
                    event = event_in;
                else
                    event = null;
        }

        /*
         * 2015年6月7日19:43:22新添加过滤规则
         * 三元事件
         * 1.如果主语、宾语中包含代词，则去除代词，降级为二元事件
         * 2.如果谓词为代词，则直接过滤
         * 3.如果主语和宾语相同，则直接过滤
         * 4.如果主语或宾语为be动词，则直接过滤
         * 二元事件
         * 1.如果包含be动词，直接过滤
         * 2.如果包含代词，则直接过滤
         * 通用规则
         * 1.将$全部改为money
         */
        if(event != null){
            if(event.eventType() == 3)
                //表示当前为三元事件
                if(event.getLeftWord().getName().equalsIgnoreCase(event.getRightWord().getName())
                        || "be".equalsIgnoreCase(event.getLeftWord().getLemma())
                        || "be".equalsIgnoreCase(event.getRightWord().getLemma())
                        || POS_PRONOUN.contains(event.getMiddleWord().getPos()))
                    //主语与宾语相同，或者其中一个为be动词，或谓词为代词，直接过滤
                    event = null;
                else {
                    if(POS_PRONOUN.contains(event.getLeftWord().getPos()))
                        //主语为代词，降级为二元事件
                        event.setLeftWord(null);
                    if(POS_PRONOUN.contains(event.getRightWord().getPos()))
                        //宾语为代词，降级为二元事件
                        event.setRightWord(null);
                }
            if(event != null && event.eventType() == 2)
                //表示当前为主谓事件
                if("be".equalsIgnoreCase(event.getLeftWord().getLemma())
                        || "be".equalsIgnoreCase(event.getMiddleWord().getLemma()))
                    //主语或谓语包含be动词，直接过滤
                    event = null;
                else if(POS_PRONOUN.contains(event.getLeftWord().getPos())
                        || POS_PRONOUN.contains(event.getMiddleWord().getPos()))
                    //主语或谓语包含代词，直接过滤
                    event = null;
            if(event != null && event.eventType() == 1)
                //表示当前为谓宾事件
                if("be".equalsIgnoreCase(event.getRightWord().getLemma())
                        || "be".equalsIgnoreCase(event.getMiddleWord().getLemma()))
                    //谓语或宾语包含be动词，直接过滤
                    event = null;
                else if(POS_PRONOUN.contains(event.getRightWord().getPos())
                        || POS_PRONOUN.contains(event.getMiddleWord().getPos()))
                    //谓语或宾语包含代词，直接过滤
                    event = null;
            if(event != null && event.eventType() == -1)
                //对于经过操作之后不能称为事件的事件进行过滤
                event = null;
            if(event != null) {
                //将美元符号全部替换成单词money
                if(event.getLeftWord() != null
                        && "$".equals(event.getLeftWord().getName())) {
                    event.getLeftWord().setName("money");
                    event.getLeftWord().setLemma("money");
                }
                if(event.getMiddleWord() != null
                        && "$".equals(event.getMiddleWord().getName())) {
                    event.getMiddleWord().setName("money");
                    event.getMiddleWord().setLemma("money");
                }
                if(event.getRightWord() != null
                        && "$".equals(event.getRightWord().getName())) {
                    event.getRightWord().setName("money");
                    event.getRightWord().setLemma("money");
                }
            }
        }

        return event;
    }

    /**
     * 将词语集合转换成句子，
     * @param words
     * @return
     */
    private String words2Sentence(List<Word> words){
        String sentenceDetail = null;
        final StringBuilder tmp = new StringBuilder();
        for (final Word word : words)
            tmp.append(word.toString() + " ");
        sentenceDetail = tmp.toString().trim();
        return sentenceDetail;
    }

    /**
     * 将词语集合转换成句子，
     * @param words
     * @return
     */
    private String words2SentenceSimply(List<Word> words){
        String sentenceDetail = null;
        final StringBuilder tmp = new StringBuilder();
        for (final Word word : words)
            tmp.append(word.wordWithPOS() + " ");
        sentenceDetail = tmp.toString().trim();
        return sentenceDetail;
    }

    /**
     * 将一个事件中的人称指代，替换成对应的人名
     * 策略：找当前词所在行前面最近的人名
     * @param words
     * @param word
     * @return
     */
    private Word personPronoun2Name(List<Word> words, Word word){
        Word pronoun = word;
        if(word != null && POS_PERSON_PRONOUN.contains(word.getPos()))
            for(int i = word.getNumInLine() - 1; i > 0; --i){
                final Word curr = words.get(i);
                if("person".equalsIgnoreCase(curr.getNer())){
                    pronoun = curr;
                    break;
                }
            }
        return pronoun;
    }

    /**
     * 将事件以精简的形式转化成字符串
     *
     * @param events
     * @return
     */
    private String getSimpilyEvents(List< ? extends Event> events){

        StringBuilder sb = new StringBuilder();

        for (Event event : events) {

            sb.append(event.toShortString() + " ");

        }

        return sb.toString().trim();

    }

    /**
     * 事件抽取测试
     * @param args
     */
    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }

        });

        ExecutorService es = Executors.newFixedThreadPool(4);
        Future<Boolean> future = es.submit(new EventsExtractBasedOnGraphV2("E:/workspace/optimization/singleText"));
        es.shutdown();
    }

}
