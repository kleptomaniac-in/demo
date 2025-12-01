package com.example.pdf.controller;

import com.example.pdf.service.MappingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal")
public class MappingDebugController {

    private final MappingService mappingService;

    public MappingDebugController(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    /**
     * Return the active configured candidate-order (patterns as configured remotely).
     */
    @GetMapping("/mapping-order")
    public List<String> mappingOrder() {
        return mappingService.getConfiguredCandidateOrder();
    }
}
