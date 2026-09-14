import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  RELEASE_CHECK_COMMAND,
  RELEASE_CHECK_SCRIPT,
  RELEASE_E2E_COVERAGE_MATRIX,
  RELEASE_COVERAGE_EXCLUDED_EDHR_SOURCE_FILES,
  RELEASE_REAL_COMMAND,
  RELEASE_REAL_SCRIPT,
  REQUIRED_FEATURE_IDS,
  getOrderedE2eFiles,
  getOrderedPackageScripts,
  main,
  runCheckMode,
  runRealMode,
  validateCoverageContract
} from './edhr-release-e2e-coverage-gate.mjs'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.resolve(repoRoot, relativePath), 'utf8')
}

function loadPackageJson() {
  return JSON.parse(readUtf8('package.json'))
}

function unique(values) {
  return [...new Set(values)]
}

function withTempReport(callback) {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'edhr-release-coverage-report-'))
  const reportPath = path.join(tempDir, 'report.json')
  try {
    callback(reportPath)
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}

function withTempDir(prefix, callback) {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), prefix))
  try {
    callback(tempDir)
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}

function readJson(reportPath) {
  return JSON.parse(fs.readFileSync(reportPath, 'utf8'))
}

function withSilencedConsole(callback) {
  const originalLog = console.log
  const originalError = console.error
  console.log = () => {}
  console.error = () => {}
  try {
    return callback()
  } finally {
    console.log = originalLog
    console.error = originalError
  }
}

test('package scripts expose the release check and real gates', () => {
  const scripts = loadPackageJson().scripts || {}

  assert.equal(scripts[RELEASE_CHECK_SCRIPT], RELEASE_CHECK_COMMAND)
  assert.equal(scripts[RELEASE_REAL_SCRIPT], RELEASE_REAL_COMMAND)
})

test('matrix contains the complete eDHR release feature set once', () => {
  const ids = RELEASE_E2E_COVERAGE_MATRIX.map((item) => item.featureId)

  assert.deepEqual(ids, REQUIRED_FEATURE_IDS)
  assert.equal(unique(ids).length, ids.length)
})

test('excluded non-release eDHR source files are explicit and present', () => {
  const exclusions = RELEASE_COVERAGE_EXCLUDED_EDHR_SOURCE_FILES

  assert.equal(unique(exclusions).length, exclusions.length)
  for (const relativePath of exclusions) {
    assert.equal(
      fs.existsSync(path.resolve(repoRoot, relativePath)),
      true,
      `${relativePath} must exist while excluded from the release-core E2E gate`
    )
  }
})

test('matrix binds every feature to source, API, route, E2E, package, check, and task evidence', () => {
  for (const item of RELEASE_E2E_COVERAGE_MATRIX) {
    assert.ok(item.featureName, `${item.featureId} featureName`)
    assert.ok(item.packageScript, `${item.featureId} packageScript`)
    assert.ok(item.checkScript, `${item.featureId} checkScript`)
    assert.ok(item.e2eFile, `${item.featureId} e2eFile`)
    assert.ok(item.taskEvidence, `${item.featureId} taskEvidence`)
    assert.ok(item.routes.length > 0, `${item.featureId} routes`)
    assert.ok(item.sourceFiles.length > 0, `${item.featureId} sourceFiles`)
    assert.ok(item.apiTokens.length > 0, `${item.featureId} apiTokens`)
    assert.ok(item.e2eTokens.length > 0, `${item.featureId} e2eTokens`)
  }

  const validation = validateCoverageContract({ cwd: repoRoot })
  assert.deepEqual(validation.failures, [])
  assert.equal(validation.ok, true)
})

