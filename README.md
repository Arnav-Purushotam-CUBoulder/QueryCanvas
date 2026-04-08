# Natural-Language SQL Analytics and Dashboard Generation Studio

QueryCanvas is a compact full-stack analytics app. Users ask business questions in plain English, the backend generates a safe SQL query over a seeded analytics database, executes it, and the React frontend renders tables and charts.

## Highlights

- React + Vite frontend with TypeScript
- Express + TypeScript backend with SQLite execution
- Natural-language-to-SQL flow with deterministic guards and optional OpenAI refinement
- Chart recommendations and metric summaries for quick analytics exploration

## Quick start

Install and run client and server separately:

```bash
cd server
npm install
npm run dev

cd ../client
npm install
npm run dev
```

Optional environment variables for the server:

```bash
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4.1-mini
PORT=8787
```

## Demo questions

- Which customer segments generated the most revenue?
- Show monthly revenue for 2025.
- What are the top products by units sold?
- Compare revenue by region.
