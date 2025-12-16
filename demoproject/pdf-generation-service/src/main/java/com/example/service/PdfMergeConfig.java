package com.example.service;

import java.util.List;
import java.util.Map;

public class PdfMergeConfig {
    // Composition support
    private String base;
    private List<String> components;
    
    private String pageNumbering;
    private boolean addBookmarks;
    private boolean addTableOfContents;
    private List<SectionConfig> sections;
    private List<ConditionalSection> conditionalSections;
    private PageNumberingConfig pageNumberingConfig;
    private List<BookmarkConfig> bookmarks;
    private HeaderFooterConfig header;
    private HeaderFooterConfig footer;

    // Getters and setters for composition
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    
    public List<String> getComponents() { return components; }
    public void setComponents(List<String> components) { this.components = components; }

    // Getters and setters
    public String getPageNumbering() { return pageNumbering; }
    public void setPageNumbering(String pageNumbering) { this.pageNumbering = pageNumbering; }

    public boolean isAddBookmarks() { return addBookmarks; }
    public void setAddBookmarks(boolean addBookmarks) { this.addBookmarks = addBookmarks; }

    public boolean isAddTableOfContents() { return addTableOfContents; }
    public void setAddTableOfContents(boolean addTableOfContents) { this.addTableOfContents = addTableOfContents; }

    public List<SectionConfig> getSections() { return sections; }
    public void setSections(List<SectionConfig> sections) { this.sections = sections; }

    public List<ConditionalSection> getConditionalSections() { return conditionalSections; }
    public void setConditionalSections(List<ConditionalSection> conditionalSections) { 
        this.conditionalSections = conditionalSections; 
    }

    public PageNumberingConfig getPageNumberingConfig() { return pageNumberingConfig; }
    public void setPageNumberingConfig(PageNumberingConfig pageNumberingConfig) { 
        this.pageNumberingConfig = pageNumberingConfig; 
    }

    public List<BookmarkConfig> getBookmarks() { return bookmarks; }
    public void setBookmarks(List<BookmarkConfig> bookmarks) { this.bookmarks = bookmarks; }
    
    public HeaderFooterConfig getHeader() { return header; }
    public void setHeader(HeaderFooterConfig header) { this.header = header; }
    
    public HeaderFooterConfig getFooter() { return footer; }
    public void setFooter(HeaderFooterConfig footer) { this.footer = footer; }
}

class SectionConfig {
    private String name;
    private String type; // "freemarker", "pdfbox", or "acroform"
    private String template;
    private boolean enabled;
    private String insertAfter;
    private Map<String, String> fieldMapping; // For acroform: PDF field → payload path
    private List<String> payloadEnrichers; // Names of enrichers to apply before rendering

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getInsertAfter() { return insertAfter; }
    public void setInsertAfter(String insertAfter) { this.insertAfter = insertAfter; }
    
    public java.util.Map<String, String> getFieldMapping() { return fieldMapping; }
    public void setFieldMapping(Map<String, String> fieldMapping) { this.fieldMapping = fieldMapping; }
    
    public List<String> getPayloadEnrichers() { return payloadEnrichers; }
    public void setPayloadEnrichers(List<String> payloadEnrichers) { this.payloadEnrichers = payloadEnrichers; }
}

class ConditionalSection {
    private String condition;
    private List<SectionConfig> sections;

    // Getters and setters
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public List<SectionConfig> getSections() { return sections; }
    public void setSections(List<SectionConfig> sections) { this.sections = sections; }
}

class PageNumberingConfig {
    private int startPage;
    private String format;
    private String position;
    private String font;
    private int fontSize;

    // Getters and setters
    public int getStartPage() { return startPage; }
    public void setStartPage(int startPage) { this.startPage = startPage; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getFont() { return font; }
    public void setFont(String font) { this.font = font; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
}

class BookmarkConfig {
    private String section;
    private String title;
    private int level;

    // Getters and setters
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}

class HeaderFooterConfig {
    private boolean enabled;
    private int height;
    private int startPage;
    private ContentConfig content;
    private BorderConfig border;

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getStartPage() { return startPage; }
    public void setStartPage(int startPage) { this.startPage = startPage; }

    public ContentConfig getContent() { return content; }
    public void setContent(ContentConfig content) { this.content = content; }

    public BorderConfig getBorder() { return border; }
    public void setBorder(BorderConfig border) { this.border = border; }
}

class ContentConfig {
    private TextConfig left;
    private TextConfig center;
    private TextConfig right;

    // Getters and setters
    public TextConfig getLeft() { return left; }
    public void setLeft(TextConfig left) { this.left = left; }

    public TextConfig getCenter() { return center; }
    public void setCenter(TextConfig center) { this.center = center; }

    public TextConfig getRight() { return right; }
    public void setRight(TextConfig right) { this.right = right; }
}

class TextConfig {
    private String text;
    private String font;
    private int fontSize;

    // Getters and setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getFont() { return font; }
    public void setFont(String font) { this.font = font; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
}

class BorderConfig {
    private boolean enabled;
    private String color;
    private int thickness;

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getThickness() { return thickness; }
    public void setThickness(int thickness) { this.thickness = thickness; }
}
