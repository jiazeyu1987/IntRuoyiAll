const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
const importDialogPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/ImportAttributionDialog.vue')
const importFormPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/ThirdPartyFeedbackImportForm.vue')
const feedbackFormPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/FeedbackForm.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')

for (const filePath of [pagePath, importDialogPath, importFormPath, feedbackFormPath, apiPath]) {
  assert(fs.existsSync(filePath), `报工追踪相关页面必须存在：${filePath}`)
}

const pageSource = fs.readFileSync(pagePath, 'utf8')
const dialogSource = fs.readFileSync(importDialogPath, 'utf8')
const importFormSource = fs.readFileSync(importFormPath, 'utf8')
const feedbackFormSource = fs.readFileSync(feedbackFormPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const token of [
  '报工编号',
  '记录编号',
  '正式报工编号',
  'route.query.feedbackId',
  'route.query.importRecordId',
  'isCancelError',
  'resolveErrorMessage',
  '删除报工失败，请检查后端接口。',
  'feedbackId',
  'attributionStatus',
  'importQueryParams.feedbackId',
  'queryParams.id',
  '归属会按所选订单工序生成草稿正式报工',
  '审批用于质量/合规确认',
  '提交正式报工后回写排产进度',
  'approvalImpactText',
  'sourceImportRecordId',
  'sourceImportFileName',
  'sourceImportSheetName',
  'sourceImportRowNo',
  'sourceImportAttributionTime',
  '当前审批人',
  '来源文件',
  '来源导入行',
  '审批影响',
  'canModifyAttribution',
  'modifyBlockedReason',
  'linkedFeedbackCount',
  'generatedFeedbackDraft',
  'linkedFeedbackStatus',
  '确认报工',
  'confirmImportRecordBatch',
  'getImportRecordBatchSummary',
  '已归属',
  '修改归属'
]) {
  assert(
    pageSource.includes(token)
      || dialogSource.includes(token)
      || importFormSource.includes(token)
      || feedbackFormSource.includes(token)
      || apiSource.includes(token),
    `报工追踪链路必须保留关键字段：${token}`
  )
}

