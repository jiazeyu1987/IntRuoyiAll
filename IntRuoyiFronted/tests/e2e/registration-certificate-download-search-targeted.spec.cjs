const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const ARTIFACT_DIR = process.env.REG_CERT_E2E_ARTIFACT_DIR
  ? path.resolve(process.env.REG_CERT_E2E_ARTIFACT_DIR)
  : path.join(
      REPO_ROOT,
      'doc',
      'tasks',
      '20260829-registration-certificate-download-search',
      'e2e-artifacts'
    )
const RESULT_PATH = path.join(
  ARTIFACT_DIR,
  'registration-certificate-download-search-targeted-result.json'
)

function readDotEnvValue(name) {
  for (const fileName of ['.env.local', '.env']) {
    const filePath = path.join(FRONTEND_ROOT, fileName)
    if (!fs.existsSync(filePath)) continue
    const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/)
    for (const line of lines) {
      const match = line.match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*?)\s*$/)
      if (match && match[1] === name) {
        return match[2].replace(/^['"]|['"]$/g, '')
      }
    }
  }
  return ''
}

const config = {
  baseUrl: (
    process.env.REG_CERT_E2E_BASE_URL ||
    process.env.E2E_BASE_URL ||
    'http://127.0.0.1:8081'
  ).replace(/\/+$/, ''),
  tenant:
    process.env.REG_CERT_E2E_TENANT ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_TENANT') ||
    '芋道源码',
  username:
    process.env.REG_CERT_E2E_USERNAME ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_USERNAME') ||
    'admin',
  password: process.env.REG_CERT_E2E_PASSWORD || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD')
}

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function extractPageResult(payload) {
  const data = payload && payload.data
  return {
    list: Array.isArray(data?.list) ? data.list : [],
    total: Number(data?.total || 0)
  }
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

function registrationPath(response, suffix) {
  if (response.request().method() !== 'GET') return false
  const pathname = new URL(response.url()).pathname
  return pathname.endsWith(`/admin-api/dcc/registration-certificates${suffix}`)
}

async function login(page) {
  expect(config.password, 'login password must be available without logging it').toBeTruthy()

  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 90000 }
  )
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()

  const loginPayload = await readJsonResponse(await loginResponsePromise)
  expect(isBusinessOk(loginPayload), `login business code ${loginPayload.code}`).toBe(true)

  const permissionPayload = await readJsonResponse(await permissionResponsePromise)
  expect(isBusinessOk(permissionPayload), `permission-info code ${permissionPayload.code}`).toBe(true)
  expect(JSON.stringify(permissionPayload.data || {})).toContain(
    'dcc:registration-certificate:query-current'
  )
}

