const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const moduleExports = {}
const helper = ts.transpileModule(fs.readFileSync(path.join(root, 'src/components/ReleaseInfoDock/releaseInfo.ts'), 'utf8'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
Function('exports', helper)(moduleExports)
const { readReleaseInfo } = moduleExports
const jsonResponse = value => new Response(JSON.stringify(value), { headers: { 'content-type': 'application/json; charset=utf-8' } })

test('valid release metadata remains readable', async () => {
  const value = { releaseTag: '2026-11-14-14-12-24', changeSet: { gitChanges: ['修复签名详情'] } }
  assert.deepEqual(await readReleaseInfo(jsonResponse(value)), value)
})
test('HTML, missing resources and broken JSON provide actionable errors', async () => {
  await assert.rejects(readReleaseInfo(new Response('<!DOCTYPE html>', { headers: { 'content-type': 'text/html' } })), /返回类型为 text\/html/)
  await assert.rejects(readReleaseInfo(new Response('', { status: 404 })), /HTTP 404/)
  await assert.rejects(readReleaseInfo(new Response('{', { headers: { 'content-type': 'application/json' } })), /不是有效 JSON/)
})
test('invalid release tags and malformed change lists fail before rendering', async () => {
  for (const value of [null, [], {}, { releaseTag: 123 }, { releaseTag: ' ' }]) {
    await assert.rejects(readReleaseInfo(jsonResponse(value)), /有效 releaseTag/)
  }
  for (const changeSet of [null, [], 'text', { gitChanges: 'text' }, { gitChanges: [1] }]) {
    await assert.rejects(readReleaseInfo(jsonResponse({ releaseTag: '2026-11-14-14-12-24', changeSet })), /数据格式无效/)
  }
})
function dock(development, response) {
  const source = parse(fs.readFileSync(path.join(root, 'src/components/ReleaseInfoDock/ReleaseInfoDock.vue'), 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('dock.ts', source, ts.ScriptTarget.Latest, true)
  const script = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
    .replaceAll('import.meta.env.DEV', String(development)).replaceAll('import.meta.env.BASE_URL', "'/app/'")
  const code = ts.transpileModule(script, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  let mounted
  const requests = []
  const state = Function('ref', 'computed', 'onMounted', 'readReleaseInfo', 'fetch', `${code}; return {statusText, loadError};`)(
    vue.ref, vue.computed, fn => { mounted = fn }, readReleaseInfo, async url => { requests.push(url); return response })
  return { state, requests, mount: () => mounted() }
}
test('development identifies itself explicitly and never requests nonexistent published metadata', async () => {
  const { state, requests, mount } = dock(true)
  await mount()
  assert.deepEqual(requests, [])
  assert.equal(state.statusText.value, '本地开发')
  assert.equal(state.loadError.value, '')
})
test('published mode reads metadata under the application base path and displays the real release tag', async () => {
  const { state, requests, mount } = dock(false, jsonResponse({ releaseTag: '2026-11-14-14-12-24' }))
  await mount()
  assert.deepEqual(requests, ['/app/release-info.json'])
  assert.equal(state.statusText.value, '2026-11-14-14-12-24')
  assert.equal(state.loadError.value, '')
})
test('published mode exposes an HTML metadata response as failure, without inventing a release', async () => {
  const { state, mount } = dock(false, new Response('<!DOCTYPE html>', { headers: { 'content-type': 'text/html' } }))
  await mount()
  assert.equal(state.statusText.value, '版本信息读取失败')
  assert.match(state.loadError.value, /text\/html/)
})
