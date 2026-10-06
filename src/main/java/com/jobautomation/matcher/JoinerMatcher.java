package com.jobautomation.matcher;

import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immediate-joiner compatibility. A JD that demands immediate joining (or a notice period
 * shorter than the candidate's) is rejected unless the profile truthfully satisfies it.
 */
public class JoinerMatcher {

    public static final class Result {
        public final boolean hardReject;
        public final boolean jdWantsImmediate;
        public final String reason;

        Result(boolean hardReject, boolean jdWantsImmediate, String reason) {
            this.hardReject = hardReject;
            this.jdWantsImmediate = jdWantsImmediate;
            this.reason = reason;
        }
    }

    private static final Pattern IMMEDIATE = Pattern.compile(
            "immediate joiner|immediate joining|join immediately|immediate joinee|joining immediately|immediate availability|available immediately|only immediate",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MAX_NOTICE = Pattern.compile(
            "notice period[^.\\n]{0,40}?(?:up ?to|max(?:imum)?|within|less than|not more than|<=?)\\s*(\\d{1,3})\\s*days"
                    + "|(\\d{1,3})\\s*days?\\s*(?:or less|max(?:imum)?)?\\s*notice",
            Pattern.CASE_INSENSITIVE);

    public Result evaluate(JobDetails job, CandidateProfile profile) {
        String text = job.getDescription() == null ? "" : job.getDescription();
        boolean wantsImmediate = IMMEDIATE.matcher(text).find() || (job.getTitle() != null && IMMEDIATE.matcher(job.getTitle()).find());
        Integer maxNoticeDays = null;
        Matcher m = MAX_NOTICE.matcher(text);
        if (m.find()) {
            String g = m.group(1) != null ? m.group(1) : m.group(2);
            if (g != null) {
                maxNoticeDays = Integer.parseInt(g);
            }
        }

        boolean immediate = Boolean.TRUE.equals(profile.getImmediateJoiner());
        Integer myNotice = profile.getNoticePeriodDays();

        if (wantsImmediate) {
            if (immediate) {
                return new Result(false, true, "JD wants immediate joiner - candidate is available immediately");
            }
            if (myNotice != null && myNotice <= 15) {
                return new Result(false, true, "JD wants immediate joiner - candidate notice is " + myNotice + " days");
            }
            return new Result(true, true, "JD requires an immediate joiner and the profile is not configured as immediately available");
        }
        if (maxNoticeDays != null) {
            if (immediate || (myNotice != null && myNotice <= maxNoticeDays)) {
                return new Result(false, false, "JD notice limit " + maxNoticeDays + " days is satisfied");
            }
            if (myNotice != null) {
                return new Result(true, false, "JD allows at most " + maxNoticeDays + " days notice; candidate has " + myNotice);
            }
            return new Result(false, false, "JD limits notice to " + maxNoticeDays + " days; candidate notice not configured (will be asked in the form)");
        }
        return new Result(false, false, "No joining-time constraint in JD");
    }
}
