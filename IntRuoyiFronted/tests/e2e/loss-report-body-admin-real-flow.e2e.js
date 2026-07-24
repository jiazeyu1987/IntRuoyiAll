const assert = require('assert')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const BASE_URL = 'http://localhost:8081'
const BACKEND_URL = 'http://127.0.0.1:48081'
const TENANT = '芋道源码'
const USERNAME = 'admin'
const PASSWORD = process.env.LOSS_REPORT_ADMIN_PASSWORD || 'admin123'
const SAMPLE_DOC_PATH =
  process.env.LOSS_REPORT_SAMPLE_DOC ||
  'C:\\Users\\BJB110\\Desktop\\文档\\INT∕RE∕8.3-09（E 1）生产过程损耗报告单--2025.09.30生效(1).doc'
const USE_EXISTING_LOSS_REPORT = process.env.LOSS_REPORT_USE_EXISTING === '1'
const DEFAULT_EXISTING_BATCH_RECORD_NAME = 'ADMIN-LOSS-BODY-1783581971625'
let batchRecordName =
  process.env.LOSS_REPORT_BATCH_RECORD_NAME ||
  (USE_EXISTING_LOSS_REPORT ? DEFAULT_EXISTING_BATCH_RECORD_NAME : `ADMIN-LOSS-BODY-${Date.now()}`)
const SKIP_MAIN_IMPORT = process.env.LOSS_REPORT_SKIP_MAIN_IMPORT === '1'
const OUTPUT_DIR = path.resolve(__dirname, '..', 'output', 'loss-report-body-admin')
const MAIN_SAMPLE_DOC = process.env.LOSS_REPORT_MAIN_SAMPLE_DOC || path.resolve(__dirname, '..', '..', 'resource', '批记录模板.doc')

let runtimeAuth = {}

function assertBusinessSuccess(payload, label) {
  assert.ok(payload && typeof payload === 'object', `${label} 必须返回 JSON 对象`)
  assert.ok([0, 200].includes(payload.code), `${label} 业务响应失败：${JSON.stringify(payload)}`)
  return payload.data
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && (await item.isEnabled().catch(() => false))) {
      await item.click()
      return
    }
  }
  throw new Error(`未找到可点击控件：${label}`)
}

