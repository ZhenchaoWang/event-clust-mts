package edu.whu.cs.nlp.mts.utils;

import java.util.ArrayList;
import java.util.List;

import edu.whu.cs.nlp.mts.domain.Word;
import edu.whu.cs.nlp.mts.sys.SystemConstant;

/**
 * 公共工具
 * @author Apache_xiaochao
 *
 */
public class CommonUtil implements SystemConstant{

    /**
     * 去除文本最后的面的换行符
     * @param text
     * @return
     */
    public static String cutLastLineSpliter(String text){
        String value = text;
        if(text != null){
            final int index = text.lastIndexOf(LINE_SPLITER);
            if(index > 0){
                value = text.substring(0, index);
            }
        }
        return value;
    }

    /**
     * 将多层List转化成字符串
     * @param <T>
     * @param lists
     * @return
     */
    public static <T> String lists2String(List<List<T>> lists){
        String result = null;
        final StringBuilder sb_out = new StringBuilder();
        for (final List<T> list : lists) {
            final StringBuilder sb_in = new StringBuilder();
            for (final T t : list) {
                sb_in.append(t.toString() + " ");
            }
            sb_out.append(sb_in.toString().trim() + LINE_SPLITER);
        }
        result = cutLastLineSpliter(sb_out.toString());
        return result;
    }

    /**
     * 将List转化成字符串
     * @param <T>
     * @param lists
     * @return
     */
    public static <T> String list2String(List<T> list){
        String result = null;
        final StringBuilder sb = new StringBuilder();
        for (final T t : list) {
            sb.append(t.toString() + " ");
        }
        result = sb.toString().trim();
        return result;
    }

    /**
     * 将输入的字符封装成对象
     * @param input
     * @param pattern
     * @return
     */
    public static Word str2Word(String str){
        Word word = null;
        if(str != null && !"".equals(str.trim())){
            final String[] attrs = str.split(WORD_ATTRBUTE_CONNECTOR);
            word = new Word();
            word.setName(attrs[0]);
            word.setLemma(attrs[1]);
            word.setPos(attrs[2]);
            word.setNer(attrs[3]);
            word.setSentenceNum(Integer.parseInt(attrs[4]));
            word.setNumInLine(Integer.parseInt(attrs[5]));
        }
        return word;
    }

    /**
     * 对输入文本按行切分，然后存入List集合
     * @param input
     * @return
     */
    public static List<String> str2List(String input){
        List<String> list = null;
        if(input != null){
            list = new ArrayList<String>();
            final String[] lines = input.split(LINE_SPLITER);
            for (final String line : lines) {
                list.add(line);
            }
        }
        return list;
    }

}
