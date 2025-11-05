package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.CompileTimeGenerate;
import top.dreamlike.panama.generator.annotation.Skip;

@CompileTimeGenerate
public class EpollEvent {
    private int events;
    private EpollEventData eventData;

    @Skip
    private long skip;

    public int getEvents() {
        return events;
    }

    public void setEvents(int events) {
        this.events = events;
    }

    public EpollEventData getEventData() {
        return eventData;
    }

    public void setEventData(EpollEventData eventData) {
        this.eventData = eventData;
    }
}
