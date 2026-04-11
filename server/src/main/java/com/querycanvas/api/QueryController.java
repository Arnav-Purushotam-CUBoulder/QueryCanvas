package com.querycanvas.api;

import com.querycanvas.service.QueryService;
import com.querycanvas.service.QueryService.QueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class QueryController {
    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/health")
    @Operation(summary = "Check API health", description = "Returns a simple readiness payload for the Spring Boot service.")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "querycanvas-server");
    }

    @PostMapping("/api/query")
    @Operation(summary = "Run a natural-language analytics query", description = "Maps a business question to a curated SQL plan and returns rows, SQL, and chart metadata.")
    public QueryResponse query(@RequestBody QueryRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Question is required.");
        }
        return queryService.runQuery(request.question());
    }

    public record QueryRequest(String question) {}
}
