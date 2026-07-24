const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawn, spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const defaultProjectRoot = path.resolve(frontendRoot, '..')
const projectRoot = path.resolve(process.env.EDHR_ASSIST_FILL_PROJECT_ROOT || defaultProjectRoot)
const taskDir = path.join(projectRoot, 'doc', 'tasks', '20260722-edhr-assist-checkbox-choice-group')
const artifactDir = path.join(taskDir, 'e2e-artifacts')
const resultFile = path.join(artifactDir, 'assist-fill-mode-real-e2e-result.json')
const screenshotFile = path.join(artifactDir, 'assist-fill-mode-real-e2e.png')

const config = {
  baseUrl: process.env.EDHR_ASSIST_FILL_BASE_URL || 'http://localhost:8081',
  tenant: process.env.EDHR_ASSIST_FILL_TENANT || '测试租户',
  username: process.env.EDHR_ASSIST_FILL_USERNAME || 'aoteman',
  password: process.env.EDHR_ASSIST_FILL_PASSWORD || '',
  headed: process.env.EDHR_ASSIST_FILL_HEADED === '1',
  maxRowsToTry: Number(process.env.EDHR_ASSIST_FILL_MAX_ROWS || 10),
  startFrontend: process.env.EDHR_ASSIST_FILL_START_FRONTEND === '1',
  backendUrl: process.env.EDHR_ASSIST_FILL_BACKEND_URL || 'http://127.0.0.1:48081'
}

const listPath = '/mes/pro/feedback/edhr-batch-execution'
const workTaskPath = '/mes/pro/feedback/edhr-work-task'
const formPath = '/mes/pro/feedback/edhr-execution/form'

function failFast(message, details = {}) {
  const error = new Error(message)
  error.details = details
  return error
}

function ensureConfig() {
  const blockers = []
  const allowedBaseUrls = new Set([
    'http://localhost:8081',
    'http://127.0.0.1:8081',
    'http://localhost:8096',
    'http://127.0.0.1:8096'
  ])
  if (!allowedBaseUrls.has(config.baseUrl)) {
    blockers.push(`baseUrl must be an approved local int_main/worktree URL, got ${config.baseUrl}`)
  }
  if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
    blockers.push(`real writable E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`)
  }
  if (!config.password) {
    blockers.push('EDHR_ASSIST_FILL_PASSWORD is required')
  }
  if (!fs.existsSync(path.join(projectRoot, 'scripts', 'preflight', 'login-preflight.mjs'))) {
    blockers.push(`EDHR_ASSIST_FILL_PROJECT_ROOT must point to IntRuoyi root with scripts/preflight, got ${projectRoot}`)
  }
  if (config.startFrontend && !fs.existsSync(path.join(frontendRoot, 'package.json'))) {
    blockers.push(`frontendRoot must contain package.json, got ${frontendRoot}`)
  }
  if (blockers.length) {
    throw failFast('edhr_assist_fill_e2e_precondition_failed', { blockers })
  }
}

async function waitForHttpReady(url, timeoutMs = 120000) {
  const deadline = Date.now() + timeoutMs
  let lastError
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url)
      if (response.ok || response.status < 500) {
        return { status: response.status }
      }
      lastError = `HTTP ${response.status}`
    } catch (error) {
      lastError = error.message
    }
    await new Promise((resolve) => setTimeout(resolve, 1500))
  }
  throw failFast('frontend_dev_server_not_ready', { url, lastError })
}

