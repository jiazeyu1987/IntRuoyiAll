const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const panelPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const apiPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/api/mes/pro/feedback/index.ts'
)

const panelSource = fs.readFileSync(panelPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.match(
  panelSource,
  /data-pqc-defect-description/,
  'PQC 填写页必须提供稳定的手动不良说明输入控件。'
)
assert.match(
  panelSource,
  /defectDescription:\s*undefined\s+as\s+string\s*\|\s*undefined/,
  'PQC 草稿必须保存手动不良说明字段，而不是依赖固定不良原因列表。'
)
assert.match(
  panelSource,
  /validatePqcDefectDescription/,
  'PQC 提交前必须校验不合格结果已填写手动不良说明。'
)
assert.match(
  panelSource,
  /nonconformanceDescription:\s*normalizePqcDefectDescription\(\)/,
  'PQC 正式提交 payload 必须包含结构化 nonconformanceDescription。'
)
assert.match(
  panelSource,
  /pqcDraft:\s*\{[\s\S]*defectDescription:\s*normalizePqcDefectDescription\(\)/,
  'PQC rawPayload.pqcDraft 必须保存原始手动不良说明快照。'
)
const defectDescriptionBlock = panelSource.match(
  /<div class="frontline-pqc-defect-description"[\s\S]*?<\/div>/
)?.[0] || ''
assert.doesNotMatch(
  defectDescriptionBlock,
  /defectReasonId|defectReasons/,
  '新版 AC-D03 不应让 PQC 不良说明依赖固定不良原因主数据列表。'
)
assert.match(
  apiSource,
  /nonconformanceDescription\?:\s*string/,
  '前端 PQC 提交类型必须声明 nonconformanceDescription。'
)

console.log('PASS role-matrix PQC manual defect note static contract')
