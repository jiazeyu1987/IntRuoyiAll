const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_ID = '20260804-dcc-upload-approval-chain-projection'
const TARGET_PATH = '/dcc/controlled-file/upload'
const TARGET_CATEGORY_CODE = process.env.DCC_UPLOAD_APPROVAL_CHAIN_E2E_CATEGORY_CODE || 'INTAUTH-26'
const TARGET_CATEGORY_NAME = process.env.DCC_UPLOAD_APPROVAL_CHAIN_E2E_CATEGORY_NAME || '技术调研报告'
const OUTPUT_DIR = path.join(WORKSPACE_ROOT, 'output', 'playwright', TASK_ID)
const EVIDENCE_PATH = path.join(OUTPUT_DIR, 'dcc-upload-approval-chain-projection-real-evidence.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'dcc-upload-approval-chain-projection-real.png')

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  const result = {}
  const content = fs.readFileSync(filePath, 'utf8')
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) {
      continue
    }
    const separatorIndex = trimmed.indexOf('=')
    if (separatorIndex <= 0) {
      continue
    }
    const key = trimmed.slice(0, separatorIndex).trim()
    let value = trimmed.slice(separatorIndex + 1).trim()
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1)
    }
    result[key] = value
  }
  return result
}

const baseEnv = readEnvFile(path.join(FRONTEND_ROOT, '.env'))
const localEnv = readEnvFile(path.join(FRONTEND_ROOT, '.env.local'))
const BASE_URL = (
  process.env.DCC_UPLOAD_APPROVAL_CHAIN_E2E_BASE_URL ||
  `http://127.0.0.1:${localEnv.VITE_PORT || baseEnv.VITE_PORT || '8081'}`
).replace(/\/+$/, '')
const TENANT =
  process.env.DCC_UPLOAD_APPROVAL_CHAIN_E2E_TENANT ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT ||
  ''
const USERNAME =
  process.env.DCC_UPLOAD_APPROVAL_CHAIN_E2E_USERNAME ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME ||
  ''
const PASSWORD =
  process.env.DCC_UPLOAD_APPROVAL_CHAIN_E2E_PASSWORD ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD ||
  ''

function ensureOutputDir() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

function writeEvidence(evidence) {
  ensureOutputDir()
  fs.writeFileSync(EVIDENCE_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

function assertPrerequisites() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/, 'E2E must target local frontend only')
  assert.ok(TENANT, 'default login tenant is missing from env')
  assert.ok(USERNAME, 'default login username is missing from env')
  assert.ok(PASSWORD, 'default login password is missing from env')
}

function unwrapCacheValue(raw) {
  if (!raw) {
    return ''
  }
  let current = raw
  for (let index = 0; index < 8; index += 1) {
    if (typeof current === 'string') {
      const trimmed = current.trim()
      if (!trimmed) {
        return ''
      }
      try {
        current = JSON.parse(trimmed)
        continue
      } catch {
        return trimmed.replace(/^"(.*)"$/, '$1')
      }
    }
    if (current && typeof current === 'object') {
      for (const key of ['accessToken', 'v', 'value', 'token', 'data']) {
        if (Object.prototype.hasOwnProperty.call(current, key)) {
          current = current[key]
          continue
        }
      }
    }
    return String(current || '')
  }
  return String(current || '')
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'commit',
    timeout: 60000
  })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}: ${loginPayload.msg || ''}`)
  const accessToken = loginPayload.data?.accessToken
  assert.ok(accessToken, 'login response must include accessToken')

  await page.waitForFunction(
    () => {
      const raw = window.localStorage.getItem('ACCESS_TOKEN')
      return Boolean(raw && raw !== 'null' && raw !== 'undefined')
    },
    undefined,
    { timeout: 60000 }
  )
  const tenantId = await page.waitForFunction(
    () => {
      const raw = window.localStorage.getItem('tenantId')
      return raw && raw !== 'null' && raw !== 'undefined' ? raw : false
    },
    undefined,
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/index`, { waitUntil: 'commit', timeout: 60000 })
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(unwrapCacheValue(await tenantId.jsonValue())),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
}

