const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const taskId = '20260721-site-message-read-time-real-e2e'
const evidenceDir = path.resolve(workspaceRoot, 'doc/tasks', taskId, 'evidence')

const config = {
  baseUrl: process.env.NOTIFY_MESSAGE_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.NOTIFY_MESSAGE_E2E_TENANT || '测试租户',
  username: process.env.NOTIFY_MESSAGE_E2E_USERNAME || 'aoteman',
  password: process.env.NOTIFY_MESSAGE_E2E_PASSWORD || '111111',
  timeout: Number(process.env.NOTIFY_MESSAGE_E2E_TIMEOUT || 60000)
}

const normalizeText = (value) => String(value || '').replace(/\s+/g, ' ').trim()

const isSuccessPayload = (payload) => payload && [0, 200].includes(payload.code)

const assertSuccessPayload = (payload, label) => {
  assert.ok(isSuccessPayload(payload), `${label} failed: ${JSON.stringify(payload)}`)
}

const parseJson = async (response) => {
  try {
    return await response.json()
  } catch (error) {
    throw new Error(`response is not json: ${response.status()} ${response.url()} ${error.message}`)
  }
}

const fillFirstVisible = async (locator, value, label) => {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.fill(value)
      return
    }
  }
  throw new Error(`visible input not found: ${label}`)
}

const login = async (page) => {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.click()
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: config.timeout })
  await tenantOption.click()

  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(
    form.locator('input[type="password"], input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await parseJson(loginResponse)
  assertSuccessPayload(loginPayload, 'login')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: config.timeout })
}

const waitForNotifyTable = async (page) => {
  const table = page.locator('.my-notify-message-list__table:visible').first()
  await table.waitFor({ state: 'visible', timeout: config.timeout })
  await page.waitForFunction(
    () => {
      const root = document.querySelector('.my-notify-message-list')
      if (!root) return false
      const visibleMasks = Array.from(root.querySelectorAll('.el-loading-mask')).filter((mask) => {
        const style = window.getComputedStyle(mask)
        return style.display !== 'none' && style.visibility !== 'hidden' && mask.getClientRects().length > 0
      })
      return visibleMasks.length === 0
    },
    null,
    { timeout: config.timeout }
  )
  return table
}

const snapshotTable = async (table) => {
  return table.evaluate((tableElement) => {
    const headers = Array.from(tableElement.querySelectorAll('.el-table__header-wrapper thead th')).map((header) =>
      header.innerText.replace(/\s+/g, ' ').trim()
    )
    const rows = Array.from(tableElement.querySelectorAll('.el-table__body-wrapper tbody tr'))
      .filter((row) => row.getClientRects().length > 0)
      .map((row, rowIndex) => ({
        rowIndex,
        cells: Array.from(row.querySelectorAll('td')).map((cell) =>
          cell.innerText.replace(/\s+/g, ' ').trim()
        ),
        text: row.innerText.replace(/\s+/g, ' ').trim()
      }))
    return { headers, rows }
  })
}

const getColumnIndex = (snapshot, label) => {
  const index = snapshot.headers.findIndex((header) => header.includes(label))
  assert.notEqual(index, -1, `table column not found: ${label}; headers=${snapshot.headers.join('|')}`)
  return index
}

const isDateTimeText = (value) => /\d{4}[-/]\d{1,2}[-/]\d{1,2}\s+\d{1,2}:\d{2}/.test(normalizeText(value))

const findRowByKey = (snapshot, indices, key) => {
  return snapshot.rows.find(
    (row) =>
      row.cells[indices.sender] === key.sender &&
      row.cells[indices.createTime] === key.createTime &&
      row.cells[indices.content] === key.content
  )
}

const waitForMyPageResponse = (page) =>
  page.waitForResponse(
    (response) =>
      response.url().includes('/system/notify-message/my-page') && response.request().method() === 'GET',
    { timeout: config.timeout }
  )

const applyReadStatusFilter = async (page, label) => {
  const quickFilter = page.locator('.my-notify-message-list .table-quick-filter:visible').first()
  await quickFilter.waitFor({ state: 'visible', timeout: config.timeout })
  const valueSelect = quickFilter.locator('.table-quick-filter__value').first()
  await valueSelect.click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: label }).first()
  await option.waitFor({ state: 'visible', timeout: config.timeout })
  const responsePromise = waitForMyPageResponse(page)
  await option.click()
  await responsePromise
  await waitForNotifyTable(page)
}

