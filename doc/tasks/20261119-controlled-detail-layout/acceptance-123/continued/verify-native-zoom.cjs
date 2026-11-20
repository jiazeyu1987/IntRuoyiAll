const { createRequire } = require('node:module')
const req = createRequire('C:/IntRuoyiAll-int_main/IntRuoyiFronted/package.json')
const { chromium, expect: baseExpect } = req('playwright/test')
const expect = baseExpect.configure({ timeout: 30000 })
const fs = require('node:fs/promises')
const path = require('node:path')

async function main() {
  const context = await chromium.launchPersistentContext(path.join(__dirname, 'zoom-profile'), {
    channel: 'chromium', headless: true, viewport: { width: 1440, height: 960 }
  })
  const settings = context.pages()[0]
  const page = await context.newPage()
  page.setDefaultTimeout(15000)
  const report = { settings: [], samples: [], errors: [], pageErrors: [] }
  page.on('pageerror', error => report.pageErrors.push(error.message))
  page.on('response', async response => {
    if (!response.url().includes('/admin-api/') || response.request().method() === 'OPTIONS') return
    try {
      const body = await response.json()
      if (body.code !== 0) report.errors.push({ path: new URL(response.url()).pathname, code: body.code, message: body.msg })
    } catch (error) {
      if (response.status() >= 400) report.errors.push({ path: new URL(response.url()).pathname, status: response.status(), message: error.message })
    }
  })
  try {
    await settings.goto('chrome://settings/appearance')
    await settings.locator('#zoomLevel').selectOption('1')
    await page.goto('http://localhost:8061', { waitUntil: 'domcontentloaded' })
    await expect.poll(async () => (await page.getByPlaceholder('请输入用户名', { exact: true }).isVisible()) || (await page.getByText('文控中心', { exact: true }).isVisible()), { timeout: 15000 }).toBe(true)
    if (await page.getByPlaceholder('请输入用户名', { exact: true }).isVisible()) {
      await page.getByRole('combobox').click()
      await page.getByRole('option', { name: '芋道源码', exact: true }).click()
      await page.getByPlaceholder('请输入用户名', { exact: true }).fill('admin')
      await page.getByPlaceholder('请输入密码', { exact: true }).filter({ visible: true }).fill('admin123')
      await page.getByRole('button', { name: '登录', exact: true }).click()
    }
    await page.getByText('文控中心', { exact: true }).waitFor()
    await page.getByText('文控中心', { exact: true }).click()
    await page.getByRole('menuitem', { name: '受控浏览', exact: true }).click()
    for (const number of ['LAOWU', '123456', 'CHINA']) {
      await page.getByPlaceholder('文件名称、编号或项目名称').fill(number)
      await page.getByRole('button', { name: '查询', exact: true }).last().click()
      const row = page.getByRole('row').filter({ hasText: `编号：${number}` }).first()
      await expect(row).toBeVisible()
      await row.getByRole('button', { name: '操作面板', exact: true }).click()
      await expect(page.getByRole('tab', { name: '概览', exact: true })).toBeVisible()
      for (const zoom of ['1', '1.25']) {
        await settings.locator('#zoomLevel').selectOption(zoom)
        await expect(settings.locator('#zoomLevel')).toHaveValue(zoom)
        const selectedLabel = await settings.locator('#zoomLevel').evaluate(e => e.options[e.selectedIndex].text)
        report.settings.push({ number, zoom, selectedLabel })
        await page.reload({ waitUntil: 'domcontentloaded' })
        await expect(page.getByRole('tab', { name: '概览', exact: true })).toBeVisible()
        await expect.poll(() => page.evaluate(() => devicePixelRatio)).toBe(Number(zoom))
        for (const tab of ['概览', '版本与生效', '审批与追溯', '分发与培训']) {
          await page.getByRole('tab', { name: tab, exact: true }).click()
          const panels = { 概览: 'dcc-controlled-detail-overview', 版本与生效: 'dcc-detail-version-panel', 审批与追溯: 'dcc-controlled-detail-approval', 分发与培训: 'dcc-detail-distribution-panel' }
          if (tab === '审批与追溯') {
            await expect(page.getByTestId(panels[tab]).locator('.el-loading-mask')).toBeHidden({ timeout: 45000 })
          }
          await page.waitForLoadState('networkidle', { timeout: 45000 })
          const metrics = await page.evaluate(() => {
            const heading = document.querySelector('.controlled-detail-title') || document.querySelector('h1')
            return {
              dpr: devicePixelRatio, innerWidth, scrollWidth: document.documentElement.scrollWidth,
              rootZoom: getComputedStyle(document.documentElement).zoom, bodyZoom: getComputedStyle(document.body).zoom,
              title: heading ? { text: heading.textContent, fontSize: getComputedStyle(heading).fontSize, lineHeight: getComputedStyle(heading).lineHeight } : null,
              actions: Array.from(document.querySelectorAll('button')).filter(e => ['返回', '更多', '更多操作'].includes(e.textContent.trim())).map(e => ({ text: e.textContent.trim(), right: e.getBoundingClientRect().right, width: e.getBoundingClientRect().width })),
              cards: Array.from(document.querySelectorAll('.controlled-detail-version__top > section, .controlled-detail-grid > section')).filter(e => e.getBoundingClientRect().width > 0).map(e => ({ left: e.getBoundingClientRect().left, right: e.getBoundingClientRect().right, top: e.getBoundingClientRect().top, bottom: e.getBoundingClientRect().bottom }))
            }
          })
          expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.innerWidth)
          expect(metrics.rootZoom).toBe('1')
          expect(metrics.bodyZoom).toBe('1')
          expect(metrics.actions.length).toBe(3)
          for (const action of metrics.actions) expect(action.right).toBeLessThanOrEqual(metrics.innerWidth)
          if (metrics.cards.length === 2) {
            expect(Math.abs(metrics.cards[0].top - metrics.cards[1].top)).toBeLessThan(1)
            expect(Math.abs(metrics.cards[0].bottom - metrics.cards[1].bottom)).toBeLessThan(1)
          }
          report.samples.push({ number, zoom, tab, ...metrics })
          const screenshot = await page.screenshot({ path: path.join(__dirname, `native-${number}-${zoom}-${tab}.png`), fullPage: true })
          report.samples[report.samples.length - 1].screenshotSize = { width: screenshot.readUInt32BE(16), height: screenshot.readUInt32BE(20) }
          if (zoom === '1.25') {
            const session = await context.newCDPSession(page)
            const capture = await session.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
            await fs.writeFile(path.join(__dirname, `native-device-${number}-${tab}.png`), Buffer.from(capture.data, 'base64'))
            await session.detach()
          }
        }
      }
      await settings.locator('#zoomLevel').selectOption('1')
      await page.getByRole('button', { name: '返回', exact: true }).click()
    }
    report.completed = true
  } catch (error) {
    report.failure = { message: error.message, url: page.url(), body: await page.locator('body').innerText({ timeout: 3000 }).catch(e => `Unreadable: ${e.message}`) }
    process.exitCode = 1
  } finally {
    await fs.writeFile(path.join(__dirname, 'native-zoom.json'), JSON.stringify(report, null, 2))
    await context.close()
  }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
