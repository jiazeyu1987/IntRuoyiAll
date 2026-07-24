const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const POLICY_MODES = new Set(['BPM_REQUIRED', 'DIRECT'])
const modeTextByValue = {
  BPM_REQUIRED: 'BPM审批',
  DIRECT: '关闭审批'
}

function normalizePolicyMode(value) {
  const mode = String(value || 'BPM_REQUIRED').trim()
  if (!POLICY_MODES.has(mode)) {
    throw new Error('unsupported BPM_POLICY_E2E_POLICY_MODE: ' + mode)
  }
  return mode
}

function resolveSwitchTargetModes(defaultPolicyMode) {
  const rawSequence = String(process.env.BPM_POLICY_E2E_SWITCH_TARGET_MODES || '').trim()
  if (!rawSequence) {
    return defaultPolicyMode === 'BPM_REQUIRED' ? ['DIRECT', 'BPM_REQUIRED'] : ['BPM_REQUIRED', 'DIRECT']
  }
  const targetModes = rawSequence
    .split(',')
    .map((item) => normalizePolicyMode(item))
  if (!targetModes.length) {
    throw new Error('BPM_POLICY_E2E_SWITCH_TARGET_MODES must contain at least one target mode')
  }
  return targetModes
}

const runId = process.env.BPM_POLICY_E2E_RUN_ID || new Date().toISOString().replace(/\D/g, '').slice(0, 14)
const policyMode = normalizePolicyMode(process.env.BPM_POLICY_E2E_POLICY_MODE)
const config = {
  baseUrl: (process.env.BPM_POLICY_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.BPM_POLICY_E2E_TENANT || '测试租户',
  username: process.env.BPM_POLICY_E2E_USERNAME || 'aoteman',
  password: process.env.BPM_POLICY_E2E_PASSWORD || '111111',
  signaturePassword:
    process.env.BPM_POLICY_E2E_SIGNATURE_PASSWORD ||
    process.env.ROUTE_VERSION_E2E_APPROVER_SIGNATURE_PASSWORD ||
    '',
  targetPath: '/approval-center/manager/business-approval-policy',
  timeout: Number(process.env.BPM_POLICY_E2E_TIMEOUT || '90000'),
  switchExistingOnly: process.env.BPM_POLICY_E2E_SWITCH_EXISTING_ONLY !== '0',
  taskDir:
    process.env.BPM_POLICY_E2E_TASK_DIR ||
    path.resolve(__dirname, '..', '..', 'output', 'e2e', 'bpm-business-approval-policy')
}

const testPolicy = {
  dataDomain: process.env.BPM_POLICY_E2E_DATA_DOMAIN || 'MES',
  systemCode: process.env.BPM_POLICY_E2E_SYSTEM_CODE || 'MES',
  objectType: process.env.BPM_POLICY_E2E_OBJECT_TYPE || 'ROUTE_VERSION',
  actionCode: process.env.BPM_POLICY_E2E_ACTION_CODE || 'PUBLISH',
  objectState: process.env.BPM_POLICY_E2E_OBJECT_STATE || 'DRAFT',
  policyMode,
  policyModeText: modeTextByValue[policyMode],
  effectExecutorCode: process.env.BPM_POLICY_E2E_EFFECT_EXECUTOR_CODE || 'MES_ROUTE_VERSION_PUBLISH',
  remark: process.env.BPM_POLICY_E2E_REMARK || 'TDD+BDD signature E2E ' + runId
}
const policyBusinessRecordCode = `${testPolicy.systemCode}:${testPolicy.objectType}:${testPolicy.actionCode}:${testPolicy.objectState}`
const switchTargetModes = resolveSwitchTargetModes(policyMode)
if (!config.switchExistingOnly && switchTargetModes.includes('BPM_REQUIRED')) {
  throw new Error('BPM_POLICY_E2E_SWITCH_EXISTING_ONLY must stay enabled when switching to BPM_REQUIRED without manual process key input')
}

const policyDisplayLabels = {
  dataDomain: {
    MES: '生产执行'
  },
  systemCode: {
    MES: '生产执行'
  },
  objectType: {
    ROUTE_VERSION: '工艺路线版本',
    BATCH_RECORD_VERSION: '批记录版本',
    EDHR_BATCH_EXECUTION: '批次执行记录'
  },
  actionCode: {
    PUBLISH: '发布',
    UPGRADE: '升版',
    OBSOLETE: '作废',
    VOID: '作废',
    SUBMIT_REVIEW: '提交审核'
  },
  objectState: {
    READY_TO_PUBLISH: '待发布',
    READY: '就绪',
    DRAFT: '草稿',
    ACTIVE: '已生效',
    PRECHECK_PASSED: '预检通过',
    RELEASED: '已放行',
    CLOSED: '已关闭',
    REJECTED: '已驳回',
    PUBLISHED: '已发布',
    DISABLED: '已禁用'
  }
}

const screenshots = {
  final: path.join(config.taskDir, 'bpm-business-approval-policy-' + runId + '-' + policyMode + '.png'),
  failure: path.join(config.taskDir, 'bpm-business-approval-policy-' + runId + '-' + policyMode + '-failed.png')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(800)
}

async function login(page) {
  await page.goto(config.baseUrl + '/login?redirect=' + encodeURIComponent('/index'), {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionResponsePromise = page
    .waitForResponse(
      (response) => response.url().includes('/system/auth/get-permission-info') && response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)

  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(loginPayload && (loginPayload.code === 0 || loginPayload.code === 200), 'login failed: ' + JSON.stringify(loginPayload))
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await permissionResponsePromise
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await settle(page)
}

function assertCommonResult(payload, label) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), label + ' failed: ' + JSON.stringify(payload))
  return payload.data
}

