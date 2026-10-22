import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('batch record report API exposes review source fields for signature markers', () => {
  const apiSource = readText('src/api/mes/pro/batchrecordreport/index.ts')

  for (const field of [
    'signatureCellKey',
    'reviewSourceType',
    'reviewSourceId',
    'reviewSourceIds',
    'reviewSourceName'
  ]) {
    assert.match(apiSource, new RegExp(field), `signature marker API should expose ${field}`)
  }
  assert.match(apiSource, /'USER'/, 'signature marker API should expose single user review source')
  assert.match(apiSource, /'ROLES'/, 'signature marker API should expose multi-role review source')
  assert.match(apiSource, /'USERS'/, 'signature marker API should expose multi-user review source')
})

test('formal cell rule designer preserves review markers and binds current user/role collections', () => {
  const source = readText('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
  for (const token of ['getSimpleRoleList', 'getSimpleUserList', 'reviewSourceType', 'reviewSourceId', 'reviewSourceIds', 'candidateSourceType', 'USERS', 'ROLE', 'multiple']) {
    assert.ok(source.includes(token), `formal designer must bind ${token}`)
  }
  assert.match(source, /existingMarker\?\.reviewSourceIds/, 'existing grouped marker identities must be retained')
  assert.match(source, /normalizeAssignmentSourceType/, 'current assignment type contract must be normalized explicitly')
})
test('approval detail page gates review signing to the current work-task signature cell', () => {
  const pageSource = readText('src/views/mes/pro/edhr/ApprovalDetailPage.vue')

  assert.match(pageSource, /signatureCellKey/, 'approval detail page should carry signature cell keys')
  assert.match(pageSource, /allowedSignatureCellKey|currentReviewSignatureCell/, 'approval detail page should compute current review cell')
  assert.match(pageSource, /workTaskId/, 'approval detail page should trust workTaskId context for review signing')
  assert.match(pageSource, /readonly|isReadonly/, 'review task form should remain readonly except signature action')
})

test('work task board displays signature-cell review context', () => {
  const apiSource = readText('src/api/mes/pro/edhr/workTask.ts')
  const pageSource = readText('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')

  assert.match(apiSource, /signatureCellKey/, 'work task API should expose signature cell key')
  assert.match(apiSource, /reviewSourceName/, 'work task API should expose review source name')
  assert.match(pageSource, /reviewSourceName|signatureCellKey/, 'task board should show signature-cell review context')
})
