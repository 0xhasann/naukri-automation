package com.jobautomation.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Canonical skill names, their synonyms as they appear in job descriptions, and the
 * scoring category each belongs to. Matching is word-boundary aware and normalised,
 * so "SpringBoot", "Spring-Boot" and "Spring Boot" all resolve to {@code spring boot}.
 */
public final class SkillTaxonomy {

    public enum Category { JAVA_BACKEND, SPRING_MICROSERVICES, DATABASE_MESSAGING, CLOUD_DEVOPS, TESTING, OTHER }

    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();
    private static final Map<String, Category> CATEGORY = new LinkedHashMap<>();
    private static final Map<String, String> SYNONYM_TO_CANONICAL = new LinkedHashMap<>();
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    /** Primary languages that, when they own the title without Java, mean "not a Java role". */
    public static final Set<String> COMPETING_PRIMARY_LANGUAGES = Set.of(
            "python", "golang", ".net", "c#", "php", "ruby", "scala", "kotlin", "rust", "c++", "node.js");

    static {
        // ---- Java / backend fundamentals
        def("java", Category.JAVA_BACKEND, "java", "core java", "java 8", "java8", "java 11", "java11", "java 17", "java17", "java 21", "java21", "j2ee", "java ee", "jdk", "jvm");
        def("backend development", Category.JAVA_BACKEND, "backend", "back-end", "back end", "server-side", "server side", "backend development", "backend engineering");
        def("concurrency", Category.JAVA_BACKEND, "concurrency", "multithreading", "multi-threading", "multi threading", "threading", "concurrent programming", "executor");
        def("distributed systems", Category.JAVA_BACKEND, "distributed systems", "distributed system", "distributed computing", "large-scale systems", "large scale systems", "high-throughput", "high throughput", "scalable systems", "low latency", "low-latency");
        def("data structures", Category.JAVA_BACKEND, "data structures", "algorithms", "dsa", "data structures and algorithms", "problem solving");
        def("oop", Category.JAVA_BACKEND, "oop", "oops", "object oriented", "object-oriented", "design patterns", "solid principles");
        def("rest apis", Category.JAVA_BACKEND, "rest", "rest api", "rest apis", "restful", "restful apis", "restful services", "web services", "api development", "http apis");
        def("system design", Category.JAVA_BACKEND, "system design", "hld", "lld", "low level design", "high level design", "architecture design");
        def("collections", Category.JAVA_BACKEND, "collections", "java collections", "streams", "java streams", "lambda", "lambdas", "functional programming");
        def("jvm tuning", Category.JAVA_BACKEND, "jvm tuning", "garbage collection", "gc tuning", "performance tuning", "profiling");
        def("event-driven architecture", Category.JAVA_BACKEND, "event-driven", "event driven", "event-driven architecture", "event sourcing", "cqrs", "asynchronous processing", "async processing");

        // ---- Spring / microservices
        def("spring boot", Category.SPRING_MICROSERVICES, "spring boot", "springboot", "spring-boot", "spring boot microservices");
        def("spring", Category.SPRING_MICROSERVICES, "spring", "spring framework", "spring mvc", "spring core", "spring data", "spring data jpa", "spring cloud", "spring batch", "spring webflux", "webflux");
        def("spring security", Category.SPRING_MICROSERVICES, "spring security", "oauth2", "oauth 2", "oauth", "jwt", "authentication and authorization");
        def("microservices", Category.SPRING_MICROSERVICES, "microservices", "microservice", "micro services", "micro-services", "microservice architecture", "service oriented", "soa");
        def("hibernate", Category.SPRING_MICROSERVICES, "hibernate", "jpa", "orm", "jdbc", "mybatis");
        def("spring ai", Category.SPRING_MICROSERVICES, "spring ai", "langchain4j");
        def("node.js", Category.SPRING_MICROSERVICES, "node.js", "nodejs", "node js", "express.js", "expressjs", "nestjs");
        def("graphql", Category.SPRING_MICROSERVICES, "graphql");
        def("grpc", Category.SPRING_MICROSERVICES, "grpc", "protobuf", "protocol buffers");

        // ---- Databases / messaging
        def("mysql", Category.DATABASE_MESSAGING, "mysql", "mariadb");
        def("postgresql", Category.DATABASE_MESSAGING, "postgresql", "postgres", "postgre sql", "psql");
        def("sql", Category.DATABASE_MESSAGING, "sql", "rdbms", "relational database", "relational databases", "oracle", "oracle db", "mssql", "sql server", "database design");
        def("mongodb", Category.DATABASE_MESSAGING, "mongodb", "mongo", "mongo db", "nosql", "no-sql", "document database");
        def("redis", Category.DATABASE_MESSAGING, "redis", "caching", "cache", "memcached", "distributed cache", "in-memory cache");
        def("kafka", Category.DATABASE_MESSAGING, "kafka", "apache kafka", "kafka streams", "event streaming", "message broker", "messaging queue", "message queue", "messaging");
        def("rabbitmq", Category.DATABASE_MESSAGING, "rabbitmq", "activemq", "jms", "pub/sub", "pubsub");
        def("aws sqs", Category.DATABASE_MESSAGING, "sqs", "aws sqs", "sns", "aws sns", "dead letter queue", "dlq");
        def("elasticsearch", Category.DATABASE_MESSAGING, "elasticsearch", "elastic search", "opensearch");
        def("cassandra", Category.DATABASE_MESSAGING, "cassandra", "dynamodb", "dynamo db", "hbase");

        // ---- Cloud / DevOps / observability
        def("aws", Category.CLOUD_DEVOPS, "aws", "amazon web services", "ec2", "s3", "lambda functions", "aws lambda", "cloud", "cloud services", "cloud platforms");
        def("docker", Category.CLOUD_DEVOPS, "docker", "containers", "containerization", "containerisation");
        def("kubernetes", Category.CLOUD_DEVOPS, "kubernetes", "k8s", "eks", "openshift", "helm");
        def("ci/cd", Category.CLOUD_DEVOPS, "ci/cd", "ci cd", "cicd", "continuous integration", "continuous delivery", "continuous deployment", "build pipelines", "devops");
        def("jenkins", Category.CLOUD_DEVOPS, "jenkins");
        def("github actions", Category.CLOUD_DEVOPS, "github actions", "gitlab ci", "gitlab");
        def("git", Category.CLOUD_DEVOPS, "git", "github", "bitbucket", "version control", "svn");
        def("linux", Category.CLOUD_DEVOPS, "linux", "unix", "shell scripting", "bash");
        def("maven", Category.CLOUD_DEVOPS, "maven", "gradle", "build tools");
        def("azure", Category.CLOUD_DEVOPS, "azure", "microsoft azure", "azure devops");
        def("gcp", Category.CLOUD_DEVOPS, "gcp", "google cloud", "google cloud platform");
        def("terraform", Category.CLOUD_DEVOPS, "terraform", "ansible", "infrastructure as code", "iac");
        def("datadog", Category.CLOUD_DEVOPS, "datadog", "new relic", "newrelic", "apm", "monitoring", "observability");
        def("prometheus", Category.CLOUD_DEVOPS, "prometheus", "grafana", "metrics");
        def("elk stack", Category.CLOUD_DEVOPS, "elk", "elk stack", "kibana", "logstash", "splunk", "centralized logging", "logging");

        // ---- Testing
        def("junit", Category.TESTING, "junit", "junit 5", "junit5", "unit testing", "unit tests", "tdd", "test driven development");
        def("mockito", Category.TESTING, "mockito", "mocking", "powermock");
        def("rest assured", Category.TESTING, "rest assured", "rest-assured", "restassured", "api testing", "api automation");
        def("k6", Category.TESTING, "k6", "load testing", "performance testing", "jmeter", "gatling");
        def("selenium", Category.TESTING, "selenium", "testng", "cucumber", "bdd", "playwright", "cypress", "appium");

        // ---- Other (recognised so they show up as matched / missing, but not scored heavily)
        def("typescript", Category.OTHER, "typescript");
        def("javascript", Category.OTHER, "javascript", "js", "es6");
        def("react", Category.OTHER, "react", "reactjs", "react.js", "redux", "next.js", "nextjs");
        def("angular", Category.OTHER, "angular", "angularjs", "angular js");
        def("html/css", Category.OTHER, "html", "css", "html5", "css3", "bootstrap", "tailwind");
        def("python", Category.OTHER, "python", "django", "flask", "fastapi");
        def("golang", Category.OTHER, "golang", "go lang", "go language");
        def(".net", Category.OTHER, ".net", "dotnet", "asp.net", ".net core");
        def("c#", Category.OTHER, "c#", "csharp");
        def("c++", Category.OTHER, "c++", "cpp");
        def("c", Category.OTHER, "c language", "c programming", "embedded c");
        def("php", Category.OTHER, "php", "laravel");
        def("ruby", Category.OTHER, "ruby", "ruby on rails", "rails");
        def("scala", Category.OTHER, "scala", "akka");
        def("kotlin", Category.OTHER, "kotlin");
        def("rust", Category.OTHER, "rust");
        def("llms", Category.OTHER, "llm", "llms", "large language models", "generative ai", "genai", "gen ai", "openai", "gpt", "prompt engineering");
        def("rag", Category.OTHER, "rag", "retrieval augmented generation", "retrieval-augmented generation", "vector database", "vector db", "embeddings");
        def("ai agents", Category.OTHER, "ai agents", "agentic", "agents", "langchain", "langgraph", "mcp");
        def("machine learning", Category.OTHER, "machine learning", "ml", "deep learning", "tensorflow", "pytorch", "data science");
        def("payment systems", Category.OTHER, "payments", "payment systems", "payment gateway", "upi", "imps", "neft", "card processing", "pci dss", "pci-dss");
        def("fintech", Category.OTHER, "fintech", "banking", "neo banking", "neobanking", "lending", "insurance", "insurtech", "kyc", "core banking");
        def("websockets", Category.OTHER, "websocket", "websockets", "webrtc", "socket.io");
        def("agile", Category.OTHER, "agile", "scrum", "jira", "sprint");
        def("multi-tenant architecture", Category.OTHER, "multi-tenant", "multi tenant", "multitenant", "saas architecture");
        def("hsm", Category.OTHER, "hsm", "encryption", "cryptography", "tokenization");
        def("cloud native", Category.OTHER, "cloud native", "cloud-native", "12 factor", "twelve-factor", "serverless");
    }

