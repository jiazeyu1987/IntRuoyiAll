import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const backendRoot = path.resolve(root, '..', 'ruoyi-vue-pro')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackendText = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')
const readOptionalText = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}
const countRoutePathDefinitions = (source, routePath) =>
  Array.from(source.matchAll(new RegExp(`path:\\s*'${routePath.replaceAll('/', '\\/')}'`, 'g'))).length
const extractInterfaceBody = (source, interfaceName) => {
  const match = source.match(new RegExp(`interface\\s+${interfaceName}\\s+extends\\s+PageParam\\s*{([\\s\\S]*?)\\n}`))
  assert.ok(match, `必须声明 ${interfaceName}`)
  return match[1]
}

test('BDD: 追踪与签名可见 -> 页面展示 eDHR 追踪与签名分页，不跳转 DCC 页面', () => {
  const trackingApi = readOptionalText('src/api/mes/pro/edhr/tracking.ts')
  const signatureApi = readOptionalText('src/api/mes/pro/edhr/signatures.ts')
  const trackingPage = readOptionalText('src/views/mes/pro/edhr/TrackingPage.vue')
  const signaturePage = readOptionalText('src/views/mes/pro/edhr/SignaturePage.vue')
  const detailPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')
  const routerSource = readText('src/router/modules/remaining.ts')
  const source = `${trackingApi}\n${signatureApi}\n${trackingPage}\n${signaturePage}\n${detailPage}`

  assert.match(trackingApi, /tracking-page/, '必须提供 eDHR 追踪分页 helper')
  assert.match(trackingApi, /tracking-timeline/, '必须提供 eDHR 追踪时间线 helper')
  assert.match(trackingApi, /FORM_REVIEW[\s\S]*SUBMIT[\s\S]*APPROVE[\s\S]*REJECT[\s\S]*ARCHIVE_SEAL/s, '追踪事件必须覆盖复核、提交、审批、驳回和归档')
  assert.match(signatureApi, /signature-page/, '必须提供 eDHR 签名分页 helper')
  assert.match(signatureApi, /meaningText/, '签名含义必须由后端 meaningText 返回')
  assert.match(signatureApi, /FIELD_CHANGE[\s\S]*FORM_REVIEW[\s\S]*SUBMIT[\s\S]*APPROVE[\s\S]*REJECT[\s\S]*ARCHIVE_SEAL/s, '签名动作必须覆盖字段变更、复核、提交、审批、驳回和归档')
  assert.match(trackingPage, /执行编号[\s\S]*生产上下文[\s\S]*当前阶段[\s\S]*最后处理[\s\S]*归档状态/s, '追踪页必须展示执行上下文、当前阶段和归档状态')
  assert.match(trackingPage, /lastEventReason/, '追踪页必须展示最后事件意见/原因字段')
  assert.match(signaturePage, /业务记录[\s\S]*签名含义[\s\S]*签名人[\s\S]*签名确认/s, '签名页必须展示合规签名字段')
  assert.match(signaturePage, /passwordVerified/, '签名页必须展示密码校验结果字段')
  assert.match(detailPage, /追踪[\s\S]*签名记录/s, '执行详情必须提供追踪和签名记录视图')
  assert.equal(countRoutePathDefinitions(routerSource, 'pro/feedback/edhr-tracking'), 1, 'eDHR 追踪菜单页必须保留一个可直达静态路由')
  assert.equal(countRoutePathDefinitions(routerSource, 'pro/feedback/edhr-signatures'), 1, 'eDHR 签名菜单页必须保留一个可直达静态路由')
  assert.match(source, /path:\s*'\/mes\/pro\/feedback\/edhr-execution\/form'/, '追踪页进入执行表单只读追踪视图必须使用保留的 form path')
  assert.doesNotMatch(source, /\/dcc\/|controlled-file|Dcc/i, '不得跳转或复用 DCC 页面作为 eDHR 追踪/签名视图')
})

