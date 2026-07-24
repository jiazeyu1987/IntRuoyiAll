import path from 'node:path'
import { createRequire } from 'node:module'

const frontendRequire = createRequire(path.resolve('package.json'))
const { chromium } = frontendRequire('playwright')

const tenant = '测试租户'

async function login(page) {
  await page.goto('http://localhost:8081/login?redirect=%2Fmes%2Fpro%2Fbatch-record-template', {
    waitUntil: 'domcontentloaded'
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.fill(tenant)
  await tenantInput.press('Enter')
  await form.locator('input.el-input__inner').nth(0).fill('aoteman')
  await form.locator('input[type="password"]').first().fill('111111')
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  if (!loginResponse.ok() || ![0, 200].includes(payload.code)) {
    throw new Error(`login failed: ${JSON.stringify(payload)}`)
  }
  await page.waitForTimeout(1500)
}

async function openTarget(page) {
  await page.goto('http://localhost:8081/mes/pro/batch-record-template', {
    waitUntil: 'domcontentloaded'
  })
  const report = page
    .locator('.batch-record-report-list__item')
    .filter({ hasText: '精洗工序生产记录' })
    .first()
  await report.click()
  await page.waitForTimeout(5000)
}

async function waitForVisiblePreviewFrame(page) {
  await page.waitForFunction(() => {
    const frames = Array.from(document.querySelectorAll('.batch-record-template-preview__frame iframe'))
    return frames.some((node) => {
      const rect = node.getBoundingClientRect()
      const style = getComputedStyle(node)
      return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0
    })
  }, { timeout: 15000 })
}

async function collect(page, label) {
  const metrics = await page.evaluate(() => {
    const frames = Array.from(document.querySelectorAll('.batch-record-template-preview__frame iframe'))
    const visibleFrame = frames.find((node) => {
      const rect = node.getBoundingClientRect()
      const style = getComputedStyle(node)
      return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0
    })
    if (!visibleFrame || !visibleFrame.contentWindow) {
      return { error: 'visible iframe not found' }
    }

    const doc = visibleFrame.contentWindow.document
    const sheet = doc.querySelector('#jm-sheet-wrapper .jm-sheet')
    const wrapper = doc.querySelector('#jm-sheet-wrapper')
    const table = doc.querySelector('#jm-sheet-wrapper table')
    const fillFormRoot = doc.querySelector('#fillFormView-app .viewApp')
    const fillFormMain = doc.querySelector('#fillFormView-app .mainContent')
    const fillFormContainer = doc.querySelector('#fillFormView-app .area-container')
    const fillFormContent = doc.querySelector('#fillFormView-app .area-content')
    const rectOf = (node) => {
      if (!node) return null
      const rect = node.getBoundingClientRect()
      return {
        left: rect.left,
        right: rect.right,
        top: rect.top,
        width: rect.width,
        height: rect.height
      }
    }
    const cells = Array.from(doc.querySelectorAll('#jm-sheet-wrapper td, #jm-sheet-wrapper th'))
    const rightMost = cells.reduce(
      (best, cell) => {
        const rect = cell.getBoundingClientRect()
        return rect.right > best.right
          ? {
              right: rect.right,
              left: rect.left,
              width: rect.width,
              text: (cell.textContent || '').trim()
            }
          : best
      },
      { right: -Infinity, left: 0, width: 0, text: '' }
    )

    const iframeRect = visibleFrame.getBoundingClientRect()
    return {
      iframeRect: rectOf(visibleFrame),
      iframeStyle: visibleFrame.getAttribute('style'),
      readyState: doc.readyState,
      docWidth: doc.documentElement.scrollWidth,
      bodyWidth: doc.body.scrollWidth,
      sheetRect: rectOf(sheet),
      wrapperRect: rectOf(wrapper),
      tableRect: rectOf(table),
      fillFormRootRect: rectOf(fillFormRoot),
      fillFormMainRect: rectOf(fillFormMain),
      fillFormContainerRect: rectOf(fillFormContainer),
      fillFormContentRect: rectOf(fillFormContent),
      sheetScrollWidth: sheet?.scrollWidth ?? null,
      sheetOffsetWidth: sheet?.offsetWidth ?? null,
      wrapperScrollWidth: wrapper?.scrollWidth ?? null,
      wrapperOffsetWidth: wrapper?.offsetWidth ?? null,
      fillFormMainScrollWidth: fillFormMain?.scrollWidth ?? null,
      fillFormContainerScrollWidth: fillFormContainer?.scrollWidth ?? null,
      fillFormContentScrollWidth: fillFormContent?.scrollWidth ?? null,
      transform: sheet instanceof HTMLElement ? sheet.style.transform : '',
      fillFormTransform: fillFormContent instanceof HTMLElement ? fillFormContent.style.transform : '',
      fillFormContainerStyle: fillFormContainer instanceof HTMLElement ? fillFormContainer.getAttribute('style') : '',
      fillFormContentStyle: fillFormContent instanceof HTMLElement ? fillFormContent.getAttribute('style') : '',
      rightMost
    }
  })

  return { label, metrics }
}

async function inspectStructure(page) {
  return await page.evaluate(() => {
    const frames = Array.from(document.querySelectorAll('.batch-record-template-preview__frame iframe'))
    const visibleFrame = frames.find((node) => {
      const rect = node.getBoundingClientRect()
      const style = getComputedStyle(node)
      return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0
    })
    if (!visibleFrame || !visibleFrame.contentWindow) {
      return { error: 'visible iframe not found' }
    }

    const doc = visibleFrame.contentWindow.document
    const nodes = Array.from(doc.querySelectorAll('#jm-sheet-wrapper *'))
      .map((node) => {
        const rect = node.getBoundingClientRect()
        return {
          tag: node.tagName,
          className: node.className,
          text: (node.textContent || '').trim().slice(0, 40),
          left: rect.left,
          right: rect.right,
          width: rect.width,
          height: rect.height,
          display: getComputedStyle(node).display,
          position: getComputedStyle(node).position,
          overflow: getComputedStyle(node).overflow
        }
      })
      .filter((node) => node.width > 0 || node.height > 0 || /canvas|svg|cell|table|row|col|content/i.test(node.className))
      .slice(0, 200)

    return {
      wrapperHtml: doc.querySelector('#jm-sheet-wrapper')?.outerHTML.slice(0, 4000) || null,
      sheetHtml: doc.querySelector('#jm-sheet-wrapper .jm-sheet')?.outerHTML.slice(0, 4000) || null,
      wrapperChildren: Array.from(doc.querySelector('#jm-sheet-wrapper')?.children || []).map((node) => ({
        tag: node.tagName,
        className: node.className,
        html: node.outerHTML.slice(0, 3000),
        width: node.getBoundingClientRect().width,
        height: node.getBoundingClientRect().height,
        left: node.getBoundingClientRect().left,
        right: node.getBoundingClientRect().right,
        display: getComputedStyle(node).display,
        position: getComputedStyle(node).position,
        overflow: getComputedStyle(node).overflow
      })),
      nodes
    }
  })
}

const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })

try {
  const context = await browser.newContext({ viewport: { width: 1600, height: 980 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)

  await login(page)
  await openTarget(page)
  await waitForVisiblePreviewFrame(page)
  const first = await collect(page, 'first')
  const structure = await inspectStructure(page)

  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  await openTarget(page)
  await waitForVisiblePreviewFrame(page)
  const refreshed = await collect(page, 'refreshed')

  console.log(JSON.stringify({ first, refreshed, structure }, null, 2))
} finally {
  await browser.close()
}