async function startFrontendIfRequested(evidence) {
  if (!config.startFrontend) {
    return null
  }
  const base = new URL(config.baseUrl)
  const port = base.port || (base.protocol === 'https:' ? '443' : '80')
  const viteBin = path.join(frontendRoot, 'node_modules', 'vite', 'bin', 'vite.js')
  if (!fs.existsSync(viteBin)) {
    throw failFast('vite_binary_not_found', { viteBin })
  }
  const child = spawn(
    process.execPath,
    [viteBin, '--mode', 'env.local', '--host', base.hostname, '--port', port],
    {
      cwd: frontendRoot,
      env: {
        ...process.env,
        VITE_PORT: port,
        VITE_BASE_URL: config.backendUrl,
        VITE_PROXY_TARGET: config.backendUrl
      },
      stdio: ['ignore', 'pipe', 'pipe']
    }
  )
  const logFile = path.join(artifactDir, `assist-fill-mode-frontend-${port}.log`)
  const logStream = fs.createWriteStream(logFile, { flags: 'a', encoding: 'utf8' })
  child.stdout.on('data', (chunk) => logStream.write(chunk))
  child.stderr.on('data', (chunk) => logStream.write(chunk))
  evidence.startedFrontend = {
    pid: child.pid,
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    logFile
  }
  await waitForHttpReady(config.baseUrl, 180000)
  return { child, logStream }
}

function runOfficialLoginPreflight() {
  const scriptPath = path.join(projectRoot, 'scripts', 'preflight', 'login-preflight.mjs')
  const result = spawnSync(
    process.execPath,
    [
      scriptPath,
      '--base-url',
      config.baseUrl,
      '--tenant',
      config.tenant,
      '--username',
      config.username,
      '--password',
      config.password,
      '--target-path',
      workTaskPath,
      '--target-text',
      '我的待办'
    ],
    {
      cwd: projectRoot,
      env: process.env,
      encoding: 'utf8',
      timeout: 120000
    }
  )
  if (result.status !== 0) {
    throw failFast('official_login_preflight_failed', {
      status: result.status,
      stdout: result.stdout,
      stderr: result.stderr
    })
  }
  return result.stdout.trim()
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 60000 })
    await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function summarizeExecutionListResponse(response) {
  if (!response) return undefined
  try {
    const payload = await response.json()
    const data = payload?.data || {}
    const rows = Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(data)
          ? data
          : []
    return {
      httpStatus: response.status(),
      code: payload?.code,
      total: data.total ?? data.count ?? rows.length,
      rowCount: rows.length,
      rows: rows.map((row) => ({
        id: row.id,
        executionCode: row.executionCode,
        batchCode: row.batchCode,
        status: row.status,
        canOpen: row.canOpen,
        preReleaseEditable: row.preReleaseEditable === true
      })),
      firstRows: rows.slice(0, 3).map((row) => ({
        id: row.id,
        executionCode: row.executionCode,
        batchCode: row.batchCode,
        status: row.status,
        canOpen: row.canOpen,
        preReleaseEditable: row.preReleaseEditable === true
      }))
    }
  } catch (error) {
    return { httpStatus: response.status(), parseError: error.message }
  }
}

async function waitForListDomSettled(page) {
  const firstLink = page.locator('.edhr-list-shell__execution-link').first()
  const emptyText = page.getByText('暂无 eDHR 执行记录', { exact: false }).first()
  await Promise.race([
    firstLink.waitFor({ state: 'visible', timeout: 15000 }).then(() => 'rows'),
    emptyText.waitFor({ state: 'visible', timeout: 15000 }).then(() => 'empty'),
    page.waitForTimeout(15000).then(() => 'timeout')
  ]).catch(() => undefined)
}

async function waitForExecutionListResponse(page, action, evidence) {
  const listResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/batch-record-execution/page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    .catch(() => null)
  await action()
  const listResponse = await listResponsePromise
  let summary
  if (listResponse) {
    assert.ok(listResponse.ok(), `execution_list_http_failed:${listResponse.status()}`)
    summary = await summarizeExecutionListResponse(listResponse)
    evidence.listResponses.push(summary)
  }
  await waitForListDomSettled(page)
  return summary
}

