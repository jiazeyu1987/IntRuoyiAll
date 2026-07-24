const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const taskDir = path.join(repoRoot, 'doc', 'tasks', '20260720-edhr-assist-fill-mode-field-recognition')
const artifactDir = path.join(taskDir, 'e2e-artifacts')
const resultFile = path.join(artifactDir, 'assist-fill-mode-admin-readonly-e2e-result.json')
const screenshotFile = path.join(artifactDir, 'assist-fill-mode-admin-readonly-e2e.png')
const failureScreenshotFile = path.join(artifactDir, 'assist-fill-mode-admin-readonly-e2e-failure.png')
const formFailureScreenshotFile = path.join(artifactDir, 'assist-fill-mode-admin-readonly-form-failure.png')

const config = {
  baseUrl: process.env.EDHR_ASSIST_FILL_ADMIN_BASE_URL || 'http://localhost:8081',
  tenant: process.env.EDHR_ASSIST_FILL_ADMIN_TENANT || '芋道源码',
  username: process.env.EDHR_ASSIST_FILL_ADMIN_USERNAME || 'admin',
  password: process.env.EDHR_ASSIST_FILL_ADMIN_PASSWORD || '',
  headed: process.env.EDHR_ASSIST_FILL_ADMIN_HEADED === '1',
  maxRowsToTry: Number(process.env.EDHR_ASSIST_FILL_ADMIN_MAX_ROWS || 4),
  maxPagesToScan: Number(process.env.EDHR_ASSIST_FILL_ADMIN_MAX_PAGES || 20)
}

const listPath = '/mes/pro/feedback/edhr-batch-execution'
const formPath = '/mes/pro/feedback/edhr-execution/form'

function failFast(message, details = {}) {
  const error = new Error(message)
  error.details = details
  return error
}

function ensureConfig() {
  const blockers = []
  if (config.baseUrl !== 'http://localhost:8081') {
    blockers.push(`baseUrl must be http://localhost:8081, got ${config.baseUrl}`)
  }
  if (config.tenant !== '芋道源码' || config.username !== 'admin') {
    blockers.push(`admin readonly E2E must use 芋道源码/admin, got ${config.tenant}/${config.username}`)
  }
  if (!config.password) {
    blockers.push('EDHR_ASSIST_FILL_ADMIN_PASSWORD is required')
  }
  if (blockers.length) {
    throw failFast('edhr_assist_fill_admin_readonly_precondition_failed', { blockers })
  }
}

