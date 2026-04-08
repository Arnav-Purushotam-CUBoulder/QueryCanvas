export type SqlPlan = {
  sql: string;
  chart: { xKey: string; yKey: string; title: string } | null;
};

export function createSqlPlan(question: string): SqlPlan {
  const normalized = question.toLowerCase();

  if (normalized.includes("monthly") && normalized.includes("revenue")) {
    return {
      sql: `
SELECT substr(order_date, 1, 7) AS month, SUM(revenue) AS total_revenue
FROM orders
GROUP BY month
ORDER BY month;
      `.trim(),
      chart: { xKey: "month", yKey: "total_revenue", title: "Monthly revenue" }
    };
  }

  if (normalized.includes("region") && normalized.includes("revenue")) {
    return {
      sql: `
SELECT customers.region, SUM(orders.revenue) AS total_revenue
FROM orders
JOIN customers ON customers.id = orders.customer_id
GROUP BY customers.region
ORDER BY total_revenue DESC;
      `.trim(),
      chart: { xKey: "region", yKey: "total_revenue", title: "Revenue by region" }
    };
  }

  if (normalized.includes("top product") || normalized.includes("units sold")) {
    return {
      sql: `
SELECT products.name, SUM(orders.units) AS units_sold, SUM(orders.revenue) AS total_revenue
FROM orders
JOIN products ON products.id = orders.product_id
GROUP BY products.name
ORDER BY units_sold DESC;
      `.trim(),
      chart: { xKey: "name", yKey: "units_sold", title: "Top products by units sold" }
    };
  }

  if (normalized.includes("segment")) {
    return {
      sql: `
SELECT customers.segment, SUM(orders.revenue) AS total_revenue, COUNT(*) AS order_count
FROM orders
JOIN customers ON customers.id = orders.customer_id
GROUP BY customers.segment
ORDER BY total_revenue DESC;
      `.trim(),
      chart: { xKey: "segment", yKey: "total_revenue", title: "Revenue by customer segment" }
    };
  }

  return {
    sql: `
SELECT customers.name AS customer, customers.region, products.name AS product, orders.order_date, orders.revenue, orders.units
FROM orders
JOIN customers ON customers.id = orders.customer_id
JOIN products ON products.id = orders.product_id
ORDER BY orders.revenue DESC
LIMIT 10;
    `.trim(),
    chart: null
  };
}
