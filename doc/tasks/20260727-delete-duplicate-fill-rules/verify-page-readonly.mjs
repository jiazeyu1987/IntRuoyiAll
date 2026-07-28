import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..', '..', '..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

const baseUrl = 'http://localhost:8081'
const targetPath = '/mes/pro/batch-record-form-list'
const targetProduct = '球囊扩张压力泵'
const targetReport = '粗洗工序生产记录'
const browserExecutable = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function parseEnv(filePath) {
  const values = new Map()
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const separator = line.indexOf('=')
    if (separator < 0) continue
    const key = line.slice(0, separator).trim()
    const value = line
      .slice(separator + 1)
      .trim()
      .replace(/^['"]|['"]$/g, '')
    values.set(key, value)
  }
  return values
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写登录控件：${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw new Error(`缺少可点击控件：${label}`)
}

async function selectOption(page, label) {
  const option = page
    .locator(
      '.el-select-dropdown:visible .el-select-dropdown__item, ' +
        '.el-popper[aria-hidden="false"] .el-select-dropdown__item'
    )
    .filter({ hasText: label })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page, credentials) {
  const redirect = encodeURIComponent(targetPath)
  await page.goto(`${baseUrl}/login?redirect=${redirect}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${baseUrl}/login?redirect=${redirect}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  assert.equal(
    await form.locator(
      '.verify-img-panel, .verify-bar-area, input[placeholder*="验证码"]'
    ).count(),
    0,
    '登录页启用了验证码，无法执行无人值守只读验证'
  )

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(credentials.tenant)
    await selectOption(page, credentials.tenant)
  } else {
    await fillFirstVisible(
      form.locator('input[placeholder="请输入租户名称"]'),
      credentials.tenant,
      '租户'
    )
  }

  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    credentials.username,
    '账号'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), credentials.password, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await clickFirstEnabled(form.getByRole('button', { name: /^登录$/ }), '登录')
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP 失败：${loginResponse.status()}`)
  assert.ok(
    [0, 200].includes(Number(loginBody.code)),
    `登录失败：${loginBody.msg || loginBody.code}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function searchTargetReport(page) {
  await page.goto(`${baseUrl}${targetPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.getByText('批记录表单').first().waitFor({
    state: 'visible',
    timeout: 90000
  })
  await page.locator('.table-quick-filter__field').first().click()
  await selectOption(page, '表单名称')
  const valueInput = page.locator('.table-quick-filter__value input').first()
  await valueInput.waitFor({ state: 'visible', timeout: 30000 })
  await valueInput.fill(targetReport)

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickFirstEnabled(page.getByRole('button', { name: '查询' }), '查询')
  const response = await responsePromise
  assert.ok(response.ok(), `表单列表 HTTP 失败：${response.status()}`)
}

async function main() {
  assert.ok(fs.existsSync(browserExecutable), `Chrome 不存在：${browserExecutable}`)
  const env = parseEnv(path.join(frontendRoot, '.env'))
  const credentials = {
    tenant: env.get('VITE_APP_DEFAULT_LOGIN_TENANT'),
    username: env.get('VITE_APP_DEFAULT_LOGIN_USERNAME'),
    password: env.get('VITE_APP_DEFAULT_LOGIN_PASSWORD')
  }
  assert.ok(credentials.tenant, '缺少默认登录租户')
  assert.ok(credentials.username, '缺少默认登录账号')
  assert.ok(credentials.password, '缺少默认登录密码')

  const browser = await chromium.launch({
    headless: true,
    executablePath: browserExecutable
  })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const mesWrites = []
  try {
    await login(page, credentials)
    page.on('request', (request) => {
      if (
        request.url().includes('/admin-api/mes/') &&
        !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
      ) {
        mesWrites.push(`${request.method()} ${request.url()}`)
      }
    })

    await searchTargetReport(page)
    const row = page
      .locator('.el-table__body-wrapper tbody tr:visible')
      .filter({ hasText: targetProduct })
      .filter({ hasText: targetReport })
      .first()
    await row.waitFor({ state: 'visible', timeout: 60000 })

    const fillerCell = row.locator('.batch-record-form-filler-cell').first()
    await fillerCell.waitFor({ state: 'visible', timeout: 60000 })
    await page.waitForFunction(
      ({ product, report }) => {
        const rows = [...document.querySelectorAll('.el-table__body-wrapper tbody tr')]
        const row = rows.find(
          (item) =>
            item.textContent?.includes(product) &&
            item.textContent?.includes(report) &&
            item.getBoundingClientRect().height > 0
        )
        const text = row?.querySelector('.batch-record-form-filler-cell')?.textContent || ''
        return !text.includes('加载中') && !text.includes('填写规则加载中')
      },
      { product: targetProduct, report: targetReport },
      { timeout: 60000 }
    )

    const fillerText = (await fillerCell.innerText()).replace(/\s+/g, ' ').trim()
    assert.doesNotMatch(fillerText, /加载失败|查看错误/)
    assert.match(fillerText, /已配置/)
    assert.match(fillerText, /王歆/)
    assert.match(fillerText, /任丹/)
    assert.equal(mesWrites.length, 0, `只读页面验证产生了 MES 写请求：${mesWrites.join(', ')}`)

    console.log(
      `PASS: product=${targetProduct}; report=${targetReport}; filler=${fillerText}; mesWrites=0`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
