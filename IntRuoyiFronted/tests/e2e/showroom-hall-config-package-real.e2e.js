const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.SHOWROOM_HALL_CONFIG_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  sourceTenant: process.env.SHOWROOM_HALL_CONFIG_E2E_SOURCE_TENANT || '芋道源码',
  sourceUsername: process.env.SHOWROOM_HALL_CONFIG_E2E_SOURCE_USERNAME || 'admin',
  sourcePassword: process.env.SHOWROOM_HALL_CONFIG_E2E_SOURCE_PASSWORD || 'admin123',
  targetTenant: process.env.SHOWROOM_HALL_CONFIG_E2E_TARGET_TENANT || '测试租户',
  targetUsername: process.env.SHOWROOM_HALL_CONFIG_E2E_TARGET_USERNAME || 'aoteman',
  targetPassword: process.env.SHOWROOM_HALL_CONFIG_E2E_TARGET_PASSWORD || '111111',
  headed: process.env.SHOWROOM_HALL_CONFIG_E2E_HEADED === '1'
}

const repoRoot = path.resolve(__dirname, '../../..')
const taskDir = path.join(repoRoot, 'doc/tasks/20260630-showroom-hall-config-package/e2e-artifacts')
const sourceZipPath = path.join(taskDir, 'showroom-hall-config-package-source.zip')
const targetZipPath = path.join(taskDir, 'showroom-hall-config-package-target.zip')
const sourceScreenshotPath = path.join(taskDir, 'showroom-hall-config-package-source.png')
const targetScreenshotPath = path.join(taskDir, 'showroom-hall-config-package-target.png')
const resultPath = path.join(taskDir, 'showroom-hall-config-package-real-result.json')
const HALL_PAGE_PATH = '/showroom/hall'
const EXPORT_API_PATH = '/admin-api/showroom/hall/config-package/export'
const IMPORT_API_PATH = '/admin-api/showroom/hall/config-package/import'

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true })
}

async function settle(page, timeout = 30000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
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
    body: body.slice(0, 1200),
    buttons: buttons.slice(0, 40)
  }
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

    const loginResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await form.getByRole('button', { name: '登录' }).click()
    const loginResponse = await loginResponsePromise
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

