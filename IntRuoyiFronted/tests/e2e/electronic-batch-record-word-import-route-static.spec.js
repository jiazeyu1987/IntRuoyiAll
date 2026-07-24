const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue'),
  'utf8'
)

assert.ok(
  !page.includes("const wordImportRouteKey = 'B'"),
  '电子批记录 Word 导入不能固定按 B 路线解析所有文件'
)

assert.ok(
  page.includes('resolveWordImportRouteKey(file.name)'),
  '电子批记录 Word 导入必须根据文件名解析 routeKey'
)

assert.match(
  page,
  /const resolveWordImportRouteKey = \(fileName: string\) => \{[\s\S]*E\\s\*1[\s\S]*return 'E'/,
  'E 1 损耗单文件名必须解析为 E 路线'
)

assert.match(
  page,
  /BatchRecordReportApi\.preflightUploadedRoute\([\s\S]*resolveWordImportRouteKey\(file\.name\)[\s\S]*selectedProjectName[\s\S]*productNames/,
  '导入预检必须使用解析后的路线'
)

assert.match(
  page,
  /BatchRecordReportApi\.recognizeUploadedRoute\([\s\S]*wordImportRouteKey[\s\S]*batchRecordName[\s\S]*upgrade[\s\S]*productNames/,
  '上传识别必须传递解析后的路线'
)

assert.ok(
  page.includes('当前版本为') && page.includes('确认后将生成'),
  '已有批记录版本时，导入确认文案必须说明将生成目标版本'
)

assert.match(
  page,
  /resolveWordImportRouteUpgradeMessage[\s\S]*生成路线候选版本，待审批\/发布后生效/,
  '已有工艺路线时，Word 重建确认文案必须说明生成路线候选版本，而不是直接切换 active 路线'
)

assert.ok(
  !page.includes('确认后只升版既有路线'),
  'Word 重建确认文案不能继续使用直接升版既有路线的旧语义'
)

assert.match(
  page,
  /BatchRecordReportApi\.recognizeUploadedRoute\([\s\S]*Boolean\(selection\.routeUpgradeConfirmed\)[\s\S]*selection\.expectedRouteId[\s\S]*selection\.expectedRouteVersionId/,
  'Word 重建上传必须携带 routeUpgradeConfirmed、expectedRouteId 和 expectedRouteVersionId'
)

assert.ok(
  page.includes("'确认生成路线候选版本'") && page.includes("'生成候选版本'"),
  'Word 重建确认框标题和按钮必须使用候选版本语义'
)

console.log('PASS: electronic batch record word import route static contract')