async function waitForApiSuccess(page, matcher, method, label, trigger) {
  const responsePromise = page.waitForResponse(
    (response) => {
      const matchesUrl = typeof matcher === 'function' ? matcher(response.url()) : response.url().includes(matcher)
      return matchesUrl && response.request().method() === method && response.status() >= 200 && response.status() < 300
    },
    { timeout: config.timeout }
  )
  await trigger()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  let requestPayload
  try {
    requestPayload = response.request().postDataJSON()
  } catch {
    requestPayload = undefined
  }
  return {
    data: assertCommonResult(payload, label),
    requestPayload
  }
}

function formItem(dialog, label) {
  return dialog.locator('.el-form-item').filter({ hasText: label }).first()
}

async function fillFormItem(dialog, label, value) {
  const item = formItem(dialog, label)
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await item.locator('input.el-input__inner, textarea.el-textarea__inner').first().fill(value)
}

async function openCreateDialog(page) {
  await page.getByRole('button', { name: '新增策略' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增平台业务审批策略' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  return dialog
}

async function fillPolicyFields(dialog) {
  await fillFormItem(dialog, '数据域', testPolicy.dataDomain)
  await fillFormItem(dialog, '系统编码', testPolicy.systemCode)
  await fillFormItem(dialog, '对象类型', testPolicy.objectType)
  await fillFormItem(dialog, '动作编码', testPolicy.actionCode)
  await fillFormItem(dialog, '对象状态', testPolicy.objectState)
  await fillFormItem(dialog, '领域执行器', testPolicy.effectExecutorCode)
  await fillFormItem(dialog, '备注', testPolicy.remark)
}

async function selectPolicyMode(dialog, mode) {
  await dialog.getByText(modeTextByValue[mode], { exact: true }).click()
}

async function applyQuickFilter(page, fieldLabel, value) {
  const quickFilter = page.locator('.table-quick-filter[data-table-key="bpm.business-approval-policy.main"]').first()
  await quickFilter.waitFor({ state: 'visible', timeout: config.timeout })
  await quickFilter.locator('.table-quick-filter__field').click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: fieldLabel }).first().click()
  const valueInput = quickFilter.locator('.table-quick-filter__value input.el-input__inner').first()
  await valueInput.fill(value)
  await waitForApiSuccess(page, '/business-approval/policies', 'GET', 'quick filter ' + fieldLabel, async () => {
    await quickFilter.getByRole('button', { name: '查询' }).click()
  })
  await settle(page)
}

