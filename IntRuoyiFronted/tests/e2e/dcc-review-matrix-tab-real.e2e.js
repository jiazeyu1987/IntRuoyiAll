const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_E2E_TEST_TENANT || '测试租户'
const USERNAME = process.env.DCC_E2E_TEST_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_E2E_TEST_PASSWORD || '111111'
const HEADLESS = process.env.DCC_E2E_HEADLESS !== 'false'
const CATEGORY_CODE = process.env.DCC_REVIEW_MATRIX_E2E_CATEGORY_CODE || 'DCC_OTHER_TEMPLATE_T122'
const TARGET_CATEGORY_ID = Number(process.env.DCC_REVIEW_MATRIX_E2E_CATEGORY_ID || '907178')
const taskId = '20260625-dcc-review-matrix-owner-role-triangle-alignment'
const evidenceDir = path.join(__dirname, '..', '..', 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'dcc-review-matrix-tab-real-evidence.json')

const evidence = {
  taskId,
  startedAt: new Date().toISOString(),
  baseUrl: BASE_URL,
  tenant: TENANT,
  username: USERNAME,
  categoryCode: CATEGORY_CODE,
  categoryId: TARGET_CATEGORY_ID,
  steps: [],
  sample: null,
  matrixBefore: null,
  matrixAfter: null,
  previewBeforeSave: null,
  sampleProbeFindings: []
}

function record(status, label, detail = {}) {
  evidence.steps.push({
    status,
    label,
    detail,
    at: new Date().toISOString()
  })
}

async function runStep(label, fn) {
  try {
    const detail = await fn()
    record('PASS', label, detail || {})
    return detail
  } catch (error) {
    record('FAIL', label, { message: error.message })
    throw error
  }
}

function assertE2EBoundary() {
  assert.equal(TENANT, '测试租户', `DCC 审阅矩阵真实 E2E 必须使用测试租户，当前为 ${TENANT}`)
  assert.equal(USERNAME, 'aoteman', `DCC 审阅矩阵真实 E2E 必须使用 aoteman，当前为 ${USERNAME}`)
}

function timestampLabel() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

function resolveCompanyRootName(departments, deptId) {
  const byId = new Map((departments || []).map((item) => [Number(item.id), item]))
  let currentId = Number(deptId)
  const ancestors = []
  const visited = new Set()
  while (Number.isFinite(currentId) && currentId > 0 && !visited.has(currentId)) {
    visited.add(currentId)
    const current = byId.get(currentId)
    if (!current) {
      break
    }
    ancestors.push(current)
    const parentId = Number(current.parentId || 0)
    if (!parentId || !byId.has(parentId)) {
      break
    }
    currentId = parentId
  }
  const topAncestor = ancestors.at(-1)
  if (!topAncestor) {
    return ''
  }
  if (String(topAncestor.name || '').trim() === '顶级部门' && ancestors.length >= 2) {
    return String(ancestors.at(-2)?.name || '')
  }
  return String(topAncestor.name || '')
}

function nextEffectiveTime() {
  const now = new Date(Date.now() + 10 * 60 * 1000)
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:00`
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(600)
}

async function fillFirstVisible(page, selector, value, label) {
  const locator = page.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label}: ${selector}`)
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    await tenantSelect.click()
    await page.locator('.login-form .el-select__input').first().fill(tenantName)
    await page.keyboard.press('Enter')
    return true
  }
  return false
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  if (page.url().includes('/login')) {
    const selected = await selectTenant(page, TENANT)
    if (!selected) {
      await fillFirstVisible(page, 'input[placeholder="请输入租户名称"]', TENANT, 'tenant input')
    }
    await fillFirstVisible(page, 'input[placeholder="请输入用户名"]', USERNAME, 'username input')
    await fillFirstVisible(page, 'input[placeholder="请输入密码"]', PASSWORD, 'password input')
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/system/auth/login') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      ),
      page.locator('.login-form .el-button--primary').first().click()
    ])
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  }
  await settle(page)
}

async function waitForText(page, text, label, timeout = 30000) {
  await page.getByText(text).first().waitFor({ state: 'visible', timeout }).catch(async () => {
    const bodyText = await page.locator('body').innerText().catch(() => '')
    throw new Error(`${label} not visible: ${text}; body=${bodyText.slice(0, 500)}`)
  })
}

