package com.jobautomation.matcher;

import com.jobautomation.config.AppConfig;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether the role itself is a Java software / backend development role.
 * Produces a 0.0-1.0 score and, for clear mismatches, a hard reject.
 */
public class RoleMatcher {

    public static final class Result {
        public final double score;          // 0..1
        public final boolean hardReject;
        public final String reason;

        Result(double score, boolean hardReject, String reason) {
            this.score = score;
            this.hardReject = hardReject;
            this.reason = reason;
        }
    }

    private static final Pattern JAVA_IN_TITLE = Pattern.compile("(?<![a-z])java(?!\\s*script)(?![a-z])", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEV_WORDS = Pattern.compile("developer|engineer|programmer|sde|backend|back-end|back end|software", Pattern.CASE_INSENSITIVE);
    private static final Pattern FULLSTACK = Pattern.compile("full[\\s-]*stack", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPTIONAL_CONTEXT = Pattern.compile(
            "(good to have|nice to have|nice-to-have|good-to-have|preferred|plus|bonus|optional|added advantage|advantage|desirable|familiarity with|exposure to)[^.\\n]{0,80}?(?<![a-z])java(?!\\s*script)(?![a-z])"
                    + "|(?<![a-z])java(?!\\s*script)(?![a-z])[^.\\n]{0,40}?(is a plus|is an advantage|good to have|nice to have|optional|preferred but not)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REQUIRED_CONTEXT = Pattern.compile(
            "(must have|must-have|required|mandatory|strong|expert|proficien|hands[\\s-]*on|experience (?:in|with)|expertise (?:in|with)|core)[^.\\n]{0,80}?(?<![a-z])java(?!\\s*script)(?![a-z])",
            Pattern.CASE_INSENSITIVE);

    private final AppConfig config;

    public RoleMatcher(AppConfig config) {
        this.config = config;
    }

    public Result evaluate(JobDetails job, CandidateProfile profile) {
        String title = job.getTitle() == null ? "" : job.getTitle();
        String titleNorm = TextNormalizer.normalize(title);
        String descNorm = TextNormalizer.normalize(job.getDescription());
        String skillsNorm = TextNormalizer.normalize(String.join(" , ", job.getKeySkills()));

        // 1. Explicitly rejected role families (QA, data, frontend, support, other languages ...)
        for (String bad : config.getRejectedRoleKeywords()) {
            String b = TextNormalizer.normalize(bad);
            if (b.isEmpty()) {
                continue;
            }
            if (SkillTaxonomy.phrasePattern(b).matcher(titleNorm).find()) {
                // "Java Developer (not a tester)" style false positives are rare; a plain Java dev title never contains these words
                return new Result(0, true, "Title matches rejected role keyword '" + bad + "'");
            }
        }

        // 2. Another language owns the title and Java does not appear in it -> not a Java role
        boolean javaInTitle = JAVA_IN_TITLE.matcher(title).find();
        if (!javaInTitle) {
            for (String lang : SkillTaxonomy.COMPETING_PRIMARY_LANGUAGES) {
                if (SkillTaxonomy.phrasePattern(lang).matcher(titleNorm).find()) {
                    return new Result(0, true, "Title is a " + lang + " role, not Java");
                }
            }
        }

        // 3. Java must be present somewhere meaningful
        boolean javaInSkills = SkillTaxonomy.phrasePattern("java").matcher(skillsNorm).find();
        int javaMentions = countJava(job.getDescription());
        if (!javaInTitle && !javaInSkills && javaMentions == 0) {
            return new Result(0, true, "Java is not mentioned in the title, key skills or description");
        }

        // 4. Java present only as optional / nice-to-have -> reject (user rule: Java must be core)
        boolean optionalOnly = OPTIONAL_CONTEXT.matcher(job.getDescription()).find()
                && !REQUIRED_CONTEXT.matcher(job.getDescription()).find()
                && !javaInTitle && !javaInSkills;
        if (optionalOnly) {
            return new Result(0, true, "Java is only listed as optional / nice-to-have in this JD");
        }

        // 5. JD is dominated by a competing language: many mentions of it, few of Java
        String dominant = dominantCompetingLanguage(descNorm, javaMentions);
        if (dominant != null && !javaInTitle) {
            return new Result(0, true, "JD is primarily a " + dominant + " role; Java is a minor skill");
        }

        // ---- Score
        double score;
        String reason;
        if (javaInTitle && DEV_WORDS.matcher(title).find()) {
            score = 1.0;
            reason = "Java development title";
        } else if (matchesPreferredRole(titleNorm, profile)) {
            score = javaInSkills || javaMentions >= 2 ? 0.9 : 0.7;
            reason = "Preferred backend/software title" + (javaInSkills ? " with Java in key skills" : "");
        } else if (DEV_WORDS.matcher(title).find()) {
            score = javaInSkills ? 0.75 : (javaMentions >= 3 ? 0.6 : 0.4);
            reason = "Generic engineering title; Java strength inferred from JD";
        } else {
            score = 0.3;
            reason = "Title does not look like a development role";
        }
        if (FULLSTACK.matcher(title).find()) {
            score = Math.min(score, 0.6);
            reason += "; full-stack title (frontend expected)";
        }
        if (titleNorm.contains("lead") || titleNorm.contains("architect") || titleNorm.contains("manager") || titleNorm.contains("principal") || titleNorm.contains("staff")) {
            score = Math.min(score, 0.5);
            reason += "; leadership-level title";
        }
        return new Result(score, false, reason);
    }

    private boolean matchesPreferredRole(String titleNorm, CandidateProfile profile) {
        List<String> roles = config.getPreferredRoles();
        for (String r : roles) {
            String n = TextNormalizer.normalize(r);
            if (!n.isEmpty() && SkillTaxonomy.phrasePattern(n).matcher(titleNorm).find()) {
                return true;
            }
        }
        for (String r : profile.getTargetRoles()) {
            String n = TextNormalizer.normalize(r);
            if (!n.isEmpty() && SkillTaxonomy.phrasePattern(n).matcher(titleNorm).find()) {
                return true;
            }
        }
        return false;
    }

    static int countJava(String text) {
        if (text == null) {
            return 0;
        }
        Matcher m = JAVA_IN_TITLE.matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    private static String dominantCompetingLanguage(String descNorm, int javaMentions) {
        for (String lang : SkillTaxonomy.COMPETING_PRIMARY_LANGUAGES) {
            Matcher m = SkillTaxonomy.phrasePattern(lang).matcher(descNorm);
            int n = 0;
            while (m.find()) {
                n++;
            }
            if (n >= 3 && n >= javaMentions * 2) {
                return lang.toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }
}