test('BDD: 追踪/签名后端契约 -> 前端 helper 与后端 endpoint、必填字段一致', () => {
  const trackingApi = readOptionalText('src/api/mes/pro/edhr/tracking.ts')
  const signatureApi = readOptionalText('src/api/mes/pro/edhr/signatures.ts')
  const trackingPage = readOptionalText('src/views/mes/pro/edhr/TrackingPage.vue')
  const signaturePage = readOptionalText('src/views/mes/pro/edhr/SignaturePage.vue')
  const controller = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionController.java')
  const trackingResp = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionTrackingRespVO.java')
  const signatureResp = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionSignatureRespVO.java')

  assert.match(controller, /@GetMapping\("\/tracking-page"\)/, '后端必须提供 tracking-page endpoint')
  assert.match(controller, /@GetMapping\("\/tracking-timeline"\)/, '后端必须提供 tracking-timeline endpoint，详情时间线不得调用不存在的接口')
  assert.match(controller, /@GetMapping\("\/signature-page"\)/, '后端必须提供 signature-page endpoint')
  assert.match(trackingApi, /url:\s*'\/mes\/pro\/batch-record-execution\/tracking-page'/, '前端 tracking-page helper 必须调用 MES eDHR 自有接口')
  assert.match(trackingApi, /url:\s*'\/mes\/pro\/batch-record-execution\/tracking-timeline'/, '前端 tracking-timeline helper 必须调用 MES eDHR 自有接口')
  assert.match(signatureApi, /url:\s*'\/mes\/pro\/batch-record-execution\/signature-page'/, '前端 signature-page helper 必须调用 MES eDHR 自有接口')

  for (const field of ['executionId', 'executionCode', 'workOrderId', 'workOrderCode', 'batchId', 'batchCode', 'status', 'processInstanceId', 'lastEventReason']) {
    assert.match(trackingResp, new RegExp(`private\\s+[^;]+\\s+${field};`), `tracking 后端 VO 必须返回 ${field}`)
    assert.match(trackingApi, new RegExp(`\\b${field}\\b`), `tracking 前端类型/页面必须消费 ${field}`)
  }

  for (const field of ['id', 'executionId', 'actorId', 'actorName', 'actionType', 'signatureMode', 'passwordVerified', 'processInstanceId', 'bpmTaskId', 'approvalResult', 'signedAt']) {
    assert.match(signatureResp, new RegExp(`private\\s+[^;]+\\s+${field};`), `signature 后端 VO 必须返回 ${field}`)
    assert.match(signatureApi, new RegExp(`\\b${field}\\b`), `signature 前端类型必须声明 ${field}`)
  }

  assert.match(signatureResp, /private\s+String\s+meaningText;/, '签名合规含义 meaningText 必须由后端返回，前端不得拼接')
  assert.match(signaturePage, /meaningText/, '签名页必须展示后端 meaningText')
  assert.match(trackingPage, /执行编号[\s\S]*生产上下文[\s\S]*当前阶段[\s\S]*最后处理[\s\S]*归档状态/s, '追踪页必须展示执行上下文、当前阶段和归档状态')
  assert.match(trackingPage, /最后处理[\s\S]*lastEventReason/, '追踪页必须展示后端 lastEventReason')
})

