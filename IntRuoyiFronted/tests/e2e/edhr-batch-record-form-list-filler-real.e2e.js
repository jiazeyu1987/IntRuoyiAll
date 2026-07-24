const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_FORM_LIST_FILLER_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_FORM_LIST_FILLER_TENANT || '测试租户'
const USERNAME = process.env.EDHR_FORM_LIST_FILLER_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_FORM_LIST_FILLER_PASSWORD || '111111'
const TARGET_PATH = '/mes/pro/batch-record-form-list'
const SEARCH_REPORT_NAME = process.env.EDHR_FORM_LIST_FILLER_REPORT_NAME || '粗洗工序生产记录'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-batch-record-form-list-filler')
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function ensurePrerequisites() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 必须固定使用本机前端 http://localhost:8081')
  assert.equal(TENANT, '测试租户', '写入型 E2E 只能使用测试租户')
  assert.equal(USERNAME, 'aoteman', '写入型 E2E 只能使用测试租户账号 aoteman')
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome 不存在: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(RESULT_DIR, { recursive: true })
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && (await item.isEnabled().catch(() => false))) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写控件：${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && !(await item.isDisabled().catch(() => true))) {
      await item.click()
      return
    }
  }
  throw new Error(`缺少可点击控件：${label}`)
}

async function searchTargetReport(page) {
  await page.locator('.table-quick-filter__field').first().click()
  await clickSelectOption(page, '表单名称')
  const valueInput = page.locator('.table-quick-filter__value input').first()
  await valueInput.waitFor({ state: 'visible', timeout: 30000 })
  await valueInput.fill(SEARCH_REPORT_NAME)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickFirstEnabled(page.getByRole('button', { name: '查询' }), '查询')
  await responsePromise
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder*="验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TENANT, '租户')
  }

  await fillFirstVisible(
    loginForm.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    USERNAME,
    '用户名'
  )
  await fillFirstVisible(loginForm.locator('input[type="password"]'), PASSWORD, '密码')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 90000 }
  )
  const permissionInfoPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info'),
    { timeout: 90000 }
  )
  await Promise.all([
    loginResponsePromise,
    clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), '登录')
  ])
  const permissionInfo = await (await permissionInfoPromise).json()
  assert.match(JSON.stringify(permissionInfo.data || permissionInfo), /mes:pro-batch-record-template:query/)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function getVisibleDialog(page) {
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '批记录表单填写人设置' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  return dialog
}

function normalizeSourceLabel(label) {
  if (label.includes('个人')) return '个人'
  if (label.includes('角色')) return '角色'
  throw new Error(`无法识别填写人来源：${label}`)
}

async function readDialogSelection(page) {
  return page.evaluate(() => {
    const visible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      return rect.width > 0 && rect.height > 0
    }
    const dialog = [...document.querySelectorAll('.el-dialog')].find((element) => visible(element) && element.innerText.includes('批记录表单填写人设置'))
    if (!dialog) throw new Error('批记录表单填写人设置弹窗未打开')
    const formItems = [...dialog.querySelectorAll('.el-form-item')]
    const findItem = (label) => {
      const item = formItems.find((element) => {
        const labelElement = element.querySelector('.el-form-item__label')
        return labelElement && labelElement.textContent.trim() === label
      })
      if (!item) throw new Error(`缺少表单项：${label}`)
      return item
    }
    const sourceItem = findItem('填写人来源')
    const sourceLabel = sourceItem.querySelector('.el-select__selected-item, .el-select__placeholder, input')?.textContent?.trim() ||
      sourceItem.querySelector('input')?.value?.trim() || ''
    const targetItem = findItem('填写人')
    const candidateLabels = [...targetItem.querySelectorAll('.el-tag__content, .el-select__tags-text')]
      .map((element) => element.textContent.replace(/×/g, '').trim())
      .filter(Boolean)
    return { sourceLabel, candidateLabels }
  })
}

