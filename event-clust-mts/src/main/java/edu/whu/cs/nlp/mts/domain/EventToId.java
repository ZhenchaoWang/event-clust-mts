package edu.whu.cs.nlp.mts.domain;

/**
 * 封装事件及其对应的编号
 *
 * @author Apache_xiaochao
 *
 */
public class EventToId {

    private EventWithWord   event;
    private Integer num;

    public EventToId() {
        super();
    }

    public EventWithWord getEvent() {
        return this.event;
    }

    public Integer getNum() {
        return this.num;
    }

    public void setEvent(EventWithWord event) {
        this.event = event;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

}