async function findPolicyRow(page, expectedPolicyModeText = testPolicy.policyModeText) {
  await applyQuickFilter(page, '动作编码', testPolicy.actionCode)
  const rows = page.locator('.el-table__body-wrapper tbody tr:visible')
  await rows.first().waitFor({ state: 'visible', timeout: 60000 })
  const count = await rows.count()
  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index)
    const text = await row.innerText()
    if (
      rowTextIncludesAny(text, codeCandidates(testPolicy.dataDomain, policyDisplayLabels.dataDomain)) &&
      rowTextIncludesAny(text, codeCandidates(testPolicy.systemCode, policyDisplayLabels.systemCode)) &&
      rowTextIncludesAny(text, codeCandidates(testPolicy.objectType, policyDisplayLabels.objectType)) &&
      rowTextIncludesAny(text, codeCandidates(testPolicy.actionCode, policyDisplayLabels.actionCode)) &&
      rowTextIncludesAny(text, codeCandidates(testPolicy.objectState, policyDisplayLabels.objectState)) &&
      (!expectedPolicyModeText || text.includes(expectedPolicyModeText))
    ) {
      return row
    }
  }
  throw new Error('policy row not found for ' + JSON.stringify({ ...testPolicy, signaturePasswordProvided: Boolean(config.signaturePassword) }))
}

function codeCandidates(value, labels) {
  return [...new Set([value, labels[String(value || '').toUpperCase()]].filter(Boolean))]
}

function rowTextIncludesAny(text, candidates) {
  return candidates.some((candidate) => text.includes(candidate))
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) return ''
  const normalizeString = (value) => {
    let current = value || ''
    for (let index = 0; index < 3; index += 1) {
      const trimmed = String(current).trim()
      if (!trimmed.startsWith('"')) return trimmed
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed !== 'string' || parsed === current) return trimmed.replace(/^"(.*)"$/, '$1')
        current = parsed
      } catch {
        return trimmed.replace(/^"(.*)"$/, '$1')
      }
    }
    return String(current).trim()
  }
  const unwrap = (value) => {
    let current = value
    for (let index = 0; index < 6; index += 1) {
      if (!current || typeof current !== 'object') return typeof current === 'string' ? normalizeString(current) : current || ''
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
      return current
    }
    return current || ''
  }
  try {
    return unwrap(JSON.parse(raw))
  } catch {
    return normalizeString(raw)
  }
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function requestJson(page, headers, relativePath) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, {
        method: 'GET',
        headers: requestHeaders
      })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { rawText: text }
      }
      return { status: response.status, ok: response.ok, payload }
    },
    { requestUrl: config.baseUrl + relativePath, requestHeaders: headers }
  )
}

async function querySignatureRecord(page, targetMode) {
  const headers = await buildAuthHeaders(page)
  const query = new URLSearchParams({
    pageNo: '1',
    pageSize: '20',
    sourceCodes: 'BPM',
    keyword: policyBusinessRecordCode
  })
  let lastPayload = null
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const response = await requestJson(page, headers, '/admin-api/signature-governance/signature-records/page?' + query.toString())
    assert.ok(response.ok, 'signature record query HTTP failed: ' + JSON.stringify({ status: response.status }))
    const pageResult = assertCommonResult(response.payload, 'signature record query')
    lastPayload = pageResult
    const list = Array.isArray(pageResult?.list) ? pageResult.list : []
    const matched = list.find(
      (record) =>
        record.sourceCode === 'BPM' &&
        record.businessRecordCode === policyBusinessRecordCode &&
        record.comment === targetMode &&
        record.evidenceStatus === 'PASSWORD_VERIFIED'
    )
    if (matched) return matched
    await page.waitForTimeout(800)
  }
  throw new Error(
    'signature record not found: ' +
      JSON.stringify({
        sourceCode: 'BPM',
        businessRecordCode: policyBusinessRecordCode,
        comment: targetMode,
        evidenceStatus: 'PASSWORD_VERIFIED',
        lastPayload
      })
  )
}

