import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const pagePath = path.join(root, 'src', 'views', 'mes', 'pro', 'batchrecordtemplate', 'index.vue')
const designerWrapperPath = path.join(
  root,
  'src',
  'views',
  'mes',
  'pro',
  'batchrecordtemplate',
  'DesignerWrapper.vue'
)
const localTemplateApiPath = path.join(root, 'src', 'api', 'mes', 'pro', 'batchrecordtemplate', 'index.ts')
const previewPath = path.join(
  root,
  'src',
  'views',
  'mes',
  'pro',
  'batchrecordtemplate',
  'TemplateLayoutPreview.vue'
)

test('electronic batch record page shows jimu report list while keeping designer wrapper entry', () => {
  const source = fs.readFileSync(pagePath, 'utf8')
  const apiPath = path.join(root, 'src', 'api', 'mes', 'pro', 'batchrecordreport', 'index.ts')
  const apiSource = fs.readFileSync(apiPath, 'utf8')
  const wrapperSource = fs.readFileSync(designerWrapperPath, 'utf8')

  assert.match(source, /DesignerWrapper/, 'page must still keep DesignerWrapper entry')
  assert.match(source, /mode === 'designer'|route\.query\.mode === 'designer'/, 'page must still support designer mode')
  assert.match(source, /BatchRecordReportApi/, 'page must use batch-record-report API')
  assert.match(source, /getGeneratedReportPage/, 'page must load jimu report list from batch-record-report page API')
  assert.match(source, /getCellRules/, 'page must load cell rules for local preview rendering')
  assert.match(source, /getSignatureCellMarkers/, 'page must load signature markers for local preview rendering')
  assert.match(source, /EdhrExecutionReadonlyForm/, 'page must reuse eDHR readonly form for inline preview')
  assert.match(source, /templatePreview\.formViewModel/, 'inline preview must render through a local form view model')
  assert.match(source, /fit-to-viewport/, 'inline preview must fit the rendered template to available width')
  assert.match(source, /executionSnapshotJson: JSON\.stringify\(\{/, 'local preview must build execution snapshot json')
  assert.match(source, /cellValuesJson:\s*'\[\]'/, 'local preview must render as readonly empty-value view before simulation')
  assert.doesNotMatch(source, /<IFrame/, 'page must no longer depend on iframe preview for the right-side template panel')
  assert.doesNotMatch(source, /EdhrExecutionTemplateGuide/, 'page must not replace the real form with a layout guide')
  assert.doesNotMatch(source, /batch-record-toolbar-shell/, 'page must hide the top toolbar in three-column layout')
  assert.match(source, /reportName|报表名称/, 'page must render report name column')
  assert.match(source, /batch-record-report-list__item/, 'page must render the report list as selectable name items')
  assert.match(source, /batch-record-report-list__name/, 'page must render the report list as a single-name list')
  assert.match(source, /BATCH_RECORD_REPORT_LIST_PAGE_SIZE\s*=\s*200/, 'report list requests must respect backend max pageSize 200')
  assert.match(source, /getAllReportsForSelectedBatchRecord/, 'page must internally load all report name pages')
  assert.match(source, /pageNo:\s*currentPageNo/, 'report list query must iterate backend pages')
  assert.match(source, /pageSize:\s*BATCH_RECORD_REPORT_LIST_PAGE_SIZE/, 'report list query must use the backend max page size')
  assert.match(source, /while \(mergedList\.length < totalCount\)/, 'report list must merge backend pages until all rows are loaded')
  assert.doesNotMatch(source, /BATCH_RECORD_REPORT_LIST_PAGE_SIZE\s*=\s*1000/, 'report list must not exceed backend max pageSize')
  assert.doesNotMatch(source, /batch-record-report-name-table/, 'page must not render the report list as a table')
  assert.doesNotMatch(source, /<Pagination/, 'report list must not render pagination controls')
  assert.doesNotMatch(source, /v-model:limit="queryParams\.pageSize"/, 'report list must not expose page size selector')
  assert.doesNotMatch(source, /label="报表编码"/, 'report list must not render report code column')
  assert.doesNotMatch(source, /label="来源文件名"/, 'report list must not render source file column')
  assert.doesNotMatch(source, /label="最近修改时间"/, 'report list must not render update time column')
  assert.doesNotMatch(source, /<el-table-column label="操作"/, 'report list actions must move to selected report detail area')
  assert.match(source, /打开/, 'page must keep open action')
  assert.match(source, /编辑/, 'page must add edit action')
  assert.match(source, /重命名/, 'page must add rename action')
  assert.match(source, /删除/, 'page must add delete action')
  assert.match(source, /handleDelete\(selectedReport\)/, 'selected report detail area must keep delete behavior')
  assert.match(source, /删除全部批记录模板/, 'page must expose delete-all template action')
  assert.match(source, /handleDeleteAll/, 'page must provide delete-all behavior')
  assert.match(source, /message\.prompt/, 'delete-all behavior must require typed confirmation')
  assert.match(source, /confirmation\.trim\(\)\s*!==\s*'PROD'/, 'delete-all must require PROD confirmation')
  assert.match(source, /BatchRecordReportApi\.deleteAllGeneratedReports\('PROD'\)/, 'delete-all must pass PROD to backend')
  assert.match(source, /skippedBoundReportCount/, 'delete-all success message must show retained bound templates')
  assert.match(apiSource, /skippedBoundReportCount/, 'delete-all API type must expose retained bound count')
  assert.match(source, /reportMode/, 'page must provide explicit report mode routing')
  assert.match(source, /handleRename/, 'page must provide rename behavior')
  assert.match(source, /deleteGeneratedReport/, 'page must reuse report delete API')
  assert.match(apiSource, /renameGeneratedReport/, 'report API must expose rename endpoint')
  assert.match(apiSource, /getEditPath/, 'report API must expose edit-path endpoint')
  assert.match(wrapperSource, /route\.query\.reportMode === 'edit'/, 'DesignerWrapper must branch on edit mode')
  assert.match(wrapperSource, /getEditPath\(reportId\)/, 'DesignerWrapper must request edit path in edit mode')
  assert.match(wrapperSource, /getDesignerPath\(reportId\)/, 'DesignerWrapper must keep preview path in preview mode')
  assert.doesNotMatch(source, /MesProBatchRecordTemplateApi/, 'page must not import local template API')
  assert.doesNotMatch(source, /TemplateLayoutPreview/, 'page must not render local template preview component')
  assert.doesNotMatch(source, /文件解析导入/, 'page must not expose local template import tab')
  assert.doesNotMatch(source, /上传 Word|提交选中|查看版式/, 'page must not expose local template generation actions')
})

test('local template frontend helper files are removed while designer wrapper remains', () => {
  assert.equal(fs.existsSync(designerWrapperPath), true, 'DesignerWrapper must remain for jmreport designer mode')
  assert.equal(fs.existsSync(localTemplateApiPath), false, 'local template API module must stay removed')
  assert.equal(fs.existsSync(previewPath), false, 'local template preview component must stay removed')
})
