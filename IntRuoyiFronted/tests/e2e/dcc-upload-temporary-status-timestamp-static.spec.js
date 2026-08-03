const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const readSource = (absoluteOrRelativePath, root = frontendRoot) => {
  const absolutePath = path.isAbsolute(absoluteOrRelativePath)
    ? absoluteOrRelativePath
    : path.join(root, absoluteOrRelativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${absolutePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const backendRespVO = readSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileUploadTemporaryStatusRespVO.java',
  workspaceRoot
)

assert.match(
  backendRespVO,
  /private LocalDateTime expireTime;/,
  'backend temporary status response emits expireTime through the global LocalDateTime timestamp serializer'
)

assert.match(
  backendRespVO,
  /private LocalDateTime cleanupTime;/,
  'backend temporary status response emits cleanupTime through the global LocalDateTime timestamp serializer'
)

assert.match(
  workflowApi,
  /export interface ControlledFileUploadTemporaryStatusRespVO[\s\S]*expireTime\?: number/,
  'frontend temporary status contract must type expireTime as numeric timestamp'
)

assert.match(
  workflowApi,
  /export interface ControlledFileUploadTemporaryStatusRespVO[\s\S]*cleanupTime\?: number/,
  'frontend temporary status contract must type cleanupTime as numeric timestamp'
)

assert.match(
  workflowApi,
  /const readOptionalTimestamp = \(payload: Record<string, unknown>, field: string\): number \| undefined =>/,
  'frontend API must provide an explicit numeric timestamp decoder'
)

assert.match(
  workflowApi,
  /if \(typeof value !== 'number' \|\| !Number\.isFinite\(value\)\) \{[\s\S]*DCC response field has invalid type/,
  'timestamp decoder must fail fast on non-numeric timestamp fields'
)

assert.match(
  workflowApi,
  /expireTime:\s*readOptionalTimestamp\(payload,\s*'expireTime'\)/,
  'temporary status parser must decode expireTime as a numeric timestamp'
)

assert.match(
  workflowApi,
  /cleanupTime:\s*readOptionalTimestamp\(payload,\s*'cleanupTime'\)/,
  'temporary status parser must decode cleanupTime as a numeric timestamp'
)

assert.doesNotMatch(
  workflowApi,
  /expireTime:\s*readOptionalString\(payload,\s*'expireTime'\)/,
  'temporary status parser must not decode expireTime as string'
)

assert.doesNotMatch(
  workflowApi,
  /cleanupTime:\s*readOptionalString\(payload,\s*'cleanupTime'\)/,
  'temporary status parser must not decode cleanupTime as string'
)

console.log('PASS: DCC upload temporary status timestamp contract')