async function switchPublishedPolicyThroughUi(page, row, targetMode) {
  const switchResult = await waitForApiSuccess(
    page,
    (url) => /\/business-approval\/policies\/\d+\/switch-mode/.test(url),
    'POST',
    'switch policy mode',
    async () => {
      const switchControl = row.locator('.el-switch').first()
      await switchControl.waitFor({ state: 'visible', timeout: config.timeout })
      await switchControl.click()
      const promptBox = page.locator('.el-message-box:visible').filter({ hasText: '审批开关电子签名' }).last()
      await promptBox.waitFor({ state: 'visible', timeout: 30000 })
      await promptBox.locator('input[type="password"]').fill(config.signaturePassword)
      await promptBox.getByRole('button', { name: /确定|确认|OK/ }).last().click()
    }
  )
  const switchedPolicy = switchResult.data
  assert.ok(switchedPolicy && switchedPolicy.id, 'switched policy id missing: ' + JSON.stringify(switchedPolicy))
  assert.equal(switchedPolicy.policyMode, targetMode, 'switched policy mode mismatch: ' + JSON.stringify(switchedPolicy))
  assert.equal(switchedPolicy.status, 'PUBLISHED', 'switched policy must be published: ' + JSON.stringify(switchedPolicy))

  const requestPayload = switchResult.requestPayload
  const redactedPayload = {
    policyMode: requestPayload?.policyMode,
    hasSignaturePassword: Boolean(requestPayload?.signaturePassword),
    signaturePasswordLength: String(requestPayload?.signaturePassword || '').length
  }
  assert.equal(requestPayload?.policyMode, targetMode, 'switch payload policyMode mismatch: ' + JSON.stringify(redactedPayload))
  assert.equal(requestPayload?.signaturePassword, config.signaturePassword, 'switch payload must carry electronic signature password: ' + JSON.stringify(redactedPayload))

  const signatureRecord = await querySignatureRecord(page, targetMode)
  assert.equal(signatureRecord.sourceCode, 'BPM', 'signature record source mismatch: ' + JSON.stringify(signatureRecord))
  assert.equal(signatureRecord.businessRecordCode, policyBusinessRecordCode, 'signature record business code mismatch: ' + JSON.stringify(signatureRecord))
  assert.equal(signatureRecord.comment, targetMode, 'signature record comment mismatch: ' + JSON.stringify(signatureRecord))
  assert.equal(signatureRecord.evidenceStatus, 'PASSWORD_VERIFIED', 'signature evidence status mismatch: ' + JSON.stringify(signatureRecord))
  return { switchedPolicy, signatureRecord }
}

