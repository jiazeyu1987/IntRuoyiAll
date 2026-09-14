const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const sourcePath = path.resolve(__dirname, '../../src/api/mes/pro/edhr/batchExecution.ts')
const source = fs.readFileSync(sourcePath, 'utf8')
const sourceFile = ts.createSourceFile(
  sourcePath,
  source,
  ts.ScriptTarget.Latest,
  true,
  ts.ScriptKind.TS
)

const diagnostics = sourceFile.parseDiagnostics
if (diagnostics.length > 0) {
  const details = diagnostics
    .map((diagnostic) => ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n'))
    .join('\n')
  throw new Error(`batchExecution.ts contains TypeScript syntax errors:\n${details}`)
}

if (!source.includes("url: BATCH_EXECUTION_BASE_URL + '/open-or-create-manual'")) {
  throw new Error('manual open/create API must use a parse-safe URL expression')
}

console.log('edhr batch execution API syntax contract: PASS')
