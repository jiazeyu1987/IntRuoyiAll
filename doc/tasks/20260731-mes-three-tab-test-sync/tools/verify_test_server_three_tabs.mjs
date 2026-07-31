#!/usr/bin/env node

import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const taskRoot = path.resolve(__dirname, '..')
const repoRoot = path.resolve(taskRoot, '..', '..', '..')
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

function required(value, label) {
  const normalized = String(value || '').trim()
  assert.ok(normalized, `Missing ${label}`)
  return normalized
}

function resolveUrl(baseUrl, targetPath) {
  return new URL(targetPath, baseUrl).toString()
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
  throw new Error(`Missing editable login control: ${label}`)
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
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
}

function median(values) {
  const sorted = values.slice().sort((a, b) => a - b)
  return sorted[Math.floor(sorted.length / 2)] || 0
}

async function calculatePuzzleDragDistance(page) {
  return await page.evaluate(async () => {
    function loadImage(src) {
      return new Promise((resolve, reject) => {
        const img = new Image()
        img.onload = () => resolve(img)
        img.onerror = reject
        img.src = src
      })
    }

    function imageData(img) {
      const canvas = document.createElement('canvas')
      canvas.width = img.naturalWidth || img.width
      canvas.height = img.naturalHeight || img.height
      const ctx = canvas.getContext('2d', { willReadFrequently: true })
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      return {
        width: canvas.width,
        height: canvas.height,
        data: ctx.getImageData(0, 0, canvas.width, canvas.height).data
      }
    }

    function alphaAt(image, x, y) {
      if (x < 0 || y < 0 || x >= image.width || y >= image.height) return 0
      return image.data[(y * image.width + x) * 4 + 3]
    }

    function grayAt(image, x, y) {
      if (x < 1 || y < 1 || x >= image.width - 1 || y >= image.height - 1) return 0
      const offset = (y * image.width + x) * 4
      return image.data[offset] * 0.299 + image.data[offset + 1] * 0.587 + image.data[offset + 2] * 0.114
    }

    function gradientAt(image, x, y) {
      const gx = Math.abs(grayAt(image, x + 1, y) - grayAt(image, x - 1, y))
      const gy = Math.abs(grayAt(image, x, y + 1) - grayAt(image, x, y - 1))
      return gx + gy
    }

    const backgroundElement = document.querySelector('.verifybox .verify-img-panel img')
    const pieceElement = document.querySelector('.verifybox .verify-sub-block img')
    const panelElement = document.querySelector('.verifybox .verify-img-panel')
    if (!backgroundElement || !pieceElement || !panelElement) {
      throw new Error('Captcha images not visible')
    }

    const background = imageData(await loadImage(backgroundElement.src))
    const piece = imageData(await loadImage(pieceElement.src))
    const boundary = []
    const nonTransparentXs = []

    for (let y = 1; y < piece.height - 1; y += 1) {
      for (let x = 1; x < piece.width - 1; x += 1) {
        if (alphaAt(piece, x, y) < 32) continue
        nonTransparentXs.push(x)
        const touchesTransparent =
          alphaAt(piece, x - 1, y) < 32 ||
          alphaAt(piece, x + 1, y) < 32 ||
          alphaAt(piece, x, y - 1) < 32 ||
          alphaAt(piece, x, y + 1) < 32
        if (touchesTransparent) {
          boundary.push([x, y])
        }
      }
    }
    if (boundary.length <= 0) {
      throw new Error('Captcha puzzle boundary not detected')
    }

    const pieceLeftPadding = Math.max(0, Math.min(...nonTransparentXs))
    let bestX = 0
    let bestScore = -Infinity
    const maxX = background.width - piece.width - 1
    for (let candidateX = 1; candidateX <= maxX; candidateX += 1) {
      let score = 0
      for (const [maskX, maskY] of boundary) {
        const bgX = candidateX + maskX
        if (bgX < 1 || bgX >= background.width - 1 || maskY < 1 || maskY >= background.height - 1) {
          continue
        }
        score += gradientAt(background, bgX, maskY)
      }
      if (score > bestScore) {
        bestScore = score
        bestX = candidateX
      }
    }

    const panelWidth = panelElement.getBoundingClientRect().width
    const normalizedX = (bestX + pieceLeftPadding) * (310 / background.width)
    return {
      dragDistance: normalizedX * (panelWidth / 310),
      normalizedX,
      naturalX: bestX,
      score: bestScore,
      backgroundWidth: background.width,
      panelWidth
    }
  })
}

async function solveSliderCaptcha(page, timeout) {
  for (let attempt = 1; attempt <= 5; attempt += 1) {
    await page.locator('.verifybox .verify-img-panel img').first().waitFor({ state: 'visible', timeout })
    await page.waitForTimeout(500)
    const distance = await calculatePuzzleDragDistance(page)
    const moveBlock = page.locator('.verifybox .verify-move-block').first()
    const blockBox = await moveBlock.boundingBox()
    assert.ok(blockBox, 'Captcha slider block not found')
    const startX = blockBox.x + blockBox.width / 2
    const startY = blockBox.y + blockBox.height / 2
    await page.mouse.move(startX, startY)
    await page.mouse.down()
    await page.mouse.move(startX + distance.dragDistance, startY, { steps: 36 })
    await page.mouse.up()

    const success = await page
      .locator('.verifybox .suc-bg, .verifybox .icon-check')
      .first()
      .waitFor({ state: 'visible', timeout: 3000 })
      .then(() => true)
      .catch(() => false)
    if (success) {
      return { attempt, ...distance }
    }
    await page.waitForTimeout(1200)
  }
  throw new Error('Captcha slider did not pass after 5 attempts')
}

