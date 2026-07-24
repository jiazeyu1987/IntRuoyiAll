import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const countRoutePathDefinitions = (source, routePath) =>
  Array.from(source.matchAll(new RegExp(`path:\\s*'${routePath.replaceAll('/', '\\/')}'`, 'g'))).length

const walkFiles = (relativePath) => {
  const basePath = path.join(root, relativePath)
  const results = []
  const stack = [basePath]
  while (stack.length) {
    const current = stack.pop()
    const entries = fs.readdirSync(current, { withFileTypes: true })
    for (const entry of entries) {
      const absolutePath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        stack.push(absolutePath)
        continue
      }
      results.push(absolutePath)
    }
  }
  return results
}

const findFilesWithText = (relativePath, matcher) =>
  walkFiles(relativePath).filter((absolutePath) => matcher.test(fs.readFileSync(absolutePath, 'utf8')))

test('Feedback API defines dedicated eDHR entry-context and open-or-create-by-context endpoints', () => {
  const source = readText('src/api/mes/pro/feedback/index.ts')
  assert.match(
    source,
    /entry-context/,
    'ProFeedbackApi should expose an eDHR entry-context endpoint for FeedbackForm entry resolution'
  )
  assert.match(
    source,
    /getEdhrEntryContext[\s\S]*request\.get<ProFeedbackEdhrEntryContextVO>/,
    'eDHR entry-context should use request.get so it matches the backend current GET contract'
  )
  assert.match(
    source,
    /open-or-create-by-context/,
    'ProFeedbackApi should expose an eDHR open-or-create-by-context endpoint for FeedbackForm entry resolution'
  )
  assert.match(
    source,
    /interface\s+ProFeedbackEdhrOpenOrCreateRespVO[\s\S]*id\s*:\s*number/,
    'open-or-create-by-context response contract should explicitly expose id'
  )
  assert.doesNotMatch(
    source,
    /interface\s+ProFeedbackEdhrOpenOrCreateRespVO[\s\S]*executionId\s*:\s*number/,
    'open-or-create-by-context response contract should not keep stale executionId'
  )
  for (const fieldName of ['canOpen', 'bindingResolved', 'created', 'activeContextKey']) {
    assert.match(
      source,
      new RegExp(`\\b${fieldName}\\b`),
      `eDHR API types should lock ${fieldName} in frontend contract`
    )
  }
})

test('FeedbackForm exposes 打开 eDHR entry and passes the required execution context', () => {
  const source = readText('src/views/mes/pro/feedback/FeedbackForm.vue')
  assert.match(source, /打开\s*eDHR/, 'FeedbackForm should expose a visible 打开 eDHR entry')
  assert.match(
    source,
    /entry-context|open-or-create-by-context|openOrCreateByContext|getEdhrEntryContext/,
    'FeedbackForm should use dedicated eDHR entry APIs instead of relying on route-process queries'
  )
  for (const fieldName of [
    'workOrderId',
    'taskId',
    'routeId',
    'processId',
    'workstationId',
    'batchCode'
  ]) {
    assert.match(
      source,
      new RegExp(`\\b${fieldName}\\b`),
      `FeedbackForm should pass ${fieldName} in the 打开 eDHR execution context`
    )
  }
  assert.match(
    source,
    /execution\?\.id/,
    'FeedbackForm should read id from open-or-create-by-context response'
  )
  assert.doesNotMatch(
    source,
    /execution\?\.executionId/,
    'FeedbackForm should not keep stale execution.executionId access'
  )
  assert.match(source, /查看批次执行/, 'FeedbackForm should expose an entry to the eDHR batch execution page')
  assert.match(
    source,
    /path:\s*'\/mes\/pro\/feedback\/edhr-batch-execution'/,
    'FeedbackForm should route batch-level eDHR lookup to the batch execution page'
  )
})

test('remaining router keeps eDHR execution form as the only execution static route under the batch active menu', () => {
  const source = readText('src/router/modules/remaining.ts')
  assert.equal(
    countRoutePathDefinitions(source, 'pro/feedback/edhr-execution'),
    0,
    'obsolete eDHR execution list must not be provided by static routing'
  )
  assert.match(
    source,
    /path:\s*'pro\/feedback\/edhr-execution\/form'[\s\S]*name:\s*'MesProFeedbackEdhrExecutionForm'/,
    'the eDHR execution form route should remain as the single-record execution surface'
  )
  assert.match(
    source,
    /path:\s*'pro\/feedback\/edhr-execution\/form'[\s\S]*activeMenu:\s*'\/mes\/pro\/feedback\/edhr-batch-execution'/,
    'the eDHR execution form route should highlight the batch execution replacement entry'
  )
})

test('eDHR execution page keeps executionSnapshotJson as the audited value contract', () => {
  const edhrFiles = findFilesWithText('src', /edhr|executionSnapshotJson/)
  assert.ok(
    edhrFiles.length > 0,
    'frontend should include a dedicated eDHR execution page or renderer source before the contract can be implemented'
  )

  const normalizedFiles = edhrFiles.map((absolutePath) => path.relative(root, absolutePath))
  const filesWithExecutionSnapshot = edhrFiles.filter((absolutePath) =>
    /executionSnapshotJson/.test(fs.readFileSync(absolutePath, 'utf8'))
  )

  assert.ok(
    filesWithExecutionSnapshot.length > 0,
    `eDHR execution files should consume executionSnapshotJson. Current candidates: ${normalizedFiles.join(', ')}`
  )
})

test('eDHR frontend provides form and batch pages with semantic-first execution summary', () => {
  const detailSource = readText('src/views/mes/pro/edhr/ExecutionPage.vue')
  const batchListSource = readText('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')

  assert.match(batchListSource, /workOrderCode/, 'batch execution list should keep work-order filtering')
  assert.match(batchListSource, /batchCode/, 'batch execution list should keep batch filtering')
  assert.match(detailSource, /工单/, 'eDHR execution form should prioritize work-order semantics')
  assert.match(detailSource, /工序/, 'eDHR execution form should prioritize process semantics')
  assert.match(detailSource, /工作站/, 'eDHR execution form should prioritize workstation semantics')
  assert.match(detailSource, /电子批记录表单/, 'eDHR execution form should remain the single-record viewing surface')
  assert.match(detailSource, /单表归档/, 'eDHR execution form should carry single-record archive actions')
})
