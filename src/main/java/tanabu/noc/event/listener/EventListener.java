package tanabu.noc.event.listener;

import tanabu.noc.model.BaseInfo;

public interface EventListener <T extends BaseInfo> {
	void onEvent(T info);
}