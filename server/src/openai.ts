export async function refineInsight(question: string, sql: string, rows: unknown[]) {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    return null;
  }

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model: process.env.OPENAI_MODEL ?? "gpt-4.1-mini",
      input: `Question: ${question}
SQL: ${sql}
Rows: ${JSON.stringify(rows)}
Write a concise 2 sentence analytics insight.`
    })
  });

  if (!response.ok) {
    return null;
  }

  const data = await response.json();
  return data.output_text ?? null;
}