test('BDD: 追踪列表按最后操作展示 -> 节点为工序、处理人为最后操作者、事件为中文', () => {
  const trackingApi = readOptionalText('src/api/mes/pro/edhr/tracking.ts')
  const trackingPage = readOptionalText('src/views/mes/pro/edhr/TrackingPage.vue')
  const executionService = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java')

  assert.match(trackingApi, /\bworkOrderId:\s*number\b/, 'tracking 前端行类型必须声明 workOrderId')
  assert.match(trackingApi, /\bbatchId\??:\s*number\b/, 'tracking 前端行类型必须声明 batchId')
  assert.match(trackingApi, /\|\s*'FIELD_CHANGE'/, 'tracking 事件类型必须覆盖字段变更')
  assert.match(trackingPage, /TRACKING_EVENT_LABELS/, '追踪页必须维护最后事件中文映射')
  for (const label of ['填写提交签名', '审批通过', '审批驳回', '归档封存', '字段变更', '表单复核']) {
    assert.match(trackingPage, new RegExp(label), `追踪页最后事件映射必须包含 ${label}`)
  }
  assert.match(trackingPage, /formatTrackingEvent/, '追踪页最后事件列必须通过格式化函数展示')
  assert.match(trackingPage, /未知追踪最后事件/, '未知最后事件必须明确报错，不得静默显示英文编码')
  assert.doesNotMatch(trackingPage, /TRACKING_EVENT_LABELS\[eventType\]\s*\|\|\s*eventType/, '最后事件不得回退直出英文 actionType 编码')
  assert.doesNotMatch(trackingPage, /<el-table-column label="最后事件" prop="lastEventType"/, '最后事件列不能直出英文 actionType 编码')
  assert.match(trackingPage, /formatTrackingLastEventAt/, '最后处理时间必须通过格式化函数展示')
  assert.match(trackingPage, /formatDate\(.*,\s*'YYYY年M月D日'\)/s, '最后处理时间必须显示为年月日格式')
  assert.doesNotMatch(trackingPage, /<el-table-column label="最后处理时间" prop="lastEventAt"/, '最后处理时间列不能直出 lastEventAt 原始值')
  assert.match(trackingPage, /BatchForm/, '追踪页必须复用现有 BatchForm 作为批次详情弹窗')
  assert.match(trackingPage, /path:\s*'\/mes\/pro\/work-order'/, '工单号必须跳转现有生产工单页')
  assert.match(executionService, /resolveTrackingCurrentNodeName\(detail,\s*lastSignature\)/, '后端当前节点必须优先解析执行记录对应工序名称')
  assert.match(executionService, /setCurrentNodeName\(currentNodeName\)/, '后端当前节点必须写入解析后的业务名称')
  assert.match(executionService, /setCurrentAssigneeNames\(List\.of\(lastSignature\.getActorName\(\)\)\)/, '后端当前处理人必须来自最后一次签名/操作记录 actorName')
  assert.doesNotMatch(executionService, /setCurrentNodeName\(snapshot\s*==\s*null\s*\?\s*null\s*:\s*snapshot\.getCurrentTaskDefinitionKey\(\)\)/, '后端当前节点不得继续展示 BPM 技术 key')
})

