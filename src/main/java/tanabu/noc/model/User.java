package tanabu.noc.model;

import com.fasterxml.jackson.databind.JsonNode;

import tanabu.noc.model.BaseInfo.Table;

@Table(name = "user")
public class User extends BaseInfo {
    @Column(name = "userId", primaryKey = true)
    String id;
    @Column(name = "name")
    String name;
    @Column(name = "photoUrl")
    String photoUrl;

    public User(JsonNode node) {
        this.id = node.get(0).get(0).asText();
        this.name = node.get(1).asText();

        String url = node.get(4).asText();
        if (url != null && !url.isEmpty()) {
            this.photoUrl = url.startsWith("//") ? "https:" + url : url;
        }
    }
}