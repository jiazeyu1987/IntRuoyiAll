const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(absolutePath), `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function assertContains(source, expected, label) {
  assert.ok(source.includes(expected), `missing ${label}: ${expected}`)
}

function assertNotContains(source, forbidden, label) {
  assert.ok(!source.includes(forbidden), `unexpected ${label}: ${forbidden}`)
}

function extractTopLevelConstFunction(source, functionName) {
  const marker = `const ${functionName} =`
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `missing ${functionName} handler`)
  const afterMarker = source.slice(start + marker.length)
  const nextTopLevelConst = afterMarker.search(/\nconst [A-Za-z_$][\w$]*\s*=/)
  assert.notEqual(nextTopLevelConst, -1, `missing boundary after ${functionName} handler`)
  return source.slice(start, start + marker.length + nextTopLevelConst)
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')
const shared = readUtf8('src/views/infra/runtime-control/components/shared.ts')

for (const fragment of [
  'export interface RuntimeControlTrustedTimeVO',
  'trustedTime?: RuntimeControlTrustedTimeVO',
  'selectedSource?: string',
  'lastOffsetMillis?: number',
  'rmsOffsetMillis?: number',
  'leapStatus?: string',
  'checkedAtUtc?: string',
  'export const downloadRuntimeControlTimeEvidence = (id: number)',
  'request.download<Blob>({',
  'url: `/infra/runtime-control/inspection-runs/${id}/time-evidence.zip`'
]) {
  assertContains(api, fragment, 'trusted-time API contract')
}

for (const fragment of [
  '可信时间证据',
  '执行巡检',
  '导出时间戳证据',
  '环境',
  '节点',
  '时间源',
  'Last 偏差',
  'RMS 偏差',
  'Leap',
  '检查时间',
  '状态',
  'v-loading="opsLoading.inspection"',
  '尚未执行巡检，暂无可导出的时间戳证据',
  '当前巡检没有可信时间检查项',
  "v-hasPermi=\"['infra:runtime-control:operate']\"",
  ':loading="timeEvidenceDownloading"',
  'trustedTimeChecks',
  "backup: '审查服'",
  'check.trustedTime?.selectedSource',
  'offsetMillisText(check.trustedTime?.lastOffsetMillis)',
  'offsetMillisText(check.trustedTime?.rmsOffsetMillis)',
  'formatRuntimeDate(check.trustedTime?.checkedAtUtc)',
  ':disabled="!inspectionRun?.id || timeEvidenceDownloading"',
  'const inspectionId = inspectionRun.value?.id',
  'RuntimeControlApi.downloadRuntimeControlTimeEvidence(inspectionId)',
  "download.zip(data, `可信时间证据_巡检${inspectionId}.zip`)",
  'reportActionError(error)'
]) {
  assertContains(page, fragment, 'trusted-time page contract')
}

for (const fragment of [
  'operationHistoryEnvironmentText(row.environment)',
  'operationReasonText(row)',
  'operationSummaryText(row)',
  'const normalizeAuditServerDisplayText =',
  ".replace(/备份服务器/g, '审查服务器')",
  ".replace(/备用服务器/g, '审查服务器')",
  ".replace(/备份服/g, '审查服')",
  "Backup(?![\\\\/])/gi, '$1审查服'",
  'return operationHistoryEnvironmentText(operation.environment)',
  'operationSummaryText(operation)'
]) {
  assertContains(page, fragment, 'historical operation terminology projection')
}

assertNotContains(page, '<el-table-column label="环境" prop="environment"', 'raw operation environment')
assertNotContains(page, '<el-table-column label="原因" prop="reason"', 'raw operation reason')
assertNotContains(page, '<el-table-column label="摘要" prop="summary"', 'raw operation summary')

const operationActionHandler = extractTopLevelConstFunction(page, 'operationActionText')
assertContains(operationActionHandler, 'operationActionLabel(actionCode)', 'current action code mapping')
assertContains(operationActionHandler, 'operation.actionLabel', 'legacy action label normalization')
assert.ok(
  operationActionHandler.indexOf('operationActionLabel(actionCode)') <
    operationActionHandler.indexOf('operation.actionLabel'),
  'known action code mapping must take precedence over historical actionLabel'
)

const normalizerMatch = page.match(
  /const normalizeAuditServerDisplayText = \(value\?: string\) => \{([\s\S]*?)\n\}/
)
assert.ok(normalizerMatch, 'missing audit server display normalizer body')
const normalizeAuditServerDisplayText = new Function('value', normalizerMatch[1])
assert.equal(normalizeAuditServerDisplayText('Backup'), '审查服')
assert.equal(normalizeAuditServerDisplayText('备份服已完成'), '审查服已完成')
assert.equal(normalizeAuditServerDisplayText('上线备份服务器'), '上线审查服务器')
assert.equal(normalizeAuditServerDisplayText('正式服/备用服务器清理'), '正式服/审查服务器清理')
assert.equal(
  normalizeAuditServerDisplayText('证据路径 Backup/ReleasePackage/r1'),
  '证据路径 Backup/ReleasePackage/r1',
  'technical Backup path must remain unchanged'
)

assertNotContains(
  page,
  ':disabled="!inspectionRun?.id || inspectionRun?.status !== \'PASS\' || timeEvidenceDownloading"',
  'status-based export disablement'
)
const exportButton = page.match(
  /<el-button\s+type="primary"\s+:disabled="!inspectionRun\?\.id \|\| timeEvidenceDownloading"[\s\S]*?<\/el-button>/
)
assert.ok(exportButton, 'missing time evidence export button')
assertNotContains(exportButton[0], 'inspectionRun?.status', 'status-based export disablement')
assertNotContains(page, '<span>Backup</span>', 'legacy visible Backup label')
assertNotContains(page, "label: 'Backup'", 'legacy visible environment label')
assertNotContains(page, "label: '备份服务器'", 'legacy visible backup server label')
assertNotContains(page, '上线备份服务器', 'legacy visible promote label')
assertNotContains(page, '测试服、备份服务器或正式服务器', 'legacy visible backup wording')
assertNotContains(page, "backup: '审查环境'", 'nonstandard audit server environment label')
assertContains(shared, "backup: '审查服'", 'shared audit server environment label')
assertContains(shared, "'promote-backup': '上线审查服'", 'shared audit server action label')
assertNotContains(shared, "backup: 'Backup'", 'legacy shared Backup label')
assertNotContains(shared, "'promote-backup': '上线备份服务器'", 'legacy shared promote label')

const exportHandler = extractTopLevelConstFunction(page, 'exportTimeEvidence')
assertContains(exportHandler, 'timeEvidenceDownloading.value = true', 'complete export handler body')
assertContains(exportHandler, 'timeEvidenceDownloading.value = false', 'complete export handler finally')
assertNotContains(exportHandler, 'runRuntimeControlInspection', 'export-triggered inspection')
assertContains(exportHandler, 'if (!inspectionId)', 'missing inspection id guard')

console.log('PASS: runtime control trusted-time UI contract is present')
