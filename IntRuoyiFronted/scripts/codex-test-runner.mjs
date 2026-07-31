#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'

await import('playwright')

const API_BASE = requiredEnv('CODEX_TEST_API_BASE').replace(/\/$/, '')
const RUNNER_TOKEN = process.env.CODEX_TEST_RUNNER_TOKEN || ''
const MANAGEMENT_TENANT_ID = requiredEnv('CODEX_TEST_TENANT_ID')
const FRONTEND_BASE_URL = requiredEnv('CODEX_TEST_FRONTEND_BASE_URL')
const WORKING_DIRECTORY = process.env.CODEX_TEST_WORKDIR || process.cwd()
const PROJECT_ROOT = process.env.CODEX_TEST_PROJECT_ROOT || WORKING_DIRECTORY
const FRONTEND_PROJECT_ROOT = process.env.CODEX_TEST_FRONTEND_ROOT || process.cwd()
const RUNNER_NAME = process.env.CODEX_TEST_RUNNER_NAME || `${os.hostname()}-codex-runner`
const CODEX_COMMAND = process.env.CODEX_CLI_COMMAND || (process.platform === 'win32' ? 'codex.cmd' : 'codex')
const LOOP = process.argv.includes('--loop')
const POLL_INTERVAL_MS = Number(process.env.CODEX_TEST_POLL_INTERVAL_MS || '5000')
const HEARTBEAT_INTERVAL_MS = Number(process.env.CODEX_TEST_HEARTBEAT_INTERVAL_MS || '20000')
const CODEX_EXEC_TIMEOUT_MS = Number(process.env.CODEX_TEST_CODEX_TIMEOUT_MS || '600000')
const CODEX_EXEC_READONLY_TIMEOUT_MS = Number(process.env.CODEX_TEST_CODEX_READONLY_TIMEOUT_MS || '120000')
const CODEX_READONLY_REASONING_EFFORT = process.env.CODEX_TEST_CODEX_READONLY_REASONING_EFFORT || 'medium'
const CODEX_MUTATING_REASONING_EFFORT = process.env.CODEX_TEST_CODEX_MUTATING_REASONING_EFFORT || 'low'
const CODEX_IGNORE_RULES = process.env.CODEX_TEST_CODEX_IGNORE_RULES !== 'false'
const CODEX_TEST_API_TIMEOUT_MS = Number(process.env.CODEX_TEST_API_TIMEOUT_MS || '30000')
const CODEX_CHILD_SETTLE_TIMEOUT_MS = Number(process.env.CODEX_TEST_CHILD_SETTLE_TIMEOUT_MS || '5000')
const COMPLETE_CASE_SUMMARY_MAX_LENGTH = 512
const CODEX_FAILURE_DETAIL_MAX_LENGTH = 2400
const RUNNER_HTTP_CONNECTION_HEADERS = { Connection: 'close' }
const READONLY_TASK_PATTERN = /(只读|仅查看|只查看|查看|确认.{0,20}可见|不修改|不保存|不提交|read[- ]?only|view only)/i
const NEGATED_WRITE_TASK_PATTERN = /(不修改|不新增|不创建|不编辑|不保存|不提交|不删除|不作废|不审批|不发布|不导入|不上传|不下载|不取消|不启用|不禁用|不清理|不复位|不生成|不填写|不签名|不写入)/gi
const WRITE_TASK_PATTERN = /(新增|创建|修改|编辑|保存|提交|删除|作废|审批|发布|导入|上传|下载|取消|启用|禁用|清理|复位|生成|填写|签名|写入|create|update|edit|save|submit|delete|void|approve|publish|import|upload|cancel|enable|disable|write)/i

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
  const headers = {
    ...RUNNER_HTTP_CONNECTION_HEADERS,
    'tenant-id': MANAGEMENT_TENANT_ID
  }
  if (RUNNER_TOKEN) {
    headers['X-Codex-Runner-Token'] = RUNNER_TOKEN
  }
  return {
    ...headers,
    ...extraHeaders
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
      signal: controller.signal,
      cache: 'no-store'
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
  return spawn(command, commandArgs, {
    stdio: ['pipe', 'pipe', 'pipe'],
    env: {
      ...process.env,
      NODE_PATH: resolveFrontendNodePath(),
      PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH: resolveBrowserExecutablePath()
    }
  })
}

function resolveFrontendNodePath() {
  const frontendNodeModules = path.join(FRONTEND_PROJECT_ROOT, 'node_modules')
  const currentNodePath = process.env.NODE_PATH || ''
  return [frontendNodeModules, currentNodePath].filter(Boolean).join(path.delimiter)
}

function resolveBrowserExecutablePath() {
  const configuredPath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  if (configuredPath) {
    if (!existsSync(configuredPath)) {
      throw new Error(`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH does not exist: ${configuredPath}`)
    }
    return configuredPath
  }
  const browserCandidates = [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  ]
  const browserExecutablePath = browserCandidates.find((candidate) => existsSync(candidate))
  if (!browserExecutablePath) {
    throw new Error('No local Chrome or Edge executable found for Playwright browser launch')
  }
  return browserExecutablePath
}

