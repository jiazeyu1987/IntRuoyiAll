const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/batchrecordreport/index.ts')

assert(fs.existsSync(pagePath), '批记录表单页必须新增 batchrecordformlist/index.vue。')

const page = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

function expectIncludes(snippet, message) {
  assert(page.includes(snippet), message || `Expected source to include: ${snippet}`)
}

expectIncludes(
  "import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'",
  '批记录表单左侧列表必须直接导入标准列表模板。'
)
expectIncludes('<UnifiedListTemplate', '批记录表单左侧必须使用标准列表模板。')
expectIncludes('table-key="mes.pro.edhrBatch.recordFormList.projectCodeV1"', '批记录表单列表新增默认列后必须升级稳定 tableKey。')
expectIncludes(':show-quick-filter-label="false"', '批记录表单列表必须删除红框中的快速过滤文字标签。')
expectIncludes(':columns="recordFormColumns"', '标准列表模板必须接入显示字段配置。')
expectIncludes('@column-change="saveRecordFormColumnConfig"', '列表列配置必须可保存。')
expectIncludes(':show-column-reset="false"', '批记录表单列表必须删除红框中的重置列按钮。')
assert.doesNotMatch(page, /@column-reset="resetRecordFormColumnConfig"/, '删除重置列后不应继续绑定列重置事件。')
assert.doesNotMatch(page, /resetRecordFormColumnConfig/, '删除重置列后不应保留废弃 resetRecordFormColumnConfig。')
assert.doesNotMatch(page, /<el-button[^>]*:icon="Refresh"[\s\S]*?>\s*刷新\s*<\/el-button>/, '批记录表单列表必须删除黄框中的刷新按钮。')
assert.doesNotMatch(page, /import\s+\{\s*Refresh\s*\}\s+from\s+'@element-plus\/icons-vue'/, '删除刷新按钮后不应继续导入 Refresh 图标。')
expectIncludes('ref="wordImportFileInputRef"', '批记录表单列表必须接入 Word 文件选择器。')
expectIncludes(':accept="wordImportFileAccept"', '导入按钮必须接入 Word 文件选择范围。')
expectIncludes('@change="handleImportFileChange"', '导入文件选择必须进入现有 Word 导入变更处理。')
expectIncludes('class="batch-record-form-toolbar__import-button"', '导入按钮必须放在批记录表单列表工具栏。')
assert.match(page, /batch-record-form-toolbar__import-button[\s\S]*@click="openWordImportDialog"[\s\S]*导入/, '黄框位置必须新增“导入”按钮并先打开导入配置弹窗。')
expectIncludes('const wordImportDialog = reactive({', '导入按钮必须复用 DCC 项目选择弹窗状态。')
expectIncludes('loadWordImportProjectOptions', '导入逻辑必须加载 DCC 项目名称候选。')
expectIncludes('getProjectCodePage', '导入逻辑必须从 DCC 项目代码页签读取项目名称。')
expectIncludes('selectedFormSlotType', '导入逻辑必须保留内部表单类型状态。')
expectIncludes('BatchRecordReportApi.preflightUploadedRoute', '主批记录导入逻辑必须复用预检接口。')
expectIncludes('BatchRecordReportApi.recognizeUploadedRoute', '导入逻辑必须复用真实 Word 识别导入接口。')
expectIncludes('BatchRecordReportApi.uploadExtraFormSlot', '附加表单导入逻辑必须复用附加槽位上传接口。')
expectIncludes('ElMessageBox.confirm', '同名批记录导入必须保留升版确认。')
expectIncludes('wordImportRouteKey', '导入逻辑必须保留 B/E 路线识别。')
expectIncludes('@pagination="getList"', '分页必须由标准列表模板触发真实列表加载。')

for (const column of ['产品名称', '项目代码', '表单名称', '类型', '版本', '状态', '更新时间']) {
  assert(page.includes(`label: '${column}'`) || page.includes(`label="${column}"`), `列表必须显示列：${column}`)
}

expectIncludes('prop="versionStatus"', '批记录表单列表状态列必须绑定版本状态。')
expectIncludes('resolveVersionStatusPresentation', '批记录表单列表必须用统一状态展示函数渲染状态标签。')
expectIncludes("PRECHECK_PASSED: { label: '审批中'", '预检通过但未发布的版本必须按审批中展示，不得显示待提交。')
expectIncludes("PENDING_APPROVAL: { label: '审批中'", '版本状态必须把审批中映射为中文标签。')
expectIncludes("APPROVED: { label: '已发布'", '版本状态必须把已发布映射为中文标签。')
expectIncludes("REJECTED: { label: '已驳回'", '版本状态必须把已驳回映射为中文标签。')
expectIncludes("OBSOLETE: { label: '已作废'", '版本状态必须把已作废映射为中文标签。')
assert.doesNotMatch(page, /label:\s*'待提交'/, '批记录表单列表状态只能展示已作废、已发布、已驳回、审批中，不得显示待提交。')

