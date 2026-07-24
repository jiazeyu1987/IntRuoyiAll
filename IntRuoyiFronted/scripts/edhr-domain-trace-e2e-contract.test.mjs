import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import vm from 'node:vm'
import { createRequire } from 'node:module'

const scriptPath = path.resolve('tests/e2e/edhr-domain-trace-real-flow.e2e.js')

function loadHarness() {
  const source = fs.readFileSync(scriptPath, 'utf8')
  const patchedSource = source.replace(
    /\nmain\(\)\s*$/,
    '\nmodule.exports = { collectConfig, assertExpectedFinalSummary, writeEvidenceMarkdown, summarizeDomainTraceListRow }'
  )
  assert.notEqual(patchedSource, source, 'contract harness must prevent the real E2E main() from running')

  const sandbox = {
    require: createRequire(import.meta.url),
    module: { exports: {} },
    exports: {},
    process,
    console,
    __dirname: path.dirname(scriptPath),
    __filename: scriptPath,
    URL,
    setTimeout,
    clearTimeout
  }
  vm.runInNewContext(patchedSource, sandbox, { filename: scriptPath })
  return sandbox.module.exports
}

function withEnv(overrides, run) {
  const previous = {}
  for (const key of Object.keys(overrides)) {
    previous[key] = process.env[key]
    const value = overrides[key]
    if (value === undefined) {
      delete process.env[key]
    } else {
      process.env[key] = value
    }
  }

  try {
    return run()
  } finally {
    for (const [key, value] of Object.entries(previous)) {
      if (value === undefined) {
        delete process.env[key]
      } else {
        process.env[key] = value
      }
    }
  }
}

const validBaseEnv = {
  EDHR_E2E_BASE_URL: 'http://localhost:8081',
  EDHR_E2E_TENANT: '测试租户',
  EDHR_E2E_EXECUTOR_USERNAME: 'executor',
  EDHR_E2E_EXECUTOR_PASSWORD: 'secret',
  EDHR_E2E_DOMAIN_TRACE_EXECUTION_ID: '123456',
  EDHR_E2E_DOMAIN_TRACE_EXECUTION_CODE: 'BR-20260528-001',
  EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS: undefined,
  EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT: undefined
}

test('BDD: 主数据追溯列表真实路径 -> E2E 必须先打开列表、等待分页并从列表进入详情', () => {
  const source = fs.readFileSync(scriptPath, 'utf8')

  assert.match(
    source,
    /DEFAULT_DOMAIN_TRACE_LIST_ROUTE\s*=\s*['"]\/mes\/pro\/feedback\/edhr-domain-trace['"]/,
    'E2E 脚本必须声明主数据追溯列表默认路由常量。'
  )
  assert.match(
    source,
    /DOMAIN_TRACE_PAGE_ENDPOINT\s*=\s*['"]\/mes\/pro\/batch-record-execution\/domain-trace\/page['"]/,
    'E2E 脚本必须声明主数据追溯分页 endpoint 常量。'
  )
  assert.match(
    source,
    /BDD: 主数据追溯列表可查询[\s\S]*\/domain-trace\/page[\s\S]*executionCode[\s\S]*domainTraceHash[\s\S]*blockerCount[\s\S]*itemCount/,
    'BDD 场景必须覆盖列表查询、分页响应和列表关键字段证据。'
  )
  assert.match(
    source,
    /async function openDomainTraceList\([^)]*\)[\s\S]*DOMAIN_TRACE_PAGE_ENDPOINT[\s\S]*buildDomainTraceListUrl[\s\S]*parseJsonResponse/,
    'E2E 脚本必须提供打开列表并解析真实分页响应的 helper。'
  )
  assert.match(
    source,
    /function findDomainTraceListRow\([^)]*\)[\s\S]*executionId[\s\S]*executionCode/,
    'E2E 脚本必须提供按 executionId 或 executionCode 选择目标行的 helper。'
  )
  assert.match(
    source,
    /domain-trace-list/,
    'E2E 证据必须包含列表步骤或截图标记 domain-trace-list。'
  )
})

