package edu.whu.cs.nlp.mts.domain;

/**
 * 封装事件及其对应的编号
 *
 * @author Apache_xiaochao
 *
 */
public class EventToId {

    private Event   event;
    private Integer num;

    public EventToId() {
        super();
    }

    public Event getEvent() {
        return this.event;
    }

    public Integer getNum() {
        return this.num;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

}
