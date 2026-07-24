const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const TASK_ID = '20260611-edhr-work-task-flow-design'
const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const APPROVAL_ROUTE = '/mes/pro/feedback/edhr-approval'
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const BATCH_DETAIL_ROUTE = '/mes/pro/feedback/edhr-batch-execution/detail'
const EXECUTION_DETAIL_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-rejection-revision')
const DEFAULT_EVIDENCE_FILE = path.resolve(
  process.cwd(),
  '..',
  'doc',
  'tasks',
  TASK_ID,
  'rejection-revision-real-e2e-evidence.md'
)
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])

const COMMON_REQUIRED_ENV = [
  ['EDHR_REJECTION_REVISION_E2E_FILLER_USERNAME', '原填写人账号'],
  ['EDHR_REJECTION_REVISION_E2E_FILLER_PASSWORD', '原填写人登录密码'],
  ['EDHR_REJECTION_REVISION_E2E_FILLER_SIGN_PASSWORD', '原填写人电子签名密码'],
  ['EDHR_REJECTION_REVISION_E2E_REVIEWER_USERNAME', '审批人账号'],
  ['EDHR_REJECTION_REVISION_E2E_REVIEWER_PASSWORD', '审批人登录密码'],
  ['EDHR_REJECTION_REVISION_E2E_REVIEWER_SIGN_PASSWORD', '审批人电子签名密码']
]

const BDD_SCENARIOS = [
  'BDD: 审批驳回锁定原版本 -> Given 测试租户存在已提交待审批 eDHR 执行记录, When 审批人从真实审批页输入签名密码和驳回原因, Then 后端返回 revisionExecutionId 和 reworkTaskId，原记录保持 REJECTED。',
  'BDD: 返工待办进入修订草稿 -> Given 驳回事务已创建 REWORK/TODO 工作任务, When 原填写人从真实任务看板点击处理, Then 页面进入新修订草稿且 URL 携带 workTaskId，不进入被驳回原版本。',
  'BDD: 既有返工待办继续验证 -> Given 测试租户已经存在真实 REWORK/TODO 工作任务, When 原填写人从任务看板打开该待办, Then E2E 从真实修订草稿继续重新提交、审批通过和批次恢复校验。',
  'BDD: 返工修订重新提交审批 -> Given 原填写人已进入新修订草稿, When 原填写人保存字段审计并重新提交, Then 新修订记录进入 SUBMITTED 且形成新的审批轮次。',
  'BDD: 修订审批通过恢复批次流转 -> Given 新修订记录已重新提交, When 审批人通过修订审批, Then 批次不再保持 REWORK_REQUIRED，并进入下一工序 IN_PROGRESS、READY_TO_CLOSE、CLOSED 或 ARCHIVED。',
  'BDD: 缺少真实前置即阻塞 -> Given 缺少真实账号、签名密码、待审执行编号或测试租户权限, When 执行 E2E, Then 脚本写入 BLOCKED 证据并退出非零，不使用 mock、默认密码或 API 绕过。'
]

