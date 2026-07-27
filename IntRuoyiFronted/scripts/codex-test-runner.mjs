#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process'
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
const HEARTBEAT_INTERVAL_MS = Number(process.env.CODEX_TEST_HEARTBEAT_INTERVAL_MS || '20000')
const CODEX_EXEC_TIMEOUT_MS = Number(process.env.CODEX_TEST_CODEX_TIMEOUT_MS || '600000')
const CODEX_TEST_API_TIMEOUT_MS = Number(process.env.CODEX_TEST_API_TIMEOUT_MS || '30000')
const COMPLETE_CASE_SUMMARY_MAX_LENGTH = 512

class ServerCanceledExecutionError extends Error {}

function requiredEnv(name) {
  const value = process.env[name]
  if (!value) {
    throw new Error(`${name} is required`)
  }
  return value
}

function normalizeCompleteCaseSummary(summary) {
  return String(summary || '').slice(0, COMPLETE_CASE_SUMMARY_MAX_LENGTH)
}

function runnerHeaders(extraHeaders = {}) {
  return {
    ...extraHeaders,
    'tenant-id': MANAGEMENT_TENANT_ID,
    'X-Codex-Runner-Token': RUNNER_TOKEN
  }
}

async function postJson(url, body) {
  const response = await requestWithTimeout(url, {
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

async function requestWithTimeout(url, options) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), CODEX_TEST_API_TIMEOUT_MS)
  try {
    return await fetch(`${API_BASE}${url}`, {
      ...options,
      signal: controller.signal
    })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new Error(`${url} timed out after ${CODEX_TEST_API_TIMEOUT_MS}ms`)
    }
    throw error
  } finally {
    clearTimeout(timeout)
  }
}

