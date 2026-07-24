import fs from 'node:fs'
import path from 'node:path'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const uiRoot = path.resolve(__dirname, '..')
const projectRoot = path.resolve(uiRoot, '..')

const args = new Set(process.argv.slice(2))
const expectBlocked = args.has('--expect-blocked')
const jsonOutput = args.has('--json')

function readText(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}

function uniqueMessages(messages) {
  return [...new Set((messages || []).filter(Boolean))]
}

function fileExists(relativePath) {
  return fs.existsSync(path.join(projectRoot, relativePath))
}

function fileSha256(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath)
  return fs.existsSync(absolutePath)
    ? createHash('sha256').update(fs.readFileSync(absolutePath)).digest('hex')
    : ''
}

function findFiles(root, fileName) {
  const absoluteRoot = path.join(projectRoot, root)
  if (!fs.existsSync(absoluteRoot)) {
    return []
  }

  const results = []
  const stack = [absoluteRoot]
  while (stack.length) {
    const current = stack.pop()
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const entryPath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        stack.push(entryPath)
      } else if (entry.name === fileName) {
        results.push({
          relativePath: path.relative(projectRoot, entryPath).replaceAll(path.sep, '/'),
          mtimeMs: fs.statSync(entryPath).mtimeMs
        })
      }
    }
  }
  return results.sort((left, right) => right.mtimeMs - left.mtimeMs || right.relativePath.localeCompare(left.relativePath))
}

function jsonArtifactPassed(fileName, predicate = () => true) {
  const files = findFiles('doc/tasks', fileName)
  if (!files.length) {
    return { ok: false, evidence: '<missing>' }
  }

  const latestArtifactOnly = files[0]
  let parseError = ''
  let failureDetails = ''
  try {
    const payload = JSON.parse(readText(latestArtifactOnly.relativePath))
    if (payload.status === 'PASS' && predicate(payload)) {
      return { ok: true, evidence: latestArtifactOnly.relativePath }
    }
    failureDetails = artifactFailureDetails(payload)
  } catch (error) {
    parseError = `; invalid JSON artifact parseError=${error.message}`
  }

  return {
    ok: false,
    evidence: `${latestArtifactOnly.relativePath} (newest matching artifact failed contract${parseError}${failureDetails}; ${files.length} matching artifacts found; found but no PASS artifact satisfied contract)`
  }
}

function assertionBag(payload) {
  return payload.finalAssertions || payload.assertions || payload.result?.finalAssertions || payload.result?.assertions || {}
}

function artifactFailureDetails(payload) {
  const assertions = assertionBag(payload)
  const failedAssertions = uniqueMessages(Object.entries(assertions)
    .filter(([, value]) => value === false)
    .map(([key]) => key))
  const artifactBlockers = uniqueMessages([
    ...(Array.isArray(payload.blockers) ? payload.blockers : []),
    ...(Array.isArray(payload.result?.blockers) ? payload.result.blockers : []),
    ...(Array.isArray(payload.evidence?.blockers) ? payload.evidence.blockers : [])
  ].map((blocker) => {
    if (typeof blocker === 'string') return blocker
    if (blocker?.reason) return blocker.reason
    if (blocker?.message) return blocker.message
    return JSON.stringify(blocker)
  }))
  const parts = []
  if (payload.status && payload.status !== 'PASS') {
    parts.push(`artifactStatus=${payload.status}`)
  }
  if (failedAssertions.length > 0) {
    parts.push(`failedAssertions=${failedAssertions.join('|')}`)
  }
  if (artifactBlockers.length > 0) {
    parts.push(`artifactBlockers=${artifactBlockers.join('|')}`)
  }
  return parts.length > 0 ? `; ${parts.join('; ')}` : ''
}

function contentTypeOf(payload) {
  return payload.contentType || payload.documentType || payload.result?.contentType || assertionBag(payload).contentType
}

