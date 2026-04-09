package com.querycanvas.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryService {
    private final JdbcTemplate jdbcTemplate;
    private final OpenAiInsightRefiner insightRefiner;

    public QueryService(JdbcTemplate jdbcTemplate, OpenAiInsightRefiner insightRefiner) {
        this.jdbcTemplate = jdbcTemplate;
        this.insightRefiner = insightRefiner;
    }

    @PostConstruct
    void seedDatabase() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS orders");
        jdbcTemplate.execute("DROP TABLE IF EXISTS products");
        jdbcTemplate.execute("DROP TABLE IF EXISTS customers");

        jdbcTemplate.execute("""
            CREATE TABLE customers (
              id INTEGER PRIMARY KEY,
              name VARCHAR(255) NOT NULL,
              segment VARCHAR(255) NOT NULL,
              region VARCHAR(255) NOT NULL
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE products (
              id INTEGER PRIMARY KEY,
              name VARCHAR(255) NOT NULL,
              category VARCHAR(255) NOT NULL
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE orders (
              id INTEGER PRIMARY KEY,
              customer_id INTEGER NOT NULL,
              product_id INTEGER NOT NULL,
              order_date VARCHAR(32) NOT NULL,
              revenue DOUBLE NOT NULL,
              units INTEGER NOT NULL
            )
            """);

        Object[][] customers = {
            {1, "Northwind Labs", "Enterprise", "North America"},
            {2, "BluePeak Retail", "Mid-Market", "Europe"},
            {3, "Atlas Freight", "Enterprise", "North America"},
            {4, "Nimbus Health", "SMB", "Asia"},
            {5, "Borealis Health", "Enterprise", "Europe"}
        };

        for (Object[] row : customers) {
            jdbcTemplate.update("INSERT INTO customers (id, name, segment, region) VALUES (?, ?, ?, ?)", row);
        }

        Object[][] products = {
            {1, "Analytics Cloud", "Software"},
            {2, "Workflow Copilot", "AI"},
            {3, "Insight API", "Platform"}
        };

        for (Object[] row : products) {
            jdbcTemplate.update("INSERT INTO products (id, name, category) VALUES (?, ?, ?)", row);
        }

        Object[][] orders = {
            {1, 1, 1, "2025-01-15", 24000, 3},
            {2, 1, 2, "2025-02-10", 18000, 2},
            {3, 2, 1, "2025-02-18", 12000, 2},
            {4, 3, 3, "2025-03-02", 31000, 4},
            {5, 4, 2, "2025-03-11", 7000, 1},
            {6, 5, 1, "2025-04-06", 28000, 3},
            {7, 2, 3, "2025-05-03", 16000, 2},
            {8, 3, 2, "2025-06-20", 22000, 2},
            {9, 5, 3, "2025-07-14", 19500, 2},
            {10, 4, 1, "2025-08-04", 9000, 1},
            {11, 1, 3, "2025-09-17", 26000, 3},
            {12, 3, 1, "2025-10-09", 33000, 4}
        };

        for (Object[] row : orders) {
            jdbcTemplate.update(
                "INSERT INTO orders (id, customer_id, product_id, order_date, revenue, units) VALUES (?, ?, ?, ?, ?, ?)",
                row
            );
        }
    }

    public QueryResponse runQuery(String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isBlank()) {
            throw new IllegalArgumentException("Question is required.");
        }

        SqlPlan plan = createSqlPlan(question);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(plan.sql());
        String fallbackInsight = buildInsight(question, rows);
        String aiInsight = insightRefiner.refineInsight(question, plan.sql(), rows);

        return new QueryResponse(
            question,
            plan.sql(),
            aiInsight != null ? aiInsight : fallbackInsight,
            rows,
            plan.chart()
        );
    }

    private SqlPlan createSqlPlan(String question) {
        String normalized = question.toLowerCase();

        if (normalized.contains("monthly") && normalized.contains("revenue")) {
            return new SqlPlan(
                """
                SELECT SUBSTRING(order_date, 1, 7) AS month, SUM(revenue) AS total_revenue
                FROM orders
                GROUP BY SUBSTRING(order_date, 1, 7)
                ORDER BY month
                """,
                new ChartConfig("month", "total_revenue", "Monthly revenue")
            );
        }

        if (normalized.contains("region") && normalized.contains("revenue")) {
            return new SqlPlan(
                """
                SELECT customers.region, SUM(orders.revenue) AS total_revenue
                FROM orders
                JOIN customers ON customers.id = orders.customer_id
                GROUP BY customers.region
                ORDER BY total_revenue DESC
                """,
                new ChartConfig("region", "total_revenue", "Revenue by region")
            );
        }

        if (normalized.contains("top product") || normalized.contains("units sold")) {
            return new SqlPlan(
                """
                SELECT products.name, SUM(orders.units) AS units_sold, SUM(orders.revenue) AS total_revenue
                FROM orders
                JOIN products ON products.id = orders.product_id
                GROUP BY products.name
                ORDER BY units_sold DESC
                """,
                new ChartConfig("name", "units_sold", "Top products by units sold")
            );
        }

        if (normalized.contains("segment")) {
            return new SqlPlan(
                """
                SELECT customers.segment, SUM(orders.revenue) AS total_revenue, COUNT(*) AS order_count
                FROM orders
                JOIN customers ON customers.id = orders.customer_id
                GROUP BY customers.segment
                ORDER BY total_revenue DESC
                """,
                new ChartConfig("segment", "total_revenue", "Revenue by customer segment")
            );
        }

        return new SqlPlan(
            """
            SELECT customers.name AS customer, customers.region, products.name AS product, orders.order_date, orders.revenue, orders.units
            FROM orders
            JOIN customers ON customers.id = orders.customer_id
            JOIN products ON products.id = orders.product_id
            ORDER BY orders.revenue DESC
            LIMIT 10
            """,
            null
        );
    }

    private String buildInsight(String question, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No rows were returned for: " + question;
        }

        Map<String, Object> first = rows.get(0);
        String descriptors = first.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + ", " + right)
            .orElse("no values");

        return "The query returned %d rows. The leading result is %s.".formatted(rows.size(), descriptors);
    }

    public record QueryResponse(
        String question,
        String sql,
        String insight,
        List<Map<String, Object>> rows,
        ChartConfig chart
    ) {}

    public record ChartConfig(String xKey, String yKey, String title) {}

    private record SqlPlan(String sql, ChartConfig chart) {}
}