async function clickSelectOption(page, label) {
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item, .el-popper[aria-hidden="false"] .el-select-dropdown__item')
    .filter({ hasText: label })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function sourceItem(dialog) {
  return dialog.locator('.el-form-item').filter({ hasText: '填写人来源' }).first()
}

async function candidateItem(dialog) {
  return dialog.locator('.el-form-item').filter({ hasText: /^填写人/ }).nth(1)
}

async function setSource(page, dialog, label) {
  const item = await sourceItem(dialog)
  await item.locator('.el-select').first().click()
  await clickSelectOption(page, label)
}

async function clearCandidates(dialog) {
  const item = await candidateItem(dialog)
  for (let guard = 0; guard < 20; guard += 1) {
    const close = item.locator('.el-tag__close').first()
    if ((await close.count()) === 0 || !(await close.isVisible().catch(() => false))) return
    await close.click()
  }
}

async function selectCandidateByLabel(page, dialog, label) {
  const item = await candidateItem(dialog)
  const select = item.locator('.el-select').first()
  await select.click()
  const input = item.locator('input[role="combobox"], input.el-select__input').first()
  if ((await input.count()) > 0 && (await input.isVisible().catch(() => false))) {
    await input.fill(label)
  }
  const alreadySelected = await page.evaluate(({ expected }) => {
    const visible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      return rect.width > 0 && rect.height > 0
    }
    const dialog = [...document.querySelectorAll('.el-dialog')].find((element) => visible(element) && element.innerText.includes('批记录表单填写人设置'))
    const labels = [...(dialog?.querySelectorAll('.el-tag__content, .el-select__tags-text') || [])]
      .map((element) => element.textContent.replace(/×/g, '').trim())
      .filter(Boolean)
    return labels.includes(expected)
  }, { expected: label })
  if (alreadySelected) return
  await clickSelectOption(page, label)
  await page.waitForFunction(
    ({ expected }) => {
      const visible = (element) => {
        if (!(element instanceof HTMLElement)) return false
        const rect = element.getBoundingClientRect()
        return rect.width > 0 && rect.height > 0
      }
      const dialog = [...document.querySelectorAll('.el-dialog')].find((element) => visible(element) && element.innerText.includes('批记录表单填写人设置'))
      const labels = [...(dialog?.querySelectorAll('.el-tag__content, .el-select__tags-text') || [])]
        .map((element) => element.textContent.replace(/×/g, '').trim())
        .filter(Boolean)
      return labels.includes(expected)
    },
    { expected: label },
    { timeout: 30000 }
  )
}

async function selectFirstCandidate(page, dialog) {
  const item = await candidateItem(dialog)
  await item.locator('.el-select').first().click()
  await page.keyboard.press('ArrowDown')
  await page.keyboard.press('Enter')
  await page.waitForFunction(() => {
    const visible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      return rect.width > 0 && rect.height > 0
    }
    const dialog = [...document.querySelectorAll('.el-dialog')].find((element) => visible(element) && element.innerText.includes('批记录表单填写人设置'))
    const labels = [...(dialog?.querySelectorAll('.el-tag__content, .el-select__tags-text') || [])]
      .map((element) => element.textContent.replace(/×/g, '').trim())
      .filter(Boolean)
    return labels.length > 0
  }, null, { timeout: 30000 })
  const selection = await readDialogSelection(page)
  const label = selection.candidateLabels.at(-1)
  assert.ok(label, '候选填写人选项不能为空')
  return label
}

async function saveDialog(page, expectedSourceLabel, expectedCandidateLabels) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-process-form-permission-rule/save-by-report') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(page.getByRole('button', { name: '保存填写设置' }), '保存填写设置')
  const response = await responsePromise
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `保存失败：${body.msg || body.code}`)
  assert.equal(body.data.fillRuleStatus, 'CONFIGURED')
  assert.ok(Number(body.data.affectedRouteBindingCount) > 0, '保存结果必须同步至少一个真实路线绑定')
  const expectedSource = normalizeSourceLabel(expectedSourceLabel)
  if (expectedSource === '角色') {
    assert.equal(body.data.fillRule?.candidateSourceType, 'ROLE', '保存结果必须保留角色来源')
    assert.ok(body.data.fillRule?.candidateSourceIds?.length > 0, '保存结果必须保留所选角色 ID')
  } else {
    assert.ok(
      ['USER', 'USERS'].includes(body.data.fillRule?.candidateSourceType),
      '保存结果必须保留个人来源'
    )
  }
  const savedNames = (body.data.fillRule?.candidateUsers || []).map((item) => item.displayName).filter(Boolean)
  if (expectedSource === '角色') {
    assert.ok(savedNames.length > 0, '角色保存后必须能解析到至少一个真实候选用户')
  } else {
    for (const label of expectedCandidateLabels) {
      assert.ok(savedNames.includes(label), `保存结果缺少候选人：${label}`)
    }
  }
  await page.locator('.el-dialog:visible').filter({ hasText: '批记录表单填写人设置' }).waitFor({ state: 'hidden', timeout: 30000 })
  return body.data
}

async function closePermissionDialog(page) {
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '批记录表单填写人设置' }).first()
  if ((await dialog.count()) === 0) return
  const cancel = dialog.getByRole('button', { name: '取消' }).first()
  if ((await cancel.count()) > 0 && (await cancel.isVisible().catch(() => false))) {
    await cancel.click({ timeout: 5000, force: true }).catch(async () => {
      await page.keyboard.press('Escape')
    })
  } else {
    await page.keyboard.press('Escape')
  }
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => undefined)
}

async function openFirstUsableFillerDialog(page, options = {}) {
  const { requireConfigured = false } = options
  await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.getByText('批记录表单').first().waitFor({ state: 'visible', timeout: 90000 })
  await searchTargetReport(page)
  const buttons = page.locator('.batch-record-form-filler-cell')
  await buttons.first().waitFor({ state: 'visible', timeout: 90000 })
  const count = await buttons.count()
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    if (!(await button.isVisible().catch(() => false))) continue
    await button.click()
    const dialog = await getVisibleDialog(page)
    const saveButton = page.getByRole('button', { name: '保存填写设置' }).first()
    const selection = await readDialogSelection(page).catch(() => ({ candidateLabels: [] }))
    if (!(await saveButton.isDisabled().catch(() => true)) && (!requireConfigured || selection.candidateLabels.length > 0)) {
      return dialog
    }
    await closePermissionDialog(page)
  }
  throw new Error(requireConfigured
    ? '批记录表单列表没有可恢复的已配置填写人真实路线绑定行。'
    : '批记录表单列表没有可保存填写人的真实路线绑定行。')
}

