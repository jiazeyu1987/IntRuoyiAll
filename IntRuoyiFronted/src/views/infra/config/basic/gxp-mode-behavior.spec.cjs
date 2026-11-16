const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, computed } = require('vue')

const source = fs.readFileSync(path.join(__dirname, 'components/GxpAuditModeSetting.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('mode.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const body = ast.statements.filter(statement => !ts.isImportDeclaration(statement) &&
  !(ts.isExpressionStatement(statement) && statement.getText(ast).startsWith('defineOptions(')))
const executable = ts.transpileModule(body.map(statement => statement.getText(ast)).join('\n') +
  '\nreturn { load, save, selectedMode, runtime, configRow, pendingRestart, error }',
  { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function harness({ canUpdate = true, mode = 'FORMAL', persisted = 'FORMAL', saveError } = {}) {
  const writes = [], messages = []
  const row = { id: 1, key: 'system.gxp.audit-mode', value: persisted, type: 1, visible: true }
  const api = {
    getConfigPage: async () => ({ list: [row] }),
    updateConfig: async request => { writes.push(request); if (saveError) throw saveError }
  }
  const component = new Function('ConfigApi', 'getGxpAuditRuntimeMode', 'defineProps', 'useMessage',
    'ref', 'computed', 'onMounted', executable)(api,
    async () => ({ mode, auditEnabled: mode !== 'DEVELOPMENT_DISABLED' }),
    () => ({ canUpdate }), () => ({ success: text => messages.push(text) }), ref, computed, () => {})
  return { ...component, api, writes, messages }
}

test('saved mode and running mode remain distinct until a restart', async () => {
  const page = harness({ mode: 'DEVELOPMENT_DISABLED', persisted: 'LOCAL_POLICY' })
  await page.load()
  assert.equal(page.selectedMode.value, 'LOCAL_POLICY')
  assert.equal(page.pendingRestart.value, true)
  page.selectedMode.value = 'FORMAL'
  await page.save()
  assert.equal(page.writes[0].value, 'FORMAL')
  assert.equal(page.configRow.value.value, 'FORMAL')
  assert.equal(page.runtime.value.mode, 'DEVELOPMENT_DISABLED')
  assert.equal(page.pendingRestart.value, true)
})

test('failed save preserves persisted and running modes and reports the failure', async () => {
  const page = harness({ saveError: new Error('当前环境不允许关闭审计') })
  await page.load()
  page.selectedMode.value = 'DEVELOPMENT_DISABLED'
  await page.save()
  assert.equal(page.configRow.value.value, 'FORMAL')
  assert.equal(page.runtime.value.mode, 'FORMAL')
  assert.equal(page.error.value, '当前环境不允许关闭审计')
  assert.equal(page.messages.length, 0)
})

test('read-only account cannot save even when handler is invoked', async () => {
  const page = harness({ canUpdate: false })
  await page.load()
  page.selectedMode.value = 'DEVELOPMENT_DISABLED'
  await page.save()
  assert.equal(page.writes.length, 0)
})

test('invalid saved configuration does not select a substitute mode', async () => {
  const page = harness({ persisted: 'invalid' })
  await page.load()
  assert.equal(page.configRow.value, undefined)
  assert.equal(page.selectedMode.value, undefined)
  assert.match(page.error.value, /缺少唯一且有效/)
})

test('refresh failure clears stale configuration and prevents saving it', async () => {
  const page = harness()
  await page.load()
  page.api.getConfigPage = async () => { throw new Error('数据库不可用') }
  await page.load()
  page.selectedMode.value = 'LOCAL_POLICY'
  await page.save()
  assert.equal(page.writes.length, 0)
  assert.equal(page.error.value, '数据库不可用')
})
