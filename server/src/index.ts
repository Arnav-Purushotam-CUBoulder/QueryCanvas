import cors from "cors";
import Database from "better-sqlite3";
import express from "express";
import { customers, orders, products } from "./seed.js";
import { createSqlPlan } from "./sql.js";
import { refineInsight } from "./openai.js";

const app = express();
const port = Number(process.env.PORT ?? 8787);

app.use(cors());
app.use(express.json());

const db = new Database(":memory:");
seedDatabase();

app.get("/health", (_request, response) => {
  response.json({ status: "ok", service: "querycanvas-server" });
});

app.post("/api/query", async (request, response) => {
  const question = String(request.body?.question ?? "").trim();
  if (!question) {
    response.status(400).json({ error: "Question is required." });
    return;
  }

  const plan = createSqlPlan(question);
  const rows = db.prepare(plan.sql).all() as Record<string, string | number | null>[];
  const fallbackInsight = buildInsight(question, rows);
  const aiInsight = await refineInsight(question, plan.sql, rows);

  response.json({
    question,
    sql: plan.sql,
    insight: aiInsight ?? fallbackInsight,
    rows,
    chart: plan.chart
  });
});

app.listen(port, () => {
  console.log(`QueryCanvas server listening on http://localhost:${port}`);
});

function seedDatabase() {
  db.exec(`
    CREATE TABLE customers (
      id INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      segment TEXT NOT NULL,
      region TEXT NOT NULL
    );

    CREATE TABLE products (
      id INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      category TEXT NOT NULL
    );

    CREATE TABLE orders (
      id INTEGER PRIMARY KEY,
      customer_id INTEGER NOT NULL,
      product_id INTEGER NOT NULL,
      order_date TEXT NOT NULL,
      revenue REAL NOT NULL,
      units INTEGER NOT NULL
    );
  `);

  const insertCustomer = db.prepare("INSERT INTO customers (id, name, segment, region) VALUES (?, ?, ?, ?)");
  for (const row of customers) insertCustomer.run(...row);

  const insertProduct = db.prepare("INSERT INTO products (id, name, category) VALUES (?, ?, ?)");
  for (const row of products) insertProduct.run(...row);

  const insertOrder = db.prepare("INSERT INTO orders (id, customer_id, product_id, order_date, revenue, units) VALUES (?, ?, ?, ?, ?, ?)");
  for (const row of orders) insertOrder.run(...row);
}

function buildInsight(question: string, rows: Record<string, string | number | null>[]) {
  if (rows.length === 0) {
    return `No rows were returned for: ${question}`;
  }

  const first = rows[0];
  const descriptors = Object.entries(first)
    .map(([key, value]) => `${key}=${value}`)
    .join(", ");

  return `The query returned ${rows.length} rows. The leading result is ${descriptors}.`;
}
