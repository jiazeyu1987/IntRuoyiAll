const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const taskDir = path.join(repoRoot, 'doc', 'tasks', '20260721-edhr-special-node-pending-attachment-persistence')
const artifactDir = path.join(taskDir, 'e2e-artifacts')
const resultFile = path.join(artifactDir, 'pending-attachment-persistence-real-e2e.json')
const screenshotFile = path.join(artifactDir, 'pending-attachment-persistence-real-e2e.png')
const failureScreenshotFile = path.join(artifactDir, 'pending-attachment-persistence-real-e2e-failure.png')

const config = {
  baseUrl: process.env.EDHR_PENDING_ATTACHMENT_BASE_URL || 'http://localhost:8081',
  backendUrl: process.env.EDHR_PENDING_ATTACHMENT_BACKEND_URL || 'http://127.0.0.1:48081',
  tenant: process.env.EDHR_PENDING_ATTACHMENT_TENANT || '测试租户',
  username: process.env.EDHR_PENDING_ATTACHMENT_USERNAME || 'aoteman',
  password: process.env.EDHR_PENDING_ATTACHMENT_PASSWORD || '111111',
  headed: process.env.EDHR_PENDING_ATTACHMENT_HEADED === '1',
  preferredWorkOrderCode: process.env.EDHR_PENDING_ATTACHMENT_WORK_ORDER_CODE || '881MO090863',
  preferredRouteKeyword: process.env.EDHR_PENDING_ATTACHMENT_ROUTE_KEYWORD || '',
  authorizedYudaoAdminWrites: process.env.EDHR_PENDING_ATTACHMENT_ALLOW_YUDAO_ADMIN_WRITES === '1',
  targetBatchCode: process.env.EDHR_PENDING_ATTACHMENT_BATCH_CODE || ''
}

const routePath = '/mes/pro/feedback/edhr-batch-execution'
const detailPath = `${routePath}/detail`
const targetNodeName = process.env.EDHR_PENDING_ATTACHMENT_NODE_NAME || '来料检报告'
const batchCodePrefix = `E2E-PENDING-ATTACH-${Date.now()}`
const testFileNamePrefix = 'codex-edhr-pending-reload-'
const expectedCreateSampleBlockers = ['工艺路线不存在', '缺少工艺流程批记录配置流程配置或默认批记录']

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function writeResult(status, payload) {
  ensureDir(artifactDir)
  fs.writeFileSync(
    resultFile,
    JSON.stringify(
      {
        status,
        generatedAt: new Date().toISOString(),
        ...payload
      },
      null,
      2
    ),
    'utf8'
  )
}

function assertWriteScope() {
  assert.equal(config.baseUrl, 'http://localhost:8081', 'real E2E must use local frontend 8081')
  assert.match(config.backendUrl, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'real E2E must use local backend 48081')
  if (config.authorizedYudaoAdminWrites) {
    assert.equal(config.tenant, '芋道源码', 'authorized admin write E2E must use 芋道源码')
    assert.equal(config.username, 'admin', 'authorized admin write E2E must use admin')
    assert.match(config.targetBatchCode, /^EDHRB-\d+$/, 'authorized admin write E2E requires a target batch code')
    assert.ok(config.password, 'authorized admin account password is required')
    return
  }
  assert.equal(config.tenant, '测试租户', 'write E2E must use 测试租户')
  assert.equal(config.username, 'aoteman', 'write E2E must use aoteman')
  assert.ok(config.password, 'test account password is required')
}