test('source-only API tokens cannot satisfy real E2E token coverage', () => {
  const sourceOnlyApprovalDetailMatrix = [
    {
      featureId: 'approval-workbench/detail-approve-reject',
      featureName: 'regression source-only approval detail token',
      routes: ['/mes/pro/feedback/edhr-approval/detail'],
      sourceFiles: [
        'src/views/mes/pro/edhr/ApprovalPage.vue',
        'src/api/mes/pro/edhr/approval.ts'
      ],
      apiTokens: ['/mes/pro/batch-record-execution/approval-detail'],
      e2eTokens: ['/mes/pro/batch-record-execution/approval-detail'],
      e2eFile: 'tests/e2e/edhr-batch-execution-real-flow.e2e.js',
      packageScript: 'e2e:edhr:batch-execution',
      checkScript: 'e2e:edhr:batch-execution:check',
      taskEvidence: 'doc/tasks/20260608-edhr-batch-execution-full-flow/real-e2e-evidence.md'
    }
  ]

  const validation = validateCoverageContract({
    cwd: repoRoot,
    matrix: sourceOnlyApprovalDetailMatrix,
    packageJson: loadPackageJson(),
    expectedFeatureIds: ['approval-workbench/detail-approve-reject']
  })

  assert.equal(validation.ok, false)
  assert.ok(
    validation.failures.some((failure) =>
      failure.includes('approval-workbench/detail-approve-reject') &&
      failure.includes('/mes/pro/batch-record-execution/approval-detail') &&
      failure.includes('E2E script code/string only')
    ),
    `expected source-only approval-detail to fail real E2E token validation, got: ${validation.failures.join('; ')}`
  )
})