async function readAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (const store of [window.localStorage, window.sessionStorage]) {
      for (let index = 0; index < store.length; index += 1) {
        const key = store.key(index)
        if (!key || Object.prototype.hasOwnProperty.call(result, key)) {
          continue
        }
        result[key] = store.getItem(key)
      }
    }
    return result
  })

  const unwrap = (raw) => {
    if (!raw) {
      return ''
    }
    let current = raw
    const visited = new Set()
    while (typeof current === 'string') {
      const trimmed = current.trim()
      if (!trimmed) {
        return ''
      }
      if (!/^[\[{"]/.test(trimmed)) {
        return trimmed
      }
      if (visited.has(trimmed)) {
        return trimmed
      }
      visited.add(trimmed)
      try {
        current = JSON.parse(trimmed)
      } catch {
        return trimmed
      }
    }
    if (current && typeof current === 'object') {
      for (const field of ['accessToken', 'v', 'value', 'token', 'data']) {
        if (Object.prototype.hasOwnProperty.call(current, field)) {
          return unwrap(current[field])
        }
      }
    }
    return String(current || '')
  }

  const accessToken = unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token)
  const tenantId = unwrap(snapshot.tenantId || snapshot.TenantId)
  const visitTenantId = unwrap(snapshot.visitTenantId || snapshot.VisitTenantId)
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  return {
    Authorization: accessToken.startsWith('Bearer ') ? accessToken : `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
  }
}

function buildJsonHeaders(authHeaders) {
  return {
    ...authHeaders,
    'Content-Type': 'application/json'
  }
}

async function fetchJson(pathname, options = {}) {
  const headers = { ...(options.headers || {}) }
  const response = await fetch(`${BASE_URL}${pathname}`, {
    ...options,
    headers
  })
  const text = await response.text()
  let payload
  try {
    payload = JSON.parse(text)
  } catch {
    throw new Error(`non-json response for ${pathname}: ${text.slice(0, 500)}`)
  }
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${pathname}: ${text.slice(0, 500)}`)
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'code') && payload.code !== 0) {
    throw new Error(`business error for ${pathname}: ${text.slice(0, 500)}`)
  }
  return payload.data
}

