const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractSignatureTemplate = (source) => {
  const match = source.match(
    /<UnifiedListTemplate[\s\S]*?table-key="mes\.pro\.edhr\.signature\.history"[\s\S]*?<\/UnifiedListTemplate>/
  )
  assert.ok(match, '签名记录页必须继续使用标准列表模板。')
  return match[0]
}

const signaturePage = readSource('src/views/mes/pro/edhr/SignaturePage.vue')
const signatureTemplate = extractSignatureTemplate(signaturePage)

assert.match(signatureTemplate, /:filter-definitions="signatureQuickFilterDefinitions"/, '必须保留标准快速过滤控件。')
assert.match(signatureTemplate, /@quick-filter-query="signatureQuickFilter\.applyQuickFilter"/, '快速过滤查询必须继续直接触发列表查询。')
assert.match(signatureTemplate, /:show-column-reset="false"/, '只保留“显示字段”，必须删除红框外的“重置列”。')
assert.doesNotMatch(signatureTemplate, /<template\s+#extra-filters>/, '必须删除红框外额外筛选控件。')
assert.doesNotMatch(signatureTemplate, /<template\s+#actions>/, '必须删除红框外查询、重置等动作控件。')
assert.doesNotMatch(signatureTemplate, />\s*高级筛选\s*</, '必须删除红框外高级筛选入口。')
assert.doesNotMatch(signatureTemplate, />\s*重置\s*</, '必须删除红框外重置按钮。')
assert.doesNotMatch(signatureTemplate, />\s*查询\s*</, '必须删除红框外额外查询按钮，保留快速过滤自带查询。')

for (const removedIdentifier of [
  'signatureAdvancedFilterNames',
  'signedTimeRange',
  'handleQuery',
  'resetQuery',
  'resetSignatureColumnConfig'
]) {
  assert.doesNotMatch(
    signaturePage,
    new RegExp(removedIdentifier),
    `删除红框外控件后不应保留废弃引用：${removedIdentifier}`
  )
}

console.log('PASS: eDHR signature page keeps only red-box controls')
