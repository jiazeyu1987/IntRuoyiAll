const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
const ast = ts.createSourceFile('api.ts', source, ts.ScriptTarget.Latest, true)
const body = ast.statements.filter(node => !ts.isImportDeclaration(node))
  .map(node => node.getText(ast).replace(/^export /, '')).join('\n')
const js = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const createApi = post => Function('request', `${js}; return applyTeamLeaderActiveOrderRelease`)({ post })

test('completion waits for a formal response beyond the default 30 seconds without retry', async () => {
  const calls = []
  const receipt = { batchExecutionId: '9001', pqcReleaseWorkTaskId: '9002' }
  const apply = createApi(async options => {
    calls.push(options)
    assert.ok(options.timeout >= 120000, 'completion needs a bounded long-operation timeout')
    assert.ok(options.timeout <= 240000)
    return receipt
  })
  const command = { activeOrderId: '1001', idempotencyKey: 'completion-test' }
  assert.equal(await apply(command), receipt)
  assert.equal(calls.length, 1)
  assert.equal(calls[0].data, command)
  assert.equal(calls[0].url, '/mes/pro/process-pool/team-leader/active-order/release/apply')
})

test('completion propagates request failures and never retries implicitly', async () => {
  let calls = 0
  const failure = new Error('formal completion rejected')
  const apply = createApi(async () => { calls++; throw failure })
  await assert.rejects(apply({ activeOrderId: '1001' }), error => error === failure)
  assert.equal(calls, 1)
})