async function openList(page, evidence) {
  const listUrl = new URL(listPath, config.baseUrl)
  await page.getByText('eDHR 执行列表', { exact: false }).first().waitFor({ state: 'attached', timeout: 60000 }).catch(() => undefined)
  return waitForExecutionListResponse(
    page,
    async () => {
      await page.goto(listUrl.toString(), { waitUntil: 'domcontentloaded' })
      await page.getByText('eDHR 执行列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    },
    evidence
  )
}

async function goToNextListPage(page, evidence) {
  const nextButton = page.locator('.el-pagination .btn-next').first()
  if (!(await nextButton.count()) || (await nextButton.isDisabled().catch(() => true))) {
    return null
  }
  return waitForExecutionListResponse(page, async () => nextButton.click(), evidence)
}

const isFillOrReworkWorkTask = (row) => ['FILL', 'REWORK'].includes(String(row?.taskType || '').toUpperCase())

const isOpenableFillWorkTask = (row) =>
  isFillOrReworkWorkTask(row) &&
  ['TODO', 'DOING'].includes(String(row?.status || '').toUpperCase()) &&
  Boolean(row?.id) &&
  Boolean(row?.actionUrl) &&
  Boolean(row?.batchTaskId)

async function summarizeWorkTaskPageResponse(response) {
  if (!response) return undefined
  try {
    const payload = await response.json()
    const data = payload?.data || {}
    const rows = Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(data)
          ? data
          : []
    return {
      httpStatus: response.status(),
      code: payload?.code,
      total: data.total ?? data.count ?? rows.length,
      rowCount: rows.length,
      rows: rows.map((row) => ({
        id: row.id,
        taskCode: row.taskCode,
        taskType: row.taskType,
        status: row.status,
        batchExecutionId: row.batchExecutionId,
        batchTaskId: row.batchTaskId,
        executionId: row.executionId,
        workOrderCode: row.workOrderCode,
        batchCode: row.batchCode,
        processName: row.processName,
        actionUrlPresent: Boolean(row.actionUrl),
        openableFillTask: isOpenableFillWorkTask(row)
      }))
    }
  } catch (error) {
    return { httpStatus: response.status(), parseError: error.message }
  }
}

async function waitForWorkTaskBoardDomSettled(page) {
  await Promise.race([
    page.getByText('我的待办', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 }).then(() => 'header'),
    page.getByRole('button', { name: '处理' }).first().waitFor({ state: 'visible', timeout: 30000 }).then(() => 'process'),
    page.getByText('暂无工作任务', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 }).then(() => 'empty'),
    page.waitForTimeout(30000).then(() => 'timeout')
  ]).catch(() => undefined)
}

async function waitForWorkTaskPageResponse(page, action, evidence) {
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/edhr-work-task/my-page') &&
        new URL(response.url()).searchParams.get('pageSize') !== '1' &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    .catch(() => null)
  await action()
  const response = await responsePromise
  let summary
  if (response) {
    assert.ok(response.ok(), `work_task_page_http_failed:${response.status()}`)
    summary = await summarizeWorkTaskPageResponse(response)
    evidence.workTaskPageResponses.push(summary)
  }
  await waitForWorkTaskBoardDomSettled(page)
  return summary
}

async function openWorkTaskBoard(page, evidence) {
  const url = new URL(workTaskPath, config.baseUrl)
  return waitForWorkTaskPageResponse(
    page,
    async () => {
      await page.goto(url.toString(), { waitUntil: 'domcontentloaded' })
      await page.getByText('我的待办', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    },
    evidence
  )
}

async function goToNextWorkTaskPage(page, evidence) {
  const nextButton = page.locator('.el-pagination .btn-next').first()
  if (!(await nextButton.count()) || (await nextButton.isDisabled().catch(() => true))) {
    return null
  }
  return waitForWorkTaskPageResponse(page, async () => nextButton.click(), evidence)
}

function isWriteMethod(method) {
  return !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())
}

function isMesWriteRequest(request) {
  const url = request.url()
  return isWriteMethod(request.method()) && (url.includes('/admin-api/mes/') || url.includes('/mes/pro/'))
}

