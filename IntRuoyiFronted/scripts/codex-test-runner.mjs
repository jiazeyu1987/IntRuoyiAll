#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { resolveCaseSpecificGuidance } from './codex-test-runner-guidance.mjs'

await import('playwright')

const API_BASE = requiredEnv('CODEX_TEST_API_BASE').replace(/\/$/, '')
const RUNNER_TOKEN = process.env.CODEX_TEST_RUNNER_TOKEN || ''
const MANAGEMENT_TENANT_ID = requiredEnv('CODEX_TEST_TENANT_ID')
const FRONTEND_BASE_URL = requiredEnv('CODEX_TEST_FRONTEND_BASE_URL')
const WORKING_DIRECTORY = process.env.CODEX_TEST_WORKDIR || process.cwd()
const PROJECT_ROOT = process.env.CODEX_TEST_PROJECT_ROOT || WORKING_DIRECTORY
const FRONTEND_PROJECT_ROOT = process.env.CODEX_TEST_FRONTEND_ROOT || process.cwd()
const PLAYWRIGHT_HARNESS_PATH = path.join(FRONTEND_PROJECT_ROOT, 'scripts', 'codex-test-playwright-harness.cjs')
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
const CODEX_TEST_HEARTBEAT_API_TIMEOUT_MS = Number(process.env.CODEX_TEST_HEARTBEAT_API_TIMEOUT_MS || '90000')
const CODEX_CHILD_SETTLE_TIMEOUT_MS = Number(process.env.CODEX_TEST_CHILD_SETTLE_TIMEOUT_MS || '5000')
const COMPLETE_CASE_SUMMARY_MAX_LENGTH = 512
const CODEX_FAILURE_DETAIL_MAX_LENGTH = 2400
const RUNNER_HTTP_CONNECTION_HEADERS = { Connection: 'close' }
const ANALYSIS_MODE_PLAYWRIGHT_E2E = 'PLAYWRIGHT_E2E'
const ANALYSIS_MODE_CODE_READONLY = 'CODE_READONLY'
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

async function postJson(url, body, options = {}) {
  const response = await requestWithTimeout(url, {
    method: 'POST',
    headers: runnerHeaders({
      'Content-Type': 'application/json'
    }),
    body: JSON.stringify(body)
  }, options.timeoutMs)
  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(`${url} failed: ${payload.msg || response.statusText}`)
  }
  return payload.data
}

async function requestWithTimeout(url, options, timeoutMs = CODEX_TEST_API_TIMEOUT_MS) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(`${API_BASE}${url}`, {
      ...options,
      signal: controller.signal,
      cache: 'no-store'
    })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new Error(`${url} timed out after ${timeoutMs}ms`)
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
      PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH: resolveBrowserExecutablePath(),
      CODEX_TEST_PLAYWRIGHT_HARNESS_PATH: PLAYWRIGHT_HARNESS_PATH
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
  const reasoningEffort = resolveAnalysisMode(task) === ANALYSIS_MODE_CODE_READONLY || isReadOnlyTask(task)
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
  }, { timeoutMs: CODEX_TEST_HEARTBEAT_API_TIMEOUT_MS })
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
  const analysisMode = resolveAnalysisMode(task)
  if (analysisMode === ANALYSIS_MODE_CODE_READONLY) {
    return buildCodeReadonlyPrompt(task, codexExecTimeoutMs)
  }
  return buildPlaywrightPrompt(task, codexExecTimeoutMs)
}

