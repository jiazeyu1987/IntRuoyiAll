import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('workflow api exposes electronic receipt and paper recovery contracts', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')

  for (const field of ['recipients', 'ackComment', 'recoveredBy', 'recoveredAt']) {
    assert.match(workflowSource, new RegExp(`${field}[?]?:|${field}:`), `workflow contract must expose ${field}`)
  }

  assert.match(workflowSource, /export const acknowledgeElectronicDistribution/)
  assert.match(workflowSource, /export const recoverPaperDistribution/)
  assert.match(workflowSource, /\/distributions\/\$\{distributionId\}\/recipients\/\$\{recipientId\}\/acknowledge/)
  assert.match(workflowSource, /\/paper-distributions\/\$\{distributionId\}\/recover/)
})

test('detail page exposes receipt actions without leaving the DCC detail page', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')

  for (const apiName of ['acknowledgeElectronicDistribution', 'recoverPaperDistribution']) {
    assert.match(detailSource, new RegExp(apiName), `detail page must call ${apiName}`)
  }

  for (const label of ['确认签收', '确认回收', '导出回执', '打印回执']) {
    assert.match(detailSource, new RegExp(label), `detail page must expose ${label}`)
  }

  assert.match(detailSource, /electronicReceiptDialog/)
  assert.match(detailSource, /handleExportDistributionReceipts/)
  assert.match(detailSource, /handlePrintDistributionReceipts/)
  assert.match(detailSource, /getCurrentElectronicReceiptRecipient/)
  assert.match(detailSource, /window\.open\('', '_blank'\)/)
  assert.doesNotMatch(detailSource, /暂未实现|功能暂未开放/)
})

test('receipt presentation covers recovered paper distribution and recipient acknowledgement', () => {
  const presentationSource = readText('src/views/dcc/controlled-file/detail/presentation.ts')

  assert.match(presentationSource, /\['RECOVERED', '已回收'\]/)
  assert.match(presentationSource, /acknowledgedAt/)
  assert.match(presentationSource, /ackComment/)
})
