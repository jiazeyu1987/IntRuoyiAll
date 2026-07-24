import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const readOptionalText = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}

const feedbackApiSource = () => readText('src/api/mes/pro/feedback/index.ts')
const edhrArchiveApiSource = () => readOptionalText('src/api/mes/pro/edhr/archive.ts')
const archiveApiSource = () => `${feedbackApiSource()}\n${edhrArchiveApiSource()}`
const detailPageSource = () => readText('src/views/mes/pro/edhr/ExecutionPage.vue')
const realE2eSource = () => readText('tests/e2e/edhr-approval-tracking-real-flow.e2e.js')

const expectMatch = (source, matcher, message) => {
  assert.ok(matcher.test(source), message)
}

const expectNoMatch = (source, matcher, message) => {
  assert.ok(!matcher.test(source), message)
}

const archiveRelatedSource = (source) => {
  const lines = source.split(/\r?\n/)
  const selectedLineIndexes = new Set()
  lines.forEach((line, index) => {
    if (/archive|归档|sealPassword|generateEdhrExecutionArchive|downloadEdhrExecutionArchive|batch-record-execution-archive/i.test(line)) {
      const start = Math.max(0, index - 8)
      const end = Math.min(lines.length - 1, index + 12)
      for (let current = start; current <= end; current += 1) {
        selectedLineIndexes.add(current)
      }
    }
  })
  return [...selectedLineIndexes]
    .sort((left, right) => left - right)
    .map((index) => lines[index])
    .join('\n')
}

test('BDD: approved closed execution detail archive -> Given an approved closed eDHR execution, When the user has create permission, Then detail page exposes generate archive and collects seal password first', () => {
  const apiSource = archiveApiSource()
  const pageSource = detailPageSource()

  expectMatch(
    apiSource,
    /interface\s+ProFeedbackEdhrArchiveGenerateReqVO[\s\S]*executionId[\s\S]*artifactType[\s\S]*sealPassword[\s\S]*regenerate/s,
    'EDHR archive API source should declare controlled archive generation request fields: executionId, artifactType, sealPassword, regenerate'
  )
  expectMatch(
    apiSource,
    /generateEdhrExecutionArchive[\s\S]*\/mes\/pro\/batch-record-execution-archive\/generate/s,
    'EDHR archive API source should expose generateEdhrExecutionArchive on the controlled archive generate endpoint'
  )
  expectMatch(
    apiSource,
    /interface\s+ProFeedbackEdhrExecutionArchiveRespVO[\s\S]*approvalSnapshotId[\s\S]*approvalSnapshotHash/s,
    'EDHR archive response type must preserve approval snapshot id/hash for production evidence'
  )
  expectMatch(pageSource, /生成归档|归档生成/, 'detail page should expose a visible archive generation action')
  expectMatch(
    pageSource,
    /归档密码|封存密码|sealPassword|archiveForm\.password|archiveForm\.sealPassword/,
    'detail page should collect an electronic signature password before archive sealing'
  )
  expectMatch(
    pageSource,
    /mes:pro-batch-record-execution-archive:create|v-hasPermi=.*archive:create|hasPermi[^\\n]*archive:create/s,
    'detail page should guard the generate archive action with archive create permission'
  )
  expectMatch(
    pageSource,
    /status\s*!==\s*EDHR_EXECUTION_STATUS\.APPROVED|isApproved|EDHR_EXECUTION_STATUS\.APPROVED[\s\S]*closedAt[\s\S]*approvalSnapshotStatus/s,
    'detail page should only offer archive generation for APPROVED closed executions with approval snapshot evidence'
  )
})

test('BDD: archive approval evidence visible -> Given a sealed archive exists, When detail page renders latest archive state, Then approval snapshot evidence remains visible for reviewer capture', () => {
  const apiSource = archiveApiSource()
  const pageSource = detailPageSource()

  expectMatch(apiSource, /\bapprovalSnapshotId\??:\s*number\b/, 'archive API type must include approvalSnapshotId')
  expectMatch(apiSource, /\bapprovalSnapshotHash\??:\s*string\b/, 'archive API type must include approvalSnapshotHash')
  expectMatch(pageSource, /approvalSnapshotId|审批快照ID/, 'detail archive summary should expose approval snapshot id')
  expectMatch(pageSource, /approvalSnapshotHash|审批快照摘要/, 'detail archive summary should expose approval snapshot hash')
})

test('BDD: archive sha256 evidence -> Given a sealed archive exists, When the execution form renders latest archive state, Then the archive SHA-256 remains visible for reviewer and download verification', () => {
  const pageSource = detailPageSource()

  expectMatch(
    pageSource,
    /SHA-256|latestArchive\?\.sha256/,
    'execution form archive area should render visible SHA-256 evidence from latestArchive.sha256'
  )
})

test('BDD: archive state visibility -> Given draft/submitted/generating/sealed/failed executions, When execution form renders, Then allowed archive actions and status are permission-aware', () => {
  const apiSource = archiveApiSource()
  const pageSource = detailPageSource()

  expectMatch(
    apiSource,
    /getLatestEdhrExecutionArchive[\s\S]*\/mes\/pro\/batch-record-execution-archive\/latest/s,
    'EDHR archive API source should expose latest archive query for execution form archive state'
  )
  expectMatch(
    apiSource,
    /getEdhrExecutionArchivePage[\s\S]*\/mes\/pro\/batch-record-execution-archive\/page/s,
    'EDHR archive API source should expose archive page query for version/state viewing'
  )
  expectMatch(pageSource, /归档状态|archiveStatus/, 'execution form should show latest archive status')
  for (const state of ['GENERATING', 'SEALED', 'FAILED']) {
    expectMatch(
      pageSource,
      new RegExp(`\\b${state}\\b`),
      `frontend should render the ${state} archive state`
    )
  }
  expectMatch(
    pageSource,
    /mes:pro-batch-record-execution-archive:query|v-hasPermi=.*archive:query|hasPermi[^\\n]*archive:query/s,
    'archive status/version visibility should respect archive query permission'
  )
})

