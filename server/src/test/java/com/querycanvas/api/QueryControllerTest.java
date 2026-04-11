package com.querycanvas.api;

import com.querycanvas.service.QueryService;
import com.querycanvas.service.QueryService.ChartConfig;
import com.querycanvas.service.QueryService.QueryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
class QueryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryService queryService;

    @Test
    void healthReturnsServiceMetadata() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.service").value("querycanvas-server"));
    }

    @Test
    void queryRejectsBlankQuestions() throws Exception {
        mockMvc.perform(post("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"question":"   "}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void queryReturnsSqlRowsAndChartMetadata() throws Exception {
        when(queryService.runQuery("Show revenue by region")).thenReturn(new QueryResponse(
            "Show revenue by region",
            "SELECT region, SUM(revenue) AS total_revenue FROM orders GROUP BY region",
            "North America leads total revenue.",
            List.of(Map.of("region", "North America", "total_revenue", 128000)),
            new ChartConfig("region", "total_revenue", "Revenue by region")
        ));

        mockMvc.perform(post("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"question":"Show revenue by region"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sql").value("SELECT region, SUM(revenue) AS total_revenue FROM orders GROUP BY region"))
            .andExpect(jsonPath("$.rows[0].region").value("North America"))
            .andExpect(jsonPath("$.chart.title").value("Revenue by region"));

        verify(queryService).runQuery("Show revenue by region");
    }
}
