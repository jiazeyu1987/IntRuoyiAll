import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const backendRoot = path.resolve(root, '..', 'IntRuoyiBackend')
const read = p => fs.readFileSync(path.join(root, p), 'utf8')
const backend = p => fs.readFileSync(path.join(backendRoot, p), 'utf8')

test('旧 eDHR 菜单保留待办已办语义并唯一进入正式审批中心', () => {
  const shell = read('src/views/mes/pro/edhr/ApprovalPage.vue')
  assert.match(shell, /router\.replace/)
  assert.match(shell, /path:\s*'\/approval-center'/)
  assert.match(shell, /moduleCode:\s*'EDHR'/)
  assert.match(shell, /route\.query\.tab === 'done' \? 'DONE' : 'TODO'/)
  assert.doesNotMatch(shell, /approveEdhr|rejectEdhr|request\.(put|post)|<el-table/)
})

test('中心列表按真实模块和视图筛选，表单详情仍保持独立入口', () => {
  const api = read('src/api/approval-center/index.ts')
  const center = read('src/views/approval-center/index.vue')
  const detail = read('src/views/mes/pro/edhr/ApprovalDetailPage.vue')
  assert.match(api, /interface ApprovalTaskPageReqVO[\s\S]*viewType: ApprovalTaskViewType[\s\S]*moduleCode\?: ApprovalModuleCode[\s\S]*keyword\?: string/)
  assert.match(center, /getApprovalTaskPage/)
  assert.match(center, /queryParams\.viewType/)
  assert.match(detail, /workTaskId/)
  assert.match(detail, /signatureCellKey/)
})

test('审批动作基于真实可用动作并要求密码、冻结预览和完整回执', () => {
  const center = read('src/views/approval-center/index.vue')
  for (const token of ['availableActions', 'password', 'getApprovalReviewPreview', 'prepareId', 'subjectVersion', 'expectedContentHash', 'contentPolicyVersion', 'receiptId', 'signatureId']) {
    assert.ok(center.includes(token), token)
  }
  assert.match(center, /expectedSubjectVersion:\s*reviewPreview\.value\.content\.subjectVersion/)
  assert.match(center, /expectedContentHash:\s*reviewPreview\.value\.content\.contentHash/)
  assert.doesNotMatch(center, /\/bpm\/task\/(approve|reject)/)
})

test('动态菜单组件与规范化路由匹配正式 redirect shell', () => {
  const router = read('src/utils/routerHelper.ts')
  assert.match(router, /APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_COMPONENTS[\s\S]*'mes\/pro\/edhr\/ApprovalPage'/)
  assert.match(router, /APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_PATHS[\s\S]*'pro\/feedback\/edhr-approval'/)
  assert.match(router, /normalizeInternalComponentPath/)
  assert.match(router, /normalizeInternalRoutePath/)
  for (const component of ['ApprovalPage', 'ApprovalDetailPage', 'TrackingPage', 'SignaturePage']) {
    assert.ok(fs.existsSync(path.join(root, `src/views/mes/pro/edhr/${component}.vue`)))
  }
})

test('正式中心 V2 请求与不可变 receipt 后端保持单一协议', () => {
  const api = read('src/api/approval-center/index.ts')
  const req = backend('yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/approval/ApprovalTaskReviewReqVO.java')
  for (const field of ['prepareId', 'idempotencyKey', 'expectedSubjectVersion', 'expectedContentHash', 'contentPolicyVersion']) {
    assert.ok(api.includes(field), `frontend ${field}`)
    assert.ok(req.includes(field), `backend ${field}`)
  }
  for (const field of ['receiptId', 'signatureId', 'evidenceHash', 'resultCode', 'resultingStatus', 'committedAt']) assert.ok(api.includes(field), field)
  assert.match(api, /\/approval-center\/tasks\/review/)
})