async function findEditableDraftInput(page, marker) {
  const rows = page.locator('.edhr-fill-workspace__assist-row')
  const count = await rows.count()
  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index)
    const input = row.locator('textarea:not([disabled]), input:not([disabled]):not([readonly])').first()
    if (!(await input.count()) || !(await input.isVisible().catch(() => false))) {
      continue
    }
    const label = (await row.locator('.edhr-fill-workspace__assist-label').first().innerText()).trim()
    const helpText = (await row.locator('.edhr-fill-workspace__assist-help').first().innerText()).trim()
    const source = (await row.locator('.edhr-fill-workspace__assist-source').first().innerText()).trim()
    const fieldId = await row.getAttribute('data-assist-field-id')
    const previousValue = await input.inputValue().catch(() => '')
    await input.scrollIntoViewIfNeeded()
    await input.fill(marker)
    await page.waitForTimeout(100)
    const currentValue = await input.inputValue().catch(() => '')
    if (currentValue === marker) {
      return { label, helpText, source, previousValue, marker, index, fieldId }
    }
    await input.fill(previousValue).catch(() => undefined)
  }
  return null
}

async function readAssistMissingCount(page) {
  const text = (await page.locator('.edhr-fill-workspace__assist-missing-jump').first().innerText()).trim()
  const match = text.match(/还差\s*(\d+)\s*项/)
  if (!match) {
    throw failFast('assist_missing_count_text_not_found', { text })
  }
  return Number(match[1])
}

async function verifyMissingJump(page) {
  const missingCount = await readAssistMissingCount(page)
  if (missingCount <= 0) {
    throw failFast('assist_missing_jump_requires_incomplete_field', { missingCount })
  }
  await page.locator('.edhr-fill-workspace__assist-missing-jump').first().click()
  await page.waitForFunction(
    () => Boolean(document.querySelector('.edhr-fill-workspace__assist-row.is-highlighted')),
    undefined,
    { timeout: 5000 }
  )
  const highlighted = page.locator('.edhr-fill-workspace__assist-row.is-highlighted').first()
  return {
    missingCount,
    highlightedFieldId: await highlighted.getAttribute('data-assist-field-id'),
    highlightedText: (await highlighted.innerText()).trim().slice(0, 160)
  }
}

async function collectAssistWorkbenchState(page) {
  const quickSwitches = await page.locator('.edhr-fill-workspace__assist-switch').allInnerTexts()
  return {
    hasTopbar: await page.locator('.edhr-fill-workspace__assist-topbar').first().isVisible().catch(() => false),
    rowCount: await page.locator('.edhr-fill-workspace__assist-row').count(),
    legacyCardCount: await page.locator('.edhr-fill-workspace__assist-card').count(),
    quickSwitches: quickSwitches.map((text) => text.replace(/\s+/g, ' ').trim()),
    missingCount: await readAssistMissingCount(page),
    signatureRowCount: await page
      .locator('.edhr-fill-workspace__assist-row')
      .filter({ hasText: '签名' })
      .count()
  }
}

async function enterAssistWorkspaceFullscreen(page) {
  const fullscreenButton = page.locator('.edhr-fill-workspace__fullscreen-action').first()
  await fullscreenButton.scrollIntoViewIfNeeded()
  await fullscreenButton.click()
  await page.waitForFunction(() => Boolean(document.fullscreenElement), undefined, { timeout: 10000 })
  await page.getByRole('button', { name: '退出全屏' }).first().waitFor({ state: 'visible', timeout: 10000 })
  return {
    fullscreenElementClass: await page.evaluate(() => document.fullscreenElement?.className || '')
  }
}

