package com.jobautomation.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Weighted score of a job against the candidate profile. Internal only. */
public class MatchResult {
    private int score;                                   // 0-100
    private Decision decision = Decision.REJECT;
    private final Map<String, Integer> breakdown = new LinkedHashMap<>();   // component -> points awarded
    private final Map<String, Integer> maxPoints = new LinkedHashMap<>();   // component -> max points
    private final List<String> matchedSkills = new ArrayList<>();
    private final List<String> missingSkills = new ArrayList<>();
    private final List<String> reasons = new ArrayList<>();
    private boolean hardReject;

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) { this.decision = decision; }
    public Map<String, Integer> getBreakdown() { return breakdown; }
    public Map<String, Integer> getMaxPoints() { return maxPoints; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public List<String> getReasons() { return reasons; }
    public boolean isHardReject() { return hardReject; }
    public void setHardReject(boolean hardReject) { this.hardReject = hardReject; }

    public void component(String name, int awarded, int max) {
        breakdown.put(name, awarded);
        maxPoints.put(name, max);
    }

    public void reason(String r) { reasons.add(r); }

    public boolean isEligible() { return decision == Decision.ELIGIBLE; }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Match: ").append(score).append("% -> ").append(decision).append('\n');
        breakdown.forEach((k, v) -> sb.append("  ").append(String.format("%-22s", k)).append(v).append('/').append(maxPoints.get(k)).append('\n'));
        sb.append("  Matched: ").append(matchedSkills.isEmpty() ? "-" : String.join(", ", matchedSkills)).append('\n');
        sb.append("  Missing: ").append(missingSkills.isEmpty() ? "-" : String.join(", ", missingSkills)).append('\n');
        if (!reasons.isEmpty()) {
            sb.append("  Notes:   ").append(String.join(" | ", reasons)).append('\n');
        }
        return sb.toString();
    }
}
