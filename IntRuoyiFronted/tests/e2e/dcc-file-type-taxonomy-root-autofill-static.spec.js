const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const source = readSource(
  'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue'
)

assert.ok(source.includes("openForm('create')"), '新增一级入口必须继续打开新增弹框')
assert.ok(
  source.includes('rootCreateMode'),
  '新增一级必须有独立模式，避免随弹框内上级分类变更影响自动生成逻辑'
)
assert.ok(
  source.includes('buildRootTaxonomyCode'),
  '新增一级必须自动生成分类编码，不能要求用户手填'
)
assert.ok(
  source.includes('resolveNextRootTaxonomySort'),
  '新增一级必须自动生成排序，不能要求用户手填'
)
assert.ok(
  source.includes('applyRootCreateDefaults'),
  '打开新增一级弹框时必须集中写入自动生成的编码和排序'
)
assert.ok(
  source.includes('ensureRootCreateDefaults'),
  '保存新增一级前必须确认自动生成值仍然有效'
)
assert.match(
  source,
  /rootCreateMode\.value\s*=\s*type\s*===\s*'create'\s*&&\s*!parent/,
  'openForm 必须只在新增一级入口启用 rootCreateMode'
)
assert.match(
  source,
  /if\s*\(rootCreateMode\.value\)\s*\{[\s\S]*applyRootCreateDefaults\(\)[\s\S]*\}\s*else\s+if\s*\(parent\?\.id\)/,
  '新增一级应自动填充默认值，新增下级仍按父级带入原行为'
)
assert.match(
  source,
  /:disabled="formType === 'update' \|\| rootCreateMode"/,
  '新增一级弹框必须锁定上级分类，确保保存的是一级分类'
)
assert.match(
  source,
  /<el-input\s+v-model="formData\.code"[\s\S]*:disabled="rootCreateMode"/,
  '新增一级弹框的分类编码必须只读显示，不能手填'
)
assert.match(
  source,
  /<el-input-number\s+v-model="formData\.sort"[\s\S]*:disabled="rootCreateMode"/,
  '新增一级弹框的排序必须只读显示，不能手填'
)
assert.match(
  source,
  /if\s*\(rootCreateMode\.value\)\s*\{[\s\S]*ensureRootCreateDefaults\(\)[\s\S]*\}[\s\S]*const payload =/,
  '保存前必须再次确保新增一级 payload 使用自动生成的编码和排序'
)
assert.ok(
  source.includes("parentId: formData.parentId || null"),
  '新增一级 payload 必须保持 parentId 为空，创建根级分类'
)
assert.doesNotMatch(
  source,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '新增一级自动生成不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC file type taxonomy root autofill static contract')