test('E2E bindings do not contain mock interception, hidden skips, default success, or default passwords', () => {
  const forbiddenPassword = ['admin', '123'].join('')

  for (const e2eFile of getOrderedE2eFiles(RELEASE_E2E_COVERAGE_MATRIX)) {
    const source = readUtf8(e2eFile)

    assert.doesNotMatch(source, /\bpage\.route\s*\(/, `${e2eFile} must not intercept routes`)
    assert.doesNotMatch(source, /\broute\.fulfill\s*\(/, `${e2eFile} must not fulfill mocked responses`)
    assert.doesNotMatch(source, /\.skip\s*\(/, `${e2eFile} must not skip tests`)
    assert.doesNotMatch(source, /\b(?:test|it|describe)\.skip\b/, `${e2eFile} must not skip tests`)
    assert.doesNotMatch(source, /\bprocess\.exitCode\s*=\s*0\b/, `${e2eFile} must not force success`)
    assert.doesNotMatch(source, /\bprocess\.exit\s*\(\s*0\s*\)/, `${e2eFile} must not force success`)
    assert.doesNotMatch(source, /\bDEFAULT_PASSWORD\b/, `${e2eFile} must not define a default password`)
    assert.equal(source.toLowerCase().includes(forbiddenPassword), false, `${e2eFile} must not contain plaintext default password`)
  }
})

test('check mode runs only static check package scripts and node syntax checks', () => {
  const calls = []
  const result = runCheckMode({
    cwd: repoRoot,
    commandRunner: (commandSpec) => {
      calls.push(commandSpec)
      return { status: 0 }
    }
  })

  assert.equal(result.ok, true)
  assert.deepEqual(
    calls.filter((call) => call.kind === 'package-check').map((call) => call.scriptName),
    getOrderedPackageScripts(RELEASE_E2E_COVERAGE_MATRIX, 'checkScript')
  )
  assert.deepEqual(
    calls.filter((call) => call.kind === 'node-check').map((call) => call.file),
    getOrderedE2eFiles(RELEASE_E2E_COVERAGE_MATRIX)
  )
  assert.equal(calls.some((call) => call.kind === 'package-real'), false)
  assert.equal(
    calls.some((call) =>
      getOrderedPackageScripts(RELEASE_E2E_COVERAGE_MATRIX, 'packageScript').includes(call.scriptName)
    ),
    false
  )
})

test('check mode writes a successful machine-readable report with --report path', () => {
  withTempReport((reportPath) => {
    const calls = []
    const code = withSilencedConsole(() =>
      main(['--check', '--report', reportPath], repoRoot, {
        commandRunner: (commandSpec) => {
          calls.push(commandSpec)
          return { status: 0 }
        }
      })
    )

    assert.equal(code, 0)
    const report = readJson(reportPath)

    assert.equal(report.schemaVersion, 1)
    assert.equal(report.mode, 'check')
    assert.equal(report.status, 'passed')
    assert.match(report.generatedAt, /^\d{4}-\d{2}-\d{2}T/)
    assert.match(report.command, /--check/)
    assert.match(report.command, /--report/)
    assert.equal(report.featureCount, RELEASE_E2E_COVERAGE_MATRIX.length)
    assert.deepEqual(report.checkedScripts, getOrderedPackageScripts(RELEASE_E2E_COVERAGE_MATRIX, 'checkScript'))
    assert.deepEqual(report.checkedE2eFiles, getOrderedE2eFiles(RELEASE_E2E_COVERAGE_MATRIX))
    assert.deepEqual(report.failures, [])
    assert.equal(report.realGateClaimed, false)
    assert.deepEqual(
      report.features,
      RELEASE_E2E_COVERAGE_MATRIX.map((item) => ({
        featureId: item.featureId,
        featureName: item.featureName,
        e2eFile: item.e2eFile,
        packageScript: item.packageScript,
        checkScript: item.checkScript,
        status: 'passed'
      }))
    )
    assert.equal(calls.some((call) => call.kind === 'package-real'), false)
  })
})

test('check mode writes a failed report with --report=path when coverage validation fails', () => {
  withTempReport((reportPath) => {
    const failingMatrix = RELEASE_E2E_COVERAGE_MATRIX.map((item, index) =>
      index === 0
        ? { ...item, e2eTokens: ['__missing_real_e2e_token_for_report_contract__'] }
        : item
    )
    const calls = []
    const code = withSilencedConsole(() =>
      main(['--check', `--report=${reportPath}`], repoRoot, {
        matrix: failingMatrix,
        commandRunner: (commandSpec) => {
          calls.push(commandSpec)
          return { status: 0 }
        }
      })
    )

    assert.equal(code, 1)
    const report = readJson(reportPath)

    assert.equal(report.schemaVersion, 1)
    assert.equal(report.mode, 'check')
    assert.equal(report.status, 'failed')
    assert.equal(report.realGateClaimed, false)
    assert.ok(report.failures.some((failure) => failure.includes('__missing_real_e2e_token_for_report_contract__')))
    assert.equal(report.features[0].featureId, RELEASE_E2E_COVERAGE_MATRIX[0].featureId)
    assert.equal(report.features[0].status, 'failed')
    assert.equal(report.features.some((feature) => feature.status === 'failed'), true)
    assert.deepEqual(calls, [])
  })
})

test('report argument rejects option-like path values before running the gate', () => {
  const cases = [
    {
      argv: ['--check', '--report', '--unknown'],
      forbiddenReportName: '--unknown'
    },
    {
      argv: ['--check', '--report=-x'],
      forbiddenReportName: '-x'
    }
  ]

  for (const { argv, forbiddenReportName } of cases) {
    withTempDir('edhr-release-coverage-invalid-report-', (tempDir) => {
      const forbiddenReportPath = path.join(tempDir, forbiddenReportName)
      const calls = []

      assert.throws(
        () =>
          withSilencedConsole(() =>
            main(argv, tempDir, {
              commandRunner: (commandSpec) => {
                calls.push(commandSpec)
                return { status: 0 }
              }
            })
          ),
        /--report requires a non-empty path that does not start with "-"/
      )
      assert.deepEqual(calls, [])
      assert.equal(fs.existsSync(forbiddenReportPath), false)
    })
  }
})

test('run-real calls real package scripts in matrix order and fails fast', () => {
  const calls = []
  const realScripts = getOrderedPackageScripts(RELEASE_E2E_COVERAGE_MATRIX, 'packageScript')
  const result = runRealMode({
    cwd: repoRoot,
    commandRunner: (commandSpec) => {
      calls.push(commandSpec)
      return { status: calls.length === 2 ? 17 : 0 }
    }
  })

  assert.equal(result.ok, false)
  assert.equal(result.code, 17)
  assert.deepEqual(
    calls.map((call) => call.scriptName),
    realScripts.slice(0, 2)
  )
})

test('release gate files and package metadata do not contain plaintext default password', () => {
  const forbiddenPassword = ['admin', '123'].join('')
  const checkedFiles = [
    'package.json',
    'scripts/edhr-release-e2e-coverage-gate.mjs',
    'scripts/edhr-release-e2e-coverage-contract.test.mjs',
    ...getOrderedE2eFiles(RELEASE_E2E_COVERAGE_MATRIX)
  ]

  for (const relativePath of checkedFiles) {
    const source = readUtf8(relativePath)
    assert.equal(
      source.toLowerCase().includes(forbiddenPassword),
      false,
      `${relativePath} must not contain plaintext default password`
    )
  }
})
