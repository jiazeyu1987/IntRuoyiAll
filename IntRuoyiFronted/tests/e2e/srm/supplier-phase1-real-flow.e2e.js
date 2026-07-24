const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = process.cwd()
const outputDir = path.resolve(frontendRoot, 'output/playwright/srm-phase1')
fs.mkdirSync(outputDir, { recursive: true })

const config = {
  baseUrl: process.env.SRM_PHASE1_BASE_URL || 'http://127.0.0.1:8118',
  tenant: process.env.SRM_PHASE1_TENANT || '测试租户',
  creatorUsername: process.env.SRM_PHASE1_CREATOR || 'aoteman',
  creatorPassword: process.env.SRM_PHASE1_CREATOR_PASSWORD || '111111',
  auditorUsername: process.env.SRM_PHASE1_AUDITOR || 'edhrmatrixapprover',
  auditorPassword: process.env.SRM_PHASE1_AUDITOR_PASSWORD || '111111',
  supplierId: process.env.SRM_PHASE1_SUPPLIER_ID ? Number(process.env.SRM_PHASE1_SUPPLIER_ID) : null,
  supplierName: process.env.SRM_PHASE1_SUPPLIER_NAME || '',
}

function assertSuccess(payload, action) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `${action} failed: ${JSON.stringify(payload)}`)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function login(page, { username, password }) {
  let authContext = { authorization: '', tenantId: '' }
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form
    .locator('input[placeholder="请输入租户名称"], input[placeholder="租户名称"], .el-select input[role="combobox"], input.el-select__input')
    .first()
  const tenantResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/tenant/get-id-by-name') &&
      response.url().includes(encodeURIComponent(config.tenant)) &&
      response.ok(),
    { timeout: 30000 }
  ).catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await tenantResponsePromise

  const textInputs = form.locator('input.el-input__inner')
  await textInputs.nth(0).fill('')
  await textInputs.nth(0).fill(username)
  await form.locator('input[type="password"]').first().fill(password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginPayload = await (await loginResponsePromise).json()
  assertSuccess(loginPayload, `login(${username})`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
  const permissionResponse = await permissionPromise
  authContext = {
    authorization:
      permissionResponse.request().headers()['authorization'] ||
      permissionResponse.request().headers()['Authorization'] ||
      '',
    tenantId:
      permissionResponse.request().headers()['tenant-id'] ||
      permissionResponse.request().headers()['Tenant-Id'] ||
      ''
  }
  return authContext
}

async function fetchJson(page, relativeUrl, authContext, { method = 'GET', body } = {}) {
  const payload = await page.evaluate(async ({ url, method, body, authHeader, tenantHeader }) => {
    const headers = { Accept: 'application/json, text/plain, */*' }
    if (authHeader) {
      headers.Authorization = authHeader
    }
    if (tenantHeader) {
      headers['tenant-id'] = tenantHeader
    }
    if (body) {
      headers['Content-Type'] = 'application/json'
    }
    const response = await fetch(url, {
      method,
      credentials: 'include',
      headers,
      body: body ? JSON.stringify(body) : undefined,
    })
    const text = await response.text()
    return { status: response.status, text }
  }, {
    url: `${config.baseUrl}${relativeUrl}`,
    method,
    body,
    authHeader: authContext?.authorization || '',
    tenantHeader: authContext?.tenantId || ''
  })

  assert.equal(payload.status, 200, `${method} ${relativeUrl} should return HTTP 200`)
  const data = JSON.parse(payload.text)
  return data
}

async function querySupplierAccessPage(page, authContext, supplierName = '') {
  const pagePayload = await fetchJson(
    page,
    `/admin-api/srm/supplier-access/page?pageNo=1&pageSize=200&supplierName=${encodeURIComponent(supplierName)}`,
    authContext
  )
  assertSuccess(pagePayload, 'query supplier access page')
  return pagePayload.data?.list || []
}

async function queryReferenceSuppliers(page, authContext, keyword = '') {
  const payload = await fetchJson(
    page,
    `/admin-api/srm/supplier-access/reference-suppliers?keyword=${encodeURIComponent(keyword)}`,
    authContext
  )
  assertSuccess(payload, 'query reference suppliers')
  return payload.data || []
}

async function getCurrentPortalApplication(page, authContext) {
  const payload = await fetchJson(page, '/admin-api/srm/supplier-portal/my', authContext)
  assertSuccess(payload, 'get current portal application')
  return payload.data || null
}

async function resolveSupplier(page, authContext) {
  if (!config.supplierId) {
    const application = await getCurrentPortalApplication(page, authContext)
    assert.ok(application?.supplierId, 'current applicant should already own a portal-created supplier')
    assert.equal(
      application.applicationStatus,
      'APPROVED',
      'current applicant portal application should be approved before Phase 1 access audit'
    )
    config.supplierId = Number(application.supplierId)
    config.supplierName = application.companyName || config.supplierName
  }
  const references = await queryReferenceSuppliers(page, authContext, config.supplierName || '')
  const matched = references.find((item) => item.id === config.supplierId)
  assert.ok(matched, `configured supplierId=${config.supplierId} is not available in reference suppliers`)
  config.supplierName = matched.name
  return matched
}

async function ensureAccessRecord(page, authContext) {
  const existing = (await querySupplierAccessPage(page, authContext, config.supplierName)).find(
    (item) => item.supplierId === config.supplierId
  )
  const baseRecord = {
    supplierId: config.supplierId,
    portalContactName: 'Phase1联系人',
    portalContactPhone: '13800001111',
    qualificationExpireDate: '2026-12-31',
    accessRemark: 'Codex Phase1 真实E2E'
  }

  if (!existing) {
    const createPayload = await fetchJson(page, '/admin-api/srm/supplier-access/create', authContext, {
      method: 'POST',
      body: baseRecord
    })
    assertSuccess(createPayload, 'create supplier access')
    return Number(createPayload.data)
  }

  const updatePayload = await fetchJson(page, '/admin-api/srm/supplier-access/update', authContext, {
    method: 'PUT',
    body: { ...baseRecord, id: existing.id }
  })
  assertSuccess(updatePayload, 'update supplier access')
  return Number(existing.id)
}

async function deleteAccessRecordIfExists(page, authContext) {
  const existing = (await querySupplierAccessPage(page, authContext, config.supplierName)).find(
    (item) => item.supplierId === config.supplierId
  )
  if (!existing?.id) {
    return null
  }
  const payload = await fetchJson(
    page,
    `/admin-api/srm/supplier-access/delete?id=${existing.id}`,
    authContext,
    { method: 'DELETE' }
  )
  assertSuccess(payload, 'delete supplier access')
  return Number(existing.id)
}

async function submitAudit(page, authContext, relativeUrl, id, auditRemark) {
  const payload = await fetchJson(page, relativeUrl, authContext, {
    method: 'PUT',
    body: { id, auditRemark }
  })
  assertSuccess(payload, `${relativeUrl} ${id}`)
}

async function loadProfile(page, authContext) {
  const payload = await fetchJson(page, `/admin-api/srm/supplier-access/profile?supplierId=${config.supplierId}`, authContext)
  assertSuccess(payload, 'get supplier profile')
  return payload.data
}

async function openAccessAndProfile(page) {
  await page.goto(`${config.baseUrl}/srm/supplier/access`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page, 30000)
  await page.getByRole('button', { name: '档案' }).first().click()
  await page.waitForURL((url) => url.href.includes('/srm/supplier/profile'), { timeout: 30000 })
  await settle(page)
}

async function main() {
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  try {
    const creatorContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const creatorPage = await creatorContext.newPage()
    const creatorAuth = await login(creatorPage, { username: config.creatorUsername, password: config.creatorPassword })
    await resolveSupplier(creatorPage, creatorAuth)
    await deleteAccessRecordIfExists(creatorPage, creatorAuth)
    const accessId = await ensureAccessRecord(creatorPage, creatorAuth)
    const blockedPayload = await fetchJson(
      creatorPage,
      `/admin-api/srm/supplier-access/check?supplierId=${config.supplierId}`,
      creatorAuth
    )
    assertSuccess(blockedPayload, 'check before stage approval')
    assert.equal(blockedPayload.data.eligible, false, 'supplier should be blocked before stage approvals')
    await openAccessAndProfile(creatorPage)
    const creatorProfile = await loadProfile(creatorPage, creatorAuth)
    assert.equal(creatorProfile.portalContactName, 'Phase1联系人')
    assert.equal(creatorProfile.portalContactPhone, '13800001111')

    const auditorContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const auditorPage = await auditorContext.newPage()
    const auditorAuth = await login(auditorPage, { username: config.auditorUsername, password: config.auditorPassword })
    await submitAudit(auditorPage, auditorAuth, '/admin-api/srm/supplier-access/sample/approve', accessId, '样品通过-Phase1-E2E')
    await submitAudit(auditorPage, auditorAuth, '/admin-api/srm/supplier-access/trial/approve', accessId, '试用通过-Phase1-E2E')
    await submitAudit(auditorPage, auditorAuth, '/admin-api/srm/supplier-access/approve', accessId, '准入通过-Phase1-E2E')

    const finalProfile = await loadProfile(auditorPage, auditorAuth)
    assert.equal(finalProfile.sampleTestStatus, 'PASSED')
    assert.equal(finalProfile.trialOrderStatus, 'PASSED')
    assert.equal(finalProfile.accessStatus, 'APPROVED')
    assert.match(finalProfile.sampleAuditRemark || '', /Phase1-E2E/)
    assert.match(finalProfile.trialAuditRemark || '', /Phase1-E2E/)

    const finalEligibility = await fetchJson(
      auditorPage,
      `/admin-api/srm/supplier-access/check?supplierId=${config.supplierId}`,
      auditorAuth
    )
    assertSuccess(finalEligibility, 'check after stage approval')
    assert.equal(finalEligibility.data.eligible, true, 'supplier should be eligible after approvals')

    await auditorPage.goto(`${config.baseUrl}/srm/supplier/profile?supplierId=${config.supplierId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(auditorPage)
    await auditorPage.screenshot({ path: path.join(outputDir, 'supplier-profile-phase1.png'), fullPage: true })
    await creatorPage.screenshot({ path: path.join(outputDir, 'supplier-access-phase1.png'), fullPage: true })

    console.log(JSON.stringify({
      ok: true,
      baseUrl: config.baseUrl,
      supplierId: config.supplierId,
      accessId,
      finalProfileUrl: auditorPage.url(),
      finalEligibility: finalEligibility.data,
      screenshots: {
        access: path.join(outputDir, 'supplier-access-phase1.png'),
        profile: path.join(outputDir, 'supplier-profile-phase1.png'),
      }
    }, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
