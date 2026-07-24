const fs = require('fs')
const os = require('os')
const path = require('path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260529-dcc-control-center-buttons-e2e'
const evidenceDir = path.join(workspaceRoot, 'doc/tasks', taskId)
const evidencePath = path.join(evidenceDir, 'dcc-control-center-buttons-real-flow-evidence.json')

const config = {
  baseUrl: process.env.DCC_E2E_BASE_URL || 'http://127.0.0.1:8098',
  testTenant: process.env.DCC_E2E_TEST_TENANT || '测试租户',
  testUsername: process.env.DCC_E2E_TEST_USERNAME || 'aoteman',
  testPassword: process.env.DCC_E2E_TEST_PASSWORD || 'admin123',
  adminTenant: process.env.DCC_E2E_ADMIN_TENANT || '芋道源码',
  adminUsername: process.env.DCC_E2E_ADMIN_USERNAME || 'admin',
  adminPassword: process.env.DCC_E2E_ADMIN_PASSWORD || 'admin123',
  headless: process.env.DCC_E2E_HEADLESS !== 'false'
}

const dccRoutes = [
  ['directories', '/dcc/controlled-file/directories'],
  ['categories', '/dcc/controlled-file/categories'],
  ['positions', '/dcc/controlled-file/positions'],
  ['routes', '/dcc/controlled-file/routes'],
  ['distribution-training', '/dcc/controlled-file/categories?tab=distribution-training'],
  ['upload', '/dcc/controlled-file/upload'],
  ['browser', '/dcc/controlled-file/browser'],
    ['approval-tasks', '/dcc/controlled-file/approval-tasks'],
  ['signatures', '/dcc/controlled-file/signatures'],
  ['training-mine', '/dcc/controlled-file/training-mine'],
  ['print-template', '/dcc/controlled-file/print-template'],
  ['external-review', '/dcc/controlled-file/external-review']
]

const evidence = {
  taskId,
  startedAt: new Date().toISOString(),
  baseUrl: config.baseUrl,
  accounts: [
    { role: 'test-writer', tenant: config.testTenant, username: config.testUsername },
    { role: 'admin-verifier', tenant: config.adminTenant, username: config.adminUsername }
  ],
  actions: [],
  apiFailures: [],
  pageFailures: [],
  requestFailures: [],
  consoleFailures: [],
  routeSnapshots: []
}

let currentStep = 'bootstrap'

function record(status, label, detail = {}) {
  evidence.actions.push({
    status,
    step: currentStep,
    label,
    detail,
    at: new Date().toISOString()
  })
}

async function runStep(label, fn) {
  currentStep = label
  const beforeFailureCount = evidence.apiFailures.length + evidence.pageFailures.length
  try {
    const detail = await fn()
    const afterFailureCount = evidence.apiFailures.length + evidence.pageFailures.length
    if (afterFailureCount > beforeFailureCount) {
      throw new Error(`step collected ${afterFailureCount - beforeFailureCount} runtime failure(s)`)
    }
    record('PASS', label, detail || {})
  } catch (error) {
    record('FAIL', label, { message: error.message })
    throw error
  }
}

function isVisibleElement(element) {
  const rect = element.getBoundingClientRect()
  const style = window.getComputedStyle(element)
  return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
}

async function visibleButtons(page) {
  return await page.locator('button:visible').evaluateAll((nodes) =>
    nodes
      .map((node, domIndex) => ({
        domIndex,
        text: (node.innerText || node.getAttribute('aria-label') || node.title || '').trim().replace(/\s+/g, ' '),
        disabled: node.disabled || node.getAttribute('aria-disabled') === 'true',
        className: node.className
      }))
      .filter((button) => button.text)
  )
}

async function findButton(page, text, { exact = true, index = 0 } = {}) {
  const buttons = await visibleButtons(page)
  const matches = buttons.filter((button) => (exact ? button.text === text : button.text.includes(text)))
  if (matches.length <= index) {
    throw new Error(`button not found: ${text}; visible=${buttons.map((button) => button.text).join(' | ')}`)
  }
  return matches[index]
}

async function clickButton(page, text, options = {}) {
  const button = await findButton(page, text, options)
  if (button.disabled && !options.allowDisabled) {
    throw new Error(`button is disabled: ${text}`)
  }
  await page.locator('button:visible').nth(button.domIndex).click({ timeout: 15000 })
  await settle(page)
  return button
}

async function clickIfPresent(page, text, options = {}) {
  const buttons = await visibleButtons(page)
  const index = buttons.findIndex((button) => (options.exact === false ? button.text.includes(text) : button.text === text))
  if (index < 0) {
    return false
  }
  const button = buttons[index]
  if (button.disabled) {
    return false
  }
  await page.locator('button:visible').nth(button.domIndex).click({ timeout: 15000 })
  await settle(page)
  return true
}

async function assertDisabled(page, text) {
  const button = await findButton(page, text)
  if (!button.disabled) {
    throw new Error(`expected disabled button: ${text}`)
  }
  return button
}

async function assertButtonAbsent(page, text) {
  const buttons = await visibleButtons(page)
  const match = buttons.find((button) => button.text === text)
  if (match) {
    throw new Error(`expected button to be absent: ${text}`)
  }
}

async function waitForEnabledButton(page, text, timeout = 30000) {
  await page.waitForFunction((label) => {
    const normalize = (value) => (value || '').trim().replace(/\s+/g, ' ')
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    }
    return Array.from(document.querySelectorAll('button')).some((button) =>
      isVisible(button) &&
      normalize(button.innerText || button.getAttribute('aria-label') || button.title) === label &&
      !button.disabled &&
      button.getAttribute('aria-disabled') !== 'true'
    )
  }, text, { timeout })
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function assertHealthyPage(page, routeName) {
  const body = await page.locator('body').innerText({ timeout: 15000 })
  const forbidden = ['Access Denied', '权限不足', '无权访问', '閲嶇疆', 'DCC鎴戠殑鍩硅']
  const found = forbidden.find((token) => body.includes(token))
  if (found) {
    throw new Error(`${routeName} contains forbidden text: ${found}`)
  }
  return body
}

async function gotoDcc(page, route) {
  await page.goto(`${config.baseUrl}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
}

async function login(page, tenant, username, password) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(800)
  const tenantInput = page.locator('.login-form:visible .el-select input[role="combobox"]').first()
  await tenantInput.click()
  await tenantInput.fill(tenant)
  await tenantInput.press('Enter')
  await page.locator('.login-form:visible input[placeholder="请输入用户名"]').fill(username)
  await page.locator('.login-form:visible input[placeholder="请输入密码"]').fill(password)
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200, {
      timeout: 60000
    }),
    page.locator('.login-form:visible .el-button--primary').first().click()
  ])
  await page.waitForFunction(() => location.pathname !== '/login', null, { timeout: 60000 })
  await settle(page)
}

function attachRuntimeWatchers(page, label) {
  page.on('pageerror', (error) => {
    evidence.pageFailures.push({
      label,
      step: currentStep,
      name: error.name || '',
      message: error.message,
      stack: (error.stack || '').slice(0, 2000)
    })
  })
  page.on('requestfailed', (request) => {
    const url = request.url()
    if (!url.includes('/admin-api/')) {
      return
    }
    evidence.requestFailures.push({
      label,
      step: currentStep,
      method: request.method(),
      url,
      failure: request.failure()?.errorText || ''
    })
  })
  page.on('console', async (message) => {
    if (message.type() !== 'error') {
      return
    }
    const text = message.text()
    if (!text.includes('AxiosError') && !text.includes('/admin-api/') && !text.includes('Uncaught')) {
      return
    }
    const values = []
    for (const arg of message.args().slice(0, 3)) {
      try {
        values.push(JSON.stringify(await arg.jsonValue()).slice(0, 1000))
      } catch (error) {
        values.push(`console arg read failed: ${error.message}`)
      }
    }
    evidence.consoleFailures.push({ label, step: currentStep, type: message.type(), text, values })
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/dcc') && !url.includes('/admin-api/bpm')) {
      return
    }
    const status = response.status()
    let body = ''
    if (status >= 400) {
      try {
        body = (await response.text()).slice(0, 1000)
      } catch (error) {
        body = `body read failed: ${error.message}`
      }
      evidence.apiFailures.push({ label, step: currentStep, status, url, body })
      return
    }
    const contentType = response.headers()['content-type'] || ''
    if (!contentType.includes('application/json')) {
      return
    }
    try {
      const json = await response.json()
      const businessFailed =
        (Object.prototype.hasOwnProperty.call(json, 'code') && json.code !== 0) || json.success === false
      if (businessFailed) {
        evidence.apiFailures.push({
          label,
          step: currentStep,
          status,
          url,
          body: JSON.stringify(json).slice(0, 1000)
        })
      }
    } catch (error) {
      evidence.apiFailures.push({ label, step: currentStep, status, url, body: `json parse failed: ${error.message}` })
    }
  })
}

async function closeTopOverlay(page) {
  const closed = await page.evaluate(() => {
    const overlays = Array.from(document.querySelectorAll('.el-message-box, .el-dialog, .el-drawer')).filter((element) => {
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    })
    const overlay = overlays[overlays.length - 1]
    if (!overlay) {
      return false
    }
    const buttons = Array.from(overlay.querySelectorAll('button')).filter((button) => {
      const rect = button.getBoundingClientRect()
      return rect.width > 0 && rect.height > 0
    })
    const closeButton = buttons.find((button) => /取消|关闭|返回/.test((button.innerText || '').trim()))
      || overlay.querySelector('.el-dialog__headerbtn,.el-message-box__headerbtn,.el-drawer__close-btn')
    if (closeButton) {
      closeButton.click()
      return true
    }
    return false
  })
  if (!closed) {
    await page.keyboard.press('Escape').catch(() => {})
  }
  await settle(page)
}

async function expectDialogFromButton(page, buttonText, expectedText) {
  await clickButton(page, buttonText)
  const overlaySelector = '.el-dialog:visible, .el-message-box:visible, .el-drawer:visible'
  await page.waitForSelector(overlaySelector, { timeout: 10000 })
  const overlayText = await page.locator(overlaySelector).last().innerText()
  if (expectedText && !overlayText.includes(expectedText)) {
    throw new Error(`dialog for ${buttonText} did not contain ${expectedText}; actual=${overlayText.slice(0, 300)}`)
  }
  await closeTopOverlay(page)
}

async function expectConfirmAndCancel(page, buttonText) {
  await clickButton(page, buttonText)
  await page.waitForSelector('.el-message-box:visible, .el-dialog:visible', { timeout: 10000 })
  await closeTopOverlay(page)
}

async function clickAndStayHealthy(page, buttonText, options = {}) {
  await clickButton(page, buttonText, options)
  await assertHealthyPage(page, `${buttonText} click`)
}

async function clickAndReturn(page, buttonText, expectedPathPart) {
  const beforeUrl = page.url()
  await clickButton(page, buttonText)
  if (expectedPathPart && !page.url().includes(expectedPathPart)) {
    throw new Error(`button ${buttonText} did not navigate to ${expectedPathPart}; url=${page.url()}`)
  }
  await assertHealthyPage(page, buttonText)
  await page.goto(beforeUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
}

async function clickAndClosePopup(page, buttonText, expectedPathPart, options = {}) {
  const popupPromise = page.waitForEvent('popup', { timeout: 30000 })
  await clickButton(page, buttonText, options)
  const popup = await popupPromise
  await popup.waitForLoadState('domcontentloaded', { timeout: 60000 })
  await settle(popup)
  if (expectedPathPart && !popup.url().includes(expectedPathPart)) {
    const actualUrl = popup.url()
    await popup.close()
    throw new Error(`button ${buttonText} did not open ${expectedPathPart}; url=${actualUrl}`)
  }
  await assertHealthyPage(popup, buttonText)
  await popup.close()
}

function writeTempFile(name, content) {
  const dir = path.join(os.tmpdir(), taskId)
  fs.mkdirSync(dir, { recursive: true })
  const filePath = path.join(dir, name)
  fs.writeFileSync(filePath, content)
  return filePath
}

async function chooseFile(page, buttonText, filePath) {
  const [fileChooser] = await Promise.all([
    page.waitForEvent('filechooser', { timeout: 10000 }),
    clickButton(page, buttonText)
  ])
  await fileChooser.setFiles(filePath)
  await settle(page)
}

async function clickDownload(page, buttonText, options = {}) {
  const downloadPromise = page.waitForEvent('download', { timeout: options.timeout || 15000 }).catch(() => null)
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/') &&
        response.url().includes('/download') &&
        response.status() >= 200 &&
        response.status() < 300,
      { timeout: options.timeout || 30000 }
    )
    .catch(() => null)
  await clickButton(page, buttonText, options)
  await clickIfPresent(page, '确认下载')
  const download = await downloadPromise
  const response = await responsePromise
  if (!download && !response) {
    throw new Error(`button did not produce a download response: ${buttonText}`)
  }
  return {
    suggestedFilename: download?.suggestedFilename() || '',
    contentDisposition: response?.headers()['content-disposition'] || ''
  }
}

async function snapshotRoute(page, tenantRole, routeName, routePath) {
  await gotoDcc(page, routePath)
  const body = await assertHealthyPage(page, routeName)
  const buttons = await visibleButtons(page)
  evidence.routeSnapshots.push({
    tenantRole,
    routeName,
    routePath,
    url: page.url(),
    buttons: buttons.map(({ text, disabled }) => ({ text, disabled })),
    bodySample: body.slice(0, 800)
  })
  return buttons
}

async function runTestTenantActions(page) {
  await runStep('test tenant login', async () => {
    await login(page, config.testTenant, config.testUsername, config.testPassword)
    return { url: page.url() }
  })

  await runStep('directories buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'directories', '/dcc/controlled-file/directories')
    await clickAndStayHealthy(page, '查询')
    await clickAndStayHealthy(page, '重置')
    await expectDialogFromButton(page, '新建目录', '新建目录')
    await clickAndStayHealthy(page, '刷新目录树')
    await assertDisabled(page, 'NAS同步')
    await clickAndStayHealthy(page, '全部展开')
    await clickAndStayHealthy(page, '全部折叠')
    await expectDialogFromButton(page, '新建', '新建目录')
    await expectDialogFromButton(page, '编辑', '编辑目录')
    await expectConfirmAndCancel(page, '删除')
  })

  await runStep('categories buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'categories', '/dcc/controlled-file/categories')
    await clickAndStayHealthy(page, '查询')
    await expectDialogFromButton(page, '新增类别', '文件类别')
    await expectDialogFromButton(page, '编辑', '文件类别')
    await expectConfirmAndCancel(page, '删除')
    await clickButton(page, 'DCC审阅矩阵')
    await settle(page)
    await assertHealthyPage(page, 'categories review matrix tab')
    await expectDialogFromButton(page, '预览', '审阅矩阵配置')
    await clickButton(page, '目录授权')
    await settle(page)
    await assertHealthyPage(page, 'categories directory auth tab')
    await clickAndStayHealthy(page, '新增目录')
    await clickAndStayHealthy(page, '新增规则')
    await clickAndStayHealthy(page, '删除')
    await clickAndStayHealthy(page, '保存规则')
    await clickButton(page, '类别列表')
    await settle(page)
  })

  await runStep('positions buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'positions', '/dcc/controlled-file/positions')
    await clickAndStayHealthy(page, '查询')
    await clickAndStayHealthy(page, '重置')
    await clickAndStayHealthy(page, '刷新列表')
    if (!(await clickIfPresent(page, '维护分配'))) {
      await clickButton(page, '新增岗位')
      await page.waitForSelector('.el-dialog:visible', { timeout: 10000 })
      const uniqueName = `DCC E2E 按钮岗位 ${Date.now()}`
      await page.locator('.el-dialog:visible input[placeholder="请输入岗位名称"]').fill(uniqueName)
      await page.locator('.el-dialog:visible textarea[placeholder="请填写新增岗位原因"]').fill('DCC 文控中心按钮 E2E 验证')
      await clickButton(page, '保存岗位')
      await settle(page)
      await findButton(page, '维护分配')
      await clickButton(page, '维护分配')
    }
    await page.waitForSelector('.el-dialog:visible', { timeout: 10000 })
    await clickButton(page, '新增分配')
    await clickButton(page, '删除')
    await clickButton(page, '保存分配')
  })

  await runStep('approval route buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'routes', '/dcc/controlled-file/routes')
    await clickAndStayHealthy(page, '查询路线')
    await clickAndStayHealthy(page, '重置')
    await assertButtonAbsent(page, '审批矩阵')
    await clickAndStayHealthy(page, '刷新预览')
  })

  await runStep('distribution and training rule buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'distribution-training', '/dcc/controlled-file/categories?tab=distribution-training')
    await clickAndStayHealthy(page, '刷新规则')
    await clickAndStayHealthy(page, '重置')
    await assertButtonAbsent(page, '新增分发部门')
    await gotoDcc(page, '/dcc/controlled-file/categories?tab=distribution-training')
    await waitForEnabledButton(page, '新增分发部门')
    const existingDeleteCount = (await visibleButtons(page)).filter((button) => button.text === '删除').length
    await clickAndStayHealthy(page, '新增分发部门')
    await clickAndStayHealthy(page, '删除', { index: existingDeleteCount })
    await clickAndStayHealthy(page, '保存分发规则')
  })

  await runStep('upload buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'upload', '/dcc/controlled-file/upload')
    const sourceFile = writeTempFile('dcc-button-source.txt', 'DCC button E2E source file\n')
    const pdfFile = writeTempFile(
      'dcc-button-drawing.pdf',
      '%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Count 0 >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n'
    )
    await chooseFile(page, '选择文件', sourceFile)
    await chooseFile(page, '选择 PDF', pdfFile)
    await clickButton(page, '提交审批')
    const body = await assertHealthyPage(page, 'upload submit validation')
    if (!/请选择|请输入|审批路线|文件/.test(body)) {
      throw new Error('upload submit did not expose a validation or workflow message')
    }
  })

  await runStep('browser buttons and detail buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'browser', '/dcc/controlled-file/browser')
    await clickAndStayHealthy(page, '全部展开')
    await clickAndStayHealthy(page, '全部折叠')
    await clickAndStayHealthy(page, '刷新')
    await clickAndStayHealthy(page, '查询')
    await clickAndStayHealthy(page, '重置')
    await waitForEnabledButton(page, '刷新列表')
    await clickAndStayHealthy(page, '刷新列表')
    await clickAndClosePopup(page, '预览', 'viewer=1')
    if ((await visibleButtons(page)).some((button) => button.text === '下载' && !button.disabled)) {
      await clickDownload(page, '下载')
    }
    await clickButton(page, '详情')
    await page.waitForURL(/\/dcc\/controlled-file\/detail\//, { timeout: 60000 })
    await settle(page)
    await assertHealthyPage(page, 'browser detail')
    await clickAndClosePopup(page, '预览受控文件', 'viewer=1')
    await clickDownload(page, '下载受控文件')
    await assertDisabled(page, '导出回执')
    await assertDisabled(page, '打印回执')
    await clickButton(page, '返回')
  })

  await runStep('my files buttons', async () => {
    await clickAndStayHealthy(page, '查询')
    await clickAndStayHealthy(page, '重置')
    await clickAndStayHealthy(page, '刷新')
    await clickAndClosePopup(page, '预览', 'viewer=1')
    if ((await visibleButtons(page)).some((button) => button.text === '下载' && !button.disabled)) {
      await clickDownload(page, '下载')
    }
    await clickAndReturn(page, '详情', '/dcc/controlled-file/detail')
    if ((await visibleButtons(page)).some((button) => button.text === '撤回' && !button.disabled)) {
      await expectConfirmAndCancel(page, '撤回')
    }
  })

  await runStep('approval task buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'approval-tasks', '/dcc/controlled-file/approval-tasks')
    await clickAndStayHealthy(page, '查询')
    await clickAndStayHealthy(page, '重置')
    await clickAndReturn(page, '查看审批', '/dcc/controlled-file/detail')
    await clickButton(page, '处理审批')
    await page.waitForURL(/\/dcc\/controlled-file\/detail\//, { timeout: 60000 })
    await settle(page)
    await assertHealthyPage(page, 'approval processing detail')
    for (const action of ['处理回退', '驳回', '转办', '加签', '撤回申请']) {
      if ((await visibleButtons(page)).some((button) => button.text === action && !button.disabled)) {
        await expectDialogFromButton(page, action)
      }
    }
    await clickButton(page, '返回')
  })

  await runStep('signature buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'signatures', '/dcc/controlled-file/signatures')
    await clickAndStayHealthy(page, '查询')
    await clickAndStayHealthy(page, '重置')
    await expectDialogFromButton(page, '查看证据', '签名证据')
    await page.locator('.el-tabs__item:visible').filter({ hasText: '签名授权' }).click()
    await settle(page)
    await clickIfPresent(page, '查询')
    await clickIfPresent(page, '重置')
    if ((await visibleButtons(page)).some((button) => button.text === '审计' && !button.disabled)) {
      await expectDialogFromButton(page, '审计')
    }
  })

  await runStep('training mine buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'training-mine', '/dcc/controlled-file/training-mine')
    await clickAndStayHealthy(page, '查询')
    await clickAndStayHealthy(page, '重置')
  })

  await runStep('print template buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'print-template', '/dcc/controlled-file/print-template')
    await clickAndStayHealthy(page, '刷新')
    await clickAndStayHealthy(page, '保存并启用')
  })

  await runStep('external review buttons', async () => {
    await snapshotRoute(page, 'test-writer', 'external-review', '/dcc/controlled-file/external-review')
    const externalFile = writeTempFile('dcc-button-external.txt', 'DCC button E2E external review file\n')
    const externalPdf = writeTempFile(
      'dcc-button-external.pdf',
      '%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Count 0 >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n'
    )
    await chooseFile(page, '选择外来文件', externalFile)
    await chooseFile(page, '选择 PDF', externalPdf)
    await clickButton(page, '提交评审')
    const body = await assertHealthyPage(page, 'external review submit validation')
    if (!/请选择|请输入|审批路线|文件/.test(body)) {
      throw new Error('external review submit did not expose a validation or workflow message')
    }
  })
}

async function runAdminReadonlyVerification(page) {
  await runStep('admin login', async () => {
    await login(page, config.adminTenant, config.adminUsername, config.adminPassword)
    return { url: page.url() }
  })

  for (const [name, route] of dccRoutes) {
    await runStep(`admin readonly route ${name}`, async () => {
      const buttons = await snapshotRoute(page, 'admin-verifier', name, route)
      for (const label of ['查询', '重置', '刷新', '刷新列表', '查询规则', '查询路线', '查询映射']) {
        if (buttons.some((button) => button.text === label && !button.disabled)) {
          await waitForEnabledButton(page, label)
          await clickAndStayHealthy(page, label)
        }
      }
      return { buttons: buttons.length }
    })
  }
}

async function main() {
  fs.mkdirSync(evidenceDir, { recursive: true })
  const browser = await chromium.launch({ headless: config.headless })
  try {
    const testContext = await browser.newContext({
      viewport: { width: 1440, height: 960 },
      locale: 'zh-CN',
      acceptDownloads: true
    })
    const testPage = await testContext.newPage()
    attachRuntimeWatchers(testPage, 'test-writer')
    await runTestTenantActions(testPage)
    await testContext.close()

    const adminContext = await browser.newContext({
      viewport: { width: 1440, height: 960 },
      locale: 'zh-CN',
      acceptDownloads: true
    })
    const adminPage = await adminContext.newPage()
    attachRuntimeWatchers(adminPage, 'admin-verifier')
    await runAdminReadonlyVerification(adminPage)
    await adminContext.close()
  } finally {
    await browser.close()
    evidence.finishedAt = new Date().toISOString()
    fs.writeFileSync(evidencePath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  }

  const fatalRequestFailures = evidence.requestFailures.filter((item) => item.failure !== 'net::ERR_ABORTED')
  if (
    evidence.apiFailures.length ||
    evidence.pageFailures.length ||
    fatalRequestFailures.length ||
    evidence.consoleFailures.length
  ) {
    throw new Error(`E2E collected runtime failures; see ${evidencePath}`)
  }
  console.log(`PASS: DCC control-center real-data button E2E evidence written to ${evidencePath}`)
}

main().catch((error) => {
  evidence.finishedAt = new Date().toISOString()
  evidence.fatalError = error.message
  fs.mkdirSync(evidenceDir, { recursive: true })
  fs.writeFileSync(evidencePath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  console.error(error)
  process.exit(1)
})
