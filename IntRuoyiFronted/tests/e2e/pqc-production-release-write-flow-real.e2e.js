const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.PQC_RELEASE_WRITE_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.PQC_RELEASE_WRITE_E2E_TENANT || ''
const USERNAME = process.env.PQC_RELEASE_WRITE_E2E_USERNAME || ''
const PASSWORD = process.env.PQC_RELEASE_WRITE_E2E_PASSWORD || ''
const CLEANUP_ACTIVE_ORDER_ID = process.env.PQC_RELEASE_WRITE_E2E_CLEANUP_ACTIVE_ORDER_ID || ''
const RESUME_ACTIVE_ORDER_ID = process.env.PQC_RELEASE_WRITE_E2E_RESUME_ACTIVE_ORDER_ID || ''
const RESUME_APPLICATION_ID = process.env.PQC_RELEASE_WRITE_E2E_RESUME_APPLICATION_ID || ''
const RESUME_PQC_WORK_TASK_ID = process.env.PQC_RELEASE_WRITE_E2E_RESUME_PQC_WORK_TASK_ID || ''
const RESUME_REVIEW_ID = process.env.PQC_RELEASE_WRITE_E2E_RESUME_REVIEW_ID || ''
const CHROME_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const RUN_ID = `PQC-RELEASE-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`
const RESULT_DIR = path.resolve(
  process.cwd(),
  'output',
  'playwright',
  'pqc-production-release-write-flow',
  RUN_ID
)
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')
const REVIEW_FIXTURE = path.resolve(
  process.cwd(),
  'tests',
  'e2e',
  'fixtures',
  'pqc-production-release-review.txt'
)

const evidence = {
  runId: RUN_ID,
  status: 'RUNNING',
  tenant: TENANT,
  username: USERNAME,
  templateActiveOrderId: null,
  templateWorkOrderCode: null,
  activeOrderId: null,
  workOrderCode: null,
  applicationId: null,
  pqcWorkTaskId: null,
  reviewId: null,
  batchExecutionId: null,
  signatureId: null,
  targetWrites: [],
  cleanup: { attempted: false, activeOrderRemoved: false, templateActiveOrderRemoved: false },
  consoleErrors: [],
  pageErrors: [],
  failedRequests: []
}

const persist = () => {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_FILE, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

const firstVisible = async (locator, label) => {
  for (let index = 0; index < (await locator.count()); index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`未找到可见控件：${label}`)
}

const clickMenu = async (page, text) => {
  const textNode = await firstVisible(
    page.locator('.el-menu').getByText(text, { exact: true }),
    text
  )
  const target = textNode.locator(
    'xpath=ancestor-or-self::*[contains(@class, "el-menu-item") or contains(@class, "el-sub-menu__title")][1]'
  )
  const submenu = target.locator('xpath=ancestor::li[contains(@class, "el-sub-menu")][1]')
  const targetClass = (await target.getAttribute('class')) || ''
  if (
    targetClass.includes('el-sub-menu__title') &&
    (await submenu.count()) > 0 &&
    (await submenu.getAttribute('aria-expanded')) === 'true'
  ) {
    return
  }
  await target.click()
  await page.waitForTimeout(250)
}

const waitBusinessWrite = async (page, endpoint, action) => {
  const responsePromise = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname.endsWith(endpoint) &&
      ['POST', 'PUT', 'DELETE'].includes(response.request().method()),
    { timeout: 120000 }
  )
  await action()
  const response = await responsePromise
  const body = await response.json()
  evidence.targetWrites.push({
    endpoint,
    method: response.request().method(),
    httpStatus: response.status(),
    businessCode: body.code
  })
  persist()
  assert.equal(response.status(), 200, `${endpoint} HTTP失败`)
  assert.equal(body.code, 0, `${endpoint} 业务失败：${body.msg || body.code}`)
  return body.data
}

const login = async (page) => {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.login-form:visible').first().waitFor({ state: 'visible', timeout: 60000 })
  const form = await firstVisible(page.locator('.login-form'), '登录表单')
  const tenantInput = await firstVisible(form.locator('input.el-select__input'), '租户')
  await tenantInput.click()
  await tenantInput.fill(TENANT)
  await page.waitForTimeout(300)
  await firstVisible(
    page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: TENANT }),
    '租户选项'
  ).then((item) => item.click())
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