function buildCodeReadonlyPrompt(task, codexExecTimeoutMs = resolveCodexExecTimeoutMs(task)) {
  const executionBudgetSeconds = Math.max(30, Math.floor(codexExecTimeoutMs / 1000) - 10)
  return `你正在执行企业级 Codex 只读代码分析测试。
本任务是只读代码分析，不是 Playwright 浏览器 E2E。
不要打开浏览器作为优先路径；请优先扫描本地代码、路由、API、服务、数据模型、迁移和测试来判断职责描述是否已经被当前代码满足。
只读代码分析必须覆盖代码、路由、API、测试等证据，不得以浏览器优先结果替代代码审查。
允许读取仓库文件、运行只读搜索命令、运行只读静态检查命令；不得创建、修改或删除任何仓库文件、任务文档、源码、配置、构建产物、Git 状态、提交、分支或 worktree。
不得运行会写入业务数据、修改数据库、启动写入型 E2E、提交表单、审批、删除、导入、上传、发布或清理数据的命令。
不得返回默认成功、不得把缺少前置条件伪装成 PASS；如果代码入口、依赖、权限、Runner、Codex CLI 或测试资料缺失，请返回 BLOCKED 并写明缺失前置条件。
如果代码与职责描述不一致，请返回 FAIL，并在 mismatchDescription 中说明具体差异、缺失文件/接口/状态链路或测试缺口。
最终回答必须是原始 JSON 对象，不要包含 markdown、解释性文字或代码块。
每个检查点都必须输出 checkpointResults；FAIL 必须包含 mismatchDescription，BLOCKED 必须包含阻塞原因。
Project guidance root: ${PROJECT_ROOT}
Working directory: ${WORKING_DIRECTORY}
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
      "actualText": "real code-analysis evidence",
      "mismatchDescription": "required when status is FAIL"
    }
  ],
  "summary": "short code analysis summary"
}

Complete the repository read-only analysis and return JSON within ${executionBudgetSeconds} seconds.`
}

