const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.DCC_ADMIN_FULL_CONFIG_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  sourceTenant: process.env.DCC_ADMIN_FULL_CONFIG_E2E_SOURCE_TENANT || '芋道源码',
  sourceUsername: process.env.DCC_ADMIN_FULL_CONFIG_E2E_SOURCE_USERNAME || 'admin',
  sourcePassword: process.env.DCC_ADMIN_FULL_CONFIG_E2E_SOURCE_PASSWORD || 'admin123',
  targetTenant: process.env.DCC_ADMIN_FULL_CONFIG_E2E_TARGET_TENANT || '测试租户',
  targetUsername: process.env.DCC_ADMIN_FULL_CONFIG_E2E_TARGET_USERNAME || 'aoteman',
  targetPassword: process.env.DCC_ADMIN_FULL_CONFIG_E2E_TARGET_PASSWORD || '111111',
  headed: process.env.DCC_ADMIN_FULL_CONFIG_E2E_HEADED === '1'
}

const repoRoot = path.resolve(__dirname, '../../..')
const taskDir = path.join(repoRoot, 'doc/tasks/20260630-dcc-admin-full-config-package/e2e-artifacts')
const sourcePackagePath = path.join(taskDir, 'dcc-admin-full-config-source.json')
const targetPackagePath = path.join(taskDir, 'dcc-admin-full-config-target.json')
const sourceScreenshotPath = path.join(taskDir, 'dcc-admin-full-config-source.png')
const targetScreenshotPath = path.join(taskDir, 'dcc-admin-full-config-target.png')
const resultPath = path.join(taskDir, 'dcc-admin-full-config-real-result.json')
const pagePath = '/dcc/controlled-file/admin'
const exportApiPath = '/admin-api/dcc/file-categories/admin-config-package/export'
const importApiPath = '/admin-api/dcc/file-categories/admin-config-package/import'
const packageVersion = 'dcc-admin-full-config-package.v1'

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true })
}

async function settle(page, timeout = 30000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
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
  throw new Error(`No visible input found for ${label}`)
}

async function collectPageDiagnostics(page) {
  const url = page.url()
  const body = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
  const buttons = await page.evaluate(() =>
    Array.from(document.querySelectorAll('button'))
      .filter((button) => button.offsetParent)
      .map((button) => ({
        text: (button.textContent || '').replace(/\s+/g, ' ').trim(),
        disabled: button.disabled
      }))
  ).catch(() => [])
  return {
    url,
    body: body.slice(0, 1600),
    buttons: buttons.slice(0, 40)
  }
}

