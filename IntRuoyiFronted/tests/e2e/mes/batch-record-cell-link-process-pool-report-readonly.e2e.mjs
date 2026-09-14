import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_REPORT_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  taskDir:
    process.env.BATCH_RECORD_CELL_LINK_REPORT_TASK_DIR ||
    path.join(repoRoot, 'doc', 'tasks', '20260811-process-pool-report-cell-link-config', 'e2e-artifacts'),
  timeout: Number(process.env.BATCH_RECORD_CELL_LINK_REPORT_TIMEOUT || 90000),
  headed: process.env.BATCH_RECORD_CELL_LINK_REPORT_HEADED === '1'
}

function readLoginDefaults() {
  const envPath = path.join(frontendRoot, '.env')
  assert.ok(fs.existsSync(envPath), `frontend .env missing: ${envPath}`)
  const entries = Object.fromEntries(
    fs
      .readFileSync(envPath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#') && line.includes('='))
      .map((line) => {
        const [key, ...rest] = line.split('=')
        return [key.trim(), rest.join('=').trim().replace(/^['"]|['"]$/g, '')]
      })
  )
  const credentials = {
    tenant: entries.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: entries.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: entries.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
  assert.ok(credentials.tenant && credentials.username && credentials.password, 'default login values are incomplete')
  return credentials
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function isWorkbenchContextResponse(response) {
  return (
    response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
    response.request().method() === 'GET'
  )
}

async function selectFirstRealDropdownOption(page, description) {
  const deadline = Date.now() + config.timeout
  while (Date.now() < deadline) {
    const options = page.locator('.el-select-dropdown__item:visible')
    const labels = (await options.allInnerTexts()).map((label) => label.trim())
    const optionIndex = labels.findIndex((label) => label && label !== 'No data')
    if (optionIndex >= 0) {
      return {
        label: labels[optionIndex],
        option: options.nth(optionIndex)
      }
    }
    await page.waitForTimeout(250)
  }
  throw new Error(`${description} has no selectable real-data option`)
}

async function selectAndReadWorkbenchContext(page, option, description) {
  const contextPromise = page.waitForResponse(isWorkbenchContextResponse, { timeout: config.timeout })
  await option.click()
  const response = await contextPromise
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && isSuccessPayload(payload), `${description} context failed: ${payload?.msg || response.status()}`)
  return payload.data
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page, credentials) {
  const targetPath = '/mes/pro/batch-record-form-list'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(credentials.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: credentials.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await form.locator('input[placeholder="请输入租户名称"]').first().fill(credentials.tenant)
  }
  await form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
    .fill(credentials.username)
  await form.locator('input[type="password"]:visible').first().fill(credentials.password)

  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && isSuccessPayload(payload), `login failed: ${payload?.msg || response.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
}

async function selectRoughWashReport(page) {
  const roughWashReportName = '粗洗工序生产记录'
  const row = page
    .locator('.batch-record-form-layout__list .el-table__body-wrapper tr')
    .filter({ hasText: roughWashReportName })
    .first()
  await row.waitFor({ state: 'visible', timeout: config.timeout })
  await row.click()
  await page.waitForFunction(
    (reportName) => document.querySelector('.batch-record-form-preview__title')?.textContent?.includes(reportName),
    roughWashReportName,
    { timeout: config.timeout }
  )
  await settle(page, 30000)
  return roughWashReportName
}

async function openWorkbench(page) {
  await page.goto(`${config.baseUrl}/mes/pro/batch-record-form-list`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.getByText('批记录表单').first().waitFor({ state: 'visible', timeout: config.timeout })
  await settle(page, 30000)
  const clickedReportName = await selectRoughWashReport(page)

  const contextPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  const linkButton = page.locator('.batch-record-form-preview__actions button').filter({ hasText: /^链接$/ }).first()
  await linkButton.waitFor({ state: 'visible', timeout: config.timeout })
  await linkButton.click()
  await page.getByText('批记录单元格链接').first().waitFor({ state: 'visible', timeout: config.timeout })
  const contextPayload = await (await contextPromise).json()
  assert.ok(isSuccessPayload(contextPayload), `workbench context failed: ${contextPayload?.msg || contextPayload?.code}`)
  await settle(page, 30000)
  return {
    context: contextPayload.data,
    clickedReportName
  }
}

async function readSelectedSelectText(page, selector) {
  return page.locator(selector).first().evaluate((root) => {
    const selectedItem =
      root.querySelector('.el-select__selected-item') ||
      root.querySelector('.el-select__placeholder') ||
      root.querySelector('.el-select__selection')
    const selectedText = selectedItem?.textContent?.trim()
    if (selectedText) return selectedText
    const input = root.querySelector('input')
    return input?.value?.trim() || root.textContent?.trim() || ''
  })
}

async function verifyClickedReportIsTarget(page, context, clickedReportName) {
  const targetReportId = new URL(page.url()).searchParams.get('targetReportId')
  assert.ok(targetReportId, `clicked report target id missing from URL: ${page.url()}`)
  const targetForm = (context.forms || []).find((form) => form.reportId === targetReportId)
  assert.ok(targetForm, `clicked target form missing from workbench context: ${targetReportId}`)
  assert.equal(targetForm.reportName, clickedReportName, 'clicked report must be resolved as the target form')

  const sourceText = await readSelectedSelectText(page, '.batch-record-cell-link__source-select')
  const targetText = await readSelectedSelectText(page, '.batch-record-cell-link__target-select')
  assert.ok(targetText.includes(clickedReportName), `target selector must show clicked report: ${targetText}`)
  assert.ok(!sourceText.includes(clickedReportName), `source selector must not show clicked report: ${sourceText}`)
  return {
    clickedTargetReportName: clickedReportName,
    initialSourceSelection: sourceText,
    initialTargetSelection: targetText
  }
}

async function verifyIndependentPanelScrolling(page) {
  const sourceScroll = page.locator('[data-cell-link-scroll-pane="source"]').first()
  const targetScroll = page.locator('[data-cell-link-scroll-pane="target"]').first()
  await sourceScroll.waitFor({ state: 'visible', timeout: config.timeout })
  await targetScroll.waitFor({ state: 'visible', timeout: config.timeout })

  await sourceScroll.evaluate((element) => {
    element.scrollTop = 0
  })
  await targetScroll.evaluate((element) => {
    element.scrollTop = 0
  })
  await page.evaluate(() => window.scrollTo(0, 0))

  const before = await page.evaluate(() => {
    const source = document.querySelector('[data-cell-link-scroll-pane="source"]')
    const target = document.querySelector('[data-cell-link-scroll-pane="target"]')
    if (!source || !target) return null
    const sourceRect = source.getBoundingClientRect()
    const targetTable = target.querySelector('table')
    return {
      sourceScrollTop: source.scrollTop,
      targetScrollTop: target.scrollTop,
      targetScrollHeight: target.scrollHeight,
      targetClientHeight: target.clientHeight,
      targetTableHeight: targetTable?.getBoundingClientRect().height || 0,
      targetTableRows: targetTable?.rows.length || 0,
      sourceTop: sourceRect.top,
      sourceBottom: sourceRect.bottom,
      windowScrollY: window.scrollY
    }
  })
  assert.ok(before, 'source and target scroll containers must be rendered')
  assert.ok(
    before.targetScrollHeight > before.targetClientHeight,
    `target form must overflow its scroll container: ${JSON.stringify(before)}`
  )

  const targetBox = await targetScroll.boundingBox()
  assert.ok(targetBox, 'target scroll container must expose a bounding box')
  await page.mouse.move(
    targetBox.x + Math.max(8, targetBox.width - 12),
    targetBox.y + Math.min(targetBox.height - 8, Math.max(24, Math.floor(targetBox.height / 2)))
  )
  await page.waitForTimeout(100)
  const wheelDelta = Math.max(600, Math.floor(before.targetClientHeight * 0.8))
  await page.mouse.wheel(0, wheelDelta)
  await page.waitForTimeout(100)
  if ((await targetScroll.evaluate((element) => element.scrollTop)) <= before.targetScrollTop) {
    await page.mouse.move(
      targetBox.x + Math.max(8, targetBox.width - 12),
      targetBox.y + Math.min(targetBox.height - 8, Math.max(24, Math.floor(targetBox.height / 2)))
    )
    await page.waitForTimeout(100)
    await page.mouse.wheel(0, wheelDelta)
    await page.waitForTimeout(150)
  }

  const after = await page.evaluate(() => {
    const source = document.querySelector('[data-cell-link-scroll-pane="source"]')
    const target = document.querySelector('[data-cell-link-scroll-pane="target"]')
    if (!source || !target) return null
    const sourceRect = source.getBoundingClientRect()
    return {
      sourceScrollTop: source.scrollTop,
      targetScrollTop: target.scrollTop,
      sourceTop: sourceRect.top,
      sourceBottom: sourceRect.bottom,
      windowScrollY: window.scrollY
    }
  })
  assert.ok(after, 'source and target scroll containers must remain rendered after wheel input')
  assert.ok(after.targetScrollTop > before.targetScrollTop, `target form did not scroll: ${JSON.stringify({ before, after })}`)
  assert.equal(after.sourceScrollTop, before.sourceScrollTop, 'source field scroll position must remain unchanged')
  assert.ok(Math.abs(after.sourceTop - before.sourceTop) < 1, 'source field panel top must remain fixed')
  assert.ok(Math.abs(after.sourceBottom - before.sourceBottom) < 1, 'source field panel bottom must remain fixed')
  assert.equal(after.windowScrollY, before.windowScrollY, 'page scroll position must remain unchanged')

  return {
    targetScrollTopBefore: before.targetScrollTop,
    targetScrollTopAfter: after.targetScrollTop,
    sourceScrollTopBefore: before.sourceScrollTop,
    sourceScrollTopAfter: after.sourceScrollTop,
    windowScrollYBefore: before.windowScrollY,
    windowScrollYAfter: after.windowScrollY
  }
}

async function verifyProcessPoolReportSource(page, context) {
  const expectedFields = [
    ['outputQuantity', '本次报工产出数量'],
    ['lossQuantity', '本次报工损耗数量'],
    ['signatureUserId', '提交签名用户'],
    ['reviewedAt', '审核时间'],
    ['reviewSignatureUserId', '审核人签名用户']
  ]
  const hiddenFields = [
    ['allocatedQuantity', '放行分配数量'],
    ['lossReasonCodeSnapshot', '损耗原因编码'],
    ['actualEmployeeId', '实际操作员工'],
    ['laborScrapQuantity', '本次报工工废数量'],
    ['materialScrapQuantity', '本次报工料废数量'],
    ['otherScrapQuantity', '本次报工其他废品数量'],
    ['lossReasonNameSnapshot', '损耗原因名称'],
    ['deviceId', '事件设备编号'],
    ['workstationId', '工作站编号'],
    ['deviceAccountId', '设备账号'],
    ['signatureId', '提交签名编号'],
    ['reviewSignatureId', '审核人签名编号']
  ]
  const sourceSelect = page.locator('.batch-record-cell-link__source-select').first()
  await sourceSelect.click()
  const reportOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: /^报工数据$/ }).first()
  await reportOption.waitFor({ state: 'visible', timeout: 30000 })
  await reportOption.click()
  await settle(page)

  const dccSelect = page.locator('[data-process-pool-dcc-project-select]').first()
  await dccSelect.waitFor({ state: 'visible', timeout: config.timeout })
  await dccSelect.click()
  const dccSelection = await selectFirstRealDropdownOption(page, 'DCC project code selector')
  const dccContext = await selectAndReadWorkbenchContext(page, dccSelection.option, 'DCC project code selection')
  assert.ok(dccSelection.label, 'selected DCC project code must have a visible label')
  await settle(page)

  const processSelect = page.locator('[data-process-pool-route-process-select]').first()
  await processSelect.waitFor({ state: 'visible', timeout: config.timeout })
  await page.waitForFunction(
    () => {
      const select = document.querySelector('[data-process-pool-route-process-select]')
      return select && select.getAttribute('aria-disabled') !== 'true' && !select.classList.contains('is-disabled')
    },
    null,
    { timeout: config.timeout }
  )
  await processSelect.click()
  const processSelection = await selectFirstRealDropdownOption(page, 'route process selector')
  const selectedContext = await selectAndReadWorkbenchContext(page, processSelection.option, 'route process selection')
  assert.ok(processSelection.label && processSelection.label !== 'No data', 'selected route process must have a real label')
  await settle(page)

  const reportFields = (selectedContext.sourceFields || []).filter((field) => field.sourceType === 'PROCESS_POOL_REPORT')
  for (const [fieldCode, fieldName] of expectedFields) {
    assert.ok(
      reportFields.some((field) => field.fieldCode === fieldCode && field.fieldName === fieldName),
      `PROCESS_POOL_REPORT source field missing after DCC project/process selection: ${fieldCode}/${fieldName}`
    )
  }
  for (const [fieldCode, fieldName] of hiddenFields) {
    assert.ok(
      !reportFields.some((field) => field.fieldCode === fieldCode && field.fieldName === fieldName),
      `PROCESS_POOL_REPORT source field must be hidden after DCC project/process selection: ${fieldCode}/${fieldName}`
    )
  }

  await page.waitForFunction(
    () => Number(document.querySelector('[data-process-pool-report-source-fields="true"]')?.getAttribute('data-process-pool-report-field-count') || 0) > 0,
    null,
    { timeout: config.timeout }
  )

  const sourcePanel = page.locator('.batch-record-cell-link__work-order-field-panel').first()
  await sourcePanel.waitFor({ state: 'visible', timeout: config.timeout })
  const panelText = (await sourcePanel.innerText()).replace(/\s+/g, ' ')
  const targetContext = selectedContext || dccContext || context
  const targetForm = (targetContext.forms || []).find((form) => form.reportId === targetContext.defaultTargetReportId)
  assert.ok(targetForm, `default target form missing: ${JSON.stringify(targetContext)}`)
  assert.match(panelText, /源字段/, 'source panel must identify source fields')
  assert.ok(
    panelText.includes(`${processSelection.label}的一线生产字段`),
    `source panel must identify the selected route process: ${panelText}`
  )
  for (const [, fieldName] of expectedFields) {
    assert.ok(panelText.includes(fieldName), `report data field is not visible: ${fieldName}`)
  }
  for (const [, fieldName] of hiddenFields) {
    assert.ok(!panelText.includes(fieldName), `hidden report data field is visible: ${fieldName}`)
  }

  const aggregationSelect = page.locator('.batch-record-cell-link__aggregation-select').first()
  await aggregationSelect.waitFor({ state: 'visible', timeout: 30000 })
  const createButton = page.locator('.batch-record-cell-link__create-button').first()
  assert.equal(await createButton.isDisabled(), true, 'create button must remain disabled before target and aggregation')

  const targetCell = page
    .locator('.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable')
    .first()
  await targetCell.waitFor({ state: 'visible', timeout: 30000 })
  const independentPanelScroll = await verifyIndependentPanelScrolling(page)
  await targetCell.click()
  assert.equal(await createButton.isDisabled(), true, 'create button must require an aggregation strategy')

  await aggregationSelect.click()
  const visibleOptions = page.locator('.el-select-dropdown__item:visible')
  await visibleOptions.filter({ hasText: /^求和$/ }).first().waitFor({ state: 'visible', timeout: 30000 })
  const aggregationLabels = await visibleOptions.allInnerTexts()
  for (const label of ['求和', '第一笔', '最后一笔', '最小值', '最大值']) {
    assert.ok(
      aggregationLabels.some((item) => item.trim() === label),
      `number aggregation option missing: ${label}; visible=${JSON.stringify(aggregationLabels)}`
    )
  }
  await visibleOptions.filter({ hasText: /^求和$/ }).first().click()
  await settle(page)
  assert.equal(await createButton.isEnabled(), true, 'create button must be enabled after source, target and aggregation')

  return {
    forms: targetContext.forms?.length || 0,
    selectedDccProjectCode: dccSelection.label,
    selectedRouteProcess: processSelection.label,
    reportFields: reportFields.map(({ fieldCode, fieldName, valueType }) => ({ fieldCode, fieldName, valueType })),
    aggregationLabels: aggregationLabels.map((label) => label.trim()).filter(Boolean),
    independentPanelScroll
  }
}

async function main() {
  assert.ok(Number.isFinite(config.timeout) && config.timeout > 0, 'timeout must be positive')
  fs.mkdirSync(config.taskDir, { recursive: true })
  const credentials = readLoginDefaults()
  assert.equal(credentials.tenant, '芋道源码', `readonly tenant must be 芋道源码, got ${credentials.tenant}`)
  assert.equal(credentials.username, 'admin', `readonly username must be admin, got ${credentials.username}`)

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const page = await browser.newPage({ viewport: { width: 1680, height: 500 }, locale: 'zh-CN' })
  const mesWriteRequests = []
  const pageErrors = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD'].includes(request.method())) {
      mesWriteRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  const screenshot = path.join(config.taskDir, 'process-pool-report-readonly-passed.png')
  const failedScreenshot = path.join(config.taskDir, 'process-pool-report-readonly-failed.png')
  try {
    await login(page, credentials)
    const { context, clickedReportName } = await openWorkbench(page)
    const directionEvidence = await verifyClickedReportIsTarget(page, context, clickedReportName)
    const evidence = await verifyProcessPoolReportSource(page, context)
    assert.equal(mesWriteRequests.length, 0, `readonly E2E sent MES writes: ${mesWriteRequests.join(', ')}`)
    assert.deepEqual(pageErrors, [], `page errors detected: ${pageErrors.join(' | ')}`)
    await page.screenshot({ path: screenshot, fullPage: true })
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          identity: `${credentials.tenant}/${credentials.username}`,
          ...directionEvidence,
          ...evidence,
          mesWriteRequests: mesWriteRequests.length,
          pageErrors,
          screenshot
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: failedScreenshot, fullPage: true }).catch(() => null)
    throw new Error(`${error.message}; screenshot=${failedScreenshot}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
