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
const CODEX_EXEC_TIMEOUT_MS = Number(process.env.CODEX_TEST_CODEX_TIMEOUT_MS || '360000')
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
Before running the temporary Node.js Playwright script, run node --check <temporary-script-path>. This syntax check does not count as running the browser script. If node --check fails, fix the generated script before browser launch instead of running invalid JavaScript.
Generated scripts must avoid redeclaring const or let identifiers in the same function or block. Do not reuse names such as modal, dialog, rows, values, result, or button for a second const/let declaration in the same scope; either assign to an existing let variable or use a unique name such as detailDialog, retryDialog, copiedRows, or tabVisibility. If a syntax error says Identifier '<name>' has already been declared, repair the duplicate declaration before executing the script.
Generated scripts must not reference block-scoped variables outside the try/catch/block where they are declared. If a value such as cleanupOutcome, resetOutcome, detailOutcome, or routeValues is needed after a try block, declare let cleanupOutcome = null before the try and assign it inside, or build the final summary from checkpointResults instead. Never write const cleanupOutcome inside try { ... } and then read cleanupOutcome after the try/catch.
Generated Playwright scripts must treat page.url() as a synchronous string-returning method. Never write await page.url(), page.url().catch(...), or await pageHandle.url().catch(...). Use a synchronous helper such as try { return page.url(); } catch { return 'url-unavailable'; } when collecting diagnostic URLs.
Do not inspect the repository before the first browser attempt; inspect local source only if the browser path is blocked by selectors, routes, or prerequisite evidence.
When the temporary script prints raw JSON with checkpointResults, return that JSON immediately.
Run the temporary Playwright script at most once before returning. If stdout contains raw JSON with checkpointResults, return that JSON verbatim immediately. Do not keep debugging, rerunning, or launching extra browsers after JSON is available.
If the temporary script exits with a JSON result that contains FAIL or BLOCKED checkpoints, return that JSON as the business evidence instead of spending the remaining budget trying to repair the generated script.
The final assistant response must be exactly the JSON object printed by the temporary Playwright script. Do not add analysis, screenshots, markdown fences, or follow-up debugging after the JSON. If the temporary script reports BLOCKED or FAIL, return that same JSON immediately as the final answer.
The temporary Playwright script must enforce its own overall deadline that is shorter than the remaining execution budget. Hard cap the temporary browser script deadline at 240000ms. Do not compute the temporary script deadline from the full Codex exec timeout, because script generation can already consume several minutes before node starts. Use const scriptDeadlineMs = Math.min(240000, Math.max(30000, Number(process.env.CODEX_TEST_BROWSER_FLOW_TIMEOUT_MS || 240000))) and const deadlineAt = Date.now() + scriptDeadlineMs. Never generate deadlines such as 300000, 540000, or 560000ms for the temporary browser script. It must race the main browser flow against that deadline and always print checkpointResults JSON before the Codex child timeout. If the deadline is reached, close the browser and return BLOCKED checkpoints for unfinished items instead of letting codex exec hit the outer child timeout. The deadline handler should include the current URL, current visible page text, and the last known phase/checkpoint in actualText, but it must not keep debugging or rerun the browser script.
For Element Plus dialogs, click visible buttons by accessible role or exact visible text.
Element Plus dialog/drawer footer action buttons such as save or confirm may be outside the field form scope; after filling fields, search the entire visible dialog/drawer or page for 保存/确定/提交, not only the field form scope.
Element Plus button innerText may contain whitespace between Chinese characters, such as 保 存; use the whitespace-tolerant action regex /保\\s*存|确\\s*定|提\\s*交/ instead of exact /^保存$/ text.
Business confirmation dialogs may use verb-specific primary buttons instead of generic save labels, for example 确认复制, 复制, 确认发布, 发布, 启用, 停用, 删除, 确认删除. Use a business action regex such as /保\\s*存|确\\s*定|提\\s*交|确\\s*认\\s*复\\s*制|复\\s*制|确\\s*认\\s*发\\s*布|发\\s*布|启\\s*用|停\\s*用|确\\s*认\\s*删\\s*除|删\\s*除/ and search the current visible dialog or drawer before any background row buttons.
If a visible copy/edit dialog footer primary button is 确认复制, use the business action regex, not only /保存|确定|提交/. For copy dialogs, use page.locator('.el-dialog__footer button, .el-drawer__footer button').filter({ hasText: /保\\s*存|确\\s*定|提\\s*交|确\\s*认\\s*复\\s*制|复\\s*制/ }) before any page-wide fallback. Never declare "No visible enabled dialog save action" while a visible enabled 确认复制 button exists in the current dialog footer.
When an Element Plus message box is visible, click the primary action only inside .el-message-box:visible or .el-overlay-message-box:visible. Do not include background page buttons in the same locator while a message box is visible. Use .el-message-box__btns button or .el-overlay-message-box button filtered by 确定/确认/删除, and prefer the visible primary button in that message box. If a click is intercepted by .el-overlay-message-box, re-scope to the visible message box and retry once.
After clicking a visible Element Plus message-box primary action, wait for the same visible .el-message-box/.el-overlay-message-box to become hidden before reading background page state. If the message box remains visible, return BLOCKED with the message-box title/text/buttons instead of polling stale workspace or table text.
Use this deterministic Element Plus footer selector after filling a dialog or drawer: page.locator('.el-dialog__footer button, .el-drawer__footer button').filter({ hasText: /保\\s*存|确\\s*定|提\\s*交/ }).
Some pages render save actions in custom footer rows instead of .el-dialog__footer or .el-drawer__footer; if the scoped footer selector has no visible candidate, locate the visible action across the current dialog/drawer or page with page.locator('button, .el-button').filter({ hasText: /保\\s*存|确\\s*定|提\\s*交/ }) before declaring the save button missing.
Do not use locator.last() for save/confirm buttons because hidden duplicate buttons can appear after the visible action in DOM order. Iterate candidates and click the first visible enabled one, for example: const actionCandidates = page.locator('.el-dialog:visible button, .el-drawer:visible button, .el-overlay:visible button, button, .el-button').filter({ hasText: /保\\s*存|确\\s*定|提\\s*交/ }); const actionCount = await actionCandidates.count(); for (let i = 0; i < actionCount; i += 1) { const action = actionCandidates.nth(i); if (await action.isVisible().catch(() => false) && await action.isEnabled().catch(() => false)) { await action.scrollIntoViewIfNeeded().catch(() => {}); await action.click(); break; } }.
Form inputs inside add/edit dialogs must be scoped to the currently visible .el-dialog or .el-drawer. Do not fill background list filter inputs after opening a dialog or drawer, even if the background field has a matching placeholder such as 路线编码. After filling required dialog fields, verify the dialog-scoped inputValue before clicking save; if the dialog-scoped 编码 input does not equal the fixed route code, keep filling the visible dialog input instead of clicking save.
When filling Element Plus dialog fields, locate only .el-form-item containers for the exact label; do not search broad div, section, row, or column containers by hasText because a whole 基础信息 section can contain both 编码 and 名称. For 名称, the container text must include 名称 and must not also include 编码 or 基础信息 before filling the contained input. Prefer .el-form-item:visible filtered by exact label text, then fill the input inside that exact form item.
After clicking 新增工艺路线, wait for the visible 新增工艺路线 dialog content to render .route-form-content before looking up field form items. Within that currently visible dialog, wait for visible .el-form-item labels such as 编码/路线编码 and 名称/路线名称 or dialog-scoped inputs with placeholders 请输入编码/请输入名称 before calling the form-item fill helper. Do not return form item not found while the dialog shell is visible but RouteFormContent is still loading; poll the current dialog for up to 30 seconds and include the latest visible dialog text only if the form content never renders. Ignore stale background table rows such as TN-ROUTE-VERSION-001 when filling the current create dialog; background list text is not evidence that the dialog-scoped create form is ready or filled.
After a successful save toast such as 新增成功 or 保存成功, some pages keep the add/edit dialog open and switch it into detail/edit tabs. In that case, close the still-open dialog or drawer before running a list search by clicking visible 关闭/返回 or the header close button. Do not treat a still-open post-save dialog as a failed save when the success toast is visible and required values remain present. If a post-save close button becomes detached or unstable, press Escape or click the header close once, then return to the list and verify the saved row by quick-filter before marking the checkpoint BLOCKED. If the list already shows the fixed route code/name after save, checkpoint 2 should pass even when closing the transient post-save dialog was flaky.
Close-only cleanup helpers must be fail-soft. If clicking a visible 关闭/返回/取消 button times out, is intercepted, becomes detached, or remains unstable, catch that click error, then try the header close button and Escape. Do not let a close-only helper throw and convert an already successful save/copy into a BLOCKED business checkpoint. For 工艺路线版本发布 after copying RT000028 into TN-ROUTE-VERSION-001, first verify the copied route appears in the list by quick-filter; if it appears, checkpoint 2 copy succeeded even when the transient copy dialog/drawer close action was flaky.
Before clicking any list quick-filter 查询/搜索 button, assert that no .el-dialog, .el-drawer, or .el-overlay-dialog is still visible. If an Element Plus overlay is still visible, close it with the scoped footer 关闭/返回 button or header .el-dialog__headerbtn/.el-drawer__close-btn, then wait for .el-dialog:visible, .el-drawer:visible, .el-overlay-dialog:visible to disappear before retrying the list query. If a quick-filter 查询 click is intercepted by .el-overlay-dialog, close the overlay and retry the same quick-filter query once instead of returning BLOCKED immediately.
For field-selector list filters, first read the visible selected field label and use the matching identifier input instead of assuming a name field exists. On the 工艺路线 list, the default selected field is 路线编码; for the fixed basic-maintenance sample, search with route code TN-ROUTE-BASIC-001 first. For fixed samples and cleanup/detail lookups, search by route code first and prefer staying on 路线编码 rather than switching to 路线名称.
For 工艺路线复制绑定 fixed source route lookup, the current test-tenant source sample is RT000028 / 球囊扩张压力泵. when the selected quick-filter field is 路线编码, fill RT000028, never 球囊扩张压力泵. when searching by source route name, first confirm the selected field is 路线名称, then fill 球囊扩张压力泵 and verify the returned visible row still has route code RT000028. Do not report the fixed source route missing after submitting 球囊扩张压力泵 while the selected field remains 路线编码; that is a field/value mismatch, not missing data.
For 工艺路线版本发布 fixed source route lookup, the current test-tenant source sample is also RT000028 / 球囊扩张压力泵; do not hardcode 按压式球囊扩充压力泵 as the only source route name. If older case text mentions 按压式球囊扩充压力泵, treat it as stale sample text and first locate the visible source row by route code RT000028, then verify the row text contains 球囊扩张压力泵 before copying.
For 工艺路线版本发布 candidate visibility, after 创建候选版本, treat the checkpoint as PASS when the visible version workspace shows a draft candidate such as V2 草稿 and any current active version marker such as V1 已生效, 已生效 ACTIVE, 当前 ACTIVE, ACTIVE, 当前生效, 生效版本, or 当前版本. Do not require the exact phrase 当前生效版本说明. Never report 创建候选版本后页面缺少候选版本或当前生效版本说明 when the workspace text contains both 草稿 and 已生效/ACTIVE.
For 工艺路线版本发布 candidate cleanup, the visible cleanup action for a draft candidate row may be 删除草稿 rather than 取消候选. scope the click to the row/card that contains 草稿 or 候选版本, then click 删除草稿/取消候选/删除候选/作废候选/撤销候选 and confirm any Element Plus message box. Never report the candidate cleanup entry missing while a visible 删除草稿 action exists. After deleting the draft candidate, verify the candidate row no longer shows 草稿/待处理/待发布 before closing the version workspace and deleting the temporary test route.
For 工艺路线版本发布 candidate cleanup completed state, if the version workspace shows 无打开候选 and no visible version row or card contains 草稿/待处理/待发布, treat the candidate cleanup as already complete. During checkpoint 4 cleanup, do not click 创建候选版本 when 无打开候选 is visible, because that recreates the draft you just cleaned. Also use the exact rule phrase: do not click 创建候选版本 during checkpoint 4 cleanup. Instead, close the version workspace and delete the temporary route from the list, then verify no visible table body row contains TN-ROUTE-VERSION-001 or 测试节点-工艺路线-版本发布.
For Element Plus link-button cleanup actions such as 删除草稿, include text descendants such as span and then climb to the closest button/.el-button/[role="button"]/a action element before judging the action missing. A safe pattern is to scan visible elements matching button, .el-button, [role="button"], a, span with the cleanup text, resolve each candidate to closest('button,.el-button,[role="button"],a') || element, and scope it to the row/card whose ancestor text contains 草稿/候选版本/待处理/待发布. If the action text is visible but the resolved action is temporarily loading or disabled, wait up to 15 seconds for the action element to stop being [disabled], [aria-disabled="true"], .is-disabled, or .is-loading before clicking. If the page body contains 删除草稿 but the action never becomes enabled, return BLOCKED as visible but disabled/loading instead of entry missing, including the action class/disabled/aria-disabled state in actualText.
For Element Plus row operation link-buttons such as 删除, 编辑, 复制, 版本, or 删除草稿, text is often inside a span rendered by a link-style button. Include span text candidates, climb to closest('button,.el-button,[role="button"],a') || element, and click the resolved ElementHandle directly after checking its DOM state with elementHandle.evaluate. Do not wrap an ElementHandle from evaluateHandle/elementHandle in page.locator(':scope').locator(handle), because Playwright locators cannot be built from ElementHandle objects that way. Never report a route delete entry missing while the visible row text contains 删除; instead include the resolved element tag/class/disabled state if the action cannot be clicked.
If a visible 删除草稿 text/action exists in the fixed operation column while the workspace text contains 草稿, click the resolved closest action element directly; do not require the same DOM ancestor to also contain 草稿. In this fixed-column case, do not return candidate cleanup failed before attempting that click. After clicking and confirming, verify the click by either 无打开候选 or disappearance of the V2 草稿 row.
Element Plus fixed columns can split the 草稿 status cell and 删除草稿 operation button into separate DOM tables or duplicate rows. If the version workspace text proves a 草稿 candidate exists and a visible 删除草稿 action exists anywhere in the same visible workspace, click the visible 删除草稿 action even when it is not a descendant of the same tr, card, or row as the 草稿 text. Do not report 页面存在删除草稿文字但未能解析到候选行可点击动作 in fixed-column tables; instead resolve the visible 删除草稿 text to its closest button/.el-button/[role="button"]/a, click it, confirm the Element Plus message box, then re-read the workspace for 无打开候选 or absence of 草稿/待处理/待发布.
For 删除草稿 candidate cleanup, start waiting for /admin-api/mes/pro/route-version/cancel before clicking the cleanup action or before confirming the message box, then wait for the same Element Plus message box to disappear and for a route-version list refresh or workspace transition to 无打开候选/no 草稿. If cleanup still fails, include whether the cancel request fired, its HTTP status/business code, message-box text, and whether the message box disappeared in actualText.
For 工艺路线状态删除 enable/disable verification, the route list status control is the 状态 column el-switch, not an operation-column text button. The row may show only 产品/编辑/复制/版本/删除 in the operation column; do not report 停用入口不可见 or 启用入口不可见 just because no text button exists. Locate the fixed route row TN-ROUTE-STATUS-001, find its status-column .el-switch / [role="switch"] / .el-switch__core, and click the switch to toggle. Confirm the Element Plus message box text for 启用 or 停用. Judge enabled/disabled state from the switch checked state, aria-checked, .is-checked class, or active/inactive value after the list refresh; do not require visible row text 启用 or 停用. If the switch starts inactive, click it to enable first, then optionally disable and enable again so checkpoint 3 can prove the route is enabled before deletion. Only block if the status switch is missing or disabled despite mes:pro-route:update permission.
For list search forms with a left field selector and a right text input, fill the right text input, not the left selector input. The left selector only chooses the field such as 路线编码/路线名称 and its inputValue may be truncated or empty; do not fail merely because the left selector inputValue is truncated or empty. After filling the right text input, click 查询/搜索 and judge success from the submitted table results.
After deleting or resetting a route and re-running the quick-filter, judge absence only from visible table body rows such as .el-table__body-wrapper tbody tr.el-table__row or .unified-list-template__table-shell tbody tr. Do not search page.locator('body').innerText() for the route code/name because the quick-filter input still contains the submitted value and success toast/header text may remain visible. Table headers, quick-filter inputs, sidebar text, toast text, and No Data placeholders are not route rows. If no visible body row contains the fixed route code or name, treat No Data/empty table body as successful absence even when the page body still contains the submitted search value in the input.
The selected field and the submitted value must match: if the left selector is 路线编码, fill the route code; if the visible field is 路线名称, fill the route name. Only switch to 路线名称 when that option is visible in the currently opened quick-filter dropdown. if 路线名称 is not visible, keep 路线编码 and search by route code instead of returning BLOCKED. Never submit route name text while the selected field remains 路线编码.
Scope quick-filter interactions to the visible .table-quick-filter or .unified-list-template__quick-filter. Do not scan all page .el-select controls when determining the quick-filter field because unrelated dialogs and page filters may have other selected values. Read the selected field only from .table-quick-filter__field, fill the query text only inside .table-quick-filter__value, and click the 查询 button inside or nearest to the same quick-filter container. After switching 路线编码/路线名称, re-read .table-quick-filter__field before filling. Do not throw just because quick-filter option 路线名称 is unavailable; use the current visible field with the matching fixed code/name value. if the visible field is 路线名称, fill the route name, never the route code; if the visible field is 路线编码, fill the route code, never the route name.
When the local login page appears, scope all login locators to .login-form and use the visible prefilled login form values or read VITE_APP_DEFAULT_LOGIN_* from the frontend .env files under the frontend project root. If a tenant select exists, select/fill it from .login-form .el-select using VITE_APP_DEFAULT_LOGIN_TENANT first. Fill the username only with .login-form input[placeholder="请输入用户名"] or .login-form input.el-input__inner:not([type="password"]):not([role="combobox"]); fill the password only with .login-form input[type="password"] or .login-form input[placeholder="请输入密码"]. Always overwrite the .login-form username and password inputs with the local default login values before clicking 登录. Do not keep stale prefilled username or password values. If the local default username or password is missing and the visible input remains empty, return BLOCKED before clicking 登录; do not submit an empty login form. Never fill login by using page.locator('input:visible').first(), and do not use locator.filter({ hasNot: page.locator('[type="password"]') }) to exclude password fields because that does not exclude the input element itself. After clicking 登录, wait for the /admin-api/system/auth/login POST response with business code 0, then wait for /admin-api/system/auth/get-permission-info and for the URL to leave /login. Never click 登录 and then continue waiting for business controls when the login response is missing or not business code 0. After the URL leaves /login, explicitly navigate back to the target history route such as /mes/pro/route before waiting for business controls; do not assume the redirect parameter completed the target navigation. Do not require INT_RUOYI_E2E_USERNAME or INT_RUOYI_E2E_PASSWORD for local IntRuoyi login. Never print passwords, tokens, cookies, Authorization headers, or raw credential values.
After login and after every direct navigation to a target history route such as /mes/pro/route, wait up to 60 seconds for either target business controls or a visible .login-form / /login URL before using list helpers. If login appears after an initial target-route navigation, perform the scoped local login, then navigate to the target route again and wait for business controls; repeat this login-or-controls loop up to 2 times. Do not return "Already authenticated" only because .login-form was not visible in the first few seconds after route navigation, because the Vue app may still be asynchronously redirecting to /login. For target controls, require .table-quick-filter or .unified-list-template__quick-filter to be visible and, for list pages, require .el-table or .unified-list-template__table-shell to be visible. Check those controls with visible-only locators such as .table-quick-filter:visible, .unified-list-template__quick-filter:visible, .el-table:visible, and .unified-list-template__table-shell:visible, or iterate all matched candidates and accept the first candidate whose isVisible() is true. Do not call isVisible() on an unfiltered multi-locator like page.locator('.table-quick-filter, .unified-list-template__quick-filter') because a hidden duplicate can be first and make a rendered page look not ready. If visible page text already contains the 工艺流程 title, 查询/新增 buttons, table headers 路线编码/路线名称/状态, and body rows, do not return Target route controls did not render; fix the selector to the visible controls and continue. If the page remains on /login after successful login, or target controls still do not render after the second target-route navigation, capture a screenshot and return a BLOCKED checkpoint with current URL plus visible page text or console/network evidence; do not call quickFilter() before this page-ready wait.
List query buttons may be labeled 查询 or 搜索; use page.getByRole('button', { name: /查询|搜索/ }) or page.locator('button, .el-button').filter({ hasText: /查询|搜索/ }) instead of searching only for 搜索.
For generic 工艺路线 detail verification, use a visible 详情/查看 action only when that exact action exists; when the route list uses a 路线编码 column link as the only detail entry, click the route code link instead. For 工艺路线基础维护 detail verification on /mes/pro/route, the 路线编码 column link opens the RouteForm detail. Do not click operation-column 编辑 for this base detail check because operation-column 编辑 is production-config editing, not base detail. Do not click 版本 for base detail verification because it opens the version workspace and can fail with 工艺路线候选版本快照不完整 for newly-created routes.
For 工艺路线复制绑定 detail verification, click the visible 路线编码 link inside the copied row, not a duplicate hidden fixed-column row, stale row, background action, or blank dialog entry. If the route-code link exists, do not fall back to 详情/查看 if the route-code link exists. After opening the dialog, the opened 工艺路线详情 dialog must show the copied route code and copied route name before checking tabs. Dialog placeholders such as 请输入编码 or 请输入名称 mean the wrong blank detail form was opened; close it and reopen from the visible copied row route-code link, then re-check that copied code/name are present before validating 基础信息、流转关系图、关联产品.
For 工艺路线复制绑定 tab checks, do not declare 流转关系图 empty just because the active tab pane innerText is short or the left/right detail sidebars say 请选择工序查看详情 or 点击左侧字段查看明细. The route flow graph is rendered mostly with div nodes and CSS connectors, not necessarily canvas or svg. Treat 流转关系图 as visible when the tab shows .route-flow-graph-designer, .route-flow-graph-designer__canvas, .route-flow-graph-designer__node, [data-flow-node="route-process"], visible process node cards, connector lines, or the route name/current-version toolbar. A blank failure is valid only when the flow tab has no visible graph container, no route-process nodes/cards, no connector/flow canvas, and shows an actual empty state such as .el-empty/暂无数据.
The route-code detail entry is the actual Element Plus link button, not the surrounding .cell table container. Do not include .cell as a clickable route-code candidate. For the copied-row route code, prefer button.el-button.is-link, .el-button.is-link, a, or [role="link"] filtered by the exact copied route code, scoped to the visible row and not the operation column. After clicking the code entry, assert that a 工艺路线详情 dialog opened; if no dialog opens, retry the real descendant link/button in the 路线编码 column before failing.
RouteForm detail values may be stored in Element Plus input values rather than dialog innerText; do not fail detail verification only because modal.innerText contains labels such as 编码 生成 名称. For 工艺路线基础维护 and 工艺路线复制绑定 detail checks, read the exact .el-form-item 编码 and 名称 inputValue from the opened 工艺路线详情 dialog and compare with the fixed/copied route code/name. Do not read once and fail immediately when values are empty: after clicking a route-code link, wait for the /admin-api/mes/pro/route/get?id= response or poll the exact form-item input values for up to 30 seconds. Use DOM value reads from input.el-input__inner, input, or textarea inside the exact .el-form-item instead of relying on locator.inputValue() only. A safe helper pattern is: async function readRouteFormValue(modal, labelRegex, rejectRegex) { const items = modal.locator('.el-form-item'); const count = await items.count(); for (let i = 0; i < count; i += 1) { const item = items.nth(i); const label = ((await item.locator('.el-form-item__label').first().innerText().catch(() => '')) || '').replace(/\\s+/g, ''); const text = ((await item.innerText().catch(() => '')) || '').replace(/\\s+/g, ''); if (!labelRegex.test(label) && !labelRegex.test(text)) continue; if (rejectRegex && (rejectRegex.test(label) || rejectRegex.test(text))) continue; const input = item.locator('input.el-input__inner, input, textarea').first(); if (await input.count()) return await input.evaluate(el => el.value || el.getAttribute('value') || ''); } return ''; }. A safe wait is: for (let i = 0; i < 60; i += 1) { const code = await readRouteFormValue(modal, /编码|路线编码/); const name = await readRouteFormValue(modal, /名称|路线名称/, /编码|基础信息/); if (code === expectedCode && name === expectedName) break; await page.waitForTimeout(500); }. placeholder-only or empty input values after this wait mean the detail data has not loaded or the wrong blank form is open; close the blank dialog, return to the list, search the fixed/copied route again, and reopen from the real route-code link button, then repeat the same 30-second value wait before failing. If the expected code/name are visible in the background row while a blank RouteForm dialog is open, that is not sufficient; the opened dialog input values must match.
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
