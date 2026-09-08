const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const csvPanePath = path.join(
  root,
  'src/views/signature-governance/components/CsvPackageGovernanceListPane.vue'
)
const csvPane = fs.readFileSync(csvPanePath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  csvPane,
  /const approvalEvidenceEntrypoints[\s\S]*?=\s*\[/,
  'CSV质量包页签必须集中定义审批/审查证据导出入口。'
)

const entrypointBlockMatch = csvPane.match(
  /const approvalEvidenceEntrypoints[\s\S]*?=\s*\[[\s\S]*?\n\]/
)
assert.ok(entrypointBlockMatch, '必须存在可静态审查的 approvalEvidenceEntrypoints 数组。')
const entrypointBlock = entrypointBlockMatch[0]

for (const entrypoint of [
  {
    id: 'dcc-signature-evidence',
    label: 'DCC签名证据PDF',
    path: '/signature-governance/signature-records',
    permission: 'dcc:controlled-file:signature:manage'
  },
  {
    id: 'trusted-time-evidence',
    label: '可信时间戳证据ZIP',
    path: '/infra/runtime-control',
    permission: 'infra:runtime-control:operate'
  },
  {
    id: 'backup-review-evidence',
    label: '备份审查证据ZIP',
    path: '/system/backup-plan',
    permission: 'system:backup-plan:evidence-export'
  },
  {
    id: 'account-security-evidence',
    label: '账号安全与通用账号证据',
    path: '/system/user',
    permission: 'system:user:export'
  },
  {
    id: 'edhr-signature-records',
    label: 'eDHR签名记录',
    path: '/mes/pro/feedback/edhr-signatures',
    permission: 'mes:pro-batch-record-execution:signature-query'
  },
  {
    id: 'edhr-field-audit-evidence',
    label: 'eDHR字段审计证据',
    path: '/mes/pro/feedback/edhr-field-audit',
    permission: 'mes:pro-batch-record-execution:field-audit-query'
  },
  {
    id: 'approval-sequence-evidence',
    label: '审批中心顺序证据',
    path: '/approval-center/done',
    permission: 'bpm:task:query'
  },
  {
    id: 'edhr-archive-evidence',
    label: 'eDHR批记录归档',
    path: '/mes/pro/feedback/edhr-batch-history',
    permission: 'mes:pro-edhr-batch-execution:query'
  },
  {
    id: 'edhr-permission-matrix-evidence',
    label: 'eDHR权限矩阵证据',
    path: '/mes/pro/feedback/edhr-permission-matrix',
    permission: 'mes:pro-edhr-permission-scope:evaluate'
  }
]) {
  assert.match(entrypointBlock, new RegExp(`id:\\s*'${entrypoint.id}'`), `${entrypoint.label} 必须有稳定入口ID。`)
  assert.match(entrypointBlock, new RegExp(`label:\\s*'${entrypoint.label}'`), `${entrypoint.label} 必须显示在CSV页签。`)
  assert.match(entrypointBlock, new RegExp(`routePath:\\s*'${entrypoint.path}'`), `${entrypoint.label} 必须跳转到正式页面。`)
  assert.match(entrypointBlock, new RegExp(`permission:\\s*'${entrypoint.permission}'`), `${entrypoint.label} 必须保留正式权限说明。`)
}

assert.match(
  csvPane,
  /class="csv-evidence-entrypoints"/,
  'CSV质量包页签必须渲染统一证据导出入口面板。'
)
assert.match(
  csvPane,
  /v-for="entrypoint in approvalEvidenceEntrypoints"/,
  '统一入口面板必须来自 approvalEvidenceEntrypoints，避免散落硬编码。'
)
assert.match(
  csvPane,
  /openApprovalEvidenceEntrypoint\(entrypoint\)/,
  '统一入口点击必须走明确处理函数。'
)
assert.match(
  csvPane,
  /router\.push\(\{ path: entrypoint\.routePath \}\)/,
  '统一入口只能跳转正式页面执行导出，不得在CSV页签伪造导出。'
)
assert.doesNotMatch(
  csvPane,
  /approvalEvidenceEntrypoints[\s\S]*request\.download|approvalEvidenceEntrypoints[\s\S]*axios\.get|approvalEvidenceEntrypoints[\s\S]*fetch\(/,
  '统一入口不得绕过正式页面直接下载或模拟导出。'
)
