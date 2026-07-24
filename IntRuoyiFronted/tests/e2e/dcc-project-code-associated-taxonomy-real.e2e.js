const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_PROJECT_CODE_ASSOC_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_PROJECT_CODE_ASSOC_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_PROJECT_CODE_ASSOC_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_PROJECT_CODE_ASSOC_E2E_PASSWORD || '111111'
const PROJECT_CODE = process.env.DCC_PROJECT_CODE_ASSOC_E2E_PROJECT_CODE || 'IKFDA'
const EXPECTED_STAGE = process.env.DCC_PROJECT_CODE_ASSOC_E2E_STAGE || '设计和开发输入阶段'
const EXPECTED_FILE_TYPE = process.env.DCC_PROJECT_CODE_ASSOC_E2E_FILE_TYPE || 'Codex输入E2E叶子'
const PROJECT_CODE_PATH = '/mdm/project-code'
const CHROME_EXECUTABLE = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH

const LEGACY_STAGE_LABELS = [
  '01 plan 策划',
  '02 input 输入',
  '03 output 输出',
  '04 verification 验证',
  '05 validation 确认',
  '06 transfer 转移'
]

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/)
  assert.equal(TENANT, '测试租户')
  assert.equal(USERNAME, 'aoteman')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if (await input.isVisible()) {
      await input.fill('')
      await input.fill(value)
      return
    }
  }
  throw new Error(`missing visible input: ${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }
  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"]):visible'), USERNAME, 'username')
  await fillFirstVisible(form.locator('input[type="password"]'), PASSWORD, 'password')
  const [response] = await Promise.all([
    page.waitForResponse(
      (item) => item.url().includes('/system/auth/login') && item.request().method() === 'POST',
      { timeout: 60000 }
    ),
    form.getByRole('button', { name: '登录' }).click()
  ])
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && payload && [0, 200].includes(payload.code), `login failed: ${JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) return ''
  const normalizeString = (value) => {
    let current = value || ''
    for (let index = 0; index < 3; index += 1) {
      const trimmed = String(current).trim()
      if (!trimmed.startsWith('"')) return trimmed
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed !== 'string' || parsed === current) return trimmed.replace(/^"(.*)"$/, '$1')
        current = parsed
      } catch {
        return trimmed.replace(/^"(.*)"$/, '$1')
      }
    }
    return String(current).trim()
  }
  const unwrap = (value) => {
    let current = value
    for (let index = 0; index < 6; index += 1) {
      if (!current || typeof current !== 'object') {
        return typeof current === 'string' ? normalizeString(current) : current || ''
      }
      if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
        current = current.accessToken
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'v')) {
        current = current.v
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'value')) {
        current = current.value
        continue
      }
      return current
    }
    return current || ''
  }
  try {
    return unwrap(JSON.parse(raw))
  } catch {
    return normalizeString(raw)
  }
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
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function requestJson(page, headers, requestPath) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, {
        method: 'GET',
        headers: requestHeaders
      })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    {
      requestUrl: `${BASE_URL}${requestPath}`,
      requestHeaders: headers
    }
  )
}

function assertApiOk(result, label) {
  assert.equal(result.status, 200, `${label} HTTP failed: ${JSON.stringify(result)}`)
  assert.ok([0, 200].includes(result.payload?.code), `${label} API failed: ${JSON.stringify(result.payload)}`)
}

async function apiGet(page, headers, requestPath, label) {
  const result = await requestJson(page, headers, requestPath)
  assertApiOk(result, label)
  return result.payload.data
}

async function findProjectCode(page, headers) {
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '20',
    projectCode: PROJECT_CODE
  })
  const data = await apiGet(page, headers, `/admin-api/dcc/project-codes/page?${params.toString()}`, 'project code page')
  const exact = (data.list || []).find((item) => String(item.projectCode || '').trim() === PROJECT_CODE)
  assert.ok(exact?.id, `missing project code ${PROJECT_CODE} in test tenant`)
  assert.ok(Number(exact.associatedFileCount || 0) > 0, `project code ${PROJECT_CODE} has no associated files`)
  return exact
}