async function selectVisibleOption(page, text) {
  const option = page
    .getByRole('option')
    .filter({ hasText: new RegExp(`^\\s*${text}\\s*$`) })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

const MONTH_NUMBER_BY_LABEL = new Map([
  ['January', 1],
  ['February', 2],
  ['March', 3],
  ['April', 4],
  ['May', 5],
  ['June', 6],
  ['July', 7],
  ['August', 8],
  ['September', 9],
  ['October', 10],
  ['November', 11],
  ['December', 12]
])

function monthDistance(from, to) {
  return (to.year - from.year) * 12 + (to.month - from.month)
}

async function readVisibleDatePanelMonth(page) {
  const panel = page.locator('.el-picker-panel:visible').last()
  await expect(panel).toBeVisible()
  const labels = (await panel.locator('.el-date-picker__header-label').allInnerTexts())
    .map((item) => item.trim())
    .filter(Boolean)
  const joined = labels.join(' ')
  const year = Number(joined.match(/\b(19|20)\d{2}\b/)?.[0])
  const monthLabel = labels.find((label) => MONTH_NUMBER_BY_LABEL.has(label))
  const chineseMonth = Number(joined.match(/(\d{1,2})\s*月/)?.[1])
  const month = monthLabel ? MONTH_NUMBER_BY_LABEL.get(monthLabel) : chineseMonth
  expect(year, `date picker year must be readable from ${joined}`).toBeGreaterThan(1900)
  expect(month, `date picker month must be readable from ${joined}`).toBeGreaterThan(0)
  return { panel, year, month }
}

async function pickDateValue(page, field, value) {
  const [yearText, monthText, dayText] = value.split('-')
  const target = {
    year: Number(yearText),
    month: Number(monthText),
    day: Number(dayText)
  }
  const input = field.locator('.table-multi-filter-field__value input:visible').first()
  await input.click()
  for (let index = 0; index < 30; index += 1) {
    const current = await readVisibleDatePanelMonth(page)
    const distance = monthDistance(current, target)
    if (distance === 0) break
    if (distance > 0) {
      await current.panel.getByRole('button', { name: '下个月' }).click()
    } else {
      await current.panel.getByRole('button', { name: '上个月' }).click()
    }
    await page.waitForTimeout(100)
  }
  const current = await readVisibleDatePanelMonth(page)
  expect(monthDistance(current, target), `date picker must reach ${value}`).toBe(0)
  const day = current.panel
    .locator('td.available:not(.prev-month):not(.next-month) .el-date-table-cell__text')
    .filter({ hasText: new RegExp(`^\\s*${target.day}\\s*$`) })
    .first()
  await expect(day, `date picker must expose day ${target.day} for ${value}`).toBeVisible()
  await day.click()
  await expect(input).toHaveValue(value)
}

async function addOldFilterCondition(page, form, fieldLabel, fieldKey, value) {
  await form.getByRole('button', { name: '新增筛选条件' }).click()
  await form.locator('.table-multi-filter__field-select').click()
  await selectVisibleOption(page, fieldLabel)
  const field = form.locator(`.table-multi-filter-field[data-filter-key="${fieldKey}"]`)
  await expect(field, `${fieldLabel} filter field must render`).toBeVisible()
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    await pickDateValue(page, field, value)
    return
  }
  const input = field.locator('.table-multi-filter-field__value input:visible').first()
  await input.fill(value)
  await input.press('Escape').catch(() => undefined)
}

async function addSelectFilterCondition(page, form, fieldLabel, fieldKey, optionText) {
  await form.getByRole('button', { name: '新增筛选条件' }).click()
  await form.locator('.table-multi-filter__field-select').click()
  await selectVisibleOption(page, fieldLabel)
  const field = form.locator(`.table-multi-filter-field[data-filter-key="${fieldKey}"]`)
  await expect(field, `${fieldLabel} filter field must render`).toBeVisible()
  await field.locator('.el-select').click()
  await selectVisibleOption(page, optionText)
}

