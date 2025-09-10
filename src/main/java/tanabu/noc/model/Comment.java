package tanabu.noc.model;

import com.fasterxml.jackson.databind.JsonNode;
import tanabu.noc.model.BaseInfo.*;

@Table(name = "comment")
public class Comment extends BaseInfo {
    public static final int POST_ID_BLOCK = 0;
    public static final int CREATED_TIMESTAMP = 1;
    public static final int UPDATED_TIMESTAMP = 2;
    public static final int SENT_TIMESTAMP = 3;
    public static final int RELATED_ID = 4;
    public static final int TITLE = 5;
    public static final int TYPE = 8;
    public static final int COMMENT_META = 9;
    public static final int COMPOSITE_ID = 13;
    public static final int STATUS = 15;
    public static final int CONTENT_BLOCK = 22;
	
    @Column(name = "id", primaryKey = true)
    public String id;
    @Column(name = "userId")
    String userId;
    @Column(name = "created")
    long created;
    @Column(name = "updated")
    long updated;
    @Column(name = "sent")
    long sent;
    @Column(name = "relatedId")
    String relatedId;
    @Column(name = "title")
    String title;
    @Column(name = "type")
    int type;
    @Column(name = "raw_content")
    String rawContent;
    @Column(name = "html_content")
    String htmlContent;

    public Comment(JsonNode root) {
        JsonNode node = root.get(0);
        JsonNode idBlock = node.get(POST_ID_BLOCK);

        this.id = idBlock.get(0).asText();
        this.userId = idBlock.get(1).get(0).asText();
        this.updated = node.get(UPDATED_TIMESTAMP).asLong();
        this.created = node.get(CREATED_TIMESTAMP).asLong();
        this.sent = node.get(SENT_TIMESTAMP).asLong();
        this.relatedId = safeText(node, RELATED_ID);
        this.title = safeText(node, TITLE);
        this.type = safeInt(node, TYPE);
        this.rawContent = node.get(CONTENT_BLOCK).get(1).asText();
        this.htmlContent = extractContent(node, CONTENT_BLOCK);
    }
}
