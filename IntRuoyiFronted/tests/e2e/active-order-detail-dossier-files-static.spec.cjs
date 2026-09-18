const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const panelPath = path.join(
  root,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const apiPath = path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts')
const filePreviewPath = path.join(root, 'src/api/common/filePreview.ts')
const pqcPagePath = path.join(root, 'src/views/mes/pro/production-release/PqcProductionReleasePage.vue')
const backendServicePath = path.resolve(
  root,
  '..',
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java'
)
const backendProviderPath = path.resolve(
  root,
  '..',
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierBusinessFileAccessProvider.java'
)
const dccPreviewControllerPath = path.resolve(
  root,
  '..',
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/filepreview/DccOnlineFilePreviewController.java'
)

const panel = fs.readFileSync(panelPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
const filePreview = fs.readFileSync(filePreviewPath, 'utf8')
const pqcPage = fs.readFileSync(pqcPagePath, 'utf8')
const backendService = fs.readFileSync(backendServicePath, 'utf8')
const backendProvider = fs.existsSync(backendProviderPath) ? fs.readFileSync(backendProviderPath, 'utf8') : ''
const dccPreviewController = fs.readFileSync(dccPreviewControllerPath, 'utf8')

for (const label of ['来料检文件', '灭菌文件', '成品检文件', '其他文件']) {
  assert(
    panel.includes(`label="${label}"`) || panel.includes(`label: '${label}'`),
    `共享详情面板缺少 ${label} 页签`
  )
}

const productionWorkOrderIndex = panel.indexOf('label="生产工单"')
const incomingIndex = panel.indexOf('来料检文件')
assert(productionWorkOrderIndex >= 0, '共享详情面板缺少生产工单页签')
assert(incomingIndex > productionWorkOrderIndex, '资料文件页签必须在生产工单之后')

for (const marker of [
  'data-active-order-dossier-file-upload',
  'data-active-order-dossier-file-preview',
  'data-active-order-dossier-file-delete'
]) {
  assert(panel.includes(marker), `共享详情面板缺少 ${marker} 动作标记`)
}

for (const fn of [
  'getActiveOrderDossierFiles',
  'uploadActiveOrderDossierFile',
  'deleteActiveOrderDossierFile'
]) {
  assert(api.includes(`export const ${fn}`), `teamLeader API 缺少 ${fn}`)
}

assert(
  api.includes('/mes/pro/process-pool/team-leader/active-order/dossier-files'),
  'teamLeader API 必须调用正式活跃订单资料文件接口'
)
assert(
  pqcPage.includes(':pqc-release-application-id=') ||
    pqcPage.includes('pqcReleaseApplicationId'),
  'PQC生产放行详情必须把 applicationId 传给共享详情面板'
)

assert(filePreview.includes("type: 'MES_ACTIVE_ORDER_DOSSIER_FILE'"),
  '统一预览来源必须定义活跃订单资料文件类型')
assert(filePreview.includes('buildMesActiveOrderDossierFilePreviewSource'),
  '统一预览来源必须提供活跃订单资料文件构造器')
assert(panel.includes('buildMesActiveOrderDossierFilePreviewSource'),
  '资料文件预览必须使用活跃订单资料文件来源')
assert(!panel.includes('buildEdhrSpecialNodeAttachmentPreviewSource'),
  '资料文件预览不得冒充 eDHR 特殊节点附件')

assert(!panel.includes('dossierFiles?.batchExecutionCode'), '资料文件页签不得显示P2来源批次')
assert(!api.match(/export interface ActiveOrderDossierFileCategoryVO[\s\S]*?batchTaskId/),
  '资料文件分类响应不得暴露P2任务编号')
assert(!api.match(/export interface ActiveOrderDossierFilesRespVO[\s\S]*?batchExecutionId/),
  '资料文件响应不得暴露P2批次编号')
assert(backendService.includes('MesProcessPoolActiveOrderDossierFileMapper'),
  '后端资料文件服务必须使用活跃订单资料文件归属 Mapper')
assert(!backendService.includes('MesProBatchRecordExecutionAttachmentMapper'),
  '后端资料文件服务不得写正式批记录附件表')
assert(!backendService.includes('resolveUniqueBatch'), '资料文件服务不得解析P2批次')
assert(!backendService.includes('resolveRequiredTasks'), '资料文件服务不得解析P2资料节点')
assert(!backendService.includes('MesPqcProductionReleaseService'),
  '活跃订单资料文件服务不得读取正式PQC放行任务或P2前置')
assert(backendService.includes('requireReadableFile'),
  '资料文件服务必须提供基于活跃订单归属的预览读取授权')
assert(backendProvider.includes('BusinessFileAccessProvider'),
  '活跃订单资料文件必须接入统一文件业务权限提供者')
assert(backendProvider.includes('requireReadableFile'),
  '统一文件预览必须复用活跃订单资料文件授权')
for (const permission of [
  'mes:pro-process-pool-team-leader:query',
  'mes:pro-production-release:query'
]) {
  assert(dccPreviewController.includes(permission),
    `统一预览接口缺少 ${permission} 读取权限`)
}

console.log('active-order-detail-dossier-files static contract passed')
