package edu.whu.cs.nlp.mts.domain;

import edu.whu.cs.nlp.mts.sys.SystemConstant;

/**
 * 原子事件
 *
 * @author Apache_xiaochao
 *
 */
public class Event implements SystemConstant {

    private Word   leftWord;   // 使动词
    private Word   negWord;    // 否定词
    private Word   middleWord; // 连接词
    private Word   rightWord;  // 被动词
    private String filename;   // 事件所属的文件名称

    public Event(Word leftWord, Word negWord, Word middleWord, Word rightWord, String filename) {
        super();
        this.leftWord = leftWord;
        this.negWord = negWord;
        this.middleWord = middleWord;
        this.rightWord = rightWord;
        this.filename = filename;
    }

    /**
     * 事件类型：3表示三元组事件，2表示主-谓事件，1表示谓-宾事件，-1表示异常事件
     *
     * @return
     */
    public int eventType() {
        if (this.leftWord != null && this.middleWord != null && this.rightWord != null) {
            return 3;
        } else if (this.leftWord != null && this.middleWord != null) {
            return 2;
        } else if (this.middleWord != null && this.rightWord != null) {
            return 1;
        }
        return -1;
    }

    public Word getLeftWord() {
        return this.leftWord;
    }

    public void setLeftWord(Word leftWord) {
        this.leftWord = leftWord;
    }

    public Word getNegWord() {
        return this.negWord;
    }

    public void setNegWord(Word negWord) {
        this.negWord = negWord;
    }

    public Word getMiddleWord() {
        return this.middleWord;
    }

    public void setMiddleWord(Word middleWord) {
        this.middleWord = middleWord;
    }

    public Word getRightWord() {
        return this.rightWord;
    }

    public void setRightWord(Word rightWord) {
        this.rightWord = rightWord;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * 返回事件的简要形式
     *
     * @return
     */
    public String toShortString() {
        return (this.leftWord == null ? "" : this.leftWord.getLemma()) + WORD_CONNECTOR
                + (this.negWord == null ? "" : (this.negWord.getLemma() + " "))
                + (this.middleWord == null ? "" : this.middleWord.getLemma()) + WORD_CONNECTOR
                + (this.rightWord == null ? "" : this.rightWord.getLemma());
    }

    /**
     * 返回事件的详细形式
     */
    @Override
    public String toString() {
        return (this.leftWord == null ? "" : this.leftWord) + WORD_CONNECTOR
                + (this.middleWord == null ? "" : (this.middleWord + " "))
                + (this.middleWord == null ? "" : this.middleWord)
                + WORD_CONNECTOR + (this.rightWord == null ? "" : this.rightWord)
                + FILENAME_REST_LEFT + this.filename + FILENAME_REST_RIGHT;
    }

}
