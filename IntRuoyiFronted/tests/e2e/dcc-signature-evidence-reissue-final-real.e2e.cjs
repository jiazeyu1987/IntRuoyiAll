const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(WORKSPACE_ROOT, 'doc', 'tasks', '20260813-dcc-residual-issues-fix')
const PDF_DIR = path.join(WORKSPACE_ROOT, 'output', 'pdf', 'dcc-residual-issues-fix')
const BASE_URL = 'http://127.0.0.1:8081'
const CONTROLLED_FILE_ID = '2054545668044070308'
const GOVERNANCE_GLOBAL_ID = 'FILE-982'
const TARGET_PATH = `/dcc/controlled-file/detail/${CONTROLLED_FILE_ID}`
const EXPECTED_SIGNATURE_COUNT = 4
const CURRENT_KEY_VERSION = 'dcc-hmac-v1'
const REISSUE_REQUEST_ID = `DCC-REISSUE-APPROVED-${Date.now()}`
const VERIFY_ONLY = process.env.DCC_REISSUE_E2E_VERIFY_ONLY === '1'

function parseEnvFile(filePath) {
  assert.ok(fs.existsSync(filePath), `required frontend environment file is missing: ${filePath}`)
  const result = {}
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/)
    if (!match) continue
    result[match[1]] = match[2].replace(/^(['"])(.*)\1$/, '$2')
  }
  return result
}

function requireCredentials() {
  const env = parseEnvFile(path.join(FRONTEND_ROOT, '.env'))
  const credentials = {
    tenant: process.env.DCC_REISSUE_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: process.env.DCC_REISSUE_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: process.env.DCC_REISSUE_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
  assert.ok(credentials.tenant, 'VITE_APP_DEFAULT_LOGIN_TENANT is required')
  assert.ok(credentials.username, 'VITE_APP_DEFAULT_LOGIN_USERNAME is required')
  assert.ok(credentials.password, 'VITE_APP_DEFAULT_LOGIN_PASSWORD is required')
  assert.equal(credentials.tenant, '\u828b\u9053\u6e90\u7801', 'E2E must use the approved local tenant')
  assert.equal(credentials.username, 'admin', 'E2E must use the approved local administrator')
  return credentials
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function login(page, credentials) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  assert.ok(await tenantInput.count(), 'visible tenant selector is required')
  await tenantInput.fill(credentials.tenant)
  const tenantOption = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: credentials.tenant })
    .first()
  await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
  await tenantOption.click()

  await form.locator('input[placeholder="\u8bf7\u8f93\u5165\u7528\u6237\u540d"]').fill(credentials.username)
  await form.locator('input[type="password"]').fill(credentials.password)
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) => item.url().includes('/system/auth/login') && item.request().method() === 'POST',
      { timeout: 60000 }
    ),
    form.getByRole('button', { name: '\u767b\u5f55' }).click()
  ])
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && payload && [0, 200].includes(payload.code), 'real frontend login failed')
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, {
    timeout: 60000
  })
}

function normalizeCacheString(value) {
  let current = String(value || '').trim()
  for (let index = 0; index < 3 && current.startsWith('"'); index += 1) {
    try {
      const parsed = JSON.parse(current)
      if (typeof parsed !== 'string') break
      current = parsed.trim()
    } catch {
      return current.replace(/^"(.*)"$/, '$1')
    }
  }
  return current
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) return ''
  let current
  try {
    current = JSON.parse(raw)
  } catch {
    return normalizeCacheString(raw)
  }
  for (let index = 0; index < 6; index += 1) {
    if (!current || typeof current !== 'object') {
      return typeof current === 'string' ? normalizeCacheString(current) : current || ''
    }
    if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) current = current.accessToken
    else if (Object.prototype.hasOwnProperty.call(current, 'v')) current = current.v
    else if (Object.prototype.hasOwnProperty.call(current, 'value')) current = current.value
    else break
  }
  return typeof current === 'string' ? normalizeCacheString(current) : current || ''
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  assert.ok(accessToken, 'browser login did not persist ACCESS_TOKEN')
  assert.ok(tenantId, 'browser login did not persist tenantId')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function readJson(response, label) {
  const payload = await response.json().catch(() => null)
  assert.ok(payload, `${label} did not return JSON`)
  return payload
}