function buildPlaywrightPrompt(task, codexExecTimeoutMs = resolveCodexExecTimeoutMs(task)) {
  const taskMode = isReadOnlyTask(task) ? 'READ_ONLY' : 'MUTATING_OR_UNKNOWN'
  const executionBudgetSeconds = Math.max(30, Math.floor(codexExecTimeoutMs / 1000) - 10)
  return `You are executing an enterprise E2E test with Playwright.
Use the real browser against ${FRONTEND_BASE_URL}. Do not use API-only shortcuts except read-only final verification.
This task is classified as ${taskMode}.
This is a browser execution task, not a repository development task.
Do not create or modify repository files, task documents, source code, configuration, build outputs, Git state, commits, branches, or worktrees.
Do not run project builds or project test suites.
Use only task-owned temporary files under ${WORKING_DIRECTORY}.
Execution strategy: create one temporary Node.js Playwright script under ${WORKING_DIRECTORY}, keep it as a short scenario script, run it with node, then return the final JSON.
Official reusable Playwright harness: ${PLAYWRIGHT_HARNESS_PATH}
Your temporary script must import the harness exactly like this:
const { createCodexTestPlaywrightHarness } = require(${JSON.stringify(PLAYWRIGHT_HARNESS_PATH)});
const harness = createCodexTestPlaywrightHarness({ baseUrl: ${JSON.stringify(FRONTEND_BASE_URL)}, targetPath: '<target Vue history path>', tempRoot: ${JSON.stringify(WORKING_DIRECTORY)}, frontendRoot: ${JSON.stringify(FRONTEND_PROJECT_ROOT)}, checkpointCount: ${task.checkpoints.length}, summaryPrefix: ${JSON.stringify(task.caseName)} });
harness.startExecution(async (h) => {
  await h.ensureHistoryPageReady('<target Vue history path>');
  // Put only checkpoint-specific browser steps here.
});
Keep the temporary scenario script under 250 lines and 12000 bytes. Generate only scenario orchestration: checkpoint order, fixed sample values, and the minimum target-page interactions that are not already provided by the harness.
Do not reimplement shared helpers such as captureScreenshot, recordCheckpoint, printOutputAndExit, clickVisibleTextAction, clickRouteRowAction, clickDialogBusinessAction, runBrowserFlow, login handling, deadline handling, Element Plus MessageBox confirmation, quick-filter helpers, row-action resolution, or route dialog form-item helpers. Use the corresponding harness methods instead. If a tiny case-local helper is absolutely required, keep it business-specific and do not duplicate any harness helper.
Before running the temporary Node.js Playwright script, run node --check <temporary-script-path>. This syntax check does not count as running the browser script. If node --check fails, fix the generated script before browser launch instead of running invalid JavaScript.
Generated scripts must avoid redeclaring const or let identifiers in the same function or block. Do not reuse names such as modal, dialog, rows, values, result, or button for a second const/let declaration in the same scope; either assign to an existing let variable or use a unique name such as detailDialog, retryDialog, copiedRows, or tabVisibility. If a syntax error says Identifier '<name>' has already been declared, repair the duplicate declaration before executing the script.
Generated scripts must not reference block-scoped variables outside the try/catch/block where they are declared. If a value such as cleanupOutcome, resetOutcome, detailOutcome, or routeValues is needed after a try block, declare let cleanupOutcome = null before the try and assign it inside, or build the final summary from checkpointResults instead. Never write const cleanupOutcome inside try { ... } and then read cleanupOutcome after the try/catch.
Generated Playwright scripts must treat page.url() as a synchronous string-returning method. Never write await page.url(), page.url().catch(...), or await pageHandle.url().catch(...). Use a synchronous helper such as try { return page.url(); } catch { return 'url-unavailable'; } when collecting diagnostic URLs.
Do not inspect the repository before the first browser attempt; inspect local source only if the browser path is blocked by selectors, routes, or prerequisite evidence.
When the temporary script prints raw JSON with checkpointResults, return that JSON immediately.
Run the temporary Playwright script at most once before returning. If stdout contains raw JSON with checkpointResults, return that JSON verbatim immediately. Do not keep debugging, rerunning, or launching extra browsers after JSON is available.
If the temporary script exits with a JSON result that contains FAIL or BLOCKED checkpoints, return that JSON as the business evidence instead of spending the remaining budget trying to repair the generated script.
The final assistant response must be exactly the JSON object printed by the temporary Playwright script. Do not add analysis, screenshots, markdown fences, or follow-up debugging after the JSON. If the temporary script reports BLOCKED or FAIL, return that same JSON immediately as the final answer.
The temporary Playwright script must enforce its own overall deadline that is shorter than the remaining execution budget. Hard cap the temporary browser script deadline at 240000ms. Do not compute the temporary script deadline from the full Codex exec timeout, because script generation can already consume several minutes before node starts. Use const scriptDeadlineMs = Math.min(240000, Math.max(30000, Number(process.env.CODEX_TEST_BROWSER_FLOW_TIMEOUT_MS || 240000))) and const deadlineAt = Date.now() + scriptDeadlineMs. Never generate deadlines such as 300000, 540000, or 560000ms for the temporary browser script. It must race the main browser flow against that deadline and always print checkpointResults JSON before the Codex child timeout. If the deadline is reached, close the browser and return BLOCKED checkpoints for unfinished items instead of letting codex exec hit the outer child timeout. After printing the deadline BLOCKED JSON, force the temporary Node process to exit with process.exit(0); do not leave an unresolved flowPromise or Playwright browser watcher alive after Promise.race resolves. The deadline handler should include the current URL, current visible page text, and the last known phase/checkpoint in actualText, but it must not keep debugging or rerun the browser script.
For Element Plus dialogs, click visible buttons by accessible role or exact visible text.
Element Plus dialog/drawer footer action buttons such as save or confirm may be outside the field form scope; after filling fields, search the entire visible dialog/drawer or page for 保存/确定/提交, not only the field form scope.
Element Plus button innerText may contain whitespace between Chinese characters, such as 保 存; use the whitespace-tolerant action regex /保\\s*存|确\\s*定|提\\s*交/ instead of exact /^保存$/ text.
Business confirmation dialogs may use verb-specific primary buttons instead of generic save labels, for example 确认复制, 复制, 确认发布, 发布, 启用, 停用, 删除, 确认删除. Use a business action regex such as /保\\s*存|确\\s*定|提\\s*交|确\\s*认\\s*复\\s*制|复\\s*制|确\\s*认\\s*发\\s*布|发\\s*布|启\\s*用|停\\s*用|确\\s*认\\s*删\\s*除|删\\s*除/ and search the current visible dialog or drawer before any background row buttons.
When an Element Plus message box is visible, click the primary action only inside .el-message-box:visible or .el-overlay-message-box:visible. Do not include background page buttons in the same locator while a message box is visible. Use .el-message-box__btns button or .el-overlay-message-box button filtered by 确定/确认/删除, and prefer the visible primary button in that message box. If a click is intercepted by .el-overlay-message-box, re-scope to the visible message box and retry once.
After clicking a visible Element Plus message-box primary action, wait for the same visible .el-message-box/.el-overlay-message-box to become hidden before reading background page state. If the message box remains visible, return BLOCKED with the message-box title/text/buttons instead of polling stale workspace or table text.
Use this deterministic Element Plus footer selector after filling a dialog or drawer: page.locator('.el-dialog__footer button, .el-drawer__footer button').filter({ hasText: /保\\s*存|确\\s*定|提\\s*交/ }).
Some pages render save actions in custom footer rows instead of .el-dialog__footer or .el-drawer__footer; if the scoped footer selector has no visible candidate, locate the visible action across the current dialog/drawer or page with page.locator('button, .el-button').filter({ hasText: /保\\s*存|确\\s*定|提\\s*交/ }) before declaring the save button missing.
Do not use locator.last() for save/confirm buttons because hidden duplicate buttons can appear after the visible action in DOM order. Iterate candidates and click the first visible enabled one, for example: const actionCandidates = page.locator('.el-dialog:visible button, .el-drawer:visible button, .el-overlay:visible button, button, .el-button').filter({ hasText: /保\\s*存|确\\s*定|提\\s*交/ }); const actionCount = await actionCandidates.count(); for (let i = 0; i < actionCount; i += 1) { const action = actionCandidates.nth(i); if (await action.isVisible().catch(() => false) && await action.isEnabled().catch(() => false)) { await action.scrollIntoViewIfNeeded().catch(() => {}); await action.click(); break; } }.
Form inputs inside add/edit dialogs must be scoped to the currently visible .el-dialog or .el-drawer. Do not fill background list filter inputs after opening a dialog or drawer, even if the background field has a matching placeholder such as 路线编码. After filling required dialog fields, verify the dialog-scoped inputValue before clicking save; if the dialog-scoped 编码 input does not equal the fixed route code, keep filling the visible dialog input instead of clicking save.
When filling Element Plus dialog fields, locate only .el-form-item containers for the exact label; do not search broad div, section, row, or column containers by hasText because a whole 基础信息 section can contain both 编码 and 名称. For 名称, the container text must include 名称 and must not also include 编码 or 基础信息 before filling the contained input. Prefer .el-form-item:visible filtered by exact label text, then fill the input inside that exact form item.
Case-specific browser guidance:
${resolveCaseSpecificGuidance(task)}
When the local login page appears, scope all login locators to .login-form and use the visible prefilled login form values or read VITE_APP_DEFAULT_LOGIN_* from the frontend .env files under the frontend project root. If a tenant select exists, select/fill it from .login-form .el-select using VITE_APP_DEFAULT_LOGIN_TENANT first. Fill the username only with .login-form input[placeholder="请输入用户名"] or .login-form input.el-input__inner:not([type="password"]):not([role="combobox"]); fill the password only with .login-form input[type="password"] or .login-form input[placeholder="请输入密码"]. Always overwrite the .login-form username and password inputs with the local default login values before clicking 登录. Do not keep stale prefilled username or password values. If the local default username or password is missing and the visible input remains empty, return BLOCKED before clicking 登录; do not submit an empty login form. Never fill login by using page.locator('input:visible').first(), and do not use locator.filter({ hasNot: page.locator('[type="password"]') }) to exclude password fields because that does not exclude the input element itself. After clicking 登录, wait for the /admin-api/system/auth/login POST response with business code 0, then wait for /admin-api/system/auth/get-permission-info and for the URL to leave /login. Never click 登录 and then continue waiting for business controls when the login response is missing or not business code 0. After the URL leaves /login, explicitly navigate back to the target history route such as /mes/pro/route before waiting for business controls; do not assume the redirect parameter completed the target navigation. Do not require INT_RUOYI_E2E_USERNAME or INT_RUOYI_E2E_PASSWORD for local IntRuoyi login. Never print passwords, tokens, cookies, Authorization headers, or raw credential values.
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

function resolveAnalysisMode(task) {
  if (!task.analysisMode) {
    return ANALYSIS_MODE_PLAYWRIGHT_E2E
  }
  if (task.analysisMode === ANALYSIS_MODE_CODE_READONLY) {
    return ANALYSIS_MODE_CODE_READONLY
  }
  if (task.analysisMode === ANALYSIS_MODE_PLAYWRIGHT_E2E) {
    return ANALYSIS_MODE_PLAYWRIGHT_E2E
  }
  throw new Error(`Unsupported Codex analysisMode: ${task.analysisMode}`)
}

function resolveCodexExecTimeoutMs(task) {
  return resolveAnalysisMode(task) === ANALYSIS_MODE_CODE_READONLY || isReadOnlyTask(task)
    ? CODEX_EXEC_READONLY_TIMEOUT_MS
    : CODEX_EXEC_TIMEOUT_MS
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
