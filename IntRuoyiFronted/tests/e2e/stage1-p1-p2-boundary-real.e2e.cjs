const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.STAGE1_P1_P2_BOUNDARY_BASE_URL || 'http://127.0.0.1:8081').replace(
  /\/+$/,
  ''
)
const TENANT = process.env.STAGE1_P1_P2_BOUNDARY_TENANT || '芋道源码'
const USERNAME = process.env.STAGE1_P1_P2_BOUNDARY_USERNAME || 'admin'
const PASSWORD = process.env.STAGE1_P1_P2_BOUNDARY_PASSWORD || 'admin123'
const WORK_ORDER_CODE =
  process.env.STAGE1_P1_P2_BOUNDARY_WORK_ORDER_CODE ||
  'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
const CHROME_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

const RUN_ID = `STAGE1-P1-P2-BOUNDARY-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`
const RESULT_DIR = path.resolve(
  __dirname,
  '../../../output/playwright/stage1-p1-p2-boundary-real',
  RUN_ID
)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')

const evidence = {
  runId: RUN_ID,
  status: 'RUNNING',
  baseUrl: BASE_URL,
  tenant: TENANT,
  username: USERNAME,
  workOrderCode: WORK_ORDER_CODE,
  urls: {},
  beforeP1: {},
  afterP1: {},
  afterP2: {},
  screenshots: {},
  consoleErrors: [],
  pageErrors: [],
  failedRequests: []
}

const persist = () => {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

const screenshot = async (page, name) => {
  const file = path.join(RESULT_DIR, `${name}.png`)
  await page.screenshot({ path: file, fullPage: true })
  evidence.screenshots[name] = file
  persist()
}

const firstVisible = async (locator, label) => {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`未找到可见控件：${label}`)
}

const clickTab = async (scope, name) => {
  const tab = await firstVisible(scope.getByRole('tab', { name }), `页签 ${name}`)
  await tab.click()
  await scope.page().waitForTimeout(200)
}

const clickMenu = async (page, text) => {
  const textNode = await firstVisible(page.locator('.el-menu').getByText(text, { exact: true }), text)
  const target = textNode.locator(
    'xpath=ancestor-or-self::*[contains(@class, "el-menu-item") or contains(@class, "el-sub-menu__title")][1]'
  )
  const targetClass = (await target.getAttribute('class')) || ''
  if (targetClass.includes('el-sub-menu__title')) {
    const submenu = target.locator('xpath=ancestor::li[contains(@class, "el-sub-menu")][1]')
    const submenuClass = (await submenu.getAttribute('class')) || ''
    if (!submenuClass.includes('is-opened')) await target.click()
    return
  }
  await target.click()
}

const normalizeText = (text) => String(text || '').replace(/\s+/g, ' ').trim()

const escapeRegExp = (value) => String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const DOSSIER_UPLOADS = [
  {
    label: '来料检文件',
    category: 'INCOMING_INSPECTION_FILE',
    fileName: `${RUN_ID}-incoming-inspection.txt`,
    content: `runId=${RUN_ID}\ncategory=INCOMING_INSPECTION_FILE\n正式来料检资料\n`
  },
  {
    label: '灭菌文件',
    category: 'STERILIZATION_FILE',
    fileName: `${RUN_ID}-sterilization.txt`,
    content: `runId=${RUN_ID}\ncategory=STERILIZATION_FILE\n正式灭菌资料\n`
  },
  {
    label: '成品检文件',
    category: 'FINISHED_PRODUCT_FILE',
    fileName: `${RUN_ID}-finished-product.txt`,
    content: `runId=${RUN_ID}\ncategory=FINISHED_PRODUCT_FILE\n正式成品检资料\n`
  }
]

const login = async (page) => {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const usernameLocator = page.locator(
    'input[placeholder="请输入用户名"], input[placeholder="请输入账号"], input[placeholder*="账号"]'
  )
  await usernameLocator.first().waitFor({ state: 'visible', timeout: 90000 })
  const form = page
    .locator('.login-form:visible, form:visible')
    .filter({ has: usernameLocator.first() })
    .first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const loginText = await page.locator('body').innerText()
  if (!loginText.includes(TENANT)) {
    const tenantInput = await firstVisible(form.locator('input.el-select__input, .el-select input'), '租户')
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    await firstVisible(
      page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }),
      '租户选项'
    ).then((item) => item.click())
  }
  await firstVisible(
    form.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'),
    '用户名'
  ).then((item) => item.fill(USERNAME))
  await firstVisible(form.locator('input[placeholder="请输入密码"]'), '密码').then((item) =>
    item.fill(PASSWORD)
  )
  await firstVisible(form.getByRole('button', { name: /^登录$/ }), '登录按钮').then((item) =>
    item.click()
  )
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 90000 })
}