const openProductionLeaderActiveOrders = async (page) => {
  await clickMenu(page, 'MES 系统')
  await clickMenu(page, 'eDHR批记录')
  await clickMenu(page, '生产组长')
  await page.waitForURL((url) => url.pathname === '/mes/pro/process-pool/production-leader', {
    timeout: 60000
  })
  await page.getByRole('tab', { name: '活跃订单池' }).click()
  await page
    .locator('[data-team-leader-active-order-list]')
    .waitFor({ state: 'visible', timeout: 60000 })
}

const rowByActiveOrderId = (page, activeOrderId) =>
  page
    .locator('[data-team-leader-active-order-list] .el-table__body-wrapper tbody tr')
    .filter({ has: page.locator(`[data-team-leader-active-order-id="${activeOrderId}"]`) })
    .first()

const createTemplateActiveOrder = async (page) => {
  await page.locator('[data-team-leader-open-active-order-dialog]').click()
  const dialog = page.locator('[data-team-leader-active-order-add-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const orderInput = dialog.locator('[data-team-leader-active-order-work-order-code] input').first()
  evidence.candidateDiagnostics = []
  let candidates = []
  let selectedKeyword = ''
  for (const keyword of ['88', '1', '2', 'A', 'S']) {
    const candidateResponse = page.waitForResponse(
      (response) => {
        const url = new URL(response.url())
        return (
          url.pathname.endsWith('/mes/pro/process-pool/team-leader/active-order/candidates') &&
          url.searchParams.get('keyword') === keyword
        )
      },
      { timeout: 60000 }
    )
    await orderInput.fill(keyword)
    const candidateBody = await (await candidateResponse).json()
    assert.equal(candidateBody.code, 0, candidateBody.msg || '活跃订单候选查询失败')
    evidence.candidateDiagnostics.push({
      keyword,
      candidates: (candidateBody.data || []).map((item) => ({
        workOrderCode: item.workOrderCode,
        candidateState: item.candidateState,
        eligible: item.eligible,
        ineligibleReason: item.ineligibleReason || null
      }))
    })
    persist()
    const addableCandidates = (candidateBody.data || []).filter(
      (item) => item.eligible && item.candidateState === 'ADDABLE'
    )
    const reusableStage1Candidates = (candidateBody.data || []).filter(
      (item) =>
        item.eligible &&
        item.candidateState === 'REUSABLE' &&
        item.workOrderCode.startsWith('STAGE1-WO-')
    )
    candidates = addableCandidates.length > 0 ? addableCandidates : reusableStage1Candidates
    if (candidates.length > 0) {
      selectedKeyword = keyword
      break
    }
  }
  assert.ok(candidates.length > 0, '宽关键词候选中没有ADDABLE工单或任务自有STAGE1可复用工单')

  let selected
  for (const candidate of candidates) {
    const exactCandidateResponse = page.waitForResponse(
      (response) => {
        const url = new URL(response.url())
        return (
          url.pathname.endsWith('/mes/pro/process-pool/team-leader/active-order/candidates') &&
          url.searchParams.get('keyword') === candidate.workOrderCode
        )
      },
      { timeout: 60000 }
    )
    await orderInput.fill(candidate.workOrderCode)
    const exactCandidateBody = await (await exactCandidateResponse).json()
    assert.equal(exactCandidateBody.code, 0, exactCandidateBody.msg || '精确候选查询失败')
    assert.ok(
      (exactCandidateBody.data || []).some(
        (item) => String(item.workOrderId) === String(candidate.workOrderId) && item.eligible
      ),
      `精确候选查询未返回可选工单：${candidate.workOrderCode}`
    )
    const option = page
      .locator('.team-leader-workbench__active-order-candidate-popper .el-select-dropdown__item')
      .filter({ hasText: candidate.workOrderCode })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    const pickListResponse = page.waitForResponse(
      (response) => {
        const url = new URL(response.url())
        return (
          url.pathname.endsWith(
            '/mes/pro/process-pool/team-leader/active-order/pick-list-options'
          ) && url.searchParams.get('workOrderId') === String(candidate.workOrderId)
        )
      },
      { timeout: 60000 }
    )
    await option.click()
    const pickListBody = await (await pickListResponse).json()
    assert.equal(pickListBody.code, 0, pickListBody.msg || '领料单候选查询失败')
    evidence.pickListDiagnostics = (pickListBody.data || []).map((item) => ({
      pickListId: item.pickListId,
      sourceBillNo: item.sourceBillNo || null,
      sourceFid: item.sourceFid || null,
      selectable: item.selectable,
      documentStatus: item.documentStatus
    }))
    persist()
    const pickList = (pickListBody.data || []).find((item) => item.selectable)
    if (!pickList) {
      await orderInput.clear()
      await orderInput.fill(selectedKeyword)
      await page.waitForTimeout(300)
      continue
    }
    const pickListInput = dialog
      .locator('[data-team-leader-active-order-pick-list] input[role="combobox"]')
      .first()
    await pickListInput.waitFor({ state: 'visible', timeout: 30000 })
    await page.waitForFunction((input) => !input.disabled, await pickListInput.elementHandle(), {
      timeout: 30000
    })
    await pickListInput.click()
    await pickListInput.press('ArrowDown')
    await pickListInput.press('Enter')
    selected = { candidate, pickList }
    break
  }
  assert.ok(selected, 'ADDABLE工单均缺少可绑定的已审核领料单')

  const receipt = await waitBusinessWrite(
    page,
    '/mes/pro/process-pool/team-leader/active-order/add',
    async () =>
      firstVisible(
        dialog.getByRole('button', { name: /加入活跃订单|确认复用/ }),
        '提交活跃订单'
      ).then((item) => item.click())
  )
  assert.ok(['ADD', 'REUSE'].includes(receipt.action), `不支持的模板提交动作：${receipt.action}`)
  evidence.templateActiveOrderId = String(receipt.activeOrderId)
  evidence.templateWorkOrderCode = selected.candidate.workOrderCode
  persist()
  await rowByActiveOrderId(page, evidence.templateActiveOrderId).waitFor({
    state: 'visible',
    timeout: 60000
  })
}

const createStage1Sample = async (page) => {
  await openProductionLeaderActiveOrders(page)
  const buttons = page.locator('[data-team-leader-simulate-active-order-stage1]')
  await buttons
    .first()
    .waitFor({ state: 'visible', timeout: 15000 })
    .catch(() => {})
  if ((await buttons.count()) === 0) {
    await createTemplateActiveOrder(page)
  }
  const refreshedButtons = page.locator('[data-team-leader-simulate-active-order-stage1]')
  let button
  for (let index = 0; index < (await refreshedButtons.count()); index += 1) {
    const candidate = refreshedButtons.nth(index)
    if ((await candidate.isVisible()) && (await candidate.isEnabled())) {
      button = candidate
      break
    }
  }
  assert.ok(button, '没有可用的 Stage1 模拟模板订单')
  const data = await waitBusinessWrite(
    page,
    '/mes/pro/process-pool/team-leader/active-order/simulation/stage1',
    async () => {
      await button.click()
      await firstVisible(page.getByRole('button', { name: '开始模拟' }), '开始模拟').then((item) =>
        item.click()
      )
    }
  )
  evidence.activeOrderId = String(data.activeOrderId)
  evidence.workOrderCode = data.workOrderCode || null
  persist()
  const row = rowByActiveOrderId(page, evidence.activeOrderId)
  await row.waitFor({ state: 'visible', timeout: 90000 })
  await row
    .locator('[data-team-leader-active-order-production-progress]')
    .filter({ hasText: '100' })
    .waitFor()
  await row
    .locator('[data-team-leader-active-order-inspection-progress]')
    .filter({ hasText: '100' })
    .waitFor()
}

const applyForRelease = async (page) => {
  const row = rowByActiveOrderId(page, evidence.activeOrderId)
  const data = await waitBusinessWrite(
    page,
    '/mes/pro/process-pool/team-leader/active-order/release/apply',
    async () => {
      await row.locator('[data-team-leader-active-order-release-apply]').click()
      await firstVisible(page.getByRole('button', { name: '申请放行' }), '申请放行').then((item) =>
        item.click()
      )
    }
  )
  assert.equal(data.status, 'PQC_RELEASE_PENDING')
  evidence.applicationId = String(data.applicationId)
  evidence.pqcWorkTaskId = String(data.pqcReleaseWorkTaskId)
  persist()
}

const openPqcReleasePage = async (page) => {
  const pageResponse = page.waitForResponse(
    (response) => {
      const url = new URL(response.url())
      return (
        response.request().method() === 'GET' &&
        url.pathname.endsWith('/mes/pro/production-release/pqc/page') &&
        url.searchParams.get('viewStatus') === 'PENDING'
      )
    },
    { timeout: 60000 }
  )
  await clickMenu(page, 'MES 系统')
  await clickMenu(page, 'eDHR批记录')
  await clickMenu(page, 'PQC生产放行')
  await page.waitForURL((url) => url.pathname === '/mes/production-release/pqc', { timeout: 60000 })
  const pageBody = await (await pageResponse).json()
  assert.equal(pageBody.code, 0, pageBody.msg || 'PQC生产放行列表加载失败')
  await page
    .locator('[data-pqc-production-release-page]')
    .waitFor({ state: 'visible', timeout: 60000 })
  const row = page
    .locator('[data-pqc-production-release-list] .el-table__body-wrapper tbody tr')
    .filter({ hasText: `申请编号：${evidence.applicationId}` })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  return row
}

const createAndConcedeReview = async (page) => {
  const releaseRow = await openPqcReleasePage(page)
  await releaseRow.locator('[data-pqc-production-release-nonconformance]').click()
  await page.waitForURL((url) => url.pathname === '/mes/pro/feedback/edhr-nonconformance-review', {
    timeout: 60000
  })
  const reasonItem = page.locator('.el-form-item').filter({ hasText: '不合格原因' }).first()
  await reasonItem.locator('textarea').fill(`M6让步评审 ${RUN_ID}`)
  const review = await waitBusinessWrite(
    page,
    '/mes/pro/edhr-nonconformance-review/create',
    async () => {
      await page.getByRole('button', { name: '提交不合格评审' }).click()
    }
  )
  evidence.reviewId = String(review.id)
  persist()

  const detail = page.locator('.edhr-ncr__detail')
  await detail.waitFor({ state: 'visible', timeout: 60000 })
  const uploadInput = detail.locator('input[type="file"]').first()
  await uploadInput.setInputFiles(REVIEW_FIXTURE)
  await page.waitForTimeout(1000)
  await detail
    .locator('.el-form-item')
    .filter({ hasText: '评审意见' })
    .locator('textarea')
    .fill('同意让步，待PQC签字')
  await detail
    .locator('.el-form-item')
    .filter({ hasText: 'QA签名' })
    .locator('input')
    .fill('admin QA')
  const disposed = await waitBusinessWrite(
    page,
    '/mes/pro/edhr-nonconformance-review/dispose',
    async () => {
      await detail.getByRole('button', { name: '让步放行', exact: true }).click()
    }
  )
  assert.equal(disposed.disposition, 'concession_release')
}

const signRelease = async (page) => {
  const row = await openPqcReleasePage(page)
  await row.getByText('待让步签字', { exact: true }).waitFor({ state: 'visible', timeout: 60000 })
  await row.locator('[data-pqc-production-release-approve]').click()
  const dialog = page.locator('[data-pqc-production-release-dialog]:visible')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="password"]').fill(PASSWORD)
  await dialog.locator('textarea').fill(`M6电子签名放行 ${RUN_ID}`)
  const data = await waitBusinessWrite(
    page,
    '/mes/pro/production-release/pqc/approve',
    async () => {
      await dialog.getByRole('button', { name: '确认放行', exact: true }).click()
    }
  )
  assert.equal(data.status, 'REPORT_UPLOAD_PENDING')
  evidence.batchExecutionId = String(data.batchExecutionId)
  evidence.signatureId = String(data.signatureId)
  persist()
  await page.getByRole('tab', { name: '已让步放行' }).click()
  await page
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: evidence.applicationId })
    .waitFor({ state: 'visible', timeout: 60000 })
}