    private SkillTaxonomy() {
    }

    private static void def(String canonical, Category category, String... synonyms) {
        List<String> list = new ArrayList<>();
        list.add(canonical);
        Collections.addAll(list, synonyms);
        SYNONYMS.put(canonical, list);
        CATEGORY.put(canonical, category);
        for (String s : list) {
            SYNONYM_TO_CANONICAL.putIfAbsent(TextNormalizer.normalize(s), canonical);
        }
    }

    /** Maps free text to its canonical skill; unknown skills are returned normalised. */
    public static String canonical(String skill) {
        String n = TextNormalizer.normalize(skill);
        if (n.isEmpty()) {
            return "";
        }
        String c = SYNONYM_TO_CANONICAL.get(n);
        if (c != null) {
            return c;
        }
        // "java 17 developer" -> still java; try longest synonym contained as a phrase
        String best = null;
        int bestLen = 0;
        for (Map.Entry<String, String> e : SYNONYM_TO_CANONICAL.entrySet()) {
            String syn = e.getKey();
            if (syn.length() > bestLen && syn.length() >= 3 && phrasePattern(syn).matcher(n).find()) {
                best = e.getValue();
                bestLen = syn.length();
            }
        }
        return best != null ? best : n;
    }

    public static Category categoryOf(String canonical) {
        return CATEGORY.getOrDefault(canonical, Category.OTHER);
    }