assert(
  /emits\('success',\s*\{[\s\S]*?feedbackId[\s\S]*?importRecordId[\s\S]*?scheduleOrderProcessId[\s\S]*?processLabel[\s\S]*?attributionTime/.test(dialogSource),
  '归属弹窗成功事件必须携带正式报工、导入记录、排产工序、工序标签和归属时间，供父页面展示下一步。'
)
assert(
  /scope\.row\.canModifyAttribution/.test(pageSource),
  '已归属且允许修改的记录必须出现修改归属入口。'
)
assert(!pageSource.includes('查看正式报工'), '待归属页不应继续保留“查看正式报工”入口。')
assert(!pageSource.includes('openFeedbackFromImportRecord'), '待归属页不应继续保留跳正式报工页的旧方法。')
assert(!pageSource.includes('catch {}'), '报工页不得保留空 catch。')
assert(!pageSource.includes('catch{}'), '报工页不得保留空 catch。')
assert(!feedbackFormSource.includes('catch {\n  }'), '报工表单不得吞掉提交异常。')
assert(!pageSource.includes('>去审批<'), '归属完成后的下一步不得用“去审批”误导非审批人。')
assert(!pageSource.includes('label="审核人"'), '正式报工列表用户侧称呼必须统一为“当前审批人”。')
assert(!feedbackFormSource.includes('label="审核人"'), '报工表单用户侧称呼必须统一为“当前审批人”。')
assert(!pageSource.includes('label="工段长"'), '待归属列表不得恢复旧称呼“工段长”。')
assert(!pageSource.includes('lastAttributionResult'), '待归属页不应继续依赖顶部归属结果卡。')
assert(!pageSource.includes('进度已在归属时回写'), '归属页旧文案必须移除，避免误导草稿保存即回写。')
assert(
  feedbackFormSource.includes('导入来源的草稿正式报工请返回待归属页批量确认提交'),
  '正式报工详情必须阻止导入草稿继续走单条提交路径。'
)
assert(
  importFormSource.includes("const importMode = ref<ImportMode>('DIRECT_WORK_REPORT')"),
  '导入报工弹窗默认必须选中“李萍报工单”，避免直接报工表头文件误走第三方报工接口。'
)
assert(
  importFormSource.includes("importMode.value = 'DIRECT_WORK_REPORT'"),
  '每次打开导入报工弹窗都必须重置为“李萍报工单”，避免沿用第三方报工模式导致表头不匹配。'
)
assert(
  !/<el-radio-group[\s\S]*?<\/el-radio-group>/.test(importFormSource)
    && !/class="mb-16px rounded-6px border border-\[#dbe3ef\][\s\S]*?<\/div>\s*\n\s*<el-upload/.test(importFormSource)
    && !/<template #tip>[\s\S]*?<\/template>/.test(importFormSource),
  '导入报工弹窗不得显示顶部导入类型切换、导入说明和底部格式提示，页面默认使用“李萍报工单”。'
)
assert(
  importFormSource.includes("import { read, utils } from 'xlsx'"),
  '导入报工弹窗必须读取 Excel 表头，避免李萍报工单误走第三方导入接口。'
)
assert(
  importFormSource.includes('DIRECT_WORK_REPORT_HEADERS')
    && importFormSource.includes('THIRD_PARTY_HEADERS')
    && importFormSource.includes('detectImportModeByHeaders'),
  '导入报工弹窗必须维护两类模板表头并按首行表头识别导入类型。'
)
assert(
  /const detectedImportMode = ref<ImportMode \| null>\(null\)/.test(importFormSource),
  '导入报工弹窗必须记录已识别的文件类型，提交前防止用户手动切回错误模式。'
)
assert(
  /handleFileChange[\s\S]*?detectImportModeByHeaders[\s\S]*?importMode\.value = detectedMode/.test(importFormSource),
  '选择文件后必须根据 Excel 表头自动切换到匹配的导入模式。'
)
assert(
  /submitForm[\s\S]*?detectedImportMode\.value[\s\S]*?detectedImportMode\.value !== importMode\.value[\s\S]*?return/.test(importFormSource),
  '提交前必须拦截文件表头与当前导入模式不一致，不能继续调用错误接口。'
)
assert(
  apiSource.includes('directWorkReportDetails')
    && apiSource.includes('DirectWorkReportImportDetailVO')
    && apiSource.includes('progressDeltaPercent'),
  '直接报工导入接口类型必须暴露逐行进度明细。'
)
assert(
  importFormSource.includes('direct-import-result-dialog')
    && importFormSource.includes('直接报工导入结果')
    && importFormSource.includes('工序 / 产线')
    && importFormSource.includes('进度增加')
    && importFormSource.includes('formatProgressDelta'),
  '李萍报工单导入完成后必须展示结构化结果弹窗，包含工序/产线和进度增加。'
)
assert(
  importFormSource.includes('direct-import-result__body')
    && importFormSource.includes('direct-import-result__work-orders')
    && importFormSource.includes('direct-import-result__detail-panel')
    && importFormSource.includes('groupedDirectWorkOrders')
    && importFormSource.includes('selectedWorkOrderCode')
    && importFormSource.includes('selectedWorkOrderDetails'),
  '李萍报工单导入结果必须使用左侧生产工单分组、右侧工序明细的双栏结构。'
)
assert(
  /v-for="group in groupedDirectWorkOrders"[\s\S]*?group\.workOrderCode[\s\S]*?selectDirectWorkOrder/.test(
    importFormSource
  ),
  '导入结果左侧必须按生产工单渲染可点击卡片。'
)
assert(
  /:data="selectedWorkOrderDetails"[\s\S]*?label="工序 \/ 产线"[\s\S]*?label="产品"[\s\S]*?label="本次完成 \/ 已报工变化"[\s\S]*?label="进度增加 \/ 报工单号"/.test(
    importFormSource
  ),
  '导入结果右侧必须用紧凑组合列展示当前生产工单的工序、产线、产品、完成数量、已报工变化、进度和报工单号。'
)
assert(
  importFormSource.includes('width="min(96vw, 1280px)"')
    && importFormSource.includes('grid-template-columns: 230px minmax(0, 1fr)')
    && !importFormSource.includes('label="报工单号" prop="feedbackCode"'),
  '导入结果弹窗必须扩大可用宽度并合并报工单号列，避免右侧列表被裁切。'
)
assert(
  importFormSource.includes('clampProgressPercent')
    && /formatPercent[\s\S]*?clampProgressPercent/.test(importFormSource)
    && /formatProgressDelta[\s\S]*?clampProgressPercent/.test(importFormSource),
  '导入结果进度百分比展示必须限制在 0-100%，但不能截断已报工数量。'
)
assert(
  !/:data="directImportResult\.directWorkReportDetails \|\| \[\]"/.test(importFormSource),
  '李萍报工单导入结果不得继续用一张横向大表混合展示所有订单。'
)
assert(
  !/导入完成；工作表数：\$\{result\.sheetCount\}；创建报工数：\$\{result\.importedCount\}[\s\S]*?feedbackCodesText/.test(importFormSource),
  '李萍报工单导入完成后不得继续使用长文本报工单号提示。'
)

console.log('PASS: MES feedback tracking static contract')
