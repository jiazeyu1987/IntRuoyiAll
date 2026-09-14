import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd().endsWith('IntRuoyiFronted') ? process.cwd() : join(process.cwd(), 'IntRuoyiFronted')
const actionPanel = readFileSync(join(root, 'src/views/dcc/registration-certificate/workflow/ActionPanel.vue'), 'utf8')

assert.match(
  actionPanel,
  /<el-tab-pane label="访问申请" name="access">/,
  '访问申请面板必须保留，普通用户仍可提交查看旧证或下载文件申请'
)

for (const forbidden of [
  'label="审批结果"',
  'registration-certificate-approval-result-action',
  'registration-certificate-workflow__grants',
  'handleDownloadGrant',
  'handleRevokeGrant',
  '撤销授权'
]) {
  assert.doesNotMatch(
    actionPanel,
    new RegExp(forbidden.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `访问申请面板不得保留独立审批结果/授权治理控件：${forbidden}`
  )
}

assert.doesNotMatch(
  actionPanel,
  /downloadRegistrationCertificateFile/,
  '访问申请面板不得直接下载注册证文件，下载应回到详情附件区按授权状态展示'
)

assert.doesNotMatch(
  actionPanel,
  /revokeRegistrationCertificateGrant/,
  '访问申请面板不得撤销注册证下载授权'
)

console.log('PASS: registration certificate access-request panel hides approval-result controls')
