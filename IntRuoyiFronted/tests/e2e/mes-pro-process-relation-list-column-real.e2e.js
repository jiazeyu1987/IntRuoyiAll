const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_PRO_PROCESS_RELATION_LIST_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_PRO_PROCESS_RELATION_LIST_TENANT || '测试租户',
  username: process.env.MES_PRO_PROCESS_RELATION_LIST_USERNAME || 'aoteman',
  password: process.env.MES_PRO_PROCESS_RELATION_LIST_PASSWORD || '111111',
  headed: process.env.MES_PRO_PROCESS_RELATION_LIST_HEADED === '1',
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  outDir: path.resolve('tests/output/mes-pro-process-relation-list-column-real')
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `工序设置关系清单真实 E2E 必须使用 测试租户/aoteman，当前为 ${config.tenant}/${config.username}`
  )
}

fs.mkdirSync(config.outDir, { recursive: true })

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantSelect = form.locator('.el-select').first()
  const selectedTenantText = await tenantSelect.innerText().catch(() => '')
  if (!selectedTenantText.includes(config.tenant)) {
    const tenantInput = form
      .locator('.el-select input[role="combobox"]:visible, input.el-select__input:visible')
      .first()
    if (await tenantInput.count()) {
      await tenantInput.click()
      await tenantInput.fill(config.tenant)
      await page.keyboard.press('Enter')
      await page.waitForTimeout(300)
      const tenantOption = page
        .locator('.el-select-dropdown__item:visible')
        .filter({ hasText: config.tenant })
        .first()
      if (await tenantOption.isVisible().catch(() => false)) {
        await tenantOption.click()
      }
    } else {
      await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
    }
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP 失败: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `登录业务失败: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function readProcessTableSnapshot(page) {
  return page.locator('.process-main-table').evaluate((table) => {
    const normalizeText = (value) => String(value || '').replace(/\s+/g, ' ').trim()
    const headerCells = Array.from(
      table.querySelectorAll('.el-table__header-wrapper thead tr:last-child th .cell')
    ).map((cell) => normalizeText(cell.textContent))
    const relationIndex = headerCells.indexOf('关系清单')
    const rows = Array.from(table.querySelectorAll('.el-table__body-wrapper tbody tr.el-table__row'))
      .map((row) => {
        const cells = Array.from(row.querySelectorAll('td .cell')).map((cell) =>
          normalizeText(cell.textContent)
        )
        return {
          cells,
          relationListText: relationIndex >= 0 ? cells[relationIndex] || '' : ''
        }
      })
      .filter((row) => row.cells.length > 0)
    return {
      headerCells,
      relationIndex,
      rowCount: rows.filter((row) => row.relationListText).length,
      relationListTexts: rows.map((row) => row.relationListText).filter(Boolean)
    }
  })
}

async function waitForRelationListTable(page) {
  await page.waitForFunction(() => {
    const table = document.querySelector('.process-main-table')
    if (!table) return false
    const normalizeText = (value) => String(value || '').replace(/\s+/g, ' ').trim()
    const headers = Array.from(
      table.querySelectorAll('.el-table__header-wrapper thead tr:last-child th .cell')
    ).map((cell) => normalizeText(cell.textContent))
    const relationIndex = headers.indexOf('关系清单')
    if (relationIndex < 0) return false
    const rows = Array.from(table.querySelectorAll('.el-table__body-wrapper tbody tr.el-table__row'))
    if (rows.length === 0) return false
    const relationTexts = rows
      .map((row) => {
        const cells = Array.from(row.querySelectorAll('td .cell')).map((cell) =>
          normalizeText(cell.textContent)
        )
        return cells[relationIndex] || ''
      })
      .filter(Boolean)
    return relationTexts.length > 0 && relationTexts.every((text) => text !== '加载中')
  }, null, { timeout: 60000 })
  return readProcessTableSnapshot(page)
}

async function openProcessSettingsList(page) {
  const processPageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/process/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/process`, { waitUntil: 'commit', timeout: 60000 })
  await page.getByText('工序编码', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  const processPageResponse = await processPageResponsePromise
  const processPagePayload = await processPageResponse.json()
  assert.equal(
    processPagePayload.code,
    0,
    `工序设置分页接口失败: ${JSON.stringify(processPagePayload)}`
  )
  return processPagePayload
}

;(async () => {
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: fs.existsSync(config.executablePath) ? config.executablePath : undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  const pageErrors = []
  const mesWriteRequests = []
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))
  page.on('request', (request) => {
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) &&
      request.url().includes('/mes/')
    ) {
      mesWriteRequests.push({ method: request.method(), url: request.url() })
    }
  })

  try {
    await login(page)
    const processPagePayload = await openProcessSettingsList(page)
    const snapshot = await waitForRelationListTable(page)
    assert.ok(snapshot.headerCells.includes('关系清单'), `表头缺少关系清单: ${JSON.stringify(snapshot.headerCells)}`)
    assert.ok(snapshot.rowCount > 0, '工序设置列表没有真实行，无法验证关系清单列')
    assert.ok(
      snapshot.relationListTexts.length > 0,
      `关系清单列没有任何单元格文本: ${JSON.stringify(snapshot)}`
    )
    assert.ok(
      snapshot.relationListTexts.every((text) => text !== '加载中'),
      `关系清单仍停留在加载中: ${JSON.stringify(snapshot.relationListTexts)}`
    )
    assert.ok(
      snapshot.relationListTexts.every((text) => !text.includes('关系加载失败')),
      `关系清单加载失败: ${JSON.stringify(snapshot.relationListTexts)}`
    )
    assert.ok(
      snapshot.relationListTexts.some((text) => text.includes('->') || text.includes('暂无关系')),
      `关系清单必须显示关系摘要或暂无关系: ${JSON.stringify(snapshot.relationListTexts)}`
    )
    assert.deepEqual(pageErrors, [], `页面错误: ${JSON.stringify(pageErrors)}`)
    assert.deepEqual(mesWriteRequests, [], `只读验证不得产生 MES 写请求: ${JSON.stringify(mesWriteRequests)}`)

    const result = {
      tenant: config.tenant,
      username: config.username,
      targetPath: '/mes/pro/process',
      processPageTotal: processPagePayload.data?.total,
      headerTexts: snapshot.headerCells,
      relationListText: snapshot.relationListTexts[0],
      relationListTexts: snapshot.relationListTexts.slice(0, 5),
      pageErrors,
      mesWriteRequests
    }
    fs.writeFileSync(
      path.join(config.outDir, 'result.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )
    await page.screenshot({
      path: path.join(config.outDir, 'process-relation-list-column.png'),
      fullPage: true
    })
    console.log(`PASS: ${JSON.stringify(result)}`)
  } catch (error) {
    await page.screenshot({
      path: path.join(config.outDir, 'failure.png'),
      fullPage: true
    }).catch(() => null)
    fs.writeFileSync(
      path.join(config.outDir, 'failure.json'),
      `${JSON.stringify(
        {
          url: page.url(),
          tenant: config.tenant,
          username: config.username,
          pageErrors,
          mesWriteRequests,
          message: error.message,
          stack: error.stack
        },
        null,
        2
      )}\n`,
      'utf8'
    )
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
})().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