async function exportPackage(identity, zipPath, screenshotPath) {
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    acceptDownloads: true,
    locale: 'zh-CN',
    viewport: { width: 1440, height: 960 }
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page, identity, HALL_PAGE_PATH)
    const exportButton = await waitForVisibleButton(page, '导出数据包', identity)
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes(EXPORT_API_PATH) && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await exportButton.click()
    const response = await responsePromise
    const payload = await readResponse(response)
    if (payload.type !== 'binary') {
      throw new Error(
        `${identity.tenant}/${identity.username} export returned JSON instead of zip: ${JSON.stringify(payload.json)}`
      )
    }
    assert.equal(response.status(), 200, `${identity.tenant}/${identity.username} export http status must be 200`)
    fs.writeFileSync(zipPath, payload.buffer)
    assert.ok(fs.statSync(zipPath).size > 0, `${zipPath} must not be empty`)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    assert.deepEqual(pageErrors, [], `page errors were emitted during export: ${pageErrors.join(' | ')}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

function summarizeManifest(manifest) {
  const halls = Array.isArray(manifest.halls) ? manifest.halls : []
  const keywords = Array.isArray(manifest.keywords) ? manifest.keywords : []
  const productCodes = new Set()
  const awardCodes = new Set()
  let previewAssetCount = 0
  let backgroundAssetCount = 0
  let narrationCount = 0
  for (const hall of halls) {
    if (hall?.previewAsset?.assetPath) {
      previewAssetCount += 1
    }
    if (hall?.canvasBackground?.assetPath) {
      backgroundAssetCount += 1
    }
    for (const item of Array.isArray(hall?.itemMappings) ? hall.itemMappings : []) {
      if (item?.itemType === 'PRODUCT' && item?.itemCode) {
        productCodes.add(item.itemCode)
      }
      if (item?.itemType === 'AWARD' && item?.itemCode) {
        awardCodes.add(item.itemCode)
      }
    }
    narrationCount += Array.isArray(hall?.narrations) ? hall.narrations.length : 0
  }
  return {
    hallCount: halls.length,
    keywordCount: keywords.length,
    previewAssetCount,
    narrationCount,
    backgroundAssetCount,
    validatedProductCount: productCodes.size,
    validatedAwardCount: awardCodes.size
  }
}

async function importAndExportTargetPackage(sourcePath, targetPath, screenshotPath, expectedSummary) {
  const identity = {
    tenant: config.targetTenant,
    username: config.targetUsername,
    password: config.targetPassword
  }
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    acceptDownloads: true,
    locale: 'zh-CN',
    viewport: { width: 1440, height: 960 }
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page, identity, HALL_PAGE_PATH)
    await waitForVisibleButton(page, '导入数据包', identity)
    const importInput = page.locator('input[type="file"][accept*=".zip"]').first()
    const importResponsePromise = page.waitForResponse(
      (response) => response.url().includes(IMPORT_API_PATH) && response.request().method() === 'POST',
      { timeout: 300000 }
    )
    await importInput.setInputFiles(sourcePath)
    const importResponse = await importResponsePromise
    const importPayload = await readResponse(importResponse)
    assert.equal(importPayload.type, 'json', 'import response must be JSON')
    assert.equal(importResponse.status(), 200, `import http status must be 200: ${JSON.stringify(importPayload.json)}`)
    assert.ok(
      importPayload.json && [0, 200].includes(importPayload.json.code),
      `import failed: ${JSON.stringify(importPayload.json)}`
    )
    const summary = importPayload.json.data || {}
    assert.equal(summary.hallCount, expectedSummary.hallCount, 'import hallCount mismatch')
    assert.equal(summary.keywordCount, expectedSummary.keywordCount, 'import keywordCount mismatch')
    assert.equal(summary.previewAssetCount, expectedSummary.previewAssetCount, 'import previewAssetCount mismatch')
    assert.equal(summary.narrationCount, expectedSummary.narrationCount, 'import narrationCount mismatch')
    assert.equal(summary.backgroundAssetCount, expectedSummary.backgroundAssetCount, 'import backgroundAssetCount mismatch')
    assert.equal(summary.validatedProductCount, expectedSummary.validatedProductCount, 'import validatedProductCount mismatch')
    assert.equal(summary.validatedAwardCount, expectedSummary.validatedAwardCount, 'import validatedAwardCount mismatch')
    await settle(page, 120000)

    const exportButton = await waitForVisibleButton(page, '导出数据包', identity)
    const exportResponsePromise = page.waitForResponse(
      (response) => response.url().includes(EXPORT_API_PATH) && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await exportButton.click()
    const exportResponse = await exportResponsePromise
    const exportPayload = await readResponse(exportResponse)
    if (exportPayload.type !== 'binary') {
      throw new Error(`target export returned JSON instead of zip: ${JSON.stringify(exportPayload.json)}`)
    }
    assert.equal(exportResponse.status(), 200, 'target export http status must be 200')
    fs.writeFileSync(targetPath, exportPayload.buffer)
    assert.ok(fs.statSync(targetPath).size > 0, `${targetPath} must not be empty`)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    assert.deepEqual(pageErrors, [], `page errors were emitted during target import/export: ${pageErrors.join(' | ')}`)
    return summary
  } finally {
    await context.close()
    await browser.close()
  }
}

function escapePowerShellLiteral(value) {
  return String(value).replace(/'/g, "''")
}

function expandZip(zipPath, destinationPath) {
  execFileSync(
    'powershell',
    [
      '-NoProfile',
      '-Command',
      `Expand-Archive -LiteralPath '${escapePowerShellLiteral(zipPath)}' -DestinationPath '${escapePowerShellLiteral(destinationPath)}' -Force`
    ],
    { stdio: 'pipe' }
  )
}

function walkFiles(rootDir, currentDir = rootDir, result = []) {
  const entries = fs.readdirSync(currentDir, { withFileTypes: true })
  for (const entry of entries) {
    const absolutePath = path.join(currentDir, entry.name)
    if (entry.isDirectory()) {
      walkFiles(rootDir, absolutePath, result)
      continue
    }
    result.push(absolutePath)
  }
  return result
}

function sha256(buffer) {
  return crypto.createHash('sha256').update(buffer).digest('hex')
}

function normalizeBinaryRef(value) {
  return value?.assetPath ? { assetPath: value.assetPath } : null
}

function normalizeManifest(manifest) {
  const keywords = (Array.isArray(manifest.keywords) ? manifest.keywords : [])
    .map((keyword) => ({
      nameZh: String(keyword?.nameZh || ''),
      nameEn: String(keyword?.nameEn || '')
    }))
    .sort((left, right) => left.nameZh.localeCompare(right.nameZh) || left.nameEn.localeCompare(right.nameEn))

  const halls = (Array.isArray(manifest.halls) ? manifest.halls : [])
    .map((hall) => ({
      hallCode: String(hall?.hallCode || ''),
      name: String(hall?.name || ''),
      nameEn: String(hall?.nameEn || ''),
      description: String(hall?.description || ''),
      descriptionEn: String(hall?.descriptionEn || ''),
      canvasBackground: normalizeBinaryRef(hall?.canvasBackground),
      itemMappings: (Array.isArray(hall?.itemMappings) ? hall.itemMappings : [])
        .map((item) => ({
          itemType: String(item?.itemType || ''),
          itemCode: String(item?.itemCode || ''),
          displayOrder: Number(item?.displayOrder || 0),
          layoutX: item?.layoutX ?? null,
          layoutY: item?.layoutY ?? null,
          layoutWidth: item?.layoutWidth ?? null,
          layoutHeight: item?.layoutHeight ?? null
        }))
        .sort(
          (left, right) =>
            left.displayOrder - right.displayOrder ||
            left.itemType.localeCompare(right.itemType) ||
            left.itemCode.localeCompare(right.itemCode)
        ),
      previewAsset: normalizeBinaryRef(hall?.previewAsset),
      narrations: (Array.isArray(hall?.narrations) ? hall.narrations : [])
        .map((narration) => ({
          language: String(narration?.language || ''),
          scriptText: String(narration?.scriptText || ''),
          voice: String(narration?.voice || ''),
          duration: Number(narration?.duration || 0),
          audioAsset: normalizeBinaryRef(narration?.audioAsset)
        }))
        .sort((left, right) => left.language.localeCompare(right.language))
    }))
    .sort((left, right) => left.hallCode.localeCompare(right.hallCode))

  return {
    schemaVersion: String(manifest.schemaVersion || ''),
    keywords,
    halls
  }
}

function parsePackage(zipPath) {
  const extractionDir = fs.mkdtempSync(path.join(os.tmpdir(), 'showroom-hall-config-package-'))
  expandZip(zipPath, extractionDir)
  const manifestPath = path.join(extractionDir, 'manifest.json')
  assert.ok(fs.existsSync(manifestPath), `manifest.json must exist in ${zipPath}`)
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'))
  const assetRoot = path.join(extractionDir, 'assets')
  const assetHashes = {}
  if (fs.existsSync(assetRoot)) {
    for (const filePath of walkFiles(assetRoot)) {
      const relativePath = path.relative(extractionDir, filePath).replace(/\\/g, '/')
      assetHashes[relativePath] = sha256(fs.readFileSync(filePath))
    }
  }
  return {
    normalizedManifest: normalizeManifest(manifest),
    assetHashes
  }
}

function comparePackages(sourcePath, targetPath) {
  const sourcePackage = parsePackage(sourcePath)
  const targetPackage = parsePackage(targetPath)
  assert.deepEqual(
    targetPackage.normalizedManifest,
    sourcePackage.normalizedManifest,
    'target re-export manifest must match source package business content'
  )
  assert.deepEqual(
    targetPackage.assetHashes,
    sourcePackage.assetHashes,
    'target re-export asset hashes must match source package assets'
  )
}

async function main() {
  ensureDir(taskDir)
  const sourceIdentity = {
    tenant: config.sourceTenant,
    username: config.sourceUsername,
    password: config.sourcePassword
  }
  await exportPackage(sourceIdentity, sourceZipPath, sourceScreenshotPath)
  const sourceManifest = parsePackage(sourceZipPath).normalizedManifest
  const expectedSummary = summarizeManifest(sourceManifest)
  const importSummary = await importAndExportTargetPackage(
    sourceZipPath,
    targetZipPath,
    targetScreenshotPath,
    expectedSummary
  )
  comparePackages(sourceZipPath, targetZipPath)

  const result = {
    status: 'PASS',
    baseUrl: config.baseUrl,
    sourceTenant: config.sourceTenant,
    sourceUsername: config.sourceUsername,
    targetTenant: config.targetTenant,
    targetUsername: config.targetUsername,
    sourceZipPath,
    targetZipPath,
    sourceScreenshotPath,
    targetScreenshotPath,
    expectedSummary,
    importSummary
  }
  fs.writeFileSync(resultPath, JSON.stringify(result, null, 2), 'utf8')
  console.log(`PASS: showroom hall config package real roundtrip -> ${resultPath}`)
}

main().catch((error) => {
  console.error(error.stack || error.message || error)
  process.exit(1)
})