async function verifyAssistSwitchDialogs(page, evidence) {
  const startUrl = page.url()
  const switches = [
    { index: 0, menu: 'task', label: '任务 / 批次' },
    { index: 1, menu: 'process', label: '工序' },
    { index: 2, menu: 'filler', label: '填写人' }
  ]
  const results = []
  const responseErrors = []
  const responseReads = []
  const onResponse = (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/mes/pro/') || response.status() < 400) {
      return
    }
    responseReads.push(
      response
        .text()
        .catch((error) => `response_body_unreadable:${error.message}`)
        .then((body) => {
          responseErrors.push({
            status: response.status(),
            method: response.request().method(),
            url,
            body: body.slice(0, 2000)
          })
        })
    )
  }
  page.on('response', onResponse)

  try {
    for (const item of switches) {
      const switchButton = page.locator('.edhr-fill-workspace__assist-switch').nth(item.index)
      await switchButton.scrollIntoViewIfNeeded()
      await switchButton.click()

      const dialog = page.locator('.edhr-fill-workspace__assist-switch-dialog').first()
      await dialog.waitFor({ state: 'visible', timeout: 30000 })
      const menu = page.locator(`[data-assist-switch-menu="${item.menu}"]`).first()
      await menu.waitFor({ state: 'visible', timeout: 30000 })
      await menu
        .locator('.edhr-fill-workspace__assist-switch-loading')
        .first()
        .waitFor({ state: 'hidden', timeout: 30000 })
        .catch(() => undefined)

      const panelVisible = await page
        .locator('.edhr-fill-workspace__assist-panel')
        .first()
        .isVisible()
        .catch(() => false)
      const assistModeActive = await page
        .locator('.edhr-fill-workspace__view-actions button.is-active')
        .filter({ hasText: '填写辅助模式' })
        .first()
        .isVisible()
        .catch(() => false)
      const optionCount = await menu.locator('.edhr-fill-workspace__assist-switch-option').count()
      const fillerRelations =
        item.menu === 'filler'
          ? await menu.locator('.edhr-fill-workspace__assist-switch-option').evaluateAll((options) =>
              options.map((option) => ({
                taskId: option.getAttribute('data-assist-filler-task-id') || '',
                userId: option.getAttribute('data-assist-filler-user-id') || '',
                text: (option.textContent || '').replace(/\s+/g, ' ').trim(),
                disabled: option.hasAttribute('disabled')
              }))
            )
          : []
      const emptyVisible = await menu.locator('.el-empty').first().isVisible().catch(() => false)
      const errorText = (await menu.locator('.el-alert--error').first().innerText().catch(() => '')).trim()
      const fullscreenActive = await page.evaluate(() => Boolean(document.fullscreenElement))
      const heading = (
        await menu.locator('.edhr-fill-workspace__assist-switch-menu-head').first().innerText()
      )
        .replace(/\s+/g, ' ')
        .trim()

      assert.equal(new URL(page.url()).pathname, formPath, `assist_switch_${item.menu}_should_stay_on_form_path`)
      assert.equal(panelVisible, true, `assist_switch_${item.menu}_should_keep_assist_panel_visible`)
      assert.equal(assistModeActive, true, `assist_switch_${item.menu}_should_keep_assist_mode_active`)
      assert.equal(fullscreenActive, true, `assist_switch_${item.menu}_dialog_should_remain_visible_in_fullscreen`)
      assert.equal(errorText, '', `assist_switch_${item.menu}_should_not_show_error:${errorText}`)
      if (item.menu === 'filler') {
        assert.ok(optionCount > 0 || emptyVisible, 'assist_switch_filler_should_show_options_or_empty_state')
      }
      if (item.menu === 'filler' && fillerRelations.length > 0) {
        assert.equal(
          fillerRelations.every(
            (relation) =>
              Number(relation.taskId) > 0 &&
              Number(relation.userId) > 0 &&
              /批处理表单|工艺路线表单槽位/.test(relation.text)
          ),
          true,
          'assist_switch_filler_should_preserve_form_task_user_relationships'
        )
      }

      results.push({
        menu: item.menu,
        label: item.label,
        heading,
        optionCount,
        emptyVisible,
        errorText,
        fullscreenActive,
        fillerRelations
      })

      await dialog.getByRole('button', { name: '取消' }).click()
      await dialog.waitFor({ state: 'hidden', timeout: 10000 })
    }
  } finally {
    page.off('response', onResponse)
    await Promise.allSettled(responseReads)
    evidence.apiErrorsDuringAssistSwitch = responseErrors
  }

  assert.equal(page.url(), startUrl, 'assist_switch_dialogs_should_not_navigate')
  evidence.assistSwitchDialogs = results
  return results
}