    public static Set<String> allCanonical() {
        return Collections.unmodifiableSet(SYNONYMS.keySet());
    }

    /** All canonical skills whose synonyms appear in the text (normalised internally). */
    public static Set<String> findSkills(String text) {
        String n = TextNormalizer.normalize(text);
        Set<String> found = new LinkedHashSet<>();
        if (n.isEmpty()) {
            return found;
        }
        for (Map.Entry<String, List<String>> e : SYNONYMS.entrySet()) {
            for (String syn : e.getValue()) {
                if (phrasePattern(TextNormalizer.normalize(syn)).matcher(n).find()) {
                    found.add(e.getKey());
                    break;
                }
            }
        }
        return found;
    }

    /** True when the specific synonym phrase occurs in the text. */
    public static boolean textMentions(String text, String phrase) {
        return phrasePattern(TextNormalizer.normalize(phrase)).matcher(TextNormalizer.normalize(text)).find();
    }

    /**
     * Word-boundary pattern that treats letters, digits and the technology characters
     * {@code + # .} as word characters, so "java" does not match "javascript" and "c#"
     * does not match "c".
     */
    static Pattern phrasePattern(String normalizedPhrase) {
        return PATTERN_CACHE.computeIfAbsent(normalizedPhrase, p -> {
            String quoted = Pattern.quote(p);
            String left = "(?<![a-z0-9+#])";
            String right = p.endsWith("+") || p.endsWith("#") ? "(?![a-z0-9])" : "(?![a-z0-9+#])";
            // allow "spring boot" to also match "spring-boot" / "spring  boot"
            String flexible = quoted.replace(" ", "\\E[\\s-]*\\Q");
            return Pattern.compile(left + flexible + right, Pattern.CASE_INSENSITIVE);
        });
    }

    public static String display(String canonical) {
        if (canonical == null || canonical.isEmpty()) {
            return "";
        }
        return canonical.toUpperCase(Locale.ROOT).charAt(0) + canonical.substring(1);
    }
}
