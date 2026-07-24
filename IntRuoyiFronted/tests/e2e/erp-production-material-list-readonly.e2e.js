const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')

const text = {
  tenant: '测试租户',
  username: 'aoteman',
  password: '111111',
  login: '登录',
  pageTitle: '生产用料清单',
  dialogTitlePrefix: '单据明细 - ',
  mainHeaders: ['单据编号', '子项数量', 'ERP修改时间', '最后同步时间'],
  detailHeaders: [
    '子项物料编码',
    '子项物料名称',
    '规格型号',
    '子项类型',
    '分子',
    '分母',
    '子项单位'
  ]
}

const config = {
  baseUrl: (process.env.ERP_PRODUCTION_MATERIAL_LIST_BASE_URL || 'http://localhost:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.ERP_PRODUCTION_MATERIAL_LIST_TENANT || text.tenant,
  username: process.env.ERP_PRODUCTION_MATERIAL_LIST_USERNAME || text.username,
  password: process.env.ERP_PRODUCTION_MATERIAL_LIST_PASSWORD || text.password,
  headed: process.env.ERP_PRODUCTION_MATERIAL_LIST_HEADED === '1',
  artifactDir: path.resolve(
    process.env.ERP_PRODUCTION_MATERIAL_LIST_ARTIFACT_DIR ||
      path.join(frontendRoot, 'output', 'playwright', 'erp-production-material-list-readonly')
  )
}

const targetPath = '/erp/production/material-list'

function unwrapData(payload) {
  if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')) {
    return payload.data
  }
  return payload
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(800)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    await page.goto(`${config.baseUrl}${targetPath}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  }

  await form.locator('input.el-input__inner').nth(0).fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: text.login }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${config.baseUrl}${targetPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
}

async function waitForApiJson(responsePromise, label) {
  const response = await responsePromise
  const payload = await response.json()
  const code = payload?.code
  assert.ok([0, 200].includes(code), `${label} failed: ${payload?.msg || code}`)
  return payload
}

function collectReadonlyWrites(page) {
  const writes = []
  page.on('request', (request) => {
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) &&
      request.url().includes('/admin-api/erp/production-material-list')
    ) {
      writes.push(`${request.method()} ${request.url()}`)
    }
  })
  return writes
}

async function openGroupPage(page) {
  const groupPageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/erp/production-material-list/group-page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )

  await page.goto(`${config.baseUrl}${targetPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.erp-production-material-list__table').waitFor({
    state: 'visible',
    timeout: 60000
  })

  const payload = await waitForApiJson(groupPageResponsePromise, 'group-page')
  const groupPage = unwrapData(payload)
  const groupList = Array.isArray(groupPage?.list) ? groupPage.list : []

  assert.ok(
    groupList.length > 0,
    `BLOCKER: ${targetPath} 在测试租户未返回任何单据汇总行，无法验证点击单据号弹窗明细。`
  )

  for (const header of text.mainHeaders) {
    await page.getByText(header, { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  }

  const firstRow = groupList[0]
  assert.ok(firstRow?.sourceBillNo, 'BLOCKER: group-page 首行缺少 sourceBillNo，无法继续明细验证。')

  const billLink = page
    .locator('.erp-production-material-list__bill-link')
    .filter({ hasText: firstRow.sourceBillNo })
    .first()
  await billLink.waitFor({ state: 'visible', timeout: 60000 })

  await page.screenshot({
    path: path.join(config.artifactDir, 'group-page.png'),
    fullPage: true
  })

  return firstRow
}

async function openDetailDialog(page, sourceBillNo) {
  const encodedBillNo = encodeURIComponent(sourceBillNo)
  const detailListResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/erp/production-material-list/detail-list') &&
      response.url().includes(`sourceBillNo=${encodedBillNo}`) &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )

  await page
    .locator('.erp-production-material-list__bill-link')
    .filter({ hasText: sourceBillNo })
    .first()
    .click()

  const payload = await waitForApiJson(detailListResponsePromise, 'detail-list')
  const detailList = Array.isArray(unwrapData(payload)) ? unwrapData(payload) : []
  assert.ok(
    detailList.length > 0,
    `BLOCKER: 单据 ${sourceBillNo} 明细接口返回空列表，无法验证 7 列弹窗明细。`
  )

  const dialog = page.locator('.el-dialog').filter({ hasText: `${text.dialogTitlePrefix}${sourceBillNo}` }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })

  for (const header of text.detailHeaders) {
    await dialog.getByText(header, { exact: true }).first().waitFor({
      state: 'visible',
      timeout: 60000
    })
  }

  const detailRows = dialog.locator('.el-table__body tbody tr')
  const detailRowCount = await detailRows.count()
  assert.ok(detailRowCount > 0, `BLOCKER: 单据 ${sourceBillNo} 弹窗未渲染任何明细行。`)

  await page.screenshot({
    path: path.join(config.artifactDir, 'detail-dialog.png'),
    fullPage: true
  })

  return {
    detailCount: detailList.length,
    renderedRowCount: detailRowCount
  }
}

async function main() {
  assert.equal(config.tenant, text.tenant, `只读 E2E 必须使用测试租户，当前为 ${config.tenant}`)
  assert.equal(config.username, text.username, `只读 E2E 必须使用 aoteman，当前为 ${config.username}`)

  fs.mkdirSync(config.artifactDir, { recursive: true })

  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })

  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 960 },
      locale: 'zh-CN'
    })
    const page = await context.newPage()
    const readonlyWrites = collectReadonlyWrites(page)

    await login(page)

    const firstRow = await openGroupPage(page)
    const dialogResult = await openDetailDialog(page, firstRow.sourceBillNo)

    assert.deepEqual(
      readonlyWrites,
      [],
      `只读验证不允许触发 ERP 生产用料清单写请求: ${readonlyWrites.join(', ')}`
    )

    const report = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      route: targetPath,
      tenant: config.tenant,
      username: config.username,
      sourceBillNo: firstRow.sourceBillNo,
      lineCount: firstRow.lineCount,
      detailCount: dialogResult.detailCount,
      renderedRowCount: dialogResult.renderedRowCount,
      mainHeaders: text.mainHeaders,
      detailHeaders: text.detailHeaders
    }

    fs.writeFileSync(
      path.join(config.artifactDir, 'erp-production-material-list-readonly-report.json'),
      `${JSON.stringify(report, null, 2)}\n`,
      'utf8'
    )

    await context.close()
    console.log(JSON.stringify(report, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