async function login(page) {
  const loginUrl = `${BASE_URL}/login?redirect=${encodeURIComponent('/mes/pro/batch-record-form-list')}`
  for (let attempt = 1; attempt <= 2; attempt += 1) {
    await page.goto(loginUrl, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const loginForm = page.locator('form.login-form:visible').first()
    await loginForm.waitFor({ state: 'visible', timeout: 60000 })
    const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
    if (await tenantInput.count()) {
      await tenantInput.fill(TENANT)
      await page.waitForTimeout(300)
      const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
      if (await tenantOption.count()) {
        await tenantOption.click()
      } else {
        await tenantInput.press('Enter')
      }
    } else {
      await loginForm.locator('input.el-input__inner').nth(0).fill(TENANT)
    }
    await loginForm.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
    await loginForm.locator('input[type="password"]').first().fill(PASSWORD)
    await page.getByRole('button', { name: '登录' }).click()
    try {
      await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
    } catch (error) {
      if (attempt === 2) {
        await page.screenshot({ path: path.join(OUTPUT_DIR, 'loss-report-login-timeout.png'), fullPage: true }).catch(() => {})
        throw error
      }
      await page.waitForTimeout(1000)
      continue
    }
    const auth = await browserAuth(page)
    assert.ok(auth.token, '芋道源码 admin 登录后必须写入浏览器 token')
    runtimeAuth = {
      token: auth.token,
      tenantId: 1
    }
    await page.goto(`${BASE_URL}/mes/pro/batch-record-form-list`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    return
  }
  throw new Error('登录重试耗尽')
}

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const unwrapStorageValue = (rawValue) => {
    let value = rawValue
    for (let depth = 0; depth < 5; depth += 1) {
      if (typeof value !== 'string') {
        break
      }
      try {
        value = JSON.parse(value)
      } catch (_) {
        break
      }
      if (value && typeof value === 'object' && Object.prototype.hasOwnProperty.call(value, 'v')) {
        value = value.v
      }
    }
    return value
  }
  let token
  let tenantId
  let visitTenantId
  for (const [key, rawValue] of Object.entries(snapshot)) {
    if (!rawValue) continue
    if (!token && /token/i.test(key)) {
      const parsed = unwrapStorageValue(rawValue)
      token = typeof parsed === 'string'
        ? parsed
        : parsed?.accessToken || parsed?.access_token || parsed?.value || parsed?.token
    }
    if (!tenantId && /tenant/i.test(key)) {
      const parsed = unwrapStorageValue(rawValue)
      tenantId = typeof parsed === 'object'
        ? parsed?.id || parsed?.tenantId || parsed?.value
        : parsed
      visitTenantId = typeof parsed === 'object'
        ? parsed?.visitTenantId || parsed?.visit_tenant_id
        : visitTenantId
    }
  }
  if (token && token.startsWith('"')) token = JSON.parse(token)
  return {
    token: token || runtimeAuth.token,
    tenantId: tenantId || runtimeAuth.tenantId,
    visitTenantId
  }
}

async function authenticatedRequest(page, method, endpoint, options, label) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, `${label} 需要浏览器登录 token`)
  assert.equal(String(tenantId), '1', `${label} 必须在芋道源码 tenant-id=1 下核验，实际 tenant-id=${tenantId}`)
  const response = await page.request[method](`${BACKEND_URL}${endpoint}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {}),
      ...(options?.headers || {})
    },
    timeout: options?.timeout || 60000
  })
  assert.equal(response.status(), 200, `${label} HTTP 必须为 200`)
  return assertBusinessSuccess(await response.json(), label)
}

async function authenticatedGet(page, endpoint, params, label) {
  return authenticatedRequest(page, 'get', endpoint, { params }, label)
}

async function authenticatedPost(page, endpoint, options, label) {
  return authenticatedRequest(page, 'post', endpoint, options, label)
}

async function findLatestLossReport(page) {
  const explicitBatchRecordName = process.env.LOSS_REPORT_BATCH_RECORD_NAME
  const queryParams = explicitBatchRecordName
    ? { pageNo: 1, pageSize: 20, batchRecordName, formSlotType: 'LOSS_REPORT' }
    : { pageNo: 1, pageSize: 200, formSlotType: 'LOSS_REPORT' }
  const pageData = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/page',
    queryParams,
    '查询已有损耗单'
  )
  const candidates = (pageData?.list || []).filter((item) => item.formSlotType === 'LOSS_REPORT' && item.reportId)
  if (explicitBatchRecordName) {
    const report = candidates.find((item) => item.batchRecordName === batchRecordName)
    assert.ok(
      report?.reportId,
      `必须存在指定批记录的可复用 LOSS_REPORT，batchRecordName=${batchRecordName}，返回：${JSON.stringify(pageData)}`
    )
    return report
  }
  const failures = []
  for (const candidate of candidates) {
    try {
      await verifyLossReportBodyRules(page, candidate.reportId)
      batchRecordName = candidate.batchRecordName
      return candidate
    } catch (error) {
      failures.push(`${candidate.batchRecordName}/${candidate.reportId}: ${error.message}`)
    }
  }
  assert.fail(`必须存在至少一份通过损耗单 Profile 结构断言的 LOSS_REPORT，候选失败：${failures.join(' | ')}`)
}

async function importMainBatchRecord(page) {
  assert.ok(fs.existsSync(MAIN_SAMPLE_DOC), `缺少主批记录样本：${MAIN_SAMPLE_DOC}`)
  const data = await authenticatedPost(
    page,
    '/admin-api/mes/pro/batch-record-report/recognize-uploaded',
    {
      multipart: {
        file: fs.createReadStream(MAIN_SAMPLE_DOC),
        routeKey: 'B',
        batchRecordName,
        upgrade: 'false',
        productNames: '球囊扩张压力泵'
      },
      timeout: 600000
    },
    'admin 临时主批记录导入'
  )
  assert.ok(data?.reports?.length > 0, '临时主批记录导入必须生成报表')
  return data
}

async function deleteExistingLossReportIfAny(page) {
  const pageData = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/page',
    { pageNo: 1, pageSize: 10, batchRecordName, formSlotType: 'LOSS_REPORT' },
    '导入前损耗单查询'
  )
  const existing = (pageData?.list || [])[0]
  if (!existing) return
  await authenticatedRequest(
    page,
    'delete',
    '/admin-api/mes/pro/batch-record-report/delete-extra-slot',
    { params: { batchRecordName, formSlotType: 'LOSS_REPORT' } },
    '删除旧损耗单'
  )
}

async function uploadLossReportByUi(page) {
  await page.goto(`${BASE_URL}/mes/pro/batch-record-form-list`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批记录名称').first().waitFor({ state: 'visible', timeout: 60000 })

  const selectedGroup = await selectBatchRecordGroup(page)
  const lossSlotRow = selectedGroup.locator('.batch-record-extra-slot-row').filter({
    hasText: '损耗单'
  }).first()
  await lossSlotRow.waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(lossSlotRow.locator('.batch-record-extra-slot__action'), '损耗单上传图标按钮')
  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/upload-extra-slot') &&
      response.request().method() === 'POST',
    { timeout: 600000 }
  )
  await page.locator('input.batch-record-word-import-input').setInputFiles(SAMPLE_DOC_PATH)
  const uploadResponse = await uploadResponsePromise
  assert.equal(uploadResponse.status(), 200, '损耗单上传接口 HTTP 必须为 200')
  const uploadData = assertBusinessSuccess(await uploadResponse.json(), 'admin 损耗单上传')
  const report = (uploadData?.reports || []).find((item) => item.formSlotType === 'LOSS_REPORT')
  assert.ok(report?.reportId, `上传响应必须返回 LOSS_REPORT reportId：${JSON.stringify(uploadData)}`)

  await page.getByText('请填写', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return report
}

async function selectBatchRecordGroup(page) {
  const searchInput = page.locator('.batch-record-toolbar-search input').first()
  if (await searchInput.count()) {
    await searchInput.fill(batchRecordName)
    await searchInput.press('Enter')
    await page.waitForTimeout(1000)
  }
  const selectedGroup = page.locator('.batch-record-record-list__group').filter({
    has: page.locator('.batch-record-record-list__name', { hasText: batchRecordName })
  }).first()
  await selectedGroup.waitFor({ state: 'visible', timeout: 60000 })
  await selectedGroup.locator('.batch-record-record-list__item').click()
  return selectedGroup
}

async function openExistingLossReportByUi(page, reportId) {
  await page.goto(`${BASE_URL}/mes/pro/batch-record-form-list`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批记录名称').first().waitFor({ state: 'visible', timeout: 60000 })
  const selectedGroup = await selectBatchRecordGroup(page)
  const lossSlotRow = selectedGroup.locator('.batch-record-extra-slot-row').filter({
    hasText: '损耗单'
  }).first()
  await lossSlotRow.waitFor({ state: 'visible', timeout: 60000 })
  const rulesResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-report/cell-rules') &&
      (!reportId || response.url().includes(`reportId=${encodeURIComponent(reportId)}`)) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await lossSlotRow.locator('.batch-record-extra-slot__name').click()
  const rulesResponse = await rulesResponsePromise
  assert.equal(rulesResponse.status(), 200, '打开既有损耗单时单元格规则接口 HTTP 必须为 200')
  const cellRules = assertBusinessSuccess(await rulesResponse.json(), '打开既有损耗单规则查询')
  assert.ok(
    (cellRules?.suggestions || []).some((rule) => rule.label === '不合格数量'),
    `打开既有损耗单必须返回损耗单正文规则，reportId=${reportId}`
  )
  return verifyLossReportBodyPreview(page)
}

async function verifyLossReportBodyRules(page, reportId) {
  const cellRules = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/cell-rules',
    { reportId },
    '损耗单单元格规则查询'
  )
  const suggestions = cellRules?.suggestions || []
  assert.equal(suggestions.length, 47, `损耗单必须生成 47 个横向明细填写规则，实际 ${suggestions.length}`)
  const labels = suggestions.map((rule) => rule.label)
  for (const label of ['产品名称', '型号规格', '批号', '生产数量', '不合格日期', '工序名称', '不合格数量', '不合格原因', '生产人员/日期']) {
    assert.ok(labels.includes(label), `损耗单规则缺少标签：${label}`)
  }
  assert.ok(labels.some((label) => label?.startsWith('检验人员')), '损耗单规则缺少检验人员/确认日期标签')
  assert.ok(labels.some((label) => label?.startsWith('批准人/日期')), '损耗单规则缺少批准人/日期标签')
  assert.equal(labels.filter((label) => label?.includes('□其他')).length, 8, '其他横线必须识别为 8 个独立填写格')
  assert.ok(
    suggestions.filter((rule) => rule.label?.includes('□其他')).every((rule) => rule.valueType === 'STRING'),
    '其他横线必须按文本填写规则识别'
  )
  const layout = JSON.parse(cellRules.sheetLayoutJson)
  const spanOf = (cell) => {
    const merge = Array.isArray(cell?.merge) ? cell.merge : []
    const rowDelta = Number(merge[0])
    const columnDelta = Number(merge[1])
    return {
      rowSpan: Number.isInteger(rowDelta) && rowDelta >= 0 ? rowDelta + 1 : 1,
      colSpan: Number.isInteger(columnDelta) && columnDelta >= 0 ? columnDelta + 1 : 1
    }
  }
  const numericRows = Object.keys(layout.rows || {})
    .filter((rowKey) => /^\d+$/.test(rowKey))
    .map((rowKey) => Number(rowKey))
    .sort((left, right) => left - right)
    .map((rowIndex) => String(rowIndex))
  const cellText = (rowKey, cellKey) => layout.rows?.[String(rowKey)]?.cells?.[String(cellKey)]?.text || ''
  const descriptionRow = numericRows.find((rowKey) => cellText(rowKey, 0).includes('损耗描述'))
  assert.ok(descriptionRow, '损耗单必须包含损耗描述横向标题行')
  const descriptionRowIndex = Number(descriptionRow)
  const headerRowIndex = descriptionRowIndex + 1
  const firstDetailRowIndex = headerRowIndex + 1
  const approvalRowIndex = firstDetailRowIndex + 8
  for (let rowIndex = descriptionRowIndex; rowIndex <= approvalRowIndex; rowIndex += 1) {
    assert.ok(layout.rows?.[String(rowIndex)], `损耗单横向正文缺少第 ${rowIndex} 行`)
  }
  const tailRows = numericRows.filter((rowKey) => Number(rowKey) > approvalRowIndex)
  assert.ok(tailRows.length <= 2, `损耗单最多只能保留 2 行非语义页脚，实际 ${tailRows.length}`)
  const tailTexts = tailRows.map((rowKey) => layout.rows?.[rowKey]?.cells?.['0']?.text || '')
  assert.ok(
    tailTexts.every((text) => text.startsWith('生效日期') || text.startsWith('打印日期')),
    `损耗单额外尾行只能是生效日期/打印日期，实际 ${tailTexts.join(' / ')}`
  )
  assert.equal(cellText(descriptionRowIndex, 0), '损耗描述：', '损耗描述行必须是横向标题行')
  assert.equal(spanOf(layout.rows?.[String(descriptionRowIndex)]?.cells?.['0']).colSpan, 8, '损耗描述必须横向合并整表 8 列')
  assert.equal(cellText(headerRowIndex, 0), '不合格日期', '横向正文必须保留不合格日期表头')
  assert.equal(cellText(headerRowIndex, 4), '处置方式', '横向表头必须保留处置方式列')
  assert.equal(spanOf(layout.rows?.[String(headerRowIndex)]?.cells?.['4']).colSpan, 2, '处置方式表头必须横跨两列')
  assert.equal(cellText(headerRowIndex, 6), '生产人员/日期', '横向表头必须保留生产人员/日期列')
  assert.ok(cellText(headerRowIndex, 7).includes('检验人员'), '横向表头必须保留检验人员确认日期列')
  assert.equal(cellText(firstDetailRowIndex, 4), '□报废   □其他：', '首行处置方式标签必须保留复选项文本')
  assert.ok(!layout.rows?.[String(firstDetailRowIndex)]?.cells?.['4']?.fillForm, '处置方式标签格不应被识别为填写格')
  assert.equal(spanOf(layout.rows?.[String(firstDetailRowIndex)]?.cells?.['4']).colSpan, 1, '处置方式标签格必须只占一列')
  assert.ok(layout.rows?.[String(firstDetailRowIndex)]?.cells?.['5']?.fillForm, '其他冒号后的长横线必须识别为填写格')
  assert.equal(spanOf(layout.rows?.[String(firstDetailRowIndex)]?.cells?.['6']).rowSpan, 8, '生产人员/日期单元格必须纵向合并 8 行')
  assert.equal(spanOf(layout.rows?.[String(firstDetailRowIndex)]?.cells?.['7']).rowSpan, 8, '检验人员确认日期单元格必须纵向合并 8 行')
  assert.ok(!layout.rows?.[String(firstDetailRowIndex + 1)]?.cells?.['6'], '生产人员/日期纵向合并覆盖行不应重复渲染单元格')
  assert.ok(!layout.rows?.[String(firstDetailRowIndex + 1)]?.cells?.['7'], '检验人员确认日期纵向合并覆盖行不应重复渲染单元格')
  assert.ok(cellText(approvalRowIndex, 0).startsWith('批准人/日期'), '横向正文后必须保留批准人/日期行')
  const fillFormCells = []
  for (const [rowKey, row] of Object.entries(layout.rows || {})) {
    if (!/^\d+$/.test(rowKey)) continue
    for (const [cellKey, cell] of Object.entries(row.cells || {})) {
      if (!/^\d+$/.test(cellKey)) continue
      if (cell.fillForm) fillFormCells.push(`${rowKey}:${cellKey}`)
    }
  }
  assert.equal(fillFormCells.length, 47, `布局中必须有 47 个 fillForm，实际 ${fillFormCells.length}`)
  assert.ok(fillFormCells.includes(`${firstDetailRowIndex}:5`), '首行其他横线必须是填写格')
  assert.ok(!fillFormCells.includes(`${firstDetailRowIndex}:4`), '首行处置方式标签不能是填写格')
  return { suggestions, fillFormCells }
}

async function verifyLossReportBodyPreview(page) {
  const previewPanel = page.locator('.batch-record-template-preview').first()
  await previewPanel.waitFor({ state: 'visible', timeout: 60000 })
  const requiredTexts = [
    '不合格日期',
    '工序名称',
    '不合格数量',
    '不合格原因',
    '处置方式',
    '生产人员/日期',
    '检验人员',
    '批准人/日期'
  ]
  let bodyText = ''
  const deadline = Date.now() + 60000
  while (Date.now() < deadline) {
    bodyText = await page.locator('body').innerText({ timeout: 60000 })
    if (requiredTexts.every((text) => bodyText.includes(text))) {
      break
    }
    await page.waitForTimeout(1000)
  }
  if (!requiredTexts.every((text) => bodyText.includes(text))) {
    const debugDir = path.join(OUTPUT_DIR, 'debug')
    fs.mkdirSync(debugDir, { recursive: true })
    await page.screenshot({ path: path.join(debugDir, 'loss-report-preview-timeout.png'), fullPage: true })
    fs.writeFileSync(path.join(debugDir, 'loss-report-preview-timeout.txt'), bodyText, 'utf8')
  }
  for (const text of requiredTexts) {
    assert.ok(bodyText.includes(text), `页面预览必须显示 ${text}`)
  }
  assert.equal((bodyText.match(/□报废/g) || []).length, 8, '页面预览必须显示 8 行报废')
  assert.equal((bodyText.match(/□其他：/g) || []).length, 8, '页面预览必须显示 8 行其他')
  assert.ok(!bodyText.includes('损耗描述：\n不合格日期\n工序名称'), '页面预览不能再显示旧的合并正文文本块')
  const placeholderCount = (bodyText.match(/请填写/g) || []).length
  const pendingSignatureCount = (bodyText.match(/未签名/g) || []).length
  assert.ok(placeholderCount >= 46, `页面预览至少应显示 46 个普通填写占位，实际 ${placeholderCount}`)
  assert.ok(pendingSignatureCount >= 1, `页面预览必须显示批准人/日期签名待签状态，实际 ${pendingSignatureCount}`)
  return { placeholderCount, pendingSignatureCount }
}

async function main() {
  assert.ok(fs.existsSync(SAMPLE_DOC_PATH), `缺少真实损耗单 Word：${SAMPLE_DOC_PATH}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const launchOptions = {
    headless: process.env.LOSS_REPORT_HEADED === '1' ? false : true,
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1600, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  try {
    await login(page)
    let report
    if (USE_EXISTING_LOSS_REPORT) {
      report = await findLatestLossReport(page)
      batchRecordName = report.batchRecordName
    } else {
      if (!SKIP_MAIN_IMPORT) {
        await importMainBatchRecord(page)
      }
      await deleteExistingLossReportIfAny(page)
      report = await uploadLossReportByUi(page)
    }
    const verification = await verifyLossReportBodyRules(page, report.reportId)
    const previewVerification = await openExistingLossReportByUi(page, report.reportId)
    const screenshotFile = path.join(OUTPUT_DIR, 'loss-report-body-admin-preview.png')
    await page.screenshot({ path: screenshotFile, fullPage: true })
    const evidence = {
      tenant: TENANT,
      username: USERNAME,
      tenantId: 1,
      batchRecordName,
      lossReportId: report.reportId,
      reportName: report.reportName,
      formSlotType: report.formSlotType,
      suggestionCount: verification.suggestions.length,
      fillFormCellCount: verification.fillFormCells.length,
      previewPlaceholderCount: previewVerification.placeholderCount,
      previewPendingSignatureCount: previewVerification.pendingSignatureCount,
      labels: verification.suggestions.map((rule) => rule.label),
      screenshotFile
    }
    fs.writeFileSync(path.join(OUTPUT_DIR, 'loss-report-body-admin-evidence.json'), JSON.stringify(evidence, null, 2), 'utf8')
    console.log(
      `PASS: admin loss body upload batchRecordName=${batchRecordName} reportId=${report.reportId} suggestions=${verification.suggestions.length} fillForms=${verification.fillFormCells.length} screenshot=${screenshotFile}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