function assertCommonSuccess(response, payload, label) {
  assert.equal(response.status(), 200, `${label} HTTP status`)
  assert.ok([0, 200].includes(payload.code), `${label} business code: ${payload.code}`)
  return payload.data
}

function assertSignatureSummary(summary, label) {
  assert.equal(String(summary.controlledFileId), CONTROLLED_FILE_ID, `${label} controlled file ID`)
  assert.equal(summary.signatures.length, EXPECTED_SIGNATURE_COUNT, `${label} signature count`)
}

async function openSignatureGovernancePage(page) {
  const recordResponsePromise = page.waitForResponse(
    (item) =>
      item.url().includes('/signature-governance/signature-records/page') &&
      item.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/signature-governance/signature-records`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const recordResponse = await recordResponsePromise
  const recordPayload = await readJson(recordResponse, 'signature governance page')
  const recordPage = assertCommonSuccess(recordResponse, recordPayload, 'signature governance page')
  assert.ok(recordPage.total > 0, 'signature governance page must contain real records')
  await page.getByText('\u7b7e\u540d\u8bb0\u5f55', { exact: true }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function filterGovernanceRecord(page) {
  await openSignatureGovernancePage(page)
  const filter = page.locator('.table-multi-filter').first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  if (!(await filter.locator('.table-multi-filter__condition-row').count())) {
    await filter.getByRole('button', { name: '\u65b0\u589e\u7b5b\u9009\u6761\u4ef6' }).click()
  }
  await filter.locator('.table-multi-filter__field-select').click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '\u5173\u952e\u5b57' }).first().click()
  await filter.locator('input[placeholder="\u4e1a\u52a1\u7f16\u53f7/\u540d\u79f0"]').fill(GOVERNANCE_GLOBAL_ID)
  const filteredResponsePromise = page.waitForResponse(
    (item) =>
      item.url().includes('/signature-governance/signature-records/page') &&
      item.url().includes(`keyword=${GOVERNANCE_GLOBAL_ID}`),
    { timeout: 60000 }
  )
  await filter.getByRole('button', { name: '\u67e5\u8be2' }).click()
  const filteredResponse = await filteredResponsePromise
  const filteredPayload = await readJson(filteredResponse, 'filtered governance record page')
  const filteredPage = assertCommonSuccess(filteredResponse, filteredPayload, 'filtered governance record page')
  assert.equal(filteredPage.total, 1, 'filtered governance record total')
  assert.equal(filteredPage.list[0].globalId, GOVERNANCE_GLOBAL_ID, 'filtered governance global ID')
  assert.equal(filteredPage.list[0].evidenceStatus, 'VALID', 'filtered governance evidence status')
  const visibleRows = page.locator('[data-user-table-key="signature.governance.records"] tbody tr')
  await visibleRows.first().waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await visibleRows.count(), 1, 'visible filtered governance row count')
  assert.match(await visibleRows.first().innerText(), /VALID/, 'visible filtered governance row status')
}

async function downloadPdf(request, headers, apiPath, outputPath, label) {
  const response = await request.get(`${BASE_URL}${apiPath}`, { headers, timeout: 60000 })
  assert.equal(response.status(), 200, `${label} HTTP status`)
  assert.match(response.headers()['content-type'] || '', /^application\/pdf\b/i, `${label} content type`)
  const content = await response.body()
  assert.equal(content.subarray(0, 5).toString('ascii'), '%PDF-', `${label} PDF signature`)
  assert.ok(content.length > 1000, `${label} must not be empty`)
  fs.writeFileSync(outputPath, content)
  return { bytes: content.length, file: path.basename(outputPath) }
}

async function main() {
  const credentials = requireCredentials()
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.mkdirSync(PDF_DIR, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
      'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  })
  const context = await browser.newContext({ acceptDownloads: true, viewport: { width: 1600, height: 1000 } })
  const page = await context.newPage()
  try {
    await login(page, credentials)
    await openSignatureGovernancePage(page)
    const headers = await buildAuthHeaders(page)
    const summaryPath = `/admin-api/dcc/controlled-files/${CONTROLLED_FILE_ID}/signature-export-summary`
    const exportPath = `/admin-api/dcc/controlled-files/${CONTROLLED_FILE_ID}/signature-evidence-export`

    const beforeSummaryResponse = await context.request.get(`${BASE_URL}${summaryPath}`, { headers })
    const beforeSummaryPayload = await readJson(beforeSummaryResponse, 'pre-reissue summary')
    const beforeSummary = assertCommonSuccess(beforeSummaryResponse, beforeSummaryPayload, 'pre-reissue summary')
    assertSignatureSummary(beforeSummary, 'pre-reissue summary')
    const beforeVerificationReasons = [
      ...new Set(beforeSummary.signatures.map((item) => item.verificationReason).filter(Boolean))
    ]
    let beforeExportStatus = null
    let beforeExportBusinessCode = null
    if (VERIFY_ONLY) {
      assert.equal(beforeSummary.allRequiredEvidenceValid, true, 'verify-only summary must already be valid')
    } else {
      assert.equal(beforeSummary.allRequiredEvidenceValid, false, 'pre-reissue summary must remain blocked')
      assert.ok(
        beforeSummary.signatures.every(
          (item) => item.verificationReason === 'EVIDENCE_HMAC_MISMATCH'
        ),
        `pre-reissue failure must be the historical evidence HMAC mismatch; actual=${JSON.stringify(beforeVerificationReasons)}`
      )

      const beforeExportResponse = await context.request.get(`${BASE_URL}${exportPath}`, { headers })
      beforeExportStatus = beforeExportResponse.status()
      assert.equal(beforeExportStatus, 400, 'pre-reissue evidence export must be blocked')
      const beforeExportPayload = await readJson(beforeExportResponse, 'pre-reissue evidence export')
      beforeExportBusinessCode = beforeExportPayload.code
      assert.equal(beforeExportBusinessCode, 1080000092, 'pre-reissue evidence export business code')

      const reissueResponse = await context.request.post(
        `${BASE_URL}/admin-api/dcc/controlled-files/${CONTROLLED_FILE_ID}/signature-evidence-reissue`,
        {
          headers: { ...headers, 'X-DCC-Request-Id': REISSUE_REQUEST_ID },
          data: { reason: '\u5386\u53f2\u7b7e\u540d\u91cd\u65b0\u5c01\u5b58\u5df2\u83b7\u4e1a\u52a1\u6279\u51c6\uff0c\u8865\u9f50\u6700\u7ec8\u53d7\u63a7\u526f\u672c\u8bc1\u636e\u7ed1\u5b9a' },
          timeout: 60000
        }
      )
      const reissuePayload = await readJson(reissueResponse, 'signature evidence reissue')
      const reissueSummary = assertCommonSuccess(reissueResponse, reissuePayload, 'signature evidence reissue')
      assertSignatureSummary(reissueSummary, 'reissue summary')
      assert.equal(reissueSummary.allRequiredEvidenceValid, true, 'reissue summary must be valid')
    }

    const afterSummaryResponse = await context.request.get(`${BASE_URL}${summaryPath}`, { headers })
    const afterSummaryPayload = await readJson(afterSummaryResponse, 'post-reissue summary')
    const afterSummary = assertCommonSuccess(afterSummaryResponse, afterSummaryPayload, 'post-reissue summary')
    assertSignatureSummary(afterSummary, 'post-reissue summary')
    assert.equal(afterSummary.allRequiredEvidenceValid, true, 'post-reissue summary must be valid')
    assert.ok(afterSummary.signatures.every((item) => item.evidenceStatus === 'VALID'), 'all evidence must be valid')
    assert.ok(
      afterSummary.signatures.every(
        (item) => item.controlledCopyHashStatus === 'BOUND' && item.controlledCopyHash && item.controlledCopyObjectKey
      ),
      'all signatures must be bound to the final controlled copy'
    )

    const signaturePageResponse = await context.request.get(
      `${BASE_URL}/admin-api/dcc/electronic-signatures/page`,
      { headers, params: { controlledFileId: CONTROLLED_FILE_ID, pageNo: 1, pageSize: 20 } }
    )
    const signaturePagePayload = await readJson(signaturePageResponse, 'post-reissue signature page')
    const signaturePage = assertCommonSuccess(signaturePageResponse, signaturePagePayload, 'post-reissue signature page')
    assert.equal(signaturePage.total, EXPECTED_SIGNATURE_COUNT, 'post-reissue signature page total')
    assert.ok(signaturePage.list.every((item) => item.keyVersion === CURRENT_KEY_VERSION), 'all signatures must use current key version')
    assert.ok(signaturePage.list.every((item) => item.evidenceStatus === 'VALID'), 'all projected evidence must be valid')

    const controlledEvidencePdf = await downloadPdf(
      context.request,
      headers,
      exportPath,
      path.join(PDF_DIR, 'controlled-file-2054545668044070308-signature-evidence.pdf'),
      'controlled file signature evidence PDF'
    )

    const governancePageResponse = await context.request.get(
      `${BASE_URL}/admin-api/signature-governance/signature-records/page`,
      { headers, params: { keyword: GOVERNANCE_GLOBAL_ID, pageNo: 1, pageSize: 20 } }
    )
    const governancePagePayload = await readJson(governancePageResponse, 'governance signature page')
    const governancePage = assertCommonSuccess(governancePageResponse, governancePagePayload, 'governance signature page')
    assert.ok(
      governancePage.list.some((item) => item.globalId === GOVERNANCE_GLOBAL_ID && item.evidenceStatus === 'VALID'),
      'governance record FILE-982 must be valid'
    )
    const governancePdf = await downloadPdf(
      context.request,
      headers,
      `/admin-api/signature-governance/signature-records/${GOVERNANCE_GLOBAL_ID}/pdf`,
      path.join(PDF_DIR, 'signature-governance-FILE-982.pdf'),
      'unified governance signature PDF'
    )

    await filterGovernanceRecord(page)
    await settle(page)
    const screenshotPath = path.join(TASK_DIR, 'e2e-signature-evidence-reissue-final.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })

    const result = {
      status: 'PASS',
      mode: VERIFY_ONLY ? 'POST_REISSUE_READ_ONLY' : 'APPROVED_REISSUE',
      controlledFileId: CONTROLLED_FILE_ID,
      requestId: VERIFY_ONLY ? null : REISSUE_REQUEST_ID,
      realFrontendLogin: true,
      filteredGovernanceRecordVisible: true,
      preReissue: {
        exportHttpStatus: beforeExportStatus,
        exportBusinessCode: beforeExportBusinessCode,
        verificationReasons: beforeVerificationReasons
      },
      postReissue: {
        allRequiredEvidenceValid: afterSummary.allRequiredEvidenceValid,
        evidenceStatuses: [...new Set(afterSummary.signatures.map((item) => item.evidenceStatus))],
        controlledCopyStatuses: [...new Set(afterSummary.signatures.map((item) => item.controlledCopyHashStatus))],
        bindingEventKeys: [...new Set(afterSummary.signatures.map((item) => item.bindingEventKey).filter(Boolean))],
        keyVersions: [...new Set(signaturePage.list.map((item) => item.keyVersion))],
        controlledCopyHashPresent: afterSummary.signatures.every((item) => Boolean(item.controlledCopyHash)),
        controlledCopyObjectKeyPresent: afterSummary.signatures.every((item) => Boolean(item.controlledCopyObjectKey))
      },
      pdfs: { controlledEvidencePdf, governancePdf },
      governanceGlobalId: GOVERNANCE_GLOBAL_ID,
      screenshot: path.basename(screenshotPath)
    }
    fs.writeFileSync(path.join(TASK_DIR, 'e2e-signature-evidence-reissue-final-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack : String(error)}\n`)
  process.exitCode = 1
})
