package com.example.service;

import com.example.config.CacheWarmingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service that preloads frequently-used configs and templates into cache at application startup.
 * This eliminates "cold start" penalty on first requests.
 */
@Service
@ConditionalOnProperty(name = "app.cache-warming.enabled", havingValue = "true", matchIfMissing = false)
public class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    @Autowired
    private CacheWarmingProperties properties;

    @Autowired
    private PdfMergeConfigService configService;

    @Autowired
    private AcroFormFillService acroFormService;

    @Autowired
    private FreemarkerService freemarkerService;

    @Value("${config.repo.path:../config-repo}")
    private String configRepoPath;

    /**
     * Triggered when application is fully started and ready to serve requests
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCaches() {
        if (!properties.isEnabled()) {
            log.info("Cache warming is disabled");
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("=== Cache Warming Started ===");

        try {
            // Step 1: Preload YAML components (if enabled)
            int componentsLoaded = 0;
            if (properties.isPreloadComponents()) {
                componentsLoaded = preloadYamlComponents();
            }

            // Step 2: Preload config files
            int configsLoaded = preloadConfigs();

            // Step 3: Preload AcroForm templates
            int acroformsLoaded = preloadAcroFormTemplates();

            // Step 4: Preload FreeMarker templates
            int freemarkerLoaded = preloadFreeMarkerTemplates();

            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Cache Warming Completed in {}ms ===", duration);
            log.info("  - Configs preloaded: {}", configsLoaded);
            log.info("  - AcroForm templates preloaded: {}", acroformsLoaded);
            log.info("  - FreeMarker templates preloaded: {}", freemarkerLoaded);
            if (properties.isPreloadComponents()) {
                log.info("  - Component files preloaded: {}", componentsLoaded);
            }
            log.info("  - Application is now production-ready with warm caches");

        } catch (Exception e) {
            log.error("Cache warming failed", e);
            // Don't fail application startup - continue with cold caches
        }
    }

    /**
     * Preload YAML component files (shared across multiple configs)
     */
    private int preloadYamlComponents() {
        log.info("Preloading YAML components...");
        
        List<String> componentFiles = new ArrayList<>();
        
        // Discover component files in common directories
        String[] componentDirs = {
            "templates/components",
            "templates/markets",
            "components",
            "shared"
        };
        
        for (String dir : componentDirs) {
            Path dirPath = Paths.get(configRepoPath, dir);
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try {
                    List<String> files = findYamlFiles(dirPath);
                    componentFiles.addAll(files);
                } catch (IOException e) {
                    log.warn("Failed to scan component directory {}: {}", dir, e.getMessage());
                }
            }
        }
        
        log.info("Discovered {} component files", componentFiles.size());
        
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(properties.getParallelThreads());
        
        for (String file : componentFiles) {
            executor.submit(() -> {
                try {
                    // This will cache the component file
                    configService.loadYamlFile(file);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("  ✗ Failed to load component {}: {}", file, e.getMessage());
                }
            });
        }
        
        shutdownExecutor(executor);
        
        int loaded = successCount.get();
        log.info("Preloaded {} component files", loaded);
        return loaded;
    }

    /**
     * Preload PDF merge configuration files
     */
    private int preloadConfigs() {
        String patterns = properties.getConfigPatterns();
        log.info("Discovering config files with patterns: {}", patterns);
        
        List<String> configFiles = discoverFiles(configRepoPath, patterns, ".yml");
        log.info("Discovered {} config files matching patterns", configFiles.size());
        
        if (configFiles.isEmpty()) {
            return 0;
        }
        
        log.info("Preloading {} configs with {} threads...", configFiles.size(), properties.getParallelThreads());
        
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(properties.getParallelThreads());
        
        for (String configFile : configFiles) {
            executor.submit(() -> {
                try {
                    // Remove .yml extension for config name
                    String configName = configFile.replace(".yml", "");
                    configService.loadConfig(configName);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("  ✗ Failed to load config {}: {}", configFile, e.getMessage());
                }
            });
        }
        
        shutdownExecutor(executor);
        
        int loaded = successCount.get();
        log.info("Preloaded {} configs", loaded);
        return loaded;
    }

    /**
     * Preload AcroForm PDF templates
     */
    private int preloadAcroFormTemplates() {
        String patterns = properties.getAcroformPatterns();
        log.info("Discovering AcroForm template files with patterns: {}", patterns);
        
        String acroformsPath = configRepoPath + "/acroforms";
        List<String> templateFiles = discoverFiles(acroformsPath, patterns, ".pdf");
        log.info("Discovered {} AcroForm template files matching patterns", templateFiles.size());
        
        if (templateFiles.isEmpty()) {
            return 0;
        }
        
        log.info("Preloading {} AcroForm templates with {} threads...", templateFiles.size(), properties.getParallelThreads());
        
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(properties.getParallelThreads());
        
        for (String templateFile : templateFiles) {
            executor.submit(() -> {
                try {
                    // This will cache the template bytes
                    acroFormService.loadTemplateBytes(templateFile);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("  ✗ Failed to load AcroForm template {}: {}", templateFile, e.getMessage());
                }
            });
        }
        
        shutdownExecutor(executor);
        
        int loaded = successCount.get();
        log.info("Preloaded {} AcroForm templates", loaded);
        return loaded;
    }

    /**
     * Preload FreeMarker templates
     */
    private int preloadFreeMarkerTemplates() {
        String patterns = properties.getFreemarkerPatterns();
        log.info("Discovering FreeMarker template files with patterns: {}", patterns);
        
        // Scan both config-repo and classpath templates
        List<String> templateFiles = new ArrayList<>();
        
        // Scan config-repo templates
        String templatesPath = configRepoPath + "/templates";
        templateFiles.addAll(discoverFiles(templatesPath, patterns, ".ftl"));
        
        // Scan classpath templates (src/main/resources/templates)
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            Path resourceTemplates = Paths.get(classLoader.getResource("templates").toURI());
            if (Files.exists(resourceTemplates)) {
                templateFiles.addAll(discoverFiles(resourceTemplates.toString(), patterns, ".ftl"));
            }
        } catch (Exception e) {
            log.debug("Could not scan classpath templates: {}", e.getMessage());
        }
        
        log.info("Discovered {} FreeMarker template files matching patterns", templateFiles.size());
        
        if (templateFiles.isEmpty()) {
            return 0;
        }
        
        log.info("Preloading {} FreeMarker templates with {} threads...", templateFiles.size(), properties.getParallelThreads());
        
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(properties.getParallelThreads());
        
        for (String templateFile : templateFiles) {
            executor.submit(() -> {
                try {
                    // Convert absolute path to relative path for FreeMarker
                    String relativePath = "templates/" + templateFile;
                    freemarkerService.preloadTemplate(relativePath);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("  ✗ Failed to load FreeMarker template {}: {}", templateFile, e.getMessage());
                }
            });
        }
        
        shutdownExecutor(executor);
        
        int loaded = successCount.get();
        log.info("Preloaded {} FreeMarker templates", loaded);
        return loaded;
    }

    /**
     * Discover files matching glob patterns in a directory
     * 
     * @param basePath Base directory to search in
     * @param patterns Comma-separated glob patterns (e.g., "dental-*.yml,medical-*.yml")
     * @param extension File extension to filter by (e.g., ".yml", ".pdf")
     * @return List of matching file names (not full paths)
     */
    private List<String> discoverFiles(String basePath, String patterns, String extension) {
        List<String> result = new ArrayList<>();
        Path baseDir = Paths.get(basePath);
        
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            log.warn("Directory does not exist: {}", basePath);
            return result;
        }
        
        // Parse patterns
        String[] patternArray = patterns.split(",");
        List<Pattern> regexPatterns = new ArrayList<>();
        
        for (String pattern : patternArray) {
            pattern = pattern.trim();
            if (pattern.isEmpty()) continue;
            
            // Convert glob pattern to regex
            String regex = globToRegex(pattern, extension);
            regexPatterns.add(Pattern.compile(regex));
        }
        
        // Scan directory for matching files
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    String fileName = entry.getFileName().toString();
                    
                    // Check if file matches any pattern
                    for (Pattern pattern : regexPatterns) {
                        if (pattern.matcher(fileName).matches()) {
                            result.add(fileName);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan directory {}: {}", basePath, e.getMessage());
        }
        
        return result;
    }

    /**
     * Find all YAML files in a directory recursively
     */
    private List<String> findYamlFiles(Path directory) throws IOException {
        List<String> files = new ArrayList<>();
        
        if (!Files.exists(directory)) {
            return files;
        }
        
        try (var stream = Files.walk(directory)) {
            files = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                .map(p -> {
                    // Get relative path from configRepoPath
                    Path configRepo = Paths.get(configRepoPath);
                    return configRepo.relativize(p).toString();
                })
                .collect(Collectors.toList());
        }
        
        return files;
    }

    /**
     * Convert glob pattern to regex
     * 
     * @param glob Glob pattern (e.g., "dental-*.yml")
     * @param extension Expected file extension (e.g., ".yml")
     * @return Regex pattern
     */
    private String globToRegex(String glob, String extension) {
        // Ensure glob has the expected extension
        if (!glob.endsWith(extension) && !glob.endsWith("*")) {
            glob = glob + extension;
        }
        
        // Escape special regex characters except * and ?
        String regex = glob
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        
        return "^" + regex + "$";
    }

    /**
     * Shutdown executor and wait for tasks to complete
     */
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
