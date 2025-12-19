package com.example.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MappingServiceResolvePathTest {

    @Test
    public void resolveNestedObjects() {
        MappingService svc = new MappingService();
        Map<String, Object> payload = Map.of(
                "a", Map.of(
                        "b", Map.of(
                                "c", "val-c"
                        )
                )
        );

        Object res = svc.resolvePath(payload, "a.b.c");
        assertEquals("val-c", res);
    }

    @Test
    public void resolveNestedArray_dotIndex() {
        MappingService svc = new MappingService();
        Map<String, Object> payload = Map.of(
                "items", List.of(
                        Map.of("name", "n1"),
                        Map.of("name", "n2")
                )
        );

        Object res = svc.resolvePath(payload, "items.1.name");
        assertEquals("n2", res);
    }

    @Test
    public void resolveNestedArray_bracketIndex() {
        MappingService svc = new MappingService();
        Map<String, Object> payload = Map.of(
                "items", List.of(
                        Map.of("name", "n1"),
                        Map.of("name", "n2")
                )
        );

        Object res = svc.resolvePath(payload, "items[0].name");
        assertEquals("n1", res);
    }

    @Test
    public void resolveMultiDimensionalArray() {
        MappingService svc = new MappingService();
        Map<String, Object> payload = Map.of(
                "matrix", List.of(
                        List.of("x", "y")
                )
        );

        Object res1 = svc.resolvePath(payload, "matrix.0.1");
        assertEquals("y", res1);

        Object res2 = svc.resolvePath(payload, "matrix[0][1]");
        assertEquals("y", res2);
    }

    @Test
    public void resolvesToNullForOutOfRangeOrMissing() {
        MappingService svc = new MappingService();
        Map<String, Object> payload = Map.of(
                "items", List.of(
                        Map.of("name", "n1")
                )
        );

        // index out of range
        Object res = svc.resolvePath(payload, "items.5.name");
        assertNull(res);

        // missing key
        Object res2 = svc.resolvePath(payload, "items.0.notthere");
        assertNull(res2);
    }
}