async function pageHasInputValue(page, expectedValue) {
  return page.evaluate((value) => {
    const controls = Array.from(document.querySelectorAll('input, textarea'))
    return controls.some((control) => control.value === value)
  }, expectedValue)
}

async function verifyCurrentAssistForm(page, source, evidence) {
  await page.waitForURL((url) => url.pathname === formPath, { timeout: 60000 })
  const currentUrl = new URL(page.url())
  const workTaskId = currentUrl.searchParams.get('workTaskId')
  if (!workTaskId) {
    throw failFast('opened_form_missing_work_task_id', { url: page.url(), source })
  }
  const panel = page.locator('.edhr-fill-workspace__assist-panel').first()
  await panel.waitFor({ state: 'visible', timeout: 60000 })
  await page
    .locator('.edhr-fill-workspace__view-actions button.is-active')
    .filter({ hasText: '填写辅助模式' })
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
  await page.getByRole('button', { name: '原表模式' }).first().waitFor({ state: 'visible', timeout: 30000 })
  const workbench = await collectAssistWorkbenchState(page)
  const helpText = (await page.locator('.edhr-fill-workspace__assist-help').first().innerText().catch(() => '')).trim()
  const missingJump =
    workbench.missingCount > 0
      ? await verifyMissingJump(page).catch((error) => ({ error: error.message, details: error.details }))
      : { missingCount: 0, highlightedFieldId: '', highlightedText: '' }
  const marker = `789${Date.now().toString().slice(-7)}`
  const draftProbe = await findEditableDraftInput(page, marker)
  const attempt = {
    source,
    url: page.url(),
    workTaskId,
    workbench,
    helpText,
    missingJump,
    editable: Boolean(draftProbe)
  }
  evidence.rowAttempts.push(attempt)
  if (
    workbench.hasTopbar &&
    workbench.rowCount > 0 &&
    workbench.legacyCardCount === 0 &&
    workbench.quickSwitches.length >= 3 &&
    helpText &&
    !missingJump.error &&
    draftProbe
  ) {
    return { ...attempt, draftProbe }
  }
  throw failFast('opened_form_not_editable_in_assist_mode', attempt)
}

