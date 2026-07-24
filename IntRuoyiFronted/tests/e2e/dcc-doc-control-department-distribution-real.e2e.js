const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_DOC_CONTROL_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_DOC_CONTROL_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_DOC_CONTROL_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_DOC_CONTROL_E2E_PASSWORD
const CATEGORY_ID = Number(process.env.DCC_DOC_CONTROL_E2E_CATEGORY_ID || 900347)
const CATEGORY_NAME = process.env.DCC_DOC_CONTROL_E2E_CATEGORY_NAME || 'Codex Local DCC Category'
const APPROVER_USER_ID = Number(process.env.DCC_DOC_CONTROL_E2E_APPROVER_USER_ID || 914520)
const DISTRIBUTION_DEPARTMENT_LABEL = process.env.DCC_DOC_CONTROL_E2E_DEPARTMENT_LABEL || '外贸部'
const PROJECT_KEYWORD = process.env.DCC_DOC_CONTROL_E2E_PROJECT_KEYWORD || ''
const FILE_TYPE_TAXONOMY_ID = Number(process.env.DCC_DOC_CONTROL_E2E_FILE_TYPE_TAXONOMY_ID || 0)
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'dcc-doc-control-department-distribution')
const TARGET_UPLOAD_PATH = '/dcc/controlled-file/upload'
const TARGET_POSITION_PATH = '/dcc/controlled-file/positions'

function writeJson(name, data) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(path.join(RESULT_DIR, name), `${JSON.stringify(data, null, 2)}\n`, 'utf8')
}

function writeFixtures(runId) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  const sourcePath = path.join(RESULT_DIR, `${runId}-source.doc`)
  const stampedPdfPath = path.join(RESULT_DIR, `${runId}-stamped.pdf`)
  fs.writeFileSync(sourcePath, `DCC E2E source document ${runId}\n`, 'utf8')
  fs.writeFileSync(
    stampedPdfPath,
    [
      '%PDF-1.4',
      '1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj',
      '2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj',
      '3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj',
      'trailer<</Root 1 0 R>>',
      '%%EOF',
      ''
    ].join('\n'),
    'ascii'
  )
  return { sourcePath, stampedPdfPath }
}

function requirePrerequisites() {
  assert.equal(TENANT, '测试租户', 'DCC doc-control E2E must use 测试租户')
  assert.equal(USERNAME, 'aoteman', 'DCC doc-control E2E must start with aoteman')
  assert.ok(PASSWORD, 'DCC_DOC_CONTROL_E2E_PASSWORD is required')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC doc-control E2E must not target production')
}

function unwrap(payload, label) {
  assert.ok([0, 200].includes(payload?.code), `${label} business code ${payload?.code}: ${payload?.msg || ''}`)
  return payload.data
}

