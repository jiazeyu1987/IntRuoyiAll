import { readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'

const root = process.cwd()
const readText = (path) => readFileSync(resolve(root, path), 'utf8')

const extractFunctionSource = (source, name) => {
  const start = source.indexOf(`const ${name} =`)
  assert.notEqual(start, -1, `${name} must exist`)
  const nextFunction = source.indexOf('\nconst ', start + 1)
  return nextFunction === -1 ? source.slice(start) : source.slice(start, nextFunction)
}

test('dcc approval print template api exposes config, print html, and docx export contracts', () => {
  const apiPath = 'src/api/dcc/controlledFile/approvalPrintTemplate.ts'
  assert.ok(existsSync(resolve(root, apiPath)), 'approval print template API module must exist')
  const apiSource = readText(apiPath)

  assert.match(apiSource, /ApprovalPrintTemplateVO/)
  assert.match(apiSource, /saveActiveApprovalPrintTemplate/)
  assert.match(apiSource, /getActiveApprovalPrintTemplate/)
  assert.match(apiSource, /getControlledFileApprovalPrintHtml/)
  assert.match(apiSource, /exportControlledFileApprovalWord/)
  assert.match(apiSource, /\/dcc\/approval-print-template\/active/)
  assert.match(apiSource, /\/dcc\/approval-print-template\/save/)
  assert.match(apiSource, /\/dcc\/controlled-files\/\$\{id\}\/approval-print\/print-html/)
  assert.match(apiSource, /\/dcc\/controlled-files\/\$\{id\}\/approval-print\/export-word/)
  assert.match(apiSource, /responseType:\s*'blob'|downloadOriginal/)
})

test('dcc approval print template settings page uploads docx and saves backend validation result', () => {
  const pagePath = 'src/views/dcc/controlled-file/print-template/index.vue'
  assert.ok(existsSync(resolve(root, pagePath)), 'DCC print-template settings page must exist')
  const pageSource = readText(pagePath)
  const routeSource = readText('src/router/modules/remaining.ts')

  assert.match(routeSource, /controlled-file\/print-template/)
  assert.match(routeSource, /DccApprovalPrintTemplate/)
  assert.match(routeSource, /模板配置/)
  assert.match(pageSource, /accept="\.docx|accept='\.docx|acceptDocx/)
  assert.match(pageSource, /updateFile|\/infra\/file\/upload/)
  assert.match(pageSource, /saveActiveApprovalPrintTemplate/)
  assert.match(pageSource, /getActiveApprovalPrintTemplate/)
  assert.match(pageSource, /requiredPlaceholders|fileNumber|approvalRecords/)
  assert.match(pageSource, /message\.error|ElMessage\.error/)
})

test('dcc detail uses backend custom template only when active and keeps built-in print/export otherwise', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')

  assert.match(detailSource, /getActiveApprovalPrintTemplate/)
  assert.match(detailSource, /exportControlledFileApprovalWord/)
  assert.match(detailSource, /getControlledFileApprovalPrintHtml/)
  assert.match(detailSource, /activeApprovalPrintTemplate/)
  assert.match(detailSource, /buildProcessPrintHtml/)
  assert.match(detailSource, /ProcessInstanceApi\.getProcessInstancePrintData/)
  assert.match(detailSource, /downloadByData\(\s*buildProcessPrintHtml/)
  assert.match(detailSource, /流程打印/)
  assert.match(detailSource, /流程导出 Word/)
})

test('dcc detail does not prefetch approval print html during detail load and fetches it on print action', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
  const loadDataSource = extractFunctionSource(detailSource, 'loadData')
  const handlePrintProcessSource = extractFunctionSource(detailSource, 'handlePrintProcess')

  assert.match(loadDataSource, /getActiveApprovalPrintTemplate/)
  assert.doesNotMatch(
    loadDataSource,
    /getControlledFileApprovalPrintHtml/,
    'detail load must not prefetch print-html because R12 failures must not block R11 paper records or main detail data'
  )
  assert.match(
    handlePrintProcessSource,
    /await\s+getControlledFileApprovalPrintHtml\(controlledFileId\.value\)/,
    'print action must request backend print-html on demand'
  )
  assert.doesNotMatch(
    handlePrintProcessSource,
    /activeApprovalPrintHtml\.value\s*\|\|/,
    'active template print must not silently reuse prefetched html or fall back from failed backend rendering'
  )
})