function envValue(key) {
  return (process.env[key] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function resolveEvidenceFile() {
  return process.env.EDHR_REJECTION_REVISION_E2E_EVIDENCE_FILE
    ? path.resolve(process.env.EDHR_REJECTION_REVISION_E2E_EVIDENCE_FILE)
    : DEFAULT_EVIDENCE_FILE
}

function collectConfig() {
  const executionCode = envValue('EDHR_REJECTION_REVISION_E2E_EXECUTION_CODE')
  const batchCode = envValue('EDHR_REJECTION_REVISION_E2E_BATCH_CODE')
  const fillWorkTaskIdText = envValue('EDHR_REJECTION_REVISION_E2E_FILL_WORK_TASK_ID')
  const reworkTaskIdText = envValue('EDHR_REJECTION_REVISION_E2E_REWORK_TASK_ID')
  const fillWorkTaskId = fillWorkTaskIdText ? Number(fillWorkTaskIdText) : undefined
  const reworkTaskId = reworkTaskIdText ? Number(reworkTaskIdText) : undefined
  const hasSubmittedInput = Boolean(executionCode || batchCode) && !fillWorkTaskIdText && !reworkTaskIdText
  const hasSetupInput = Boolean(fillWorkTaskIdText || batchCode) && !executionCode && !reworkTaskIdText
  const hasExistingReworkInput = Boolean(reworkTaskIdText || batchCode) && !executionCode && !fillWorkTaskIdText
  const config = {
    baseUrl: envValue('EDHR_REJECTION_REVISION_E2E_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_REJECTION_REVISION_E2E_TENANT') || DEFAULT_TENANT,
    executionCode,
    batchCode,
    fillWorkTaskId,
    reworkTaskId,
    mode: hasExistingReworkInput ? 'existing-rework-task' : hasSetupInput ? 'setup-from-fill-task' : 'existing-submitted',
    rejectReason:
      envValue('EDHR_REJECTION_REVISION_E2E_REJECT_REASON') ||
      `受控修订 E2E 驳回原因 ${Date.now()}`,
    filler: {
      username: envValue('EDHR_REJECTION_REVISION_E2E_FILLER_USERNAME'),
      password: envValue('EDHR_REJECTION_REVISION_E2E_FILLER_PASSWORD'),
      signaturePassword: envValue('EDHR_REJECTION_REVISION_E2E_FILLER_SIGN_PASSWORD')
    },
    reviewer: {
      username: envValue('EDHR_REJECTION_REVISION_E2E_REVIEWER_USERNAME'),
      password: envValue('EDHR_REJECTION_REVISION_E2E_REVIEWER_PASSWORD'),
      signaturePassword: envValue('EDHR_REJECTION_REVISION_E2E_REVIEWER_SIGN_PASSWORD')
    },
    finalReviewer: {
      username: envValue('EDHR_REJECTION_REVISION_E2E_FINAL_REVIEWER_USERNAME'),
      password: envValue('EDHR_REJECTION_REVISION_E2E_FINAL_REVIEWER_PASSWORD'),
      signaturePassword: envValue('EDHR_REJECTION_REVISION_E2E_FINAL_REVIEWER_SIGN_PASSWORD')
    },
    evidenceFile: resolveEvidenceFile(),
    headed: envValue('EDHR_REJECTION_REVISION_E2E_HEADED') === '1'
  }

  const missing = COMMON_REQUIRED_ENV.filter(([name]) => !envValue(name)).map(([name, description]) => ({
    name,
    description
  }))

  const inputModeCount = [Boolean(executionCode), Boolean(fillWorkTaskIdText), Boolean(reworkTaskIdText)].filter(Boolean).length
  if (inputModeCount > 1) {
    missing.push({
      name: 'EDHR_REJECTION_REVISION_E2E_EXECUTION_CODE / EDHR_REJECTION_REVISION_E2E_FILL_WORK_TASK_ID / EDHR_REJECTION_REVISION_E2E_REWORK_TASK_ID',
      description: '直接使用待审执行编号、从填写待办准备待审记录、从既有返工待办继续三种模式互斥。'
    })
  } else if (hasSubmittedInput) {
    if (!executionCode) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_EXECUTION_CODE',
        description: 'existing-submitted 模式必须提供已提交待审批的真实 eDHR 执行编号。'
      })
    }
    if (!batchCode) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_BATCH_CODE',
        description: 'existing-submitted 模式必须提供目标执行记录所在真实批次号。'
      })
    }
  } else if (hasSetupInput) {
    config.mode = 'setup-from-fill-task'
    if (!batchCode) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_BATCH_CODE',
        description: 'setup-from-fill-task 模式必须提供真实填写待办所在批次号。'
      })
    }
    if (!fillWorkTaskIdText) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_FILL_WORK_TASK_ID',
        description: 'setup-from-fill-task 模式必须提供测试租户内真实 FILL/TODO 工作任务 ID。'
      })
    } else if (!Number.isFinite(fillWorkTaskId) || fillWorkTaskId <= 0) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_FILL_WORK_TASK_ID',
        description: '填写待办工作任务 ID 必须是正整数。'
      })
    }
  } else if (hasExistingReworkInput) {
    config.mode = 'existing-rework-task'
    if (!batchCode) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_BATCH_CODE',
        description: 'existing-rework-task 模式必须提供真实返工待办所在批次号。'
      })
    }
    if (!reworkTaskIdText) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_REWORK_TASK_ID',
        description: 'existing-rework-task 模式必须提供测试租户内真实 REWORK/TODO 工作任务 ID。'
      })
    } else if (!Number.isFinite(reworkTaskId) || reworkTaskId <= 0) {
      missing.push({
        name: 'EDHR_REJECTION_REVISION_E2E_REWORK_TASK_ID',
        description: '返工工作任务 ID 必须是正整数。'
      })
    }
  } else {
    missing.push({
      name: 'EDHR_REJECTION_REVISION_E2E_EXECUTION_CODE + EDHR_REJECTION_REVISION_E2E_BATCH_CODE | EDHR_REJECTION_REVISION_E2E_FILL_WORK_TASK_ID + EDHR_REJECTION_REVISION_E2E_BATCH_CODE | EDHR_REJECTION_REVISION_E2E_REWORK_TASK_ID + EDHR_REJECTION_REVISION_E2E_BATCH_CODE',
      description: '必须选择一种真实 E2E 输入：已提交待审批执行记录、可通过真实 UI 打开并提交的 FILL/TODO 填写待办，或既有 REWORK/TODO 返工待办。'
    })
  }

  if (config.baseUrl !== REQUIRED_BASE_URL) {
    missing.push({
      name: 'EDHR_REJECTION_REVISION_E2E_BASE_URL',
      description: `真实 E2E 前端入口必须固定为 ${REQUIRED_BASE_URL}`
    })
  }
  const normalizedTenant = config.tenant.toLowerCase()
  if (config.tenant !== DEFAULT_TENANT || FORBIDDEN_TENANTS.has(normalizedTenant)) {
    missing.push({
      name: 'EDHR_REJECTION_REVISION_E2E_TENANT',
      description: `真实 E2E 只能使用 ${DEFAULT_TENANT}，不得写入 live 租户`
    })
  }

  const finalReviewerFields = [
    ['EDHR_REJECTION_REVISION_E2E_FINAL_REVIEWER_USERNAME', config.finalReviewer.username],
    ['EDHR_REJECTION_REVISION_E2E_FINAL_REVIEWER_PASSWORD', config.finalReviewer.password],
    ['EDHR_REJECTION_REVISION_E2E_FINAL_REVIEWER_SIGN_PASSWORD', config.finalReviewer.signaturePassword]
  ]
  const providedFinalReviewerCount = finalReviewerFields.filter(([, value]) => Boolean(value)).length
  if (providedFinalReviewerCount > 0 && providedFinalReviewerCount < finalReviewerFields.length) {
    for (const [name, value] of finalReviewerFields) {
      if (!value) {
        missing.push({
          name,
          description: '多人审核闭环提供第二审核人时，账号、登录密码、签名密码必须同时提供。'
        })
      }
    }
  }

  return { ...config, missing }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('缺少 Playwright runtime，请先在 yudao-ui-admin-vue3 执行 pnpm install。')
  }
}