function formatDate(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatProjectCodeOptionLabel(project) {
  return [project.projectName, project.projectCode, project.docControlNo].filter(Boolean).join(' · ')
}

function buildTaxonomyPathMap(rows) {
  const byId = new Map((rows || []).map((row) => [Number(row.id), row]))
  const pathMap = new Map()
  const visit = (row, visiting = new Set()) => {
    const id = Number(row?.id)
    if (!id || pathMap.has(id) || visiting.has(id)) {
      return pathMap.get(id)
    }
    visiting.add(id)
    const parentId = Number(row.parentId || 0)
    const parentPathInfo = parentId > 0 ? visit(byId.get(parentId), visiting) : undefined
    if (parentId > 0 && !parentPathInfo) {
      return undefined
    }
    const pathInfo = {
      id,
      names: [...(parentPathInfo?.names || []), row.name],
      levelNo: Number(row.levelNo || (parentPathInfo?.names?.length || 0) + 1)
    }
    pathMap.set(id, pathInfo)
    return pathInfo
  }
  for (const row of rows || []) {
    if (row?.active) {
      visit(row)
    }
  }
  return pathMap
}

function selectTaxonomyPath(rows) {
  const pathMap = buildTaxonomyPathMap(rows)
  if (FILE_TYPE_TAXONOMY_ID > 0) {
    const selected = pathMap.get(FILE_TYPE_TAXONOMY_ID)
    assert.ok(
      selected && selected.names.length >= 3,
      `configured taxonomy must be active and at least level 3: ${FILE_TYPE_TAXONOMY_ID}`
    )
    return selected
  }
  const candidates = [...pathMap.values()]
    .filter((item) => item.names.length >= 3)
    .sort((left, right) => {
      const leftCodex = left.names.some((name) => String(name).includes('Codex')) ? 1 : 0
      const rightCodex = right.names.some((name) => String(name).includes('Codex')) ? 1 : 0
      return leftCodex - rightCodex || left.names.length - right.names.length || left.id - right.id
    })
  assert.ok(candidates.length > 0, 'DCC file type taxonomy must contain an active level-3 path')
  return candidates[0]
}

async function selectDccProjectAndFileTypeThroughUi(page) {
  const projectPage = await api(
    page,
    `/dcc/project-codes/page?pageNo=1&pageSize=20&status=ENABLE&keyword=${encodeURIComponent(PROJECT_KEYWORD)}`
  )
  const project = (projectPage.list || []).find((item) => item.status === 'ENABLE') || projectPage.list?.[0]
  assert.ok(project?.id, 'DCC project code page must return an enabled project')
  const projectLabel = formatProjectCodeOptionLabel(project)
  const projectItem = formItem(page, 'DCC项目')
  await projectItem.locator('.el-select').first().click()
  await projectItem.locator('input').first().fill(project.projectCode || project.projectName)
  const projectOption = page.locator('.el-select-dropdown__item:visible')
    .filter({ hasText: project.projectCode || project.projectName })
    .first()
  await projectOption.waitFor({ state: 'visible', timeout: 60000 })
  await projectOption.click()
  await page.getByText(projectLabel, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })

  const taxonomyRows = await api(page, '/dcc/file-type-taxonomies')
  const taxonomyPath = selectTaxonomyPath(taxonomyRows)
  const taxonomyItem = formItem(page, '文件分类')
  await taxonomyItem.locator('.el-cascader').first().click()
  for (const [index, label] of taxonomyPath.names.entries()) {
    const node = page.locator('.el-cascader-node:visible')
      .filter({ hasText: label })
      .last()
    await node.waitFor({ state: 'visible', timeout: 60000 })
    if (index === taxonomyPath.names.length - 1) {
      const strictSelector = node.locator('.el-radio, .el-checkbox').first()
      if (await strictSelector.count()) {
        await strictSelector.click()
      } else {
        await node.click()
      }
    } else {
      await node.click()
    }
    await page.waitForTimeout(200)
  }
  await page
    .waitForFunction(
      ({ labels }) => {
        const scope = document.querySelector('[data-testid="dcc-upload-section-scope"]')
        const scopeText = scope?.textContent || ''
        const inputValues = Array.from(scope?.querySelectorAll('input') || [])
          .map((input) => input.value || '')
          .join(' / ')
        const fullPath = labels.join(' / ')
        return scopeText.includes(fullPath) || inputValues.includes(labels[labels.length - 1])
      },
      { labels: taxonomyPath.names },
      { timeout: 60000 }
    )
  return {
    project: {
      id: project.id,
      projectName: project.projectName,
      projectCode: project.projectCode,
      label: projectLabel
    },
    taxonomy: taxonomyPath
  }
}

function sanitizeDccRequest(request) {
  const sanitized = {
    method: request.method(),
    url: request.url().replace(/\?.*$/, '')
  }
  const postData = request.postData()
  if (!postData || !request.url().includes('/approve-task')) {
    return sanitized
  }
  try {
    const parsed = JSON.parse(postData)
    sanitized.approvePayload = {
      taskId: parsed.taskId,
      hasPassword: Boolean(parsed.password),
      hasStampedPdfUploadTicket: Boolean(parsed.stampedPdfUploadTicket),
      hasSessionId: Boolean(parsed.sessionId),
      confirmedDirectoryId: parsed.confirmedDirectoryId,
      selectedDistributionScopes: parsed.selectedDistributionScopes || []
    }
  } catch (error) {
    sanitized.approvePayload = { parseError: error.message }
  }
  return sanitized
}

async function login(page, targetPath) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', targetPath)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first().click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
  await page.goto(`${BASE_URL}${targetPath}`, { waitUntil: 'domcontentloaded' })
}

