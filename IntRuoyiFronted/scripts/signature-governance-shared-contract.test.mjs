import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('signature governance frontend shared contract is explicit and non-mock', () => {
  const source = readText('src/api/signature-governance/shared.ts')

  assert.match(source, /export type SignatureGovernanceModuleCode/)
  for (const moduleCode of ["'DCC'", "'EDHR'", "'SHOWROOM'", "'INTAUTH'"]) {
    assert.match(source, new RegExp(moduleCode))
  }

  assert.match(source, /export type SignatureGovernanceBlockerCode/)
  for (const blockerCode of [
    'OWNER_MISSING',
    'OBJECT_LOCK_MISSING',
    'VERSIONING_MISSING',
    'DEFAULT_RETENTION_MISSING',
    'RECOVERY_RUNTIME_MISSING',
    'QUALITY_APPROVAL_MISSING',
    'POLICY_SOURCE_MISSING',
    'MODULE_ADAPTER_MISSING',
    'TEST_TENANT_MISSING'
  ]) {
    assert.match(source, new RegExp(`'${blockerCode}'`))
  }

  assert.match(source, /export interface SignatureGovernanceBlocker/)
  assert.match(source, /code: SignatureGovernanceBlockerCode/)
  assert.match(source, /message: string/)
  assert.match(source, /impact: string/)

  for (const permission of [
    'signature-governance:retention:query',
    'signature-governance:retention:manage',
    'signature-governance:periodic-review:query',
    'signature-governance:periodic-review:manage',
    'signature-governance:csv-package:query',
    'signature-governance:csv-package:manage',
    'signature-governance:policy:query',
    'signature-governance:policy:manage'
  ]) {
    assert.match(source, new RegExp(permission))
  }

  assert.doesNotMatch(source, /mock|placeholder|fallback|TODO/i)
})