async function main() {
  const isDefaultTestTenant = config.tenant === '测试租户' && config.username === 'aoteman'
  const isExplicitYudaoAdminWrite =
    process.env.BPM_POLICY_E2E_ALLOW_YUDAO_ADMIN_WRITE === '1' &&
    config.tenant === '芋道源码' &&
    config.username === 'admin' &&
    config.switchExistingOnly &&
    switchTargetModes.length === 2 &&
    switchTargetModes[0] === 'DIRECT' &&
    switchTargetModes[1] === 'BPM_REQUIRED'
  if (!isDefaultTestTenant && !isExplicitYudaoAdminWrite) {
    throw new Error('bpm_policy_e2e_must_use_test_tenant_aoteman_or_explicit_yudao_admin_close_then_reopen')
  }
  if (!config.signaturePassword.trim()) {
    throw new Error('BPM_POLICY_E2E_SIGNATURE_PASSWORD or ROUTE_VERSION_E2E_APPROVER_SIGNATURE_PASSWORD is required')
  }
  fs.mkdirSync(config.taskDir, { recursive: true })
  const launchOptions = { headless: process.env.BPM_POLICY_E2E_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1680, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    await page.goto(config.baseUrl + config.targetPath, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('业务审批策略').first().waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.unified-list-template[data-table-key="bpm.business-approval-policy.main"]').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    let createdPolicy = null
    if (!config.switchExistingOnly) {
      const dialog = await openCreateDialog(page)
      await fillPolicyFields(dialog)
      await selectPolicyMode(dialog, testPolicy.policyMode)

      const createResult = await waitForApiSuccess(page, '/business-approval/policies', 'POST', 'save policy', () =>
        dialog.getByRole('button', { name: '保存草稿' }).click()
      )
      createdPolicy = createResult.data
      assert.ok(createdPolicy && createdPolicy.id, 'created policy id missing: ' + JSON.stringify(createdPolicy))
      assert.equal(createdPolicy.policyMode, testPolicy.policyMode, 'created policy mode mismatch: ' + JSON.stringify(createdPolicy))
      assert.equal(createdPolicy.status, 'DRAFT', 'created policy must start as DRAFT: ' + JSON.stringify(createdPolicy))

      await page.locator('.el-dialog:visible').waitFor({ state: 'hidden', timeout: 30000 })
      const draftRow = await findPolicyRow(page)
      await draftRow.getByText('草稿', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })

      const publishResult = await waitForApiSuccess(page, '/business-approval/policies/' + createdPolicy.id + '/publish', 'POST', 'publish policy', async () => {
        await draftRow.getByRole('button', { name: '发布' }).click()
        const confirmBox = page.locator('.el-message-box:visible').last()
        await confirmBox.waitFor({ state: 'visible', timeout: 30000 })
        await confirmBox.getByRole('button', { name: /确定|确认|OK/ }).last().click()
      })
      assert.equal(publishResult.data, true, 'publish result must be true: ' + JSON.stringify(publishResult.data))
    }

    let currentRow = config.switchExistingOnly ? await findPolicyRow(page, null) : await findPolicyRow(page)
    await currentRow.getByText('已发布').waitFor({ state: 'visible', timeout: 60000 })
    const switchVerifications = []
    for (const targetMode of switchTargetModes) {
      const verification = await switchPublishedPolicyThroughUi(page, currentRow, targetMode)
      switchVerifications.push({
        switchedPolicy: verification.switchedPolicy,
        signatureRecord: {
          globalId: verification.signatureRecord.globalId,
          sourceCode: verification.signatureRecord.sourceCode,
          businessRecordCode: verification.signatureRecord.businessRecordCode,
          comment: verification.signatureRecord.comment,
          evidenceStatus: verification.signatureRecord.evidenceStatus
        }
      })
      await page.getByText(targetMode === 'BPM_REQUIRED' ? '审批流程已开启' : '审批已关闭').first().waitFor({ state: 'visible', timeout: 30000 }).catch(() => null)
      await settle(page)
      currentRow = await findPolicyRow(page, modeTextByValue[targetMode])
      await currentRow.getByText(modeTextByValue[targetMode]).waitFor({ state: 'visible', timeout: 30000 })
    }

    assert.deepEqual(pageErrors, [], 'page errors: ' + pageErrors.join(' || '))
    await page.screenshot({ path: screenshots.final, fullPage: true })
    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      targetPath: config.targetPath,
      createdPolicy,
      switchExistingOnly: config.switchExistingOnly,
      switchVerifications,
      signaturePasswordProvided: true,
      policyNaturalKey: {
        dataDomain: testPolicy.dataDomain,
        systemCode: testPolicy.systemCode,
        objectType: testPolicy.objectType,
        actionCode: testPolicy.actionCode,
        objectState: testPolicy.objectState,
        businessRecordCode: policyBusinessRecordCode
      },
      screenshots
    }
    fs.writeFileSync(
      path.join(config.taskDir, 'bpm-business-approval-policy-' + runId + '-' + policyMode + '-result.json'),
      JSON.stringify(result, null, 2) + '\n',
      'utf8'
    )
    process.stdout.write('bpm business approval policy real e2e passed\n' + JSON.stringify(result, null, 2) + '\n')
  } catch (error) {
    await page.screenshot({ path: screenshots.failure, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write((error.stack || error.message) + '\n')
  process.exit(1)
})
