package com.reeman.phone.mode;

import java.util.List;

public class CurrentTaskMode {
    private String id;
    private List<String> content;
    private List<String> details;

    public CurrentTaskMode(String id, List<String> content, List<String> details) {
        this.id = id;
        this.content = content;
        this.details = details;
    }

    public CurrentTaskMode() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
