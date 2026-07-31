const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')

assert.match(
  runner,
  /function redactSensitiveText\(value\)[\s\S]*Bearer[\s\S]*Authorization[\s\S]*Cookie[\s\S]*sk-/,
  'Runner must redact common credential forms before returning Codex diagnostics.'
)
assert.match(
  runner,
  /function summarizeCodexFailure\(stderrText\)[\s\S]*remote installed plugin bundle sync failed[\s\S]*unknown feature key in config[\s\S]*slice\(-CODEX_FAILURE_DETAIL_MAX_LENGTH\)/,
  'Runner must remove known non-fatal startup warnings and preserve the stderr tail.'
)
assert.match(
  runner,
  /throw new Error\(`codex exec failed with exit \$\{childResult\.exitCode\}: \$\{summarizeCodexFailure\(stderrText\)\}`\)/,
  'Non-zero Codex exits must report the sanitized meaningful tail instead of the warning prefix.'
)

console.log('PASS: Codex runner failure diagnostics static contract')