async function openFillableWorkTask(page, evidence) {
  let summary = await openWorkTaskBoard(page, evidence)
  const maxPages = Math.max(1, Math.ceil((summary?.total || 0) / Math.max(1, summary?.rowCount || 10)))
  for (let pageIndex = 1; pageIndex <= maxPages; pageIndex += 1) {
    const rows = summary?.rows || []
    const candidateIndex = rows.findIndex((row) => row.openableFillTask)
    evidence.workTaskPageAttempts = evidence.workTaskPageAttempts || []
    evidence.workTaskPageAttempts.push({
      pageIndex,
      rowCount: summary?.rowCount || 0,
      openableFillTaskCount: rows.filter((row) => row.openableFillTask).length,
      candidateTaskIds: rows.filter((row) => row.openableFillTask).map((row) => row.id)
    })
    if (candidateIndex >= 0) {
      const task = rows[candidateIndex]
      await page.getByText(task.taskCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
      await page.getByRole('button', { name: '处理' }).first().waitFor({ state: 'visible', timeout: 60000 })
      const processButtons = page.getByRole('button', { name: '处理' })
      const buttonCount = await processButtons.count()
      if (buttonCount <= candidateIndex) {
        throw failFast('work_task_process_button_missing', { pageIndex, candidateIndex, buttonCount, task })
      }
      const button = processButtons.nth(candidateIndex)
      await button.scrollIntoViewIfNeeded()
      const taskOpenPromise = page
        .waitForResponse(
          (response) =>
            response.url().includes('/mes/pro/edhr-batch-execution/task/open') &&
            response.request().method() === 'POST',
          { timeout: 15000 }
        )
        .catch(() => null)
      await button.click()
      const taskOpenResponse = await taskOpenPromise
      let taskOpenPayload = null
      if (taskOpenResponse) {
        assert.ok(taskOpenResponse.ok(), `work_task_open_http_failed:${taskOpenResponse.status()}`)
        taskOpenPayload = await taskOpenResponse.json()
        assert.ok([0, 200].includes(taskOpenPayload?.code), `work_task_open_payload_failed:${JSON.stringify(taskOpenPayload)}`)
      }
      evidence.workTaskOpen = {
        source: 'work-task-board',
        task,
        openApi: taskOpenResponse
          ? {
              httpStatus: taskOpenResponse.status(),
              code: taskOpenPayload?.code,
              executionId: taskOpenPayload?.data?.executionId,
              workTaskId: taskOpenPayload?.data?.workTaskId,
              taskId: taskOpenPayload?.data?.taskId
            }
          : null
      }
      return verifyCurrentAssistForm(page, evidence.workTaskOpen, evidence)
    }
    if (pageIndex >= maxPages) break
    summary = await goToNextWorkTaskPage(page, evidence)
    if (!summary) break
  }
  throw failFast('no_real_work_task_fill_task_found', {
    workTaskPageAttempts: evidence.workTaskPageAttempts,
    workTaskPageResponses: evidence.workTaskPageResponses
  })
}

async function verifyCandidateForm(page, rowIndex, executionLabel, evidence) {
  const link = page.locator('.edhr-list-shell__execution-link').nth(rowIndex)
  if (!(await link.count())) return null
  await link.scrollIntoViewIfNeeded()
  await link.click()
  await page.waitForURL((url) => url.pathname === formPath, { timeout: 60000 })

  const panel = page.locator('.edhr-fill-workspace__assist-panel').first()
  try {
    await panel.waitFor({ state: 'visible', timeout: 60000 })
    await page
      .locator('.edhr-fill-workspace__view-actions button.is-active')
      .filter({ hasText: '填写辅助模式' })
      .first()
      .waitFor({ state: 'visible', timeout: 30000 })
    await page.getByRole('button', { name: '原表模式' }).first().waitFor({ state: 'visible', timeout: 30000 })
    const workbench = await collectAssistWorkbenchState(page)
    const helpText = (await page.locator('.edhr-fill-workspace__assist-help').first().innerText().catch(() => '')).trim()
    const missingJump = await verifyMissingJump(page).catch((error) => ({ error: error.message, details: error.details }))
    const marker = `789${Date.now().toString().slice(-7)}`
    const draftProbe = await findEditableDraftInput(page, marker)
    evidence.rowAttempts.push({
      rowIndex,
      executionLabel,
      workbench,
      helpText,
      missingJump,
      editable: Boolean(draftProbe)
    })
    if (
      workbench.hasTopbar &&
      workbench.rowCount > 0 &&
      workbench.legacyCardCount === 0 &&
      workbench.quickSwitches.length >= 3 &&
      workbench.missingCount > 0 &&
      helpText &&
      !missingJump.error &&
      draftProbe
    ) {
      return { rowIndex, executionLabel, workbench, helpText, missingJump, draftProbe }
    }
  } catch (error) {
    evidence.rowAttempts.push({ rowIndex, executionLabel, error: error.message, url: page.url() })
  }
  await page.goBack({ waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => undefined)
  await page.getByText('eDHR 执行列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 }).catch(() => undefined)
  return null
}

async function tryOpenFillableExecution(page, evidence) {
  let summary = await openList(page, evidence)
  const maxPages = Math.max(1, Math.ceil((summary?.total || 0) / Math.max(1, summary?.rowCount || 10)))

  for (let pageIndex = 1; pageIndex <= maxPages; pageIndex += 1) {
    const totalLinks = await page.locator('.edhr-list-shell__execution-link').count()
    if (totalLinks === 0) {
      throw failFast('no_real_edhr_execution_rows_visible')
    }
    const rows = summary?.rows || []
    const candidateIndexes = rows
      .map((row, index) => ({ row, index }))
      .filter(({ row, index }) => index < totalLinks && row.canOpen !== false && (row.status === 0 || row.preReleaseEditable === true))

    evidence.pageAttempts = evidence.pageAttempts || []
    evidence.pageAttempts.push({
      pageIndex,
      rowCount: rows.length,
      candidateCount: candidateIndexes.length,
      statuses: rows.reduce((acc, row) => {
        const key = String(row.status)
        acc[key] = (acc[key] || 0) + 1
        return acc
      }, {})
    })

    for (const candidate of candidateIndexes.slice(0, config.maxRowsToTry)) {
      const executionLabel = candidate.row.executionCode || `execution-${candidate.row.id}`
      const selected = await verifyCandidateForm(page, candidate.index, executionLabel, evidence)
      if (selected) return selected
    }

    if (pageIndex >= maxPages) break
    summary = await goToNextListPage(page, evidence)
    if (!summary) break
  }
  throw failFast('no_real_fillable_edhr_execution_found', { pageAttempts: evidence.pageAttempts, rowAttempts: evidence.rowAttempts })
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  ensureConfig()
  const evidence = {
    status: 'RUNNING',
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    frontendRoot,
    projectRoot,
    tenant: config.tenant,
    username: config.username,
    officialPreflight: '',
    rowAttempts: [],
    listResponses: [],
    batchListResponses: [],
    workTaskPageResponses: [],
    writeRequestsDuringModeSwitch: []
  }
  let frontendProcess = null
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })

  try {
    frontendProcess = await startFrontendIfRequested(evidence)
    evidence.officialPreflight = runOfficialLoginPreflight()
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    const selected = await openFillableWorkTask(page, evidence)

    let captureWrites = true
    page.on('request', (request) => {
      if (captureWrites && isMesWriteRequest(request)) {
        evidence.writeRequestsDuringModeSwitch.push({
          method: request.method(),
          url: request.url()
        })
      }
    })

    evidence.assistFullscreen = await enterAssistWorkspaceFullscreen(page)
    await verifyAssistSwitchDialogs(page, evidence)

    await page.getByRole('button', { name: '原表模式' }).first().click()
    await page.locator('.edhr-fill-workspace__assist-panel').waitFor({ state: 'hidden', timeout: 30000 })
    evidence.originalModeAfterSwitch = {
      markerVisibleInOriginalMode: await pageHasInputValue(page, selected.draftProbe.marker)
    }

    await page.getByRole('button', { name: '填写辅助模式' }).first().click()
    await page.locator('.edhr-fill-workspace__assist-panel').first().waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(
      await pageHasInputValue(page, selected.draftProbe.marker),
      true,
      'draft_value_should_be_visible_in_assist_mode_after_switch_back'
    )
    captureWrites = false

    assert.deepEqual(
      evidence.writeRequestsDuringModeSwitch,
      [],
      `mode_switch_should_not_send_mes_write_requests:${JSON.stringify(evidence.writeRequestsDuringModeSwitch)}`
    )

    await page.screenshot({ path: screenshotFile, fullPage: true })
    evidence.status = 'PASS'
    evidence.selected = selected
    evidence.screenshot = screenshotFile
    fs.writeFileSync(resultFile, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.log(JSON.stringify(evidence, null, 2))
  } catch (error) {
    evidence.status = error.message.startsWith('no_real_') ? 'BLOCKED' : 'FAIL'
    evidence.error = {
      message: error.message,
      details: error.details,
      stack: error.stack
    }
    fs.writeFileSync(resultFile, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.error(JSON.stringify(evidence, null, 2))
    process.exitCode = 1
  } finally {
    await browser.close()
    if (frontendProcess?.child && !frontendProcess.child.killed) {
      frontendProcess.child.kill()
    }
    if (frontendProcess?.logStream) {
      frontendProcess.logStream.end()
    }
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