async function verifyReadOnly(page, batchRecordReportId, expectedSourceLabel, expectedLabels) {
  const authHeaders = await page.evaluate(() => {
    const deserializeCachePayload = (value) => {
      if (typeof value !== 'string') return value
      try {
        return JSON.parse(value)
      } catch (error) {
        return value
      }
    }
    const readCacheValue = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) return undefined
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && 'v' in parsed) return deserializeCachePayload(parsed.v)
        return parsed
      } catch (error) {
        return raw
      }
    }
    const accessToken = readCacheValue('ACCESS_TOKEN')
    const tenantId = readCacheValue('tenantId')
    const visitTenantId = readCacheValue('visitTenantId')
    if (!accessToken) throw new Error('浏览器缺少 ACCESS_TOKEN，无法执行登录态只读核验')
    if (!tenantId) throw new Error('浏览器缺少 tenantId，无法执行租户隔离只读核验')
    const headers = {
      Authorization: `Bearer ${accessToken}`,
      'tenant-id': String(tenantId)
    }
    if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
    return headers
  })
  const data = await page.evaluate(async ({ reportId, headers }) => {
    const response = await fetch(`/admin-api/mes/pro/edhr-process-form-permission-rule/get-by-report?batchRecordReportId=${encodeURIComponent(reportId)}`, {
      method: 'GET',
      headers
    })
    const body = await response.json()
    if (![0, 200].includes(Number(body.code))) {
      throw new Error(body.msg || `接口返回 ${body.code}`)
    }
    return body.data
  }, { reportId: batchRecordReportId, headers: authHeaders })
  assert.equal(data.fillRuleStatus, 'CONFIGURED')
  const expectedSource = normalizeSourceLabel(expectedSourceLabel)
  if (expectedSource === '角色') {
    assert.equal(data.fillRule?.candidateSourceType, 'ROLE', '只读核验必须保留角色来源')
    assert.ok(data.fillRule?.candidateSourceIds?.length > 0, '只读核验必须保留角色 ID')
  } else {
    assert.ok(['USER', 'USERS'].includes(data.fillRule?.candidateSourceType), '只读核验必须保留个人来源')
  }
  const names = (data.fillRule?.candidateUsers || []).map((item) => item.displayName).filter(Boolean)
  if (expectedSource === '角色') {
    assert.ok(names.length > 0, '只读核验角色必须解析到至少一个真实候选用户')
  } else {
    for (const label of expectedLabels) {
      assert.ok(names.includes(label), `只读核验缺少候选人：${label}`)
    }
  }
  return data
}

async function main() {
  ensurePrerequisites()
  const browser = await chromium.launch({
    headless: true,
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const writes = []
  try {
    await login(page)

    let dialog = await openFirstUsableFillerDialog(page, { requireConfigured: true })
    const original = await readDialogSelection(page)
    original.sourceLabel =
      original.sourceLabel.includes('个人') || original.sourceLabel.includes('角色')
        ? normalizeSourceLabel(original.sourceLabel)
        : '个人'

    await setSource(page, dialog, '个人')
    await clearCandidates(dialog)
    const personalLabel = await selectFirstCandidate(page, dialog)
    const personalSaved = await saveDialog(page, '个人', [personalLabel])
    writes.push({ source: '个人', labels: [personalLabel], reportId: personalSaved.batchRecordReportId })
    await verifyReadOnly(page, personalSaved.batchRecordReportId, '个人', [personalLabel])

    dialog = await openFirstUsableFillerDialog(page)
    await setSource(page, dialog, '角色')
    await clearCandidates(dialog)
    const roleLabel = await selectFirstCandidate(page, dialog)
    const roleSaved = await saveDialog(page, '角色', [roleLabel])
    writes.push({ source: '角色', labels: [roleLabel], reportId: roleSaved.batchRecordReportId })
    await verifyReadOnly(page, roleSaved.batchRecordReportId, '角色', [roleLabel])

    const finalData = roleSaved
    await page.screenshot({ path: path.join(RESULT_DIR, 'batch-record-form-list-filler-pass.png'), fullPage: true })

    const result = {
      status: 'PASS',
      tenant: TENANT,
      username: USERNAME,
      batchRecordReportId: finalData.batchRecordReportId,
      original,
      writes,
      restored: false,
      finalSource: '角色',
      finalLabels: [roleLabel]
    }
    fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(`PASS: 批记录表单列表填写人真实 E2E 通过，reportId=${finalData.batchRecordReportId}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(
    path.join(RESULT_DIR, 'result.json'),
    `${JSON.stringify({ status: 'FAIL', message: error.message, stack: error.stack }, null, 2)}\n`,
    'utf8'
  )
  console.error(error)
  process.exit(1)
})