async function getAllAssociatedFiles(page, headers, projectCodeId) {
  const first = await apiGet(
    page,
    headers,
    `/admin-api/dcc/project-codes/${projectCodeId}/controlled-files/page?pageNo=1&pageSize=200`,
    'associated files page 1'
  )
  const list = [...(first.list || [])]
  const total = Number(first.total || list.length)
  const pageCount = Math.ceil(total / 200)
  for (let pageNo = 2; pageNo <= pageCount; pageNo += 1) {
    const data = await apiGet(
      page,
      headers,
      `/admin-api/dcc/project-codes/${projectCodeId}/controlled-files/page?pageNo=${pageNo}&pageSize=200`,
      `associated files page ${pageNo}`
    )
    list.push(...(data.list || []))
  }
  assert.equal(list.length, total, `associated file page total mismatch: list=${list.length}, total=${total}`)
  return list
}

async function main() {
  assertSafeBoundary()
  const browser = await chromium.launch({
    headless: process.env.DCC_PROJECT_CODE_ASSOC_E2E_HEADED !== '1',
    args: ['--disable-dev-shm-usage'],
    ...(CHROME_EXECUTABLE ? { executablePath: CHROME_EXECUTABLE } : {})
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const writeRequests = []
  page.on('request', (request) => {
    const method = request.method()
    if (request.url().includes('/admin-api/dcc/') && !['GET', 'HEAD'].includes(method)) {
      writeRequests.push(`${method} ${request.url()}`)
    }
  })
  try {
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const headers = await buildAuthHeaders(page)
    const projectCode = await findProjectCode(page, headers)
    const associatedFiles = await getAllAssociatedFiles(page, headers, projectCode.id)
    const expectedFile = associatedFiles.find(
      (file) =>
        String(file.fileTypeLevel2 || '').trim() === EXPECTED_STAGE &&
        String(file.fileTypeLevel3 || '').trim() === EXPECTED_FILE_TYPE
    )
    assert.ok(
      expectedFile,
      `project code ${PROJECT_CODE} must have an associated file in ${EXPECTED_STAGE} / ${EXPECTED_FILE_TYPE}`
    )

    const firstAssociatedResponse = page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/dcc/project-codes/${projectCode.id}/controlled-files/page`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}${PROJECT_CODE_PATH}?projectCodeId=${projectCode.id}`, {
      waitUntil: 'commit',
      timeout: 60000
    })
    await page.getByText('基础数据 / DCC项目代码', { exact: false }).first().waitFor({ state: 'visible' })
    await firstAssociatedResponse

    const stageList = page.locator('[data-testid="dcc-project-code-associated-stage-list"]').first()
    await stageList.waitFor({ state: 'visible', timeout: 60000 })
    const stageButtons = stageList.locator('.dcc-project-code-associated-list-item')
    const stageLabels = await stageButtons
      .locator('.dcc-project-code-associated-item-label')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').trim()).filter(Boolean))
    assert.ok(stageLabels.includes(EXPECTED_STAGE), `stage list must include current taxonomy stage: ${stageLabels.join(' | ')}`)
    for (const legacyLabel of LEGACY_STAGE_LABELS) {
      assert.ok(!stageLabels.includes(legacyLabel), `stage list must not include legacy stage: ${legacyLabel}`)
    }

    await stageButtons.filter({ hasText: EXPECTED_STAGE }).first().click()
    const typeList = page.locator('[data-testid="dcc-project-code-associated-type-list"]').first()
    await typeList.waitFor({ state: 'visible', timeout: 30000 })
    const typeLabels = await typeList
      .locator('.dcc-project-code-associated-item-label')
      .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').trim()).filter(Boolean))
    assert.ok(typeLabels.includes(EXPECTED_FILE_TYPE), `type list must include current taxonomy leaf: ${typeLabels.join(' | ')}`)

    await typeList.locator('.dcc-project-code-associated-list-item').filter({ hasText: EXPECTED_FILE_TYPE }).first().click()
    const table = page.locator('[data-testid="dcc-project-code-associated-file-table"]').first()
    await table.waitFor({ state: 'visible', timeout: 30000 })
    await table.locator('.el-table__body-wrapper tbody tr').first().waitFor({ state: 'visible', timeout: 30000 })
    const tableText = await table.innerText()
    assert.match(tableText, new RegExp(expectedFile.fileNumber || expectedFile.fileName), 'file table must show expected associated file')
    assert.deepEqual(writeRequests, [], `readonly E2E must not issue DCC write requests: ${writeRequests.join('; ')}`)

    console.log(
      `PASS: DCC project-code associated taxonomy real E2E projectCode=${PROJECT_CODE} projectCodeId=${projectCode.id} stage=${EXPECTED_STAGE} fileType=${EXPECTED_FILE_TYPE} fileId=${expectedFile.id}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
