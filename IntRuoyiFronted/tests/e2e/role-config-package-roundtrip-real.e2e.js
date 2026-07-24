const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const os = require('node:os')
const { chromium } = require('playwright')

const BASE_URL = process.env.ROLE_CONFIG_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.ROLE_CONFIG_E2E_TENANT || '测试租户'
const USERNAME = process.env.ROLE_CONFIG_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.ROLE_CONFIG_E2E_PASSWORD || '111111'
const OUTPUT_ROOT =
  process.env.ROLE_CONFIG_E2E_OUTPUT_DIR ||
  path.resolve(__dirname, '../../output/playwright/role-config-package-roundtrip-real')

const PAGE_CONFIGS = [
  {
    name: 'permission-role',
    path: '/system/role',
    text: '权限角色',
    exportButton: '导出配置包',
    importButton: '导入配置包',
    fileName: '权限角色配置包.json',
    requiresExportConfirm: true,
    exportApiPath: '/admin-api/system/role/config-package/export',
    importApiPath: '/admin-api/system/role/config-package/import'
  },
  {
    name: 'organization-role',
    path: '/system/post',
    text: '组织角色',
    exportButton: '导出配置包',
    importButton: '导入配置包',
    fileName: '组织角色配置包.json',
    requiresExportConfirm: true,
    exportApiPath: '/admin-api/system/post/config-package/export',
    importApiPath: '/admin-api/system/post/config-package/import'
  },
  {
    name: 'approval-role',
    path: '/dcc/controlled-file/positions',
    text: '审批角色',
    exportButton: '导出配置包',
    importButton: '导入配置包',
    fileName: '审批角色配置包.json',
    requiresExportConfirm: false,
    exportApiPath: '/admin-api/dcc/approval-positions/config-package/export',
    importApiPath: '/admin-api/dcc/approval-positions/config-package/import'
  }
]

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true })
}

function normalizeJson(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeJson).sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)))
  }
  if (value && typeof value === 'object') {
    const entries = Object.entries(value)
      .filter(([, current]) => current !== undefined)
      .map(([key, current]) => [key, normalizeJson(current)])
      .sort(([left], [right]) => left.localeCompare(right))
    return Object.fromEntries(entries)
  }
  return value
}

function compareJsonFiles(firstPath, secondPath, label) {
  const first = JSON.parse(fs.readFileSync(firstPath, 'utf8'))
  const second = JSON.parse(fs.readFileSync(secondPath, 'utf8'))
  const normalizedFirst = normalizeJson(first)
  const normalizedSecond = normalizeJson(second)
  assert.deepStrictEqual(
    normalizedSecond,
    normalizedFirst,
    `${label} 导入后再次导出的配置包与首次导出不一致`
  )
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'networkidle' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    await tenantInput.press('Enter')
  } else {
    const textInputs = form.locator('input.el-input__inner')
    await textInputs.nth(0).fill(TENANT)
  }
  const textInputs = form.locator('input.el-input__inner')
  await textInputs.nth(0).fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)
  await form.getByRole('button', { name: /登录/ }).click()
  await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 30000 })
}

async function exportPackage(page, context, config, targetPath) {
  const exportResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(config.exportApiPath) &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: config.exportButton }).click()
  if (config.requiresExportConfirm) {
    const dialog = page.locator('.app-confirm-message-box-overlay').last()
    await dialog.waitFor({ state: 'visible', timeout: 15000 })
    await dialog.getByRole('button', { name: /^确定$/ }).click()
  }
  const exportResponse = await exportResponsePromise
  const exportBuffer = await exportResponse.body()
  fs.writeFileSync(targetPath, exportBuffer)
  await page.waitForLoadState('networkidle')
}

async function importPackage(page, config, sourcePath) {
  const importResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('config-package/import'),
    { timeout: 30000 }
  )
  const importInput = page.locator('input[type="file"][accept=".json"]').first()
  await importInput.setInputFiles(sourcePath)
  const importResponse = await importResponsePromise
  console.log(`IMPORT_RESPONSE ${config.name} ${importResponse.status()} ${importResponse.url()}`)
  const importPayload = await importResponse.json()
  assert.ok(
    importPayload && [0, 200].includes(importPayload.code),
    `${config.text} 配置包导入失败: ${JSON.stringify(importPayload)}`
  )
  await page.waitForLoadState('networkidle')
  await page.getByText(new RegExp(`${config.text}配置包导入成功`)).waitFor({ timeout: 15000 })
}

async function verifyPageRoundTrip(page, context, config) {
  const pageDir = path.join(OUTPUT_ROOT, config.name)
  ensureDir(pageDir)
  const firstExportPath = path.join(pageDir, `first-${config.fileName}`)
  const secondExportPath = path.join(pageDir, `second-${config.fileName}`)

  await page.goto(`${BASE_URL}${config.path}`, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: config.exportButton }).waitFor({ timeout: 15000 })
  await exportPackage(page, context, config, firstExportPath)
  await importPackage(page, config, firstExportPath)
  await exportPackage(page, context, config, secondExportPath)
  compareJsonFiles(firstExportPath, secondExportPath, config.text)
}

async function main() {
  ensureDir(OUTPUT_ROOT)
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    acceptDownloads: true,
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  try {
    await login(page)
    for (const config of PAGE_CONFIGS) {
      await verifyPageRoundTrip(page, context, config)
    }
    const resultPath = path.join(OUTPUT_ROOT, 'result.json')
    fs.writeFileSync(
      resultPath,
      JSON.stringify(
        {
          baseUrl: BASE_URL,
          tenant: TENANT,
          username: USERNAME,
          verifiedPages: PAGE_CONFIGS.map((item) => item.name)
        },
        null,
        2
      ),
      'utf8'
    )
    console.log(`PASS: role config package real roundtrip -> ${resultPath}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
