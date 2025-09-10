package tanabu.noc.model;

import tanabu.noc.model.BaseInfo.*;
import com.fasterxml.jackson.databind.JsonNode;

@Table(name = "task")
public class Task extends Comment {
    @Column(name = "maxPoints")
    int maxPoints;
    @Column(name = "graded")
    boolean graded;
    @Column(name = "driveId")
    String driveId;
    @Column(name = "driveUrl")
    String driveUrl;
    @Column(name = "statusCode")
    int statusCode;
    @Column(name = "subStatus")
    int subStatus;

    public Task(JsonNode root) {
        super(root);

        JsonNode taskBlock = root.get(1);
        if (taskBlock != null && taskBlock.isArray()) {
            this.maxPoints = safeInt(taskBlock, 3);
            this.graded = taskBlock.get(4) != null && taskBlock.get(4).asBoolean(false);
        }

        JsonNode driveBlock = root.get(2);
        if (driveBlock != null && driveBlock.isArray()) {
            this.driveId = safeText(driveBlock, 0);
            this.driveUrl = safeText(driveBlock, 2);
        }

        JsonNode statusBlock = root.get(4);
        if (statusBlock != null && statusBlock.isArray() && statusBlock.size() >= 2) {
            this.statusCode = statusBlock.get(0).asInt();
            this.subStatus = statusBlock.get(1).asInt();
        }
    }
}