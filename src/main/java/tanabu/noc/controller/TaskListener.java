package tanabu.noc.controller;

import tanabu.noc.Main;
import tanabu.noc.event.listener.EventListener;
import tanabu.noc.model.BaseInfo;
import tanabu.noc.model.Task;

public class TaskListener implements EventListener<Task> {

	@Override
	public void onEvent(Task info) {
		Main.getDb().saveToDb(info);
		Main.notifier(Main.TASK, info.id);
		
	}

}
