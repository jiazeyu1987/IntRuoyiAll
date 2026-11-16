const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')

function permissions(roles = [], grants = []) {
  const source = fs.readFileSync('src/views/dcc/controlled-file/basic-data/components/CatalogWorkbookActions.vue', 'utf8')
    .match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const exports = {}
  const dependencies = {
    vue: require('vue'),
    '@/config/axios': {},
    '@/utils/download': {},
    '@/utils/permission': {
      checkRole: values => values.some(value => roles.includes(value)),
      checkPermi: values => values.some(value => grants.includes(value))
    }
  }
  const code = ts.transpileModule(source + '\nexport { canImport, canExport }', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code, {
    exports, defineProps: () => ({}), withDefaults: (props, defaults) => Object.assign(props, defaults),
    defineEmits: () => () => {}, useMessage: () => ({}),
    require: name => { assert.ok(name in dependencies, name); return dependencies[name] }
  })
  return exports
}

test('project code administrator sees workbook import without unrelated export access', () => {
  const result = permissions(['dcc_project_code_admin'])
  assert.equal(result.canImport.value, true)
  assert.equal(result.canExport.value, false)
})

test('ordinary users and project editors alone cannot import the complete workbook', () => {
  for (const grants of [[], ['dcc:project-code:create', 'dcc:project-code:update']]) {
    assert.equal(permissions([], grants).canImport.value, false)
  }
})

test('system and tenant administrators retain import and export access', () => {
  for (const role of ['super_admin', 'tenant_admin']) {
    const result = permissions([role])
    assert.equal(result.canImport.value, true)
    assert.equal(result.canExport.value, true)
  }
})

test('users with the complete import permission set retain import access', () => {
  const grants = ['mdm:product:create', 'mdm:product:update', 'dcc:project-code:create',
    'dcc:project-code:update', 'mdm:enterprise:create', 'mdm:enterprise:update',
    'mdm:company-scope:create', 'mdm:company-scope:update', 'dcc:registration-certificate:config:update']
  assert.equal(permissions([], grants).canImport.value, true)
  assert.equal(permissions([], grants.slice(0, -1)).canImport.value, false)
})