function requireTestTenantPlaywrightWriteArtifact(payload, predicate) {
  const tenant = payload.tenant || payload.tenantName || payload.environment?.tenantName
  const username = payload.username || payload.actorUsername || payload.actor?.username
  const assertions = assertionBag(payload)
  const userPath = payload.userPath || payload.userPaths || payload.steps
  const writeRequests = payload.writeRequests || payload.networkWriteRequests

  return (
    tenant === '测试租户' &&
    username === 'aoteman' &&
    payload.executionMode === 'playwright-ui' &&
    payload.writeChannel === 'frontend-ui' &&
    payload.directApiWrites === 0 &&
    payload.sqlBusinessDataWritePerformed === false &&
    payload.mockDataUsed === false &&
    Array.isArray(userPath) &&
    userPath.length > 0 &&
    Array.isArray(writeRequests) &&
    writeRequests.length > 0 &&
    predicate(payload, assertions)
  )
}

function requireLiveMigrationArtifact(payload) {
  const tables = payload.postCheck?.controlledContentTablesAfterMigration || payload.controlledContentTablesAfterMigration || []
  const blockers = payload.preflight?.blockers || []
  const scriptSha256 = payload.scriptSha256 || payload.migration?.scriptSha256
  return (
    payload.kind === 'live-migration' &&
    scriptSha256 === currentMigrationScriptSha256 &&
    payload.migration?.result === 'PASS' &&
    Array.isArray(blockers) &&
    blockers.length === 0 &&
    Array.isArray(tables) &&
    tables.includes('controlled_content_version_ref') &&
    tables.includes('controlled_content_transition_audit') &&
    payload.postCheck?.sqlBusinessDataWritePerformed === false
  )
}

function requireReadonlyHealthArtifact(payload) {
  const guarantee = payload.readonlyGuarantee || {}
  const checkedTables = payload.checkedTables || payload.tables || []
  const migrationScriptSha256 = payload.migrationScriptSha256 || payload.sourceMigration?.scriptSha256
  return (
    payload.kind === 'health-check-readonly' &&
    migrationScriptSha256 === currentMigrationScriptSha256 &&
    Array.isArray(checkedTables) &&
    checkedTables.includes('controlled_content_version_ref') &&
    checkedTables.includes('controlled_content_transition_audit') &&
    guarantee.healthCheckWritesExecuted === false &&
    guarantee.autoRepairExecuted === false &&
    guarantee.activeRollbackExecuted === false &&
    guarantee.successAuditBackfillExecuted === false
  )
}

const implementationReport = readText('doc/tasks/20260718-controlled-content-state-machine-implementation/verification-report.md')
const hardeningReport = readText('doc/tasks/20260719-controlled-content-doc-dev-test-completion/verification-report.md')
const fullObjectiveMatrix = readText('doc/tasks/20260719-controlled-content-full-objective-completion-audit/completion-claim-matrix.md')
const stateMachineSpec = readText('yudao-ui-admin-vue3/tests/e2e/controlled-content-version-state-machine.spec.ts')
const packageJson = JSON.parse(readText('yudao-ui-admin-vue3/package.json') || '{}')
const currentMigrationScriptSha256 = fileSha256('ruoyi-vue-pro/sql/mysql/20260718_controlled_content_lifecycle.sql')

