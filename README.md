# Naukri Java Developer – Search & Easy Apply Automation

Java 17 · Selenium 4 · TestNG · Maven · Page Object Model

Logs into Naukri.com with **your** credentials, searches Java Developer / Java Backend roles city by
city, reads every job description, scores it against a **verified candidate profile built from the
resume**, and applies through Easy Apply **only when the match is strong**. It never invents an
answer, never bypasses CAPTCHA/OTP/MFA, never applies twice, and stops exactly at the configured
daily limit.

> **Read section 15 before running LIVE.** Automated use of Naukri may be restricted by Naukri's
> terms of use; this tool acts only as the logged-in candidate, at human-like pace, on their own
> account, and stops the moment the site asks for human verification. Use it responsibly.

---

## Table of contents

1. [Requirements](#1-requirements)
2. [Java installation](#2-java-installation)
3. [Maven installation](#3-maven-installation)
4. [Chrome setup](#4-chrome-setup)
5. [Environment variables (credentials)](#5-environment-variables-credentials)
6. [Configuration (`config.yaml`)](#6-configuration-configyaml)
7. [Candidate profile (`candidate-profile.json`)](#7-candidate-profile-candidate-profilejson)
8. [Test mode](#8-test-mode)
9. [Live mode](#9-live-mode)
10. [Running the application](#10-running-the-application)
11. [Daily limit](#11-daily-limit)
12. [Application history & resume/recovery](#12-application-history--resumerecovery)
13. [Reports, logs & screenshots](#13-reports-logs--screenshots)
14. [Troubleshooting](#14-troubleshooting)
15. [CAPTCHA / OTP / security challenge handling](#15-captcha--otp--security-challenge-handling)
16. [How to change location priority](#16-how-to-change-location-priority)
17. [How to change salary](#17-how-to-change-salary)
18. [How to change the match threshold](#18-how-to-change-the-match-threshold)
19. [How it decides (matching engine)](#19-how-it-decides-matching-engine)
20. [How it answers Easy Apply questions](#20-how-it-answers-easy-apply-questions)
21. [Failure handling](#21-failure-handling)
22. [Project structure](#22-project-structure)
23. [Updating selectors when Naukri changes](#23-updating-selectors-when-naukri-changes)

---

## 1. Requirements

| Component      | Version                                                                                            |
| -------------- | -------------------------------------------------------------------------------------------------- |
| Java (JDK)     | 17 or newer (tested on 17 and 21)                                                                  |
| Maven          | 3.8+                                                                                               |
| Google Chrome  | Any current stable version (Edge / Firefox also supported via `browser:`)                          |
| OS             | Windows, macOS or Linux with a desktop session (`headless: false` is the default so you can watch) |
| Naukri account | Your own, with the resume already uploaded on Naukri                                               |

Internet access is needed for Maven dependencies (first build) and for WebDriverManager to fetch
the matching ChromeDriver automatically.

## 2. Java installation

- **Windows / macOS**: download a JDK 17+ from <https://adoptium.net> and install it. Set `JAVA_HOME`
  to the install folder and add `%JAVA_HOME%\bin` (Windows) / `$JAVA_HOME/bin` (macOS) to `PATH`.
- **Ubuntu / Debian**: `sudo apt install openjdk-17-jdk`
- Verify: `java -version` should print 17 or newer.

## 3. Maven installation

- **Windows**: download the binary zip from <https://maven.apache.org/download.cgi>, unzip to
  `C:\tools\apache-maven`, add `C:\tools\apache-maven\bin` to `PATH`.
- **macOS**: `brew install maven`
- **Ubuntu / Debian**: `sudo apt install maven`
- Verify: `mvn -version`

## 4. Chrome setup

Install Google Chrome normally. Nothing else is required: **WebDriverManager** downloads the
ChromeDriver that matches your Chrome version on first run and caches it under `~/.cache/selenium`.

To use another browser set `browser: edge` or `browser: firefox` in `config.yaml`.

The framework starts a plain Chrome with no stealth/fingerprint tricks, on purpose.

## 5. Environment variables (credentials)

Credentials are **never** stored in files. Export them in the shell that runs Maven:

```bash
# Linux / macOS
export NAUKRI_USERNAME="you@example.com"
export NAUKRI_PASSWORD="your-password"
```

```powershell
# Windows PowerShell
$env:NAUKRI_USERNAME = "you@example.com"
$env:NAUKRI_PASSWORD = "your-password"
```

```cmd
:: Windows cmd
set NAUKRI_USERNAME=you@example.com
set NAUKRI_PASSWORD=your-password
```

**Alternative – a local file.** If you prefer not to use environment variables, copy
`credentials.properties.example` to `credentials.properties` (next to `pom.xml`) and fill in:

```properties
NAUKRI_USERNAME=your-naukri-email
NAUKRI_PASSWORD=your-password
```

The file is git-ignored and is only read when the environment variables are absent
(`-Dcredentials=/other/path.properties` points elsewhere). Environment variables always win.

If neither is provided the run stops **before the browser opens** with
`Naukri credentials missing ...` – so "no browser appeared" together with that message simply
means the credentials were not found. Only the first two characters of the username ever appear
in logs.

## 6. Configuration (`config.yaml`)

File: `src/main/resources/config/config.yaml`. Every value can also be overridden on the command
line with `-D<name>=<value>` (see [section 10](#10-running-the-application)).

| Key                                               | Meaning                                                                                      | Default                                              |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| `browser`, `headless`                             | Browser and whether to hide it                                                               | `chrome`, `false`                                    |
| `mode`                                            | `DRY_RUN`, `TEST` or `LIVE` (usually set with `-Dmode=`)                                     | `DRY_RUN`                                            |
| `dailyApplicationLimit`                           | Hard stop for LIVE mode, counted from persistent history                                     | `50`                                                 |
| `maxJobsPerSearch`, `maxSearchPagesPerKeyword`    | How deep each keyword/city search goes                                                       | `40`, `2`                                            |
| `minStrongMatchesPerCity`                         | After 4 keyword searches, leave the city if fewer strong matches than this                   | `8`                                                  |
| `maxUnknownQuestionsBeforeStop`                   | Stop the run after this many applications hit unknown mandatory questions                    | `5`                                                  |
| `maxConsecutiveFailuresBeforeStop`                | Stop when this many jobs fail in a row (layout change / broken session)                      | `8`                                                  |
| `manualInterventionWaitMinutes`                   | On CAPTCHA/OTP (non-headless) wait this long for you to complete it, then resume; `0` = stop | `10`                                                 |
| `matching.eligibleScore` / `matching.reviewScore` | Apply at ≥ eligible; flag for manual review between review and eligible                      | `75` / `60`                                          |
| `matching.allowReviewBandApplications`            | Also apply in the review band                                                                | `false`                                              |
| `matching.weights.*`                              | Scoring weights (must total 100)                                                             | 25/25/15/10/10/10/5                                  |
| `minimumSalaryLpa`, `rejectUndisclosedSalary`     | Salary filter                                                                                | `20`, `false`                                        |
| `targetExperience.minimum/maximum`                | **Search filter only** – never used as an application answer                                 | `4` / `7`                                            |
| `locationPriority`, `locationSlugs`               | City order and their Naukri URL slugs                                                        | Bengaluru → Hyderabad → Delhi NCR → Pune → … → India |
| `searchKeywords`                                  | Keyword combinations rotated per city                                                        | 19 Java combinations                                 |
| `preferredRoles`, `rejectedRoleKeywords`          | Title screening                                                                              | see file                                             |
| `companies.preferred/blocked/spamIndicators`      | Company screening                                                                            | empty lists + spam markers                           |
| `timeouts.*`, `retry.*`                           | Explicit-wait and transient-retry settings                                                   | 45/20/5 s, 3 × 1.5 s                                 |
| `paths.*`                                         | History file, screenshots, reports, profile, selectors                                       | `data/`, `screenshots/`, `reports/`                  |

## 7. Candidate profile (`candidate-profile.json`)

File: `src/main/resources/testdata/candidate-profile.json`. **This file is the only source of
application answers.** It was built from `Mahboob_Hasan_Resume.pdf` and contains only skills and
experience that the resume supports (Java 8–21, Spring Boot, Spring Security, Spring AI, Node.js,
Microservices, Kafka, Redis, MySQL, PostgreSQL, MongoDB, SQS/DLQ, JUnit, Mockito, K6, REST Assured,
AWS EC2/S3, Docker, GitHub Actions, Jenkins, CI/CD, Git, Linux, Maven, Datadog, Prometheus, Grafana,
ELK). Hibernate, JPA, Kubernetes, Azure, GCP, React and Angular are deliberately **not** listed.

Rules the code enforces:

- A field that is `null` is _unknown_. Any form question that needs it stops that application with
  `REQUIRES_MANUAL_REVIEW` – nothing is guessed. Currently `null`: `currentSalaryLpa`,
  `noticePeriodDays`, `age` (derived from `dob` automatically).
- `totalExperienceYears` (3.7) comes from the resume dates (Nov 2021 – Jul 2025). The search targets
  4+ years, but the form answer is always the verified 3.7 – they are kept separate on purpose.
- `skillExperienceYears` gives per-technology years for "How many years in Kafka?" questions. A
  technology **not** in the profile is never answered "Yes" or "No"; add it with `0` to state
  explicitly that there is no experience (then the truthful answer is "No"/"0").
- `relocationWilling`, `immediateJoiner` drive the Yes/No answers with those names; set them to what
  is actually true.
- `dob`, `gender`, `expectedSalaryLpa`, `currentLocation` are used only when a form makes them
  mandatory.

Edit the JSON, keep it valid, and re-run. It is loaded on every start.

## 8. Test mode

```bash
mvn test -Dsuite=testng-e2e.xml -Dmode=TEST
```

TEST mode logs in, searches, extracts and scores every JD, checks that Easy Apply is available,
and **rehearses the question engine** (logs what it would answer to the common Naukri questions).
It does **not** click _Apply_. Reason: on Naukri, for jobs without screening questions the Apply
button itself submits the application instantly, so the only way to guarantee "no submission" is
never to click it. Results are recorded as `WOULD_APPLY`.

`DRY_RUN` (the default) is the same without the rehearsal and prints a decision block per job:

```
JOB:        Java Backend Developer
COMPANY:    ABC
LOCATION:   Bengaluru
SALARY:     20-30 Lacs PA
EXPERIENCE: 4-7 Yrs
MATCH:      87%
MATCHED:    java, spring boot, kafka, redis, aws, microservices
MISSING:    kubernetes
DECISION:   ELIGIBLE
```

Always run DRY_RUN/TEST first and read `reports/` before going LIVE.

## 9. Live mode

```bash
mvn test -Dsuite=testng-e2e.xml -Dmode=LIVE
```

Full flow: for each eligible job it re-checks the daily limit, clicks Easy Apply, answers each
chatbot question **only** from the profile, saves, waits for Naukri's confirmation, screenshots it,
then **reloads the job page and checks that Naukri now shows it as applied** before recording
`APPLIED`. If that verification fails the job is recorded as `FAILED` (retryable) instead of being
claimed as applied. Any unknown/unanswerable question aborts that application with a screenshot and
`REQUIRES_MANUAL_REVIEW`; the run continues with the next job.

Start with a small limit the first time: `-DdailyApplicationLimit=5`.

## 10. Running the application

```bash
# 1. unit tests only (no browser, no credentials) – run this after any change
mvn test

# 2. browser smoke + pipeline in the chosen mode
mvn test -Dsuite=testng-e2e.xml -Dmode=DRY_RUN
mvn test -Dsuite=testng-e2e.xml -Dmode=TEST
mvn test -Dsuite=testng-e2e.xml -Dmode=LIVE

# 3. without TestNG (plain main class)
mvn compile exec:java -Dmode=DRY_RUN
mvn compile exec:java -Dmode=LIVE -DdailyApplicationLimit=100 -Dheadless=false
F
# useful overrides
-DminimumSalaryLpa=25  -DminimumMatchScore=80  -DmaxJobsPerSearch=20
-DcandidateProfile=/path/to/other-profile.json  -Dconfig=/path/to/other-config.yaml
```

IntelliJ / Eclipse: import as a Maven project; run `Main` with VM option `-Dmode=DRY_RUN` and the
two environment variables set in the run configuration.

## 11. Daily limit

`dailyApplicationLimit` counts `APPLIED` records in `data/application-history.json` whose
timestamp is today. Because the count is read from disk, restarting the program, or a crash, cannot
reset it. The limit is checked before each search batch and again immediately before every Apply
click; when reached the run logs `Daily application limit reached.` and ends cleanly. TEST and
DRY_RUN never count towards it.

## 12. Application history & resume/recovery

`data/application-history.json` (created automatically, git-ignored) holds one record per job:

```json
{
  "jobId": "123456",
  "url": "...",
  "title": "Java Backend Developer",
  "company": "Example",
  "location": "Bengaluru",
  "matchScore": 86,
  "status": "APPLIED",
  "reason": "Submitted via Easy Apply; 3 question(s) answered from profile",
  "timestamp": "2026-09-24T10:30:00",
  "screenshot": "screenshots/2026-09-24/applied_123456_103012.png"
}
```

Statuses: `DISCOVERED`, `REJECTED`, `SKIPPED`, `APPLIED`, `WOULD_APPLY`, `FAILED`,
`REQUIRES_MANUAL_REVIEW`. The file also stores `lastProcessedJobId` and the last city/keyword.

Recovery: the file is rewritten atomically after **every** job, so a crash or Ctrl-C loses nothing.
On the next start, jobs already `APPLIED`, `REJECTED`, `SKIPPED` or flagged for review are skipped
without opening them; `FAILED` jobs are retried once. An `APPLIED` record is never overwritten.
If the file is ever corrupt it is backed up as `*.corrupt-<timestamp>` and a fresh one is started.

## 13. Reports, logs & screenshots

- `reports/daily-application-report_<date_time>.html` – Extent report (open in a browser)
- `reports/daily-application-report_<date_time>.json` and `.md` – same data, machine/markdown
- Contents: totals (discovered, evaluated, rejected, skipped, applied, failed, manual review,
  remaining vs limit) and one row per job with city, company, title, score, salary, status, reason.
- `logs/automation.log` – full log (rolled daily, 30 days kept). Console shows the same at INFO.
- `screenshots/YYYY-MM-DD/` – login failures, extraction failures, unknown questions, application
  failures, security challenges, every successful confirmation.

## 14. Troubleshooting

| Symptom                                                           | What to do                                                                                                                                               |
| ----------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Naukri credentials missing`                                      | Export `NAUKRI_USERNAME` / `NAUKRI_PASSWORD` in the same shell                                                                                           |
| `Login could not be confirmed`                                    | Log in manually once in a normal browser (new-device check), then retry. Check `screenshots/`                                                            |
| `SECURITY CHALLENGE ...`                                          | See [section 15](#15-captcha--otp--security-challenge-handling)                                                                                          |
| `No visible element for 'details.title'` / many `FAILED` in a row | Naukri changed its markup – update `selectors.yaml` ([section 23](#23-updating-selectors-when-naukri-changes))                                           |
| `REQUIRES_MANUAL_REVIEW: Required information missing ...`        | Fill the named field in `candidate-profile.json` (only if true)                                                                                          |
| `Unknown application question` repeatedly                         | Add keywords for it in `QuestionClassifier` and an answer source in `AnswerResolver`, or answer those jobs manually                                      |
| Browser opens and closes instantly                                | Chrome/ChromeDriver mismatch – delete `~/.cache/selenium` and rerun; or set `-Dwebdriver.chrome.driver=/path/to/chromedriver`                            |
| Chrome installed in a non-standard place                          | `-Dchrome.binary="C:\Path\To\chrome.exe"`                                                                                                                |
| DRY_RUN takes very long                                           | It opens every job it finds; for a first look use `-DmaxJobsPerSearch=10 -DmaxSearchPagesPerKeyword=1` and shorten `searchKeywords` / `locationPriority` |
| `session not created` on Linux servers                            | Run with a desktop session or `-Dheadless=true`                                                                                                          |
| Jobs you expected are `REJECTED`                                  | Read the `reason` column: title keyword, salary, experience, "Java optional only", or score – then tune thresholds                                       |
| Score looks off                                                   | Weights are in `config.yaml`; matched/missing lists are in the log and report                                                                            |

## 15. CAPTCHA / OTP / security challenge handling

The framework only **detects** CAPTCHA, OTP/verification-code prompts, MFA, "verify you are
human", access-denied and unusual-traffic pages (`SecurityChallengeDetector`). On detection it:

1. stops clicking immediately and saves a screenshot under `screenshots/`,
2. logs a loud `SECURITY CHALLENGE ...` banner,
3. in non-headless mode **pauses for up to `manualInterventionWaitMinutes` (default 10)** so that
   **you** complete the CAPTCHA / OTP in the open browser window – once the challenge is gone and the
   session is still logged in, the run resumes with the same job (this pause happens at most once per
   run; set the value to `0` to stop immediately instead),
4. otherwise (headless, timeout, or a second challenge) ends the run, writes the report; history is
   already saved, so a restart resumes where it left off.

There is no code that solves, automates or circumvents a challenge, and there is no stealth or
fingerprint manipulation – the only "handling" is waiting for a human.

## 16. How to change location priority

Edit `locationPriority` in `config.yaml` (top entry is searched first). If you add a city that is
not in `locationSlugs`, add its Naukri URL slug too (the part after `-jobs-in-` in a Naukri search
URL). `India` at the end is a nationwide catch-all. The run leaves a city when 4 keyword searches
produced fewer than `minStrongMatchesPerCity` strong matches, and otherwise keeps searching it until
the keyword list is exhausted, so earlier cities receive more applications.

## 17. How to change salary

`minimumSalaryLpa` in `config.yaml` (or `-DminimumSalaryLpa=25`). A job whose disclosed range tops
out below this is rejected; undisclosed salary is allowed unless `rejectUndisclosedSalary: true`.
The **answer** given to "expected CTC" questions is `expectedSalaryLpa` in the profile – a separate
value.

## 18. How to change the match threshold

`matching.eligibleScore` (apply) and `matching.reviewScore` (flag) in `config.yaml`, or
`-DminimumMatchScore=80`. Raise it for fewer, stronger applications. Weights per component are
under `matching.weights` and must total 100.

## 19. How it decides (matching engine)

For every job, in this order (`JobMatcher`):

1. **Company** – blocked list / staffing-spam indicators → reject.
2. **Salary** – disclosed and below minimum → reject.
3. **Role** (`RoleMatcher`) – title contains a rejected keyword (QA, SDET, tester, data, frontend,
   React, Angular, MERN, Node-only, DevOps-only, support, .NET, Python, …) → reject; another language
   owns the title without Java → reject; Java absent from title, key skills and JD → reject; Java
   only as "good to have / plus / optional" → reject; JD dominated by another language → reject.
4. **Experience** (`ExperienceMatcher`) – JD minimum more than 1 year above verified experience →
   reject (3.7 verified vs "4+" is tolerated, "8+" is not).
5. **Immediate joiner** (`JoinerMatcher`) – JD demands immediate joining / short notice that the
   profile cannot truthfully satisfy → reject.
6. **Weighted score** – role 25, Java/backend fundamentals 25, Spring/microservices 15 (+ combo bonus
   for Java+Spring Boot, Java+Kafka, Java+Redis, …), database/messaging 10, cloud/DevOps 10,
   experience 10, location 5. Skill matching is synonym- and word-boundary aware
   (`SkillTaxonomy`): "SpringBoot", "event-driven", "high-throughput", "Postgres" all count;
   "JavaScript" never counts as Java.
7. `≥ eligibleScore` → **ELIGIBLE**, `≥ reviewScore` → **REVIEW** (recorded, not applied),
   otherwise **REJECT**. The score is internal and never sent to Naukri.

## 20. How it answers Easy Apply questions

`QuestionClassifier` normalises the question text and maps it to a category (relocation, immediate
joiner, notice period, total/relevant/technology experience, current/preferred location, work
authorisation, visa sponsorship, expected/current salary, DOB, age, gender, qualification, contact
details, confirmation…). `AnswerResolver` then reads the matching profile field:

- value present → answer (for choice questions the answer must be one of the offered options, e.g.
  3.7 years → "3-5 years");
- value `null` → `MISSING_DATA` → application aborted, screenshot, `REQUIRES_MANUAL_REVIEW`;
- category not recognised (shift preferences, essay questions, anything new) → `UNKNOWN` → same.

Every question and the chosen answer are logged. Yes is only ever answered when the profile makes it
true (`relocationWilling`, `immediateJoiner`, a skill that is on the resume).

## 21. Failure handling

- Transient Selenium errors (stale element, click intercepted, timeout, not interactable) are retried
  up to `retry.maxAttempts` with back-off; JS click is used only as a fallback for the same element.
- Any other problem with one job (missing title, un-parseable page, form not advancing) is caught,
  screenshotted, recorded as `FAILED`, and the run continues with the next job.
- A dead browser session is restarted once and re-logged-in; history makes it resume where it was.
- `maxConsecutiveFailuresBeforeStop` consecutive failures stop the run (the layout probably changed).
- Only these end the run: daily limit, login failure, security challenge, missing credentials,
  repeated unknown mandatory questions, unrecoverable browser, configuration error.
- State is persisted after every job; a shutdown hook flushes history on Ctrl-C.

## 22. Project structure

```
src/main/java/com/jobautomation
 ├── Main.java                      plain entry point
 ├── config/        AppConfig, ConfigLoader, ProfileLoader, Credentials, SelectorRepository, RunMode
 ├── driver/        DriverFactory, DriverManager (liveness + restart)
 ├── pages/         BasePage, LoginPage, HomePage, JobSearchPage, JobResultsPage, JobDetailsPage,
 │                  EasyApplyPage, ApplicationConfirmationPage
 ├── components/    SecurityChallengeDetector
 ├── models/        CandidateProfile, JobListing, JobDetails, MatchResult, Question, Answer,
 │                  ApplicationRecord, ApplicationStatus, Decision, RunSummary
 ├── matcher/       TextNormalizer, SkillTaxonomy, JdParser, RoleMatcher, SkillMatcher,
 │                  ExperienceMatcher, LocationMatcher, SalaryMatcher, JoinerMatcher, JobMatcher
 ├── filters/       CompanyFilter
 ├── questions/     QuestionCategory, QuestionClassifier, AnswerResolver, QuestionAnswerService
 ├── application/   ApplicationService
 ├── services/      JobSearchService, DailyApplicationLimitService, DuplicateCheckService,
 │                  ReportService, JobAutomationRunner
 ├── storage/       ApplicationHistoryStore, HistoryState
 ├── utils/         ScreenshotUtil, RetryUtil
 └── exceptions/    AutomationStopException (+StopReason), JobSkipException, SecurityChallengeException, …
src/main/resources
 ├── config/config.yaml, config/selectors.yaml
 ├── testdata/candidate-profile.json
 └── logback.xml
src/test/java/com/jobautomation/tests
 ├── unit/   JobMatcherTest, SkillMatcherTest, ExperienceMatcherTest, LocationMatcherTest,
 │           SalaryMatcherTest, QuestionAnswerServiceTest, DailyLimitServiceTest,
 │           DuplicateCheckServiceTest, ConfigAndProfileTest
 └── e2e/    LoginTest, JobSearchTest, JobMatchingTest, EasyApplyTest (browser; skipped without credentials)
src/test/resources/testng-unit.xml, testng-e2e.xml
```

## 23. Updating selectors when Naukri changes

All locators live in `src/main/resources/config/selectors.yaml`, grouped by page. Each key holds a
list of fallbacks tried in order (`css=` or `xpath=`); the first that resolves is used. When a page
fails, open the screenshot, inspect the element in Chrome DevTools, and add a new selector at the
top of the list – prefer ids, data attributes, stable partial class names (`[class*='jd-header']`)
and short relative XPath; avoid absolute XPath and generated hash suffixes. No Java change is needed.

---

### Principles baked into the code

Java development roles only · resume is the source of truth · no invented skills or personal data ·
read the whole JD, never apply because a title says "Java" · quality over quantity · one application
per job and per company+role · persistent history · exact daily limit · unknown question = stop, not
guess · CAPTCHA/OTP/MFA = stop, never bypass · DRY_RUN → TEST → LIVE.
