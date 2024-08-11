package com.infernalsuite.aswm.loaders.api;

import java.util.List;
import java.util.Map;

public class MapStructure {
    private String worldId;
    private String name;
    private int size;
    private long uploadTimestamp;
    private long updateTimestamp;
    private int version;
    private Map<String, String> notes;
    private List<String> authors;
    private Map<String, String> pictureUrls;

    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public long getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public Map<String, String> getPictureUrls() {
        return pictureUrls;
    }

    public void setPictureUrls(Map<String, String> pictureUrls) {
        this.pictureUrls = pictureUrls;
    }
}
