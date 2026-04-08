import { useState } from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ResultsTable } from "./components/ResultsTable";

type QueryResponse = {
  question: string;
  sql: string;
  insight: string;
  rows: Record<string, string | number | null>[];
  chart: {
    xKey: string;
    yKey: string;
    title: string;
  } | null;
};

const starterQuestions = [
  "Show monthly revenue for 2025",
  "Compare revenue by region",
  "What are the top products by units sold?"
];

export default function App() {
  const [question, setQuestion] = useState(starterQuestions[0]);
  const [data, setData] = useState<QueryResponse | null>(null);
  const [loading, setLoading] = useState(false);

  async function runQuery(input: string) {
    setLoading(true);
    try {
      const response = await fetch("/api/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: input })
      });
      const payload = (await response.json()) as QueryResponse;
      setData(payload);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app-shell">
      <section className="hero-card">
        <div className="eyebrow">React + analytics AI</div>
        <h1>Natural-Language SQL Analytics and Dashboard Generation Studio</h1>
        <p>
          Ask a business question in plain English, inspect the generated SQL, and review results as both a table and a chart.
        </p>

        <div className="input-row">
          <input value={question} onChange={(event) => setQuestion(event.target.value)} />
          <button onClick={() => runQuery(question)}>{loading ? "Running..." : "Run query"}</button>
        </div>

        <div className="chip-row">
          {starterQuestions.map((item) => (
            <button key={item} className="chip" onClick={() => setQuestion(item)}>
              {item}
            </button>
          ))}
        </div>
      </section>

      {data ? (
        <section className="content-grid">
          <article className="panel">
            <div className="panel-label">Generated SQL</div>
            <pre>{data.sql}</pre>
            <div className="panel-label">Insight</div>
            <p>{data.insight}</p>
          </article>

          <article className="panel">
            <div className="panel-label">Results</div>
            <ResultsTable rows={data.rows} />
          </article>

          {data.chart ? (
            <article className="panel wide-panel">
              <div className="panel-label">Chart</div>
              <div className="chart-title">{data.chart.title}</div>
              <div style={{ width: "100%", height: 320 }}>
                <ResponsiveContainer>
                  <BarChart data={data.rows}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey={data.chart.xKey} />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey={data.chart.yKey} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </article>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
