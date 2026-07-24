import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { showroomPhase1E2ECases } from './showroom-phase1-e2e.manifest.mjs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const rootDir = path.resolve(__dirname, '..')
const frontendBase = 'http://localhost:8081'
const outputDir = path.join(rootDir, 'output', 'playwright', 'showroom-phase1-e2e')

const args = new Set(process.argv.slice(2))
const getArgValue = (name) => {
  const prefix = `${name}=`
  const matched = process.argv.slice(2).find((arg) => arg.startsWith(prefix))
  return matched ? matched.slice(prefix.length) : ''
}

const selectedCaseId = getArgValue('--case')
const shouldList = args.has('--list')
const shouldDryRun = args.has('--dry-run')
const isHeaded = args.has('--headed')

const fail = (message) => {
  throw new Error(`[showroom-phase1-e2e] ${message}`)
}

const selectedCases = selectedCaseId
  ? showroomPhase1E2ECases.filter((testCase) => testCase.id === selectedCaseId)
  : showroomPhase1E2ECases

if (selectedCaseId && selectedCases.length === 0) {
  fail(`unknown case id: ${selectedCaseId}`)
}

const printCaseList = () => {
  for (const testCase of selectedCases) {
    console.log(`${testCase.id} - ${testCase.title}`)
    for (const scenario of testCase.scenarios) {
      console.log(`  - ${scenario}`)
    }
  }
}

if (shouldList) {
  printCaseList()
  process.exit(0)
}

const assertRequiredEnv = (names) => {
  const missing = [...new Set(names)].filter((name) => !process.env[name])
  if (missing.length > 0) {
    fail(`missing required env: ${missing.join(', ')}`)
  }
}

const resolvePlaywright = () => {
  assertRequiredEnv(['SHOWROOM_E2E_PLAYWRIGHT_MODULE'])
  const modulePath = process.env.SHOWROOM_E2E_PLAYWRIGHT_MODULE
  if (!fs.existsSync(modulePath)) {
    fail(`SHOWROOM_E2E_PLAYWRIGHT_MODULE does not exist: ${modulePath}`)
  }
  const require = createRequire(import.meta.url)
  return require(modulePath)
}

const normalizeText = (value) => String(value || '').replace(/\s+/g, ' ').trim()

const expectVisibleText = async (page, text, context = text) => {
  const locator = page.getByText(text, { exact: false }).first()
  await locator.waitFor({ state: 'visible', timeout: 15000 }).catch((error) => {
    throw new Error(`visible_text_missing: ${context}: ${error.message}`)
  })
  return locator
}

const failIfMissing = async (locator, message) => {
  const count = await locator.count()
  if (count === 0) {
    throw new Error(message)
  }
  return locator.first()
}

const waitForQuiet = async (page, timeout = 15000) => {
  await page.waitForLoadState('networkidle', { timeout }).catch((error) => {
    throw new Error(`network_idle_timeout: ${error.message}`)
  })
  await page.waitForTimeout(500)
}

const waitForJsonResponse = async (page, urlPart, action, timeout = 30000) => {
  const [response] = await Promise.all([
    page.waitForResponse((item) => item.url().includes(urlPart), { timeout }),
    action()
  ])
  const payload = await response.json().catch((error) => {
    throw new Error(`json_response_unreadable: ${urlPart}: ${error.message}`)
  })
  return { response, payload }
}

