const { createRequire } = require('node:module')
const requireFrontend = createRequire('C:/IntRuoyiAll-int_main/IntRuoyiFronted/package.json')
const { chromium, expect } = requireFrontend('playwright/test')
const fs = require('node:fs/promises')
const path = require('node:path')

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(15000)
  const report = { samples: [], errors: [], pageErrors: [] }
  page.on('pageerror', e => report.pageErrors.push(e.message))
  page.on('response', async response => {
    if (!response.url().includes('/admin-api/') || response.request().method() === 'OPTIONS') return
    try {
      const value = await response.json()
      if (value.code !== 0) report.errors.push({ path: new URL(response.url()).pathname, status: response.status(), code: value.code, message: value.msg })
    } catch (error) {
      if (response.status() >= 400) report.errors.push({ path: new URL(response.url()).pathname, status: response.status(), message: error.message })
    }
  })
  try {
    await page.goto('http://localhost:8061', { waitUntil: 'domcontentloaded' })
    await page.getByRole('combobox').click()
    await page.getByRole('option', { name: '芋道源码', exact: true }).click()
    await page.getByPlaceholder('请输入用户名', { exact: true }).fill('admin')
    await page.getByPlaceholder('请输入密码', { exact: true }).filter({ visible: true }).fill('admin123')
    await page.getByRole('button', { name: '登录', exact: true }).click()
    await page.getByText('文控中心', { exact: true }).waitFor()
    await page.getByText('文控中心', { exact: true }).click()
    await page.getByRole('menuitem', { name: '受控浏览', exact: true }).click()
    await page.getByPlaceholder('文件名称、编号或项目名称').waitFor()
    const requested = process.argv.slice(2)
    for (const number of requested.length ? requested : ['HTML29-VERS', 'E2E1414VERIFY', 'LAOWU', '666666']) {
      const sample = { number, errorStart: report.errors.length }
      report.samples.push(sample)
      await page.getByPlaceholder('文件名称、编号或项目名称').fill(number)
      await page.getByRole('button', { name: '查询', exact: true }).last().click()
      const row = page.getByRole('row').filter({ hasText: `编号：${number}` }).first()
      await expect(row).toBeVisible()
      sample.listText = await row.innerText()
      await row.getByRole('button', { name: '操作面板', exact: true }).click()
      await expect(page.getByRole('tab', { name: '版本与生效', exact: true })).toBeVisible()
      sample.url = page.url()
      await page.getByRole('tab', { name: '版本与生效', exact: true }).click()
      await expect(page.getByRole('heading', { name: '版本历史', exact: true })).toBeVisible()
      sample.versionText = await page.locator('body').innerText()
      await page.getByRole('tab', { name: '审批与追溯', exact: true }).click()
      const panel = page.getByTestId('dcc-controlled-detail-approval')
      await expect(panel).toBeVisible()
      await expect(panel.locator('.el-loading-mask')).toBeHidden({ timeout: 90000 })
      await expect(panel.locator('button.approval-group-title').first()).toBeVisible()
      sample.groups = await panel.locator('button.approval-group-title').allTextContents()
      sample.collapsedText = await panel.innerText()
      sample.defaultCollapsed = await panel.locator('button.approval-group-title').evaluateAll(es => es.every(e => e.getAttribute('aria-expanded') === 'false'))
      expect(sample.defaultCollapsed).toBe(true)
      const replacement = panel.locator('button.approval-group-title').filter({ hasText: '换版变更' })
      sample.replacementCount = await replacement.count()
      if (sample.replacementCount) {
        await replacement.first().click()
        sample.replacementExpandedText = await panel.innerText()
        await expect(replacement.first()).toHaveAttribute('aria-expanded', 'true')
        sample.otherGroupsCollapsed = await panel.locator('button.approval-group-title').evaluateAll(es => es.filter(e => !e.textContent.includes('换版变更')).every(e => e.getAttribute('aria-expanded') === 'false'))
        expect(sample.otherGroupsCollapsed).toBe(true)
      }
      if (number === 'HTML29-VERS') {
        const groupButtons = panel.locator('button.approval-group-title')
        for (let i = 0; i < await groupButtons.count(); i++) {
          if (await groupButtons.nth(i).getAttribute('aria-expanded') === 'false') await groupButtons.nth(i).click()
        }
        sample.allExpandedText = await panel.innerText()
        sample.allExpandedRows = await panel.locator('.el-table__body tbody > tr').count()
      }
      if (number === '666666') {
        const buttons = panel.locator('button.approval-group-title').filter({ hasText: 'A/1' })
        await expect(buttons).toHaveCount(3)
        sample.sameVersionGroups = await buttons.allTextContents()
        await buttons.nth(0).click()
        await expect(buttons.nth(0)).toHaveAttribute('aria-expanded', 'true')
        await expect(buttons.nth(1)).toHaveAttribute('aria-expanded', 'false')
        await expect(buttons.nth(2)).toHaveAttribute('aria-expanded', 'false')
        sample.firstIndependentText = await panel.innerText()
        await buttons.nth(1).click()
        await expect(buttons.nth(0)).toHaveAttribute('aria-expanded', 'true')
        await expect(buttons.nth(1)).toHaveAttribute('aria-expanded', 'true')
        sample.bothIndependentText = await panel.innerText()
        sample.sameVersionIndependent = true
        await panel.locator('.el-select').first().click()
        await page.getByRole('option', { name: 'A/1', exact: true }).click()
        await expect(panel.locator('button.approval-group-title')).toHaveCount(3)
        await panel.locator('.el-select').last().click()
        await page.getByRole('option', { name: '作废申请', exact: true }).click()
        await expect(panel.locator('button.approval-group-title')).toHaveCount(2)
        sample.filteredObsoleteText = await panel.innerText()
        await panel.locator('.el-select').last().hover()
        await panel.locator('.el-select').last().locator('.el-select__clear').click()
        await expect(panel.locator('button.approval-group-title')).toHaveCount(3)
        await panel.locator('.el-select').first().hover()
        await panel.locator('.el-select').first().locator('.el-select__clear').click()
        await expect(panel.locator('button.approval-group-title')).toHaveCount(sample.groups.length)
        sample.clearRestoresGroups = true
      }
      sample.errors = report.errors.slice(sample.errorStart)
      expect(sample.errors.filter(error => error.code !== 1080000012)).toEqual([])
      await page.screenshot({ path: path.join(__dirname, `${number}-after-update.png`), fullPage: true })
      await fs.writeFile(path.join(__dirname, 'rounds-after-update.json'), JSON.stringify(report, null, 2))
      await page.getByRole('button', { name: '返回', exact: true }).click()
      console.log(`${number}: ${sample.groups.length} groups, replacement=${sample.replacementCount}, errors=${sample.errors.length}`)
    }
    report.completed = true
  } catch (error) {
    report.failure = { message: error.message, url: page.url(), body: await page.locator('body').innerText({ timeout: 3000 }).catch(e => `Unreadable: ${e.message}`) }
    await page.screenshot({ path: path.join(__dirname, 'failure.png'), fullPage: true }).catch(() => {})
    process.exitCode = 1
  } finally {
    await fs.writeFile(path.join(__dirname, 'rounds-after-update.json'), JSON.stringify(report, null, 2))
    await browser.close()
  }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
