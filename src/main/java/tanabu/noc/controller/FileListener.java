package tanabu.noc.controller;

import tanabu.noc.Main;
import tanabu.noc.event.listener.EventListener;
import tanabu.noc.model.BaseInfo;
import tanabu.noc.model.FileInfo;

public class FileListener implements EventListener<FileInfo> {

	@Override
	public void onEvent(FileInfo info) {
		Main.getDb().saveToDb(info);
		
	}

}
