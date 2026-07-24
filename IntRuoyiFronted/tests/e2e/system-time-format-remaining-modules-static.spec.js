const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const dayjs = require('dayjs')
const ts = require('typescript')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const formatTimePath = 'src/utils/formatTime.ts'
const formatTimeSource = read(formatTimePath)

assert.match(formatTimeSource, /export type DateTimeDisplayValue/)
assert.match(formatTimeSource, /export function toDateTimeValue/)
assert.match(formatTimeSource, /export function formatDateTimeValue/)
assert.match(formatTimeSource, /export function dateTimeValueFormatter/)
assert.match(formatTimeSource, /\/\^\\d\+\$\/\.test\(trimmedValue\)/)
assert.match(formatTimeSource, /new Date\(Number\(trimmedValue\)\)/)

const compiledFormatTime = ts.transpileModule(formatTimeSource, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText

const formatTimeSandbox = {
  exports: {},
  require: (moduleName) => {
    if (moduleName === 'dayjs') {
      return { default: dayjs }
    }
    if (moduleName === 'element-plus') {
      return {}
    }
    throw new Error(`Unexpected dependency: ${moduleName}`)
  }
}
vm.runInNewContext(compiledFormatTime, formatTimeSandbox, { filename: formatTimePath })

assert.equal(formatTimeSandbox.exports.formatDateTimeValue(1784816552000), '2026-07-23 22:22:32')
assert.equal(formatTimeSandbox.exports.formatDateTimeValue('1784816552000'), '2026-07-23 22:22:32')
assert.equal(formatTimeSandbox.exports.formatDateTimeValue('2026-07-23 22:22:32'), '2026-07-23 22:22:32')
assert.equal(formatTimeSandbox.exports.formatDateTimeValue('bad-time'), '时间格式异常')

const sourceExpectations = [
  {
    file: 'src/views/dcc/controlled-file/signatures/index.vue',
    required: [
      'formatDateTimeValue(row.signedAt)',
      'formatDateTimeValue(row.latestAuditAt)',
      'prop="operatedAt"',
      ':formatter="dateTimeValueFormatter"'
    ],
    forbidden: ['row.signedAt ||', 'row.latestAuditAt ||']
  },
  {
    file: 'src/views/dcc/controlled-file/print-template/index.vue',
    required: ['formatDateTimeValue(activeTemplate.updateTime)'],
    forbidden: ['activeTemplate.updateTime ||']
  },
  {
    file: 'src/views/showroom-admin/approval/ApprovalTaskPanel.vue',
    required: [
      "formatDateTimeValue(activeDetail.changeRequest.submittedAt, '未记录')",
      'prop="signedAt" :formatter="dateTimeValueFormatter"'
    ],
    forbidden: ['activeDetail.changeRequest.submittedAt ||']
  },
  {
    file: 'src/views/showroom-admin/product/ProductDetailDialog.vue',
    required: ['prop="signedAt" :formatter="dateTimeValueFormatter"']
  },
  {
    file: 'src/views/showroom-admin/product/ProductHistoryDrawer.vue',
    required: ['prop="createdAt" :formatter="dateTimeValueFormatter"']
  },
  {
    file: 'src/views/showroom-admin/history/VersionDiffDrawer.vue',
    required: ['prop="createdAt" :formatter="dateTimeValueFormatter"']
  },
  {
    file: 'src/views/showroom-admin/version-center/VersionDiffPanel.vue',
    required: ['formatDateTimeValue(currentRelease?.publishedAt, \'未发布\')'],
    forbidden: ['currentRelease?.publishedAt ||']
  },
  {
    file: 'src/views/showroom-admin/version-center/VersionHistoryList.vue',
    required: ['formatDateTimeValue(item.publishedAt, \'发布时间未记录\')'],
    forbidden: ['item.publishedAt ||']
  },
  {
    file: 'src/views/showroom-admin/version-center/VersionCenterHeader.vue',
    required: ['formatDateTimeValue(currentRelease.publishedAt)'],
    forbidden: ['currentRelease.publishedAt}`']
  },
  {
    file: 'src/views/srm/outsource-execution/index.vue',
    required: [
      'prop="createTime" width="180" :formatter="dateTimeValueFormatter"',
      'prop="eventTime" width="180" :formatter="dateTimeValueFormatter"'
    ],
    forbidden: ['dateFormatter']
  },
  {
    file: 'src/views/srm/outsource-execution/my.vue',
    required: ['prop="eventTime" width="180" :formatter="dateTimeValueFormatter"']
  },
  {
    file: 'src/views/srm/payment-execution/index.vue',
    required: ['prop="eventTime" width="180" :formatter="dateTimeValueFormatter"']
  },
  {
    file: 'src/views/srm/supplier-portal/review/index.vue',
    required: ['prop="submittedTime" width="168" :formatter="dateTimeValueFormatter"']
  }
]

for (const expectation of sourceExpectations) {
  const source = read(expectation.file)
  assert.match(
    source,
    /from '@\/utils\/formatTime'/,
    `${expectation.file} 必须复用全局安全时间格式化入口。`
  )
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

console.log('PASS: remaining module system time format static contract')
