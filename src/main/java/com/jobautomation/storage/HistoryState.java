package com.jobautomation.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobautomation.models.ApplicationRecord;

import java.util.ArrayList;
import java.util.List;

/** On-disk shape of application-history.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryState {
    private String lastProcessedJobId;
    private String lastSearchCity;
    private String lastSearchKeyword;
    private int lastSearchPage;
    private List<ApplicationRecord> records = new ArrayList<>();

    public String getLastProcessedJobId() { return lastProcessedJobId; }
    public void setLastProcessedJobId(String lastProcessedJobId) { this.lastProcessedJobId = lastProcessedJobId; }
    public String getLastSearchCity() { return lastSearchCity; }
    public void setLastSearchCity(String lastSearchCity) { this.lastSearchCity = lastSearchCity; }
    public String getLastSearchKeyword() { return lastSearchKeyword; }
    public void setLastSearchKeyword(String lastSearchKeyword) { this.lastSearchKeyword = lastSearchKeyword; }
    public int getLastSearchPage() { return lastSearchPage; }
    public void setLastSearchPage(int lastSearchPage) { this.lastSearchPage = lastSearchPage; }
    public List<ApplicationRecord> getRecords() { return records; }
    public void setRecords(List<ApplicationRecord> records) { this.records = records == null ? new ArrayList<>() : records; }
}
