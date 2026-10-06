package com.jobautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobautomation.matcher.TextNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The verified candidate profile ({@code candidate-profile.json}).
 * <p>
 * This is the ONLY source of application answers. A {@code null} field means the value
 * is unknown and any question requiring it must abort the application - never guess.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateProfile {

    private String name;
    private String email;
    private String phone;
    private String linkedin;
    private String github;
    private String currentTitle;
    private String targetRole;
    private List<String> targetRoles = new ArrayList<>();

    private Double totalExperienceYears;
    private Double relevantJavaExperienceYears;
    private Integer experienceTarget;
    private Map<String, Double> skillExperienceYears = new LinkedHashMap<>();
    private List<String> skills = new ArrayList<>();
    private List<String> domains = new ArrayList<>();
    private List<Employment> employmentHistory = new ArrayList<>();
    private List<Education> education = new ArrayList<>();
    private String highestQualification;

    private String currentLocation;
    private List<String> preferredLocations = new ArrayList<>();
    private Boolean relocationWilling;
    private Boolean immediateJoiner;
    private Integer noticePeriodDays;
    private Boolean currentlyEmployed;

    private Integer minimumSalaryLpa;
    private Double expectedSalaryLpa;
    private Double currentSalaryLpa;

    private List<String> workAuthorizationCountries = new ArrayList<>();
    private Boolean requiresVisaSponsorship;

    private String dob;
    private Integer age;
    private String gender;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Employment {
        private String title;
        private String company;
        private String location;
        private String from;
        private String to;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Education {
        private String degree;
        private String institution;
        private String from;
        private String to;

        public String getDegree() { return degree; }
        public void setDegree(String degree) { this.degree = degree; }
        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    // ------------------------------------------------------------------ helpers

    /** True only when the skill (or a known synonym) is explicitly on the profile. */
    public boolean hasSkill(String skill) {
        String wanted = TextNormalizer.canonicalSkill(skill);
        if (wanted.isEmpty()) {
            return false;
        }
        for (String s : skills) {
            if (TextNormalizer.canonicalSkill(s).equals(wanted)) {
                return true;
            }
        }
        for (String s : skillExperienceYears.keySet()) {
            if (TextNormalizer.canonicalSkill(s).equals(wanted) && skillExperienceYears.get(s) != null) {
                return true;
            }
        }
        return false;
    }

    /** Verified years for a specific technology; empty when not configured. */
    public Optional<Double> yearsFor(String skill) {
        String wanted = TextNormalizer.canonicalSkill(skill);
        for (Map.Entry<String, Double> e : skillExperienceYears.entrySet()) {
            if (TextNormalizer.canonicalSkill(e.getKey()).equals(wanted) && e.getValue() != null) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }

    public List<String> canonicalSkills() {
        List<String> out = new ArrayList<>();
        for (String s : skills) {
            String c = TextNormalizer.canonicalSkill(s);
            if (!c.isEmpty() && !out.contains(c)) {
                out.add(c);
            }
        }
        for (Map.Entry<String, Double> e : skillExperienceYears.entrySet()) {
            if (e.getValue() != null) {
                String c = TextNormalizer.canonicalSkill(e.getKey());
                if (!c.isEmpty() && !out.contains(c)) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    public boolean isLocationPreferred(String location) {
        if (location == null) {
            return false;
        }
        String loc = location.toLowerCase(Locale.ROOT);
        for (String p : preferredLocations) {
            if (loc.contains(p.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ getters / setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
    public String getGithub() { return github; }
    public void setGithub(String github) { this.github = github; }
    public String getCurrentTitle() { return currentTitle; }
    public void setCurrentTitle(String currentTitle) { this.currentTitle = currentTitle; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public List<String> getTargetRoles() { return targetRoles; }
    public void setTargetRoles(List<String> targetRoles) { this.targetRoles = targetRoles == null ? new ArrayList<>() : targetRoles; }
    public Double getTotalExperienceYears() { return totalExperienceYears; }
    public void setTotalExperienceYears(Double v) { this.totalExperienceYears = v; }
    public Double getRelevantJavaExperienceYears() { return relevantJavaExperienceYears; }
    public void setRelevantJavaExperienceYears(Double v) { this.relevantJavaExperienceYears = v; }
    public Integer getExperienceTarget() { return experienceTarget; }
    public void setExperienceTarget(Integer experienceTarget) { this.experienceTarget = experienceTarget; }
    public Map<String, Double> getSkillExperienceYears() { return skillExperienceYears; }
    public void setSkillExperienceYears(Map<String, Double> v) { this.skillExperienceYears = v == null ? new LinkedHashMap<>() : v; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills == null ? new ArrayList<>() : skills; }
    public List<String> getDomains() { return domains; }
    public void setDomains(List<String> domains) { this.domains = domains == null ? new ArrayList<>() : domains; }
    public List<Employment> getEmploymentHistory() { return employmentHistory; }
    public void setEmploymentHistory(List<Employment> v) { this.employmentHistory = v == null ? new ArrayList<>() : v; }
    public List<Education> getEducation() { return education; }
    public void setEducation(List<Education> education) { this.education = education == null ? new ArrayList<>() : education; }
    public String getHighestQualification() { return highestQualification; }
    public void setHighestQualification(String v) { this.highestQualification = v; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public List<String> getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(List<String> v) { this.preferredLocations = v == null ? new ArrayList<>() : v; }
    public Boolean getRelocationWilling() { return relocationWilling; }
    public void setRelocationWilling(Boolean relocationWilling) { this.relocationWilling = relocationWilling; }
    public Boolean getImmediateJoiner() { return immediateJoiner; }
    public void setImmediateJoiner(Boolean immediateJoiner) { this.immediateJoiner = immediateJoiner; }
    public Integer getNoticePeriodDays() { return noticePeriodDays; }
    public void setNoticePeriodDays(Integer noticePeriodDays) { this.noticePeriodDays = noticePeriodDays; }
    public Boolean getCurrentlyEmployed() { return currentlyEmployed; }
    public void setCurrentlyEmployed(Boolean currentlyEmployed) { this.currentlyEmployed = currentlyEmployed; }
    public Integer getMinimumSalaryLpa() { return minimumSalaryLpa; }
    public void setMinimumSalaryLpa(Integer minimumSalaryLpa) { this.minimumSalaryLpa = minimumSalaryLpa; }
    public Double getExpectedSalaryLpa() { return expectedSalaryLpa; }
    public void setExpectedSalaryLpa(Double expectedSalaryLpa) { this.expectedSalaryLpa = expectedSalaryLpa; }
    public Double getCurrentSalaryLpa() { return currentSalaryLpa; }
    public void setCurrentSalaryLpa(Double currentSalaryLpa) { this.currentSalaryLpa = currentSalaryLpa; }
    public List<String> getWorkAuthorizationCountries() { return workAuthorizationCountries; }
    public void setWorkAuthorizationCountries(List<String> v) { this.workAuthorizationCountries = v == null ? new ArrayList<>() : v; }
    public Boolean getRequiresVisaSponsorship() { return requiresVisaSponsorship; }
    public void setRequiresVisaSponsorship(Boolean v) { this.requiresVisaSponsorship = v; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