async function login(page, identity, targetPath) {
  const loginUrl = `${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`
  await page.goto(loginUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  }).catch(() => null)
  await page.goto(loginUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)

  if (page.url().includes('/login')) {
    const form = page.locator('form.login-form:visible').first()
    await form.waitFor({ state: 'visible', timeout: 30000 })

    const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
    if (await tenantInput.count()) {
      await tenantInput.fill(identity.tenant)
      await tenantInput.press('Enter')
    } else {
      await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), identity.tenant, 'tenant')
    }

    const usernameInput = form.locator('input.el-input__inner:not([role="combobox"])').first()
    if (await usernameInput.count()) {
      await usernameInput.fill('')
      await usernameInput.fill(identity.username)
    } else {
      await fillFirstVisible(form.locator('input[placeholder="请输入用户名"]'), identity.username, 'username')
    }
    await fillFirstVisible(form.locator('input[type="password"]'), identity.password, 'password')

    const [loginResponse] = await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
        { timeout: 60000 }
      ),
      form.getByRole('button', { name: '登录' }).click()
    ])
    const loginPayload = await loginResponse.json().catch(() => null)
    assert.ok(
      loginResponse.ok() && loginPayload && [0, 200].includes(loginPayload.code),
      `login failed for ${identity.tenant}/${identity.username}: ${JSON.stringify(loginPayload)}`
    )
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  }

  await page.goto(`${config.baseUrl}${targetPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page, 60000)
}

async function readResponse(response) {
  const headers = response.headers()
  const contentType = String(headers['content-type'] || '')
  const buffer = Buffer.from(await response.body())
  if (contentType.includes('application/json')) {
    return {
      type: 'json',
      buffer,
      json: JSON.parse(buffer.toString('utf8'))
    }
  }
  return { type: 'binary', buffer }
}

function parseExportPackagePayload(payload, identity, actionLabel) {
  if (payload.type === 'binary') {
    return {
      buffer: payload.buffer,
      packageJson: JSON.parse(payload.buffer.toString('utf8'))
    }
  }
  if (
    payload.type === 'json' &&
    payload.json &&
    typeof payload.json === 'object' &&
    payload.json.packageVersion === packageVersion
  ) {
    return {
      buffer: Buffer.from(JSON.stringify(payload.json, null, 2), 'utf8'),
      packageJson: payload.json
    }
  }
  throw new Error(
    `${identity.tenant}/${identity.username} ${actionLabel} returned unexpected payload: ${JSON.stringify(payload.json)}`
  )
}

async function waitForVisibleButton(page, label, identity) {
  const button = page.getByRole('button', { name: label }).first()
  try {
    await button.waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    const diagnostics = await collectPageDiagnostics(page)
    throw new Error(
      `${identity.tenant}/${identity.username} missing button ${label}: ${error.message}; diagnostics=${JSON.stringify(diagnostics)}`
    )
  }
  assert.equal(await button.isEnabled(), true, `${label} button must be enabled for ${identity.tenant}/${identity.username}`)
  return button
}

function parseCommonResultPayload(payload, actionLabel) {
  assert.ok(payload && typeof payload === 'object', `${actionLabel} response must be JSON object`)
  assert.ok([0, 200].includes(payload.code), `${actionLabel} failed: ${JSON.stringify(payload)}`)
  return payload.data || {}
}

function summarizePackage(pkg) {
  const categories = Array.isArray(pkg.categories) ? pkg.categories : []
  const directories = Array.isArray(pkg.directories) ? pkg.directories : []
  const approvalPositions = Array.isArray(pkg.approvalPositions) ? pkg.approvalPositions : []
  return {
    approvalPositionCount: approvalPositions.length,
    directoryCount: directories.length,
    directoryAccessRuleCount: directories.reduce(
      (total, item) => total + (Array.isArray(item.accessRules) ? item.accessRules.length : 0),
      0
    ),
    categoryCount: categories.length,
    permissionRuleCount: categories.reduce(
      (total, item) => total + (Array.isArray(item.permissionRules) ? item.permissionRules.length : 0),
      0
    ),
    approvalMatrixRuleCount: categories.reduce(
      (total, item) => total + (Array.isArray(item.approvalMatrix?.rules) ? item.approvalMatrix.rules.length : 0),
      0
    ),
    viewMatrixRuleCount: categories.reduce(
      (total, item) => total + (Array.isArray(item.viewMatrix?.rules) ? item.viewMatrix.rules.length : 0),
      0
    ),
    distributionRuleCount: categories.reduce(
      (total, item) => total + (Array.isArray(item.distributionRules) ? item.distributionRules.length : 0),
      0
    ),
    trainingRuleCount: categories.reduce(
      (total, item) => total + (Array.isArray(item.trainingRules) ? item.trainingRules.length : 0),
      0
    )
  }
}

function canonicalDigest(value) {
  const hash = crypto.createHash('sha256')
  if (Array.isArray(value)) {
    const elementDigests = value.map((item) => canonicalDigest(item)).sort()
    hash.update('array:')
    for (const digest of elementDigests) {
      hash.update(digest)
      hash.update('\n')
    }
    return hash.digest('hex')
  }
  if (value && typeof value === 'object') {
    hash.update('object:')
    for (const key of Object.keys(value).sort()) {
      hash.update(key)
      hash.update('=')
      hash.update(canonicalDigest(value[key]))
      hash.update('\n')
    }
    return hash.digest('hex')
  }
  hash.update(`primitive:${JSON.stringify(value)}`)
  return hash.digest('hex')
}

function comparePackages(sourcePackage, targetPackage) {
  const sourceDigest = canonicalDigest(sourcePackage)
  const targetDigest = canonicalDigest(targetPackage)
  assert.equal(targetDigest, sourceDigest, 'target re-export package must match source package business content')
  return { sourceDigest, targetDigest }
}

async function exportPackage(identity, packagePath, screenshotPath) {
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    locale: 'zh-CN',
    viewport: { width: 1440, height: 960 }
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page, identity, pagePath)
    const exportButton = await waitForVisibleButton(page, '导出数据包', identity)
    const [response] = await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes(exportApiPath) && response.request().method() === 'GET',
        { timeout: 60000 }
      ),
      exportButton.click()
    ])
    const payload = await readResponse(response)
    const exported = parseExportPackagePayload(payload, identity, 'export')
    assert.equal(response.status(), 200, `${identity.tenant}/${identity.username} export http status must be 200`)
    fs.writeFileSync(packagePath, exported.buffer)
    assert.ok(fs.statSync(packagePath).size > 0, `${packagePath} must not be empty`)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    assert.deepEqual(pageErrors, [], `page errors were emitted during export: ${pageErrors.join(' | ')}`)
    return exported.packageJson
  } finally {
    await context.close()
    await browser.close()
  }
}

async function importAndExportTargetPackage(sourcePath, expectedSourcePackage) {
  const identity = {
    tenant: config.targetTenant,
    username: config.targetUsername,
    password: config.targetPassword
  }
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    locale: 'zh-CN',
    viewport: { width: 1440, height: 960 }
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page, identity, pagePath)
    await waitForVisibleButton(page, '导入数据包', identity)
    const importInput = page.locator('input[type="file"][accept*=".json"]').first()
    const [importResponse] = await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes(importApiPath) && response.request().method() === 'POST',
        { timeout: 300000 }
      ),
      importInput.setInputFiles(sourcePath)
    ])
    const importPayload = await readResponse(importResponse)
    assert.equal(importPayload.type, 'json', 'import response must be JSON')
    assert.equal(importResponse.status(), 200, `import http status must be 200: ${JSON.stringify(importPayload.json)}`)
    const importSummary = parseCommonResultPayload(importPayload.json, 'import package')
    const expectedSummary = summarizePackage(expectedSourcePackage)
    assert.equal(importSummary.approvalPositionCount, expectedSummary.approvalPositionCount, 'import approvalPositionCount mismatch')
    assert.equal(importSummary.directoryCount, expectedSummary.directoryCount, 'import directoryCount mismatch')
    assert.equal(importSummary.directoryAccessRuleCount, expectedSummary.directoryAccessRuleCount, 'import directoryAccessRuleCount mismatch')
    assert.equal(importSummary.categoryCount, expectedSummary.categoryCount, 'import categoryCount mismatch')
    assert.equal(importSummary.permissionRuleCount, expectedSummary.permissionRuleCount, 'import permissionRuleCount mismatch')
    assert.equal(importSummary.approvalMatrixRuleCount, expectedSummary.approvalMatrixRuleCount, 'import approvalMatrixRuleCount mismatch')
    assert.equal(importSummary.viewMatrixRuleCount, expectedSummary.viewMatrixRuleCount, 'import viewMatrixRuleCount mismatch')
    assert.equal(importSummary.distributionRuleCount, expectedSummary.distributionRuleCount, 'import distributionRuleCount mismatch')
    assert.equal(importSummary.trainingRuleCount, expectedSummary.trainingRuleCount, 'import trainingRuleCount mismatch')
    assert.ok(importSummary.removedApprovalPositionCount >= 0, 'removedApprovalPositionCount must be non-negative')
    assert.ok(importSummary.removedDirectoryCount >= 0, 'removedDirectoryCount must be non-negative')
    assert.ok(importSummary.removedCategoryCount >= 0, 'removedCategoryCount must be non-negative')
    await settle(page, 120000)

    const exportButton = await waitForVisibleButton(page, '导出数据包', identity)
    const [exportResponse] = await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes(exportApiPath) && response.request().method() === 'GET',
        { timeout: 60000 }
      ),
      exportButton.click()
    ])
    const exportPayload = await readResponse(exportResponse)
    const exported = parseExportPackagePayload(exportPayload, identity, 'target export')
    assert.equal(exportResponse.status(), 200, 'target export http status must be 200')
    fs.writeFileSync(targetPackagePath, exported.buffer)
    assert.ok(fs.statSync(targetPackagePath).size > 0, `${targetPackagePath} must not be empty`)
    await page.screenshot({ path: targetScreenshotPath, fullPage: true })
    assert.deepEqual(pageErrors, [], `page errors were emitted during target import/export: ${pageErrors.join(' | ')}`)
    return {
      importSummary,
      targetPackage: exported.packageJson
    }
  } finally {
    await context.close()
    await browser.close()
  }
}

async function main() {
  ensureDir(taskDir)
  const sourceIdentity = {
    tenant: config.sourceTenant,
    username: config.sourceUsername,
    password: config.sourcePassword
  }
  const sourcePackage = await exportPackage(sourceIdentity, sourcePackagePath, sourceScreenshotPath)
  const { importSummary, targetPackage } = await importAndExportTargetPackage(sourcePackagePath, sourcePackage)
  const packageDigest = comparePackages(sourcePackage, targetPackage)

  const result = {
    status: 'PASS',
    baseUrl: config.baseUrl,
    sourceTenant: config.sourceTenant,
    sourceUsername: config.sourceUsername,
    targetTenant: config.targetTenant,
    targetUsername: config.targetUsername,
    sourcePackagePath,
    targetPackagePath,
    sourceScreenshotPath,
    targetScreenshotPath,
    sourceSummary: summarizePackage(sourcePackage),
    importSummary,
    packageDigest
  }
  fs.writeFileSync(resultPath, JSON.stringify(result, null, 2), 'utf8')
  console.log(`PASS: dcc admin full config real roundtrip -> ${resultPath}`)
}

main().catch((error) => {
  console.error(error.stack || error.message || error)
  process.exit(1)
})
