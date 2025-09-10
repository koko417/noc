package tanabu.noc.controller;

import tanabu.noc.Main;
import tanabu.noc.event.listener.EventListener;
import tanabu.noc.model.BaseInfo;
import tanabu.noc.model.User;

public class UserListener implements EventListener<User> {

	@Override
	public void onEvent(User info) {
		Main.getDb().saveToDb(info);
		
	}

}
