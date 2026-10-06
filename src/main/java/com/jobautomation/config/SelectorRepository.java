package com.jobautomation.config;

import com.jobautomation.exceptions.ConfigurationException;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code selectors.yaml}. Every selector key resolves to an ordered list of
 * {@link By} fallbacks ("css=..." / "xpath=..."). Page objects never hardcode locators.
 */
public final class SelectorRepository {

    private static final Logger log = LoggerFactory.getLogger(SelectorRepository.class);
    private static volatile SelectorRepository instance;

    private final Map<String, Object> root;

    private SelectorRepository(Map<String, Object> root) {
        this.root = root;
    }

    public static SelectorRepository get() {
        SelectorRepository r = instance;
        if (r == null) {
            synchronized (SelectorRepository.class) {
                r = instance;
                if (r == null) {
                    r = load(ConfigLoader.get().getPaths().getSelectors());
                    instance = r;
                }
            }
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    public static SelectorRepository load(String location) {
        Yaml yaml = new Yaml();
        try {
            Path p = Paths.get(location);
            if (Files.exists(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    return new SelectorRepository(yaml.load(in));
                }
            }
            String cp = location.replace("src/main/resources/", "");
            try (InputStream in = SelectorRepository.class.getClassLoader().getResourceAsStream(cp)) {
                if (in == null) {
                    throw new ConfigurationException("selectors.yaml not found at " + location + " or classpath:" + cp);
                }
                return new SelectorRepository(yaml.load(in));
            }
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read selectors: " + e.getMessage(), e);
        }
    }

    /**
     * @param key dotted path, e.g. {@code "login.usernameField"}
     * @return ordered fallback locators, never empty
     */
    @SuppressWarnings("unchecked")
    public List<By> locators(String key) {
        Object node = root;
        for (String part : key.split("\\.")) {
            if (!(node instanceof Map)) {
                throw new ConfigurationException("Selector key not found: " + key);
            }
            node = ((Map<String, Object>) node).get(part);
        }
        if (node == null) {
            throw new ConfigurationException("Selector key not found: " + key);
        }
        List<String> raw = node instanceof List ? (List<String>) node : Collections.singletonList(node.toString());
        List<By> out = new ArrayList<>();
        for (String s : raw) {
            By by = parse(s);
            if (by != null) {
                out.add(by);
            }
        }
        if (out.isEmpty()) {
            throw new ConfigurationException("Selector key has no usable locators: " + key);
        }
        return out;
    }

    /** Raw string values (used for attribute names such as {@code results.jobId}). */
    @SuppressWarnings("unchecked")
    public List<String> rawValues(String key) {
        Object node = root;
        for (String part : key.split("\\.")) {
            if (!(node instanceof Map)) {
                throw new ConfigurationException("Selector key not found: " + key);
            }
            node = ((Map<String, Object>) node).get(part);
        }
        if (node == null) {
            throw new ConfigurationException("Selector key not found: " + key);
        }
        return node instanceof List ? (List<String>) node : Collections.singletonList(node.toString());
    }

    static By parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        if (v.startsWith("css=")) {
            return By.cssSelector(v.substring(4));
        }
        if (v.startsWith("xpath=")) {
            return By.xpath(v.substring(6));
        }
        if (v.startsWith("id=")) {
            return By.id(v.substring(3));
        }
        if (v.startsWith("//") || v.startsWith("(//")) {
            return By.xpath(v);
        }
        log.debug("Selector without prefix treated as CSS: {}", v);
        return By.cssSelector(v);
    }
}
