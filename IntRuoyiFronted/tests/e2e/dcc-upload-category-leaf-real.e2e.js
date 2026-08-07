const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_ID = '20260803-controlled-file-category-missing'
const TARGET_PATH = '/dcc/controlled-file/upload'
const OLD_DIRECTORY_BINDING_MESSAGE = '当前文件类别未绑定提交目录，请先在 DCC 文件类别维护目录绑定'
const AUTO_UNCLASSIFIED_MESSAGE = '当前文件类别未绑定提交目录，系统将自动提交到未分类目录。'
const OUTPUT_DIR = path.resolve(
  process.env.DCC_UPLOAD_CATEGORY_LEAF_E2E_OUTPUT_DIR ||
    path.join(WORKSPACE_ROOT, 'output', 'playwright', TASK_ID)
)
const EVIDENCE_PATH = path.join(OUTPUT_DIR, 'dcc-upload-category-leaf-real-evidence.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'dcc-upload-category-leaf-real.png')

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
  process.env.DCC_UPLOAD_CATEGORY_LEAF_E2E_BASE_URL ||
  `http://127.0.0.1:${localEnv.VITE_PORT || '8081'}`
).replace(/\/+$/, '')
const TENANT =
  process.env.DCC_UPLOAD_CATEGORY_LEAF_E2E_TENANT ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT ||
  ''
const USERNAME =
  process.env.DCC_UPLOAD_CATEGORY_LEAF_E2E_USERNAME ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME ||
  ''
const PASSWORD =
  process.env.DCC_UPLOAD_CATEGORY_LEAF_E2E_PASSWORD ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD ||
  ''
const PREFERRED_TAXONOMY_PATH = (
  process.env.DCC_UPLOAD_CATEGORY_LEAF_E2E_TAXONOMY_PATH ||
  '技术文档/设计和开发输入阶段/专利检索与分析报告'
)
  .split('/')
  .map((item) => item.trim())
  .filter(Boolean)

function assertPrerequisites() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/, 'E2E must target local frontend only')
  assert.ok(TENANT, 'default login tenant is missing from env')
  assert.ok(USERNAME, 'default login username is missing from env')
  assert.ok(PASSWORD, 'default login password is missing from env')
  assert.ok(PREFERRED_TAXONOMY_PATH.length >= 3, 'preferred taxonomy path must contain at least 3 segments')
}

function ensureOutputDir() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