async function findUsableOwnerRoleSample(authHeaders) {
  const [departments, roles] = await Promise.all([
    fetchJson('/admin-api/system/dept/simple-list', { headers: authHeaders }),
    fetchJson('/admin-api/system/role/simple-list', { headers: authHeaders })
  ])
  const users = await fetchJson('/admin-api/system/user/simple-list', { headers: authHeaders })
  const activeUserIds = new Set((users || []).map((item) => Number(item.id)).filter(Number.isFinite))
  const probeFindings = []
  const departmentNameCounts = new Map()
  for (const department of departments || []) {
    const name = String(department?.name || '').trim()
    if (!name) {
      continue
    }
    departmentNameCounts.set(name, (departmentNameCounts.get(name) || 0) + 1)
  }

  const deptSample = (departments || []).find(
    (dept) =>
      Number(dept.id) > 0 &&
      Number(dept.leaderUserId) > 0 &&
      activeUserIds.has(Number(dept.leaderUserId)) &&
      departmentNameCounts.get(String(dept.name || '').trim()) === 1 &&
      resolveCompanyRootName(departments, dept.id) &&
      resolveCompanyRootName(departments, dept.id) !== String(dept.name || '').trim()
  )
  if (!deptSample) {
    throw new Error(
      'BLOCKER: 测试租户未找到“名称唯一、leaderUserId 可用、且不是公司根本身”的系统部门，无法验证审阅矩阵首条 DEPT 规则的手工选部门路径。'
    )
  }

  for (const role of roles || []) {
    if (!role?.id) {
      continue
    }
    const preview = await fetchJson(`/admin-api/dcc/file-categories/${TARGET_CATEGORY_ID}/matrix/effective-preview`, {
      method: 'POST',
      headers: buildJsonHeaders(authHeaders),
      body: JSON.stringify({
        effectiveTime: nextEffectiveTime(),
        remark: `sample-probe-${timestampLabel()}`,
        rules: [
          {
            stageType: 'SIGNOFF',
            active: true,
            subjectLabel: deptSample.name,
            marker: '▲',
            subjectType: 'DEPT',
            subjectId: deptSample.id,
            subjectName: deptSample.name,
            subjectDepartmentPath: deptSample.name
          },
          {
            stageType: 'APPROVAL',
            active: true,
            subjectLabel: role.name,
            marker: '▲',
            subjectType: 'ROLE',
            subjectId: role.id,
            subjectName: role.name
          }
        ]
      })
    })

    const finding = {
      roleId: role.id,
      roleName: role.name,
      blocking: Boolean(preview?.blocking),
      riskCodes: Array.isArray(preview?.risks) ? preview.risks.map((item) => item.code) : [],
      stageSourceRules: Array.isArray(preview?.stages)
        ? preview.stages.map((stage) => stage.sourceRule).filter(Boolean)
        : []
    }
    probeFindings.push(finding)
    evidence.sampleProbeFindings = probeFindings

    if (preview?.blocking) {
      continue
    }

    const hasLegacyRuntimeRisk = finding.riskCodes.includes('SUBJECT_TYPE_UNSUPPORTED')
    const hasLegacyRuntimeStageSource = finding.stageSourceRules.some((rule) =>
      ['按部门树解析', '按规则解析'].includes(rule)
    )
    if (hasLegacyRuntimeRisk || hasLegacyRuntimeStageSource) {
      throw new Error(
        `BLOCKER: 本机 48081 后端仍返回旧审阅矩阵口径，角色样本 ${role.name} 的 preview 风险=${finding.riskCodes.join(',') || '-'}，阶段来源=${finding.stageSourceRules.join(',') || '-'}；需先重启到最新后端后再执行真实 E2E。`
      )
    }

    const hasDeptStage = (preview.stages || []).some(
      (stage) =>
        stage.sourceRule === '按部门负责人解析' &&
        Array.isArray(stage.positionNames) &&
        stage.positionNames.some((name) => String(name).includes(`${deptSample.name} ▲`))
    )
    const hasRoleStage = (preview.stages || []).some(
      (stage) =>
        stage.sourceRule === '按系统角色解析' &&
        Array.isArray(stage.positionNames) &&
        stage.positionNames.some((name) => String(name).includes(`${role.name} ▲`))
    )

    if (hasDeptStage && hasRoleStage) {
      return {
        department: deptSample,
        departmentCompanyRootName: resolveCompanyRootName(departments, deptSample.id),
        role,
        preview
      }
    }
  }

  const sampleSummary = probeFindings
    .slice(0, 5)
    .map((item) => `${item.roleName}: blocking=${item.blocking}, risks=${item.riskCodes.join('/') || '-'}`)
    .join(' | ')
  throw new Error(
    `BLOCKER: 测试租户未找到存在有效成员的系统角色样本，无法执行 ROLE 审阅矩阵真实 E2E。最近探测结果：${sampleSummary || '无可用角色返回。'}`
  )
}

async function openReviewMatrixTab(page) {
  await page.goto(`${BASE_URL}/dcc/controlled-file/categories`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.getByRole('tab', { name: '审阅矩阵' }).click()
  await settle(page)
  await waitForText(
    page,
    '第 1 / 4 层文控继续固定',
    'review matrix tab alert',
    40000
  )
}

async function filterReviewMatrixRow(page, keyword) {
  const codeInput = page.locator('.review-matrix-toolbar input[placeholder="请输入类别编码"]').first()
  await codeInput.fill(keyword)
  await page.getByRole('button', { name: '查询' }).click()
  await settle(page)
  const rows = page
    .locator('[data-testid="dcc-review-matrix-table"] .el-table__body-wrapper tbody .el-table__row')
    .filter({ hasText: keyword })
  await rows.first().waitFor({ state: 'attached', timeout: 30000 })
  const count = await rows.count()
  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index)
    if (await row.isVisible()) {
      return row
    }
  }
  throw new Error(`review matrix row is not visible for ${keyword}`)
}

async function openMatrixDialog(page) {
  await openReviewMatrixTab(page)
  const row = await filterReviewMatrixRow(page, CATEGORY_CODE)
  const mode = (await row.getByRole('button', { name: '编辑' }).isVisible().catch(() => false)) ? '编辑' : '新增'
  await row.getByRole('button', { name: mode }).click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  return { dialog, mode }
}

async function clearExistingRules(dialog) {
  const deleteButtons = dialog.getByRole('button', { name: '删除' })
  while ((await deleteButtons.count()) > 0) {
    await deleteButtons.last().click()
  }
}

async function chooseSelectOption(page, text) {
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: text })
    .first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
  await settle(page)
}

