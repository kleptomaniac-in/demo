package com.example.service;

import java.util.Map;

/**
 * Configuration for MappingPreprocessor. All fields have sensible defaults
 * to preserve existing behavior; callers may override to adapt to different
 * payload shapes.
 */
public class PreprocessorConfig {

    public String rootAppKey = "app";
    public String applicantsKey = "applicants";
    public String applicantTypeField = "type";
    // Leave type values null by default to avoid embedding domain-specific
    // strings in the library. Callers should supply these when they want
    // role-based selection behavior.
    public String primaryTypeValue = null;
    public String spouseTypeValue = null;
    public String childTypeValue = null;

    // Output keys are left null by default so the preprocessor does not
    // impose any domain-specific output naming. Callers should set these
    // if they want normalized outputs produced.
    public String primaryOutputKey = null;
    public String spouseOutputKey = null;
    public String childSlotPrefix = "child";
    public int childSlotCount = 3;

    public String addressListKey = "addresses";
    public String addressTypeField = "addressType";
    public String homeAddressValue = "HOME";
    public String billingAddressValue = "BILLING";

    public String priorCoveragesKey = "priorCoverages";
    public String coverageTypeField = "coverageType";
        // mapping from coverageType value -> output key name; empty by default
        public Map<String,String> coverageOutputMap = Map.of();

        // output keys for addresses
        public String homeAddressOutputKey = null;
        public String billingAddressOutputKey = null;

        // Optional declarative selectors expressed as JSONPath. If provided,
        // selectors will be used to find applicants and other elements. Keys are
        // arbitrary logical names (e.g. "primary", "spouse", "children").
        // Example: selectors.put("primary", "$.app.applicants[?(@.type=='Primary')][0]")
        public Map<String, String> selectors = Map.of();

    public String productsKey = "products";
    public String productTypeField = "type"; // fallback to productType in code

    public PreprocessorConfig() {}
}