function writeEvidence(evidence) {
  ensureOutputDir()
  fs.writeFileSync(EVIDENCE_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
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
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
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

function isApiResponse(response, pathname) {
  if (response.request().method() !== 'GET') {
    return false
  }
  try {
    return new URL(response.url()).pathname === pathname
  } catch {
    return false
  }
}

async function unwrapResponseData(response, label) {
  assert.equal(response.ok(), true, `${label} HTTP ${response.status()}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `${label} business code ${payload.code}: ${payload.msg || ''}`)
  return payload.data
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

function samePath(left, right) {
  return left.length === right.length && left.every((item, index) => item === right[index])
}

function chooseUnclassifiedCandidate(categories, taxonomies) {
  const activeTaxonomies = taxonomies.filter((row) => row && row.active !== false && row.id)
  const activeChildCount = new Map()
  for (const row of activeTaxonomies) {
    const parentId = Number(row.parentId || 0)
    activeChildCount.set(parentId, (activeChildCount.get(parentId) || 0) + 1)
  }
  const categoriesByTaxonomy = new Map()
  for (const category of categories) {
    if (!category || category.active === false || !category.fileTypeTaxonomyId) {
      continue
    }
    const taxonomyId = Number(category.fileTypeTaxonomyId)
    if (!categoriesByTaxonomy.has(taxonomyId)) {
      categoriesByTaxonomy.set(taxonomyId, [])
    }
    categoriesByTaxonomy.get(taxonomyId).push(category)
  }

  const candidates = []
  for (const [taxonomyId, boundCategories] of categoriesByTaxonomy.entries()) {
    if (boundCategories.length !== 1) {
      continue
    }
    const category = boundCategories[0]
    if (category.directoryId) {
      continue
    }
    const pathRows = buildTaxonomyPath(activeTaxonomies, taxonomyId)
    const pathNames = pathRows.map((row) => String(row.name || '').trim()).filter(Boolean)
    if (pathNames.length < 3 || (activeChildCount.get(taxonomyId) || 0) > 0) {
      continue
    }
    candidates.push({
      categoryId: Number(category.id),
      categoryName: category.name,
      categoryCode: category.code,
      taxonomyId,
      taxonomyPath: pathNames,
      taxonomyLeafName: pathNames[pathNames.length - 1]
    })
  }

  const preferred = candidates.find((candidate) => samePath(candidate.taxonomyPath, PREFERRED_TAXONOMY_PATH))
  return preferred || candidates[0]
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
  await item.locator('.el-cascader').first().click()
  for (const [index, segment] of segments.entries()) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).first()
    await node.waitFor({ state: 'visible', timeout: 30000 })
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
  await item.getByText(segments.join(' / '), { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

;(async () => {
  assertPrerequisites()
  ensureOutputDir()
  const evidence = {
    taskId: TASK_ID,
    startedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    tenantLabel: `${TENANT}/${USERNAME}`,
    targetPath: TARGET_PATH,
    preferredTaxonomyPath: PREFERRED_TAXONOMY_PATH,
    selectedCandidate: null,
    uploadDirectoryTree: null,
    writeRequests: [],
    targetNetworkFailures: [],
    consoleErrors: [],
    pageErrors: [],
    screenshot: SCREENSHOT_PATH
  }

  const browser = await chromium.launch({ headless: process.env.DCC_UPLOAD_CATEGORY_LEAF_E2E_HEADLESS !== 'false' })
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
    if (request.url().includes('/admin-api/dcc/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      evidence.writeRequests.push({ method: request.method(), url: request.url() })
    }
  })
  page.on('response', async (response) => {
    if (!response.url().includes('/admin-api/dcc/')) {
      return
    }
    if (response.status() >= 400) {
      evidence.targetNetworkFailures.push({ method: response.request().method(), url: response.url(), status: response.status() })
    }
  })

  try {
    await login(page)
    const categoryResponsePromise = page.waitForResponse(
      (response) => isApiResponse(response, '/admin-api/dcc/file-categories'),
      { timeout: 60000 }
    )
    const taxonomyResponsePromise = page.waitForResponse(
      (response) => isApiResponse(response, '/admin-api/dcc/file-type-taxonomies'),
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    const [categories, taxonomies] = await Promise.all([
      categoryResponsePromise.then((response) => unwrapResponseData(response, 'file categories')),
      taxonomyResponsePromise.then((response) => unwrapResponseData(response, 'file type taxonomies'))
    ])
    await settle(page)

    const candidate = chooseUnclassifiedCandidate(categories || [], taxonomies || [])
    if (!candidate) {
      throw new Error('BLOCKER: 当前登录租户没有“唯一可上传、文件分类叶子节点绑定、且未绑定提交目录”的 DCC 文件类别，无法验证自动落位未分类目录。')
    }
    evidence.selectedCandidate = candidate

    const directoryResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/upload-directory-tree') &&
        response.url().includes(`categoryId=${candidate.categoryId}`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await selectCascaderPath(page, '文件分类', candidate.taxonomyPath)
    const directoryResponse = await directoryResponsePromise
    const directoryPayload = await directoryResponse.json()
    assert.ok([0, 200].includes(directoryPayload.code), `directory tree business code ${directoryPayload.code}: ${directoryPayload.msg || ''}`)
    evidence.uploadDirectoryTree = directoryPayload.data

    const categoryItem = formItem(page, '文件类别')
    const leafDisplay = categoryItem.locator('[data-testid="dcc-upload-category-leaf-display"]').first()
    await leafDisplay.waitFor({ state: 'visible', timeout: 30000 })
    const categoryText = await categoryItem.innerText()
    assert.ok(categoryText.includes(candidate.taxonomyLeafName), `readonly file category must show taxonomy leaf ${candidate.taxonomyLeafName}`)
    assert.equal(await categoryItem.locator('.el-select').count(), 0, 'controlled upload file category must not render an editable select')
    assert.ok(!categoryText.includes('自动取文件分类最后一级'), 'readonly file category must not show the taxonomy path helper')
    assert.equal(await categoryItem.locator('.el-alert').count(), 0, 'readonly file category must not show a permission preflight alert')

    const directoryItem = formItem(page, '提交目录')
    await directoryItem.waitFor({ state: 'visible', timeout: 30000 })
    const directoryText = await directoryItem.innerText()
    assert.equal(directoryPayload.data?.defaultUnclassified, true, 'directory tree must mark defaultUnclassified=true')
    assert.ok(String(directoryPayload.data?.bindingDirectoryPath || '').includes('未分类'), 'bindingDirectoryPath must point to 未分类')
    assert.ok(directoryText.includes('未分类'), 'directory section must display 未分类 path')
    assert.ok(directoryText.includes(AUTO_UNCLASSIFIED_MESSAGE), 'directory section must show automatic unclassified message')

    const bodyText = await page.locator('body').innerText()
    assert.ok(!bodyText.includes(OLD_DIRECTORY_BINDING_MESSAGE), 'old manual directory-binding blocker message must not be visible')
    assert.deepEqual(evidence.writeRequests, [], 'readonly E2E must not send DCC write requests')
    assert.deepEqual(evidence.targetNetworkFailures, [], 'target DCC network requests must not fail')
    assert.deepEqual(evidence.pageErrors, [], 'page must not throw runtime errors')
    assert.deepEqual(evidence.consoleErrors, [], 'page must not log console errors')

    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.finishedAt = new Date().toISOString()
    evidence.status = 'PASS'
    writeEvidence(evidence)
    console.log(`PASS: DCC upload category leaf real E2E evidence=${EVIDENCE_PATH}`)
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