async function fetchReadonlyApi(page, headers, pathname, label) {
  const result = await page.evaluate(
    async ({ requestPath, requestHeaders }) => {
      const response = await fetch(requestPath, { headers: requestHeaders })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    { requestPath: pathname, requestHeaders: headers }
  )
  assert.equal(result.status, 200, `${label} HTTP ${result.status}`)
  assert.ok([0, 200].includes(result.payload?.code), `${label} business code ${result.payload?.code}: ${result.payload?.msg || ''}`)
  return result.payload.data
}

function unwrapRows(data, label) {
  if (Array.isArray(data)) {
    return data
  }
  if (Array.isArray(data?.list)) {
    return data.list
  }
  throw new Error(`${label} must return an array or paged list`)
}

function buildTaxonomyPath(rows, taxonomyId) {
  const byId = new Map(rows.map((row) => [Number(row.id), row]))
  const pathRows = []
  let current = byId.get(Number(taxonomyId))
  const visited = new Set()
  while (current) {
    const currentId = Number(current.id)
    if (visited.has(currentId)) {
      throw new Error(`taxonomy cycle detected at ${currentId}`)
    }
    visited.add(currentId)
    pathRows.unshift(current)
    const parentId = Number(current.parentId || 0)
    current = parentId > 0 ? byId.get(parentId) : null
  }
  return pathRows
}

function findTargetCategory(categories) {
  const activeRows = categories.filter((category) => category && category.active !== false)
  const exactCode = activeRows.filter((category) => String(category.code || '').trim() === TARGET_CATEGORY_CODE)
  if (exactCode.length === 1) {
    return exactCode[0]
  }
  const exactName = activeRows.filter((category) => String(category.name || '').trim() === TARGET_CATEGORY_NAME)
  assert.equal(
    exactName.length,
    1,
    `BLOCKER: expected exactly one active DCC file category ${TARGET_CATEGORY_CODE}/${TARGET_CATEGORY_NAME}, codeMatches=${exactCode.length}, nameMatches=${exactName.length}`
  )
  return exactName[0]
}

function formItem(page, label) {
  return page
    .locator('.el-form-item')
    .filter({ has: page.locator('.el-form-item__label').filter({ hasText: label }) })
    .first()
}

async function selectCascaderPath(page, label, segments) {
  const item = formItem(page, label)
  await item.waitFor({ state: 'visible', timeout: 30000 })
  let lastError = null
  for (let attempt = 0; attempt < 4; attempt += 1) {
    try {
      await item.locator('.el-cascader').first().click()
      for (const [index, segment] of segments.entries()) {
        const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).first()
        await node.waitFor({ state: 'visible', timeout: 15000 })
        if (index === segments.length - 1) {
          const selector = node.locator('.el-radio__input, .el-checkbox__input').first()
          if (await selector.count()) {
            await selector.click({ force: true })
          } else {
            await node.click()
          }
        } else {
          await node.click()
        }
        await page.waitForTimeout(250)
      }
      await page.keyboard.press('Escape').catch(() => undefined)
      await item.getByText(segments[segments.length - 1], { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
      return
    } catch (error) {
      lastError = error
      await page.keyboard.press('Escape').catch(() => undefined)
      await page.waitForTimeout(1000)
    }
  }
  throw lastError || new Error(`failed to select cascader path ${segments.join(' / ')}`)
}

function requestPathname(requestOrResponse) {
  try {
    return new URL(requestOrResponse.url()).pathname
  } catch {
    return ''
  }
}

;(async () => {
  assertPrerequisites()
  const evidence = {
    taskId: TASK_ID,
    startedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    tenantLabel: `${TENANT}/${USERNAME}`,
    targetPath: TARGET_PATH,
    targetCategoryCode: TARGET_CATEGORY_CODE,
    targetCategoryName: TARGET_CATEGORY_NAME,
    runtimeProjection: null,
    uiPreflightText: '',
    dccWriteRequests: [],
    targetNetworkFailures: [],
    consoleErrors: [],
    pageErrors: [],
    screenshot: SCREENSHOT_PATH
  }

  const browser = await chromium.launch({ headless: process.env.DCC_UPLOAD_APPROVAL_CHAIN_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  page.on('console', (message) => {
    if (message.type() === 'error') {
      evidence.consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    evidence.pageErrors.push(error.message)
  })
  page.on('request', (request) => {
    const pathname = requestPathname(request)
    if (pathname.startsWith('/admin-api/dcc/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      evidence.dccWriteRequests.push({ method: request.method(), pathname })
    }
  })
  page.on('response', async (response) => {
    const pathname = requestPathname(response)
    if (pathname.startsWith('/admin-api/dcc/') && response.status() >= 400) {
      evidence.targetNetworkFailures.push({ method: response.request().method(), pathname, status: response.status() })
    }
  })

  try {
    const authHeaders = await login(page)
    const [categoryData, taxonomyData] = await Promise.all([
      fetchReadonlyApi(page, authHeaders, '/admin-api/dcc/file-categories', 'file categories'),
      fetchReadonlyApi(page, authHeaders, '/admin-api/dcc/file-type-taxonomies', 'file type taxonomies')
    ])
    const categories = unwrapRows(categoryData, 'file categories')
    const taxonomies = unwrapRows(taxonomyData, 'file type taxonomies')
    const category = findTargetCategory(categories)
    const approvalPositionIds = Array.isArray(category.approvalPositionIds) ? category.approvalPositionIds : []
    const signoffPositionIds = Array.isArray(category.signoffPositionIds) ? category.signoffPositionIds : []
    assert.ok(approvalPositionIds.length > 0, `runtime category ${category.code || category.name} must return approvalPositionIds`)
    assert.ok(signoffPositionIds.length > 0, `runtime category ${category.code || category.name} must return signoffPositionIds`)
    const taxonomyPath = buildTaxonomyPath(taxonomies, category.fileTypeTaxonomyId)
      .map((row) => String(row.name || '').trim())
      .filter(Boolean)
    assert.ok(taxonomyPath.length >= 3, `category ${category.code || category.name} must bind an active 3-level taxonomy path`)
    evidence.runtimeProjection = {
      categoryId: Number(category.id),
      categoryCode: category.code,
      categoryName: category.name,
      taxonomyId: Number(category.fileTypeTaxonomyId),
      taxonomyPath,
      approvalPositionCount: approvalPositionIds.length,
      signoffPositionCount: signoffPositionIds.length
    }

    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
    await selectCascaderPath(page, '文件分类', taxonomyPath)

    const preflightPanel = page.locator('[data-testid="dcc-upload-preflight-panel"]').first()
    await preflightPanel.waitFor({ state: 'visible', timeout: 30000 })
    await page.waitForFunction(
      ({ approvalCount, signoffCount }) => {
        const panel = document.querySelector('[data-testid="dcc-upload-preflight-panel"]')
        const text = panel?.innerText || ''
        return (
          text.includes(`审批岗位 ${approvalCount} 个`) &&
          text.includes(`会签/签核岗位 ${signoffCount} 个`) &&
          text.includes('审批人链路已具备')
        )
      },
      { approvalCount: approvalPositionIds.length, signoffCount: signoffPositionIds.length },
      { timeout: 60000 }
    )
    const preflightText = await preflightPanel.innerText()
    assert.ok(preflightText.includes('审批人链路已具备'), 'upload preflight must show approval chain ready')
    assert.ok(!preflightText.includes('请先补齐分类审批链路'), 'upload preflight must not show approval-chain incomplete')
    assert.ok(!preflightText.includes('审批岗位 0 个'), 'upload preflight must not show zero approval positions')
    assert.ok(!preflightText.includes('会签/签核岗位 0 个'), 'upload preflight must not show zero signoff positions')
    evidence.uiPreflightText = preflightText

    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    assert.deepEqual(evidence.dccWriteRequests, [], 'approval-chain E2E must not send DCC write requests')
    assert.deepEqual(evidence.targetNetworkFailures, [], 'target DCC network requests must not fail')
    assert.deepEqual(evidence.pageErrors, [], 'page must not throw runtime errors')

    evidence.finishedAt = new Date().toISOString()
    evidence.status = 'PASS'
    writeEvidence(evidence)
    console.log(`PASS: DCC upload approval-chain projection real E2E evidence=${EVIDENCE_PATH}`)
  } catch (error) {
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true }).catch(() => undefined)
    evidence.finishedAt = new Date().toISOString()
    evidence.status = 'FAIL'
    evidence.error = error.message
    writeEvidence(evidence)
    throw error
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