async function api(page, url, options = {}) {
  const result = await page.evaluate(
    async ({ url, options }) => {
      const readWsCache = (key) => {
        const raw = window.localStorage.getItem(key)
        if (!raw) return undefined
        const cacheItem = JSON.parse(raw)
        if (!cacheItem || typeof cacheItem !== 'object' || !('v' in cacheItem)) return undefined
        return JSON.parse(cacheItem.v)
      }
      const accessToken = readWsCache('ACCESS_TOKEN')
      const tenantId = readWsCache('tenantId')
      const visitTenantId = readWsCache('visitTenantId')
      if (!accessToken || !tenantId) {
        throw new Error('missing authenticated browser cache')
      }
      const headers = {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId),
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(options.headers || {})
      }
      if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
      const response = await fetch(`/admin-api${url}`, {
        method: options.method || 'GET',
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
      })
      return {
        ok: response.ok,
        status: response.status,
        payload: await response.json()
      }
    },
    { url, options }
  )
  assert.equal(result.ok, true, `${options.method || 'GET'} ${url} HTTP ${result.status}`)
  return unwrap(result.payload, `${options.method || 'GET'} ${url}`)
}

async function verifyE2eRoute(page) {
  const routes = await api(page, `/dcc/approval-routes?categoryId=${CATEGORY_ID}`)
  assert.ok(routes.length > 0, `category ${CATEGORY_ID} must have an approval route`)
  const activeRoute = routes.find((route) => route.active) || routes[0]
  const routeAlreadyReady = activeRoute.nodes.every(
    (node) =>
      node.candidateSourceType === 'USER' &&
      (node.candidateSourceId === APPROVER_USER_ID ||
        (Array.isArray(node.candidateSourceIds) && node.candidateSourceIds.includes(APPROVER_USER_ID)))
  )
  assert.ok(
    routeAlreadyReady,
    `category ${CATEGORY_ID} route must be configured through formal user/route management before E2E`
  )
  const preview = await api(page, '/dcc/approval-routes/preview', {
    method: 'POST',
    body: { categoryId: CATEGORY_ID }
  })
  assert.ok(preview.length >= 4, 'route preview must include the four DCC approval stages')
  assert.ok(
    preview.every((node) => (node.resolvedUserIds || []).includes(APPROVER_USER_ID)),
    `route preview must resolve every stage to user ${APPROVER_USER_ID}`
  )
  return {
    categoryId: CATEGORY_ID,
    categoryName: CATEGORY_NAME,
    changed: false,
    previousRoute: {
      id: activeRoute.id,
      versionNo: activeRoute.versionNo,
      nodes: activeRoute.nodes.map((node) => ({
        stageNo: node.stageNo,
        stageName: node.stageName,
        candidateSourceType: node.candidateSourceType,
        candidateSourceId: node.candidateSourceId,
        candidateSourceIds: node.candidateSourceIds
      }))
    },
    preview: preview.map((node) => ({
      stageNo: node.stageNo,
      stageName: node.stageName,
      resolvedUserIds: node.resolvedUserIds
    }))
  }
}

function formItem(page, label) {
  return page
    .locator('.el-form-item')
    .filter({ has: page.locator('.el-form-item__label').filter({ hasText: label }) })
    .first()
}

async function selectOptionByFormLabel(page, label, optionText) {
  const item = formItem(page, label)
  await item.locator('.el-select, .el-cascader').first().click()
  await page.locator('.el-select-dropdown__item:visible, .el-cascader-node:visible').filter({ hasText: optionText }).first().click()
}

async function fillFormInput(page, label, value) {
  await formItem(page, label).locator('input').first().fill(value)
}

async function fillFormTextarea(page, label, value) {
  await formItem(page, label).locator('textarea').first().fill(value)
}