function serializeError(error) {
  return {
    name: error?.name || 'Error',
    message: error?.message || String(error),
    stack: error?.stack,
    apiBody: error?.apiBody
  }
}

function writeEvidence(result, config) {
  ensureDir(path.dirname(config.evidenceFile))
  const lines = [
    '# eDHR 驳回受控修订真实 E2E 证据',
    '',
    `- 更新时间：${new Date().toISOString()}`,
    `- 状态：${result.status}`,
    `- 前端入口：\`${config.baseUrl || REQUIRED_BASE_URL}\``,
    `- 租户：\`${config.tenant || DEFAULT_TENANT}\``,
    `- 输入模式：\`${config.mode || 'unresolved'}\``,
    `- 填写待办ID：\`${config.fillWorkTaskId || '--'}\``,
    `- 返工待办ID：\`${config.reworkTaskId || '--'}\``,
    `- 执行编号：\`${result.executionCode || config.executionCode || '--'}\``,
    `- 批次号：\`${config.batchCode || '--'}\``,
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- ${scenario}`),
    '',
    '## Result',
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push(`- BLOCKED: \`node tests/e2e/edhr-rejection-revision-real-flow.e2e.js\` -> FAIL，${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- 缺失前置：\`${item.name}\`，${item.description}`)
    }
    lines.push('- 影响：无法在真实前端页面完成审批驳回、返工任务打开和修订草稿验证；未使用 mock、默认密码或 API-only 路径。')
  } else if (result.status === 'PASS') {
    lines.push('- GREEN: 真实前端路径已完成审批驳回、返工修订草稿打开、重新提交、审批通过和批次状态恢复校验。')
    lines.push(`- 原执行编号：\`${result.executionCode || config.executionCode}\``)
    lines.push(`- 原执行ID：\`${result.executionId || '--'}\``)
    lines.push(`- 新修订执行ID：\`${result.revisionExecutionId}\``)
    lines.push(`- 返工任务ID：\`${result.reworkTaskId}\``)
    lines.push(`- 批次恢复状态：\`${result.batchStatusAfterApproval || '--'}\``)
    lines.push(`- 修订审批状态：\`${result.revisionApprovalStatus || '--'}\``)
    if (result.fieldAuditBatchId) {
      lines.push(`- 字段审计批次：\`${result.fieldAuditBatchId}\``)
    }
  } else {
    lines.push(`- RED: 真实前端路径失败，${result.error?.message || '未知错误'}`)
  }

  fs.writeFileSync(config.evidenceFile, `${lines.join('\n')}\n`, 'utf8')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`页面缺少可见输入框：${label}`)
}