test.describe('registration certificate download and search targeted real flow', () => {
  test('real page exposes download file selection and old-index business/date filters', async ({
    page
  }) => {
    test.setTimeout(180000)

    const evidence = {
      status: 'RUNNING',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      responses: [],
      failedResponses: [],
      writeRequests: [],
      pageErrors: [],
      consoleErrors: []
    }

    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') evidence.consoleErrors.push(message.text())
    })
    page.on('request', (request) => {
      const method = request.method()
      const url = request.url()
      if (
        !['GET', 'HEAD', 'OPTIONS'].includes(method) &&
        url.includes('/admin-api/dcc/registration-certificates')
      ) {
        evidence.writeRequests.push({ method, path: new URL(url).pathname })
      }
    })
    page.on('response', async (response) => {
      const url = response.url()
      if (response.status() >= 400) {
        evidence.failedResponses.push({
          method: response.request().method(),
          path: new URL(url).pathname,
          status: response.status()
        })
      }
      if (!url.includes('/admin-api/dcc/registration-certificates')) return
      const payload = await readJsonResponse(response)
      evidence.responses.push({
        method: response.request().method(),
        path: new URL(url).pathname,
        status: response.status(),
        code: payload.code,
        message: payload.msg || payload.message || ''
      })
    })

    try {
      await login(page)

      const pageResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, '/page'),
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit' })
      await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
        timeout: 60000
      })

      const currentPayload = await readJsonResponse(await pageResponsePromise)
      expect(isBusinessOk(currentPayload), `current page code ${currentPayload.code}`).toBe(true)
      let currentPage = extractPageResult(currentPayload)
      let downloadableCandidate = currentPage.list.find(
        (item) => item.hasProjectCode && item.hasRegistrationFile
      )
      if (!downloadableCandidate) {
        const currentForm = page.locator('[data-testid="registration-certificate-current-filter-form"]')
        await addSelectFilterCondition(page, currentForm, '项目代码', 'missingProjectCode', '已提供')
        await addSelectFilterCondition(page, currentForm, '注册证文件', 'missingFile', '已提供')
        const currentFilteredResponsePromise = page.waitForResponse(
          (response) => registrationPath(response, '/page'),
          { timeout: 60000 }
        )
        await currentForm.getByRole('button', { name: '查询' }).click()
        const currentFilteredResponse = await currentFilteredResponsePromise
        const currentFilteredPayload = await readJsonResponse(currentFilteredResponse)
        expect(
          isBusinessOk(currentFilteredPayload),
          `filtered current page code ${currentFilteredPayload.code}`
        ).toBe(true)
        const currentQuery = new URL(currentFilteredResponse.url()).searchParams
        expect(currentQuery.get('missingProjectCode')).toBe('false')
        expect(currentQuery.get('missingFile')).toBe('false')
        currentPage = extractPageResult(currentFilteredPayload)
        downloadableCandidate = currentPage.list.find(
          (item) => item.hasProjectCode && item.hasRegistrationFile
        )
        evidence.currentFilteredQuery = {
          missingProjectCode: currentQuery.get('missingProjectCode'),
          missingFile: currentQuery.get('missingFile'),
          total: currentPage.total
        }
      }
      expect(
        downloadableCandidate,
        'B-TEST requires one current certificate with project code and formal registration file'
      ).toBeTruthy()
      evidence.currentCount = currentPage.total
      evidence.downloadableCandidate = {
        certificateId: downloadableCandidate.certificateId,
        versionId: downloadableCandidate.versionId,
        certificateNo: downloadableCandidate.certificateNo,
        hasProjectCode: downloadableCandidate.hasProjectCode,
        hasRegistrationFile: downloadableCandidate.hasRegistrationFile
      }

      const detailResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, `/${downloadableCandidate.certificateId}`),
        { timeout: 60000 }
      )
      const historyResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, `/${downloadableCandidate.certificateId}/history`),
        { timeout: 60000 }
      )
      await page.goto(
        `${config.baseUrl}/mdm/registration-certificate/detail/${downloadableCandidate.certificateId}`,
        { waitUntil: 'commit' }
      )
      await expect(page.locator('[data-testid="registration-certificate-detail-page"]')).toBeVisible({
        timeout: 60000
      })
      const detailPayload = await readJsonResponse(await detailResponsePromise)
      const historyPayload = await readJsonResponse(await historyResponsePromise)
      expect(isBusinessOk(detailPayload), `detail code ${detailPayload.code}`).toBe(true)
      expect(isBusinessOk(historyPayload), `history code ${historyPayload.code}`).toBe(true)
      expect(detailPayload.data?.registrationFileId).toBeTruthy()
      expect(detailPayload.data?.projectCodeId).toBeTruthy()

      await page.getByRole('tab', { name: '访问申请' }).click()
      const accessPanel = page.locator(
        '[data-testid="registration-certificate-access-request-action"]'
      )
      await expect(accessPanel).toBeVisible()
      await accessPanel.getByText('下载文件', { exact: true }).click()
      const fileSelect = accessPanel.locator(
        '[data-testid="registration-certificate-download-file-select"]'
      )
      await expect(fileSelect).toBeVisible()
      await expect(fileSelect).not.toHaveClass(/is-disabled/)
      await fileSelect.click()
      const visibleFileOptions = page.getByRole('option').filter({ hasText: /注册证文件|变更文件/ })
      await expect(visibleFileOptions.first()).toBeVisible()
      const fileOptions = await visibleFileOptions.allInnerTexts()
      expect(fileOptions.length, 'download request must offer at least one formal file option').toBeGreaterThan(0)
      expect(fileOptions.join('\n')).toMatch(/注册证文件|变更文件/)
      evidence.downloadFileOptions = fileOptions

      const oldIndexInitialResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, '/old-index/page'),
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit' })
      await page.getByRole('tab', { name: '老证' }).click()
      await expect(page.locator('[data-testid="registration-certificate-old-index"]')).toBeVisible({
        timeout: 60000
      })
      const oldInitialPayload = await readJsonResponse(await oldIndexInitialResponsePromise)
      expect(isBusinessOk(oldInitialPayload), `old-index code ${oldInitialPayload.code}`).toBe(true)
      evidence.oldIndexInitialCount = extractPageResult(oldInitialPayload).total

      const oldForm = page.locator('[data-testid="registration-certificate-old-filter-form"]')
      await expect(oldForm).toBeVisible()
      await oldForm.getByRole('button', { name: '新增筛选条件' }).click()
      await oldForm.locator('.table-multi-filter__field-select').click()
      await expect(page.getByRole('option').filter({ hasText: '注册证编号' }).first()).toBeVisible()
      const fieldLabels = (await page.getByRole('option').allInnerTexts())
        .map((item) => item.trim())
        .filter(Boolean)
      expect(fieldLabels).toEqual(
        expect.arrayContaining([
          '注册证编号',
          '所属公司',
          '产品名称',
          '分类',
          '注册人',
          '型号规格',
          '生产地址',
          '受托企业',
          '实际项目代码',
          '首次获证起始',
          '首次获证截止',
          '生效日期起始',
          '生效日期截止',
          '有效期起始',
          '有效期截止'
        ])
      )
      await selectVisibleOption(page, '产品名称')
      await oldForm
        .locator('.table-multi-filter-field[data-filter-key="productName"] .table-multi-filter-field__value input:visible')
        .first()
        .fill('REGCERT-E2E-NO-MATCH')

      await addOldFilterCondition(page, oldForm, '首次获证起始', 'firstObtainedStart', '2026-06-01')
      await addOldFilterCondition(page, oldForm, '首次获证截止', 'firstObtainedEnd', '2026-09-01')

      const filteredResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, '/old-index/page'),
        { timeout: 60000 }
      )
      await oldForm.getByRole('button', { name: '查询' }).click()
      const filteredResponse = await filteredResponsePromise
      const filteredPayload = await readJsonResponse(filteredResponse)
      expect(isBusinessOk(filteredPayload), `filtered old-index code ${filteredPayload.code}`).toBe(true)
      const query = new URL(filteredResponse.url()).searchParams
      expect(query.get('productName')).toBe('REGCERT-E2E-NO-MATCH')
      expect(query.get('firstObtainedStart')).toBe('2026-06-01')
      expect(query.get('firstObtainedEnd')).toBe('2026-09-01')
      evidence.oldIndexFilteredQuery = {
        productName: query.get('productName'),
        firstObtainedStart: query.get('firstObtainedStart'),
        firstObtainedEnd: query.get('firstObtainedEnd'),
        total: extractPageResult(filteredPayload).total
      }

      const avatarFailures = evidence.failedResponses.filter(
        (failure) =>
          failure.method === 'GET' &&
          failure.status === 502 &&
          /^\/user\/avatar\//.test(failure.path)
      )
      const unexpectedFailedResponses = evidence.failedResponses.filter(
        (failure) => !avatarFailures.includes(failure)
      )
      const unexplainedConsoleErrors = evidence.consoleErrors.filter(
        (message) =>
          !(
            message ===
              'Failed to load resource: the server responded with a status of 502 (Bad Gateway)' &&
            avatarFailures.length > 0
          )
      )
      evidence.ignoredAssetFailures = avatarFailures
      evidence.unexpectedFailedResponses = unexpectedFailedResponses
      evidence.unexplainedConsoleErrors = unexplainedConsoleErrors
      expect(evidence.pageErrors, 'targeted flow must not emit page errors').toEqual([])
      expect(unexpectedFailedResponses, 'targeted flow must not emit unexpected failed responses').toEqual([])
      expect(unexplainedConsoleErrors, 'targeted flow must not emit unexplained console errors').toEqual([])
      expect(evidence.writeRequests, 'targeted read-only flow must not write registration data').toEqual([])

      evidence.status = 'PASS'
      writeResult(evidence)
    } catch (error) {
      evidence.status = 'FAIL'
      evidence.error = error.stack || error.message
      writeResult(evidence)
      throw error
    }
  })
})