function resolveNavigationHints(task) {
  const text = taskText(task)
  const hints = [
    'This frontend uses Vue history routes, not hash routes; do not navigate with /#/ paths.'
  ]
  if (/工艺路线|route/i.test(text)) {
    hints.push('工艺路线 list page: /mes/pro/route (Vue history route; do not use /#/mes/route).')
  }
  if (/批记录|eDHR|edhr|batch record/i.test(text)) {
    hints.push('批记录 execution list page: /mes/pro/feedback/edhr-batch-execution (Vue history route).')
    hints.push('批记录表单配置 list page: /mes/pro/batch-record-form-list (Vue history route).')
  }
  if (/智能排产|排产|排程|schedule/i.test(text)) {
    hints.push('智能排产 task list page: /mes/pro/task (Vue history route).')
    hints.push('排程日历 page: /mes/pro/schedule-calendar (Vue history route).')
  }
  return hints.join('\n')
}

function codexExecutionArgs(task) {
  const args = []
  if (CODEX_IGNORE_RULES) {
    args.push('--ignore-rules')
  }
  args.push('--disable', 'remote_plugin')
  const reasoningEffort = isReadOnlyTask(task)
    ? CODEX_READONLY_REASONING_EFFORT
    : CODEX_MUTATING_REASONING_EFFORT
  if (reasoningEffort) {
    args.push('-c', `model_reasoning_effort=${JSON.stringify(reasoningEffort)}`)
  }
  return args
}

function redactSensitiveText(value) {
  return String(value || '')
    .replace(/Bearer\s+[A-Za-z0-9._~+/=-]+/gi, 'Bearer <redacted>')
    .replace(/(Authorization\s*[:=]\s*)[^\r\n]+/gi, '$1<redacted>')
    .replace(/(Cookie\s*[:=]\s*)[^\r\n]+/gi, '$1<redacted>')
    .replace(/sk-[A-Za-z0-9_-]+/g, '<redacted-api-key>')
}