test('BDD: controlled archive download -> Given a sealed archive exists, When the user downloads it, Then frontend uses the EDHR backend download endpoint instead of raw file/Jimu links', () => {
  const apiSource = archiveApiSource()
  const pageSource = detailPageSource()
    expectMatch(
    apiSource,
    /downloadEdhrExecutionArchive[\s\S]*request\.download[\s\S]*\/mes\/pro\/batch-record-execution-archive\/download/s,
    'EDHR archive API source should download sealed archives through request.download and the controlled EDHR archive endpoint'
  )
  expectMatch(pageSource, /下载归档|归档下载/, 'execution form should expose sealed archive download')
  expectMatch(
    pageSource,
    /mes:pro-batch-record-execution-archive:download|v-hasPermi=.*archive:download|hasPermi[^\\n]*archive:download/s,
    'archive download action should respect archive download permission'
  )
  expectNoMatch(
    `${apiSource}\n${pageSource}`,
    /window\.open\([^)]*(jmreport|fileId|fileUrl|url)[^)]*\)/i,
    'archive download must not use unauthenticated raw file urls or Jimu links in a new tab'
  )
})

test('BDD: archive failure no fallback -> Given archive generation or download fails, When backend returns an error, Then frontend displays the failure and never navigates to Jimu preview/designer as final archive', () => {
  const apiSource = archiveApiSource()
  const pageSource = detailPageSource()
  const archiveSource = `${apiSource}\n${pageSource}`
  const archiveActionSource = archiveRelatedSource(archiveSource)

  expectMatch(
    archiveSource,
    /failureReason|archiveError|归档失败|生成失败|下载失败/,
    'frontend should display backend archive generation/download failure messages'
  )
  expectNoMatch(
    archiveActionSource,
    /generateEdhrExecutionArchive[\s\S]*(jmreport\/view|jmreport\/index|designer-path|edit-path)/,
    'archive generation must not fall back to Jimu preview or designer paths'
  )
  expectNoMatch(
    archiveActionSource,
    /downloadEdhrExecutionArchive[\s\S]*(jmreport\/view|jmreport\/index|designer-path|edit-path)/,
    'archive download must not fall back to Jimu preview or designer paths'
  )
  expectNoMatch(
    archiveActionSource,
    /html2canvas|window\.print|print\(|screenshot/i,
    'archive-related generate/download code must not treat screenshots or browser print as the final archive'
  )
})

test('BDD: real E2E archive evidence -> Given a real UI flow generates a sealed archive, When E2E records evidence, Then it validates approval snapshot hash and downloads through the controlled endpoint', () => {
  const e2eSource = realE2eSource()

  expectMatch(e2eSource, /approvalSnapshotId/, 'real E2E should assert archive approvalSnapshotId from generate response')
  expectMatch(e2eSource, /approvalSnapshotHash/, 'real E2E should assert archive approvalSnapshotHash from generate response')
  expectMatch(e2eSource, /signatureHash/, 'real E2E should assert archive signatureHash from generate response')
  expectMatch(e2eSource, /sha256/, 'real E2E should assert archive sha256 from generate response')
  expectMatch(
    e2eSource,
    /waitForEvent\(\s*['"]download['"][\s,)]|page\.on\(\s*['"]download['"]/,
    'real E2E should wait for the controlled archive download artifact'
  )
  expectMatch(
    e2eSource,
    /\/mes\/pro\/batch-record-execution-archive\/download/,
    'real E2E should observe the controlled archive download endpoint'
  )
  expectMatch(
    e2eSource,
    /createHash\(\s*['"]sha256['"]\s*\)|downloadedSha256/,
    'real E2E should recompute the downloaded archive file SHA-256'
  )
  expectMatch(
    e2eSource,
    /downloadedSha256[\s\S]*archiveEvidence\.sha256|archiveEvidence\.sha256[\s\S]*downloadedSha256/,
    'real E2E should compare downloaded archive SHA-256 with the archive response sha256'
  )
})

test('BDD: real E2E action responses fail closed -> Given submit/approve/reject requests return HTTP 200 with a CommonResult body, When E2E continues the workflow, Then it must assert the response code and data instead of trusting HTTP status alone', () => {
  const e2eSource = realE2eSource()

  expectMatch(
    e2eSource,
    /function\s+readApiBoolean\s*\(/,
    'real E2E should have a boolean CommonResult assertion helper for submit actions'
  )
  expectMatch(
    e2eSource,
    /提交接口[\s\S]{0,900}readApiBoolean|readApiBoolean[\s\S]{0,900}提交接口/,
    'submit flow should assert CommonResult code/data before waiting for SUBMITTED UI state'
  )
  expectMatch(
    e2eSource,
    /审批通过接口[\s\S]{0,900}readApiData|readApiData[\s\S]{0,900}审批通过接口/,
    'approve flow should assert CommonResult code/data before waiting for CLOSED UI state'
  )
  expectMatch(
    e2eSource,
    /审批驳回接口[\s\S]{0,900}readApiData|readApiData[\s\S]{0,900}审批驳回接口/,
    'reject flow should assert CommonResult code/data before waiting for REJECTED UI state'
  )
})
