const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = process.cwd()
const outputDir = path.resolve(frontendRoot, 'output/playwright/srm-phase1')
fs.mkdirSync(outputDir, { recursive: true })

const nowToken = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)
const companyName = `SRM Portal E2E ${nowToken}`
const unifiedSocialCreditCode = `91310000${nowToken.slice(-10)}`

const config = {
  baseUrl: process.env.SRM_PORTAL_BASE_URL || 'http://127.0.0.1:8118',
  tenant: process.env.SRM_PORTAL_TENANT || '测试租户',
  applicantUsername: process.env.SRM_PORTAL_APPLICANT || 'aoteman',
  applicantPassword: process.env.SRM_PORTAL_APPLICANT_PASSWORD || '111111',
  reviewerUsername: process.env.SRM_PORTAL_REVIEWER || 'edhrmatrixapprover',
  reviewerPassword: process.env.SRM_PORTAL_REVIEWER_PASSWORD || '111111',
  companyName,
  unifiedSocialCreditCode,
  contactName: '门户测试联系人',
  contactPhone: '13900001234',
  contactEmail: `portal.${nowToken}@example.com`,
  qualificationAttachmentUrls: `https://example.com/srm/${nowToken}/qualification.pdf`,
  qualificationExpireDate: '2026-12-31',
  bankName: '招商银行上海分行',
  bankAccount: `62258888${nowToken.slice(-8)}`,
  bankAddress: '上海市浦东新区张江高科路88号'
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function assertSuccess(payload, action) {
  assert.ok(isSuccessPayload(payload), `${action} failed: ${JSON.stringify(payload)}`)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function login(page, { username, password, redirectPath = '/index' }) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = loginForm
    .locator(
      'input[placeholder="请输入租户名称"], input[placeholder="租户名称"], .el-select input[role="combobox"], input.el-select__input'
    )
    .first()
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(config.tenant)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await tenantResponsePromise

  const textInputs = loginForm.locator('input.el-input__inner')
  await textInputs.nth(0).fill('')
  await textInputs.nth(0).fill(username)
  await loginForm.locator('input[type="password"]').first().fill(password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).first().click()
  const loginPayload = await (await loginResponsePromise).json()
  assertSuccess(loginPayload, `login(${username})`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
  const permissionResponse = await permissionPromise
  const permissionPayload = await permissionResponse.json().catch(() => null)
  assertSuccess(permissionPayload, `permission(${username})`)

  return {
    authorization:
      permissionResponse.request().headers()['authorization'] ||
      permissionResponse.request().headers()['Authorization'] ||
      '',
    tenantId:
      permissionResponse.request().headers()['tenant-id'] ||
      permissionResponse.request().headers()['Tenant-Id'] ||
      '',
    permissionPayload
  }
}

async function fetchJson(page, relativeUrl, authContext, { method = 'GET', body } = {}) {
  const payload = await page.evaluate(
    async ({ url, method, body, authHeader, tenantHeader }) => {
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
        body: body ? JSON.stringify(body) : undefined
      })
      return {
        status: response.status,
        text: await response.text()
      }
    },
    {
      url: `${config.baseUrl}${relativeUrl}`,
      method,
      body,
      authHeader: authContext?.authorization || '',
      tenantHeader: authContext?.tenantId || ''
    }
  )

  assert.equal(payload.status, 200, `${method} ${relativeUrl} should return HTTP 200`)
  return JSON.parse(payload.text)
}

async function fillPortalApplication(page) {
  const form = page.locator('form').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const fillInput = async (label, value) => {
    const item = page.locator('.el-form-item').filter({ hasText: label }).first()
    await item.locator('input').first().fill(value)
  }

  await fillInput('企业名称', config.companyName)
  await fillInput('统一社会信用代码', config.unifiedSocialCreditCode)
  await fillInput('联系人', config.contactName)
  await fillInput('联系电话', config.contactPhone)
  await fillInput('联系邮箱', config.contactEmail)
  await page
    .locator('.el-form-item')
    .filter({ hasText: '资质附件 URL' })
    .first()
    .locator('textarea')
    .fill(config.qualificationAttachmentUrls)
  await page
    .locator('.el-form-item')
    .filter({ hasText: '资质到期日' })
    .first()
    .locator('input')
    .first()
    .fill(config.qualificationExpireDate)
  await fillInput('开户行', config.bankName)
  await fillInput('银行账号', config.bankAccount)
  await fillInput('开户地址', config.bankAddress)
}

