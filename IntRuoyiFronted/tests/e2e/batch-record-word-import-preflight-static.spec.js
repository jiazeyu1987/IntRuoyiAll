const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const projectRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(projectRoot, 'src/api/mes/pro/batchrecordreport/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert(
  apiSource.includes('preflightUploadedRoute'),
  '批记录 Word 导入 API 必须提供预检接口，展示当前批记录版本和产线版本。'
)

assert(
  apiSource.includes('new URLSearchParams()') &&
    apiSource.includes("params.append('productNames', productName)") &&
    !apiSource.includes('params: { routeKey, batchRecordName, productNames }'),
  '批记录 Word 导入预检必须用 URLSearchParams 逐个追加 productNames，避免后端 List<String> 绑定缺参。'
)

assert(
  apiSource.includes('const query = params.toString()') &&
    apiSource.includes('recognize-uploaded/preflight?${query}') &&
    !apiSource.includes('\n      params\n') &&
    !apiSource.includes('\n      params,'),
  '批记录 Word 导入预检必须把 URLSearchParams 写入 URL 查询串，避免全局 paramsSerializer 将参数序列化为空。'
)

assert(
  apiSource.includes('rebuildBatchRecord') &&
    apiSource.includes('selectedRouteProductIds') &&
    apiSource.includes('selectedProductNames'),
  '批记录 Word 导入 API 必须显式传递重建批记录与选中产线范围。'
)

assert(
  apiSource.includes('currentBatchRecordHasMainReports'),
  '批记录 Word 导入预检必须返回当前版本是否仍有可见主批记录表单，避免空列表仍提示 V1.0 升版。'
)

assert(
  apiSource.includes("export type BatchRecordWordImportAction = 'REBUILD_V1' | 'UPGRADE'") &&
    apiSource.includes('allowedActions?: BatchRecordWordImportAction[]') &&
    apiSource.includes('recommendedAction?: BatchRecordWordImportAction') &&
    apiSource.includes('latestBatchRecordVersionNo?: string') &&
    apiSource.includes('nextVersionNo?: string') &&
    apiSource.includes('referenceBlockers?: BatchRecordReportReferenceBlockerVO[]'),
  '批记录 Word 导入预检必须返回导入动作、推荐动作、最新已生成版本、下一版本号和历史引用清单。'
)

assert(
  apiSource.includes('importAction: BatchRecordWordImportAction') &&
    apiSource.includes('expectedSourceVersionId?: number') &&
    apiSource.includes("data.append('importAction', importAction)") &&
    apiSource.includes("data.append('expectedSourceVersionId', String(expectedSourceVersionId))"),
  '批记录 Word 导入写接口必须显式传递导入动作和预检源版本 ID，避免再次按旧 upgrade 布尔值推断。'
)

const pageSources = new Map(
  [
    'src/views/mes/pro/batchrecordformlist/index.vue',
    'src/views/mes/pro/batchrecordformlist/index.vue'
  ].map((page) => [page, fs.readFileSync(path.join(projectRoot, page), 'utf8')])
)