async function chooseTreeOption(page, text) {
  const filterInputs = page.locator(
    '.el-select-dropdown:visible input, .el-popper:visible input, .review-matrix-dialog__department-select input'
  )
  const filterCount = await filterInputs.count()
  for (let index = 0; index < filterCount; index += 1) {
    const input = filterInputs.nth(index)
    if (await input.isVisible().catch(() => false)) {
      await input.fill(text)
      await page.waitForTimeout(300)
    }
  }

  for (const selector of [
    '.el-select-dropdown:visible .el-tree-node__label',
    '.el-popper:visible .el-tree-node__label',
    '.el-select-dropdown:visible .el-tree-node__content',
    '.el-popper:visible .el-tree-node__content'
  ]) {
    const options = page.locator(selector).filter({ hasText: text })
    const count = await options.count()
    for (let index = 0; index < count; index += 1) {
      const option = options.nth(index)
      if (await option.isVisible().catch(() => false)) {
        await option.click()
        await settle(page)
        return
      }
    }
  }

  throw new Error(`visible tree option not found: ${text}`)
}

async function assertCompanyRootHiddenInTree(page, companyRootName) {
  const visibleOptions = page.locator(
    '.el-tree-select__popper:visible .el-select-dropdown__item, .el-select-dropdown:visible .el-select-dropdown__item'
  )
  await page.waitForTimeout(300)
  const count = await visibleOptions.count()
  assert.ok(count > 0, '部门树已打开但未渲染任何可见部门选项')
  const labels = []
  for (let index = 0; index < count; index += 1) {
    const option = visibleOptions.nth(index)
    if (!(await option.isVisible().catch(() => false))) {
      continue
    }
    const label = (await option.innerText().catch(() => '')).trim()
    if (label) {
      labels.push(label)
    }
  }
  assert.ok(companyRootName, '样本部门必须能解析出公司根节点名称')
  assert.ok(!labels.includes(companyRootName), `部门树首层不得显示公司根节点：${companyRootName}`)
}

async function configureRuleRow(page, row, sample, type) {
  const labelInput = row.locator('input[placeholder="如 QMS / 新品开发部"]').first()
  const selectCells = row.locator('.el-select')

  if (type === 'DEPT') {
    await labelInput.fill(sample.department.name)
    await selectCells.nth(0).click()
    await chooseSelectOption(page, '审核')
    await selectCells.nth(1).click()
    await chooseSelectOption(page, '部门')
    await row.locator('.review-matrix-dialog__department-select .el-select__wrapper').click()
    await assertCompanyRootHiddenInTree(page, sample.department.companyRootName)
    await chooseTreeOption(page, sample.department.name)
    return
  }

  await labelInput.fill(sample.role.name)
  await selectCells.nth(0).click()
  await chooseSelectOption(page, '批准')
  await selectCells.nth(1).click()
  await chooseSelectOption(page, '系统角色')
  await row.locator('.el-select').last().click()
  await chooseSelectOption(page, sample.role.name)
}

async function configureDialog(page, dialog, sample) {
  const effectiveInput = dialog.locator('input[placeholder="请选择审阅矩阵生效时间"]').first()
  const effectiveTime = nextEffectiveTime()
  await effectiveInput.click()
  await page.keyboard.press('Control+A')
  await page.keyboard.type(effectiveTime)
  await page.keyboard.press('Enter')
  await settle(page)

  const remark = `DCC 审阅矩阵真实 E2E owner-role ${timestampLabel()}`
  await dialog.locator('input[placeholder="填写本次矩阵调整说明"]').fill(remark)

  await clearExistingRules(dialog)
  await dialog.getByRole('button', { name: '新增规则' }).click()
  await dialog.getByRole('button', { name: '新增规则' }).click()
  await settle(page)

  const rows = dialog.locator('[data-testid="dcc-review-matrix-rule-editor"] tbody tr')
  assert.equal(await rows.count(), 2, '审阅矩阵规则表应包含 2 条规则')
  await configureRuleRow(page, rows.nth(0), sample, 'DEPT')
  await configureRuleRow(page, rows.nth(1), sample, 'ROLE')

  return { effectiveTime, remark }
}

async function verifyPreview(dialog, sample) {
  const stageTable = dialog.locator('[data-testid="dcc-matrix-effective-preview"]')
  await stageTable.waitFor({ state: 'visible', timeout: 30000 })
  const stageText = await stageTable.innerText()
  assert.ok(stageText.includes('主体集合'), '预览表应显示 主体集合 列')
  assert.ok(stageText.includes('按部门负责人解析'), '预览表应显示 按部门负责人解析')
  assert.ok(stageText.includes('按系统角色解析'), '预览表应显示 按系统角色解析')
  assert.ok(stageText.includes(`${sample.department.name} ▲`), '预览表应显示部门三角标记')
  assert.ok(stageText.includes(`${sample.role.name} ▲`), '预览表应显示角色三角标记')

  const editorText = await dialog.locator('[data-testid="dcc-review-matrix-rule-editor"]').innerText()
  assert.ok(!editorText.includes('备注'), '规则表内不应继续展示行备注列')
  assert.ok(!editorText.includes('●'), '规则表内不应继续展示圆点标记')
}

