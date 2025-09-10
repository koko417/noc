package tanabu.noc.event;

import java.util.*;

import tanabu.noc.event.listener.EventListener;
import tanabu.noc.model.BaseInfo;

public class EventBus <T extends BaseInfo> {
	
	private final Map<Class<T>, List<EventListener<T>>> listeners = new HashMap<>();
	
	public void register(Class<T> type, EventListener<T> listener) {
		listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
		
	}
	
    public void callEvent(T model) {
        List<EventListener<T>> ls = listeners.get(model.getClass());
        if (ls != null) {
            for (EventListener<T> l : ls) {
            	l.onEvent(model);
            }
        }
    }
}