for (const [page, source] of pageSources) {

  assert(
    source.includes('preflightUploadedRoute'),
    `${page} 必须在选择 DCC 项目后调用导入预检接口。`
  )
  assert(
    source.includes('rebuildBatchRecord') &&
      source.includes('selectedRouteProductOptionKeys') &&
      source.includes('routeProductOptions'),
    `${page} 必须维护重建批记录和产线选择状态。`
  )
  assert(
    source.includes('hasVisibleCurrentBatchRecordVersion') &&
      source.includes('currentBatchRecordHasMainReports') &&
      source.includes('formatWordImportCurrentBatchRecordVersion') &&
      !source.includes('if (rebuildBatchRecord && wordImportDialog.preflight?.currentBatchRecordVersionNo)'),
    `${page} 当前批记录版本展示和升版确认必须按可见主表单判断，空列表不得提示 V1.0 升版。`
  )
  assert(
    source.includes('selectedAction') &&
      source.includes('REBUILD_V1') &&
      source.includes('UPGRADE') &&
      source.includes('preflight.recommendedAction') &&
      source.includes('expectedSourceVersionId'),
    `${page} 必须支持重建 V1.0 / 升版导入动作选择，并提交预检源版本 ID。`
  )
  assert(
    source.includes('主批记录已上传，请先删除后重新上传') &&
      source.includes("selectedAction === 'REBUILD_V1'") &&
      source.includes('currentBatchRecordHasMainReports'),
    `${page} 仅重建 V1.0 时遇到已有主批记录表单必须阻断，升版导入不能被旧阻断逻辑挡住。`
  )
  assert(
    source.includes('确认批记录升版') &&
      source.includes('resolveWordImportUpgradeVersionMessage') &&
      source.includes('latestBatchRecordVersionNo') &&
      source.includes('nextVersionNo') &&
      source.includes('退出导入'),
    `${page} 升版导入必须明确提示最新已生成版本和目标版本，并允许退出。`
  )
  assert(
    source.includes('最新批记录版本为 ${latestVersion}') &&
      source.includes('当前生效源版本为 ${currentVersion}') &&
      !source.includes('当前生效版本为 ${currentVersion}'),
    `${page} 升版确认必须把最新已生成版本作为用户看到的当前版本，不能把源版本写成当前版本。`
  )
  assert(
    !source.includes("wordImportDialog.selectedAction === 'REBUILD_V1' && wordImportDialog.preflight.referenceBlockers?.length") &&
      !source.includes('cleanupEntrance') &&
      !source.includes('cleanupAction'),
    `${page} 导入弹窗不得展示历史引用位置和处理方式。`
  )
  assert(
    source.includes('未选择重建内容') && source.includes('return false'),
    `${page} 全部取消选择时必须直接退出，不调用导入写接口。`
  )
  assert(
    source.includes('confirmWordImportUpgradeSelections') &&
      source.includes('跳过该项') &&
      source.includes('退出导入') &&
      source.includes('const buildWordImportConfirmedSelection') &&
      source.includes('return buildWordImportConfirmedSelection({') &&
      source.includes('importAction: selection.importAction') &&
      source.includes('expectedSourceVersionId: selection.expectedSourceVersionId'),
    `${page} 升版项必须逐项提示，并允许跳过该项或退出导入，所有继续导入分支必须返回完整导入动作结构。`
  )
  assert(
    source.includes('wordImportDialog.visible = false') &&
      source.includes('const dialogClosed = await closeWordImportDialogBeforeMessageBox()') &&
      source.includes('if (confirmedSelection) {') &&
      source.includes('wordImportDialog.visible = true'),
    `${page} 点击确定后必须先关闭导入弹框再显示升版确认，取消确认时恢复导入弹框。`
  )
  assert(
    source.includes('ElLoading.service') &&
      source.includes('正在导入 Word，请稍候...') &&
      source.includes('loadingInstance.close()'),
    `${page} 真正执行 Word 导入期间必须显示全屏运行中提示。`
  )
}

const templateSource = pageSources.get('src/views/mes/pro/batchrecordformlist/index.vue')
assert(
  templateSource.includes('最新批记录版本') && templateSource.includes('当前工艺流程版本'),
  '批记录表单列表必须展示最新批记录版本和当前工艺流程版本。'
)
assert(
  templateSource.includes('isWordImportActionAllowed'),
  '批记录模板页必须按预检结果控制重建 V1.0 / 升版导入动作。'
)

assert(
  templateSource.includes('hasWordImportAllowedAction') &&
    templateSource.includes('resolveWordImportActionLockedMessage') &&
    templateSource.includes("latestBatchRecordVersionStatus === 'PENDING_APPROVAL'") &&
    templateSource.includes('!isWordImportActionAllowed(wordImportDialog.selectedAction)') &&
    templateSource.includes('wordImportDialog.preflight && !hasWordImportAllowedAction'),
  '批记录模板页在预检返回无允许动作时必须禁用确认并提示待审批只能等待或撤回，不能继续普通导入。'
)

const formListSource = pageSources.get('src/views/mes/pro/batchrecordformlist/index.vue')
assert(
  formListSource.includes('<el-form-item v-if="isMainWordImport" label="导入内容">') &&
    formListSource.includes('<div class="batch-record-word-import-form__route-title">工艺流程</div>') &&
    formListSource.includes('v-model="wordImportDialog.rebuildBatchRecord"') &&
    formListSource.includes('batch-record-word-import-form__file-state') &&
    formListSource.includes('正在预检 Word 文件'),
  '批记录表单列表导入弹窗必须保留文件预检反馈，并恢复批记录表单与工艺流程选择。'
)

console.log('PASS: batch-record Word import preflight static contract')