const openActiveOrderPool = async (page) => {
  if (!page.url().includes('/mes/pro/process-pool/production-leader')) {
    await clickMenu(page, 'MES 系统')
    await clickMenu(page, 'eDHR批记录')
    await clickMenu(page, '生产组长')
    await page.waitForURL((url) => url.pathname === '/mes/pro/process-pool/production-leader', {
      timeout: 60000
    })
  }
  await firstVisible(page.getByRole('tab', { name: '活跃订单池' }), '活跃订单池').then((tab) =>
    tab.click()
  )
  await page.locator('[data-team-leader-active-order-list]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  const filter = page.locator('[data-team-leader-active-order-work-order-filter]').first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  await filter.fill(WORK_ORDER_CODE)
}

const targetRow = (page) =>
  page
    .locator('[data-team-leader-active-order-list] .el-table__body-wrapper tbody tr')
    .filter({ hasText: WORK_ORDER_CODE })
    .first()

const resetFixedOrder = async (page) => {
  await openActiveOrderPool(page)
  const resetButton = page.locator('[data-team-leader-reset-fixed-active-order]').first()
  await resetButton.waitFor({ state: 'visible', timeout: 30000 })
  const resetResponse = page.waitForResponse((response) =>
    response.url().includes('/active-order/simulation/test-reset') && response.request().method() === 'POST',
    { timeout: 120000 }
  )
  await resetButton.click()
  const response = await resetResponse
  assert.equal(response.ok(), true, '重置请求必须成功')
  const body = await response.json()
  if (body.code !== 0) {
    evidence.resetResponse = body
    persist()
  }
  assert.equal(body.code, 0, body.msg)
  assert.ok(body.data.activeOrderId, '重置必须生成活跃订单')
  evidence.beforeP1.activeOrderId = body.data.activeOrderId
  await page
    .locator('.el-message--success', { hasText: '测试订单已重置并重新加入' })
    .first()
    .waitFor({ state: 'visible', timeout: 120000 })
  await openActiveOrderPool(page)
  const row = targetRow(page)
  await row.waitFor({ state: 'visible', timeout: 60000 })
  return row
}

const openDetailFromRow = async (page) => {
  const row = targetRow(page)
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.locator('[data-team-leader-active-order-detail]').first().click()
  await page.waitForURL((url) => url.pathname.includes('/submission-detail'), { timeout: 60000 })
  const detail = page.locator('[data-team-leader-active-order-detail-page]', {
    hasText: WORK_ORDER_CODE
  }).first()
  await detail.waitFor({ state: 'visible', timeout: 60000 })
  evidence.urls.activeOrderDetail = page.url()
  persist()
  return detail
}

const readSummaryMaterialBatchCodes = async (detail) => {
  await clickTab(detail, '总表')
  const table = detail.locator('[data-active-order-summary-material-batches-table]').first()
  await table.waitFor({ state: 'visible', timeout: 30000 })
  const rowCount = await table.locator('tbody tr').count()
  const batchCodes = []
  for (let index = 2; index < rowCount; index += 1) {
    const cells = (await table.locator('tbody tr').nth(index).locator('td').allInnerTexts()).map((text) =>
      text.trim()
    )
    if (cells.length >= 6) {
      for (const code of [cells[2], cells[5]]) {
        if (code && code !== '-') batchCodes.push(code)
      }
    }
  }
  return batchCodes
}

const assertSummaryMaterialBatchesEmpty = async (detail, phase) => {
  const codes = await readSummaryMaterialBatchCodes(detail)
  evidence[phase].summaryMaterialBatchCodes = codes
  assert.deepEqual(codes, [], `${phase} 总表物料批号必须为空`)
}

const assertSummaryMaterialBatchesPresent = async (detail, phase) => {
  const codes = await readSummaryMaterialBatchCodes(detail)
  evidence[phase].summaryMaterialBatchCodes = codes
  assert.ok(codes.length > 0, `${phase} 总表必须显示 P2 回填后的物料批号`)
}

const assertMaterialTabHasNoBatchLots = async (detail, phase) => {
  await clickTab(detail, '领料单')
  const bodyText = await detail.innerText()
  if (bodyText.includes('暂无领料单物料批号')) {
    evidence[phase].materialTabBatchTexts = []
    return
  }
  const rows = detail.locator('.el-table__body-wrapper tbody tr:visible')
  const batchTexts = []
  for (let index = 0; index < (await rows.count()); index += 1) {
    const cells = (await rows.nth(index).locator('td').allInnerTexts()).map((text) => text.trim())
    if (cells.length >= 5) {
      const batch = cells[4]
      if (batch && batch !== '-') batchTexts.push(batch)
    }
  }
  evidence[phase].materialTabBatchTexts = batchTexts
  assert.deepEqual(batchTexts, [], `${phase} 领料单页签不得显示 P2 前物料批号`)
}

const assertNoDetailP2Entrypoints = async (detail, phase) => {
  const productionEntryCount = await detail.locator('[data-active-order-open-batch-execution-production-form]').count()
  const pqcEntryCount = await detail.locator('[data-active-order-open-batch-execution-pqc-form]').count()
  evidence[phase].detailP2Entrypoints = { productionEntryCount, pqcEntryCount }
  assert.equal(productionEntryCount, 0, `${phase} 详情不得出现打开批次执行生产表单入口`)
  assert.equal(pqcEntryCount, 0, `${phase} 详情不得出现打开批次执行过程检验记录入口`)
}

const assertDossierTabsReadable = async (detail, phase) => {
  const definitions = [
    ['来料检文件', 'INCOMING_INSPECTION_FILE'],
    ['灭菌文件', 'STERILIZATION_FILE'],
    ['成品检文件', 'FINISHED_PRODUCT_FILE'],
    ['其他文件', 'OTHER_FILE']
  ]
  const tabs = []
  for (const [label, category] of definitions) {
    await clickTab(detail, label)
    const panel = detail.locator(`[data-active-order-dossier-file-category="${category}"]`).first()
    await panel.waitFor({ state: 'visible', timeout: 60000 })
    const text = await panel.innerText()
    assert.ok(!text.includes('尚未执行P2生成正式批次'), `${phase} ${label} 不得提示缺少P2正式批次`)
    assert.match(text, /资料归属：当前活跃订单/, `${phase} ${label} 必须声明当前活跃订单资料归属`)
    assert.ok(!text.includes('来源批次：'), `${phase} ${label} 不得暴露P2批次来源字段`)
    tabs.push({ label, category, text: text.replace(/\s+/g, ' ').slice(0, 600) })
  }
  evidence[phase].dossierTabs = tabs
}

const dossierPanel = async (detail, upload) => {
  await clickTab(detail, upload.label)
  const panel = detail.locator(`[data-active-order-dossier-file-category="${upload.category}"]`).first()
  await panel.waitFor({ state: 'visible', timeout: 60000 })
  return panel
}

const uploadDossierFilesViaDetail = async (detail, phase) => {
  const uploaded = []
  for (const upload of DOSSIER_UPLOADS) {
    const panel = await dossierPanel(detail, upload)
    const responsePromise = detail.page().waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/process-pool/team-leader/active-order/dossier-files/upload') &&
        response.request().method() === 'POST',
      { timeout: 120000 }
    )
    const fileInput = panel.locator('[data-active-order-dossier-file-upload] input[type="file"]').first()
    await fileInput.setInputFiles({
      name: upload.fileName,
      mimeType: 'text/plain',
      buffer: Buffer.from(upload.content, 'utf8')
    })
    const response = await responsePromise
    const result = await response.json()
    assert.equal(response.ok(), true, `${phase} ${upload.label} 上传HTTP请求失败`)
    assert.equal(result.code, 0, `${phase} ${upload.label} 上传失败：${result.msg}`)
    await panel
      .locator('[data-active-order-dossier-file-preview]')
      .filter({ hasText: upload.fileName })
      .first()
      .waitFor({ state: 'visible', timeout: 60000 })
    const text = normalizeText(await panel.innerText())
    assert.match(text, new RegExp(escapeRegExp(upload.fileName)), `${phase} ${upload.label} 必须显示上传文件名`)
    assert.match(text, /上传人|上传时间/, `${phase} ${upload.label} 必须显示上传元信息`)
    uploaded.push({
      label: upload.label,
      category: upload.category,
      fileName: upload.fileName,
      response: { status: response.status(), code: result.code },
      text: text.slice(0, 800)
    })
  }
  evidence[phase].uploadedDossierFiles = uploaded
  persist()
  return uploaded
}