function syncConfigFromApplication(application) {
  if (!application) {
    return
  }
  config.companyName = application.companyName || config.companyName
  config.unifiedSocialCreditCode =
    application.unifiedSocialCreditCode || config.unifiedSocialCreditCode
  config.contactName = application.contactName || config.contactName
  config.contactPhone = application.contactPhone || config.contactPhone
  config.contactEmail = application.contactEmail || config.contactEmail
  config.qualificationAttachmentUrls =
    application.qualificationAttachmentUrls || config.qualificationAttachmentUrls
  config.qualificationExpireDate =
    application.qualificationExpireDate || config.qualificationExpireDate
  config.bankName = application.bankName || config.bankName
  config.bankAccount = application.bankAccount || config.bankAccount
  config.bankAddress = application.bankAddress || config.bankAddress
}

async function getCurrentApplication(page, authContext) {
  const payload = await fetchJson(page, '/admin-api/srm/supplier-portal/my', authContext)
  assertSuccess(payload, 'get current portal application')
  return payload.data || null
}

async function submitPortalApplication(page) {
  await page.goto(`${config.baseUrl}/srm/portal/application`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.getByRole('heading', { name: '供应商注册与资料提交' }).waitFor({ timeout: 30000 })
  await fillPortalApplication(page)

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/srm/supplier-portal/submit') &&
      response.request().method() === 'POST' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '提交审核' }).click()
  const submitPayload = await (await submitResponsePromise).json()
  assertSuccess(submitPayload, 'submit portal application')
  await expectVisibleText(page, '资料已提交，等待内部审核')
  await page.getByText('已提交').first().waitFor({ timeout: 30000 })
  await settle(page)
}

