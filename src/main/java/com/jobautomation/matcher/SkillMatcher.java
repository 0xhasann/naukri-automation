package com.jobautomation.matcher;

import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Category-level skill comparison between a JD and the verified profile.
 * Skills the JD asks for and the profile has -> matched; asked for but absent -> missing.
 */
public class SkillMatcher {

    public static final class CategoryScore {
        public final SkillTaxonomy.Category category;
        public final double fraction;        // 0..1
        public final List<String> matched;
        public final List<String> missing;
        public final boolean jdMentionsCategory;

        CategoryScore(SkillTaxonomy.Category category, double fraction, List<String> matched, List<String> missing, boolean jdMentionsCategory) {
            this.category = category;
            this.fraction = fraction;
            this.matched = matched;
            this.missing = missing;
            this.jdMentionsCategory = jdMentionsCategory;
        }
    }

    /** High-value combinations from the spec; each one present on both sides earns a bonus. */
    private static final String[][] HIGH_VALUE_COMBOS = {
            {"java", "spring boot"}, {"java", "microservices"}, {"java", "kafka"}, {"java", "redis"},
            {"java", "postgresql"}, {"java", "mysql"}, {"java", "aws"}, {"java", "distributed systems"},
            {"java", "concurrency"}, {"java", "spring security"}
    };

    /** The skills the JD asks for (title + key skills + description), canonicalised. */
    public Set<String> requiredSkills(JobDetails job) {
        Set<String> jd = new LinkedHashSet<>(SkillTaxonomy.findSkills(job.fullText()));
        for (String ks : job.getKeySkills()) {
            String c = SkillTaxonomy.canonical(ks);
            if (!c.isEmpty()) {
                jd.add(c);
            }
        }
        return jd;
    }

    public CategoryScore score(SkillTaxonomy.Category category, Set<String> jdSkills, CandidateProfile profile) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<String> mine = new LinkedHashSet<>(profile.canonicalSkills());
        for (String s : jdSkills) {
            if (SkillTaxonomy.categoryOf(s) != category) {
                continue;
            }
            if (mine.contains(s)) {
                matched.add(s);
            } else {
                missing.add(s);
            }
        }
        int asked = matched.size() + missing.size();
        if (asked == 0) {
            // JD is silent on this category: neutral, neither reward nor punish
            return new CategoryScore(category, 0.5, matched, missing, false);
        }
        double fraction = (double) matched.size() / asked;
        return new CategoryScore(category, fraction, matched, missing, true);
    }

    /**
     * Java/backend fundamentals are scored more strictly than the other categories:
     * Java itself must be required, then every additional fundamental adds.
     */
    public CategoryScore scoreJavaBackend(Set<String> jdSkills, CandidateProfile profile) {
        CategoryScore base = score(SkillTaxonomy.Category.JAVA_BACKEND, jdSkills, profile);
        boolean javaAsked = jdSkills.contains("java");
        boolean javaHave = profile.hasSkill("java");
        if (!javaAsked || !javaHave) {
            return new CategoryScore(base.category, 0.0, base.matched, base.missing, base.jdMentionsCategory);
        }
        double fraction = 0.6;
        int extras = 0;
        for (String s : base.matched) {
            if (!"java".equals(s)) {
                extras++;
            }
        }
        fraction += Math.min(0.4, extras * 0.1);
        // penalise fundamentals the JD wants but the profile lacks
        fraction -= Math.min(0.3, base.missing.size() * 0.1);
        return new CategoryScore(base.category, clamp(fraction), base.matched, base.missing, true);
    }

    /** 0..1 bonus factor for high-value Java combinations present in both JD and profile. */
    public double comboBonus(Set<String> jdSkills, CandidateProfile profile) {
        int hits = 0;
        for (String[] combo : HIGH_VALUE_COMBOS) {
            boolean all = true;
            for (String s : combo) {
                if (!jdSkills.contains(s) || !profile.hasSkill(s)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                hits++;
            }
        }
        return Math.min(1.0, hits / 4.0);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
