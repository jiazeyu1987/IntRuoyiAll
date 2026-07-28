const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const sourcePath = path.join(__dirname, 'node-chain-first-node-real.e2e.cjs')
let source = fs.readFileSync(sourcePath, 'utf8')

const replacements = [
  [
    "const repoRoot = 'D:\\\\IntRuoyiWorktree\\\\20260728-codex-node-chain-first-node-contract'",
    "const repoRoot = 'E:\\\\IntRuoyi'"
  ],
  [
    "const frontendBaseUrl = 'http://127.0.0.1:8083'",
    "const frontendBaseUrl = 'http://127.0.0.1:8081'"
  ],
  [
    "const apiBase = 'http://127.0.0.1:48083/admin-api'",
    "const apiBase = 'http://127.0.0.1:48081/admin-api'"
  ],
  [
    "'20260728-codex-node-chain-first-node-contract'",
    "'20260728-codex-node-chain-first-node-contract-int-main'"
  ]
]

for (const [from, to] of replacements) {
  if (!source.includes(from)) {
    throw new Error(`Expected E2E source token not found: ${from}`)
  }
  source = source.split(from).join(to)
}

const context = {
  require,
  console,
  process,
  Buffer,
  __dirname,
  __filename: sourcePath,
  setTimeout,
  clearTimeout,
  setInterval,
  clearInterval,
  setImmediate,
  clearImmediate
}
context.global = context

vm.runInNewContext(source, context, { filename: sourcePath })
