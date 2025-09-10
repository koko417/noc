package tanabu.noc.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import tanabu.noc.Main;
import tanabu.noc.model.BaseInfo.Table;

@Table(name = "fileinfo")
public class FileInfo extends BaseInfo {
	private static final int POST_ID_BLOCK = 0;
    private static final int FILE_BLOCK = 7;
	
    @Column(name = "attachmentId")
    String attachmentId;
    @Column(name = "name")
    String name;
    @Column(name = "driveId", primaryKey = true)
    public String driveId;
    @Column(name = "mime")
    String mime;
    @Column(name = "url")
    String url;
    @Column(name = "thumbnailUrl")
    String thumbnailUrl;

    public FileInfo(JsonNode root) {
        this.attachmentId = root.get(0).get(0).asText();

        JsonNode node = root.get(FILE_BLOCK);
    	
    	JsonNode f1 = node.get(0);
    	this.attachmentId = root.get(0).get(0).asText();
        this.name = f1.get(0).asText();
        this.driveId = f1.get(2).asText();
        this.mime = f1.get(4).asText();
        this.url = f1.get(6).asText();
        this.thumbnailUrl = f1.get(5).asText();
    	if (node.size() == 1) {
    		return;
    	}
    	ArrayNode arrNode = (ArrayNode) root.get(FILE_BLOCK);
        arrNode.remove(0);
    	Main.getDb().saveToDb(new FileInfo(root));
    	
    }
}