function runOfficialLoginPreflight() {
  const scriptPath = path.join(repoRoot, 'scripts', 'preflight', 'login-preflight.mjs')
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
      listPath,
      '--target-text',
      '执行列表'
    ],
    {
      cwd: repoRoot,
      env: process.env,
      encoding: 'utf8',
      timeout: 120000
    }
  )
  if (result.status !== 0) {
    throw failFast('official_admin_login_preflight_failed', {
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
  return waitForExecutionListResponse(
    page,
    async () => {
      await page.goto(listUrl.toString(), { waitUntil: 'domcontentloaded' })
      await page.getByText('执行列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
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

function isWriteMethod(method) {
  return !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())
}

function redactUrl(rawUrl) {
  try {
    const url = new URL(rawUrl)
    for (const key of Array.from(url.searchParams.keys())) {
      if (/token|password|secret|key/i.test(key)) {
        url.searchParams.set(key, '[REDACTED]')
      }
    }
    return url.toString()
  } catch (error) {
    return rawUrl
  }
}

function isMesWriteRequest(request) {
  const url = request.url()
  return isWriteMethod(request.method()) && (url.includes('/admin-api/mes/') || url.includes('/mes/pro/'))
}

function isAssistModeCandidateRow(row) {
  const status = String(row.status ?? '').toUpperCase()
  return (
    row.canOpen !== false &&
    (
      row.preReleaseEditable === true ||
      status === '0' ||
      status === 'DRAFT' ||
      status === 'FILLING' ||
      status === 'IN_PROGRESS'
    )
  )
}

async function pageHasAssistPanel(page) {
  const panel = page.locator('.edhr-fill-workspace__assist-panel').first()
  return (await panel.count()) > 0 && (await panel.isVisible().catch(() => false))
}

async function collectAssistEvidence(page) {
  const labels = await page.locator('.edhr-fill-workspace__assist-label').evaluateAll((nodes) =>
    nodes.slice(0, 5).map((node) => node.textContent.trim()).filter(Boolean)
  )
  const helpTexts = await page.locator('.edhr-fill-workspace__assist-help').evaluateAll((nodes) =>
    nodes.slice(0, 5).map((node) => node.textContent.trim()).filter(Boolean)
  )
  const configuredHelpTextCount = await page.locator('.edhr-fill-workspace__assist-help').evaluateAll((nodes) =>
    nodes.map((node) => node.textContent.trim()).filter((text) => text && text !== '字段说明未配置').length
  )
  return {
    assistCardCount: await page.locator('.edhr-fill-workspace__assist-card').count(),
    labels,
    helpTexts,
    configuredHelpTextCount,
    saveButtonVisible: await page.getByRole('button', { name: '保存' }).first().isVisible().catch(() => false),
    submitButtonVisible: await page.getByRole('button', { name: '提交执行' }).first().isVisible().catch(() => false)
  }
}

async function collectFormFailureEvidence(page) {
  const visibleAlerts = await page.locator('.el-alert:visible').evaluateAll((nodes) =>
    nodes.slice(0, 8).map((node) => node.textContent.replace(/\s+/g, ' ').trim()).filter(Boolean)
  ).catch(() => [])
  const bodyText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
  await page.screenshot({ path: formFailureScreenshotFile, fullPage: true }).catch(() => undefined)
  return {
    url: page.url(),
    visibleAlerts,
    pageTextSample: bodyText.replace(/\s+/g, ' ').trim().slice(0, 1200),
    formFailureScreenshot: fs.existsSync(formFailureScreenshotFile) ? formFailureScreenshotFile : undefined
  }
}

async function verifyReadonlyForm(page, rowIndex, executionLabel, evidence) {
  const link = page.locator('.edhr-list-shell__execution-link').nth(rowIndex)
  if (!(await link.count())) return null
  await link.scrollIntoViewIfNeeded()
  await link.click()
  await page.waitForURL((url) => url.pathname === formPath, { timeout: 60000 })

  try {
    await page.locator('.edhr-fill-workspace__assist-panel').first().waitFor({ state: 'visible', timeout: 15000 })
    await page
      .locator('.edhr-fill-workspace__view-actions button.is-active')
      .filter({ hasText: '填写辅助模式' })
      .first()
      .waitFor({ state: 'visible', timeout: 30000 })
    await page.getByRole('button', { name: '原表模式' }).first().waitFor({ state: 'visible', timeout: 30000 })
    const assistEvidence = await collectAssistEvidence(page)
    evidence.rowAttempts.push({ rowIndex, executionLabel, ...assistEvidence })
    if (assistEvidence.assistCardCount <= 0) {
      throw failFast('admin_form_has_no_assist_fields', { rowIndex, executionLabel, assistEvidence })
    }
    if (assistEvidence.helpTexts.length <= 0) {
      throw failFast('admin_form_has_no_assist_help_text_area', { rowIndex, executionLabel, assistEvidence })
    }
    if (assistEvidence.saveButtonVisible || assistEvidence.submitButtonVisible) {
      throw failFast('admin_readonly_form_shows_write_actions', { rowIndex, executionLabel, assistEvidence })
    }

    await page.getByRole('button', { name: '原表模式' }).first().click()
    await page.locator('.edhr-fill-workspace__assist-panel').waitFor({ state: 'hidden', timeout: 30000 })
    assert.equal(await pageHasAssistPanel(page), false, 'assist_panel_should_hide_in_original_mode')

    await page.getByRole('button', { name: '填写辅助模式' }).first().click()
    await page.locator('.edhr-fill-workspace__assist-panel').first().waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(await pageHasAssistPanel(page), true, 'assist_panel_should_show_after_switch_back')

    return { rowIndex, executionLabel, ...assistEvidence }
  } catch (error) {
    const failureEvidence = await collectFormFailureEvidence(page)
    evidence.rowAttempts.push({ rowIndex, executionLabel, error: error.message, ...failureEvidence })
  }
  await page.goBack({ waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => undefined)
  await page.getByText('执行列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 }).catch(() => undefined)
  return null
}

async function tryOpenReadableExecution(page, evidence) {
  let summary = await openList(page, evidence)
  const inferredMaxPages = Math.max(1, Math.ceil((summary?.total || 0) / Math.max(1, summary?.rowCount || 10)))
  const maxPages = Math.min(config.maxPagesToScan, inferredMaxPages)

  for (let pageIndex = 1; pageIndex <= maxPages; pageIndex += 1) {
    const totalLinks = await page.locator('.edhr-list-shell__execution-link').count()
    if (totalLinks === 0) {
      throw failFast('no_admin_edhr_execution_rows_visible', { summary })
    }
    const rows = summary?.rows || []
    const candidateIndexes = rows.length
      ? rows
        .map((row, index) => ({ row, index }))
        .filter(({ row, index }) => index < totalLinks && isAssistModeCandidateRow(row))
      : Array.from({ length: totalLinks }, (_, index) => ({ row: {}, index }))

    evidence.pageAttempts.push({
      pageIndex,
      rowCount: rows.length,
      visibleLinkCount: totalLinks,
      candidateCount: candidateIndexes.length,
      statuses: rows.reduce((acc, row) => {
        const key = String(row.status)
        acc[key] = (acc[key] || 0) + 1
        return acc
      }, {})
    })

    for (const candidate of candidateIndexes.slice(0, config.maxRowsToTry)) {
      const executionLabel = candidate.row.executionCode || `execution-row-${candidate.index}`
      const selected = await verifyReadonlyForm(page, candidate.index, executionLabel, evidence)
      if (selected) return selected
    }

    if (pageIndex >= maxPages) break
    summary = await goToNextListPage(page, evidence)
    if (!summary) break
  }
  throw failFast('no_admin_readable_edhr_form_with_assist_mode_found', {
    pageAttempts: evidence.pageAttempts,
    rowAttempts: evidence.rowAttempts
  })
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  ensureConfig()
  const officialPreflight = runOfficialLoginPreflight()
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const evidence = {
    status: 'RUNNING',
    readonly: true,
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    officialPreflight,
    pageAttempts: [],
    rowAttempts: [],
    listResponses: [],
    mesWriteRequests: []
  }
  let page

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('request', (request) => {
      if (isMesWriteRequest(request)) {
        evidence.mesWriteRequests.push({
          method: request.method(),
          url: redactUrl(request.url())
        })
      }
    })

    await login(page)
    const selected = await tryOpenReadableExecution(page, evidence)

    assert.deepEqual(
      evidence.mesWriteRequests,
      [],
      `admin_readonly_e2e_should_not_send_mes_write_requests:${JSON.stringify(evidence.mesWriteRequests)}`
    )

    await page.screenshot({ path: screenshotFile, fullPage: true })
    evidence.status = 'PASS'
    evidence.selected = selected
    evidence.screenshot = screenshotFile
    fs.writeFileSync(resultFile, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.log(JSON.stringify(evidence, null, 2))
  } catch (error) {
    if (page) {
      await page.screenshot({ path: failureScreenshotFile, fullPage: true }).catch(() => undefined)
    }
    evidence.status = error.message.startsWith('no_admin_') || error.message.startsWith('admin_form_has_no_')
      ? 'BLOCKED'
      : 'FAIL'
    evidence.error = {
      message: error.message,
      details: error.details,
      stack: error.stack
    }
    evidence.failureScreenshot = fs.existsSync(failureScreenshotFile) ? failureScreenshotFile : undefined
    fs.writeFileSync(resultFile, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.error(JSON.stringify(evidence, null, 2))
    process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
