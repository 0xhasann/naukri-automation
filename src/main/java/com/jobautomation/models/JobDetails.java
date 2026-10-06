package com.jobautomation.models;

import java.util.ArrayList;
import java.util.List;

/** Everything extracted from the job details page. */
public class JobDetails {
    private String jobId;
    private String url;
    private String title;
    private String company;
    private String location;
    private String experienceText;
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private String salaryText;
    private Double minSalaryLpa;
    private Double maxSalaryLpa;
    private boolean salaryDisclosed;
    private String employmentType;
    private String workMode;
    private List<String> keySkills = new ArrayList<>();
    private String description = "";
    private boolean easyApplyAvailable;
    private boolean applyOnCompanySite;
    private boolean alreadyApplied;

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
    public String getExperienceText() { return experienceText; }
    public void setExperienceText(String experienceText) { this.experienceText = experienceText; }
    public Integer getMinExperienceYears() { return minExperienceYears; }
    public void setMinExperienceYears(Integer v) { this.minExperienceYears = v; }
    public Integer getMaxExperienceYears() { return maxExperienceYears; }
    public void setMaxExperienceYears(Integer v) { this.maxExperienceYears = v; }
    public String getSalaryText() { return salaryText; }
    public void setSalaryText(String salaryText) { this.salaryText = salaryText; }
    public Double getMinSalaryLpa() { return minSalaryLpa; }
    public void setMinSalaryLpa(Double minSalaryLpa) { this.minSalaryLpa = minSalaryLpa; }
    public Double getMaxSalaryLpa() { return maxSalaryLpa; }
    public void setMaxSalaryLpa(Double maxSalaryLpa) { this.maxSalaryLpa = maxSalaryLpa; }
    public boolean isSalaryDisclosed() { return salaryDisclosed; }
    public void setSalaryDisclosed(boolean salaryDisclosed) { this.salaryDisclosed = salaryDisclosed; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    public List<String> getKeySkills() { return keySkills; }
    public void setKeySkills(List<String> keySkills) { this.keySkills = keySkills == null ? new ArrayList<>() : keySkills; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public boolean isEasyApplyAvailable() { return easyApplyAvailable; }
    public void setEasyApplyAvailable(boolean v) { this.easyApplyAvailable = v; }
    public boolean isApplyOnCompanySite() { return applyOnCompanySite; }
    public void setApplyOnCompanySite(boolean v) { this.applyOnCompanySite = v; }
    public boolean isAlreadyApplied() { return alreadyApplied; }
    public void setAlreadyApplied(boolean alreadyApplied) { this.alreadyApplied = alreadyApplied; }

    /** Title + key skills + description, the text the matcher works on. */
    public String fullText() {
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title).append('\n');
        if (!keySkills.isEmpty()) sb.append(String.join(", ", keySkills)).append('\n');
        sb.append(description);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "[" + jobId + "] " + title + " @ " + company + " (" + location + ")";
    }
}
