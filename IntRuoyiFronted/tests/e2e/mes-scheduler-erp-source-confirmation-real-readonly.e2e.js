const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const artifactDir = path.resolve(
  repoRoot,
  'output/playwright/scheduler-seven-issues-erp-source-confirmation'
)
const baseUrl = (process.env.MES_SCHEDULER_ERP_CONFIRM_BASE_URL || 'http://127.0.0.1:8081').replace(
  /\/+$/,
  ''
)
const tenant =
  process.env.MES_SCHEDULER_ERP_CONFIRM_TENANT ||
  readEnvDefault('VITE_APP_DEFAULT_LOGIN_TENANT') ||
  '芋道源码'
const username =
  process.env.MES_SCHEDULER_ERP_CONFIRM_USERNAME ||
  readEnvDefault('VITE_APP_DEFAULT_LOGIN_USERNAME') ||
  'admin'
const password =
  process.env.MES_SCHEDULER_ERP_CONFIRM_PASSWORD || readEnvDefault('VITE_APP_DEFAULT_LOGIN_PASSWORD')
const chromePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

const applyPath = '/admin-api/mes/pro/auto-schedule/replan/apply'
const warningReasonCode = 'WARN_ERP_SYNC_RECORD_MISSING'
const warningText =
  '排产范围内存在缺少 ERP 正式同步记录或正式 ID/编号的工单。确认后才会应用正式排程；请先确认这些工单来源可信。'

function readEnvDefault(key) {
  for (const envFileName of ['.env.local', '.env']) {
    const envPath = path.join(frontendRoot, envFileName)
    if (!fs.existsSync(envPath)) continue
    const source = fs.readFileSync(envPath, 'utf8')
    const pattern = new RegExp(`^\\s*${key}\\s*=\\s*(.*)\\s*$`)
    for (const line of source.split(/\r?\n/)) {
      const match = line.match(pattern)
      if (match) return match[1].trim().replace(/^['"]|['"]$/g, '')
    }
  }
  return ''
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function clickFirstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.click()
      return item
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return item
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 120000
  })
  await settle(page)
  if (!page.url().includes('/login')) return
  if (
    (await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实页面受控验证。')
  }

  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible().catch(() => false))) {
    await tenantSelect.click()
    const selectInput = page.locator('.login-form .el-select__input').first()
    await selectInput.fill(tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), username, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 120000 }
    ),
    clickFirstVisible(page.locator('.login-form .el-button--primary'), 'login button')
  ])
  const loginPayload = await loginResponse.json()
  assert.equal(loginPayload.code, 0, `登录接口返回业务错误: ${loginPayload.msg || loginPayload.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 120000 })
  await settle(page)
}

async function openScheduleOrderPage(page) {
  const responsePromise = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        url.pathname === '/admin-api/mes/pro/schedule-order/page' &&
        response.request().method() === 'GET' &&
        response.status() === 200
      )
    },
    { timeout: 180000 }
  )
  await page.goto(`${baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 120000
  })
  const response = await responsePromise
  const payload = await response.json()
  assert.equal(response.status(), 200, `排产工单列表接口 HTTP ${response.status()}`)
  assert.equal(payload.code, 0, `排产工单列表接口业务错误: ${payload.msg || payload.code}`)
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 30000 })
  return payload.data?.list?.length ?? 0
}

async function closeReplanDrawer(page) {
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '排产前检查 / 手动重排' }).first()
  if (!(await drawer.isVisible().catch(() => false))) return
  const closeButton = drawer.locator('.el-drawer__close-btn').first()
  await closeButton.click()
  await drawer.waitFor({ state: 'hidden', timeout: 30000 })
}

async function inspectSelectedRowPreflight(page) {
  await clickFirstVisible(page.getByRole('button', { name: /手动重排/ }), 'manual replan button')
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '排产前检查 / 手动重排' }).first()
  await drawer.waitFor({ state: 'visible', timeout: 120000 })
  const preflightPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/preflight') &&
      response.status() === 200,
    { timeout: 180000 }
  )
  await drawer.getByRole('button', { name: /重新检查/ }).click()
  const response = await preflightPromise
  const payload = await response.json()
  assert.equal(payload.code, 0, `排产前检查接口业务错误: ${payload.msg || payload.code}`)
  const issues = payload.data?.issues || []
  return {
    drawer,
    requestBody: response.request().postDataJSON(),
    result: payload.data?.result,
    issues,
    hasErpWarning: issues.some((issue) => issue.reasonCode === warningReasonCode),
    hasGlobalBlocker: issues.some((issue) => issue.severity === 'BLOCKED' && !issue.scheduleOrderId)
  }
}

