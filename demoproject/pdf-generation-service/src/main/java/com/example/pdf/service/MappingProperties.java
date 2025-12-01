package com.example.pdf.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "mapping")
public class MappingProperties {

    /**
     * Ordered list of candidate patterns. Patterns may contain placeholders:
     * {template}, {product}, {market}, {state}
     */
    private List<String> candidateOrder;

    public List<String> getCandidateOrder() {
        return candidateOrder;
    }

    public void setCandidateOrder(List<String> candidateOrder) {
        this.candidateOrder = candidateOrder;
    }
}
