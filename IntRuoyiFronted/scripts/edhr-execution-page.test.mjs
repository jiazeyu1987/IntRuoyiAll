import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('eDHR execution page renders minimal editable controls from executionSnapshotJson.fields', () => {
  const pageSource = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(
    pageSource,
    /executionSnapshotJson/,
    'ExecutionPage should still load executionSnapshotJson as the only runtime snapshot source'
  )
  assert.match(
    pageSource,
    /\.fields\b|snapshotFields/,
    'ExecutionPage should read executionSnapshotJson.fields to build editable controls'
  )
  assert.match(
    pageSource,
    /el-form|el-input|el-input-number|el-date-picker|el-select/,
    'ExecutionPage should render minimal form controls instead of only pretty-printing raw JSON'
  )
  assert.match(
    pageSource,
    /cellValues/,
    'ExecutionPage should map execution detail cellValues back into editable field values'
  )
  assert.match(
    pageSource,
    /saveEdhrFieldChanges|field-audit\/save-changes/,
    'ExecutionPage should save editable execution fields through the field audit path'
  )
  assert.match(
    pageSource,
    /loadExecution\(\)|await loadExecution\(\)/,
    'ExecutionPage should reload execution detail after draft save to replay backend cellValues'
  )
})

test('eDHR execution API exposes save-draft contract for cellValues persistence', () => {
  const apiSource = readText('src/api/mes/pro/feedback/index.ts')

  assert.match(
    apiSource,
    /interface\s+ProFeedbackEdhrExecutionCellValueVO[\s\S]*rowIndex[\s\S]*columnIndex[\s\S]*value/s,
    'feedback API should declare the execution cell value contract'
  )
  assert.match(
    apiSource,
    /interface\s+ProFeedbackEdhrSaveDraftReqVO[\s\S]*cellValues[\s\S]*remark/s,
    'feedback API should declare save-draft request fields'
  )
  assert.match(
    apiSource,
    /save-draft/,
    'feedback API should expose the save-draft endpoint'
  )
  assert.match(
    apiSource,
    /saveEdhrExecutionDraft/,
    'feedback API should export a saveEdhrExecutionDraft method'
  )
})