const readonlyDccArtifact = jsonArtifactPassed(
  'controlled-content-state-view-real-readonly.json',
  (payload) => Array.isArray(payload.writeRequests) && payload.writeRequests.length === 0
)
const dccSopArtifact = jsonArtifactPassed('controlled-content-dcc-sop-release-real.json', (payload) =>
  requireTestTenantPlaywrightWriteArtifact(
    payload,
    (_payload, assertions) =>
      contentTypeOf(payload) === 'SOP' && assertions.oldStatus === 'SUPERSEDED' && assertions.newStatus === 'ACTIVE'
  )
)
const dccWorkInstructionArtifact = jsonArtifactPassed(
  'controlled-content-dcc-work-instruction-review-readonly-real.json',
  (payload) =>
    requireTestTenantPlaywrightWriteArtifact(
      payload,
      (_payload, assertions) =>
        contentTypeOf(payload) === 'WORK_INSTRUCTION' &&
        assertions.reviewStatus === 'IN_REVIEW' &&
        assertions.uiReadonly === true &&
        assertions.directEditRejected === true
    )
)
const dccInspectionArtifact = jsonArtifactPassed('controlled-content-dcc-inspection-withdraw-draft-real.json', (payload) =>
  requireTestTenantPlaywrightWriteArtifact(
    payload,
    (_payload, assertions) =>
      contentTypeOf(payload) === 'INSPECTION_PROCEDURE' &&
      assertions.withdrawnStatus === 'WITHDRAWN' &&
      assertions.newDraftCount === 1 &&
      assertions.sourceActiveUnchanged === true
  )
)
const dccDrawingObsoleteArtifact = jsonArtifactPassed('controlled-content-dcc-drawing-obsolete-real.json', (payload) =>
  requireTestTenantPlaywrightWriteArtifact(
    payload,
    (_payload, assertions) =>
      contentTypeOf(payload) === 'DRAWING' &&
      assertions.masterCurrentActiveCleared === true &&
      assertions.activeCount === 0 &&
      assertions.noFallbackToOldVersion === true
  )
)
const mesRouteArtifact = jsonArtifactPassed('controlled-content-mes-route-version-full-flow-real.json', (payload) =>
  requireTestTenantPlaywrightWriteArtifact(
    payload,
    (_payload, assertions) =>
      payload.domain === 'MES_ROUTE' &&
      assertions.createdCandidate === true &&
      assertions.submitted === true &&
      assertions.withdrawn === true &&
      assertions.editedAfterWithdraw === true &&
      assertions.published === true &&
      assertions.oldActiveStatus === 'SUPERSEDED' &&
      assertions.newActiveStatus === 'ACTIVE'
  )
)
const migrationArtifact = jsonArtifactPassed('controlled-content-live-migration-real.json', requireLiveMigrationArtifact)
const healthArtifact = jsonArtifactPassed('controlled-content-health-check-readonly-real.json', requireReadonlyHealthArtifact)