const assertUploadedDossierFilesVisible = async (detail, phase, expectedUploads = DOSSIER_UPLOADS) => {
  const visibleFiles = []
  for (const upload of expectedUploads) {
    const panel = await dossierPanel(detail, upload)
    const text = normalizeText(await panel.innerText())
    assert.match(text, new RegExp(escapeRegExp(upload.fileName)), `${phase} ${upload.label} 必须保留上传文件`)
    assert.match(text, /资料归属：当前活跃订单/, `${phase} ${upload.label} 必须继续声明活跃订单资料归属`)
    assert.match(text, /上传人|上传时间/, `${phase} ${upload.label} 必须保留上传人和上传时间`)
    assert.ok(!text.includes('暂无文件'), `${phase} ${upload.label} 不得显示暂无文件`)
    visibleFiles.push({
      label: upload.label,
      category: upload.category,
      fileName: upload.fileName,
      text: text.slice(0, 800)
    })
  }
  evidence[phase].visibleDossierFiles = visibleFiles
  persist()
  return visibleFiles
}

const collectProductionFacts = async (scope, phase) => {
  await clickTab(scope, '生产表单')
  const processTabs = scope.locator('[data-team-leader-active-order-detail-production-process-tab]')
  const processCount = await processTabs.count()
  assert.ok(processCount > 0, `${phase} 必须显示生产工序`)
  let mainRows = 0
  let submitterSignatures = 0
  let reviewerSignatures = 0
  for (let index = 0; index < processCount; index += 1) {
    await processTabs.nth(index).click()
    await scope.page().waitForTimeout(150)
    mainRows += await scope.locator('[data-active-order-production-record-main-row]:visible').count()
    submitterSignatures += await scope
      .locator('[data-active-order-production-record-submitter-signature]:visible')
      .count()
    reviewerSignatures += await scope
      .locator('[data-active-order-production-record-reviewer-signature]:visible')
      .count()
  }
  assert.ok(mainRows > 0, `${phase} 必须显示生产提交记录`)
  assert.ok(submitterSignatures > 0, `${phase} 必须显示生产提交签名`)
  assert.ok(reviewerSignatures > 0, `${phase} 必须显示生产复核签名`)
  return { processCount, mainRows, submitterSignatures, reviewerSignatures }
}