async function submitControlledFileThroughUi(page, fixtures, runId) {
  await page.goto(`${BASE_URL}${TARGET_UPLOAD_PATH}`, { waitUntil: 'domcontentloaded' })
  await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible' })
  const scopeSelection = await selectDccProjectAndFileTypeThroughUi(page)
  await selectOptionByFormLabel(page, '文件类别', CATEGORY_NAME)
  await page.getByText('当前绑定目录已经是最后一层目录', { exact: false }).first().waitFor({ state: 'visible' })

  const fileNumber = `CODEX-DCC-DEPT-${runId}`
  await fillFormInput(page, '文件名称', `部门下发范围E2E-${runId}`)
  await fillFormInput(page, '文件编号', fileNumber)
  await page.locator('.el-radio-button__inner').filter({ hasText: '新建' }).first().click()
  await fillFormInput(page, '版本号', 'V1.0')
  await fillFormInput(page, '生效日期', formatDate(new Date()))
  await fillFormTextarea(page, '提交备注', `Codex E2E department distribution ${runId}`)

  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/upload-preview') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page
    .locator('section[data-testid="dcc-upload-section-attachment"] input[type="file"]')
    .first()
    .setInputFiles(fixtures.sourcePath)
  const uploadResponse = await uploadResponsePromise
  assert.equal(uploadResponse.ok(), true, `source upload HTTP ${uploadResponse.status()}`)
  const uploadPayload = await uploadResponse.json()
  assert.ok([0, 200].includes(uploadPayload.code), `source upload business code ${uploadPayload.code}`)
  await page.getByText(path.basename(fixtures.sourcePath), { exact: false }).first().waitFor({ state: 'visible' })

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/submit') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '提交审批' }).click()
  const submitResponse = await submitResponsePromise
  assert.equal(submitResponse.ok(), true, `submit HTTP ${submitResponse.status()}`)
  const submitPayload = await submitResponse.json()
  const controlledFileId = unwrap(submitPayload, 'controlled-file submit')
  assert.ok(controlledFileId, 'submit must return controlled file id')
  return { controlledFileId, fileNumber, uploadPayload: uploadPayload.data, scopeSelection }
}

async function openApprovalDialog(page) {
  await page
    .getByRole('button', { name: /审核通过|批准通过/ })
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('button', { name: /审核通过|批准通过/ }).first().click()
  await page.locator('.el-dialog:visible').filter({ hasText: '签名' }).first().waitFor({ state: 'visible' })
}

async function selectDistributionDepartment(page) {
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '文件下发范围' }).first()
  const treeSelect = dialog.locator('[data-testid="dcc-doc-control-distribution-departments"]').first()
  await treeSelect.click()
  await treeSelect.locator('input').first().fill(DISTRIBUTION_DEPARTMENT_LABEL)
  const node = page.locator('.el-tree-node:visible').filter({ hasText: DISTRIBUTION_DEPARTMENT_LABEL }).first()
  await node.waitFor({ state: 'visible', timeout: 60000 })
  await node.locator('.el-checkbox').first().click()
  await dialog.locator('.el-dialog__header').click({ force: true })
  await page
    .waitForFunction(
      (label) =>
        !Array.from(document.querySelectorAll('.el-select-dropdown, .el-popper')).some((element) => {
          const style = window.getComputedStyle(element)
          return (
            element.textContent?.includes(label) &&
            style.display !== 'none' &&
            style.visibility !== 'hidden' &&
            element.getClientRects().length > 0
          )
        }),
      DISTRIBUTION_DEPARTMENT_LABEL,
      { timeout: 5000 }
    )
    .catch(() => undefined)
}

async function approveCurrentStage(page, controlledFileId, fixtures, stageIndex) {
  await page.goto(`${BASE_URL}/dcc/controlled-file/detail/${controlledFileId}`, { waitUntil: 'domcontentloaded' })
  await page.getByText('审批阶段进度', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await openApprovalDialog(page)
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '签名' }).first()
  await dialog.locator('input[type="password"]').first().fill(PASSWORD)
  await dialog.locator('textarea').first().fill(`Codex E2E approve stage ${stageIndex}`)

  if (stageIndex === 4) {
    await dialog.getByText('存入路径确认', { exact: false }).waitFor({ state: 'visible' })
    await dialog.getByText('文件下发范围', { exact: false }).waitFor({ state: 'visible' })
    const stampedResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/upload-preview') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.locator('input[type="file"]').first().setInputFiles(fixtures.stampedPdfPath)
    const stampedResponse = await stampedResponsePromise
    assert.equal(stampedResponse.ok(), true, `stamped PDF upload HTTP ${stampedResponse.status()}`)
    const stampedPayload = await stampedResponse.json()
    assert.ok([0, 200].includes(stampedPayload.code), `stamped PDF upload business code ${stampedPayload.code}`)
    await dialog.getByText(path.basename(fixtures.stampedPdfPath), { exact: false }).last().waitFor({ state: 'visible' })
    await selectDistributionDepartment(page)
  }

  const [approveResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/') &&
        response.url().includes('/approve-task') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    dialog.getByRole('button', { name: '确认签名' }).click()
  ])
  assert.equal(approveResponse.ok(), true, `approve stage ${stageIndex} HTTP ${approveResponse.status()}`)
  const approvePayload = await approveResponse.json()
  unwrap(approvePayload, `approve stage ${stageIndex}`)
  await page.locator('.el-dialog:visible').waitFor({ state: 'hidden', timeout: 60000 }).catch(() => {})
}