const requirements = [
  {
    key: 'backend-core-regression',
    description: 'System/MES/DCC controlled-content unit and integration regression evidence exists.',
    ok:
      /ControlledContent\*Test[\s\S]+30 tests/.test(hardeningReport) &&
      /MesProRouteControlledContentAdapterTest/.test(hardeningReport) &&
      /DccControlledContentAdapterTest/.test(hardeningReport),
    evidence: 'doc/tasks/20260719-controlled-content-doc-dev-test-completion/verification-report.md'
  },
  {
    key: 'sql-static-contract',
    description: 'SQL lifecycle migration contract is covered by backend-owned static tests.',
    ok:
      fileExists('ruoyi-vue-pro/script/tests/test_controlled_content_lifecycle_sql.py') &&
      /test_controlled_content_lifecycle_sql\.py[\s\S]+5 tests/.test(hardeningReport),
    evidence: 'ruoyi-vue-pro/script/tests/test_controlled_content_lifecycle_sql.py'
  },
  {
    key: 'frontend-state-view-readonly',
    description: 'Existing DCC state view readonly path is covered and does not write DCC data.',
    ok:
      readonlyDccArtifact.ok &&
      /e2e:controlled-content:state-view:real|controlled-content-state-view-real-readonly/.test(implementationReport),
    evidence: readonlyDccArtifact.evidence
  },
  {
    key: 'state-machine-spec-not-full-e2e',
    description: 'The state-machine Playwright spec must not be mistaken for full write E2E until it references release matrix artifacts.',
    ok:
      /controlled-content-dcc-sop-release-real/.test(stateMachineSpec) &&
      /controlled-content-mes-route-version-full-flow-real/.test(stateMachineSpec),
    evidence: 'yudao-ui-admin-vue3/tests/e2e/controlled-content-version-state-machine.spec.ts'
  },
  {
    key: 'dcc-sop-release-real',
    description: 'DCC SOP real write E2E covers upload, submit, approve, publish, old SUPERSEDED and new ACTIVE.',
    ok: dccSopArtifact.ok,
    evidence: dccSopArtifact.evidence
  },
  {
    key: 'dcc-work-instruction-review-readonly-real',
    description: 'DCC work instruction real write E2E proves in-review versions are readonly and direct edits are rejected.',
    ok: dccWorkInstructionArtifact.ok,
    evidence: dccWorkInstructionArtifact.evidence
  },
  {
    key: 'dcc-inspection-withdraw-draft-real',
    description: 'DCC inspection procedure real write E2E proves WITHDRAWN old revision plus one new DRAFT with active source inheritance.',
    ok: dccInspectionArtifact.ok,
    evidence: dccInspectionArtifact.evidence
  },
  {
    key: 'dcc-drawing-obsolete-real',
    description: 'DCC drawing real write E2E proves obsolete clears current active, leaves zero active, and does not fall back.',
    ok: dccDrawingObsoleteArtifact.ok,
    evidence: dccDrawingObsoleteArtifact.evidence
  },
  {
    key: 'mes-route-version-full-flow-real',
    description: 'MES route version real write E2E covers create candidate, submit, withdraw, edit, publish, and post-read verification.',
    ok: mesRouteArtifact.ok,
    evidence: mesRouteArtifact.evidence
  },
  {
    key: 'live-migration-real',
    description: 'Live MySQL migration/preflight evidence exists for controlled-content lifecycle tables.',
    ok: migrationArtifact.ok,
    evidence: migrationArtifact.evidence
  },
  {
    key: 'health-check-readonly-real',
    description: 'Runtime health check evidence proves inconsistencies are reported readonly with no auto repair.',
    ok: healthArtifact.ok,
    evidence: healthArtifact.evidence
  },
  {
    key: 'full-objective-audit-recorded',
    description: 'Current audit explicitly records partial status and missing write E2E instead of claiming full completion.',
    ok: /PARTIAL，不可宣称完整完成/.test(fullObjectiveMatrix),
    evidence: 'doc/tasks/20260719-controlled-content-full-objective-completion-audit/completion-claim-matrix.md'
  },
  {
    key: 'package-release-gate-entry',
    description: 'Frontend package exposes the release claim gate as a named command.',
    ok:
      packageJson.scripts?.['e2e:controlled-content:release-gate'] ===
      'node scripts/controlled-content-release-claim-gate.mjs',
    evidence: 'yudao-ui-admin-vue3/package.json'
  }
]

const passed = requirements.filter((requirement) => requirement.ok)
const blockers = requirements.filter((requirement) => !requirement.ok)
const result = {
  status: blockers.length ? 'BLOCKED' : 'PASS',
  checkedAt: new Date().toISOString(),
  passed: passed.map(({ key, description, evidence }) => ({ key, description, evidence })),
  blockers: blockers.map(({ key, description, evidence }) => ({ key, description, evidence }))
}

if (jsonOutput) {
  process.stdout.write(`${JSON.stringify(result, null, 2)}\n`)
} else {
  process.stdout.write(`controlled-content-release-claim-gate: ${result.status}\n`)
  for (const blocker of result.blockers) {
    process.stdout.write(`BLOCKER ${blocker.key}: ${blocker.description} [evidence=${blocker.evidence}]\n`)
  }
}

if (expectBlocked) {
  process.exit(result.status === 'BLOCKED' ? 0 : 1)
}

process.exit(result.status === 'PASS' ? 0 : 1)