function extractStageRules(matrix, stageType) {
  return (matrix.rules || []).filter((rule) => rule.stageType === stageType)
}

async function main() {
  assertE2EBoundary()
  fs.mkdirSync(evidenceDir, { recursive: true })

  const browser = await chromium.launch({ headless: HEADLESS })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  let authHeaders

  try {
    await runStep('login', async () => {
      await login(page)
      return { url: page.url() }
    })

    authHeaders = await runStep('capture auth headers', async () => {
      return await readAuthHeaders(page)
    })

    const sample = await runStep('find usable department leader and role samples', async () => {
      const resolved = await findUsableOwnerRoleSample(authHeaders)
      evidence.sample = {
        departmentId: resolved.department.id,
        departmentName: resolved.department.name,
        departmentCompanyRootName: resolved.departmentCompanyRootName,
        leaderUserId: resolved.department.leaderUserId,
        roleId: resolved.role.id,
        roleName: resolved.role.name
      }
      evidence.previewBeforeSave = resolved.preview
      return evidence.sample
    })

    evidence.matrixBefore = await runStep('read matrix before save', async () => {
      return await fetchJson(`/admin-api/dcc/file-categories/${TARGET_CATEGORY_ID}/matrix`, {
        headers: authHeaders
      })
    })

    await runStep('configure matrix in UI', async () => {
      const { dialog } = await openMatrixDialog(page)
      const config = await configureDialog(page, dialog, {
        department: {
          id: sample.departmentId,
          name: sample.departmentName,
          companyRootName: sample.departmentCompanyRootName
        },
        role: { id: sample.roleId, name: sample.roleName }
      })

      await dialog.getByRole('button', { name: '刷新预览' }).click()
      await settle(page)
      await verifyPreview(dialog, {
        department: { name: sample.departmentName },
        role: { name: sample.roleName }
      })

      const saveResponse = page.waitForResponse(
        (response) =>
          response.url().includes(`/admin-api/dcc/file-categories/${TARGET_CATEGORY_ID}/matrix`) &&
          response.request().method() === 'PUT',
        { timeout: 30000 }
      )
      await dialog.getByRole('button', { name: '保存矩阵' }).click()
      const response = await saveResponse
      const payload = await response.json().catch(() => null)
      assert.equal(response.status(), 200, `matrix save failed: ${JSON.stringify(payload)}`)
      assert.equal(payload?.code, 0, `matrix save business failed: ${JSON.stringify(payload)}`)
      await settle(page)
      return config
    })

    evidence.matrixAfter = await runStep('read matrix after save', async () => {
      return await fetchJson(`/admin-api/dcc/file-categories/${TARGET_CATEGORY_ID}/matrix`, {
        headers: authHeaders
      })
    })

    await runStep('verify saved matrix contract', async () => {
      const signoffRules = extractStageRules(evidence.matrixAfter, 'SIGNOFF')
      const approvalRules = extractStageRules(evidence.matrixAfter, 'APPROVAL')

      assert.ok(signoffRules.some((rule) => rule.subjectType === 'DEPT' && Number(rule.subjectId) === Number(sample.departmentId)))
      assert.ok(approvalRules.some((rule) => rule.subjectType === 'ROLE' && Number(rule.subjectId) === Number(sample.roleId)))
      assert.ok((evidence.matrixAfter.rules || []).every((rule) => rule.marker === '▲' || rule.marker == null))
      assert.ok((evidence.matrixAfter.rules || []).every((rule) => !rule.remark), '回读规则行 remark 应为空')

      return {
        signoffRules,
        approvalRules
      }
    })
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
    evidence.finishedAt = new Date().toISOString()
    fs.writeFileSync(evidencePath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  }

  console.log(`PASS: DCC 审阅矩阵真实 E2E evidence written to ${evidencePath}`)
}

main().catch((error) => {
  evidence.finishedAt = new Date().toISOString()
  evidence.fatalError = error.message
  fs.mkdirSync(evidenceDir, { recursive: true })
  fs.writeFileSync(evidencePath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
