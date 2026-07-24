const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const artifactDir = path.resolve(
  __dirname,
  '../output/20260709-schedule-order-process-route-unified-list-real'
)

const config = {
  baseUrl: process.env.E2E_BASE_URL || 'http://localhost:8081',
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: process.env.E2E_HEADED !== '1',
  tenant: '测试租户',
  username: 'aoteman',
  password: '111111'
}

const ensureDir = (dir) => fs.mkdirSync(dir, { recursive: true })

const clickVisibleButton = async (scope, text, message) => {
  const button = scope.locator('button:visible').filter({ hasText: text }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 }).catch((error) => {
    throw new Error(`${message}: ${error.message}`)
  })
  await button.click()
}

const selectTenant = async (page) => {
  const form = page.locator('form.login-form:visible').first()
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    if (await option.count()) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    const textboxes = form.locator('input.el-input__inner')
    await textboxes.nth(0).fill(config.tenant)
  }
}

const login = async (page) => {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded'
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page)
  await form.locator('input.el-input__inner:visible:not([role="combobox"])').first().fill(config.username)
  await form.locator('input[type="password"]:visible').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginPayload.code, 0, `登录失败: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  await page.getByText('排产工单', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
}

const waitForScheduleOrderRows = async (page) => {
  const pageResponse = await page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const payload = await pageResponse.json()
  assert.equal(payload.code, 0, `排产工单列表接口失败: ${JSON.stringify(payload)}`)
  assert.ok(Array.isArray(payload.data?.list), '排产工单列表接口必须返回真实 list。')
  assert.ok(payload.data.list.length > 0, '测试租户排产工单列表没有真实数据，无法打开工艺流程排产配置。')
  await page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  return payload.data.list
}

const closeProcessDialog = async (page, dialog) => {
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

const tryOpenVisibleProcessDialogWithShiftData = async (page) => {
  const rows = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr:visible')
  const rowCount = await rows.count()
  for (let rowIndex = 0; rowIndex < rowCount; rowIndex += 1) {
    const row = rows.nth(rowIndex)
    const rowText = (await row.innerText()).trim()
    const viewButton = row.locator('button:visible').filter({ hasText: '查看' }).first()
    if (!(await viewButton.count())) continue

    const processResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/schedule-order/process-list') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await viewButton.click()
    const processResponse = await processResponsePromise
    const payload = await processResponse.json()
    assert.equal(payload.code, 0, `工艺流程排产配置接口失败: ${JSON.stringify(payload)}`)
    assert.ok(Array.isArray(payload.data), '工艺流程排产配置接口必须返回真实数组。')
    assert.ok(payload.data.length > 0, '工艺流程排产配置没有真实工序数据，无法验证列表列。')

    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '工艺流程排产配置' }).last()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    if (payload.data.some((processRow) => typeof processRow.nightShiftEnabled === 'boolean')) {
      const responseUrl = new URL(processResponse.url())
      return {
        dialog,
        processRows: payload.data,
        rowText,
        scheduleOrderId: responseUrl.searchParams.get('scheduleOrderId')
      }
    }
    await closeProcessDialog(page, dialog)
  }
  return undefined
}

const findScheduleOrderWithShiftData = async (page) => {
  for (let pageNo = 1; pageNo <= 5; pageNo += 1) {
    const target = await tryOpenVisibleProcessDialogWithShiftData(page)
    if (target) return target

    const nextButton = page.locator('.schedule-order-pool .el-pagination button.btn-next:not([disabled])').first()
    if (!(await nextButton.count())) break
    const nextResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/schedule-order/page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await nextButton.click()
    const nextResponse = await nextResponsePromise
    const nextPayload = await nextResponse.json()
    assert.equal(nextPayload.code, 0, `排产工单翻页接口失败: ${JSON.stringify(nextPayload)}`)
    assert.ok(Array.isArray(nextPayload.data?.list), '排产工单翻页接口必须返回真实 list。')
    await page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr').first().waitFor({
      state: 'visible',
      timeout: 60000
    })
  }
  throw new Error('测试租户前 5 页排产工单均缺少 nightShiftEnabled，无法真实验证班次状态。')
}

async function main() {
  ensureDir(artifactDir)
  if (!fs.existsSync(config.executablePath)) {
    throw new Error(`browser executable missing: ${config.executablePath}`)
  }

  const browser = await chromium.launch({
    headless: config.headless,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(30000)
  page.setDefaultNavigationTimeout(60000)

  const writeRequests = []
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())
    ) {
      writeRequests.push({ method: request.method(), url: request.url() })
    }
  })

  try {
    await login(page)
    await waitForScheduleOrderRows(page)
    const target = await findScheduleOrderWithShiftData(page)
    const processRows = target.processRows

    const dialog = target.dialog
    await dialog.waitFor({ state: 'visible', timeout: 30000 })

    await dialog.locator('.unified-list-template[data-table-key="mes.pro.scheduleOrder.processRoute"]').waitFor({
      state: 'visible',
      timeout: 30000
    })
    await dialog.locator('[data-user-table-key="mes.pro.scheduleOrder.processRoute"]').waitFor({
      state: 'visible',
      timeout: 30000
    })

    for (const header of ['工序编号', '工序名称', '班次状态', '预计完成时间']) {
      await dialog.getByText(header, { exact: false }).first().waitFor({
        state: 'visible',
        timeout: 30000
      })
    }
    const processWithShift = processRows.find((row) => typeof row.nightShiftEnabled === 'boolean')
    assert.ok(processWithShift, '接口应返回工序班次状态 nightShiftEnabled')
    const expectedShiftText = processWithShift.nightShiftEnabled ? '夜班' : '白班'
    await dialog.getByText(expectedShiftText, { exact: true }).first().waitFor({
      state: 'visible',
      timeout: 30000
    })
    await dialog.locator('.el-pagination').first().waitFor({ state: 'visible', timeout: 30000 })

    await page.screenshot({
      path: path.join(artifactDir, 'process-route-unified-list.png'),
      fullPage: true
    })

    assert.equal(writeRequests.length, 0, `只读 E2E 不应产生 MES 写请求: ${JSON.stringify(writeRequests)}`)

    fs.writeFileSync(
      path.join(artifactDir, 'result.json'),
      JSON.stringify(
        {
          baseUrl: config.baseUrl,
          tenant: config.tenant,
          username: config.username,
          scheduleOrder: {
            id: target.scheduleOrderId,
            rowText: target.rowText
          },
          processRowCount: processRows.length,
          firstProcess: {
            processCode: processRows[0].processCode,
            processName: processRows[0].processName,
            nightShiftEnabled: processRows[0].nightShiftEnabled,
            plannedEndTime: processRows[0].plannedEndTime
          },
          verifiedShiftRow: {
            processCode: processWithShift.processCode,
            processName: processWithShift.processName,
            nightShiftEnabled: processWithShift.nightShiftEnabled,
            shiftText: expectedShiftText
          },
          verified: [
            'UnifiedListTemplate table key',
            'User table key',
            '班次状态',
            '预计完成时间',
            '分页',
            '无 MES 写请求'
          ]
        },
        null,
        2
      ),
      'utf8'
    )
    console.log('PASS: mes schedule order process route unified list real e2e')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  ensureDir(artifactDir)
  fs.writeFileSync(path.join(artifactDir, 'error.txt'), error.stack || String(error), 'utf8')
  console.error(error)
  process.exit(1)
})
