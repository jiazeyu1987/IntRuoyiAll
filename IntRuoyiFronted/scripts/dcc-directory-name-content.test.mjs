import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import ts from 'typescript'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue', import.meta.url)), 'utf8')
const start = source.indexOf('const mergeRuleReadPermission =')
const end = source.indexOf('const loadRules =', start)
const c = vm.createContext({})
vm.runInContext(ts.transpileModule(source.slice(start,end) + ';globalThis.normalize=mergeRuleReadPermission', {compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
test('name-only grant remains name-only through UI normalization',()=> {
  const result = c.normalize({canQuery:true,canPreview:false,canDownload:false})
  assert.equal(result.canQuery,true)
  assert.equal(result.canPreview,false)
})
test('content grant includes name visibility',()=> {
  const result = c.normalize({canQuery:false,canPreview:true,canDownload:false})
  assert.equal(result.canQuery,true)
  assert.equal(result.canPreview,true)
})
