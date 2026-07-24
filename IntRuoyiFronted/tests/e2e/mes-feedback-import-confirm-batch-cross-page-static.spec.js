const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const pagePath = path.resolve(frontendRoot, 'src/views/mes/pro/feedback/index.vue')

assert(fs.existsSync(pagePath), `报工待归属页面必须存在：${pagePath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const fragment of [
  'const importRecordDraftMap = ref<Record<number, ProFeedbackImportConfirmBatchRowReqVO>>({})',
  'const persistCurrentPageImportRecordDrafts = (rows: ProFeedbackImportRecordVO[] = importRecordList.value) => {',
  'const getCurrentImportBatchAllRecords = async () => {',
  'const pageSize = Math.max(currentImportRecordIds.value.length, 1)',
  'pageSize,',
  'importRecordIds: [...currentImportRecordIds.value]',
  'const buildConfirmBatchPayload = (rows: ProFeedbackImportRecordVO[]): ProFeedbackImportConfirmBatchReqVO => ({'
]) {
  assert(pageSource.includes(fragment), `整批确认跨分页修复必须具备关键实现片段：${fragment}`)
}

assert(
  !pageSource.includes('pageSize: -1'),
  '整批确认全量拉取不能再使用 pageSize=-1，否则会触发后端分页参数校验失败。'
)

assert(
  /const getImportRecordList = async \(\) => \{[\s\S]*persistCurrentPageImportRecordDrafts\(\)[\s\S]*applyImportRecordDrafts\(data\.list\)/.test(
    pageSource
  ),
  '分页刷新待归属列表前必须先缓存当前页编辑值，并在回填时重新合并草稿。'
)

assert(
  /const getCurrentImportBatchAllRecords = async \(\) => \{[\s\S]*persistCurrentPageImportRecordDrafts\(\)[\s\S]*const pageSize = Math\.max\(currentImportRecordIds\.value\.length, 1\)[\s\S]*ProFeedbackApi\.getImportRecordPage\(\{[\s\S]*pageNo: 1,[\s\S]*pageSize,[\s\S]*importRecordIds: \[\.\.\.currentImportRecordIds\.value\][\s\S]*\}\)[\s\S]*applyImportRecordDrafts\(data\.list\)/.test(
    pageSource
  ),
  '确认报工前必须按当前锁定批次拉取全量记录，并使用满足后端校验的正数 pageSize，而不是沿用当前分页数据或传 -1。'
)

assert(
  /const handleConfirmBatch = async \(\) => \{[\s\S]*const confirmRows = await getCurrentImportBatchAllRecords\(\)[\s\S]*buildConfirmBatchBlockReasons\(confirmRows\)[\s\S]*const payload = buildConfirmBatchPayload\(confirmRows\)/.test(
    pageSource
  ),
  '确认报工必须基于全量批次记录做拦截判断和 payload 构建。'
)

console.log('PASS: MES feedback import confirm batch cross-page static contract')