async function findErpWarningRow(page, inspectedRows) {
  const rows = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr')
  const rowCount = await rows.count()
  for (let index = 0; index < rowCount; index += 1) {
    const row = rows.nth(index)
    if (!(await row.isVisible().catch(() => false))) continue
    const text = (await row.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
    if (!text || text.includes('暂无数据')) continue
    const checkbox = row.locator('.el-checkbox').first()
    if (!(await checkbox.isVisible().catch(() => false))) continue
    const disabledByClass = ((await checkbox.getAttribute('class').catch(() => '')) || '').includes(
      'is-disabled'
    )
    const disabledInput = await checkbox.locator('input').first().isDisabled().catch(() => false)
    if (disabledByClass || disabledInput) continue

    await checkbox.click()
    const preflight = await inspectSelectedRowPreflight(page)
    inspectedRows.push({
      index,
      rowText: text,
      requestBody: preflight.requestBody,
      result: preflight.result,
      reasonCodes: preflight.issues.map((issue) => issue.reasonCode),
      hasErpWarning: preflight.hasErpWarning,
      hasGlobalBlocker: preflight.hasGlobalBlocker
    })
    if (preflight.hasErpWarning && !preflight.hasGlobalBlocker) {
      return { rowText: text, preflight }
    }
    await closeReplanDrawer(page)
    await checkbox.click()
  }
  throw new Error(
    `当前可见排产工单中没有“缺 ERP 同步证据”且无全局阻断的可验证样本: ${JSON.stringify(inspectedRows)}`
  )
}

async function openStartDateDialog(drawer) {
  const startButton = drawer.getByRole('button', { name: /开始重排/ }).first()
  await startButton.waitFor({ state: 'visible', timeout: 120000 })
  assert.equal(await startButton.isDisabled(), false, '开始重排按钮必须可用。')
  await startButton.click()
  const page = drawer.page()
  const dateDialog = page.locator('.el-dialog:visible').filter({ hasText: '开始重排日期' }).last()
  await dateDialog.waitFor({ state: 'visible', timeout: 120000 })
  return dateDialog
}

async function clickDateConfirmAndWaitForErpWarning(page, dateDialog) {
  const preflightPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/preflight') &&
      response.status() === 200,
    { timeout: 180000 }
  )
  await dateDialog.getByRole('button', { name: /确认应用重排/ }).click()
  const preflightResponse = await preflightPromise
  const preflightPayload = await preflightResponse.json()
  assert.equal(preflightPayload.code, 0, '应用前排产检查必须成功。')
  assert.ok(
    preflightPayload.data?.issues?.some((issue) => issue.reasonCode === warningReasonCode),
    '应用前排产检查必须返回缺 ERP 正式来源警告。'
  )
  const warningDialog = page.locator('.el-message-box:visible').filter({ hasText: warningText }).last()
  await warningDialog.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForTimeout(350)
  assert.ok((await warningDialog.innerText()).includes(warningText), '二次确认必须完整说明 ERP 来源风险。')
  return { warningDialog, preflightRequestBody: preflightResponse.request().postDataJSON() }
}

async function collectDialogLayerEvidence(page) {
  return page.evaluate(({ expectedWarningText }) => {
    const isVisible = (element) => {
      if (!element) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    }
    const dateDialog = [...document.querySelectorAll('.el-dialog')].find(
      (element) => isVisible(element) && (element.textContent || '').includes('开始重排日期')
    )
    const warningDialog = [...document.querySelectorAll('.el-message-box')].find(
      (element) => isVisible(element) && (element.textContent || '').includes(expectedWarningText)
    )
    const dateOverlay = dateDialog?.closest('.el-overlay')
    const warningOverlay = warningDialog?.closest('.el-overlay')
    const warningRect = warningDialog?.getBoundingClientRect()
    const centerX = warningRect ? warningRect.left + warningRect.width / 2 : 0
    const centerY = warningRect ? warningRect.top + warningRect.height / 2 : 0
    const centerTopElement = warningRect ? document.elementFromPoint(centerX, centerY) : null
    return {
      dateOverlayZIndex: dateOverlay ? Number(window.getComputedStyle(dateOverlay).zIndex) : null,
      warningOverlayZIndex: warningOverlay ? Number(window.getComputedStyle(warningOverlay).zIndex) : null,
      warningReceivesPointer: Boolean(
        warningDialog && centerTopElement && warningDialog.contains(centerTopElement)
      ),
      warningCenterTopElement: centerTopElement
        ? `${centerTopElement.tagName}.${centerTopElement.className}`
        : null
    }
  }, { expectedWarningText: warningText })
}