const collectPqcOriginalFacts = async (scope, phase) => {
  await clickTab(scope, '过程检表单')
  await clickTab(scope, '原始提交')
  const processTabs = scope.locator('[data-team-leader-active-order-detail-pqc-process-tab]')
  const processCount = await processTabs.count()
  assert.ok(processCount > 0, `${phase} 必须显示 PQC 检验工序`)
  let submissionBlocks = 0
  let detailTables = 0
  let detailRows = 0
  const inspectionTypes = new Set()
  for (let index = 0; index < processCount; index += 1) {
    await processTabs.nth(index).click()
    await scope.page().waitForTimeout(150)
    const blocks = scope.locator('[data-active-order-pqc-original-submission-block]:visible')
    const blockCount = await blocks.count()
    assert.ok(blockCount > 0, `${phase} 每个 PQC 工序必须显示一线提交`)
    submissionBlocks += blockCount
    detailTables += await scope.locator('[data-active-order-pqc-original-submission-table]:visible').count()
    for (let blockIndex = 0; blockIndex < blockCount; blockIndex += 1) {
      const text = await blocks.nth(blockIndex).innerText()
      assert.match(text, /提交人/, `${phase} PQC 原始提交必须显示提交人`)
      assert.match(text, /复核人/, `${phase} PQC 原始提交必须显示复核人`)
      assert.ok(!text.includes('未复核'), `${phase} PQC 原始提交必须已经复核`)
      for (const match of text.matchAll(/首检|上午巡检|下午巡检|末检/g)) {
        inspectionTypes.add(match[0])
      }
    }
    const visibleTables = scope.locator('[data-active-order-pqc-original-submission-table]:visible')
    for (let tableIndex = 0; tableIndex < (await visibleTables.count()); tableIndex += 1) {
      detailRows += await visibleTables.nth(tableIndex).locator('tbody tr').count()
    }
  }
  assert.ok(detailTables > 0, `${phase} PQC 原始提交必须显示明细表`)
  assert.ok(detailRows > 0, `${phase} PQC 原始提交明细不能为空`)
  return {
    processCount,
    submissionBlocks,
    detailTables,
    detailRows,
    inspectionTypes: [...inspectionTypes].sort()
  }
}

const assertNoPqcProcessInspectionRecord = async (scope, phase) => {
  await clickTab(scope, '过程检表单')
  await clickTab(scope, '过程检验记录')
  const tableCount = await scope.locator('[data-pqc-inspection-record-form-table]:visible').count()
  evidence[phase].processInspectionRecordTableCount = tableCount
  assert.equal(tableCount, 0, `${phase} 未点击 P2 前不得显示过程检验汇总记录表`)
  await scope
    .locator('.el-empty', { hasText: '暂无PQC检验记录' })
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
}

const assertPqcProcessInspectionRecordPresent = async (scope, phase) => {
  await clickTab(scope, '过程检表单')
  await clickTab(scope, '过程检验记录')
  const table = scope.locator('[data-pqc-inspection-record-form-table]:visible').first()
  await table.waitFor({ state: 'visible', timeout: 60000 })
  const rows = await table.locator('tbody tr').count()
  const inspectorSignatures = await table
    .locator('[data-active-order-pqc-inspection-record-inspector-signature]')
    .count()
  const reviewerSignatures = await table
    .locator('[data-active-order-pqc-inspection-record-reviewer-signature]')
    .count()
  const text = await table.innerText()
  assert.ok(rows > 0, `${phase} 过程检验记录明细不能为空`)
  assert.ok(inspectorSignatures > 0, `${phase} 过程检验记录必须显示检验人签名`)
  assert.ok(reviewerSignatures > 0, `${phase} 过程检验记录必须显示复核人签名`)
  assert.match(text, /首检|上午巡检|下午巡检|末检/, `${phase} 过程检验记录必须保留检验类型`)
  assert.match(text, /合格|不合格|PASS|FAIL/, `${phase} 过程检验记录必须保留判定结果`)
  evidence[phase].processInspectionRecord = {
    rows,
    inspectorSignatures,
    reviewerSignatures
  }
}

const assertProductionInputBatchesPresent = async (scope, phase) => {
  await clickTab(scope, '生产表单')
  const processTabs = scope.locator('[data-team-leader-active-order-detail-production-process-tab]')
  const processCount = await processTabs.count()
  const batchTexts = []
  for (let index = 0; index < processCount; index += 1) {
    await processTabs.nth(index).click()
    await scope.page().waitForTimeout(150)
    const cells = scope.locator('[data-active-order-production-record-input-material-batch]:visible')
    for (let cellIndex = 0; cellIndex < (await cells.count()); cellIndex += 1) {
      const text = (await cells.nth(cellIndex).innerText()).trim()
      if (text && text !== '-') batchTexts.push(text)
    }
  }
  evidence[phase].productionInputMaterialBatchTexts = batchTexts
  assert.ok(batchTexts.length > 0, `${phase} 批次执行生产表单必须显示 P2 回填后的输入物料批号`)
}

