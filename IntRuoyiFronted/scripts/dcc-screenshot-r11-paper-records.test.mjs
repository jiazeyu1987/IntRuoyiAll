import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('workflow api exposes dedicated paper distribution record contract', () => {
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')

  assert.match(workflowSource, /export interface ControlledFilePaperDistributionRecordVO/)
  for (const field of [
    'fileNumber',
    'versionNo',
    'fileName',
    'issuerName',
    'recipientNames',
    'issuedAt',
    'recovererName',
    'recoveredAt'
  ]) {
    assert.match(workflowSource, new RegExp(`${field}[?]?:|${field}:`), `paper record must expose ${field}`)
  }
  assert.match(workflowSource, /export const getPaperDistributionRecords/)
  assert.match(workflowSource, /\/paper-distributions\/records/)
})

test('detail export and print use dedicated paper record data source', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')

  assert.match(detailSource, /getPaperDistributionRecords/)
  assert.match(detailSource, /paperDistributionRecords/)
  assert.match(detailSource, /const distributionReceiptRows = computed\(\(\) =>\s*paperDistributionRecords\.value\s*\)/)
  assert.match(detailSource, /record\.issuerName/)
  assert.match(detailSource, /record\.recipientNames/)
  assert.doesNotMatch(
    detailSource,
    /const buildDistributionReceiptRows = \(\) => \{[\s\S]*fileDetail\.value\?\.distributionStatuses/,
    'export/print rows must not be built from detail distributionStatuses'
  )
})
