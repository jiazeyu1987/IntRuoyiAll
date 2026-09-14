const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const envPath = path.join(frontendRoot, '.env')
const READ_ONLY_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])
const EXPECTED_VISIBLE_COLUMNS = [
  '路线编码',
  '路线名称',
  '状态',
  '当前生效版本',
  '待发布版本',
  '关联产品',
  '创建时间',
  '操作'
]
const EXPECTED_HIDDEN_COLUMNS = ['负责人', '关键工序', '关系图']

function readLoginDefaults() {
  const values = new Map()
  for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*(VITE_APP_DEFAULT_LOGIN_[A-Z]+)\s*=\s*(.+?)\s*$/)
    if (!match) continue
    values.set(match[1], match[2].trim().replace(/^['"]|['"]$/g, ''))
  }
  const result = {
    tenant: values.get('VITE_APP_DEFAULT_LOGIN_TENANT'),
    username: values.get('VITE_APP_DEFAULT_LOGIN_USERNAME'),
    password: values.get('VITE_APP_DEFAULT_LOGIN_PASSWORD')
  }
  for (const [key, value] of Object.entries(result)) {
    assert.ok(value, `缺少本机默认登录配置：${key}`)
  }
  return result
}

function requiredExecutable(name) {
  const executablePath = String(process.env[name] || '').trim()
  assert.ok(executablePath, `缺少 ${name}`)
  assert.equal(fs.existsSync(executablePath), true, `浏览器不存在：${executablePath}`)
  return executablePath
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

async function selectTenant(page, form, tenant) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: tenant })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), tenant, '租户')
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form, config.tenant)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    '账号'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `登录 HTTP ${loginResponse.status()}`)
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
}

function normalizeHeaderText(text) {
  return String(text || '').replace(/\s+/g, '')
}

async function assertColumnSettings(page) {
  await page.getByRole('button', { name: '显示字段', exact: true }).click()
  const panel = page.locator('.user-table-column-settings__panel:visible').last()
  await panel.waitFor({ state: 'visible' })

  for (const label of EXPECTED_VISIBLE_COLUMNS.filter((item) => item !== '操作')) {
    const checkbox = panel.locator('label.el-checkbox').filter({ hasText: label }).first()
    await checkbox.waitFor({ state: 'visible' })
    assert.equal(await checkbox.locator('input[type="checkbox"]').isChecked(), true, `${label} 默认必须勾选`)
  }
  for (const label of EXPECTED_HIDDEN_COLUMNS) {
    const checkbox = panel.locator('label.el-checkbox').filter({ hasText: label }).first()
    await checkbox.waitFor({ state: 'visible' })
    assert.equal(await checkbox.locator('input[type="checkbox"]').isChecked(), false, `${label} 默认必须取消勾选`)
  }
}

async function verifyBrowser(browserConfig, config) {
  const browser = await chromium.launch({
    headless: true,
    executablePath: browserConfig.executablePath
  })
  const context = await browser.newContext({
    viewport: { width: 1680, height: 900 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const consoleErrors = []
  const writeRequests = []
  const columnConfigKeys = []

  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })

  try {
    await login(page, config)
    page.on('request', (request) => {
      const url = request.url()
      if (url.includes('/admin-api/system/user-table-column-config/get')) {
        columnConfigKeys.push(new URL(url).searchParams.get('tableKey'))
      }
      if (url.includes('/admin-api/') && !READ_ONLY_METHODS.has(request.method())) {
        writeRequests.push(`${request.method()} ${new URL(url).pathname}`)
      }
    })

    await page.goto(`${config.baseUrl}/mes/pro/route`, {
      waitUntil: 'domcontentloaded',
      timeout: config.timeout
    })
    await page.getByText('路线编码', { exact: true }).first().waitFor({
      state: 'visible',
      timeout: config.timeout
    })
    await page.locator('.el-table__body-wrapper:visible tbody tr').first().waitFor({
      state: 'visible',
      timeout: config.timeout
    })

    const headers = [
      ...new Set(
        (await page.locator('.el-table__header-wrapper:visible th').allTextContents())
          .map(normalizeHeaderText)
          .filter(Boolean)
      )
    ]
    for (const label of EXPECTED_VISIBLE_COLUMNS) {
      assert.equal(headers.includes(label), true, `${browserConfig.label} 缺少表头：${label}`)
    }
    for (const label of EXPECTED_HIDDEN_COLUMNS) {
      assert.equal(headers.includes(label), false, `${browserConfig.label} 不应显示表头：${label}`)
    }

    for (const buttonName of ['显示字段', '导入', '导出', '复制', '删除']) {
      await page.getByRole('button', { name: buttonName, exact: true }).first().waitFor({
        state: 'visible',
        timeout: config.timeout
      })
    }
    await assertColumnSettings(page)

    assert.equal(
      columnConfigKeys.includes('mes.pro.route.main.admin-layout-v1'),
      true,
      `${browserConfig.label} 必须读取升级后的列配置 key`
    )
    assert.equal(
      columnConfigKeys.includes('mes.pro.route.main'),
      false,
      `${browserConfig.label} 不得读取旧列配置 key`
    )
    assert.deepEqual(writeRequests, [], `${browserConfig.label} 只读验证不得发送写请求`)
    assert.deepEqual(consoleErrors, [], `${browserConfig.label} 不得出现 console error`)
    return headers
  } finally {
    await context.close()
    await browser.close()
  }
}

async function main() {
  const loginDefaults = readLoginDefaults()
  const config = {
    baseUrl: String(process.env.MES_ROUTE_ADMIN_LAYOUT_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
    timeout: Number(process.env.MES_ROUTE_ADMIN_LAYOUT_TIMEOUT || 90000),
    ...loginDefaults
  }
  const browsers = [
    {
      label: 'Chrome',
      executablePath: requiredExecutable('MES_ROUTE_ADMIN_LAYOUT_CHROME_EXECUTABLE_PATH')
    },
    {
      label: 'Edge',
      executablePath: requiredExecutable('MES_ROUTE_ADMIN_LAYOUT_EDGE_EXECUTABLE_PATH')
    }
  ]

  const results = []
  for (const browserConfig of browsers) {
    results.push(await verifyBrowser(browserConfig, config))
  }
  assert.deepEqual(results[0], results[1], 'Chrome 与 Edge 的工艺路线表头必须一致')
  console.log(
    `PASS: route admin layout is consistent in Chrome and Edge tenant=${config.tenant} username=${config.username}`
  )
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
