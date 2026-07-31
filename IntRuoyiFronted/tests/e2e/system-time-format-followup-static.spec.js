const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const dayjs = require('dayjs')
const ts = require('typescript')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const formatTimeSource = read('src/utils/formatTime.ts')
const compiledFormatTime = ts.transpileModule(formatTimeSource, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText

const formatTimeSandbox = {
  exports: {},
  require: (moduleName) => {
    if (moduleName === 'dayjs') return { default: dayjs }
    if (moduleName === 'element-plus') return {}
    throw new Error(`Unexpected dependency: ${moduleName}`)
  }
}
vm.runInNewContext(compiledFormatTime, formatTimeSandbox, { filename: 'src/utils/formatTime.ts' })

assert.equal(formatTimeSandbox.exports.formatDateTimeValue(1784816552000), '2026-07-23 22:22:32')
assert.equal(formatTimeSandbox.exports.formatDateTimeValue('1784816552000'), '2026-07-23 22:22:32')
assert.equal(formatTimeSandbox.exports.formatDateTimeValue('bad-time'), '时间格式异常')

const expectations = [
  {
    file: 'src/views/dcc/controlled-file/logs/index.vue',
    required: ['formatDateTimeValue(value, \'-\')'],
    forbidden: ['formatDate(new Date(value))']
  },
  {
    file: 'src/views/dcc/controlled-file/workbench/index.vue',
    required: ['formatDateTimeValue(new Date())'],
    forbidden: ['new Date().toLocaleString()']
  },
  {
    file: 'src/views/form-center/business-action/ActionFormPanel.vue',
    required: ['prop="createdTime" width="180" :formatter="dateTimeValueFormatter"'],
    forbidden: ['label="时间" prop="createdTime" width="180" />']
  },
  {
    file: 'src/views/mes/pro/edhr-init-batch/InitBatchPage.vue',
    required: ['formatDateTimeValue(row.lastPrecheckAt, \'-\')'],
    forbidden: ['label="最后预检" prop="lastPrecheckAt" width="180" />']
  },
  {
    file: 'src/views/mes/pro/route/index.vue',
    required: ['formatDateTimeValue(version.publishedTime, \'-\')'],
    forbidden: ['version.publishedTime || \'-\'']
  },
  {
    file: 'src/views/showroom-admin/index.vue',
    required: ["formatDateTimeValue(batchMediaSummaryResult.nextCheckAt, '待计算')"],
    forbidden: ["batchMediaSummaryResult.nextCheckAt || '待计算'"]
  },
  {
    file: 'src/views/mes/pro/edhr/ApprovalDetailPage.vue',
    required: ['formatEdhrDateTime(latestArchive.sealedAt)'],
    forbidden: ['latestArchive.sealedAt || \'--\'']
  },
  {
    file: 'src/views/mes/pro/task/calendar/index.vue',
    required: ["formatDateTimeValue(monthData.currentScheduleStatus?.updatedAt, '--')"],
    forbidden: ["monthData.currentScheduleStatus?.updatedAt || '--'"]
  },
  {
    file: 'src/views/mes/pro/batchrecordformlist/index.vue',
    required: [
      "formatDateTimeValue(value, '-')"
    ],
    forbidden: [
      'formatDate(new Date(value))',
      "formatDate(new Date(value), 'YYYY-MM-DD HH:mm')"
    ]
  },
  {
    file: 'src/views/mes/pro/scheduleorder/index.vue',
    required: ["formatDateTimeValue(value, '-')"],
    forbidden: ["formatDate(new Date(value), 'YYYY-MM-DD HH:mm')"]
  },
  {
    file: 'src/views/mes/pro/scheduler-workbench/index.vue',
    required: [
      "formatDateTimeValue(value, '无法估算')",
      "formatDateTimeValue(value, '—')"
    ],
    forbidden: ["dayjs(value).format('YYYY-MM-DD HH:mm')"]
  },
  {
    file: 'src/views/mes/pro/task/components/ProTaskSelectDialog.vue',
    required: [
      "formatDateTimeValue(scope.row.startTime, '-')",
      "formatDateTimeValue(scope.row.endTime, '-')"
    ],
    forbidden: ["formatDate(scope.row.startTime, 'YYYY-MM-DD HH:mm')"]
  },
  {
    file: 'src/views/mes/pro/feedback/index.vue',
    required: ["formatDateTimeValue(value, '-')"],
    forbidden: ['date.toLocaleString(\'zh-CN\', { hour12: false })']
  },
  {
    file: 'src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue',
    required: ["formatDateTimeValue(value, '-', '时间格式错误')"],
    forbidden: ['value instanceof Date ? value : new Date(value)']
  }
]

for (const expectation of expectations) {
  const source = read(expectation.file)
  for (const requiredSnippet of expectation.required || []) {
    assert.ok(
      source.includes(requiredSnippet),
      `${expectation.file} 缺少安全时间格式化契约：${requiredSnippet}`
    )
  }
  for (const forbiddenSnippet of expectation.forbidden || []) {
    assert.ok(!source.includes(forbiddenSnippet), `${expectation.file} 不得继续包含：${forbiddenSnippet}`)
  }
}

console.log('PASS: follow-up system time format static contract')
