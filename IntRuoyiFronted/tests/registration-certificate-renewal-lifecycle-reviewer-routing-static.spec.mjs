import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const testRoot = path.dirname(fileURLToPath(import.meta.url))
const lifecyclePath = path.join(testRoot, 'e2e', 'registration-certificate-renewal-lifecycle-real.spec.js')
const lifecycle = fs.readFileSync(lifecyclePath, 'utf8')

assert.match(
  lifecycle,
  /const APPROVER_ROLE_CODE = 'dcc_registration_certificate_approver'/,
  '生命周期 E2E 必须显式锁定注册部经理角色码。'
)
assert.match(
  lifecycle,
  /const APPROVER_PASSWORD = process\.env\.REG_CERT_E2E_APPROVER_PASSWORD \|\| 'admin123'/,
  '生命周期 E2E 必须使用统一的本地审批密码。'
)
assert.match(
  lifecycle,
  /async function ensureActiveSignatureImage\(page, headers, username\)/,
  '生命周期 E2E 必须在审批前确保签名图片已启用。'
)
assert.match(
  lifecycle,
  /async function getApproverCandidates\(page, headers\)/,
  '生命周期 E2E 必须先读取审批候选人列表。'
)
assert.match(
  lifecycle,
  /\/admin-api\/system\/user\/page'/,
  '生命周期 E2E 必须通过真实接口读取候选人用户。'
)
assert.match(
  lifecycle,
  /\/admin-api\/system\/user\/update-password'/,
  '生命周期 E2E 必须先将候选人密码统一到本地测试密码。'
)
assert.match(
  lifecycle,
  /\/admin-api\/dcc\/electronic-signature-authorizations\/my-image\/upload'/,
  '生命周期 E2E 必须上传候选人的电子签名图片。'
)
assert.match(
  lifecycle,
  /\/admin-api\/dcc\/electronic-signature-authorizations\/my-image\/[^']*\/enable/,
  '生命周期 E2E 必须启用上传后的电子签名图片。'
)
assert.match(
  lifecycle,
  /for \(const candidate of approverCandidates\) \{[\s\S]{0,200}?await updateUserPassword\(page, headers, candidate\.id, APPROVER_PASSWORD\)/,
  '生命周期 E2E 必须在审批前准备所有候选账号。'
)
assert.match(
  lifecycle,
  /async function approveRequestInApprovalCenter\(browser, request, label, candidates, evidence\)/,
  '生命周期 E2E 审批辅助函数必须接收候选人列表。'
)
assert.match(
  lifecycle,
  /for \(const reviewer of candidates\)/,
  '生命周期 E2E 必须逐个尝试候选审批人，而不是绑定单一账号。'
)
assert.match(
  lifecycle,
  /password:\s*APPROVER_PASSWORD/,
  '生命周期 E2E 必须用统一的本地审批密码登录候选人。'
)
assert.match(
  lifecycle,
  /submitAndApproveOldViewAccess\(/,
  '旧证查看审批必须复用同一候选人池。'
)

console.log('registration certificate lifecycle reviewer routing static checks passed')
