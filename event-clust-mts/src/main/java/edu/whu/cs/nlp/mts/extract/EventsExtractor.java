package edu.whu.cs.nlp.mts.extract;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

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
import edu.whu.cs.nlp.mts.domain.Event;
import edu.whu.cs.nlp.mts.domain.ParseItem;
import edu.whu.cs.nlp.mts.domain.Word;
import edu.whu.cs.nlp.mts.pretreat.Pretreatment;
import edu.whu.cs.nlp.mts.sys.CoreNlpObject;
import edu.whu.cs.nlp.mts.sys.SystemConstant;
import edu.whu.cs.nlp.mts.utils.CommonUtil;
import edu.whu.cs.nlp.mts.utils.FileUtil;

/**
 * 事件抽取
 *
 * @author Apache_xiaochao
 *
 */
@Deprecated
public class EventsExtractor implements SystemConstant, Callable<Boolean>{

    private final Logger log = Logger.getLogger(this.getClass());
    private final StanfordCoreNLP pipeline;
    private final String textDir;  //输入文件所在目录

    public EventsExtractor(String textDir) {
        super();
        this.textDir = textDir;
        this.pipeline = CoreNlpObject.getPipeLine();
    }

    /**
     * 将词语集合转换成句子，
     * @param words
     * @return
     */
    private String words2Sentence(List<Word> words){
        String sentenceDetail = null;
        final StringBuilder tmp = new StringBuilder();
        for (final Word word : words) {
            tmp.append(word.toString() + " ");
        }
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
        for (final Word word : words) {
            tmp.append(word.wordWithPOS() + " ");
        }
        sentenceDetail = tmp.toString().trim();
        return sentenceDetail;
    }