const collectPqcReleaseBusinessFacts = async (scope, phase) => {
  try {
    await scope.getByRole('tab', { name: '总表' }).first().waitFor({
      state: 'visible',
      timeout: 90000
    })
  } catch (error) {
    const detailError = scope.locator('[data-team-leader-active-order-detail-error]:visible').first()
    if (await detailError.count()) {
      assert.fail(`${phase} 详情加载失败：${normalizeText(await detailError.innerText())}`)
    }
    throw error
  }
  await clickTab(scope, '总表')
  const errorCount = await scope.locator('[data-team-leader-active-order-detail-error]:visible').count()
  assert.equal(errorCount, 0, `${phase} 详情不得显示系统异常或加载错误`)
  const releaseTable = scope.locator('[data-active-order-summary-pqc-release-table]').first()
  await releaseTable.waitFor({ state: 'visible', timeout: 60000 })
  const releaseText = normalizeText(await releaseTable.innerText())
  assert.match(releaseText, /PQC生产放行/, `${phase} 必须显示PQC生产放行事实`)
  assert.match(releaseText, /已生产放行|已让步放行/, `${phase} 必须显示已放行状态`)
  const signatureButton = releaseTable
    .locator('[data-active-order-summary-pqc-release-signature]')
    .first()
  const signatureText = normalizeText(await signatureButton.innerText())
  assert.ok(signatureText && !['--', '未签名'].includes(signatureText), `${phase} 必须显示PQC放行签名`)
  assert.equal(await signatureButton.isDisabled(), false, `${phase} PQC放行签名必须可查看`)
  const operationTable = scope.locator('[data-active-order-summary-operation-facts-table]').first()
  await operationTable.waitFor({ state: 'visible', timeout: 60000 })
  const operationText = normalizeText(await operationTable.innerText())
  assert.match(operationText, /PQC生产放行|生产放行/, `${phase} 操作事实必须包含PQC生产放行`)
  const facts = {
    releaseText,
    signatureText,
    operationText: operationText.slice(0, 1200)
  }
  if (!evidence[phase]) {
    evidence[phase] = {}
  }
  evidence[phase].pqcReleaseFacts = facts
  persist()
  return facts
}

const runP1 = async (page) => {
  await openActiveOrderPool(page)
  const row = targetRow(page)
  const button = row.locator('[data-team-leader-simulate-active-order-stage1-p1]').first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await button.isEnabled(), true, 'P1 按钮必须可点击')
  await button.click()
  await firstVisible(page.getByRole('button', { name: '开始模拟' }), '开始模拟').then((item) =>
    item.click()
  )
  await row
    .locator('[data-team-leader-active-order-production-progress]')
    .filter({ hasText: '100' })
    .waitFor({ timeout: 240000 })
  await row
    .locator('[data-team-leader-active-order-inspection-progress]')
    .filter({ hasText: '100' })
    .waitFor({ timeout: 240000 })
  evidence.afterP1.listProgress = {
    production: await row.locator('[data-team-leader-active-order-production-progress]').innerText(),
    inspection: await row.locator('[data-team-leader-active-order-inspection-progress]').innerText()
  }
  persist()
}

const runP2 = async (page) => {
  await openActiveOrderPool(page)
  const row = targetRow(page)
  const button = row.locator('[data-team-leader-generate-active-order-stage1-p2]').first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await button.isEnabled(), true, 'P2 按钮必须在 P1 双100后可点击')
  await button.click()
  const resultResponse = page.waitForResponse((response) =>
    response.url().includes('/active-order/simulation/stage2-5') && response.request().method() === 'POST',
    { timeout: 240000 }
  )
  const listRefresh = page.waitForResponse((response) =>
    response.url().includes('/team-leader/active-order/list') && response.request().method() === 'GET',
    { timeout: 240000 }
  )
  await firstVisible(page.getByRole('button', { name: '开始生成' }), '开始生成').then((item) =>
    item.click()
  )
  const response = await resultResponse
  const result = await response.json()
  evidence.afterP2.response = { status: response.status(), code: result.code, message: result.msg }
  persist()
  assert.equal(response.ok(), true, 'P2 HTTP请求失败')
  assert.equal(result.code, 0, `P2失败：${result.msg}`)
  const refreshed = await listRefresh
  assert.equal(refreshed.ok(), true, 'P2后列表刷新必须成功')
  await page.locator('.el-message--success', { hasText: 'P2 生成完成' }).waitFor({ timeout: 10000 })
  assert.equal(new URL(page.url()).pathname, '/mes/pro/process-pool/production-leader', 'P2完成后必须留在列表')
  evidence.afterP2.batchExecutionId = String(result.data.batchExecutionId)
  evidence.afterP2.stayedOnList = true
  evidence.urls.afterP2 = page.url()
  persist()
  return row
}

const addQuery = (urlText, params) => {
  const url = new URL(urlText)
  for (const [key, value] of Object.entries(params)) {
    url.searchParams.set(key, value)
  }
  return url.toString()
}

const runP3 = async (page, expectSuccess) => {
  await openActiveOrderPool(page)
  const row = targetRow(page)
  await row.locator('[data-team-leader-push-pqc-stage3]').click()
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/active-order/release/push-generated') && response.request().method() === 'POST',
    { timeout: 120000 }
  )
  await page.getByRole('button', { name: '推送PQC', exact: true }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(response.ok(), true)
  if (!expectSuccess) {
    assert.notEqual(body.code, 0, 'P2前P3必须失败')
    assert.match(body.msg, /P2/)
    evidence.p3BeforeP2 = { code: body.code, message: body.msg }
    persist()
    return
  }
  assert.equal(body.code, 0, `P3推送失败：${body.msg}`)
  assert.equal(body.data.status, 'PQC_RELEASE_PENDING')
  assert.ok(body.data.applicationId && body.data.pqcReleaseWorkTaskId)
  await row.locator('[data-team-leader-push-pqc-stage3]:disabled').waitFor({ timeout: 60000 })
  assert.equal(new URL(page.url()).pathname, '/mes/pro/process-pool/production-leader')
  evidence.afterP3 = { applicationId: body.data.applicationId, taskId: body.data.pqcReleaseWorkTaskId, status: body.data.status }
  persist()
  await screenshot(page, 'after-p3-list')
  await clickMenu(page, 'PQC生产放行')
  const releasePage = page.locator('[data-pqc-production-release-page]')
  await releasePage.waitFor({ state: 'visible', timeout: 60000 })
  await releasePage.locator('[data-pqc-production-release-work-order-filter]').fill(WORK_ORDER_CODE)
  await releasePage.locator('[data-pqc-production-release-query]').click()
  const releaseRow = releasePage.locator('[data-pqc-production-release-list] .el-table__body-wrapper tbody tr').filter({ hasText: WORK_ORDER_CODE })
  await releaseRow.first().waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(await releaseRow.count(), 1, 'PQC应只有一条对应放行记录')
  assert.ok((await releaseRow.innerText()).includes(String(body.data.applicationId)))
  evidence.afterP3.visibleInPqcRelease = true
  await screenshot(page, 'pqc-release-received')
  return body.data
}