async function expectVisibleText(page, text) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function approvePortalApplication(page) {
  await page.goto(`${config.baseUrl}/srm/supplier-portal-review`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page, 30000)
  await page.getByRole('heading', { name: '供应商门户审核台' }).waitFor({ timeout: 30000 })

  const companyQuery = page.locator('.el-form-item').filter({ hasText: '企业名称' }).first().locator('input').first()
  await companyQuery.fill(config.companyName)
  await page.getByRole('button', { name: '查询' }).click()
  await settle(page)

  const row = page.locator('.el-table__row').filter({ hasText: config.companyName }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.getByRole('button', { name: '通过' }).click()
  const dialog = page.locator('.el-dialog').filter({ hasText: '审核通过' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('textarea').fill('真实 E2E 审核通过')

  const approveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/srm/supplier-portal/approve') &&
      response.request().method() === 'PUT' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '确认' }).click()
  const approvePayload = await (await approveResponsePromise).json()
  assertSuccess(approvePayload, 'approve portal application')
  await expectVisibleText(page, '已审核通过')
  await settle(page)
}

async function verifyPortalResults(page, authContext, { reusedApprovedApplication = false } = {}) {
  const portalPagePayload = await fetchJson(
    page,
    `/admin-api/srm/supplier-portal/page?pageNo=1&pageSize=20&companyName=${encodeURIComponent(config.companyName)}`,
    authContext
  )
  assertSuccess(portalPagePayload, 'query portal page')
  const portalRecord = (portalPagePayload.data?.list || []).find(
    (item) => item.companyName === config.companyName
  )
  assert.ok(portalRecord?.id, 'portal record should exist after approval')
  assert.equal(portalRecord.applicationStatus, 'APPROVED')
  assert.ok(portalRecord.supplierId, 'portal approval should create or bind ERP supplier')

  const accessPagePayload = await fetchJson(
    page,
    `/admin-api/srm/supplier-access/page?pageNo=1&pageSize=20&supplierName=${encodeURIComponent(config.companyName)}`,
    authContext
  )
  assertSuccess(accessPagePayload, 'query supplier access page')
  const accessRecord = (accessPagePayload.data?.list || []).find(
    (item) => item.supplierId === portalRecord.supplierId
  )
  assert.ok(accessRecord?.id, 'portal approval should create supplier access record')

  const profilePayload = await fetchJson(
    page,
    `/admin-api/srm/supplier-access/profile?supplierId=${portalRecord.supplierId}`,
    authContext
  )
  assertSuccess(profilePayload, 'query supplier profile')
  assert.ok(profilePayload.data.portalContactName, 'supplier profile should include portal contact name')
  assert.ok(profilePayload.data.portalContactPhone, 'supplier profile should include portal contact phone')

  const eligibilityPayload = await fetchJson(
    page,
    `/admin-api/srm/supplier-access/check?supplierId=${portalRecord.supplierId}`,
    authContext
  )
  assertSuccess(eligibilityPayload, 'query supplier eligibility')
  if (reusedApprovedApplication) {
    assert.ok(
      ['PENDING', 'APPROVED'].includes(accessRecord.accessStatus),
      `reused application access status should stay in supported range, got ${accessRecord.accessStatus}`
    )
    assert.ok(
      ['PENDING', 'PASSED'].includes(accessRecord.sampleTestStatus),
      `reused application sample status should stay in supported range, got ${accessRecord.sampleTestStatus}`
    )
    assert.ok(
      ['NOT_STARTED', 'PASSED'].includes(accessRecord.trialOrderStatus),
      `reused application trial status should stay in supported range, got ${accessRecord.trialOrderStatus}`
    )
  } else {
    assert.equal(accessRecord.accessStatus, 'PENDING')
    assert.equal(eligibilityPayload.data.eligible, false)
    assert.equal(accessRecord.sampleTestStatus, 'PENDING')
    assert.equal(accessRecord.trialOrderStatus, 'NOT_STARTED')
  }

  return {
    portalRecord,
    accessRecord,
    profile: profilePayload.data,
    eligibility: eligibilityPayload.data
  }
}

async function main() {
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  try {
    const applicantContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const applicantPage = await applicantContext.newPage()
    const applicantAuth = await login(applicantPage, {
      username: config.applicantUsername,
      password: config.applicantPassword,
      redirectPath: '/srm/portal/application'
    })
    const currentApplication = await getCurrentApplication(applicantPage, applicantAuth)
    syncConfigFromApplication(currentApplication)

    let reusedApprovedApplication = false
    let pendingReviewApplication = false
    if (currentApplication?.applicationStatus === 'APPROVED') {
      reusedApprovedApplication = true
      await applicantPage.goto(`${config.baseUrl}/srm/portal/application`, {
        waitUntil: 'domcontentloaded',
        timeout: 60000
      })
      await settle(applicantPage)
      await expectVisibleText(applicantPage, '审核通过')
    } else if (currentApplication?.applicationStatus === 'SUBMITTED') {
      pendingReviewApplication = true
    } else {
      await submitPortalApplication(applicantPage)
    }
    await applicantPage.screenshot({
      path: path.join(outputDir, 'supplier-portal-application-submitted.png'),
      fullPage: true
    })

    const reviewerContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const reviewerPage = await reviewerContext.newPage()
    const reviewerAuth = await login(reviewerPage, {
      username: config.reviewerUsername,
      password: config.reviewerPassword,
      redirectPath: '/srm/supplier-portal-review'
    })
    const permissions = reviewerAuth.permissionPayload?.data?.permissions || []
    assert.ok(
      permissions.includes('srm:supplier-portal:audit'),
      'reviewer should include srm:supplier-portal:audit permission'
    )
    if (!reusedApprovedApplication) {
      await approvePortalApplication(reviewerPage)
    }
    await reviewerPage.screenshot({
      path: path.join(outputDir, 'supplier-portal-review-approved.png'),
      fullPage: true
    })

    const verification = await verifyPortalResults(reviewerPage, reviewerAuth, {
      reusedApprovedApplication
    })
    console.log(
      JSON.stringify(
        {
          ok: true,
          baseUrl: config.baseUrl,
          companyName: config.companyName,
          portalApplicationId: verification.portalRecord.id,
          supplierId: verification.portalRecord.supplierId,
          supplierAccessId: verification.accessRecord.id,
          accessStatus: verification.accessRecord.accessStatus,
          eligibility: verification.eligibility.eligible,
          reusedApprovedApplication,
          pendingReviewApplication,
          screenshots: {
            application: path.join(outputDir, 'supplier-portal-application-submitted.png'),
            review: path.join(outputDir, 'supplier-portal-review-approved.png')
          }
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