    /**
     * 利用stanford的nlp处理工具coreNlp对传入的正文进行处理，主要包括：
     * 句子切分；词性标注，命名实体识别，依存分析等	 *
     * @param text
     * @return 结果信息全部存在一个map集合中返回，通过key来获取
     * key如下：
     * segedText：切分后的句子集合，类型List<String>
     * segedTextDetail：切分后的句子详细信息，类型List<String>
     * words:所有词的对象信息，按行组织，类型：List<List<Word>>
     * parseItems：所有词的依存关系集合，按行组织，类型：List<List<ParseItem>>
     */
    public Map<String, Object> coreNlpOperate(String text) {
        final Map<String, Object> coreNlpResults = new HashMap<String, Object>();
        final Annotation document = new Annotation(text);
        this.pipeline.annotate(document);
        final List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        final List<List<Word>>  wordsList = new ArrayList<List<Word>>();  //按照文章的组织结果，将每个词用对象进行存储
        final List<List<ParseItem>> parseItemList = new ArrayList<List<ParseItem>>();  //存放正文中所有的依存关系
        final StringBuilder textAfterSSeg = new StringBuilder();  //用来存放经过按行切分后的正文
        final StringBuilder textWithPOS = new StringBuilder();  //仅包含词性的文本
        final StringBuilder textAfterSsegDetail = new StringBuilder();  //相对于上面的区别在于每个词都带有详细信息
        //获取句子中词的详细信息，并封装成对象
        for(int i = 0; i < sentences.size(); ++i){
            final List<Word> words = new ArrayList<Word>();  //存放一行中所有词的对象信息
            //构建一个Root词对象，保证与依存分析中的词顺序统一
            final Word root = new Word();
            root.setName("Root");
            root.setLemma("root");
            root.setPos("PUNT");
            root.setNer("O");
            root.setNumInLine(0);
            root.setSentenceNum(i + 1);
            words.add(0, root);

            final CoreMap sentence = sentences.get(i);
            for (final CoreLabel token : sentence.get(TokensAnnotation.class)) {
                final String name = token.get(TextAnnotation.class);
                final String lemma = token.get(LemmaAnnotation.class);
                final String pos = token.get(PartOfSpeechAnnotation.class);
                final String ne = token.get(NamedEntityTagAnnotation.class);
                final Word word = new Word();
                word.setName(name);
                word.setLemma(lemma);
                word.setPos(pos);
                word.setNer(ne);
                word.setSentenceNum((i + 1));
                word.setNumInLine(token.index());
                words.add(word);
            }
            wordsList.add(words);
            textAfterSSeg.append(sentence.toString() + LINE_SPLITER);
            textAfterSsegDetail.append(words2Sentence(words) + LINE_SPLITER);
            textWithPOS.append(words2SentenceSimply(words) + LINE_SPLITER);

            //获取依存依存分析结果
            //Tree tree = sentence.get(TreeAnnotation.class);
            final SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            final List<TypedDependency> typedDependencies = (List<TypedDependency>) dependencies.typedDependencies();
            final List<ParseItem> parseItems = new ArrayList<ParseItem>();  //存放一行中的依存信息
            for (final TypedDependency typedDependency : typedDependencies) {
                //依存关系单元
                //String type = typedDependency.reln().getShortName();
                final Word leftWord = words.get(typedDependency.gov().index());
                final Word rightWord = words.get(typedDependency.dep().index());
                /*if("nsubj".equals(type) || "dobj".equals(type)){
		    		log.info(type + "(" + typedDependency.gov().value() + "-" + typedDependency.gov().index() + ", " + typedDependency.dep().value() + "-" + typedDependency.dep().index() + ")\t>>\t(" + leftWord + ", " + rightWord + ")");
		    	}*/
                // 构建依存关系单元
                final ParseItem parseItem = new ParseItem();
                parseItem.setDependencyType(typedDependency.reln().getShortName());
                parseItem.setLeftWord(leftWord);
                parseItem.setRightWord(rightWord);
                parseItems.add(parseItem);
            }
            parseItemList.add(parseItems);
        }

        //缓存处理的结果，用于返回
        coreNlpResults.put("segedText",
                CommonUtil.cutLastLineSpliter(textAfterSSeg.toString()));
        coreNlpResults.put("segedTextDetail",
                CommonUtil.cutLastLineSpliter(textAfterSsegDetail.toString()));
        coreNlpResults.put("segedTextPOS",
                CommonUtil.cutLastLineSpliter(textWithPOS.toString()));
        coreNlpResults.put("words", wordsList);
        coreNlpResults.put("parseItems", parseItemList);

        return coreNlpResults;
    }

    /**
     * 事件过滤函数，对于不符合要求的事件，返回null
     * 暂时不在抽取时进行过滤，放在读取时进行过滤
     * @param event_in
     * @return
     */
    private Event eventFilter(Event event_in){
        Event event = null;
        if(event_in != null){
            /*
             * 对事件进行过滤，过滤规则：
             * 对于三元组事件：
             * 1.如果谓词不是英文单词，则返回null
             * 2.对于三元组事件，如果主语或者宾语有一个不为单词，则将其替换为二元组事件，如果满足二元组事件要求，就将得到的二元组事件返回，都则返回null
             * 对于二元组事件：
             * 1.如果谓词不是单词，则返回null
             * 2.如果主语或者宾语不是单词，则返回null
             */
            final Pattern pattern_include = Pattern.compile("[a-zA-Z0-9$]+");  //必须包含的项
            final Pattern pattern_exclude = Pattern.compile("[&]");  //不能包含的字符
            if(event_in.getMiddleWord() == null
                    || !pattern_include.matcher(event_in.getMiddleWord().getLemma()).find()
                    || pattern_exclude.matcher(event_in.getMiddleWord().getLemma()).find()){
                //谓语不是单词
                event = null;
            }else{
                if(event_in.getLeftWord() != null
                        && event_in.getRightWord() != null){
                    //当前为三元组事件
                    if(pattern_include.matcher(event_in.getLeftWord().getLemma()).find()
                            && pattern_include.matcher(event_in.getRightWord().getLemma()).find()
                            && !pattern_exclude.matcher(event_in.getLeftWord().getLemma()).find()
                            && !pattern_exclude.matcher(event_in.getRightWord().getLemma()).find()){
                        event = event_in;
                    } else{
                        if(pattern_include.matcher(event_in.getLeftWord().getLemma()).find()
                                && !pattern_exclude.matcher(event_in.getLeftWord().getLemma()).find()){
                            //将当前三元事件降级为二元事件
                            event = new Event(event_in.getLeftWord(), event_in.getMiddleWord(), null, event_in.getFilename());
                        } else if(pattern_include.matcher(event_in.getRightWord().getLemma()).find()
                                && !pattern_exclude.matcher(event_in.getRightWord().getLemma()).find()){
                            event = new Event(null, event_in.getMiddleWord(), event_in.getRightWord(), event_in.getFilename());
                        } else {
                            event = null;
                        }
                    }
                } else {
                    //当前为二元组事件
                    if(event_in.getLeftWord() != null
                            && pattern_include.matcher(event_in.getLeftWord().getLemma()).find()
                            && !pattern_exclude.matcher(event_in.getLeftWord().getLemma()).find()){
                        event = event_in;
                    }
                    else if(event_in.getRightWord() != null
                            && pattern_include.matcher(event_in.getRightWord().getLemma()).find()
                            && !pattern_exclude.matcher(event_in.getRightWord().getLemma()).find()){
                        event = event_in;
                    } else {
                        event = null;
                    }
                }
            }
        }
        return event;
    }

