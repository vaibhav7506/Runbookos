import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const cases = JSON.parse(
  await readFile(resolve("evaluation/incident-analysis-cases.json"), "utf8")
);
const results = JSON.parse(
  await readFile(resolve("evaluation/incident-analysis-results.json"), "utf8")
);

const required = new Set([
  "deployment-regression",
  "expired-credential",
  "database-timeout",
  "dependency-outage",
  "memory-pressure",
  "noisy-duplicate-alerts",
]);
const ids = new Set(cases.map((entry) => entry.id));
if (cases.length !== required.size || [...required].some((id) => !ids.has(id))) {
  throw new Error("Evaluation dataset does not contain all required incident classes");
}
for (const entry of cases) {
  if (!entry.service || !entry.evidenceKind || !entry.expectedComponent || !entry.expectedAction) {
    throw new Error(`${entry.id}: incomplete deterministic expectation`);
  }
}
if (
  results.analyzer !== "deterministic-v1" ||
  results.promptVersion !== 1 ||
  results.cases !== cases.length ||
  results.passed !== cases.length ||
  results.failed !== 0
) {
  throw new Error("Stored evaluation result is stale or failing");
}
console.log(`Analysis evaluation passed ${results.passed}/${results.cases} cases.`);
