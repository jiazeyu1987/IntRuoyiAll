#!/usr/bin/env node

import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..', '..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

function parseArgs(argv) {
  const args = new Map()
  for (let index = 0; index < argv.length; index += 1) {
    const key = argv[index]
    if (!key.startsWith('--')) continue
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) {
      args.set(key.slice(2), 'true')
    } else {
      args.set(key.slice(2), value)
      index += 1
    }
  }
  return args
}

function getRequired(args, key) {
  const value = (args.get(key) || '').trim()
  assert.ok(value, `Missing required --${key}`)
  return value
}

function resolveUrl(baseUrl, targetPath) {
  return new URL(targetPath || '/index', baseUrl).toString()
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
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: config.timeout })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  if (
    (await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) >
    0
  ) {
    throw new Error('登录页启用了验证码，无法执行无人值守真实登录前置。')
  }

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
  assert.ok(loginResponse.ok(), `登录 HTTP 失败：${loginResponse.status()}`)
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg || loginBody.code}`)
  if (new URL(page.url()).pathname.includes('/login')) {
    await page.waitForURL((url) => !url.pathname.includes('/login'), {
      timeout: config.timeout,
      waitUntil: 'commit'
    })
  }
  assert.ok(!new URL(page.url()).pathname.includes('/login'), `登录成功后页面必须离开登录页：${page.url()}`)
}

async function run() {
  const args = parseArgs(process.argv.slice(2))
  const config = {
    baseUrl: getRequired(args, 'base-url'),
    tenant: getRequired(args, 'tenant'),
    username: getRequired(args, 'username'),
    password: getRequired(args, 'password'),
    targetPath: args.get('target-path') || '/index',
    targetText: args.get('target-text') || '',
    timeout: Number(args.get('timeout') || 90000),
    headed: args.get('headed') === 'true' || process.env.LOGIN_PREFLIGHT_HEADED === '1',
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || ''
  }
  assert.match(config.baseUrl, /^https?:\/\//, '--base-url must be an HTTP URL')
  assert.ok(Number.isFinite(config.timeout) && config.timeout > 0, '--timeout must be a positive number')
  if (config.executablePath) {
    assert.ok(fs.existsSync(config.executablePath), `Chrome not found: ${config.executablePath}`)
  }

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.executablePath || undefined
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page, config)
    await page.goto(resolveUrl(config.baseUrl, config.targetPath), {
      waitUntil: 'domcontentloaded',
      timeout: config.timeout
    })
    if (config.targetText) {
      await page.getByText(config.targetText, { exact: false }).first().waitFor({
        state: 'visible',
        timeout: config.timeout
      })
    }
    console.log(
      `PASS: login preflight tenant=${config.tenant} username=${config.username} target=${config.targetPath}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
