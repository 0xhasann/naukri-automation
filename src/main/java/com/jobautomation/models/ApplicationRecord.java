package com.jobautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/** One line of application-history.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationRecord {
    private String jobId;
    private String url;
    private String title;
    private String company;
    private String location;
    private String searchCity;
    private Integer matchScore;
    private String salary;
    private String experience;
    private ApplicationStatus status;
    private String reason;
    private LocalDateTime timestamp;
    private String screenshot;

    public static ApplicationRecord of(JobDetails d, ApplicationStatus status, Integer score, String reason) {
        ApplicationRecord r = new ApplicationRecord();
        r.jobId = d.getJobId();
        r.url = d.getUrl();
        r.title = d.getTitle();
        r.company = d.getCompany();
        r.location = d.getLocation();
        r.matchScore = score;
        r.salary = d.getSalaryText();
        r.experience = d.getExperienceText();
        r.status = status;
        r.reason = reason;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static ApplicationRecord of(JobListing l, ApplicationStatus status, String reason) {
        ApplicationRecord r = new ApplicationRecord();
        r.jobId = l.getJobId();
        r.url = l.getUrl();
        r.title = l.getTitle();
        r.company = l.getCompany();
        r.location = l.getLocation();
        r.searchCity = l.getSearchCity();
        r.salary = l.getSalaryText();
        r.experience = l.getExperienceText();
        r.status = status;
        r.reason = reason;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSearchCity() { return searchCity; }
    public void setSearchCity(String searchCity) { this.searchCity = searchCity; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getScreenshot() { return screenshot; }
    public void setScreenshot(String screenshot) { this.screenshot = screenshot; }
}