    /**
     * 判断当前词是不是名词或命名实体
     * @param word
     * @return
     */
    private boolean isNounOrNE(Word word){
        if(word != null){
            if(!"O".equalsIgnoreCase(word.getNer())){
                return true;
            }else if(word.getPos().contains("NN")){
                return true;
            }
        }
        return false;
    }

    /**
     * 将一个事件中的人称指代，替换成对应的人名
     * 策略：找当前词所在行前面最近的人名
     * @param words
     * @param word
     * @return
     */
    private Word personPronoun2Name(List<List<Word>> words, Word word){
        Word pronoun = word;
        if(word != null && POS_PERSON_PRONOUN.contains(word.getPos())){
            final List<Word> sentence = words.get(word.getSentenceNum() - 1);
            for(int i = word.getNumInLine() - 1; i >= 0; --i){
                final Word curr = sentence.get(i);
                if("person".equalsIgnoreCase(curr.getNer())){
                    pronoun = curr;
                    break;
                }
            }
        }
        return pronoun;
    }

    /**
     * 事件抽取函数
     * @param parsedList
     * @param words
     * @param filename
     * @return
     */
    public Map<Integer, List<Event>> extract(
            List<List<ParseItem>> parsedList, List<List<Word>> words, String filename){
        Map<Integer, List<Event>> events = null;
        if(parsedList != null && words != null){
            events = new LinkedHashMap<>();
            List<Event> eventsInLine = null;
            Set<Integer> match_dobj = null;
            int sentenceNum = 0;  //行号
            for (final List<ParseItem> items : parsedList) {
                //每一句话的item集合
                eventsInLine = new ArrayList<Event>();  //一句话中的原子事件集合
                match_dobj = new HashSet<Integer>();  //记录有匹配的dobj
                for (final ParseItem item_1 : items) {
                    //每一个item
                    if("nsubj".equals(item_1.getDependencyType())){
                        boolean matching = false;  //用于判断是否存在dobj与当前nsubj进行匹配
                        for (final ParseItem item_2 : items) {
                            if("dobj".equals(item_2.getDependencyType())
                                    && item_1.getLeftWord().getName().equals(item_2.getLeftWord().getName())
                                    && item_1.getLeftWord().getNumInLine() == item_2.getLeftWord().getNumInLine()){
                                //对事件中的词去人称指代
                                final Word left = personPronoun2Name(words, item_1.getRightWord());
                                final Word right = personPronoun2Name(words, item_2.getRightWord());
                                Event event = new Event(left, item_1.getLeftWord(), right, filename);
                                event = eventFilter(event);
                                if(event != null){
                                    //System.out.println("主-谓-宾：" + event.toString());
                                    eventsInLine.add(event);
                                }
                                match_dobj.add(item_2.hashCode());  //记录当前已经有匹配的dobj
                                matching = true;
                            }
                        }

                        //论元缺失，缺少宾语
                        if(!matching){
                            boolean replenish = false;  //是否采用命名实体进行论元缺失补充
                            //采用当前item后面最短距离的命名实体或名词作为宾语，同时不能与当前词相同
                            final int sentNum = item_1.getRightWord().getSentenceNum() - 1;
                            final List<Word> sentence = words.get(sentNum);
                            for(int i = item_1.getLeftWord().getNumInLine() + 1; i < sentence.size(); ++i){
                                final Word word = sentence.get(i);
                                if(isNounOrNE(word)
                                        && !word.getLemma().equalsIgnoreCase(item_1.getRightWord().getLemma())
                                        && !word.getLemma().equalsIgnoreCase(item_1.getLeftWord().getLemma())){
                                    final Word left = personPronoun2Name(words, item_1.getRightWord());
                                    final Word right = word;
                                    Event event = new Event(left, item_1.getLeftWord(), right, filename);
                                    event = eventFilter(event);
                                    if(event != null){
                                        //System.out.println("主-谓-：" + event.toString());
                                        eventsInLine.add(event);
                                    }
                                    replenish = true;
                                    break;
                                }
                            }

                            //如果没有采用命名实体进行论元补充，则采用二元组作为事件
                            if(!replenish){
                                Event event = new Event(
                                        personPronoun2Name(words, item_1.getRightWord()),
                                        item_1.getLeftWord(), null, filename);
                                event = eventFilter(event);
                                if(event != null){
                                    eventsInLine.add(event);
                                }
                            }

                        }
                    }
                }

                //对于论元缺失dobj进行处理
                for (final ParseItem item : items) {
                    if(!match_dobj.contains(item.hashCode()) && "dobj".equals(item.getDependencyType())){
                        boolean replenish = false;  //是否采用命名实体进行论元缺失补充
                        //如果当前命名实体集合不为空，则采用当前item后面最短距离的命名实体作为宾语
                        final int sentNum = item.getLeftWord().getSentenceNum() - 1;
                        final List<Word> sentence = words.get(sentNum);
                        for(int i = item.getLeftWord().getNumInLine() - 1; i >= 0; --i){
                            final Word word = sentence.get(i);
                            if(isNounOrNE(word)
                                    && !word.getLemma().equalsIgnoreCase(item.getRightWord().getLemma())
                                    && !word.getLemma().equalsIgnoreCase(item.getLeftWord().getLemma())){
                                final Word left = word;
                                final Word right = personPronoun2Name(words, item.getRightWord());
                                Event event = new Event(left, item.getLeftWord(), right, filename);
                                event = eventFilter(event);
                                if(event != null){
                                    //System.out.println("-谓-宾：" + event.toString());
                                    eventsInLine.add(event);
                                }
                                replenish = true;
                                break;
                            }
                        }

                        //如果没有采用命名实体进行论元补充，则采用二元组作为事件
                        if(!replenish){
                            Event event = new Event(
                                    null, item.getLeftWord(),
                                    personPronoun2Name(words, item.getRightWord()), filename);
                            event = eventFilter(event);
                            if(event != null){
                                //System.out.println("二元事件：" + event.toString());
                                eventsInLine.add(event);
                            }
                        }
                    }
                }
                events.put(++sentenceNum, eventsInLine);
            }
        }
        return events;
    }

