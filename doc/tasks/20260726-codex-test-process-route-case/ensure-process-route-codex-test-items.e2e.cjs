const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const taskDir = __dirname
const workspaceRoot = path.resolve(taskDir, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')
const artifactsDir = path.join(taskDir, 'artifacts')
const summaryPath = path.join(artifactsDir, 'process-route-codex-test-items-summary.json')
const projectName = '工艺路线'

const testCases = [
  {
    name: '工艺路线基础信息与工序维护闭环',
    methodItems: [
      '打开 MES 系统 -> 工艺管理 -> 工艺路线，进入工艺路线列表。',
      '新增任务自有工艺路线，维护路线编码、名称、版本、产品族和基础说明。',
      '进入路线工序维护区域，新增、编辑并调整至少两个工序节点后保存。'
    ],
    testDataText:
      '测试租户=芋道源码；路线编码前缀=CODR-ROUTE-BASIC-YYYYMMDD；工序编码前缀=CODR-STEP-BASIC；仅使用任务自有样本数据。',
    checkpoints: [
      {
        name: '基础信息保存成功',
        expectedText: '保存后列表可按路线编码精确检索，项目列、路线编码、路线名称和版本信息与输入一致。'
      },
      {
        name: '必填校验 fail-fast',
        expectedText: '缺少路线编码、路线名称或关键版本字段时页面展示明确校验错误，不创建半成品路线。'
      },
      {
        name: '工序节点可回读',
        expectedText: '工序维护保存后重新打开详情，工序编码、名称、顺序和关键配置完整回读。'
      },
      {
        name: '重复编码被阻止',
        expectedText: '再次使用相同路线编码新增时返回明确业务错误，不覆盖原路线或默认保存成功。'
      }
    ]
  },
  {
    name: '工艺路线复制与产品绑定闭环',
    methodItems: [
      '在工艺路线列表检索任务自有源路线，并点击复制或另存为路线入口。',
      '填写新路线编码和名称，保留源路线工序结构后绑定任务自有产品或产品族。',
      '保存复制路线并从列表进入详情，核对源路线与新路线的独立关系。'
    ],
    testDataText:
      '源路线编码前缀=CODR-ROUTE-SOURCE；复制路线编码前缀=CODR-ROUTE-COPY；产品编码前缀=CODR-PRODUCT-ROUTE。',
    checkpoints: [
      {
        name: '复制生成独立路线',
        expectedText: '新路线拥有独立 ID、编码、名称和版本状态，源路线基础信息未被覆盖。'
      },
      {
        name: '工序结构完整复制',
        expectedText: '复制路线详情中的工序数量、顺序、关键参数和流转关系与源路线一致。'
      },
      {
        name: '产品绑定可回读',
        expectedText: '保存后产品或产品族绑定信息在详情页和只读接口中均可回读，且指向任务自有产品。'
      },
      {
        name: '编码冲突明确失败',
        expectedText: '复制时使用已存在路线编码会被明确阻止，不生成重复路线或静默改码。'
      }
    ]
  },
  {
    name: '工艺路线候选版本编辑发布闭环',
    methodItems: [
      '打开已有 ACTIVE 工艺路线详情，创建候选版本或草稿版本。',
      '在候选版本中编辑工序、流转关系、表单槽位或批记录绑定后先执行普通保存。',
      '点击提交发布或提交审批，按页面流程完成候选版本发布核验。'
    ],
    testDataText:
      '路线编码前缀=CODR-ROUTE-VERSION；候选版本号=V-CODR-YYYYMMDD；表单/批记录绑定均使用任务自有样本。',
    checkpoints: [
      {
        name: '草稿保存不隐式发布',
        expectedText: '普通保存后候选版本仍保持草稿或候选状态，不弹出隐式发布确认，不推进 ACTIVE 版本。'
      },
      {
        name: '候选变更可回读',
        expectedText: '重新打开候选版本时，工序、流转关系和绑定配置与保存内容一致。'
      },
      {
        name: '提交发布状态正确',
        expectedText: '显式提交发布后版本状态按业务流程进入待审批、待发布或生效状态，并展示清晰状态标签。'
      },
      {
        name: '发布前校验完整',
        expectedText: '缺少必需工序、流转关系或绑定配置时提交发布被明确阻止，不生成不完整 ACTIVE 版本。'
      }
    ]
  },
  {
    name: '工艺路线状态切换与删除约束闭环',
    methodItems: [
      '在工艺路线列表检索任务自有未引用路线，执行停用和启用操作。',
      '对已被工单、批次或配置引用的路线尝试删除，观察业务阻止结果。',
      '对未被引用的任务自有路线执行删除或作废，并回到列表核验结果。'
    ],
    testDataText:
      '未引用路线编码前缀=CODR-ROUTE-FREE；已引用路线编码前缀=CODR-ROUTE-LINKED；关联数据仅限任务自有样本。',
    checkpoints: [
      {
        name: '停用启用状态回读',
        expectedText: '停用和启用操作成功后，列表与详情状态同步刷新，并保留最近操作时间或操作人信息。'
      },
      {
        name: '已引用路线不可删除',
        expectedText: '被工单、批次、版本或配置引用的路线删除时返回明确业务错误，不物理删除或隐藏失败。'
      },
      {
        name: '未引用路线删除受控',
        expectedText: '未引用任务自有路线删除或作废后，按当前页面规则从列表消失或显示终态，详情不可继续编辑。'
      },
      {
        name: '删除失败不影响源数据',
        expectedText: '删除被阻止时原路线状态、工序和产品绑定保持不变，可重新检索并打开详情。'
      }
    ]
  }
]

function parseDotEnv(filePath) {
  const values = {}
  const source = fs.readFileSync(filePath, 'utf8')
  for (const line of source.split(/\r?\n/)) {
    const match = line.match(/^\s*([^#=\s][^=]*?)\s*=\s*(.*)\s*$/)
    if (!match) continue
    const key = match[1].trim()
    let value = match[2].trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    values[key] = value
  }
  return values
}

const env = parseDotEnv(path.join(frontendRoot, '.env'))
const config = {
  baseUrl: (process.env.PROCESS_ROUTE_TEST_ITEMS_BASE_URL || 'http://127.0.0.1:8082').replace(/\/+$/, ''),
  backendUrl: (process.env.PROCESS_ROUTE_TEST_ITEMS_BACKEND_URL || 'http://127.0.0.1:48082').replace(/\/+$/, ''),
  tenant: process.env.PROCESS_ROUTE_TEST_ITEMS_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.PROCESS_ROUTE_TEST_ITEMS_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.PROCESS_ROUTE_TEST_ITEMS_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  headed: process.env.PROCESS_ROUTE_TEST_ITEMS_HEADED === '1'
}

for (const [key, value] of Object.entries(config)) {
  if (key !== 'headed') assert.ok(value, `missing config ${key}`)
}

function assertPairedLocalUrls() {
  const frontend = new URL(config.baseUrl)
  const backend = new URL(config.backendUrl)
  const localHosts = new Set(['127.0.0.1', 'localhost', '::1', '[::1]'])
  assert.ok(localHosts.has(frontend.hostname), `E2E must use local frontend, got ${config.baseUrl}`)
  assert.ok(localHosts.has(backend.hostname), `E2E must use local backend, got ${config.backendUrl}`)
  assert.equal(frontend.port, '8082', 'process route E2E must use registered frontend slot port 8082')
  assert.equal(backend.port, '48082', 'process route E2E must use registered backend slot port 48082')
}

function menuContains(menus, predicate) {
  const queue = Array.isArray(menus) ? [...menus] : []
  while (queue.length > 0) {
    const menu = queue.shift()
    if (predicate(menu)) return true
    if (Array.isArray(menu.children)) queue.push(...menu.children)
  }
  return false
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`No visible element found for ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const item = await firstVisible(locator, label)
  await item.fill(value)
  return item
}

async function selectTenant(page, form) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) === 0 || !(await tenantInput.isVisible())) {
    return
  }
  await tenantInput.click()
  await tenantInput.fill(config.tenant)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function readWebStorageValue(page, key) {
  return page.evaluate((cacheKey) => {
    const raw = window.localStorage.getItem(cacheKey)
    if (!raw) return null
    try {
      const wrapper = JSON.parse(raw)
      if (wrapper && Object.prototype.hasOwnProperty.call(wrapper, 'v')) {
        return JSON.parse(wrapper.v)
      }
      return wrapper
    } catch {
      return raw
    }
  }, key)
}

async function authHeaders(page) {
  const accessToken = await readWebStorageValue(page, 'ACCESS_TOKEN')
  const tenantId = await readWebStorageValue(page, 'tenantId')
  assert.ok(accessToken, 'logged-in context is missing ACCESS_TOKEN')
  assert.ok(tenantId, 'logged-in context is missing tenantId')
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
}

async function apiGetJson(page, url, params = {}) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  }
  const response = await page.request.get(
    `${config.backendUrl}/admin-api${url}${search.size ? `?${search.toString()}` : ''}`,
    { headers: await authHeaders(page) }
  )
  const body = await response.json()
  assert.equal(response.status(), 200, `${url} HTTP ${response.status()}`)
  assert.equal(body.code, 0, `${url} business error: ${body.msg || JSON.stringify(body)}`)
  return body.data
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  if (page.url().includes('/login')) {
    const form = page.locator('form.login-form:visible').first()
    await form.waitFor({ state: 'visible', timeout: 60000 })
    await selectTenant(page, form)
    await fillFirstVisible(
      form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'),
      config.username,
      'username'
    )
    await fillFirstVisible(form.locator('input[type="password"], input[placeholder="请输入密码"]'), config.password, 'password')
    await form.getByRole('button', { name: /^登录$/ }).click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  }
  return apiGetJson(page, '/system/auth/get-permission-info')
}

async function openTestManagement(page) {
  const permissionInfo = await login(page)
  const permissions = Array.isArray(permissionInfo.permissions) ? permissionInfo.permissions : []
  const menus = Array.isArray(permissionInfo.menus) ? permissionInfo.menus : []
  assert.ok(
    permissions.includes('system:codex-test:query') || permissions.includes('*:*:*'),
    'permission response must include system:codex-test:query'
  )
  assert.ok(
    menuContains(
      menus,
      (menu) =>
        menu?.name === '测试管理' ||
        menu?.path === 'codex-test-management' ||
        menu?.component === 'system/codex-test-management/index'
    ),
    'dynamic menu response must include 测试管理 menu'
  )

  await page.getByText('系统管理', { exact: true }).first().click()
  const testMenu = page.getByText('测试管理', { exact: true }).first()
  await testMenu.waitFor({ state: 'visible', timeout: 30000 })
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/codex-test-case/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await testMenu.click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `case page business error: ${body.msg || body.code}`)
  await page.locator('text=测试方法项').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('text=测试目标项').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function searchCase(page, caseName) {
  const nameInput = page.locator('input[placeholder="输入测试项名称"]').first()
  await nameInput.waitFor({ state: 'visible', timeout: 30000 })
  await nameInput.fill(caseName)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/codex-test-case/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: /查询/ }).first().click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `case search business error: ${body.msg || body.code}`)
  const rows = body.data?.list || []
  return rows.find((row) => row.name === caseName) || null
}

async function selectProject(page, dialog) {
  const projectItem = dialog.locator('.el-form-item').filter({ hasText: '项目' }).first()
  const projectSelect = projectItem.locator('.el-select').first()
  await projectSelect.waitFor({ state: 'visible', timeout: 30000 })
  await projectSelect.click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: new RegExp(`^${projectName}$`) }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await page.waitForTimeout(200)
  assert.match(await projectItem.innerText(), new RegExp(projectName))
}

async function ensureSwitch(dialog, index, expectedChecked) {
  const item = dialog.locator('.el-form-item').filter({ hasText: '执行控制' }).first()
  const toggle = item.locator('.el-switch').nth(index)
  await toggle.waitFor({ state: 'visible', timeout: 30000 })
  const checked = await toggle.evaluate((node) => node.classList.contains('is-checked'))
  if (checked !== expectedChecked) await toggle.click()
}

async function normalizeRows(dialog, rowSelector, addButtonName, expectedCount) {
  let rows = dialog.locator(rowSelector)
  let count = await rows.count()
  while (count < expectedCount) {
    await dialog.getByRole('button', { name: addButtonName }).click()
    rows = dialog.locator(rowSelector)
    count = await rows.count()
  }
  while (count > expectedCount) {
    await rows.nth(count - 1).getByRole('button', { name: /删除/ }).click()
    rows = dialog.locator(rowSelector)
    count = await rows.count()
  }
}

async function fillCaseDialog(page, testCase, existingCase) {
  if (existingCase) {
    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: testCase.name }).first()
    await row.waitFor({ state: 'visible', timeout: 30000 })
    await row.getByRole('button', { name: /修改/ }).click()
  } else {
    await page.getByRole('button', { name: /新增测试项|新增/ }).first().click()
  }

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: existingCase ? '修改测试项' : '新增测试项' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[placeholder="例如：排产手动重排工单校验"]').fill(testCase.name)
  await selectProject(page, dialog)

  await normalizeRows(dialog, '.codex-test-method', /新增方法项/, testCase.methodItems.length)
  const methodRows = dialog.locator('.codex-test-method')
  for (let index = 0; index < testCase.methodItems.length; index += 1) {
    await methodRows.nth(index).locator('.codex-test-method__text input').fill(testCase.methodItems[index])
  }

  await dialog
    .locator('textarea[placeholder="用户手写数据，例如：来源生产工单号=881MO093613,881MO093615"]')
    .fill(testCase.testDataText)

  await dialog.locator('.el-radio-button').filter({ hasText: '顺序执行' }).first().click()
  await ensureSwitch(dialog, 0, false)
  await ensureSwitch(dialog, 1, true)

  await normalizeRows(dialog, '.codex-test-checkpoint', /新增目标项/, testCase.checkpoints.length)
  const checkpointRows = dialog.locator('.codex-test-checkpoint')
  for (let index = 0; index < testCase.checkpoints.length; index += 1) {
    const row = checkpointRows.nth(index)
    const checkpoint = testCase.checkpoints[index]
    await row.locator('.codex-test-checkpoint__name input').fill(checkpoint.name)
    await row.locator('.codex-test-checkpoint__target textarea').fill(checkpoint.expectedText)
  }

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(
        existingCase ? '/admin-api/system/codex-test-case/update' : '/admin-api/system/codex-test-case/create'
      ) && ['POST', 'PUT'].includes(response.request().method()),
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /^保存$/ }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(response.status(), 200, `save case HTTP ${response.status()}`)
  assert.equal(body.code, 0, `save case business error for ${testCase.name}: ${body.msg || JSON.stringify(body)}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function assertSavedCase(page, testCase) {
  const saved = await searchCase(page, testCase.name)
  assert.ok(saved, `saved test case must be searchable by exact name: ${testCase.name}`)
  assert.equal(saved.project, projectName, `${testCase.name} project`)
  assert.equal(saved.checkpointCount, testCase.checkpoints.length, `${testCase.name} checkpoint count`)
  const detail = await apiGetJson(page, '/system/codex-test-case/get', { id: saved.id })
  assert.equal(detail.name, testCase.name)
  assert.equal(detail.project, projectName)
  assert.equal(detail.defaultExecutionMode, 'SEQUENTIAL')
  assert.equal(detail.parallelSafe, false)
  assert.equal(detail.status, 'ENABLE')
  assert.equal(detail.methodText, testCase.methodItems.join('\n'))
  assert.equal(detail.testDataText, testCase.testDataText)
  assert.deepEqual(
    (detail.checkpoints || []).map((item) => item.name),
    testCase.checkpoints.map((item) => item.name)
  )
  assert.deepEqual(
    (detail.checkpoints || []).map((item) => item.expectedText),
    testCase.checkpoints.map((item) => item.expectedText)
  )
  return {
    id: detail.id,
    name: detail.name,
    project: detail.project,
    checkpointCount: detail.checkpoints?.length || 0,
    defaultExecutionMode: detail.defaultExecutionMode,
    parallelSafe: detail.parallelSafe,
    status: detail.status
  }
}

async function run() {
  assertPairedLocalUrls()
  fs.mkdirSync(artifactsDir, { recursive: true })

  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage', '--no-sandbox'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const summary = {
    status: 'RUNNING',
    project: projectName,
    tenant: config.tenant,
    username: config.username,
    baseUrl: config.baseUrl,
    backendUrl: config.backendUrl,
    checkedAt: new Date().toISOString(),
    requestedCaseCount: testCases.length,
    cases: []
  }

  try {
    await openTestManagement(page)
    const existing = []
    const missing = []
    for (const testCase of testCases) {
      const found = await searchCase(page, testCase.name)
      if (found) existing.push({ name: testCase.name, id: found.id, project: found.project })
      else missing.push(testCase.name)
    }
    summary.existingBefore = existing
    summary.missingBefore = missing

    for (const testCase of testCases) {
      const beforeCase = await searchCase(page, testCase.name)
      await fillCaseDialog(page, testCase, beforeCase)
      const saved = await assertSavedCase(page, testCase)
      summary.cases.push({ ...saved, existedBefore: Boolean(beforeCase) })
    }

    const verified = []
    for (const testCase of testCases) {
      verified.push(await assertSavedCase(page, testCase))
    }
    summary.verified = verified
    summary.status = 'PASS'
    await page.screenshot({ path: path.join(artifactsDir, 'process-route-test-cases-saved.png'), fullPage: true })
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    console.log(`PASS: ensured ${verified.length} process route Codex test cases`)
  } catch (error) {
    summary.status = 'FAIL'
    summary.error = error.stack || error.message || String(error)
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    await page.screenshot({ path: path.join(artifactsDir, 'process-route-test-cases-failure.png'), fullPage: true }).catch(
      () => undefined
    )
    throw error
  } finally {
    await browser.close().catch(() => undefined)
  }
}

run().catch((error) => {
  console.error(error.stack || error.message || error)
  process.exit(1)
})
