package com.querycanvas.api;

import com.querycanvas.service.QueryService;
import com.querycanvas.service.QueryService.QueryResponse;
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
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "querycanvas-server");
    }

    @PostMapping("/api/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Question is required.");
        }
        return queryService.runQuery(request.question());
    }

    public record QueryRequest(String question) {}
}