const loginAs = async (page, role, redirectPath = '/') => {
  const normalizedRole = role.toUpperCase()
  const usernameKey = `SHOWROOM_E2E_${normalizedRole}_USERNAME`
  const passwordKey = `SHOWROOM_E2E_${normalizedRole}_PASSWORD`
  assertRequiredEnv(['SHOWROOM_E2E_TENANT_NAME', usernameKey, passwordKey])

  await page.goto(`${frontendBase}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 30000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${frontendBase}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 30000
  })
  await page.waitForSelector('.login-form input', { timeout: 15000 })

  const loginForm = page.locator('.login-form')
  const textboxes = loginForm.getByRole('textbox')
  const textboxCount = await textboxes.count()
  const passwordInput = loginForm.locator('input[type="password"]').first()
  const tenantInput = textboxCount >= 3 ? textboxes.nth(0) : null
  const usernameInput = textboxes.nth(textboxCount >= 3 ? 1 : 0)

  if (tenantInput) {
    await tenantInput.fill('')
    await tenantInput.fill(process.env.SHOWROOM_E2E_TENANT_NAME)
  }
  await usernameInput.fill('')
  await usernameInput.fill(process.env[usernameKey])
  await passwordInput.fill('')
  await passwordInput.fill(process.env[passwordKey])

  await Promise.all([loginForm.locator('.el-button--primary').first().click(), page.waitForTimeout(1500)])
  await waitForQuiet(page, 15000)

  if (page.url().includes('/login')) {
    throw new Error(`login_failed: role=${role}, url=${page.url()}`)
  }
}

const takeScreenshot = async (page, caseId, name) => {
  fs.mkdirSync(outputDir, { recursive: true })
  const safeName = name.replace(/[^a-z0-9-]+/gi, '-').toLowerCase()
  const screenshotPath = path.join(outputDir, `${caseId}-${safeName}.png`)
  await page.screenshot({ path: screenshotPath, fullPage: true })
  return screenshotPath
}

const loadCaseModule = async (entry) => {
  const moduleUrl = pathToFileURL(path.join(__dirname, entry.modulePath)).href
  const module = await import(moduleUrl)
  const testCase = module.default
  if (!testCase || typeof testCase !== 'object') {
    fail(`${entry.id} default export must be an object`)
  }
  if (testCase.id !== entry.id) {
    fail(`${entry.id} default export id mismatch: ${testCase.id}`)
  }
  if (typeof testCase.run !== 'function') {
    fail(`${entry.id} default export must provide async run(ctx)`)
  }
  if (!Array.isArray(testCase.requiredEnv)) {
    fail(`${entry.id} default export must provide requiredEnv array`)
  }
  return testCase
}

const loadedCases = []
for (const entry of selectedCases) {
  const testCase = await loadCaseModule(entry)
  loadedCases.push({ entry, testCase })
}

if (shouldDryRun) {
  for (const { entry, testCase } of loadedCases) {
    console.log(`${entry.id} -> module ok, requiredEnv=${testCase.requiredEnv.join(', ')}`)
  }
  process.exit(0)
}

assertRequiredEnv([
  'SHOWROOM_E2E_PLAYWRIGHT_MODULE',
  ...loadedCases.flatMap(({ testCase }) => testCase.requiredEnv)
])

const { chromium } = resolvePlaywright()
const browser = await chromium.launch({ headless: !isHeaded })
const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } })
const consoleErrors = []
let activeCaseId = 'showroom-phase1-e2e'

page.on('console', (message) => {
  if (message.type() === 'error') {
    consoleErrors.push(message.text())
  }
})
page.on('pageerror', (error) => {
  consoleErrors.push(error.message)
})

try {
  for (const { entry, testCase } of loadedCases) {
    activeCaseId = entry.id
    assertRequiredEnv(testCase.requiredEnv)
    console.log(`RUN ${entry.id} - ${entry.title}`)
    await testCase.run({
      page,
      env: process.env,
      frontendBase,
      loginAs,
      expectVisibleText,
      failIfMissing,
      waitForJsonResponse,
      waitForQuiet,
      normalizeText,
      takeScreenshot
    })
    if (consoleErrors.length > 0) {
      throw new Error(`browser_console_errors: ${consoleErrors.join(' | ')}`)
    }
    console.log(`PASS ${entry.id}`)
  }
} catch (error) {
  try {
    await takeScreenshot(page, activeCaseId, 'failure')
  } catch (screenshotError) {
    console.error(`failure_screenshot_failed: ${screenshotError.message}`)
  }
  throw error
} finally {
  await page.close()
  await browser.close()
}