    @Override
    public Boolean call() {
        final boolean success = true;
        // TODO Auto-generated method stub
        final File file = new File(this.textDir);
        final String[] filenames = file.list();
        final Pretreatment pretreatment = new Pretreatment();
        for (final String filename : filenames) {
            //加载文件
            this.log.info(Thread.currentThread().getId() + "正在操作文件：" + this.textDir + "/" + filename);
            try {
                String text = FileUtil.read(this.textDir + "/" + filename, DEFAULT_CHARSET);
                //对文本进行指代消解
                final Map<String, String> preTreatResult = pretreatment.coreferenceResolution(text);
                text = preTreatResult.get(Pretreatment.KEY_CR_TEXT);
                //利用stanford的nlp核心工具进行处理
                final Map<String, Object> coreNlpResults =  coreNlpOperate(text);
                //获取句子切分后的文本
                final String segedtext = (String) coreNlpResults.get("segedText");
                FileUtil.write(this.textDir + "/" + DIR_CR_TEXT + "/" + filename, segedtext, DEFAULT_CHARSET);

                //获取句子切分后的文本详细信息
                final String segedTextDetail = (String) coreNlpResults.get("segedTextDetail");
                FileUtil.write(this.textDir + "/" + DIR_CR_TEXT_DETAIL + "/" + filename, segedTextDetail, DEFAULT_CHARSET);

                //获取句子切分后的带有词性的文本信息
                final String segedTextPOS = (String) coreNlpResults.get("segedTextPOS");
                FileUtil.write(this.textDir + "/" + DIR_CR_TEXT_DETAIL + "/pos/" + filename, segedTextPOS, DEFAULT_CHARSET);

                //获取对句子中单词进行对象化后的文本
                @SuppressWarnings("unchecked")
                final
                List<List<Word>> words = (List<List<Word>>) coreNlpResults.get("words");

                //获取依存分析结果
                @SuppressWarnings("unchecked")
                final
                List<List<ParseItem>> parseItemList = (List<List<ParseItem>>) coreNlpResults.get("parseItems");
                final String listStr = CommonUtil.lists2String(parseItemList);
                FileUtil.write(this.textDir + "/" + DIR_PARSE + "/" + filename, listStr, DEFAULT_CHARSET);

                //对当前文本进行事件抽取
                final Map<Integer, List<Event>> events = extract(parseItemList, words, filename);
                final StringBuilder sb_events = new StringBuilder();
                final StringBuilder sb_simplify_events = new StringBuilder();
                for (final Entry<Integer, List<Event>> entry : events.entrySet()) {
                    final String eventsInSentence = CommonUtil.list2String(entry.getValue());
                    sb_events.append(entry.getKey() + "\t" + eventsInSentence + LINE_SPLITER);
                    sb_simplify_events.append(entry.getKey() + "\t" + getSimpilyEvents(entry.getValue()) + LINE_SPLITER);
                }
                FileUtil.write(
                        this.textDir + "/" + DIR_EVENTS + "/" + filename,
                        CommonUtil.cutLastLineSpliter(sb_events.toString()), DEFAULT_CHARSET);
                FileUtil.write(
                        this.textDir + "/" + DIR_SIMPLIFY_EVENT + "/" + filename,
                        CommonUtil.cutLastLineSpliter(sb_simplify_events.toString()), DEFAULT_CHARSET);

            } catch (final IOException e) {
                this.log.error("文件读或写失败：" + this.textDir + "/" + filename, e);
                //e.printStackTrace();
            }
        }
        return success;
    }

    /**
     * 将事件以精简的形式转化成字符串
     * @param events
     * @return
     */
    private String getSimpilyEvents(List<Event> events){
        String result = null;
        final StringBuilder sb = new StringBuilder();
        for (final Event event : events) {
            sb.append(event.toShortString() + " ");
        }
        result = sb.toString().trim();
        return result;
    }

    /**
     * 事件抽取测试
     * @param args
     */
    public static void main(String[] args) {
        final ExecutorService es = Executors.newFixedThreadPool(1);
        es.submit(new EventsExtractor("E:/mts_dir_singleoptimization/D0736H"));
        es.shutdown();
    }

}