const findRowAcrossVisiblePages = async (page, table, indices, rowKey) => {
  for (let pageIndex = 1; pageIndex <= 10; pageIndex += 1) {
    const snapshot = await snapshotTable(table)
    const row = findRowByKey(snapshot, indices, rowKey)
    if (row) {
      return { row, snapshot, pageIndex }
    }
    const nextButton = page.locator('.my-notify-message-list .el-pagination button.btn-next').last()
    const nextButtonCount = await nextButton.count()
    if (!nextButtonCount) break
    const disabled = await nextButton.evaluate((button) => button.disabled || button.classList.contains('is-disabled'))
    if (disabled) break
    const responsePromise = waitForMyPageResponse(page)
    await nextButton.click()
    await responsePromise
    await waitForNotifyTable(page)
  }
  return null
}

const main = async () => {
  assert.equal(config.tenant, '测试租户', `real write-capable E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `real write-capable E2E must use aoteman, got ${config.username}`)
  fs.mkdirSync(evidenceDir, { recursive: true })

  const launchOptions = {
    headless: !process.env.NOTIFY_MESSAGE_E2E_HEADED,
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }

  const browser = await chromium.launch(launchOptions)
  const evidence = {
    taskId,
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    startedAt: new Date().toISOString(),
    writeRequestsAfterReadClick: []
  }

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(config.timeout)
    page.setDefaultNavigationTimeout(config.timeout)

    let captureActionWrites = false
    page.on('request', (request) => {
      if (!captureActionWrites) return
      if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) return
      const url = new URL(request.url())
      evidence.writeRequestsAfterReadClick.push({
        method: request.method(),
        path: `${url.pathname}${url.search}`
      })
    })

    await login(page)
    await page.goto(new URL('/user/profile?tab=notifyMessage', config.baseUrl).toString(), {
      waitUntil: 'domcontentloaded',
      timeout: config.timeout
    })
    await page.getByText('我的站内信', { exact: true }).first().waitFor({ state: 'visible' })

    const table = await waitForNotifyTable(page)
    const beforeSnapshot = await snapshotTable(table)
    assert.ok(beforeSnapshot.rows.length > 0, 'no station messages visible in 我的站内信 list')

    const indices = {
      sender: getColumnIndex(beforeSnapshot, '发送人'),
      createTime: getColumnIndex(beforeSnapshot, '发送时间'),
      content: getColumnIndex(beforeSnapshot, '消息内容'),
      readStatus: getColumnIndex(beforeSnapshot, '是否已读'),
      readTime: getColumnIndex(beforeSnapshot, '阅读时间'),
      operation: getColumnIndex(beforeSnapshot, '操作')
    }
    const candidate = beforeSnapshot.rows.find((row) => row.cells[indices.operation] === '阅读')
    assert.ok(
      candidate,
      `no unread station message with 阅读 action on current page; rows=${JSON.stringify(beforeSnapshot.rows)}`
    )
    assert.equal(candidate.cells[indices.readTime], '', `unread row readTime should start empty: ${candidate.text}`)

    const rowKey = {
      sender: candidate.cells[indices.sender],
      createTime: candidate.cells[indices.createTime],
      content: candidate.cells[indices.content]
    }
    evidence.beforeRow = {
      rowIndex: candidate.rowIndex,
      sender: rowKey.sender,
      createTime: rowKey.createTime,
      content: rowKey.content,
      readStatus: candidate.cells[indices.readStatus],
      readTime: candidate.cells[indices.readTime],
      operation: candidate.cells[indices.operation]
    }

    const rowLocator = table.locator('.el-table__body-wrapper tbody tr:visible').nth(candidate.rowIndex)
    const readButton = rowLocator.getByRole('button', { name: /^阅读$/ })
    await readButton.waitFor({ state: 'visible', timeout: config.timeout })
    const buttonClass = await readButton.evaluate((button) => button.className)
    assert.match(String(buttonClass), /\bel-button--success\b/, '阅读 button must be green success type')

    const updateResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/system/notify-message/update-read') && response.request().method() === 'PUT',
      { timeout: config.timeout }
    )
    const refreshResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/system/notify-message/my-page') && response.request().method() === 'GET',
      { timeout: config.timeout }
    )

    captureActionWrites = true
    await readButton.click()
    const updateResponse = await updateResponsePromise
    const updatePayload = await parseJson(updateResponse)
    assertSuccessPayload(updatePayload, 'update-read')
    const refreshResponse = await refreshResponsePromise
    const refreshPayload = await parseJson(refreshResponse)
    assertSuccessPayload(refreshPayload, 'my-page after update-read')
    captureActionWrites = false

    const detailDialog = page.locator('.el-dialog:visible').filter({ hasText: '消息详情' }).first()
    await detailDialog.waitFor({ state: 'visible', timeout: config.timeout })
    const detailReadTime = normalizeText(
      await detailDialog
        .locator('.notify-message-detail__meta-item')
        .filter({ hasText: '阅读时间' })
        .locator('strong')
        .first()
        .innerText()
    )
    assert.ok(isDateTimeText(detailReadTime), `detail readTime should be date-time text, got "${detailReadTime}"`)
    await page.keyboard.press('Escape')
    try {
      await detailDialog.waitFor({ state: 'hidden', timeout: 5000 })
    } catch (_error) {
      await detailDialog.locator('.el-dialog__header .is-hover.cursor-pointer').last().click()
      await detailDialog.waitFor({ state: 'hidden', timeout: config.timeout })
    }

    await applyReadStatusFilter(page, '是')
    const afterSearch = await findRowAcrossVisiblePages(page, table, indices, rowKey)
    assert.ok(afterSearch, `read row not found in read-status list by key: ${JSON.stringify(rowKey)}`)
    const afterRow = afterSearch.row
    const afterReadTime = afterRow.cells[indices.readTime]
    const afterReadStatus = afterRow.cells[indices.readStatus]
    const afterOperation = afterRow.cells[indices.operation]
    assert.ok(isDateTimeText(afterReadTime), `list readTime should be date-time text, got "${afterReadTime}"`)
    assert.doesNotMatch(afterReadStatus, /否|未读|false/i, `read status should not remain unread: ${afterReadStatus}`)
    assert.equal(afterOperation, '详情', `operation should become 详情 after reading, got ${afterOperation}`)

    const unexpectedWrites = evidence.writeRequestsAfterReadClick.filter(
      (request) =>
        !(request.method === 'PUT' && request.path.includes('/admin-api/system/notify-message/update-read'))
    )
    assert.deepEqual(unexpectedWrites, [], `unexpected write requests: ${JSON.stringify(unexpectedWrites)}`)

    evidence.updateRead = {
      status: updateResponse.status(),
      payload: updatePayload
    }
    evidence.refreshAfterRead = {
      status: refreshResponse.status(),
      total: refreshPayload?.data?.total ?? refreshPayload?.total,
      pageSize: refreshPayload?.data?.list?.length ?? refreshPayload?.list?.length
    }
    evidence.detailReadTime = detailReadTime
    evidence.afterRow = {
      rowIndex: afterRow.rowIndex,
      readStatusPageIndex: afterSearch.pageIndex,
      readStatus: afterReadStatus,
      readTime: afterReadTime,
      operation: afterOperation
    }
    evidence.finishedAt = new Date().toISOString()
    evidence.result = 'PASS'

    await page.screenshot({
      path: path.resolve(evidenceDir, 'notify-message-read-time-after.png'),
      fullPage: true
    })
    fs.writeFileSync(
      path.resolve(evidenceDir, 'notify-message-read-time-real-e2e.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    console.log('PASS: notify message read time real E2E')
  } catch (error) {
    evidence.finishedAt = new Date().toISOString()
    evidence.result = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    try {
      const browserPages = await browser.contexts().at(-1)?.pages()
      const activePage = browserPages?.at(-1)
      if (activePage) {
        await activePage.screenshot({
          path: path.resolve(evidenceDir, 'notify-message-read-time-failure.png'),
          fullPage: true
        })
      }
    } catch (screenshotError) {
      evidence.screenshotError = screenshotError && screenshotError.message ? screenshotError.message : String(screenshotError)
    }
    fs.writeFileSync(
      path.resolve(evidenceDir, 'notify-message-read-time-real-e2e.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