expectIncludes('BatchRecordReportApi.getGeneratedReportPage', '列表必须复用现有生成报表分页接口。')
expectIncludes('BatchRecordReportApi.getCellRules', '右侧预览必须读取真实单元格规则。')
expectIncludes('BatchRecordReportApi.getSignatureCellMarkers', '右侧预览必须读取真实签名位。')
expectIncludes('EdhrExecutionReadonlyForm', '右侧预览必须复用 eDHR 只读表单组件。')
expectIncludes('@row-click="selectReport"', '点击左侧表单行必须切换右侧预览。')
assert.doesNotMatch(page, /type="selection"/, '删除批量删除按钮后不应继续保留仅服务于批量操作的多选列。')
assert.doesNotMatch(page, /@selection-change="handleSelectionChange"/, '删除批量删除按钮后不应继续维护批量选择状态。')
assert.doesNotMatch(page, /批量删除/, '批记录表单列表顶部不得继续显示批量删除按钮。')
assert.doesNotMatch(page, /handleBatchDelete/, '删除批量删除按钮后不应保留废弃 handleBatchDelete。')
assert.doesNotMatch(page, /selectedRows/, '删除批量删除按钮后不应保留废弃 selectedRows 状态。')
assert.doesNotMatch(page, /getUniqueSelectedReports/, '删除批量删除按钮后不应保留批量删除去重逻辑。')
assert.doesNotMatch(page, /deleteSelectedReports/, '删除批量删除按钮后不应保留批量删除请求处理。')
assert.doesNotMatch(page, /是否批量解绑后删除/, '删除批量删除按钮后不应保留批量解绑删除弹窗文案。')

for (const action of ['打开', '编辑', '填写', '签名', '规则', '链接', '重命名', '删除']) {
  expectIncludes(action, `右侧预览顶部必须保留动作：${action}`)
}

const tableStart = page.indexOf('<el-table')
const tableEnd = page.indexOf('</el-table>', tableStart)
assert.notEqual(tableStart, -1, '批记录表单列表必须保留左侧表格。')
assert.notEqual(tableEnd, -1, '批记录表单列表左侧表格必须完整闭合。')
const listTable = page.slice(tableStart, tableEnd)
assert.doesNotMatch(listTable, /label="填写人"/, '左侧批记录表单列表不得再显示“填写人”列。')
assert.doesNotMatch(listTable, /prop="fillRule"/, '左侧批记录表单列表不得再绑定填写人列字段。')
assert.doesNotMatch(listTable, /batch-record-form-filler-cell/, '左侧批记录表单列表不得再显示“未配置 / 配置填写人”入口。')
assert.doesNotMatch(page, /\{\s*key:\s*'fillRule'[\s\S]*?label:\s*'填写人'/, '批记录表单列表默认列配置不得再注册“填写人”。')
assert.doesNotMatch(page, /EdhrProcessFormPermissionRuleApi/, '删除列表“填写人”列后，当前页面不应再为不可见列延迟加载填写人规则。')
assert.match(
  page,
  /<el-button link type="primary" @click="openTemplateAction\(selectedReport, 'cellRules'\)">填写配置<\/el-button>/,
  '需要进入全屏填写配置时必须继续通过右侧“填写配置”动作打开。'
)

expectIncludes('class="batch-record-form-preview__actions"', '右侧预览顶部必须承载表单操作区。')
assert(!page.includes('prop="operation"'), '批记录表单列表不应继续保留操作列。')

for (const mapping of [
  "MAIN: '批记录'",
  "LOSS_REPORT: '损耗单'",
  "PROCESS_INSPECTION: '过程检验单'",
  "PARAMETER_RECORD: '参数记录表'"
]) {
  expectIncludes(mapping, `表单类型映射缺失：${mapping}`)
}

assert.match(api, /productName\?:\s*string/, '前端报表 VO 必须包含产品名称字段。')
assert.match(api, /projectCode\?:\s*string/, '前端报表 VO 必须包含 DCC 项目代码字段。')
assert.match(api, /versionNo\?:\s*string/, '前端报表 VO 必须包含版本号字段。')
assert.match(api, /versionStatus\?:\s*string/, '前端报表 VO 必须包含版本状态字段。')
assert.match(api, /deleteGeneratedReports:\s*async/, '前端 API 必须暴露批量删除接口。')
assert.match(api, /forceUnbind\?:\s*boolean/, '前端批量删除请求必须支持 forceUnbind。')
assert.doesNotMatch(page, /mock|fixture|demo|fallback|默认成功|静默跳过/i, '批记录表单页不得引入 mock、fallback 或静默成功。')

console.log('PASS: eDHR batch record form list static contract')