function spawnCodex(args) {
  const isWindowsCommandScript = process.platform === 'win32' && /\.(cmd|bat)$/i.test(CODEX_COMMAND)
  const command = isWindowsCommandScript ? 'cmd.exe' : CODEX_COMMAND
  const commandArgs = isWindowsCommandScript ? ['/d', '/s', '/c', CODEX_COMMAND, ...args] : args
  return spawn(command, commandArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
}

function toPowerShellSingleQuoted(value) {
  return String(value).replace(/'/g, "''")
}

function stopWindowsProcessTree(childPid, outputFile) {
  if (childPid) {
    spawnSync('taskkill.exe', ['/pid', String(childPid), '/t', '/f'], { stdio: 'ignore', timeout: 10000 })
  }
  const escapedOutputFile = toPowerShellSingleQuoted(outputFile)
  const stopByOutputFileCommand = [
    `$needle = '${escapedOutputFile}'`,
    'Get-CimInstance Win32_Process |',
    'Where-Object { $_.ProcessId -ne $PID -and $_.CommandLine -and $_.CommandLine.Contains($needle) } |',
    'ForEach-Object { Stop-Process -Id $_.ProcessId -Force }'
  ].join(' ')
  spawnSync('powershell.exe', ['-NoProfile', '-Command', stopByOutputFileCommand], {
    stdio: 'ignore',
    timeout: 10000
  })
}

async function uploadArtifact(executionCaseId, checkpointSort, screenshotPath) {
  const content = await fs.readFile(screenshotPath)
  const data = new FormData()
  data.append('executionCaseId', String(executionCaseId))
  data.append('checkpointSort', String(checkpointSort))
  data.append('artifactType', 'FAILURE_SCREENSHOT')
  data.append('file', new Blob([content]), path.basename(screenshotPath))
  const response = await requestWithTimeout('/system/codex-test-runner/artifact', {
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

async function heartbeat(runnerSessionId, runningExecutionCaseIds = []) {
  return await postJson('/system/codex-test-runner/heartbeat', {
    runnerSessionId,
    runningExecutionCaseIds
  })
}

async function reportProgress(task, progress) {
  await postJson('/system/codex-test-runner/progress', {
    executionCaseId: task.executionCaseId,
    phase: progress.phase,
    currentMethodSort: progress.currentMethodSort,
    currentCheckpointSort: progress.currentCheckpointSort,
    progressMessage: progress.progressMessage
  })
}

function assertTaskNotCanceled(task, heartbeatResult) {
  const cancelExecutionCaseIds = heartbeatResult?.cancelExecutionCaseIds || []
  if (cancelExecutionCaseIds.includes(task.executionCaseId)) {
    throw new ServerCanceledExecutionError(`execution case ${task.executionCaseId} was canceled by server`)
  }
}

async function runCodexForTask(task, runnerSessionId) {
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
  const child = spawnCodex(args)
  const stdout = []
  const stderr = []
  let heartbeatError
  let timeoutError
  let heartbeatTimer
  let timeoutTimer
  const runningExecutionCaseIds = [task.executionCaseId]
  const stopChild = () => {
    if (process.platform === 'win32') {
      stopWindowsProcessTree(child.pid, outputFile)
      return
    }
    if (!child.killed) {
      child.kill()
    }
  }
  child.stdout.on('data', (chunk) => stdout.push(chunk))
  child.stderr.on('data', (chunk) => stderr.push(chunk))
  try {
    await reportProgress(task, {
      phase: 'METHOD',
      currentMethodSort: 1,
      progressMessage: '正在执行测试方法项第 1 项'
    })
    child.stdin.write(prompt, 'utf8')
    child.stdin.end()
    assertTaskNotCanceled(task, await heartbeat(runnerSessionId, runningExecutionCaseIds))
    heartbeatTimer = setInterval(() => {
      heartbeat(runnerSessionId, runningExecutionCaseIds)
        .then((heartbeatResult) => assertTaskNotCanceled(task, heartbeatResult))
        .catch((error) => {
          heartbeatError = error
          stopChild()
        })
    }, HEARTBEAT_INTERVAL_MS)
    timeoutTimer = setTimeout(() => {
      timeoutError = new Error(`codex exec timed out after ${CODEX_EXEC_TIMEOUT_MS}ms`)
      stopChild()
    }, CODEX_EXEC_TIMEOUT_MS)
    const exitCode = await new Promise((resolve, reject) => {
      child.once('error', reject)
      child.once('close', resolve)
    })
    if (heartbeatError) {
      throw heartbeatError
    }
    if (timeoutError) {
      throw timeoutError
    }
    const stderrText = Buffer.concat(stderr).toString('utf8')
    if (exitCode !== 0) {
      throw new Error(`codex exec failed with exit ${exitCode}: ${stderrText}`)
    }
  } catch (error) {
    stopChild()
    throw error
  } finally {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
    }
    if (timeoutTimer) {
      clearTimeout(timeoutTimer)
    }
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
    await reportProgress(task, {
      phase: 'CHECKPOINT',
      currentCheckpointSort: checkpoint.checkpointSort,
      progressMessage: `正在验证目标项第 ${checkpoint.checkpointSort} 项`
    })
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
    summary: normalizeCompleteCaseSummary(result.summary)
  })
}

async function reportTaskBlocked(task, error) {
  const summary = `Codex Runner 执行失败：${error instanceof Error ? error.message : String(error)}`
  for (const checkpoint of task.checkpoints) {
    await reportProgress(task, {
      phase: 'CHECKPOINT',
      currentCheckpointSort: checkpoint.sort,
      progressMessage: `正在验证目标项第 ${checkpoint.sort} 项`
    })
    await postJson('/system/codex-test-runner/checkpoint-result', {
      executionCaseId: task.executionCaseId,
      checkpointSort: checkpoint.sort,
      status: 'BLOCKED',
      actualText: summary.slice(0, 1000),
      mismatchDescription: undefined,
      screenshotArtifactId: undefined
    })
  }
  await postJson('/system/codex-test-runner/complete-case', {
    executionCaseId: task.executionCaseId,
    status: 'BLOCKED',
    summary: normalizeCompleteCaseSummary(summary)
  })
}

async function runOnce(runnerSessionId) {
  await heartbeat(runnerSessionId)
  const claim = await claimTasks(runnerSessionId)
  for (const task of claim.tasks) {
    try {
      const result = await runCodexForTask(task, runnerSessionId)
      await reportTaskResult(task, result)
    } catch (error) {
      if (error instanceof ServerCanceledExecutionError) {
        continue
      }
      await reportTaskBlocked(task, error)
    }
  }
  return claim.tasks.length
}

function logLoopError(error) {
  const message = error instanceof Error ? error.message : String(error)
  console.error(`[codex-test-runner] ${message}`)
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function registerRunnerWithRetry() {
  while (true) {
    try {
      return await registerRunner()
    } catch (error) {
      if (!LOOP) {
        throw error
      }
      logLoopError(error)
      await sleep(POLL_INTERVAL_MS)
    }
  }
}

let registration = await registerRunnerWithRetry()
do {
  try {
    const count = await runOnce(registration.runnerSessionId)
    if (!LOOP || count === 0) {
      if (!LOOP) {
        break
      }
      await sleep(POLL_INTERVAL_MS)
    }
  } catch (error) {
    if (!LOOP) {
      throw error
    }
    logLoopError(error)
    await sleep(POLL_INTERVAL_MS)
    registration = await registerRunnerWithRetry()
  }
} while (true)