async function clickButton(root, name) {
  const deadline = Date.now() + 30000
  let sawDisabled = false
  while (Date.now() < deadline) {
    const buttons = root.getByRole('button', { name })
    const count = await buttons.count()
    for (let index = 0; index < count; index += 1) {
      const button = buttons.nth(index)
      if (!(await button.isVisible().catch(() => false))) continue
      await button.scrollIntoViewIfNeeded().catch(() => {})
      if (await button.isDisabled().catch(() => true)) {
        sawDisabled = true
        continue
      }
      await button.click()
      return
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  if (sawDisabled) throw new Error(`按钮不可用：${String(name)}`)
  throw new Error(`页面缺少可见按钮：${String(name)}`)
}

async function fillFormItem(root, label, value) {
  const item = root.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(item.locator('input, textarea'), value, label)
}

async function login(page, config, account, redirectPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) {
    return
  }
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，真实 E2E 无法无人值守执行。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await page.keyboard.press('Enter')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), account.username, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), account.password, '密码')
  await clickButton(loginForm, /^登录$/)
  await page.waitForURL((url) => !url.href.includes('/login'), {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
}

async function readApiData(response, label) {
  assert.equal(response.status(), 200, `${label} 必须返回 HTTP 200，实际 ${response.status()}`)
  const body = await response.json()
  assertApiSuccess(body, label)
  assert.ok(body.data && typeof body.data === 'object', `${label} 必须返回 data 对象`)
  return body.data
}

async function readApiEnvelope(response, label) {
  assert.equal(response.status(), 200, `${label} 必须返回 HTTP 200，实际 ${response.status()}`)
  const body = await response.json()
  assertApiSuccess(body, label)
  return body
}

async function readApiBoolean(response, label) {
  assert.equal(response.status(), 200, `${label} 必须返回 HTTP 200，实际 ${response.status()}`)
  const body = await response.json()
  assertApiSuccess(body, label)
  assert.equal(body.data, true, `${label} 必须返回 true：${describeApiBody(body)}`)
  return true
}

function assertApiSuccess(body, label) {
  if (body?.code === 0) return
  const error = new Error(`${label} 业务响应必须成功：${describeApiBody(body)}`)
  error.apiBody = body
  throw error
}

function describeApiBody(body) {
  const text = JSON.stringify({
    code: body?.code,
    msg: body?.msg,
    data: body?.data
  })
  return text.length > 1200 ? `${text.slice(0, 1200)}...` : text
}

async function waitForText(root, textOrPattern, label) {
  try {
    await root.getByText(textOrPattern).first().waitFor({ state: 'visible', timeout: 60000 })
  } catch (error) {
    throw new Error(`${label}: ${error.message}`)
  }
}

async function chooseEditableField(page) {
  await page.waitForSelector('.edhr-page-shell__form', { timeout: 30000 })
  const controls = page.locator(
    '.edhr-page-shell__form input:not([disabled]):visible, .edhr-page-shell__form textarea:not([disabled]):visible'
  )
  const count = await controls.count()
  assert.ok(count > 0, 'eDHR 执行页没有可编辑字段控件，无法产生 FIELD_CHANGE。')

  for (let index = 0; index < count; index += 1) {
    const control = controls.nth(index)
    const meta = await control.evaluate((element) => ({
      tag: element.tagName,
      type: element.getAttribute('type') || '',
      readOnly: element.readOnly,
      value: element.value || '',
      inDate: Boolean(element.closest('.el-date-editor')),
      inSelect: Boolean(element.closest('.el-select')),
      inNumber: Boolean(element.closest('.el-input-number'))
    }))
    if (meta.readOnly || meta.inDate || meta.inSelect) continue
    const newValue = meta.inNumber ? String((Date.now() % 9000) + 1000) : `RR-E2E-${Date.now()}-${index}`
    if (newValue === meta.value) continue
    await control.fill(newValue)
    await control.blur()
    return { index, ...meta, newValue }
  }

  throw new Error('eDHR 执行页没有可直接填写并审计的字段。')
}

async function saveFieldAuditByUi(page, config) {
  await waitForText(page, /草稿（可编辑）|草稿/, '准备提交的执行记录不是草稿状态')
  const editedField = await chooseEditableField(page)
  const reasonForm = page.locator('.edhr-page-shell__field-audit-reason').first()
  await reasonForm.waitFor({ state: 'visible', timeout: 30000 })
  await reasonForm.locator('.el-select').click()
  await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: '纠正录入' })
    .first()
    .click()
  await fillFirstVisible(
    reasonForm.getByPlaceholder('请输入字段变更原因'),
    `驳回修订真实E2E字段变更 ${new Date().toISOString()}`,
    '字段审计原因说明'
  )
  const pendingRows = await page
    .locator('.edhr-page-shell__field-audit-table .el-table__body-wrapper tbody tr')
    .count()
  assert.ok(pendingRows > 0, '修改字段后没有生成待保存 FIELD_CHANGE 行。')

  const fieldAuditPanel = page.locator('.edhr-page-shell__field-audit').first()
  await clickButton(fieldAuditPanel, /^保存变更$/)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.filler.signaturePassword, '字段审计签名密码')
  const saveResponse = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/mes/pro/batch-record-execution/field-audit/save-changes'),
    { timeout: 60000 }
  )
  await clickButton(dialog, /确\s*认\s*保\s*存/)
  const data = await readApiData(await saveResponse, 'FIELD_CHANGE save-changes')
  assert.equal(data.hashVerification?.status, 'VALID', 'FIELD_CHANGE 保存后 hashVerification.status 必须为 VALID')
  await waitForText(page, '字段变更已写入不可篡改审计链', '字段审计保存后页面未展示成功提示')
  return {
    editedField,
    fieldAuditBatchId: data.auditBatchId
  }
}