const cleanupActiveOrder = async (page) => {
  evidence.cleanup.attempted = true
  persist()
  await openProductionLeaderActiveOrders(page)
  const row = rowByActiveOrderId(page, evidence.activeOrderId)
  await row.waitFor({ state: 'visible', timeout: 15000 })
  const data = await waitBusinessWrite(
    page,
    '/mes/pro/process-pool/team-leader/active-order/remove',
    async () => row.locator('[data-team-leader-remove-active-order]').click()
  )
  void data
  evidence.cleanup.activeOrderRemoved = true
  if (evidence.templateActiveOrderId) {
    const templateRow = rowByActiveOrderId(page, evidence.templateActiveOrderId)
    if (await templateRow.isVisible().catch(() => false)) {
      await waitBusinessWrite(
        page,
        '/mes/pro/process-pool/team-leader/active-order/remove',
        async () => templateRow.locator('[data-team-leader-remove-active-order]').click()
      )
    }
    evidence.cleanup.templateActiveOrderRemoved = true
  }
  persist()
}

async function main() {
  assert.equal(BASE_URL, 'http://127.0.0.1:8311')
  assert.ok(TENANT && USERNAME && PASSWORD, '真实登录输入缺失')
  assert.ok(fs.existsSync(CHROME_EXECUTABLE), 'Chrome不存在')
  assert.ok(fs.existsSync(REVIEW_FIXTURE), '评审材料fixture不存在')
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: CHROME_EXECUTABLE })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
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
  persist()
  try {
    await login(page)
    if (CLEANUP_ACTIVE_ORDER_ID) {
      evidence.activeOrderId = CLEANUP_ACTIVE_ORDER_ID
      await cleanupActiveOrder(page)
      evidence.status = evidence.cleanup.activeOrderRemoved ? 'CLEANUP_PASS' : 'CLEANUP_FAIL'
      persist()
      return
    }
    if (RESUME_APPLICATION_ID) {
      assert.ok(
        RESUME_ACTIVE_ORDER_ID && RESUME_PQC_WORK_TASK_ID,
        '续跑必须同时提供活跃订单ID、放行申请ID和PQC待办ID'
      )
      evidence.activeOrderId = RESUME_ACTIVE_ORDER_ID
      evidence.applicationId = RESUME_APPLICATION_ID
      evidence.pqcWorkTaskId = RESUME_PQC_WORK_TASK_ID
      persist()
    } else {
      await createStage1Sample(page)
      await applyForRelease(page)
    }
    if (RESUME_REVIEW_ID) {
      evidence.reviewId = RESUME_REVIEW_ID
      persist()
    } else {
      await createAndConcedeReview(page)
    }
    await signRelease(page)
    await page.screenshot({ path: path.join(RESULT_DIR, 'released.png'), fullPage: true })
    try {
      await cleanupActiveOrder(page)
    } catch (cleanupError) {
      evidence.cleanup.error =
        cleanupError instanceof Error ? cleanupError.message : String(cleanupError)
    }
    evidence.status =
      evidence.cleanup.activeOrderRemoved && evidence.cleanup.templateActiveOrderRemoved
        ? 'PASS'
        : 'PASS_WITH_CLEANUP_GAP'
    persist()
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error instanceof Error ? error.message : String(error)
    await page
      .screenshot({ path: path.join(RESULT_DIR, 'failure.png'), fullPage: true })
      .catch(() => {})
    if (evidence.activeOrderId && !evidence.applicationId) {
      try {
        await page.keyboard.press('Escape')
        await cleanupActiveOrder(page)
      } catch (cleanupError) {
        evidence.cleanup.error =
          cleanupError instanceof Error ? cleanupError.message : String(cleanupError)
      }
    }
    persist()
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
})
