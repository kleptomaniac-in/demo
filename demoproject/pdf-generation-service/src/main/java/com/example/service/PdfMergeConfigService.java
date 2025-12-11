package com.example.service;

import com.example.pdf.service.ConfigServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class PdfMergeConfigService {


    @Autowired(required = false)
    private ConfigServerClient configServerClient;
    
    @Value("${config.repo.path:../config-repo}")
    private String configRepoPath;

    public PdfMergeConfig loadConfig(String configName) {
        try {
            String yamlContent;
            
            // Try to load from config server first (if available)
            if (configServerClient != null) {
                System.out.println("Attempting to load config from config server: " + configName);
                Optional<Map<String, Object>> configOpt = configServerClient.getFileSource(
                    "default", "master", configName
                );
                if (configOpt.isPresent()) {
                    System.out.println("Config loaded from config server: " + configName);
                    Yaml yaml = new Yaml();
                    return parsePdfMergeConfig(configOpt.get());
                }
            }

            System.out.println("Config server not available or config not found, falling back to file system.");
            
            // Fallback to loading from file system
            String configPath = configRepoPath + "/" + configName;
            if (!Files.exists(Paths.get(configPath))) {
                // Try current working directory
                configPath = configName;
            }
            
            try (InputStream inputStream = new FileInputStream(configPath)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(inputStream);
                return parsePdfMergeConfig(data);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load PDF merge config: " + configName, e);
        }
    }
    
    private PdfMergeConfig parsePdfMergeConfig(Map<String, Object> data) {
        Map<String, Object> pdfMerge = (Map<String, Object>) data.get("pdfMerge");
        
        PdfMergeConfig config = new PdfMergeConfig();
        
        // Parse settings
        if (pdfMerge.containsKey("settings")) {
            Map<String, Object> settings = (Map<String, Object>) pdfMerge.get("settings");
            config.setPageNumbering((String) settings.get("pageNumbering"));
            config.setAddBookmarks((Boolean) settings.getOrDefault("addBookmarks", false));
            config.setAddTableOfContents((Boolean) settings.getOrDefault("addTableOfContents", false));
        }
        
        // Parse sections
        if (pdfMerge.containsKey("sections")) {
            List<Map<String, Object>> sections = (List<Map<String, Object>>) pdfMerge.get("sections");
            List<SectionConfig> sectionConfigs = new ArrayList<>();
            
            for (Map<String, Object> section : sections) {
                SectionConfig sectionConfig = new SectionConfig();
                sectionConfig.setName((String) section.get("name"));
                sectionConfig.setType((String) section.get("type"));
                sectionConfig.setTemplate((String) section.get("template"));
                sectionConfig.setEnabled((Boolean) section.getOrDefault("enabled", true));
                sectionConfigs.add(sectionConfig);
            }
            
            config.setSections(sectionConfigs);
        }
        
        // Parse conditional sections
        if (pdfMerge.containsKey("conditionalSections")) {
            List<Map<String, Object>> conditionals = (List<Map<String, Object>>) pdfMerge.get("conditionalSections");
            List<ConditionalSection> conditionalConfigs = new ArrayList<>();
            
            for (Map<String, Object> conditional : conditionals) {
                ConditionalSection condConfig = new ConditionalSection();
                condConfig.setCondition((String) conditional.get("condition"));
                
                List<Map<String, Object>> sections = (List<Map<String, Object>>) conditional.get("sections");
                List<SectionConfig> sectionConfigs = new ArrayList<>();
                
                for (Map<String, Object> section : sections) {
                    SectionConfig sectionConfig = new SectionConfig();
                    sectionConfig.setName((String) section.get("name"));
                    sectionConfig.setType((String) section.get("type"));
                    sectionConfig.setTemplate((String) section.get("template"));
                    sectionConfig.setInsertAfter((String) section.get("insertAfter"));
                    sectionConfigs.add(sectionConfig);
                }
                
                condConfig.setSections(sectionConfigs);
                conditionalConfigs.add(condConfig);
            }
            
            config.setConditionalSections(conditionalConfigs);
        }
        
        // Parse page numbering config
        if (pdfMerge.containsKey("pageNumbering")) {
            Map<String, Object> pageNum = (Map<String, Object>) pdfMerge.get("pageNumbering");
            PageNumberingConfig pnConfig = new PageNumberingConfig();
            pnConfig.setStartPage((Integer) pageNum.getOrDefault("startPage", 1));
            pnConfig.setFormat((String) pageNum.getOrDefault("format", "Page {current}"));
            pnConfig.setPosition((String) pageNum.getOrDefault("position", "bottom-center"));
            pnConfig.setFont((String) pageNum.getOrDefault("font", "Helvetica"));
            pnConfig.setFontSize((Integer) pageNum.getOrDefault("fontSize", 10));
            config.setPageNumberingConfig(pnConfig);
        }
        
        // Parse bookmarks
        if (pdfMerge.containsKey("bookmarks")) {
            List<Map<String, Object>> bookmarks = (List<Map<String, Object>>) pdfMerge.get("bookmarks");
            List<BookmarkConfig> bookmarkConfigs = new ArrayList<>();
            
            for (Map<String, Object> bookmark : bookmarks) {
                BookmarkConfig bmConfig = new BookmarkConfig();
                bmConfig.setSection((String) bookmark.get("section"));
                bmConfig.setTitle((String) bookmark.get("title"));
                bmConfig.setLevel((Integer) bookmark.getOrDefault("level", 1));
                bookmarkConfigs.add(bmConfig);
            }
            
            config.setBookmarks(bookmarkConfigs);
        }
        
        // Parse header
        if (pdfMerge.containsKey("header")) {
            config.setHeader(parseHeaderFooterConfig((Map<String, Object>) pdfMerge.get("header")));
        }
        
        // Parse footer
        if (pdfMerge.containsKey("footer")) {
            config.setFooter(parseHeaderFooterConfig((Map<String, Object>) pdfMerge.get("footer")));
        }
        
        return config;
    }
    
    private HeaderFooterConfig parseHeaderFooterConfig(Map<String, Object> hfMap) {
        HeaderFooterConfig hfConfig = new HeaderFooterConfig();
        hfConfig.setEnabled((Boolean) hfMap.getOrDefault("enabled", false));
        hfConfig.setHeight((Integer) hfMap.getOrDefault("height", 40));
        hfConfig.setStartPage((Integer) hfMap.getOrDefault("startPage", 1));
        
        if (hfMap.containsKey("content")) {
            Map<String, Object> content = (Map<String, Object>) hfMap.get("content");
            ContentConfig contentConfig = new ContentConfig();
            
            if (content.containsKey("left")) {
                contentConfig.setLeft(parseTextConfig((Map<String, Object>) content.get("left")));
            }
            if (content.containsKey("center")) {
                contentConfig.setCenter(parseTextConfig((Map<String, Object>) content.get("center")));
            }
            if (content.containsKey("right")) {
                contentConfig.setRight(parseTextConfig((Map<String, Object>) content.get("right")));
            }
            
            hfConfig.setContent(contentConfig);
        }
        
        if (hfMap.containsKey("border")) {
            Map<String, Object> border = (Map<String, Object>) hfMap.get("border");
            BorderConfig borderConfig = new BorderConfig();
            borderConfig.setEnabled((Boolean) border.getOrDefault("enabled", false));
            borderConfig.setColor((String) border.getOrDefault("color", "#000000"));
            borderConfig.setThickness((Integer) border.getOrDefault("thickness", 1));
            hfConfig.setBorder(borderConfig);
        }
        
        return hfConfig;
    }
    
    private TextConfig parseTextConfig(Map<String, Object> textMap) {
        TextConfig textConfig = new TextConfig();
        textConfig.setText((String) textMap.get("text"));
        textConfig.setFont((String) textMap.getOrDefault("font", "Helvetica"));
        textConfig.setFontSize((Integer) textMap.getOrDefault("fontSize", 10));
        return textConfig;
    }
}
