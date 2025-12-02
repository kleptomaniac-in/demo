package com.example.pdf.service;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

@Service
public class FreemarkerService {

    private static final Logger log = LoggerFactory.getLogger(FreemarkerService.class);
    private final Configuration cfg;

    public FreemarkerService() throws IOException {
        cfg = new Configuration(new Version(2, 3, 32));
        cfg.setDefaultEncoding("UTF-8");

        // Classpath loader (templates packaged in resources)
        ClassTemplateLoader classLoader = new ClassTemplateLoader(this.getClass(), "/");

        // File system loader (relative to working directory)
        FileTemplateLoader fileLoader = new FileTemplateLoader(new java.io.File("."));

        // URL loader for http/https resources
        URLTemplateLoader urlLoader = new URLTemplateLoader() {
            @Override
            protected java.net.URL getURL(String name) {
                try {
                    if (name.startsWith("http://") || name.startsWith("https://")) {
                        return new java.net.URL(name);
                    }
                } catch (Exception e) {
                    log.debug("Invalid URL for template loader: {}", name, e);
                }
                return null;
            }
        };

        TemplateLoader[] loaders = new TemplateLoader[] { classLoader, fileLoader, urlLoader };
        MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
        cfg.setTemplateLoader(mtl);

        // Cache templates for 5 seconds (adjust as needed); 0 means always check
        cfg.setTemplateUpdateDelayMilliseconds(5000);
    }

    /**
     * Process a template given its content string.
     */
    public String processTemplate(String templateContent, Map<String, Object> model) throws IOException {
        if (templateContent == null) templateContent = "";
        StringTemplateLoader loader = new StringTemplateLoader();
        String name = "inline-template";
        loader.putTemplate(name, templateContent);
        Configuration tmpCfg = new Configuration(cfg.getIncompatibleImprovements());
        tmpCfg.setTemplateLoader(loader);
        tmpCfg.setDefaultEncoding("UTF-8");
        tmpCfg.setTemplateUpdateDelayMilliseconds(cfg.getTemplateUpdateDelayMilliseconds());

        try (StringWriter out = new StringWriter()) {
            Template t = tmpCfg.getTemplate(name);
            t.process(model, out);
            return out.toString();
        } catch (TemplateException te) {
            log.error("Freemarker processing failed", te);
            throw new IOException("Freemarker processing failed: " + te.getMessage(), te);
        }
    }

    /**
     * Process a template by location/name. Supported forms:
     * - classpath: "/templates/foo.ftl" or "templates/foo.ftl"
     * - relative file: "templates/foo.ftl"
     * - absolute or http URL: "http://.../foo.ftl" or "/absolute/path.ftl" (fallback to reading file)
     */
    public String processTemplateFromLocation(String location, Map<String, Object> model) throws IOException {
        if (location == null) throw new IOException("Template location is null");
        String name = location.trim();
        // If leading slash, drop it for classpath/file lookups
        if (name.startsWith("/")) name = name.substring(1);

        try (StringWriter out = new StringWriter()) {
            try {
                Template t = cfg.getTemplate(name);
                t.process(model, out);
                return out.toString();
            } catch (freemarker.template.TemplateNotFoundException tnfe) {
                // As a fallback, if the original location looks like an absolute path, try to read file directly
                java.io.File f = new java.io.File(location);
                if (f.exists() && f.isFile()) {
                    String content = java.nio.file.Files.readString(f.toPath());
                    return processTemplate(content, model);
                }
                throw new IOException("Template not found: " + location, tnfe);
            } catch (TemplateException te) {
                log.error("Freemarker processing failed for template {}", location, te);
                throw new IOException("Freemarker processing failed: " + te.getMessage(), te);
            }
        }
    }
}
