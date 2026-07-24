import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('eDHR execution API exposes submit(password, comment) contract', () => {
  const apiSource = readText('src/api/mes/pro/feedback/index.ts')

  assert.match(
    apiSource,
    /interface\s+ProFeedbackEdhrSubmitReqVO[\s\S]*password[\s\S]*comment/s,
    'feedback API should declare submit request password/comment contract'
  )
  assert.match(apiSource, /\/submit\b/, 'feedback API should expose the execution submit endpoint')
  assert.match(
    apiSource,
    /submitEdhrExecution/,
    'feedback API should export a submitEdhrExecution method'
  )
})

test('eDHR execution API exposes form review cosign contract', () => {
  const apiSource = readText('src/api/mes/pro/feedback/index.ts')

  assert.match(
    apiSource,
    /interface\s+ProFeedbackEdhrFormReviewSignReqVO[\s\S]*executionId[\s\S]*password[\s\S]*comment/s,
    'feedback API should declare form review signature request executionId/password/comment contract'
  )
  assert.match(
    apiSource,
    /interface\s+ProFeedbackEdhrFormReviewSignRespVO[\s\S]*signatureId[\s\S]*actionType[\s\S]*meaningText/s,
    'feedback API should declare form review signature response contract'
  )
  assert.match(
    apiSource,
    /cosignEdhrExecution[\s\S]*\/mes\/pro\/batch-record-execution\/cosign/s,
    'feedback API should expose a cosignEdhrExecution method on the MES execution cosign endpoint'
  )
})

test('eDHR execution page requires password confirmation and becomes readonly after submit', () => {
  const pageSource = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(
    pageSource,
    /password/,
    'ExecutionPage should collect a password before submit'
  )
  assert.match(
    pageSource,
    /comment/,
    'ExecutionPage should allow an optional submit comment'
  )
  assert.match(
    pageSource,
    /Dialog|el-dialog|ElMessageBox\.prompt/,
    'ExecutionPage should open a submit confirmation dialog instead of submitting silently'
  )
  assert.match(
    pageSource,
    /submitEdhrExecution|\/submit\b/,
    'ExecutionPage should call the execution submit API'
  )
  assert.match(
    pageSource,
    /readonly|isReadonly|status\s*!==\s*0|status\s*===\s*0/,
    'ExecutionPage should switch to readonly once the execution is no longer draft'
  )
  assert.match(
    pageSource,
    /await loadExecution\(\)|loadExecution\(\)/,
    'ExecutionPage should refresh execution detail after submit'
  )
})

test('eDHR execution page exposes password-confirmed form review signature action', () => {
  const pageSource = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(pageSource, /复核签名/, 'ExecutionPage should expose a visible form review signature action')
  assert.match(pageSource, /FORM_REVIEW/, 'ExecutionPage should identify the form review signature action as FORM_REVIEW')
  assert.match(
    pageSource,
    /cosignEdhrExecution/,
    'ExecutionPage should call the cosign API instead of recording a local-only signature'
  )
  assert.match(
    pageSource,
    /hasPendingFieldChanges/,
    'ExecutionPage should block review signing while unsaved field changes exist'
  )
  assert.match(
    pageSource,
    /loadTrackingAndSignatures\(\)/,
    'ExecutionPage should refresh signature records after a form review signature is saved'
  )
})
