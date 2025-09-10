package tanabu.noc.controller;

import tanabu.noc.Main;
import tanabu.noc.event.listener.EventListener;
import tanabu.noc.model.BaseInfo;
import tanabu.noc.model.Comment;

public class CommentListener implements EventListener<Comment> {

	@Override
	public void onEvent(Comment info) {
		Main.getDb().saveToDb(info);
		Main.notifier(Main.COMMENT, info.id);
		
	}

}
