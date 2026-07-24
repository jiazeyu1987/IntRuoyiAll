const assert = require('node:assert/strict')
const { mkdirSync, writeFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { chromium } = require('playwright')

const baseUrl = process.env.MES_PRO_TASK_E2E_BASE_URL || 'http://localhost:8081'
const tenant = process.env.MES_PRO_TASK_E2E_TENANT
const username = process.env.MES_PRO_TASK_E2E_USERNAME
const password = process.env.MES_PRO_TASK_E2E_PASSWORD
const outputDir = resolve(__dirname, '../../output/playwright/20260719-production-schedule-gantt-fullpage')

for (const [name, value] of Object.entries({ tenant, username, password })) {
  assert.ok(value, `missing required env MES_PRO_TASK_E2E_${name.toUpperCase()}`)
}

async function main() {
  mkdirSync(outputDir, { recursive: true })

  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-dev-shm-usage']
  })

  try {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  const network = []

  page.on('request', (request) => {
    const url = request.url()
    if (url.includes('/mes/')) {
      network.push({ method: request.method(), url })
    }
  })

  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  const loginUrl = new URL('/login', baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST'
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code failed: ${loginPayload.code}`)

  await page.waitForURL((current) => !current.pathname.includes('/login'), { waitUntil: 'commit' })
  await page.goto(new URL('/mes/pro/task', baseUrl).toString(), { waitUntil: 'commit' })
  await page.getByText('当前排产甘特图', { exact: false }).first().waitFor({ state: 'visible' })
  await page.locator('.gantt_container').first().waitFor({ state: 'visible' })

  const forbiddenTexts = ['待排产工单', '工单编码', '工单名称', '需求日期', '自动排产', '甘特图编辑']
  for (const text of forbiddenTexts) {
    await assert.rejects(
      page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 1000 }),
      undefined,
      `red-box-outside content is still visible: ${text}`
    )
  }

  const metrics = await page.evaluate(() => {
    const card = document.querySelector('.production-schedule-gantt-page')
    const gantt = document.querySelector('.gantt_container')
    const cardRect = card?.getBoundingClientRect()
    const ganttRect = gantt?.getBoundingClientRect()
    return {
      viewportWidth: window.innerWidth,
      viewportHeight: window.innerHeight,
      card: cardRect
        ? { width: cardRect.width, height: cardRect.height, top: cardRect.top, left: cardRect.left }
        : null,
      gantt: ganttRect
        ? { width: ganttRect.width, height: ganttRect.height, top: ganttRect.top, left: ganttRect.left }
        : null
    }
  })

  assert.ok(metrics.card, 'gantt card is missing')
  assert.ok(metrics.gantt, 'gantt container is missing')
  assert.ok(metrics.card.width >= 1100, `gantt card width is too small: ${metrics.card.width}`)
  assert.ok(metrics.card.height >= 760, `gantt card height is too small: ${metrics.card.height}`)
  assert.ok(metrics.gantt.height >= 700, `gantt height is too small: ${metrics.gantt.height}`)

  const screenshotPath = resolve(outputDir, 'gantt-only.png')
  await page.screenshot({ path: screenshotPath, fullPage: true })

  const evidence = {
    baseUrl,
    tenant,
    username,
    targetPath: '/mes/pro/task',
    forbiddenTexts,
    metrics,
    screenshotPath,
    mesRequests: network
  }
  writeFileSync(resolve(outputDir, 'gantt-only-result.json'), JSON.stringify(evidence, null, 2), 'utf8')

  console.log(`mes-pro-task gantt-only real page check passed: ${screenshotPath}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