function isExpectedCreateSampleBlocker(bodyOrMessage) {
  const message =
    typeof bodyOrMessage === 'string'
      ? bodyOrMessage
      : String(bodyOrMessage?.message || bodyOrMessage?.msg || JSON.stringify(bodyOrMessage))
  return expectedCreateSampleBlockers.some((blocker) => message.includes(blocker))
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`Missing visible input: ${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && !(await item.isDisabled().catch(() => true))) {
      await item.click()
      return item
    }
  }
  throw new Error(`Missing enabled target: ${label}`)
}

async function login(page) {
  let lastError = ''
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      const loginUrl = new URL('/login', config.baseUrl)
      loginUrl.searchParams.set('redirect', '/index')
      await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

      const form = page.locator('form.login-form:visible').first()
      await form.waitFor({ state: 'visible', timeout: 60000 })

      const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
      if (await tenantInput.count()) {
        await tenantInput.fill(config.tenant)
        const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
        await tenantOption.waitFor({ state: 'visible', timeout: 60000 })
        await tenantOption.click()
      } else {
        const textboxes = form.locator('input.el-input__inner')
        await textboxes.nth(0).fill(config.tenant)
      }

      const usernameInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
      await usernameInput.fill(config.username)
      await form.locator('input[type="password"]').first().fill(config.password)

      const loginResponsePromise = page.waitForResponse(
        (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
        { timeout: 60000 }
      )
      await form.getByRole('button', { name: '登录' }).click()
      const response = await loginResponsePromise
      const body = await response.json()
      assert.ok(response.ok(), `login HTTP failed: ${response.status()}`)
      assert.ok([0, 200].includes(body.code), `login business failed: ${JSON.stringify(body)}`)
      await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
      return
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error)
      if (attempt === 3) break
    }
  }
  throw new Error(`真实登录失败：${lastError}`)
}

async function openCreateDialog(page) {
  await page.goto(`${config.baseUrl}${routePath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
  await clickFirstEnabled(page.getByRole('button', { name: '打开/创建' }), '打开/创建')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  return dialog
}

async function selectPreferredWorkOrder(page, dialog) {
  const workOrderSelect = dialog.locator('.el-select input[role="combobox"]').first()
  await workOrderSelect.click()
  await workOrderSelect.fill(config.preferredWorkOrderCode)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: config.preferredWorkOrderCode })
    .first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
}

async function selectRouteOption(page, dialog) {
  const routeFormItem = dialog.locator('.el-form-item').filter({ hasText: '工艺路线' }).first()
  const routeDropdown = routeFormItem.locator('.el-select').first()
  const routeSelect = routeFormItem.locator('.el-select input[role="combobox"]').first()
  await routeSelect.waitFor({ state: 'visible', timeout: 60000 })
  await page.waitForFunction(
    (input) => Boolean(input && !input.disabled && !input.closest('.is-disabled')),
    await routeSelect.elementHandle(),
    { timeout: 60000 }
  )
  const selectedRouteText = await routeFormItem.evaluate((element) => {
    const textCandidates = Array.from(element.querySelectorAll('.el-select__selected-item'))
      .map((item) => item.textContent?.trim() || '')
      .filter((text) => text && !text.includes('请先选择工单'))
    const input = element.querySelector('input[role="combobox"]')
    const inputValue = input?.value?.trim() || ''
    return inputValue || textCandidates[0] || ''
  })
  if (selectedRouteText.trim()) {
    return
  }
  await routeDropdown.click()
  const routeOptions = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
  const routeOption = config.preferredRouteKeyword
    ? routeOptions.filter({ hasText: config.preferredRouteKeyword }).first()
    : routeOptions.first()
  await routeOption.waitFor({ state: 'visible', timeout: 60000 })
  if (config.preferredRouteKeyword) {
    await routeOption.click()
  } else {
    await routeSelect.press('ArrowDown')
    await routeSelect.press('Enter')
  }
}

async function submitCreateDialog(page, dialog, batchCode) {
  await selectRouteOption(page, dialog)
  await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '批次号' }).locator('input'), batchCode, 'batch code')
  const openResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/open-or-create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(dialog.getByRole('button', { name: '确 认' }), 'confirm create')
  const response = await openResponsePromise
  assert.equal(response.status(), 200, 'open-or-create HTTP status must be 200')
  return await response.json()
}

async function selectWorkOrderByIndex(page, dialog, optionIndex) {
  const workOrderSelect = dialog.locator('.el-select input[role="combobox"]').first()
  await workOrderSelect.click()
  const options = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
  await options.first().waitFor({ state: 'visible', timeout: 60000 })
  const count = await options.count()
  if (optionIndex >= count) {
    throw new Error(`测试租户可见未冻结工单不足，无法选择第 ${optionIndex + 1} 条工单。`)
  }
  await options.nth(optionIndex).click()
}

