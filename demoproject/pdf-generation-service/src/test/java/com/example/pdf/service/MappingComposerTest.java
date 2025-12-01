package com.example.pdf.service;

import com.example.pdf.controller.GenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MappingComposerTest {

    // Helper test client that records calls and returns simple fragments
    static class TestClient extends ConfigServerClient {
        public final List<String> calls = new ArrayList<>();
        public final Map<String, Optional<Map<String,Object>>> responses = new HashMap<>();

        public TestClient() {
            super(null, null);
        }

        @Override
        public Optional<Map<String, Object>> getFileSource(String profile, String label, String pathWithExtension) {
            String key = String.format("file:%s:%s:%s", profile, label, pathWithExtension);
            calls.add(key);
            return responses.getOrDefault(key, Optional.empty());
        }

        @Override
        public Optional<Map<String, Object>> getApplicationSource(String application, String profile, String label) {
            String key = String.format("app:%s:%s:%s", application, profile, label);
            calls.add(key);
            return responses.getOrDefault(key, Optional.empty());
        }
    }

    private GenerateRequest makeReq() {
        GenerateRequest r = new GenerateRequest();
        r.setTemplateName("t");
        r.setClientService("c");
        return r;
    }

    @Test
    public void dispatchesToFileAndAppSources() {
        TestClient client = new TestClient();
        // Prepare responses
        Map<String,Object> fileMap = Map.of("foo.bar", "1");
        Map<String,Object> appMap = Map.of("app.key", "v");

        client.responses.put("file:default:main:mappings/foo.yml", Optional.of(fileMap));
        client.responses.put("app:myapp:default:main", Optional.of(appMap));

        MappingComposer composer = new MappingComposer(client);

        List<String> candidates = List.of("file:mappings/foo", "app:myapp", "mappings/baz", "literalapp");

        // ensure responses for mappings/baz and literalapp are present
        client.responses.put("file:default:main:mappings/baz.yml", Optional.of(Map.of("baz.k","x")));
        client.responses.put("app:literalapp:default:main", Optional.of(Map.of("lit.k","y")));

        composer.compose(makeReq(), "main", candidates);

        // expected call order: file:mappings/foo -> app:myapp -> file:mappings/baz -> app:literalapp
        List<String> expected = List.of(
                "file:default:main:mappings/foo.yml",
                "app:myapp:default:main",
                "file:default:main:mappings/baz.yml",
                "app:literalapp:default:main"
        );

        assertEquals(expected, client.calls);
    }

    @Test
    public void doesNotDoubleAppendYml() {
        TestClient client = new TestClient();
        MappingComposer composer = new MappingComposer(client);

        // Provide same fragment for both variants
        client.responses.put("file:default:main:mappings/foo.yml", Optional.of(Map.of("a.b","1")));

        List<String> candidates = List.of("mappings/foo.yml", "mappings/foo");
        composer.compose(makeReq(), "main", candidates);

        // Ensure no double-suffixed path is requested
        boolean hasDouble = client.calls.stream().anyMatch(s -> s.contains(".yml.yml"));
        assertFalse(hasDouble, "Should not request paths with double .yml suffix");

        // Ensure both candidates resolved to the same single .yml call (first candidate will call, second will use cache)
        long fileCalls = client.calls.stream().filter(s -> s.startsWith("file:")).count();
        assertEquals(1, fileCalls);
    }

    @Test
    public void perComposeCacheAvoidsDuplicateCalls() {
        TestClient client = new TestClient();
        MappingComposer composer = new MappingComposer(client);

        client.responses.put("file:default:main:mappings/dup.yml", Optional.of(Map.of("k","v")));

        List<String> candidates = List.of("file:mappings/dup", "file:mappings/dup", "mappings/other");
        client.responses.put("file:default:main:mappings/other.yml", Optional.of(Map.of("o","1")));

        composer.compose(makeReq(), "main", candidates);

        // Expect only two file calls: one for dup, one for other
        long fileCalls = client.calls.stream().filter(s -> s.startsWith("file:")).count();
        assertEquals(2, fileCalls);

        // ensure dup was only called once
        long dupCalls = client.calls.stream().filter(s -> s.contains("mappings/dup.yml")).count();
        assertEquals(1, dupCalls);
    }
}