test('BDD: 主数据追溯列表计数来源 -> E2E 按真实 UI 合同计算并 fail fast', () => {
  const { summarizeDomainTraceListRow } = loadHarness()

  const itemArraySummary = summarizeDomainTraceListRow(
    {
      executionId: 40,
      executionCode: 'BRE202605280518101280040',
      status: 'VERIFIED',
      domainTraceHash: 'abc123def456',
      blockerCount: 0,
      items: [{ itemKey: 'A' }, { itemKey: 'B' }]
    },
    '主数据追溯列表目标行'
  )
  assert.equal(itemArraySummary.blockerCount, 0)
  assert.equal(itemArraySummary.itemCount, 2)

  const blockerArraySummary = summarizeDomainTraceListRow(
    {
      executionId: 41,
      executionCode: 'BRE202605280518101280041',
      status: 'BLOCKED',
      domainTraceHash: 'def456abc123',
      blockers: [{ blockerCode: 'MISSING_MASTER' }],
      itemCount: '3'
    },
    '主数据追溯列表目标行'
  )
  assert.equal(blockerArraySummary.blockerCount, 1)
  assert.equal(blockerArraySummary.itemCount, 3)

  assert.throws(
    () =>
      summarizeDomainTraceListRow(
        {
          status: 'VERIFIED',
          domainTraceHash: 'abc123def456',
          itemCount: 1
        },
        '主数据追溯列表目标行'
      ),
    /缺少 blockerCount 来源/
  )
  assert.throws(
    () =>
      summarizeDomainTraceListRow(
        {
          status: 'VERIFIED',
          domainTraceHash: 'abc123def456',
          blockerCount: 0
        },
        '主数据追溯列表目标行'
      ),
    /缺少 itemCount 来源/
  )
  assert.throws(
    () =>
      summarizeDomainTraceListRow(
        {
          status: 'VERIFIED',
          domainTraceHash: 'abc123def456',
          blockerCount: 0,
          itemCount: 0
        },
        '主数据追溯列表目标行'
      ),
    /itemCount 必须大于 0/
  )
})

test('collectConfig accepts explicit VERIFIED/BLOCKED status and non-negative blocker count expectations', () => {
  const { collectConfig } = loadHarness()

  const config = withEnv(
    {
      ...validBaseEnv,
      EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS: 'VERIFIED',
      EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT: '0'
    },
    () => collectConfig()
  )

  assert.equal(config.expectedStatus, 'VERIFIED')
  assert.equal(config.expectedBlockerCount, 0)
  assert.equal(config.missing.length, 0)

  const blockedConfig = withEnv(
    {
      ...validBaseEnv,
      EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS: 'BLOCKED',
      EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT: '2'
    },
    () => collectConfig()
  )
  assert.equal(blockedConfig.expectedStatus, 'BLOCKED')
  assert.equal(blockedConfig.expectedBlockerCount, 2)
})

test('collectConfig rejects invalid expected status and blocker count fail-fast', () => {
  const { collectConfig } = loadHarness()

  const config = withEnv(
    {
      ...validBaseEnv,
      EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS: 'PASS',
      EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT: '-1'
    },
    () => collectConfig()
  )

  assert.equal(config.invalidConfig, true)
  assert.equal(config.missing.length, 2)
  const invalidKeys = config.missing.map((item) => item.key).sort()
  assert.equal(invalidKeys[0], 'EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT')
  assert.equal(invalidKeys[1], 'EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS')
})

test('assertExpectedFinalSummary fails closed on status or blocker count mismatch', () => {
  const { assertExpectedFinalSummary } = loadHarness()
  const config = {
    expectedStatus: 'VERIFIED',
    expectedBlockerCount: 0
  }
  const actualSummary = {
    status: 'BLOCKED',
    blockerCount: 1
  }

  assert.throws(
    () => assertExpectedFinalSummary(config, actualSummary),
    /Expected final DomainTrace status VERIFIED, actual BLOCKED/
  )

  assert.throws(
    () => assertExpectedFinalSummary({ expectedBlockerCount: 0 }, actualSummary),
    /Expected final DomainTrace blockerCount 0, actual 1/
  )
})

test('writeEvidenceMarkdown records expected and actual final status and blocker count', () => {
  const { writeEvidenceMarkdown } = loadHarness()
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'edhr-domain-trace-contract-'))
  const evidenceFile = path.join(tmpDir, 'evidence.md')

  writeEvidenceMarkdown(
    {
      status: 'PASS',
      steps: [],
      trace: path.join(tmpDir, 'trace.zip'),
      expectedSummary: {
        status: 'VERIFIED',
        blockerCount: 0
      },
      finalSummary: {
        status: 'VERIFIED',
        hash: 'abc123def456',
        blockerCount: 0,
        itemCount: 3
      }
    },
    evidenceFile
  )

  const markdown = fs.readFileSync(evidenceFile, 'utf8')
  assert.match(markdown, /Expected final status: `VERIFIED`/)
  assert.match(markdown, /Actual final status: `VERIFIED`/)
  assert.match(markdown, /Expected final blocker count: `0`/)
  assert.match(markdown, /Actual final blocker count: `0`/)
})

test('writeEvidenceMarkdown records the configured task id for audit traceability', () => {
  const { writeEvidenceMarkdown } = withEnv(
    { EDHR_E2E_TASK_ID: '20260528-edhr-domain-trace-verified-e2e' },
    () => loadHarness()
  )
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'edhr-domain-trace-contract-'))
  const evidenceFile = path.join(tmpDir, 'evidence.md')

  writeEvidenceMarkdown(
    {
      status: 'PASS',
      steps: [],
      trace: path.join(tmpDir, 'trace.zip')
    },
    evidenceFile
  )

  const markdown = fs.readFileSync(evidenceFile, 'utf8')
  assert.match(markdown, /Task ID: `20260528-edhr-domain-trace-verified-e2e`/)
})
