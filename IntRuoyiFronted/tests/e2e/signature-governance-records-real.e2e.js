const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'signature-governance-records')
const BASE_URL = (process.env.SIGNATURE_GOVERNANCE_RECORDS_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.SIGNATURE_GOVERNANCE_RECORDS_E2E_TENANT || '测试租户'
const USERNAME = process.env.SIGNATURE_GOVERNANCE_RECORDS_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.SIGNATURE_GOVERNANCE_RECORDS_E2E_PASSWORD
const TARGET_PATH = '/signature-governance/signature-records'
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'Yudao', 'YUDAO'])

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(path.join(RESULT_DIR, 'real-e2e-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function requirePrerequisites() {
  if (!PASSWORD) {
    const result = {
      status: 'BLOCKED',
      reason: 'missing-password',
      impact: 'Cannot run real login without the current test tenant password.'
    }
    writeResult(result)
    throw new Error(result.impact)
  }
  if (FORBIDDEN_TENANTS.has(TENANT)) {
    const result = {
      status: 'BLOCKED',
      reason: 'forbidden-tenant',
      tenant: TENANT,
      impact: 'Real E2E must not target a protected tenant.'
    }
    writeResult(result)
    throw new Error(result.impact)
  }
}

async function login(page) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', TARGET_PATH)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function main() {
  requirePrerequisites()
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)

    const pageResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/signature-governance/signature-records/page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    const pageResponse = await pageResponsePromise
    assert.equal(pageResponse.ok(), true, `signature records HTTP status ${pageResponse.status()}`)
    const pagePayload = await pageResponse.json()
    assert.ok([0, 200].includes(pagePayload.code), `signature records business code ${pagePayload.code}`)

    const table = page.locator('[data-user-table-key="signature.governance.records"]').first()
    await table.waitFor({ state: 'visible' })
    await page.getByText('来源', { exact: false }).first().waitFor({ state: 'visible' })

    const bodyText = await page.locator('.signature-governance').first().innerText()
    assert.equal(bodyText.includes('文件签名记录'), false, 'legacy file signature pane title must not render')
    assert.equal(bodyText.includes('批记录签名记录'), false, 'legacy batch signature pane title must not render')

    const sidebarText = await page
      .locator('aside, nav, .el-menu')
      .evaluateAll((nodes) =>
        nodes
          .filter((node) => {
            const style = window.getComputedStyle(node)
            const rect = node.getBoundingClientRect()
            return style.visibility !== 'hidden' && style.display !== 'none' && rect.width > 0 && rect.height > 0
          })
          .map((node) => node.innerText || '')
          .join('\n')
      )
    assert.equal(sidebarText.includes('签名记录'), true, 'unified signature records menu must render')
    assert.equal(sidebarText.includes('文件签名记录'), false, 'legacy file signature menu must not render')
    assert.equal(sidebarText.includes('批记录签名记录'), false, 'legacy batch signature menu must not render')
    const signatureRecordsSubMenuCount = await page
      .locator('.el-sub-menu__title')
      .filter({ hasText: /^签名记录$/ })
      .count()
    assert.equal(
      signatureRecordsSubMenuCount,
      0,
      'signature records is a leaf menu and must not render the submenu arrow'
    )

    await page.goto(`${BASE_URL}/signature-governance/authorizations`, { waitUntil: 'domcontentloaded' })
    await page
      .locator('[data-user-table-key="dcc.electronicSignature.authorizations"]')
      .first()
      .waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(
      await page.locator('.signature-image-panel').count(),
      0,
      'authorization page must not render the standalone signature image intro panel'
    )
    const imageActions = page.locator('[data-testid="dcc-signature-image-toolbar-actions"]').first()
    await imageActions.waitFor({ state: 'visible', timeout: 60000 })
    for (const label of ['上传图片', '启用图片', '停用图片']) {
      await imageActions.getByRole('button', { name: label }).waitFor({ state: 'visible' })
      assert.ok(label.length <= 4, `signature image toolbar label must be at most 4 chars: ${label}`)
    }

    const rows = pagePayload.data?.list || []
    const sourceLabels = [...new Set(rows.map((row) => row.sourceLabel).filter(Boolean))]
    const invalidSources = sourceLabels.filter((label) => !['文件', '批记录', '展厅'].includes(label))
    assert.deepEqual(invalidSources, [], `unexpected source labels: ${invalidSources.join(',')}`)

    const result =
      rows.length === 0
        ? {
            status: 'DATA_GAP',
            baseUrl: BASE_URL,
            tenant: TENANT,
            username: USERNAME,
            targetPath: TARGET_PATH,
            rowCount: 0,
            impact: 'The unified signature records page rendered, but the test tenant has no real signature records to prove source labels.'
          }
        : {
            status: 'PASS',
            baseUrl: BASE_URL,
            tenant: TENANT,
            username: USERNAME,
            targetPath: TARGET_PATH,
            rowCount: rows.length,
            sourceLabels
          }
    writeResult(result)
    console.log(`signature governance records real e2e ${result.status}: rows=${result.rowCount}, sources=${sourceLabels.join(',') || '<none>'}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  writeResult({
    status: 'FAIL',
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    targetPath: TARGET_PATH,
    error: error.message
  })
  console.error(error)
  process.exit(1)
})
