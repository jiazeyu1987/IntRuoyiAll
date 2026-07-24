const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const frontendRequire = createRequire(path.resolve(__dirname, '../../package.json'))
const { chromium } = frontendRequire('playwright')

const args = new Map()
for (let index = 2; index < process.argv.length; index += 2) {
  args.set(process.argv[index], process.argv[index + 1])
}

const requiredArgs = ['--base-url', '--tenant', '--username', '--password', '--output-dir']
for (const key of requiredArgs) {
  if (!args.get(key)) {
    throw new Error(`missing required argument: ${key}`)
  }
}

const baseUrl = args.get('--base-url')
const tenant = args.get('--tenant')
const username = args.get('--username')
const password = args.get('--password')
const outputDir = path.resolve(args.get('--output-dir'))

fs.mkdirSync(outputDir, { recursive: true })

async function main() {
  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-dev-shm-usage']
  })

  try {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  const loginUrl = new URL('/login', baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(tenant)
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first().click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  if (!loginResponse.ok() || ![0, 200].includes(loginPayload.code)) {
    throw new Error(`login failed: HTTP ${loginResponse.status()} ${loginPayload.msg || ''}`)
  }

  await page.waitForURL((current) => !current.pathname.includes('/login'))
  await page.goto(new URL('/index', baseUrl).toString(), { waitUntil: 'domcontentloaded' })
  const logo = page.locator('a.v-logo').first()
  await logo.waitFor({ state: 'visible' })

  const result = await logo.evaluate((node) => {
    const image = node.querySelector('img')
    const title = Array.from(node.children).find((child) => child.tagName === 'DIV')
    const titleStyle = title ? window.getComputedStyle(title) : null
    const imageRect = image?.getBoundingClientRect()
    const titleRect = title?.getBoundingClientRect()
    return {
      titleText: title?.textContent?.trim() || '',
      imageSrc: image?.getAttribute('src') || '',
      naturalWidth: image?.naturalWidth || 0,
      naturalHeight: image?.naturalHeight || 0,
      titleColor: titleStyle?.color || '',
      titleMarginLeft: titleStyle?.marginLeft || '',
      gapPx: imageRect && titleRect ? Math.round(titleRect.left - imageRect.right) : null,
      logoBox: node.getBoundingClientRect().toJSON(),
      imageBox: imageRect?.toJSON(),
      titleBox: titleRect?.toJSON()
    }
  })

  if (!result.imageSrc.includes('/src/assets/imgs/sidebar-brand-logo.png')) {
    throw new Error(`unexpected logo source: ${result.imageSrc}`)
  }
  if (result.titleText !== '瑛泰管理系统') {
    throw new Error(`unexpected title text: ${result.titleText}`)
  }
  if (result.titleColor !== 'rgb(3, 56, 134)') {
    throw new Error(`unexpected title color: ${result.titleColor}`)
  }
  if (result.titleMarginLeft !== '18px' || result.gapPx !== 18) {
    throw new Error(`unexpected title spacing: margin=${result.titleMarginLeft}, gap=${result.gapPx}`)
  }
  if (result.naturalWidth !== 80 || result.naturalHeight !== 80) {
    throw new Error(`unexpected logo natural size: ${result.naturalWidth}x${result.naturalHeight}`)
  }

  const screenshotPath = path.join(outputDir, 'header-brand.png')
  await logo.screenshot({ path: screenshotPath })
  const resultPath = path.join(outputDir, 'header-brand-result.json')
  fs.writeFileSync(resultPath, JSON.stringify({ ...result, screenshotPath }, null, 2), 'utf8')
    console.log(`PASS: header brand verified at ${resultPath}`)
  } finally {
    await browser.close()
  }
}

main()
