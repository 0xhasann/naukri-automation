package com.jobautomation.matcher;

import com.jobautomation.config.AppConfig;
import com.jobautomation.filters.CompanyFilter;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.Decision;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.MatchResult;

import java.util.Set;

/**
 * Weighted JD-vs-profile matching engine.
 * <pre>
 *   role                 25
 *   java / backend       25
 *   spring / micro       15
 *   database / messaging 10
 *   cloud / devops       10
 *   experience           10
 *   location              5
 * </pre>
 * Evaluation order per job: role -> full JD -> company/salary filters -> Java relevance ->
 * experience -> immediate-joiner compatibility -> skill overlap by category -> location.
 * Hard rejects (wrong role family, no Java, salary below minimum, experience far above,
 * joining constraint not satisfiable, blocked/spam company) short-circuit to REJECT.
 * The score is internal and is never sent to the portal.
 */
public class JobMatcher {

    private final AppConfig config;
    private final RoleMatcher roleMatcher;
    private final SkillMatcher skillMatcher;
    private final ExperienceMatcher experienceMatcher;
    private final LocationMatcher locationMatcher;
    private final SalaryMatcher salaryMatcher;
    private final JoinerMatcher joinerMatcher;
    private final CompanyFilter companyFilter;

    public JobMatcher(AppConfig config) {
        this.config = config;
        this.roleMatcher = new RoleMatcher(config);
        this.skillMatcher = new SkillMatcher();
        this.experienceMatcher = new ExperienceMatcher();
        this.locationMatcher = new LocationMatcher(config);
        this.salaryMatcher = new SalaryMatcher(config);
        this.joinerMatcher = new JoinerMatcher();
        this.companyFilter = new CompanyFilter(config);
    }

    public MatchResult match(JobDetails job, CandidateProfile profile) {
        JdParser.enrich(job);
        MatchResult r = new MatchResult();
        AppConfig.Weights w = config.getMatching().getWeights();
        double weightTotal = Math.max(1, w.total());

        // ---- hard filters first (cheap, decisive)
        CompanyFilter.Result company = companyFilter.evaluate(job);
        if (company.reject()) {
            return reject(r, company.reason);
        }
        SalaryMatcher.Result salary = salaryMatcher.evaluate(job);
        if (salary.reject) {
            return reject(r, salary.reason);
        }
        r.reason(salary.reason);

        RoleMatcher.Result role = roleMatcher.evaluate(job, profile);
        if (role.hardReject) {
            return reject(r, role.reason);
        }
        ExperienceMatcher.Result exp = experienceMatcher.evaluate(job, profile);
        if (exp.hardReject) {
            return reject(r, exp.reason);
        }
        JoinerMatcher.Result joiner = joinerMatcher.evaluate(job, profile);
        if (joiner.hardReject) {
            return reject(r, joiner.reason);
        }
        if (joiner.jdWantsImmediate) {
            r.reason(joiner.reason);
        }

        // ---- weighted components
        Set<String> jdSkills = skillMatcher.requiredSkills(job);
        SkillMatcher.CategoryScore javaBackend = skillMatcher.scoreJavaBackend(jdSkills, profile);
        SkillMatcher.CategoryScore spring = skillMatcher.score(SkillTaxonomy.Category.SPRING_MICROSERVICES, jdSkills, profile);
        SkillMatcher.CategoryScore data = skillMatcher.score(SkillTaxonomy.Category.DATABASE_MESSAGING, jdSkills, profile);
        SkillMatcher.CategoryScore cloud = skillMatcher.score(SkillTaxonomy.Category.CLOUD_DEVOPS, jdSkills, profile);
        SkillMatcher.CategoryScore testing = skillMatcher.score(SkillTaxonomy.Category.TESTING, jdSkills, profile);
        SkillMatcher.CategoryScore other = skillMatcher.score(SkillTaxonomy.Category.OTHER, jdSkills, profile);
        LocationMatcher.Result loc = locationMatcher.evaluate(job, profile);

        double combo = skillMatcher.comboBonus(jdSkills, profile);
        double springFraction = Math.min(1.0, spring.fraction + combo * 0.25);

        double points = 0;
        points += award(r, "Role", role.score, w.getRole());
        points += award(r, "Java / Backend", javaBackend.fraction, w.getJavaBackend());
        points += award(r, "Spring / Microservices", springFraction, w.getSpringMicroservices());
        points += award(r, "Database / Messaging", data.fraction, w.getDatabaseMessaging());
        points += award(r, "Cloud / DevOps", cloud.fraction, w.getCloudDevops());
        points += award(r, "Experience", exp.score, w.getExperience());
        points += award(r, "Location", loc.score, w.getLocation());

        int score = (int) Math.round(points * 100.0 / weightTotal);
        if (company.preferred) {
            score = Math.min(100, score + 3);
            r.reason(company.reason);
        }
        if (joiner.jdWantsImmediate && Boolean.TRUE.equals(profile.getImmediateJoiner())) {
            score = Math.min(100, score + 2);   // "immediate joiner" JDs that the candidate can truthfully satisfy
        }
        r.setScore(score);

        // ---- matched / missing lists (all categories, for the report)
        for (SkillMatcher.CategoryScore cs : new SkillMatcher.CategoryScore[]{javaBackend, spring, data, cloud, testing, other}) {
            cs.matched.forEach(s -> addUnique(r.getMatchedSkills(), s));
            cs.missing.forEach(s -> addUnique(r.getMissingSkills(), s));
        }
        r.reason(role.reason);
        r.reason(exp.reason);
        r.reason(loc.reason);

        // ---- decision
        int eligible = config.getMatching().getEligibleScore();
        int review = config.getMatching().getReviewScore();
        if (score >= eligible) {
            r.setDecision(Decision.ELIGIBLE);
        } else if (score >= review) {
            r.setDecision(config.getMatching().isAllowReviewBandApplications() ? Decision.ELIGIBLE : Decision.REVIEW);
            if (r.getDecision() == Decision.REVIEW) {
                r.reason("Score in review band " + review + "-" + (eligible - 1));
            }
        } else {
            r.setDecision(Decision.REJECT);
            r.reason("Score below review threshold " + review);
        }
        return r;
    }

    private static double award(MatchResult r, String name, double fraction, int weight) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        int awarded = (int) Math.round(f * weight);
        r.component(name, awarded, weight);
        return awarded;
    }

    private static MatchResult reject(MatchResult r, String reason) {
        r.setScore(0);
        r.setHardReject(true);
        r.setDecision(Decision.REJECT);
        r.reason(reason);
        return r;
    }

    private static void addUnique(java.util.List<String> list, String s) {
        if (!list.contains(s)) {
            list.add(s);
        }
    }
}