function summarizeCodexFailure(stderrText) {
  const sanitized = redactSensitiveText(stderrText)
    .split(/\r?\n/)
    .filter((line) => !/remote installed plugin bundle sync failed/.test(line))
    .filter((line) => !/unknown feature key in config/.test(line))
    .join('\n')
    .trim()
  return (sanitized || redactSensitiveText(stderrText)).slice(-CODEX_FAILURE_DETAIL_MAX_LENGTH)
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

function releaseChildIo(child) {
  for (const stream of [child.stdin, child.stdout, child.stderr]) {
    if (stream && !stream.destroyed) {
      stream.destroy()
    }
  }
  child.unref()
}

async function stopChildAndWait(child, outputFile, childExitPromise) {
  if (process.platform === 'win32') {
    stopWindowsProcessTree(child.pid, outputFile)
  } else if (!child.killed) {
    child.kill()
  }
  const stopResult = await Promise.race([
    childExitPromise,
    sleep(CODEX_CHILD_SETTLE_TIMEOUT_MS).then(() => ({ closeTimedOut: true }))
  ])
  if (stopResult.closeTimedOut) {
    releaseChildIo(child)
  }
  return stopResult
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
  const codexExecTimeoutMs = resolveCodexExecTimeoutMs(task)
  const prompt = buildPrompt(task, codexExecTimeoutMs)
  const args = [
    'exec',
    '-',
    '--skip-git-repo-check',
    '--dangerously-bypass-approvals-and-sandbox',
    '--ephemeral',
    ...codexExecutionArgs(task),
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
  const childExitPromise = new Promise((resolve) => {
    child.once('error', (error) => resolve({ error }))
    child.once('close', (exitCode) => resolve({ exitCode }))
  })
  let resolveStopRequested
  const stopRequestedPromise = new Promise((resolve) => {
    resolveStopRequested = resolve
  })
  let stopPromise
  const stopChild = () => {
    if (!stopPromise) {
      stopPromise = stopChildAndWait(child, outputFile, childExitPromise)
      resolveStopRequested(stopPromise)
    }
    return stopPromise
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
      timeoutError = new Error(`codex exec timed out after ${codexExecTimeoutMs}ms`)
      stopChild()
    }, codexExecTimeoutMs)
    const childResult = await Promise.race([
      childExitPromise,
      stopRequestedPromise
    ])
    if (heartbeatError) {
      throw heartbeatError
    }
    if (timeoutError) {
      throw timeoutError
    }
    if (childResult.error) {
      throw childResult.error
    }
    if (childResult.closeTimedOut) {
      throw new Error(`codex exec process did not settle after ${CODEX_CHILD_SETTLE_TIMEOUT_MS}ms`)
    }
    const stderrText = Buffer.concat(stderr).toString('utf8')
    if (childResult.exitCode !== 0) {
      throw new Error(`codex exec failed with exit ${childResult.exitCode}: ${summarizeCodexFailure(stderrText)}`)
    }
  } catch (error) {
    const stopResult = await stopChild()
    if (stopResult.closeTimedOut) {
      logLoopError(
        new Error(`codex exec child did not emit close after ${CODEX_CHILD_SETTLE_TIMEOUT_MS}ms`)
      )
    }
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

function buildPrompt(task, codexExecTimeoutMs = resolveCodexExecTimeoutMs(task)) {
  const taskMode = isReadOnlyTask(task) ? 'READ_ONLY' : 'MUTATING_OR_UNKNOWN'
  const executionBudgetSeconds = Math.max(30, Math.floor(codexExecTimeoutMs / 1000) - 10)
  return `You are executing an enterprise E2E test with Playwright.
Use the real browser against ${FRONTEND_BASE_URL}. Do not use API-only shortcuts except read-only final verification.
This task is classified as ${taskMode}.
This is a browser execution task, not a repository development task.
Do not create or modify repository files, task documents, source code, configuration, build outputs, Git state, commits, branches, or worktrees.
Do not run project builds or project test suites.
Use only task-owned temporary files under ${WORKING_DIRECTORY}.
Execution strategy: create one temporary Node.js Playwright script under ${WORKING_DIRECTORY}, run it with node, then return the final JSON.
Do not inspect the repository before the first browser attempt; inspect local source only if the browser path is blocked by selectors, routes, or prerequisite evidence.
When the temporary script prints raw JSON with checkpointResults, return that JSON immediately.
For Element Plus dialogs, click visible buttons by accessible role or exact visible text.
Element Plus dialog/drawer footer action buttons such as save or confirm may be outside the field form scope; after filling fields, search the entire visible dialog/drawer or page for 保存/确定/提交, not only the field form scope.
Use this deterministic Element Plus footer selector after filling a dialog or drawer: page.locator('.el-dialog__footer button, .el-drawer__footer button').filter({ hasText: /保存|确定|提交/ }).last().click().
Some pages render save actions in custom footer rows instead of .el-dialog__footer or .el-drawer__footer; if the scoped footer selector has no visible candidate, locate the visible action across the current dialog/drawer or page with page.locator('button, .el-button').filter({ hasText: /^保存$|^确定$|^提交$/ }).last().click() before declaring the save button missing.
For field-selector list filters, first read the visible selected field label and use the matching identifier input instead of assuming a name field exists. On the 工艺路线 list, the default selected field is 路线编码; for the fixed basic-maintenance sample, search with route code TN-ROUTE-BASIC-001 before trying route name.
List query buttons may be labeled 查询 or 搜索; use page.getByRole('button', { name: /查询|搜索/ }) or page.locator('button, .el-button').filter({ hasText: /查询|搜索/ }) instead of searching only for 搜索.
Playwright project root: ${FRONTEND_PROJECT_ROOT}
Project guidance root: ${PROJECT_ROOT}
Playwright dependency note: temporary Node scripts can use require('playwright') because NODE_PATH includes ${FRONTEND_PROJECT_ROOT}/node_modules.
Browser executable path: ${resolveBrowserExecutablePath()}
Browser launch note: temporary Playwright scripts must launch with chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '${resolveBrowserExecutablePath()}' }).
Navigation hints:
${resolveNavigationHints(task)}
Complete the browser verification and return the final JSON within ${executionBudgetSeconds} seconds.
Do not ask for clarification. If login, selector, data, service, permission, or runtime prerequisites are missing, return a BLOCKED checkpoint result instead of waiting.
For READ_ONLY tasks, do not click create, save, submit, delete, import, upload, approve, cancel, or any action that mutates business data.
For READ_ONLY tasks, take the shortest browser path. Prefer one temporary Node.js Playwright script and finish once the listed checkpoints are observed.
Prefer the existing local Playwright/browser tooling from the frontend project, and read only the minimum local login/access guidance needed. Do not print passwords, tokens, Authorization headers, or cookies.
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

function taskText(task) {
  return [
    task.caseName,
    task.methodText,
    task.testDataText,
    ...(task.checkpoints || []).flatMap((item) => [item.name, item.expectedText])
  ]
    .filter(Boolean)
    .join('\n')
}

function isReadOnlyTask(task) {
  const text = taskText(task)
  const hasReadOnlyIntent = READONLY_TASK_PATTERN.test(text)
  const textWithoutNegatedWriteIntent = text.replace(NEGATED_WRITE_TASK_PATTERN, '')
  const hasWriteIntent = WRITE_TASK_PATTERN.test(textWithoutNegatedWriteIntent)
  return hasReadOnlyIntent && !hasWriteIntent
}

function resolveCodexExecTimeoutMs(task) {
  return isReadOnlyTask(task) ? CODEX_EXEC_READONLY_TIMEOUT_MS : CODEX_EXEC_TIMEOUT_MS
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
