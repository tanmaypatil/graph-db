package com.graphdb.model;

public class Defect {
    private String id;
    private String title;
    private String severity;
    private String status;

    public Defect() {
    }

    public Defect(String id, String title, String severity, String status) {
        this.id = id;
        this.title = title;
        this.severity = severity;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Defect{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", severity='" + severity + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
