import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('BDD: eDHR submit gate scans all required fields before opening submit dialog', () => {
  const pageSource = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(
    pageSource,
    /missingRequiredFields\s*=\s*computed[\s\S]*snapshotFields\.value[\s\S]*field\.required[\s\S]*!field\.readonly[\s\S]*componentKind\s*!==\s*'signature'/,
    'ExecutionPage must compute missing required business fields from the current snapshot.'
  )
  assert.match(
    pageSource,
    /isRequiredTypedValueMissing[\s\S]*value\s*==\s*null[\s\S]*typeof value === 'string'[\s\S]*Array\.isArray\(value\)[\s\S]*Object\.keys\(value\)\.length === 0/s,
    'Required value checks must treat null, blank strings, empty arrays, and empty objects as missing.'
  )
  assert.match(
    pageSource,
    /return false[\s\S]*isRequiredTypedValueMissing/s,
    'Required value checks must leave boolean false and numeric zero as valid values.'
  )
  assert.match(
    pageSource,
    /missingRequiredFieldsSubmitError\s*=\s*computed[\s\S]*eDHR 必填字段未填写/,
    'ExecutionPage must expose a user-visible missing required field message.'
  )
  assert.match(
    pageSource,
    /formSubmitGateError\s*=\s*computed[\s\S]*missingRequiredFieldsSubmitError\.value[\s\S]*return missingRequiredFieldsSubmitError\.value/s,
    'Submit gate must block when required fields are missing.'
  )
  assert.match(
    pageSource,
    /openSubmitDialog[\s\S]*formSubmitGateError\.value[\s\S]*message\.error\(formSubmitGateError\.value\)[\s\S]*return/s,
    'The submit dialog must not open when required fields are missing.'
  )
  assert.match(
    pageSource,
    /handleSubmitExecution[\s\S]*formSubmitGateError\.value[\s\S]*message\.error\(formSubmitGateError\.value\)[\s\S]*return/s,
    'The final submit action must re-check the required-field gate before calling the backend.'
  )
})

console.log('PASS: eDHR required submit gate static contract')