async function login(page, config) {
  await page.goto(resolveUrl(config.baseUrl, '/login?redirect=/index'), {
    waitUntil: 'commit',
    timeout: config.timeout
  })
  if (!page.url().includes('/login')) return { captcha: null }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form, config.tenant)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  ).catch((error) => error)
  await form.getByRole('button', { name: /^登录$/ }).click()
  let captcha = null
  if ((await page.locator('.verifybox .verify-img-panel img').count()) > 0) {
    captcha = await solveSliderCaptcha(page, config.timeout)
  }

  const loginResponse = await loginResponsePromise
  if (loginResponse instanceof Error) {
    throw loginResponse
  }
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `Login HTTP failed: ${loginResponse.status()}`)
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `Login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), {
    timeout: config.timeout,
    waitUntil: 'commit'
  })
  return { captcha }
}

function extractTotal(responseBody) {
  const data = responseBody?.data
  if (typeof data?.total === 'number') return data.total
  if (typeof data?.total === 'string') return Number(data.total)
  if (Array.isArray(data?.list)) return data.list.length
  return 0
}

async function verifyTab(page, config, definition) {
  const errors = []
  for (const targetPath of definition.paths) {
    try {
      const responsePromise = page.waitForResponse(
        (response) =>
          response.url().includes(definition.apiPath) &&
          response.request().method() === 'GET',
        { timeout: config.timeout }
      )
      await page.goto(resolveUrl(config.baseUrl, targetPath), {
        waitUntil: 'domcontentloaded',
        timeout: config.timeout
      })
      const response = await responsePromise
      const body = await response.json()
      assert.ok(response.ok(), `${definition.name} API HTTP failed: ${response.status()}`)
      assert.ok(body.code === 0 || body.code === 200, `${definition.name} API failed: ${body.msg || body.code}`)
      const total = extractTotal(body)
      assert.ok(total >= definition.minTotal, `${definition.name} total ${total} < ${definition.minTotal}`)
      await page.getByText(definition.visibleText, { exact: false }).first().waitFor({
        state: 'visible',
        timeout: config.timeout
      })
      await page.locator('.el-table__body-wrapper tbody tr').first().waitFor({
        state: 'visible',
        timeout: config.timeout
      })
      const systemErrorVisible = await page.getByText('系统异常', { exact: false }).first().isVisible().catch(() => false)
      assert.equal(systemErrorVisible, false, `${definition.name} page shows system error`)

      const screenshot = path.join(config.artifactDir, `${definition.id}.png`)
      await page.screenshot({ path: screenshot, fullPage: true })
      return {
        id: definition.id,
        name: definition.name,
        path: targetPath,
        apiPath: definition.apiPath,
        total,
        screenshot
      }
    } catch (error) {
      errors.push({ path: targetPath, message: error.message })
    }
  }
  throw new Error(`${definition.name} verification failed: ${JSON.stringify(errors)}`)
}

async function run() {
  const args = parseArgs(process.argv.slice(2))
  const artifactDir = path.join(taskRoot, 'artifacts', 'test-server-e2e')
  fs.mkdirSync(artifactDir, { recursive: true })

  const config = {
    baseUrl: required(args.get('base-url') || process.env.TEST_SERVER_BASE_URL, 'base URL'),
    tenant: required(args.get('tenant') || process.env.TEST_TENANT, 'tenant'),
    username: required(args.get('username') || process.env.TEST_USER, 'username'),
    password: required(args.get('password') || process.env.TEST_PASS, 'password'),
    artifactDir,
    timeout: Number(args.get('timeout') || process.env.TEST_E2E_TIMEOUT || 90000),
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || ''
  }
  if (config.executablePath) {
    assert.ok(fs.existsSync(config.executablePath), `Chrome not found: ${config.executablePath}`)
  }

  const browser = await chromium.launch({
    headless: true,
    executablePath: config.executablePath || undefined
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const result = {
    generatedAt: new Date().toISOString(),
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    login: null,
    tabs: []
  }
  try {
    result.login = await login(page, config)
    const definitions = [
      {
        id: 'process',
        name: 'process settings',
        visibleText: '工序编码',
        apiPath: '/admin-api/mes/pro/process/page',
        paths: ['/mes/pro/process'],
        minTotal: 1
      },
      {
        id: 'route',
        name: 'route flow',
        visibleText: '路线编码',
        apiPath: '/admin-api/mes/pro/route/page',
        paths: ['/mes/pro/route'],
        minTotal: 1
      },
      {
        id: 'schedule-order',
        name: 'schedule order',
        visibleText: '排产工单号',
        apiPath: '/admin-api/mes/pro/schedule-order/page',
        paths: ['/mes/pro/scheduleorder', '/mes/pro/schedule-order'],
        minTotal: 1
      }
    ]
    for (const definition of definitions) {
      result.tabs.push(await verifyTab(page, config, definition))
    }
    result.status = 'PASS'
  } catch (error) {
    result.status = 'FAIL'
    result.error = error.stack || error.message
    const screenshot = path.join(config.artifactDir, 'failure.png')
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => undefined)
    result.failureScreenshot = screenshot
    throw error
  } finally {
    const resultPath = path.join(config.artifactDir, 'result.json')
    fs.writeFileSync(resultPath, JSON.stringify(result, null, 2), 'utf8')
    await context.close()
    await browser.close()
    console.log(`E2E result: ${result.status || 'UNKNOWN'} -> ${resultPath}`)
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