async function main() {
  requirePrerequisites()
  const runId = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
  const fixtures = writeFixtures(runId)
  const launchOptions = { headless: true, args: ['--disable-dev-shm-usage'] }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const dccWriteRequests = []
  const resultBase = {
    status: 'FAIL',
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    categoryId: CATEGORY_ID,
    categoryName: CATEGORY_NAME,
    distributionDepartmentLabel: DISTRIBUTION_DEPARTMENT_LABEL,
    runId
  }
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('request', (request) => {
      const method = request.method()
      const requestUrl = request.url()
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && requestUrl.includes('/admin-api/dcc/')) {
        dccWriteRequests.push(sanitizeDccRequest(request))
      }
    })

    await login(page, TARGET_POSITION_PATH)
    const routeSetup = await verifyE2eRoute(page)
    const submitted = await submitControlledFileThroughUi(page, fixtures, runId)
    for (const stageIndex of [1, 2, 3, 4]) {
      await approveCurrentStage(page, submitted.controlledFileId, fixtures, stageIndex)
    }
    const finalDetail = await api(page, `/dcc/controlled-files/${submitted.controlledFileId}`)
    const matchingDistribution = (finalDetail.distributionStatuses || []).find(
      (item) => item.departmentName === DISTRIBUTION_DEPARTMENT_LABEL || item.departmentId
    )
    assert.ok(matchingDistribution, 'final detail must include a generated distribution status')
    assert.ok(
      (matchingDistribution.recipientUserIds || []).length > 0 ||
        (matchingDistribution.recipients || []).length > 0,
      'generated distribution must include recipients'
    )
    const fourthApprovalWrite = dccWriteRequests
      .filter((item) => item.url.includes('/approve-task'))
      .map((item) => item.approvePayload)
      .find((payload) => (payload?.selectedDistributionScopes || []).length > 0)
    assert.ok(fourthApprovalWrite, 'fourth-node approve request must include selected distribution scopes')
    assert.ok(fourthApprovalWrite.confirmedDirectoryId, 'fourth-node approve request must include confirmed directory id')
    assert.ok(
      fourthApprovalWrite.selectedDistributionScopes.every(
        (scope) => scope.departmentId && ['PUBLIC_FOLDER', 'PAPER'].includes(scope.distributionMedium)
      ),
      'fourth-node approve request must include department id and distribution medium for every scope'
    )

    const result = {
      ...resultBase,
      status: 'PASS',
      routeSetup,
      submitted: {
        controlledFileId: submitted.controlledFileId,
        fileNumber: submitted.fileNumber,
        scopeSelection: submitted.scopeSelection
      },
      finalDetail: {
        id: finalDetail.id,
        fileNumber: finalDetail.fileNumber,
        versionNo: finalDetail.versionNo,
        status: finalDetail.status,
        distributionStatuses: finalDetail.distributionStatuses
      },
      fourthApprovalWrite,
      dccWriteRequests
    }
    writeJson('real-e2e-result.json', result)
    console.log(
      `PASS: DCC doc-control department distribution real E2E, fileId=${submitted.controlledFileId}, fileNumber=${submitted.fileNumber}`
    )
  } catch (error) {
    writeJson('real-e2e-result.json', {
      ...resultBase,
      status: 'FAIL',
      dccWriteRequests,
      error: error.message
    })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
