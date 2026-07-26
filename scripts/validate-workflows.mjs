import { readdir, readFile } from "node:fs/promises";
import { resolve } from "node:path";
const directory = resolve("automation/n8n/workflows");
const files = (await readdir(directory)).filter((file) => file.endsWith(".json"));
if (files.length < 10) throw new Error("Expected the execution gateway and reusable sub-workflows");
for (const file of files) {
  const workflow = JSON.parse(await readFile(resolve(directory, file), "utf8"));
  if (!workflow.name || !workflow.versionId || !Array.isArray(workflow.nodes) || workflow.nodes.length === 0) throw new Error(`${file}: incomplete workflow`);
  if (workflow.active !== false) throw new Error(`${file}: repository exports must be inactive`);
  const names = new Set();
  for (const node of workflow.nodes) {
    if (!node.name || names.has(node.name)) throw new Error(`${file}: node names must be present and unique`);
    names.add(node.name);
  }
}
console.log(`Validated ${files.length} n8n workflow exports.`);