async function createBatchFromUi(page) {
  const dialog = await openCreateDialog(page)
  const preferredBatchCode = `${batchCodePrefix}-PREFERRED`
  let lastFailure = ''
  try {
    await selectPreferredWorkOrder(page, dialog)
    const body = await submitCreateDialog(page, dialog, preferredBatchCode)
    if (body.code === 0 || body.code === 200) {
      assert.ok(Number.isFinite(Number(body.data?.id)), 'open-or-create must return batch execution id')
      await page.waitForURL((url) => url.pathname === detailPath, { timeout: 60000 })
      await page.getByText(preferredBatchCode).first().waitFor({ state: 'visible', timeout: 60000 })
      return { batchExecutionId: Number(body.data.id), batchCode: preferredBatchCode, source: 'created' }
    }
    lastFailure = JSON.stringify(body)
    if (!isExpectedCreateSampleBlocker(body)) {
      throw new Error(`preferred open-or-create failed: ${JSON.stringify(body)}`)
    }
  } catch (error) {
    lastFailure = error instanceof Error ? error.message : String(error)
    if (!isExpectedCreateSampleBlocker(error instanceof Error ? error.message : String(error))) {
      throw error
    }
  }

  for (let optionIndex = 0; optionIndex < 10; optionIndex += 1) {
    await selectWorkOrderByIndex(page, dialog, optionIndex)
    const batchCode = `${batchCodePrefix}-${optionIndex + 1}`
    const body = await submitCreateDialog(page, dialog, batchCode)
    if (body.code === 0 || body.code === 200) {
      assert.ok(Number.isFinite(Number(body.data?.id)), 'open-or-create must return batch execution id')
      await page.waitForURL((url) => url.pathname === detailPath, { timeout: 60000 })
      await page.getByText(batchCode).first().waitFor({ state: 'visible', timeout: 60000 })
      return { batchExecutionId: Number(body.data.id), batchCode, source: 'created' }
    }
    lastFailure = JSON.stringify(body)
    if (!isExpectedCreateSampleBlocker(body)) {
      throw new Error(`open-or-create failed: ${lastFailure}`)
    }
  }
  await dialog.getByRole('button', { name: '取 消' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => undefined)
  return { createBlocked: `测试租户前 10 条未冻结工单均无法解析 eDHR 工艺路线，最后响应：${lastFailure}` }
}

async function selectSpecialNode(page, preferredNodeName = targetNodeName, options = {}) {
  const specialButtons = page.locator(
    '.edhr-batch-detail__special-process-task-group .edhr-batch-detail__process-task-group-head'
  )
  await specialButtons.first().waitFor({ state: 'visible', timeout: 30000 })
  const candidates = []
  const count = await specialButtons.count()
  for (let index = 0; index < count; index += 1) {
    const button = specialButtons.nth(index)
    const nodeName = (await button.locator('.edhr-batch-detail__review-process-name').innerText()).trim()
    const candidate = { index, nodeName }
    if (preferredNodeName && nodeName.includes(preferredNodeName)) {
      candidates.unshift(candidate)
    } else {
      candidates.push(candidate)
    }
  }
  let lastFailure = ''
  for (const candidate of candidates) {
    const button = specialButtons.nth(candidate.index)
    await button.click()
    await page.locator('.edhr-batch-detail__special-node-attachments').waitFor({ state: 'visible', timeout: 30000 })
    await page.getByText('待提交附件', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    if (options.requireUploadable) {
      const uploadButton = page
        .locator('.edhr-batch-detail__special-node-action-grid .edhr-batch-detail__rail-task-action')
        .filter({ hasText: '上传文件' })
        .first()
      await uploadButton.waitFor({ state: 'visible', timeout: 15000 }).catch(() => undefined)
      if (!(await uploadButton.isVisible().catch(() => false)) || (await uploadButton.isDisabled().catch(() => true))) {
        lastFailure = `${candidate.nodeName}: upload button is disabled or hidden`
        continue
      }
    }
    if (options.requireNoPending && (await countPendingRows(page)) > 0) {
      lastFailure = `${candidate.nodeName}: selected special node already has pending attachment rows`
      continue
    }
    return candidate.nodeName
  }
  throw new Error(`未找到可验证的特殊节点。${lastFailure}`)
}

async function openTargetSpecialNode(page, batchExecutionId, batchCode, preferredNodeName = targetNodeName, options = {}) {
  const url = new URL(detailPath, config.baseUrl)
  url.searchParams.set('id', String(batchExecutionId))
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText(batchCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return await selectSpecialNode(page, preferredNodeName, options)
}

async function countPendingRows(page) {
  const pendingSection = page.locator('section[aria-label="待提交附件"]').first()
  await pendingSection.waitFor({ state: 'visible', timeout: 30000 })
  return await pendingSection.locator('.edhr-batch-detail__special-node-file-row').count()
}

async function openExistingBatchCandidateFromUi(page, createBlocked) {
  let lastFailure = createBlocked || ''
  for (let rowIndex = 0; rowIndex < 10; rowIndex += 1) {
    await page.goto(`${config.baseUrl}${routePath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
    const rows = page.locator('.el-table__body-wrapper tbody tr').filter({
      has: page.getByRole('button', { name: '编辑' })
    })
    const rowCount = await rows.count()
    if (rowIndex >= rowCount) break
    const row = rows.nth(rowIndex)
    const rowText = await row.innerText()
    const batchCode = rowText.match(/EDHRB-\d+/)?.[0] || `EXISTING-ROW-${rowIndex + 1}`
    await row.getByRole('button', { name: '编辑' }).first().click()
    await page.waitForURL((url) => url.pathname === detailPath && Boolean(url.searchParams.get('id')), {
      timeout: 60000
    })
    const batchExecutionId = Number(new URL(page.url()).searchParams.get('id'))
    if (!Number.isFinite(batchExecutionId) || batchExecutionId <= 0) {
      lastFailure = `${batchCode}: detail URL did not expose a valid batch execution id`
      continue
    }
    try {
      const nodeName = await selectSpecialNode(page, targetNodeName, {
        requireUploadable: true,
        requireNoPending: true
      })
      return {
        batchExecutionId,
        batchCode,
        nodeName,
        source: 'existing',
        createBlocked
      }
    } catch (error) {
      lastFailure = `${batchCode}: ${error instanceof Error ? error.message : String(error)}`
    }
  }
  throw new Error(`测试租户无法创建新批次，也未找到可安全验证的既有特殊节点批次。最后阻塞：${lastFailure}`)
}

async function openAuthorizedBatchFromUi(page) {
  await page.goto(`${config.baseUrl}${routePath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批次执行编码').first().waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(page.getByPlaceholder('请输入批次执行编码'), config.targetBatchCode, 'batch execution code')
  await clickFirstEnabled(page.getByRole('button', { name: '查询' }), 'query batch execution')
  const batchButton = page
    .locator('.el-table__body-wrapper')
    .getByRole('button', { name: config.targetBatchCode, exact: true })
    .first()
  await batchButton.waitFor({ state: 'visible', timeout: 60000 })
  await batchButton.click()
  await page.waitForURL((url) => url.pathname === detailPath && Boolean(url.searchParams.get('id')), {
    timeout: 60000
  })
  const batchExecutionId = Number(new URL(page.url()).searchParams.get('id'))
  assert.ok(Number.isFinite(batchExecutionId) && batchExecutionId > 0, 'authorized batch detail URL must expose id')
  return {
    batchExecutionId,
    batchCode: config.targetBatchCode,
    source: 'authorized-yudao-admin'
  }
}

async function uploadFileThroughUi(page, fileName, text) {
  const prepareResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/task/special-node/attachment/prepare-upload') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const chooserPromise = page.waitForEvent('filechooser', { timeout: 30000 })
  await page
    .locator('.edhr-batch-detail__special-node-action-grid .edhr-batch-detail__rail-task-action')
    .filter({ hasText: '上传文件' })
    .first()
    .click()
  const chooser = await chooserPromise
  await chooser.setFiles({
    name: fileName,
    mimeType: 'text/plain',
    buffer: Buffer.from(text, 'utf8')
  })
  const response = await prepareResponsePromise
  assert.equal(response.status(), 200, 'prepare-upload HTTP status must be 200')
  const body = await response.json()
  assert.ok(body.code === 0 || body.code === 200, `prepare-upload business failed: ${JSON.stringify(body)}`)
  return body.data?.data || body.data
}

async function assertPendingRow(page, fileName, expectedCount) {
  const pendingSection = page.locator('section[aria-label="待提交附件"]').first()
  await pendingSection.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForFunction(
    ({ selector, count, name }) =>
      Array.from(document.querySelectorAll(selector)).filter((row) => row.textContent?.includes(name)).length === count,
    {
      selector: 'section[aria-label="待提交附件"] .edhr-batch-detail__special-node-file-row',
      count: expectedCount,
      name: fileName
    },
    { timeout: 30000 }
  )
  const rows = pendingSection.locator('.edhr-batch-detail__special-node-file-row').filter({ hasText: fileName })
  assert.equal(await rows.count(), expectedCount, `pending file row count for ${fileName} must be ${expectedCount}`)
  if (expectedCount > 0) {
    await pendingSection.getByText(fileName, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    await rows.first().getByText('待提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  }
}

async function previewPendingAttachment(page, fileName) {
  const pendingSection = page.locator('section[aria-label="待提交附件"]').first()
  const row = pendingSection.locator('.edhr-batch-detail__special-node-file-row').filter({ hasText: fileName }).first()
  await row.getByRole('button', { name: '预览' }).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: fileName }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('受控预览', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('replacement pending upload', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => undefined)
  return 'inline-protected-preview-dialog'
}

async function deletePendingRowsByPrefix(page, prefix) {
  const pendingSection = page.locator('section[aria-label="待提交附件"]').first()
  await pendingSection.waitFor({ state: 'visible', timeout: 30000 })
  for (let index = 0; index < 20; index += 1) {
    const row = pendingSection.locator('.edhr-batch-detail__special-node-file-row').filter({ hasText: prefix }).first()
    if (!(await row.isVisible().catch(() => false))) return
    const fileName = (await row.locator('.edhr-batch-detail__special-node-file-name').innerText()).trim()
    await row.getByRole('button', { name: '删除' }).first().click()
    await page.waitForFunction(
      ({ selector, name }) =>
        !Array.from(document.querySelectorAll(selector)).some((item) => item.textContent?.includes(name)),
      {
        selector: 'section[aria-label="待提交附件"] .edhr-batch-detail__special-node-file-row',
        name: fileName
      },
      { timeout: 30000 }
    )
  }
  throw new Error(`仍存在超过 20 条 ${prefix} 待提交测试附件，已停止清理。`)
}

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  const unwrap = (raw) => {
    if (!raw) return ''
    let current = raw
    for (let index = 0; index < 6; index += 1) {
      try {
        current = JSON.parse(current)
      } catch {
        break
      }
      if (current && typeof current === 'object') {
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
          current = current.accessToken
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'v')) {
          current = current.v
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'value')) {
          current = current.value
          continue
        }
      }
      if (typeof current !== 'string') break
    }
    return String(current || '').replace(/^"|"$/g, '')
  }
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function apiGetDetail(page, batchExecutionId) {
  const auth = await browserAuth(page)
  assert.ok(auth.token, 'final API verification requires browser token')
  assert.ok(auth.tenantId, 'final API verification requires tenant-id')
  const response = await page.request.get(`${config.backendUrl}/admin-api/mes/pro/edhr-batch-execution/get`, {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'tenant-id': String(auth.tenantId),
      ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
    },
    params: { id: batchExecutionId }
  })
  assert.equal(response.status(), 200, 'batch detail API HTTP status must be 200')
  const body = await response.json()
  assert.ok(body.code === 0 || body.code === 200, `batch detail business failed: ${JSON.stringify(body)}`)
  return body.data
}

function findTaskWithPending(detail, fileName) {
  return (detail?.tasks || []).find((task) =>
    (task.pendingSpecialNodeAttachments || []).some((attachment) => attachment.fileName === fileName)
  )
}

async function main() {
  ensureDir(artifactDir)
  assertWriteScope()
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const mesWriteRequests = []
  page.on('request', (request) => {
    const url = request.url()
    const method = request.method().toUpperCase()
    if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS' && url.includes('/admin-api/mes/')) {
      mesWriteRequests.push({ method, url: url.replace(/([?&](?:password|token|secret|key)=)[^&]+/gi, '$1[REDACTED]') })
    }
  })

  let target
  let resolvedNodeName = ''
  let cleanupError = ''
  try {
    await login(page)
    const created = config.authorizedYudaoAdminWrites ? await openAuthorizedBatchFromUi(page) : await createBatchFromUi(page)
    target = created.batchExecutionId
      ? created
      : await openExistingBatchCandidateFromUi(page, created.createBlocked)
    resolvedNodeName = await openTargetSpecialNode(
      page,
      target.batchExecutionId,
      target.batchCode,
      target.nodeName || targetNodeName,
      { requireUploadable: true }
    )
    await deletePendingRowsByPrefix(page, testFileNamePrefix)
    const uploadButton = page
      .locator('.edhr-batch-detail__special-node-action-grid .edhr-batch-detail__rail-task-action')
      .filter({ hasText: '上传文件' })
      .first()
    await uploadButton.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(await uploadButton.isDisabled(), false, 'upload button must be enabled before release')

    const fileName = `${testFileNamePrefix}${Date.now()}.txt`
    const first = await uploadFileThroughUi(page, fileName, `first pending upload ${new Date().toISOString()}`)
    await assertPendingRow(page, fileName, 1)

    await openTargetSpecialNode(page, target.batchExecutionId, target.batchCode, resolvedNodeName)
    await assertPendingRow(page, fileName, 1)

    const second = await uploadFileThroughUi(page, fileName, `replacement pending upload ${new Date().toISOString()}`)
    await assertPendingRow(page, fileName, 1)
    assert.notEqual(first.uploadToken, second.uploadToken, 'same-name upload must replace pending attachment')

    await openTargetSpecialNode(page, target.batchExecutionId, target.batchCode, resolvedNodeName)
    await assertPendingRow(page, fileName, 1)
    const previewUrl = await previewPendingAttachment(page, fileName)

    const detailWithPending = await apiGetDetail(page, target.batchExecutionId)
    const pendingTask = findTaskWithPending(detailWithPending, fileName)
    assert.ok(pendingTask, 'final API detail must expose persisted pending attachment')
    const pendingRows = pendingTask.pendingSpecialNodeAttachments.filter((attachment) => attachment.fileName === fileName)
    assert.equal(pendingRows.length, 1, 'final API detail must keep only latest same-name pending attachment')
    assert.equal(pendingRows[0].uploadToken, second.uploadToken, 'final API detail must expose latest pending attachment')

    await page.screenshot({ path: screenshotFile, fullPage: true })
    const pendingSection = page.locator('section[aria-label="待提交附件"]').first()
    await pendingSection
      .locator('.edhr-batch-detail__special-node-file-row')
      .filter({ hasText: fileName })
      .first()
      .getByRole('button', { name: '删除' })
      .first()
      .click()
    await assertPendingRow(page, fileName, 0)
    await openTargetSpecialNode(page, target.batchExecutionId, target.batchCode, resolvedNodeName)
    await assertPendingRow(page, fileName, 0)
    const detailAfterDelete = await apiGetDetail(page, target.batchExecutionId)
    assert.equal(Boolean(findTaskWithPending(detailAfterDelete, fileName)), false, 'deleted pending attachment must not reload')

    const evidence = {
      tenant: config.tenant,
      username: config.username,
      batchExecutionId: target.batchExecutionId,
      batchCode: target.batchCode,
      batchSource: target.source,
      createBlocked: target.createBlocked,
      targetNodeName: resolvedNodeName,
      fileName,
      firstUploadToken: first.uploadToken,
      secondUploadToken: second.uploadToken,
      previewUrl,
      pendingTaskId: pendingTask.id,
      mesWriteRequests,
      screenshotFile
    }
    writeResult('PASS', evidence)
    await context.close()
    await browser.close()
    console.log(`PASS: eDHR pending attachment reload E2E batch=${target.batchCode} task=${pendingTask.id}`)
  } catch (error) {
    if (target?.batchExecutionId && resolvedNodeName) {
      try {
        await openTargetSpecialNode(page, target.batchExecutionId, target.batchCode, resolvedNodeName)
        await deletePendingRowsByPrefix(page, testFileNamePrefix)
      } catch (cleanupFailure) {
        cleanupError = cleanupFailure instanceof Error ? cleanupFailure.message : String(cleanupFailure)
      }
    }
    await page.screenshot({ path: failureScreenshotFile, fullPage: true }).catch(() => undefined)
    writeResult('FAIL', {
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
      cleanupError: cleanupError || undefined,
      mesWriteRequests,
      failureScreenshotFile: fs.existsSync(failureScreenshotFile) ? failureScreenshotFile : undefined
    })
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
    throw error
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
