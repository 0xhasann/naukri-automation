package com.jobautomation.models;

import java.util.Objects;

/** A job as it appears on a search-results card (before the details page is opened). */
public class JobListing {
    private String jobId;
    private String title;
    private String company;
    private String location;
    private String experienceText;
    private String salaryText;
    private String url;
    private String searchCity;
    private String searchKeyword;

    public JobListing() { }

    public JobListing(String jobId, String title, String company, String location, String url) {
        this.jobId = jobId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getExperienceText() { return experienceText; }
    public void setExperienceText(String experienceText) { this.experienceText = experienceText; }
    public String getSalaryText() { return salaryText; }
    public void setSalaryText(String salaryText) { this.salaryText = salaryText; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSearchCity() { return searchCity; }
    public void setSearchCity(String searchCity) { this.searchCity = searchCity; }
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobListing)) return false;
        JobListing that = (JobListing) o;
        return Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() { return Objects.hash(jobId); }

    @Override
    public String toString() {
        return "[" + jobId + "] " + title + " @ " + company + " (" + location + ")";
    }
}
