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

test('template designer lets APPROVE signature cells bind role and user source collections', () => {
  const pageSource = readText('src/views/mes/pro/batchrecordtemplate/index.vue')

  assert.match(pageSource, /getSimplePostList/, 'template designer should load system posts')
  assert.match(pageSource, /getSimpleRoleList/, 'template designer should load system roles')
  assert.match(pageSource, /getSimpleUserList/, 'template designer should load system users')
  assert.match(pageSource, /reviewSourceType/, 'template designer should bind review source type')
  assert.match(pageSource, /reviewSourceId/, 'template designer should bind review source id')
  assert.match(pageSource, /reviewSourceIds/, 'template designer should bind multi review source ids')
  assert.match(pageSource, /POST/, 'template designer should support POST review sources')
  assert.match(pageSource, /ROLE/, 'template designer should support ROLE review sources')
  assert.match(pageSource, /USER/, 'template designer should support USER review sources')
  assert.match(pageSource, /ROLES/, 'template designer should support ROLES review sources')
  assert.match(pageSource, /USERS/, 'template designer should support USERS review sources')
  assert.match(pageSource, /multiple/, 'template designer should use multi-select for grouped sources')
  assert.match(pageSource, /isMultipleReviewSourceType/, 'template designer should branch validation for grouped sources')
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