test('BDD: 追踪/签名筛选真实有效 -> 页面只展示并发送后端真实支持的查询字段', () => {
  const trackingApi = readOptionalText('src/api/mes/pro/edhr/tracking.ts')
  const signatureApi = readOptionalText('src/api/mes/pro/edhr/signatures.ts')
  const trackingPage = readOptionalText('src/views/mes/pro/edhr/TrackingPage.vue')
  const signaturePage = readOptionalText('src/views/mes/pro/edhr/SignaturePage.vue')
  const trackingReq = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionTrackingPageReqVO.java')
  const signatureReq = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionSignaturePageReqVO.java')
  const executionService = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java')
  const trackingMapper = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProBatchRecordExecutionMapper.java')
  const signatureMapper = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProBatchRecordExecutionSignatureMapper.java')
  const trackingReqBody = extractInterfaceBody(trackingApi, 'EdhrTrackingPageReqVO')
  const signatureReqBody = extractInterfaceBody(signatureApi, 'EdhrSignaturePageReqVO')

  for (const field of [
    'executionCode',
    'workOrderCode',
    'batchCode',
    'processId',
    'workstationId',
    'status',
    'submittedBy',
    'approvedBy',
    'processInstanceId',
    'actorName',
    'occurredAtStart',
    'occurredAtEnd'
  ]) {
    assert.match(trackingReq, new RegExp(`private\\s+[^;]+\\s+${field};`), `后端 tracking ReqVO 必须声明 ${field}`)
    assert.match(trackingReqBody, new RegExp(`\\b${field}\\??:`), `前端 tracking 请求类型必须声明 ${field}`)
  }
  assert.match(executionService, /resolveRouteProcessIds\(pageReqVO\.getProcessId\(\)\)/, '后端 tracking service 必须把 processId 转换为真实 routeProcessId 过滤')
  assert.match(executionService, /resolveExecutionIdsByActorName\(pageReqVO\.getActorName\(\)\)/, '后端 tracking service 必须把 actorName 转换为真实执行记录过滤')
  assert.match(trackingMapper, /getExecutionCode\(\)[\s\S]*getWorkOrderCode\(\)[\s\S]*getBatchCode\(\)[\s\S]*getProcessInstanceId\(\)[\s\S]*inIfPresent\(MesProBatchRecordExecutionDO::getRouteProcessId,\s*routeProcessIds\)[\s\S]*inIfPresent\(MesProBatchRecordExecutionDO::getId,\s*actorMatchedExecutionIds\)[\s\S]*getWorkstationId\(\)[\s\S]*getStatus\(\)[\s\S]*getSubmittedBy\(\)[\s\S]*getApprovedBy\(\)[\s\S]*getOccurredAtStart\(\)[\s\S]*getOccurredAtEnd\(\)/s, '后端 tracking mapper 必须真实应用前端筛选字段')

  for (const field of ['executionId', 'executionCode', 'actionType', 'actorId', 'actorName', 'processInstanceId', 'bpmTaskId', 'signedAtStart', 'signedAtEnd']) {
    assert.match(signatureReq, new RegExp(`private\\s+[^;]+\\s+${field};`), `后端 signature ReqVO 必须声明 ${field}`)
    assert.match(signatureReqBody, new RegExp(`\\b${field}\\??:`), `前端 signature 请求类型必须声明 ${field}`)
  }
  assert.match(executionService, /resolveExecutionIdsByExecutionCode\(pageReqVO\.getExecutionCode\(\)\)/, '后端 signature service 必须把 executionCode 转换为真实签名记录过滤')
  assert.match(signatureMapper, /getExecutionId\(\)[\s\S]*inIfPresent\(MesProBatchRecordExecutionSignatureDO::getExecutionId,\s*executionIds\)[\s\S]*getActionType\(\)[\s\S]*getActorId\(\)[\s\S]*getActorName\(\)[\s\S]*getProcessInstanceId\(\)[\s\S]*getBpmTaskId\(\)[\s\S]*getSignedAtStart\(\)[\s\S]*getSignedAtEnd\(\)/s, '后端 signature mapper 必须真实应用前端筛选字段')
})

test('BDD: 追踪时间线按后端 occurredAt 升序渲染 -> 前端不得反向重排时间线', () => {
  const executionPage = readOptionalText('src/views/mes/pro/edhr/ExecutionPage.vue')
  const approvalDetailPage = readOptionalText('src/views/mes/pro/edhr/ApprovalDetailPage.vue')
  const timelineSource = `${executionPage}\n${approvalDetailPage}`

  assert.match(timelineSource, /trackingTimeline\.value\s*=\s*await getEdhrTrackingTimeline/, '详情页必须直接使用后端 tracking-timeline 数据渲染')
  assert.doesNotMatch(timelineSource, /trackingTimeline[\s\S]{0,200}\.reverse\s*\(/, '时间线不得通过 reverse() 假设后端倒序')
  assert.doesNotMatch(timelineSource, /\.sort\s*\([^)]*occurredAt[\s\S]{0,160}right[\s\S]{0,160}left/, '时间线不得按 occurredAt 做反向排序')
  assert.doesNotMatch(timelineSource, /new Date\([^)]*right\.occurredAt[\s\S]{0,160}-[\s\S]{0,160}new Date\([^)]*left\.occurredAt/s, '时间线不得按 occurredAt 做降序排序')
})
