#!/usr/bin/env node
import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'

await import('playwright')

const API_BASE = requiredEnv('CODEX_TEST_API_BASE').replace(/\/$/, '')
const RUNNER_TOKEN = requiredEnv('CODEX_TEST_RUNNER_TOKEN')
const MANAGEMENT_TENANT_ID = requiredEnv('CODEX_TEST_TENANT_ID')
const FRONTEND_BASE_URL = requiredEnv('CODEX_TEST_FRONTEND_BASE_URL')
const WORKING_DIRECTORY = process.env.CODEX_TEST_WORKDIR || process.cwd()
const RUNNER_NAME = process.env.CODEX_TEST_RUNNER_NAME || `${os.hostname()}-codex-runner`
const CODEX_COMMAND = process.env.CODEX_CLI_COMMAND || (process.platform === 'win32' ? 'codex.cmd' : 'codex')
const LOOP = process.argv.includes('--loop')
const POLL_INTERVAL_MS = Number(process.env.CODEX_TEST_POLL_INTERVAL_MS || '5000')

function requiredEnv(name) {
  const value = process.env[name]
  if (!value) {
    throw new Error(`${name} is required`)
  }
  return value
}

function runnerHeaders(extraHeaders = {}) {
  return {
    ...extraHeaders,
    'tenant-id': MANAGEMENT_TENANT_ID,
    'X-Codex-Runner-Token': RUNNER_TOKEN
  }
}

async function postJson(url, body) {
  const response = await fetch(`${API_BASE}${url}`, {
    method: 'POST',
    headers: runnerHeaders({
      'Content-Type': 'application/json'
    }),
    body: JSON.stringify(body)
  })
  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(`${url} failed: ${payload.msg || response.statusText}`)
  }
  return payload.data
}

async function uploadArtifact(executionCaseId, checkpointSort, screenshotPath) {
  const content = await fs.readFile(screenshotPath)
  const data = new FormData()
  data.append('executionCaseId', String(executionCaseId))
  data.append('checkpointSort', String(checkpointSort))
  data.append('artifactType', 'FAILURE_SCREENSHOT')
  data.append('file', new Blob([content]), path.basename(screenshotPath))
  const response = await fetch(`${API_BASE}/system/codex-test-runner/artifact`, {
    method: 'POST',
    headers: runnerHeaders(),
    body: data
  })
  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(`artifact upload failed: ${payload.msg || response.statusText}`)
  }
  return payload.data.artifactId
}

async function registerRunner() {
  return await postJson('/system/codex-test-runner/register', {
    runnerName: RUNNER_NAME,
    capabilities: JSON.stringify({ codex: true, playwright: true, frontendBaseUrl: FRONTEND_BASE_URL }),
    maxParallelism: Number(process.env.CODEX_TEST_MAX_PARALLELISM || '1'),
    playwrightVersion: 'installed',
    codexVersion: CODEX_COMMAND
  })
}

async function claimTasks(runnerSessionId) {
  return await postJson('/system/codex-test-runner/claim', {
    runnerSessionId,
    capacity: Number(process.env.CODEX_TEST_CLAIM_CAPACITY || '1')
  })
}

async function runCodexForTask(task) {
  const outputFile = path.join(os.tmpdir(), `codex-test-result-${task.executionCaseId}-${Date.now()}.json`)
  const prompt = buildPrompt(task)
  const args = [
    'exec',
    '-',
    '--skip-git-repo-check',
    '--dangerously-bypass-approvals-and-sandbox',
    '--ephemeral',
    '--output-last-message',
    outputFile,
    '-C',
    WORKING_DIRECTORY
  ]
  const child = spawn(CODEX_COMMAND, args, { stdio: ['pipe', 'pipe', 'pipe'] })
  child.stdin.write(prompt, 'utf8')
  child.stdin.end()
  const stdout = []
  const stderr = []
  child.stdout.on('data', (chunk) => stdout.push(chunk))
  child.stderr.on('data', (chunk) => stderr.push(chunk))
  const exitCode = await new Promise((resolve) => child.on('close', resolve))
  if (exitCode !== 0) {
    throw new Error(`codex exec failed with exit ${exitCode}: ${Buffer.concat(stderr).toString('utf8')}`)
  }
  const raw = await fs.readFile(outputFile, 'utf8')
  await fs.rm(outputFile, { force: true })
  const result = JSON.parse(raw)
  if (!Array.isArray(result.checkpointResults)) {
    throw new Error(`codex exec output missing checkpointResults: ${Buffer.concat(stdout).toString('utf8')}`)
  }
  return result
}

function buildPrompt(task) {
  return `You are executing an enterprise E2E test with Playwright.
Use the real browser against ${FRONTEND_BASE_URL}. Do not use API-only shortcuts except read-only final verification.
Target tenant id: ${task.targetTenantId}
Case: ${task.caseName}
Method:
${task.methodText}
User-written test data:
${task.testDataText || ''}
Checkpoints:
${task.checkpoints.map((item) => `${item.sort}. ${item.name}: ${item.expectedText}`).join('\n')}

Return raw JSON only:
{
  "checkpointResults": [
    {
      "checkpointSort": 1,
      "status": "PASS|FAIL|BLOCKED",
      "actualText": "real observed result",
      "mismatchDescription": "required when status is FAIL",
      "screenshotPath": "temporary screenshot file path when failed"
    }
  ],
  "summary": "short execution summary"
}`
}

async function reportTaskResult(task, result) {
  let hasFailure = false
  let hasBlocked = false
  for (const checkpoint of result.checkpointResults) {
    if (checkpoint.status === 'FAIL' && !checkpoint.mismatchDescription) {
      throw new Error(`checkpoint ${checkpoint.checkpointSort} failed without mismatchDescription`)
    }
    let screenshotArtifactId
    if (checkpoint.screenshotPath) {
      screenshotArtifactId = await uploadArtifact(
        task.executionCaseId,
        checkpoint.checkpointSort,
        checkpoint.screenshotPath
      )
    }
    await postJson('/system/codex-test-runner/checkpoint-result', {
      executionCaseId: task.executionCaseId,
      checkpointSort: checkpoint.checkpointSort,
      status: checkpoint.status,
      actualText: checkpoint.actualText,
      mismatchDescription: checkpoint.mismatchDescription,
      screenshotArtifactId
    })
    hasFailure = hasFailure || checkpoint.status === 'FAIL'
    hasBlocked = hasBlocked || checkpoint.status === 'BLOCKED'
  }
  await postJson('/system/codex-test-runner/complete-case', {
    executionCaseId: task.executionCaseId,
    status: hasFailure ? 'FAIL' : hasBlocked ? 'BLOCKED' : 'PASS',
    summary: result.summary
  })
}

async function runOnce(runnerSessionId) {
  const claim = await claimTasks(runnerSessionId)
  for (const task of claim.tasks) {
    const result = await runCodexForTask(task)
    await reportTaskResult(task, result)
  }
  return claim.tasks.length
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

const registration = await registerRunner()
do {
  const count = await runOnce(registration.runnerSessionId)
  if (!LOOP || count === 0) {
    if (!LOOP) {
      break
    }
    await sleep(POLL_INTERVAL_MS)
  }
} while (true)