const approvePqcReleaseAndVerifyReleasedDetail = async (page) => {
  const releasePage = page.locator('[data-pqc-production-release-page]')
  await releasePage.waitFor({ state: 'visible', timeout: 60000 })
  await releasePage.locator('[data-pqc-production-release-work-order-filter]').fill(WORK_ORDER_CODE)
  await releasePage.locator('[data-pqc-production-release-query]').click()
  const releaseRow = releasePage
    .locator('[data-pqc-production-release-list] .el-table__body-wrapper tbody tr')
    .filter({ hasText: WORK_ORDER_CODE })
    .first()
  await releaseRow.waitFor({ state: 'visible', timeout: 60000 })
  const approveButton = releaseRow.locator('[data-pqc-production-release-approve]').first()
  assert.equal(await approveButton.isEnabled(), true, 'PQC放行按钮必须可点击')
  await approveButton.click()
  const dialog = page.locator('[data-pqc-production-release-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-pqc-production-release-signature-password]').fill(PASSWORD)
  await dialog.locator('[data-pqc-production-release-approval-opinion]').fill(`E2E生产放行 ${RUN_ID}`)
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/production-release/pqc/approve') &&
      response.request().method() === 'POST',
    { timeout: 240000 }
  )
  await dialog.locator('[data-pqc-production-release-confirm]').click()
  const response = await responsePromise
  const result = await response.json()
  evidence.afterPqcRelease = {
    response: { status: response.status(), code: result.code, message: result.msg },
    applicationId: result.data?.applicationId,
    batchExecutionId: String(result.data?.batchExecutionId || evidence.afterP2.batchExecutionId || ''),
    signatureId: String(result.data?.signatureId || ''),
    decision: result.data?.decision,
    status: result.data?.status
  }
  persist()
  assert.equal(response.ok(), true, 'PQC生产放行HTTP请求失败')
  assert.equal(result.code, 0, `PQC生产放行失败：${result.msg}`)
  assert.equal(result.data?.decision, 'APPROVE', 'PQC生产放行必须返回APPROVE决定')
  assert.ok(result.data?.signatureId, 'PQC生产放行必须产生电子签名')
  await dialog.locator('[data-pqc-production-release-batch-execution-id]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  await screenshot(page, 'pqc-release-approved-dialog')
  await dialog.getByRole('button', { name: '关闭' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  await releasePage.waitFor({ state: 'visible', timeout: 60000 })
  await firstVisible(releasePage.getByRole('tab', { name: '已放行' }), '已放行').then((tab) => tab.click())
  await releasePage.locator('[data-pqc-production-release-work-order-filter]').fill(WORK_ORDER_CODE)
  await releasePage.locator('[data-pqc-production-release-query]').click()
  const releasedRow = releasePage
    .locator('[data-pqc-production-release-list] .el-table__body-wrapper tbody tr')
    .filter({ hasText: WORK_ORDER_CODE })
    .first()
  await releasedRow.waitFor({ state: 'visible', timeout: 60000 })
  assert.match(await releasedRow.innerText(), /已生产放行|已放行/)
  await releasedRow.locator('[data-pqc-production-release-detail]').first().click()
  const detail = page.locator('[data-pqc-production-release-order-detail]').first()
  await detail.waitFor({ state: 'visible', timeout: 60000 })
  const facts = await collectPqcReleaseBusinessFacts(detail, 'afterPqcReleaseReleasedDetail')
  evidence.afterPqcRelease.releasedDetailFacts = facts
  await screenshot(page, 'pqc-release-released-detail')
  await detail.locator('[data-pqc-production-release-detail-back]').click()
  await releasePage.waitFor({ state: 'visible', timeout: 60000 })
  persist()
  return facts
}

const openBatchExecutionDetailAndVerifyReleaseFacts = async (page, expectedFacts) => {
  await page.goto(
    addQuery(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, {
      workOrderCode: WORK_ORDER_CODE
    }),
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  const listPage = page.locator('[data-user-table-key="mes.pro.edhrBatch.execution.main"]').first()
  await listPage.waitFor({ state: 'visible', timeout: 60000 })
  const row = listPage
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: WORK_ORDER_CODE })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.locator('[data-edhr-batch-active-order-detail]').first().click()
  const detail = page.locator('[data-edhr-batch-active-order-detail-page]').first()
  await detail.waitFor({ state: 'visible', timeout: 60000 })
  const facts = await collectPqcReleaseBusinessFacts(detail, 'afterPqcReleaseBatchExecutionDetail')
  assert.equal(
    facts.signatureText,
    expectedFacts.signatureText,
    '批次执行详情必须显示与PQC已放行详情一致的放行签名'
  )
  evidence.afterPqcRelease.batchExecutionDetailFacts = facts
  await screenshot(page, 'batch-execution-active-order-detail-after-pqc-release')
  persist()
  return facts
}

const approveMarketReleaseAndVerifyHistory = async (page, expectedFacts) => {
  await page.goto(
    addQuery(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, {
      workOrderCode: WORK_ORDER_CODE
    }),
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  const listPage = page.locator('[data-user-table-key="mes.pro.edhrBatch.execution.main"]').first()
  await listPage.waitFor({ state: 'visible', timeout: 60000 })
  const row = listPage
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: WORK_ORDER_CODE })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const releaseButton = row.locator('[data-edhr-batch-action="release"]').first()
  await releaseButton.waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(await releaseButton.isEnabled(), true, 'PQC放行后批次执行必须可以上市放行')
  await releaseButton.click()
  const dialog = page.locator('.el-dialog').filter({ hasText: '上市放行确认' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByPlaceholder('请输入上市放行负责人的电子签名密码').fill(PASSWORD)
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/edhr-release/approve') &&
      response.request().method() === 'POST',
    { timeout: 240000 }
  )
  await dialog.getByRole('button', { name: '确认上市放行' }).click()
  const response = await responsePromise
  const result = await response.json()
  evidence.afterMarketRelease = {
    response: { status: response.status(), code: result.code, message: result.msg },
    data: result.data || null
  }
  persist()
  assert.equal(response.ok(), true, '上市放行HTTP请求失败')
  assert.equal(result.code, 0, `上市放行失败：${result.msg}`)
  await page.waitForURL(/\/mes\/pro\/feedback\/edhr-batch-history/, { timeout: 60000 })
  const historyPage = page.locator('[data-edhr-batch-history-page]').first()
  await historyPage.waitFor({ state: 'visible', timeout: 60000 })
  await historyPage.locator('[data-edhr-history-work-order-filter]').fill(WORK_ORDER_CODE)
  await historyPage.locator('[data-edhr-history-query]').click()
  const historyRow = historyPage
    .locator('[data-edhr-history-batch-item]')
    .filter({ hasText: WORK_ORDER_CODE })
    .first()
  await historyRow.waitFor({ state: 'visible', timeout: 60000 })
  await historyRow.click()
  const timeline = historyPage.locator('[data-edhr-history-timeline-item]')
  await timeline.first().waitFor({ state: 'visible', timeout: 60000 })
  const timelineText = await timeline.allInnerTexts()
  assert.match(timelineText.join('\n'), /上市放行|放行/, '历史追溯必须包含上市放行历史信息')
  assert.match(timelineText.join('\n'), /签名|电子签名|APPROVED|已批准|已放行/, '历史追溯必须包含放行签名或审批证据')
  const detailButton = historyPage.locator('[data-edhr-history-active-order-detail]').first()
  await detailButton.click()
  const detail = page.locator('[data-edhr-batch-active-order-detail-page]').first()
  await detail.waitFor({ state: 'visible', timeout: 60000 })
  const facts = await collectPqcReleaseBusinessFacts(detail, 'afterMarketReleaseHistoryDetail')
  assert.equal(
    facts.signatureText,
    expectedFacts.signatureText,
    '历史追溯入口的详情必须显示与PQC已放行详情一致的放行签名'
  )
  assert.match(
    facts.operationText,
    /活跃订单资料上传|DOSSIER_UPLOAD/,
    '历史追溯入口的详情必须显示资料上传操作事实'
  )
  await assertUploadedDossierFilesVisible(detail, 'afterMarketReleaseHistoryDetail')
  evidence.afterMarketRelease.historyDetailFacts = facts
  await screenshot(page, 'history-active-order-detail-after-market-release')
  await page.goBack({ waitUntil: 'domcontentloaded', timeout: 60000 })
  await historyPage.waitFor({ state: 'visible', timeout: 60000 })
  const attachmentItems = await historyPage.locator('[data-edhr-history-attachment-item]').count()
  evidence.afterMarketRelease.historyAttachmentItems = attachmentItems
  evidence.afterMarketRelease.historyTimelineItems = await timeline.count()
  await screenshot(page, 'batch-history-after-market-release')
  persist()
  return evidence.afterMarketRelease
}

const verifyMaterialActualUsage = async (scope) => {
  await clickTab(scope, '生产表单')
  const processTabs = scope.locator('[data-team-leader-active-order-detail-production-process-tab]')
  const expected = new Map()
  for (let index = 0; index < await processTabs.count(); index += 1) {
    await processTabs.nth(index).click()
    const rows = scope.locator('[data-active-order-production-record-input-materials]:visible .el-table__body-wrapper tbody tr')
    for (let i = 0; i < await rows.count(); i += 1) {
      const cells = (await rows.nth(i).locator('td').allInnerTexts()).map(value => value.trim())
      assert.ok(cells[0] && cells[5] && cells[5] !== '-', '输入物料必须有代码和数量')
      const quantity = Number(cells[5].replace(/,/g, ''))
      assert.ok(Number.isFinite(quantity))
      expected.set(cells[0], Math.max(expected.get(cells[0]) ?? quantity, quantity))
    }
  }
  assert.ok(expected.size > 0, '必须从真实生产输入行采集期望数量')
  await clickTab(scope, '生产用料清单')
  const document = scope.locator('[data-active-order-production-material-list-document]')
  await document.first().waitFor({ state: 'visible', timeout: 60000 })
  const rows = document.locator('tbody tr')
  const comparisons = []
  for (let i = 0; i < await rows.count(); i += 1) {
    const cells = (await rows.nth(i).locator('td').allInnerTexts()).map(value => value.trim())
    const code = cells[1]
    comparisons.push({ code, actualUsage: cells[7], expected: expected.get(code) ?? null })
  }
  evidence.afterP2.materialUsageComparisons = comparisons
  persist()
  await screenshot(scope.page(), 'p2-material-actual-usage')
  assert.ok(comparisons.length > 0)
  for (const row of comparisons) {
    if (row.expected === null) {
      assert.equal(row.actualUsage, '', `物料${row.code}没有生产输入来源时不得补造实际用量`)
      continue
    }
    assert.notEqual(row.actualUsage, '', `物料${row.code}存在生产输入来源但实际用量为空`)
    assert.equal(
      Number(row.actualUsage.replace(/,/g, '')),
      row.expected,
      `物料${row.code}实际用量应取生产输入最大值`
    )
  }
}

async function main() {
  assert.ok(BASE_URL, '缺少前端地址')
  assert.ok(TENANT && USERNAME && PASSWORD, '缺少真实登录信息')
  assert.ok(WORK_ORDER_CODE, '缺少目标生产订单号')
  assert.ok(fs.existsSync(CHROME_EXECUTABLE), 'Chrome 不存在，无法执行真实浏览器 E2E')
  persist()
  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false', executablePath: CHROME_EXECUTABLE })
  const page = await browser.newPage({ viewport: { width: 1680, height: 960 } })
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    evidence.failedRequests.push({
      method: request.method(),
      url: request.url(),
      error: request.failure()?.errorText || 'unknown'
    })
  })

  try {
    await login(page)
    await resetFixedOrder(page)

    const beforeDetail = await openDetailFromRow(page)
    await assertSummaryMaterialBatchesEmpty(beforeDetail, 'beforeP1')
    await assertMaterialTabHasNoBatchLots(beforeDetail, 'beforeP1')
    await assertNoDetailP2Entrypoints(beforeDetail, 'beforeP1')
    await screenshot(page, 'before-p1-detail')

    await runP1(page)
    const afterP1Detail = await openDetailFromRow(page)
    await assertSummaryMaterialBatchesEmpty(afterP1Detail, 'afterP1')
    await assertMaterialTabHasNoBatchLots(afterP1Detail, 'afterP1')
    await assertNoDetailP2Entrypoints(afterP1Detail, 'afterP1')
    evidence.afterP1.productionFacts = await collectProductionFacts(afterP1Detail, 'afterP1')
    evidence.afterP1.pqcOriginalFacts = await collectPqcOriginalFacts(afterP1Detail, 'afterP1')
    await assertNoPqcProcessInspectionRecord(afterP1Detail, 'afterP1')
    await screenshot(page, 'after-p1-detail')

    if (process.argv.includes('--with-p3')) await runP3(page, false)

    await runP2(page)
    await screenshot(page, 'after-p2-list')

    await openActiveOrderPool(page)
    const afterP2ActiveDetail = await openDetailFromRow(page)
    await assertProductionInputBatchesPresent(afterP2ActiveDetail, 'afterP2')
    evidence.afterP2.productionFacts = await collectProductionFacts(afterP2ActiveDetail, 'afterP2')
    evidence.afterP2.pqcOriginalFacts = await collectPqcOriginalFacts(afterP2ActiveDetail, 'afterP2')
    assert.deepEqual(evidence.afterP2.productionFacts, evidence.afterP1.productionFacts, 'P2 不得新增生产提交或复核')
    assert.deepEqual(evidence.afterP2.pqcOriginalFacts, evidence.afterP1.pqcOriginalFacts, 'P2 不得新增PQC提交或明细')
    await assertPqcProcessInspectionRecordPresent(afterP2ActiveDetail, 'afterP2')
    await assertSummaryMaterialBatchesPresent(afterP2ActiveDetail, 'afterP2')
    await assertNoDetailP2Entrypoints(afterP2ActiveDetail, 'afterP2')
    await assertDossierTabsReadable(afterP2ActiveDetail, 'afterP2')
    await uploadDossierFilesViaDetail(afterP2ActiveDetail, 'afterP2')
    await assertUploadedDossierFilesVisible(afterP2ActiveDetail, 'afterP2')
    await screenshot(page, 'after-p2-active-order-detail')

    if (process.argv.includes('--material-usage')) await verifyMaterialActualUsage(afterP2ActiveDetail)

    if (process.argv.includes('--with-p3')) {
      await runP3(page, true)
      const releasedFacts = await approvePqcReleaseAndVerifyReleasedDetail(page)
      await openBatchExecutionDetailAndVerifyReleaseFacts(page, releasedFacts)
      if (process.argv.includes('--with-market-release')) {
        await approveMarketReleaseAndVerifyHistory(page, releasedFacts)
      }
    }

    assert.equal(evidence.pageErrors.length, 0, `页面运行错误：${evidence.pageErrors.join('\n')}`)
    evidence.status = 'PASS'
    persist()
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error.stack || error.message
    evidence.urls.failure = page.url()
    try {
      evidence.failureBodyText = (await page.locator('body').innerText({ timeout: 5000 }))
        .replace(/\s+/g, ' ')
        .slice(0, 4000)
      await screenshot(page, 'failure')
    } catch (captureError) {
      evidence.consoleErrors.push(`失败证据采集失败: ${captureError.message}`)
    }
    persist()
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exitCode = 1
})
