const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const dayjs = require('dayjs')
const ts = require('typescript')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const helperPath = 'src/views/mes/pro/edhr/shared/dateTime.ts'
assert.ok(fs.existsSync(path.join(repoRoot, helperPath)), `${helperPath} 必须作为 eDHR 统一时间格式化入口。`)

const helper = read(helperPath)
assert.match(helper, /export type EdhrDateTimeValue/)
assert.match(helper, /export const toEdhrDateTime/)
assert.match(helper, /export const formatEdhrDateTime/)
assert.match(helper, /export const edhrDateTimeFormatter/)
assert.match(helper, /\/\^\\d\+\$\/\.test\(trimmedValue\)/)
assert.match(helper, /new Date\(Number\(trimmedValue\)\)/)
assert.match(helper, /Number\.isNaN\(date\.getTime\(\)\)/)
assert.match(helper, /formatDate\(date,\s*'YYYY-MM-DD HH:mm:ss'\)/)

const compiledHelper = ts.transpileModule(helper, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
const helperSandbox = {
  exports: {},
  require: (moduleName) => {
    if (moduleName === '@/utils/formatTime') {
      return { formatDate: (date, format = 'YYYY-MM-DD HH:mm:ss') => dayjs(date).format(format) }
    }
    throw new Error(`Unexpected helper dependency: ${moduleName}`)
  }
}
vm.runInNewContext(compiledHelper, helperSandbox, { filename: helperPath })
assert.equal(helperSandbox.exports.formatEdhrDateTime(1784816552000), '2026-07-23 22:22:32')
assert.equal(helperSandbox.exports.formatEdhrDateTime('1784816552000'), '2026-07-23 22:22:32')
assert.equal(helperSandbox.exports.formatEdhrDateTime('2026-07-23 22:22:32'), '2026-07-23 22:22:32')
assert.equal(helperSandbox.exports.formatEdhrDateTime('bad-time'), '时间格式异常')

const sourceExpectations = [
  {
    file: 'src/views/mes/pro/edhr/OperationAuditPage.vue',
    required: [
      ':formatter="edhrDateTimeFormatter"',
      "formatEdhrDateTime(detail.occurredAt)"
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/ApprovalDetailPage.vue',
    required: ["formatEdhrDateTime(value, '')"],
    forbidden: [
      "formatDate(parsedDate, 'YYYY年M月D日 HH:mm:ss')",
      'throw new Error(`审批详情时间不可解析'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/components/OperationAuditListPane.vue',
    required: [
      ':formatter="edhrDateTimeFormatter"',
      "formatEdhrDateTime(detail.occurredAt)"
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/DomainTracePage.vue',
    required: ["formatEdhrDateTime(row.verifiedAt, '未校验')"]
  },
  {
    file: 'src/views/mes/pro/edhr/components/DomainTraceListPane.vue',
    required: ["formatEdhrDateTime(row.verifiedAt, '未校验')"]
  },
  {
    file: 'src/views/mes/pro/edhr/DomainTraceDetailPage.vue',
    required: ["formatEdhrDateTime(detail.verifiedAt, '未校验')"]
  },
  {
    file: 'src/views/mes/pro/edhr/FieldAuditDetailPage.vue',
    required: [
      'formatEdhrDateTime(detail.auditBatch?.changedAt || detail.signature?.signedAt)',
      'formatEdhrDateTime(row.changedAt)',
      'formatEdhrDateTime(detail.signature?.signedAt)',
      'formatEdhrDateTime(detail.hashVerification?.checkedAt)'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/FieldAuditPage.vue',
    required: [
      'formatEdhrDateTime(row.currentValueChangedAt)',
      "formatEdhrDateTime(row.firstHumanChangedAt, '暂无有效填写人')",
      'formatEdhrDateTime(row.firstHumanChangedAt)',
      'formatEdhrDateTime(row.changedAt)'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue',
    required: [
      'formatEdhrDateTime(row.createTime)',
      'formatEdhrDateTime(row.completedAt)',
      'formatEdhrDateTime(row.overdueAt)',
      'formatEdhrDateTime(row.dueTime)'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/ExecutionPage.vue',
    required: [
      ':formatter="edhrDateTimeFormatter"',
      'return toEdhrDateTime(value)?.getTime() ?? 0',
      'return formatEdhrDateTime(value)'
    ],
    forbidden: [
      "formatDate(parsedDate, 'YYYY年M月D日')",
      'throw new Error(`追踪执行参数时间不可解析'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue',
    required: [
      'return toEdhrDateTime(value)?.getTime() ?? 0',
      'return formatEdhrDateTime(value)'
    ],
    forbidden: [
      'const date = new Date(value)',
      'const time = new Date(value).getTime()'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/FormFillLogPage.vue',
    required: ['return formatEdhrDateTime(value)'],
    forbidden: ['const parsedDate = new Date(value)']
  },
  {
    file: 'src/views/mes/pro/edhr/TrackingPage.vue',
    required: ['formatEdhrDateTime(lastEventAt)'],
    forbidden: [
      "formatDate(parsedDate, 'YYYY年M月D日')",
      'throw new Error(`最后处理时间不可解析'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/SignaturePage.vue',
    required: ["formatEdhrDateTime(signedAt, '')"],
    forbidden: [
      "formatDate(parsedDate, 'YYYY年M月D日 HH:mm:ss')",
      'throw new Error(`签名时间不可解析'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
    required: [
      ':formatter="edhrDateTimeFormatter"',
      'formatEdhrDateTime(archivePreview.generatedAt)'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr-dhr-template/DhrTemplatePage.vue',
    required: [
      'prop="createTime" width="180" :formatter="edhrDateTimeFormatter"',
      'prop="confirmedAt" width="180" :formatter="edhrDateTimeFormatter"'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue',
    required: [
      'prop="generatedAt" width="180" :formatter="edhrDateTimeFormatter"',
      'prop="requestedAt" width="180" :formatter="edhrDateTimeFormatter"'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr-report/ReportPage.vue',
    required: [
      'prop="occurredAt" width="180" :formatter="edhrDateTimeFormatter"',
      'formatEdhrDateTime(queryResult.dataUpdatedAt)'
    ],
    forbidden: ['new Date(value)']
  },
  {
    file: 'src/views/mes/pro/edhr-traveler/TravelerPage.vue',
    required: [
      'prop="generatedAt" width="180" :formatter="edhrDateTimeFormatter"',
      'prop="activeAt" width="180" :formatter="edhrDateTimeFormatter"',
      'prop="occurredAt" width="180" :formatter="edhrDateTimeFormatter"'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue',
    forbidden: ['new Date(value)']
  },
  {
    file: 'src/views/mes/pro/edhr/form-trace/FormTraceAuditTab.vue',
    required: ['formatEdhrDateTime(lastEventAt)'],
    forbidden: [
      "formatDate(parsedDate, 'YYYY年M月D日')",
      'throw new Error(`最后处理时间不可解析'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue',
    required: ['formatEdhrDateTime(value)'],
    forbidden: [
      "formatDate(parsedDate, 'YYYY-MM-DD HH:mm')",
      'if (Number.isNaN(parsedDate.getTime())) return'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr/components/ReleaseEventListPane.vue',
    forbidden: ['new Date(value)']
  },
  {
    file: 'src/views/mes/pro/edhr-release/ReleasePage.vue',
    forbidden: ['new Date(value)']
  },
  {
    file: 'src/views/mes/pro/edhr-flow-intervention/FlowInterventionPage.vue',
    forbidden: ['new Date(value)']
  },
  {
    file: 'src/views/mes/pro/edhr-unified-change/UnifiedChangePage.vue',
    forbidden: ['new Date(value)']
  }
]

for (const expectation of sourceExpectations) {
  const source = read(expectation.file)
  assert.match(
    source,
    /from '@\/views\/mes\/pro\/edhr\/shared\/dateTime'/,
    `${expectation.file} 必须使用 eDHR 统一时间格式化入口。`
  )
  for (const requiredSnippet of expectation.required || []) {
    assert.ok(
      source.includes(requiredSnippet),
      `${expectation.file} 缺少时间格式化契约：${requiredSnippet}`
    )
  }
  for (const forbiddenSnippet of expectation.forbidden || []) {
    assert.ok(
      !source.includes(forbiddenSnippet),
      `${expectation.file} 不得继续使用不支持数字字符串毫秒值的 ${forbiddenSnippet}`
    )
  }
}

for (const file of [
  'src/views/mes/pro/edhr/RecordChangePage.vue',
  'src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue'
]) {
  const source = read(file)
  assert.match(source, /edhrDateTimeFormatter,\s*formatEdhrDateTime/)
  assert.match(source, /from '@\/views\/mes\/pro\/edhr\/shared\/dateTime'/)
}

console.log('PASS: eDHR system time format hardening static contract')