async function submitExecutionByUi(page, config) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/mes/pro/batch-record-execution/submit'),
    { timeout: 60000 }
  )
  await clickButton(page, /提交执行/)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '提交 eDHR 执行' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.filler.signaturePassword, '提交签名密码')
  await fillFirstVisible(
    dialog.locator('textarea'),
    `驳回修订真实E2E提交 ${new Date().toISOString()}`,
    '提交备注'
  )
  await clickButton(dialog, /确\s*认\s*提\s*交/)
  await readApiBoolean(await responsePromise, '提交执行')
  await waitForText(page, /待审批（审批关闭后才可归档）|待审批/, '提交后页面未展示待审批状态')
}

async function openFillTaskAndSubmit(page, config) {
  await page.goto(`${config.baseUrl}${WORK_TASK_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const board = page.locator('.edhr-work-task-page').first()
  await board.waitFor({ state: 'visible', timeout: 60000 })
  await waitForText(board, '我的待办', '工作任务看板未展示我的待办页签')
  await fillFormItem(board, '批次', config.batchCode)
  const taskPageResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-work-task/my-page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(board, '查询')
  const taskPageData = await readApiData(await taskPageResponse, '工作任务待办分页')
  const targetTask = (taskPageData.list || []).find((task) => Number(task.id) === Number(config.fillWorkTaskId))
  assert.ok(targetTask, `工作任务看板未返回填写待办 ID=${config.fillWorkTaskId}`)
  assert.equal(targetTask.taskType, 'FILL', 'setup 工作任务必须是 FILL 类型')
  assert.equal(targetTask.status, 'TODO', 'setup 工作任务必须是 TODO 状态')

  let row = board.locator('.el-table__body-wrapper tr').filter({ hasText: targetTask.batchCode || config.batchCode })
  if (targetTask.processName || targetTask.taskCode) {
    row = row.filter({ hasText: targetTask.processName || targetTask.taskCode })
  } else {
    row = row.filter({ hasText: '填写' })
  }
  row = row.first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickButton(row, '处理')
  let openedExecutionId = targetTask.executionId ? Number(targetTask.executionId) : undefined
  if (openedExecutionId) {
    await page.waitForURL(
      (url) =>
        url.pathname === EXECUTION_DETAIL_ROUTE &&
        url.searchParams.get('id') === String(openedExecutionId) &&
        url.searchParams.get('workTaskId') === String(config.fillWorkTaskId),
      { timeout: 60000 }
    )
  } else {
    await page.waitForURL(
      (url) =>
        url.pathname === BATCH_DETAIL_ROUTE &&
        url.searchParams.get('workTaskId') === String(config.fillWorkTaskId),
      { timeout: 60000 }
    )
    await page.locator('.edhr-batch-detail__table').first().waitFor({ state: 'visible', timeout: 60000 })
    const openResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/mes/pro/edhr-batch-execution/task/open'),
      { timeout: 60000 }
    )
    let batchTaskRow = page.locator('.edhr-batch-detail__table .el-table__body-wrapper tr')
    if (targetTask.processName) {
      batchTaskRow = batchTaskRow.filter({ hasText: targetTask.processName })
    }
    batchTaskRow = batchTaskRow.first()
    await batchTaskRow.waitFor({ state: 'visible', timeout: 60000 })
    await clickButton(batchTaskRow, /打开填写|打开返工/)
    const opened = await readApiData(await openResponse, '打开批次工序任务')
    assert.ok(opened.executionId, '打开批次工序任务后端未返回 executionId')
    openedExecutionId = Number(opened.executionId)
    await page.waitForURL(
      (url) =>
        url.pathname === EXECUTION_DETAIL_ROUTE &&
        url.searchParams.get('id') === String(openedExecutionId) &&
        url.searchParams.get('workTaskId') === String(config.fillWorkTaskId),
      { timeout: 60000 }
    )
  }
  const summary = page.locator('.edhr-page-shell__summary').first()
  await summary.waitFor({ state: 'visible', timeout: 60000 })
  const summaryText = (await summary.textContent()) || ''
  const executionCodeMatch = summaryText.match(/BRE\d+/)
  assert.ok(executionCodeMatch, '执行详情页未展示业务执行编号，无法继续审批查询。')

  const fieldAudit = await saveFieldAuditByUi(page, config)
  await submitExecutionByUi(page, config)
  return {
    executionId: openedExecutionId,
    executionCode: executionCodeMatch[0],
    batchCode: targetTask.batchCode || config.batchCode,
    fieldAuditBatchId: fieldAudit.fieldAuditBatchId
  }
}

async function rejectExecution(page, config) {
  await page.goto(`${config.baseUrl}${APPROVAL_ROUTE}?tab=pending`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('待我审批').first().waitFor({ state: 'visible', timeout: 60000 })
  await fillFormItem(page, '执行编号', config.executionCode)
  const pageResponse = page.waitForResponse(
    (response) => response.url().includes('/approval-pending-page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(page, '查询')
  const pageResult = await pageResponse
  assert.equal(pageResult.status(), 200, '审批待办分页必须返回 HTTP 200')
  const row = page.locator('.el-table__body-wrapper tr').filter({ hasText: config.executionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickButton(row, '驳回')

  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.reviewer.signaturePassword, '审批签名密码')
  await fillFormItem(dialog, '驳回原因', config.rejectReason)
  const rejectResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/batch-record-execution/reject') && response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickButton(dialog, '确 认')
  const result = await rejectResponse
  assert.equal(result.status(), 200, '审批驳回接口必须返回 HTTP 200')
  const body = await result.json()
  assertApiSuccess(body, '审批驳回')
  assert.ok(body.data?.revisionExecutionId, '审批驳回必须返回 revisionExecutionId')
  assert.ok(body.data?.reworkTaskId, '审批驳回必须返回 reworkTaskId')
  await page.getByText('已驳回并创建返工任务').first().waitFor({ state: 'visible', timeout: 30000 })
  return {
    revisionExecutionId: Number(body.data.revisionExecutionId),
    reworkTaskId: Number(body.data.reworkTaskId)
  }
}

async function approveExecution(page, config, executionCode) {
  await page.goto(`${config.baseUrl}${APPROVAL_ROUTE}?tab=pending`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('待我审批').first().waitFor({ state: 'visible', timeout: 60000 })
  await fillFormItem(page, '执行编号', executionCode)
  const pageResponse = page.waitForResponse(
    (response) => response.url().includes('/approval-pending-page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(page, '查询')
  await readApiData(await pageResponse, '修订审批待办分页')
  const row = page.locator('.el-table__body-wrapper tr').filter({ hasText: executionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickButton(row, '通过')

  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(dialog.locator('input[type="password"]'), config.reviewer.signaturePassword, '审批通过签名密码')
  const commentInputs = dialog.locator('textarea')
  if ((await commentInputs.count()) > 0) {
    await fillFirstVisible(commentInputs, `驳回修订真实E2E通过 ${new Date().toISOString()}`, '审批意见')
  }
  const approveResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/batch-record-execution/approve') && response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await clickButton(dialog, '确 认')
  const body = await readApiEnvelope(await approveResponse, '修订审批通过')
  assert.ok([1, 3].includes(body.data?.status), `修订审批通过后必须保持 SUBMITTED=1 或进入 APPROVED=3，实际：${describeApiBody(body)}`)
  if (body.data?.status === 3) {
    await page.getByText('eDHR 已审批关闭').first().waitFor({ state: 'visible', timeout: 30000 })
  }
  return body.data
}

async function openReworkTask(page, config, rejectResult) {
  await page.goto(`${config.baseUrl}${WORK_TASK_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const board = page.locator('.edhr-work-task-page').first()
  await board.waitFor({ state: 'visible', timeout: 60000 })
  await board.getByText('驳回待改').first().waitFor({ state: 'visible', timeout: 60000 })
  await fillFormItem(board, '批次', config.batchCode)
  const taskPageResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-work-task/my-page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(board, '查询')
  const taskPageResult = await taskPageResponse
  assert.equal(taskPageResult.status(), 200, '工作任务待办分页必须返回 HTTP 200')
  const row = board
    .locator('.el-table__body-wrapper tr')
    .filter({ hasText: config.batchCode })
    .filter({ hasText: 'REWORK' })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickButton(row, '处理')
  await page.waitForURL(
    (url) =>
      url.pathname === '/mes/pro/feedback/edhr-execution/detail' &&
      url.searchParams.get('id') === String(rejectResult.revisionExecutionId) &&
      url.searchParams.get('workTaskId') === String(rejectResult.reworkTaskId),
    { timeout: 60000 }
  )
  await page.getByText('受控修订草稿').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('来源驳回版本').first().waitFor({ state: 'visible', timeout: 30000 })
  await waitForText(page, /草稿（可编辑）|草稿/, '返工修订页面未展示 DRAFT 草稿状态')
  const summaryText = (await page.locator('.edhr-page-shell__summary').first().textContent()) || ''
  const executionCodeMatch = summaryText.match(/BRE\d+/)
  assert.ok(executionCodeMatch, '返工修订草稿未展示新的执行编号。')
  return {
    revisionExecutionCode: executionCodeMatch[0]
  }
}

async function openExistingReworkTask(page, config) {
  await page.goto(`${config.baseUrl}${WORK_TASK_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const board = page.locator('.edhr-work-task-page').first()
  await board.waitFor({ state: 'visible', timeout: 60000 })
  await board.getByText('驳回待改').first().waitFor({ state: 'visible', timeout: 60000 })
  await fillFormItem(board, '批次', config.batchCode)
  const taskPageResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-work-task/my-page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(board, '查询')
  const taskPageData = await readApiData(await taskPageResponse, '既有返工工作任务待办分页')
  const targetTask = (taskPageData.list || []).find((task) => Number(task.id) === Number(config.reworkTaskId))
  assert.ok(targetTask, `工作任务看板未返回返工待办 ID=${config.reworkTaskId}`)
  assert.equal(targetTask.taskType, 'REWORK', '既有返工工作任务必须是 REWORK 类型')
  assert.equal(targetTask.status, 'TODO', '既有返工工作任务必须是 TODO 状态')
  assert.ok(targetTask.executionId, '既有返工工作任务必须指向修订草稿 executionId')

  const row = board
    .locator('.el-table__body-wrapper tr')
    .filter({ hasText: targetTask.batchCode || config.batchCode })
    .filter({ hasText: targetTask.taskCode || String(config.reworkTaskId) })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await clickButton(row, '处理')
  await page.waitForURL(
    (url) =>
      url.pathname === EXECUTION_DETAIL_ROUTE &&
      url.searchParams.get('id') === String(targetTask.executionId) &&
      url.searchParams.get('workTaskId') === String(config.reworkTaskId),
    { timeout: 60000 }
  )
  await page.getByText('受控修订草稿').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('来源驳回版本').first().waitFor({ state: 'visible', timeout: 30000 })
  await waitForText(page, /草稿（可编辑）|草稿/, '既有返工修订页面未展示 DRAFT 草稿状态')
  const summaryText = (await page.locator('.edhr-page-shell__summary').first().textContent()) || ''
  const executionCodeMatch = summaryText.match(/BRE\d+/)
  assert.ok(executionCodeMatch, '既有返工修订草稿未展示新的执行编号。')
  return {
    revisionExecutionId: Number(targetTask.executionId),
    reworkTaskId: Number(targetTask.id),
    revisionExecutionCode: executionCodeMatch[0]
  }
}

async function verifyBatchRecovered(page, config) {
  await page.goto(`${config.baseUrl}/mes/pro/feedback/edhr-batch-execution`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await fillFormItem(page, '批次号', config.batchCode)
  const pageResponse = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/edhr-batch-execution/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(page, '查询')
  const data = await readApiData(await pageResponse, '批次执行分页')
  const matched = (data.list || []).find((item) => item.batchCode === config.batchCode)
  assert.ok(matched, `批次执行列表未返回批次号 ${config.batchCode}`)
  assert.ok(
    [10, 20, 30, 40].includes(Number(matched.status)),
    `修订审批通过后批次必须恢复到 IN_PROGRESS/READY_TO_CLOSE/CLOSED/ARCHIVED，实际 status=${matched.status}`
  )
  return matched.status
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  try {
    let preparedExecution = {}
    if (config.mode === 'setup-from-fill-task') {
      const setupContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
      const setupPage = await setupContext.newPage()
      await login(setupPage, config, config.filler, WORK_TASK_ROUTE)
      preparedExecution = await openFillTaskAndSubmit(setupPage, config)
      config.executionCode = preparedExecution.executionCode
      config.batchCode = preparedExecution.batchCode
      await setupContext.close()
    }

    let rejectResult = {}
    if (config.mode !== 'existing-rework-task') {
      const reviewerContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
      const reviewerPage = await reviewerContext.newPage()
      await login(reviewerPage, config, config.reviewer, `${APPROVAL_ROUTE}?tab=pending`)
      rejectResult = await rejectExecution(reviewerPage, config)
      await reviewerContext.close()
    }

    const fillerContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const fillerPage = await fillerContext.newPage()
    await login(fillerPage, config, config.filler, WORK_TASK_ROUTE)
    const reworkOpenResult =
      config.mode === 'existing-rework-task'
        ? await openExistingReworkTask(fillerPage, config)
        : await openReworkTask(fillerPage, config, rejectResult)
    const reworkFieldAudit = await saveFieldAuditByUi(fillerPage, config)
    await submitExecutionByUi(fillerPage, config)
    await fillerContext.close()

    const approveContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const approvePage = await approveContext.newPage()
    await login(approvePage, config, config.reviewer, `${APPROVAL_ROUTE}?tab=pending`)
    let approveResult = await approveExecution(approvePage, config, reworkOpenResult.revisionExecutionCode)
    await approveContext.close()

    if (approveResult.status === 1) {
      if (!config.finalReviewer.username || !config.finalReviewer.password || !config.finalReviewer.signaturePassword) {
        throw new Error(
          '修订审批仍处于 SUBMITTED，多人审核闭环缺少 EDHR_REJECTION_REVISION_E2E_FINAL_REVIEWER_USERNAME/PASSWORD/SIGN_PASSWORD。'
        )
      }
      const finalApproveContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
      const finalApprovePage = await finalApproveContext.newPage()
      await login(finalApprovePage, config, config.finalReviewer, `${APPROVAL_ROUTE}?tab=pending`)
      approveResult = await approveExecution(finalApprovePage, config, reworkOpenResult.revisionExecutionCode)
      assert.ok(
        approveResult.status === 3,
        `第二审核人通过后修订记录必须进入 APPROVED=3，实际：${JSON.stringify(approveResult)}`
      )
      await finalApproveContext.close()
    }
    const verifyContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const verifyPage = await verifyContext.newPage()
    await login(verifyPage, config, config.filler, `${BATCH_DETAIL_ROUTE}`)
    const batchStatusAfterApproval = await verifyBatchRecovered(verifyPage, config)
    await verifyContext.close()

    return {
      ...preparedExecution,
      ...rejectResult,
      ...reworkOpenResult,
      fieldAuditBatchId: reworkFieldAudit.fieldAuditBatchId,
      revisionApprovalStatus: approveResult.status,
      batchStatusAfterApproval
    }
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: '真实 E2E 前置条件缺失或命中受保护租户。',
      missing: config.missing
    }
    writeEvidence(result, config)
    console.error(result.reason)
    process.exitCode = 1
    return
  }

  try {
    const rejectResult = await runRealFlow(config)
    writeEvidence({ status: 'PASS', ...rejectResult }, config)
    console.log('PASS: eDHR rejection revision real flow')
  } catch (error) {
    ensureDir(RESULT_DIR)
    const result = { status: 'FAIL', error: serializeError(error) }
    fs.writeFileSync(path.join(RESULT_DIR, 'failure.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    writeEvidence(result, config)
    throw error
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