async function main() {
  assert.ok(password, '默认本机登录密码缺失，无法执行真实页面受控验证。')
  fs.mkdirSync(artifactDir, { recursive: true })
  const resultPath = path.join(artifactDir, 'result.json')
  const warningScreenshotPath = path.join(artifactDir, 'erp-source-second-confirm.png')
  const interceptedApplyRequests = []
  const forwardedApplyRequests = []
  const inspectedRows = []
  const browser = await chromium.launch({
    headless: process.env.MES_SCHEDULER_ERP_CONFIRM_HEADED !== '1',
    executablePath: fs.existsSync(chromePath) ? chromePath : undefined
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  const result = {
    status: 'UNKNOWN',
    baseUrl,
    tenantUser: `${tenant}/${username}`,
    readOnlyScope:
      '真实页面触发排产前检查与重排预览；正式 apply 请求在浏览器网络层捕获并中止，未到达后端。',
    inspectedRows,
    interceptedApplyRequests,
    forwardedApplyRequests,
    artifactDir,
    warningScreenshotPath
  }

  await page.route(`**${applyPath}`, async (route) => {
    const request = route.request()
    interceptedApplyRequests.push({
      method: request.method(),
      path: new URL(request.url()).pathname,
      body: request.postDataJSON()
    })
    await route.abort('blockedbyclient')
  })
  page.on('requestfinished', (request) => {
    if (new URL(request.url()).pathname === applyPath) {
      forwardedApplyRequests.push({ method: request.method(), path: applyPath })
    }
  })

  try {
    await login(page)
    result.initialListCount = await openScheduleOrderPage(page)
    const target = await findErpWarningRow(page, inspectedRows)
    result.selectedRowText = target.rowText
    result.initialPreflight = {
      requestBody: target.preflight.requestBody,
      result: target.preflight.result,
      reasonCodes: target.preflight.issues.map((issue) => issue.reasonCode)
    }

    const dateDialog = await openStartDateDialog(target.preflight.drawer)
    const firstAttempt = await clickDateConfirmAndWaitForErpWarning(page, dateDialog)
    result.firstAttempt = {
      warningText: await firstAttempt.warningDialog.innerText(),
      preflightRequestBody: firstAttempt.preflightRequestBody,
      applyRequestCountBeforeDecision: interceptedApplyRequests.length,
      layerEvidence: await collectDialogLayerEvidence(page)
    }
    assert.equal(
      interceptedApplyRequests.length,
      0,
      '用户尚未确认 ERP 来源风险时不得生成正式 apply 请求。'
    )
    assert.ok(
      result.firstAttempt.layerEvidence.warningOverlayZIndex >
        result.firstAttempt.layerEvidence.dateOverlayZIndex,
      'ERP 来源二次确认层级必须高于开始重排日期窗口。'
    )
    assert.equal(
      result.firstAttempt.layerEvidence.warningReceivesPointer,
      true,
      'ERP 来源二次确认内容区域必须接收用户点击。'
    )
    await page.screenshot({ path: warningScreenshotPath, fullPage: true })
    await firstAttempt.warningDialog.getByRole('button', { name: /取消/ }).click()
    await firstAttempt.warningDialog.waitFor({ state: 'hidden', timeout: 30000 })
    await page.waitForTimeout(500)
    assert.equal(interceptedApplyRequests.length, 0, '取消 ERP 来源风险确认后不得生成正式 apply 请求。')

    const confirmedAttempt = await clickDateConfirmAndWaitForErpWarning(page, dateDialog)
    const applyRequestPromise = page.waitForRequest(
      (request) => new URL(request.url()).pathname === applyPath,
      { timeout: 180000 }
    )
    await confirmedAttempt.warningDialog.getByRole('button', { name: /确定/ }).click()
    const applyRequest = await applyRequestPromise
    const applyBody = applyRequest.postDataJSON()
    assert.equal(applyBody.erpSourceRiskConfirmed, true, '确认风险后的正式 apply 请求必须显式携带确认字段。')
    await page.waitForTimeout(500)
    assert.equal(interceptedApplyRequests.length, 1, '受控验证只允许生成一次被拦截的正式 apply 请求。')
    assert.equal(forwardedApplyRequests.length, 0, '受控验证不得让正式 apply 请求到达后端。')
    result.confirmedAttempt = {
      warningText: await confirmedAttempt.warningDialog.innerText().catch(() => warningText),
      preflightRequestBody: confirmedAttempt.preflightRequestBody,
      applyRequestBody: applyBody,
      browserInterception: 'aborted-before-backend'
    }
    result.status = 'PASS'
  } catch (error) {
    result.status = 'FAIL'
    result.error = error && error.stack ? error.stack : String(error)
    await page.screenshot({ path: path.join(artifactDir, 'failure.png'), fullPage: true }).catch(
      () => undefined
    )
    throw error
  } finally {
    fs.writeFileSync(resultPath, JSON.stringify(result, null, 2), 'utf8')
    await browser.close()
    console.log(
      JSON.stringify(
        {
          status: result.status,
          tenantUser: result.tenantUser,
          selectedRowPresent: Boolean(result.selectedRowText),
          inspectedRowCount: inspectedRows.length,
          firstAttemptApplyRequestCount: result.firstAttempt?.applyRequestCountBeforeDecision,
          confirmedErpSourceRisk: result.confirmedAttempt?.applyRequestBody?.erpSourceRiskConfirmed,
          interceptedApplyRequestCount: interceptedApplyRequests.length,
          forwardedApplyRequestCount: forwardedApplyRequests.length,
          resultPath,
          warningScreenshotPath
        },
        null,
        2
      )
    )
  }
}

main().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
