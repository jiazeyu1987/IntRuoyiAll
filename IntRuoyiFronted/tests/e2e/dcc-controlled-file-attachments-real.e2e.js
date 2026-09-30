const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_ATTACHMENTS_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_ATTACHMENTS_E2E_TENANT || '芋道源码'
const USERNAME = process.env.DCC_ATTACHMENTS_E2E_USERNAME || 'admin'
const PASSWORD = process.env.DCC_ATTACHMENTS_E2E_PASSWORD || 'admin123'
const OUTPUT_DIR = path.resolve(process.cwd(), 'test-results', 'dcc-controlled-file-attachments')
const EVIDENCE_PATH = path.join(OUTPUT_DIR, 'real-e2e-result.json')

function ensureFixtures(runId) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const sourcePath = path.join(OUTPUT_DIR, `${runId}-source.docx`)
  const controlledPdfPath = path.join(OUTPUT_DIR, `${runId}-controlled.pdf`)
  const attachmentOnePath = path.join(OUTPUT_DIR, `${runId}-attachment-one.txt`)
  const attachmentTwoPath = path.join(OUTPUT_DIR, `${runId}-attachment-two.pdf`)
  const sourceTemplatePath = path.resolve(
    __dirname,
    '..',
    '..',
    '..',
    'docs',
    'csv-validation',
    'SOP-EDHR-系统操作手册编写与批准模板.docx'
  )
  const pdfTemplatePath = path.resolve(
    __dirname,
    '..',
    '..',
    '..',
    'e2e_test',
    'registration',
    'upload',
    'upload_file.pdf'
  )
  assert.ok(fs.existsSync(sourceTemplatePath), `valid editable source fixture missing: ${sourceTemplatePath}`)
  assert.ok(fs.existsSync(pdfTemplatePath), `valid non-editable PDF fixture missing: ${pdfTemplatePath}`)
  fs.copyFileSync(sourceTemplatePath, sourcePath)
  fs.copyFileSync(pdfTemplatePath, controlledPdfPath)
  fs.writeFileSync(attachmentOnePath, `DCC plain attachment ${runId}\n`, 'utf8')
  fs.copyFileSync(pdfTemplatePath, attachmentTwoPath)
  return { sourcePath, controlledPdfPath, attachmentOnePath, attachmentTwoPath }
}

function writeEvidence(evidence) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  fs.writeFileSync(EVIDENCE_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

function todayString() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function runId() {
  return new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
}

function assertPrerequisites() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/, 'E2E must target local frontend')
  assert.equal(TENANT, '芋道源码', 'E2E tenant must be 芋道源码')
  assert.equal(USERNAME, 'admin', 'E2E username must be admin')
  assert.equal(PASSWORD, 'admin123', 'E2E password must be admin123')
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/dcc/controlled-file/upload`, { waitUntil: 'domcontentloaded' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first().click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}: ${loginPayload.msg || ''}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function api(page, url, options = {}) {
  const result = await page.evaluate(
    async ({ url, options }) => {
      const unwrap = (raw) => {
        let current = raw
        for (let index = 0; index < 8; index += 1) {
          if (!current) return ''
          if (typeof current === 'string') {
            try {
              current = JSON.parse(current)
              continue
            } catch {
              return current.replace(/^"(.*)"$/, '$1')
            }
          }
          if (current && typeof current === 'object') {
            if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) current = current.accessToken
            else if (Object.prototype.hasOwnProperty.call(current, 'v')) current = current.v
            else if (Object.prototype.hasOwnProperty.call(current, 'value')) current = current.value
            else return String(current)
            continue
          }
          return String(current)
        }
        return String(current || '')
      }
      const accessToken = unwrap(window.localStorage.getItem('ACCESS_TOKEN'))
      const tenantId = unwrap(window.localStorage.getItem('tenantId'))
      const visitTenantId = unwrap(window.localStorage.getItem('visitTenantId'))
      if (!accessToken || !tenantId) throw new Error('missing authenticated browser cache')
      const headers = {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId)
      }
      if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
      if (options.body) headers['Content-Type'] = 'application/json'
      const response = await fetch(`/admin-api${url}`, {
        method: options.method || 'GET',
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
      })
      return { ok: response.ok, status: response.status, payload: await response.json() }
    },
    { url, options }
  )
  assert.equal(result.ok, true, `${options.method || 'GET'} ${url} HTTP ${result.status}`)
  assert.ok([0, 200].includes(result.payload?.code), `${options.method || 'GET'} ${url} business code ${result.payload?.code}: ${result.payload?.msg || ''}`)
  return result.payload.data
}

function buildTaxonomyPath(rows, id) {
  const byId = new Map((rows || []).map((row) => [Number(row.id), row]))
  const names = []
  let current = byId.get(Number(id))
  const seen = new Set()
  while (current && !seen.has(Number(current.id))) {
    seen.add(Number(current.id))
    names.unshift(String(current.name || '').trim())
    current = Number(current.parentId || 0) > 0 ? byId.get(Number(current.parentId)) : null
  }
  return names.filter(Boolean)
}

function collectLeafDirectoryIds(tree) {
  if (!tree) return []
  if (tree.leafBinding && tree.bindingDirectoryId) return [Number(tree.bindingDirectoryId)]
  const result = []
  const walk = (nodes) => {
    for (const node of nodes || []) {
      if (Array.isArray(node.children) && node.children.length) walk(node.children)
      else if (node.id) result.push(Number(node.id))
    }
  }
  walk(tree.children || [])
  return result
}

function firstDirectoryLeafPath(tree) {
  const paths = []
  const walk = (nodes, prefix = []) => {
    for (const node of nodes || []) {
      const nextPath = [...prefix, String(node.name || '').trim()].filter(Boolean)
      if (Array.isArray(node.children) && node.children.length) {
        walk(node.children, nextPath)
      } else if (node.id) {
        paths.push(nextPath)
      }
    }
  }
  walk(tree?.children || [])
  paths.sort((left, right) => left.length - right.length || left.join('/').localeCompare(right.join('/')))
  return paths[0] || []
}

async function chooseScenario(page) {
  const [categories, taxonomies, projectPage] = await Promise.all([
    api(page, '/dcc/file-categories'),
    api(page, '/dcc/file-type-taxonomies'),
    api(page, '/dcc/project-codes/page?pageNo=1&pageSize=200&status=ENABLE')
  ])
  const categoryByTaxonomy = new Map()
  const stats = {
    projects: (projectPage.list || []).length,
    templateProjects: 0,
    templateItems: 0,
    validItems: 0,
    categoryMatches: 0,
    directoryMatches: 0,
    routeMatches: 0,
    routeWithResolvedUsers: 0
  }
  for (const category of categories || []) {
    if (category?.active !== false && category.fileTypeTaxonomyId) {
      categoryByTaxonomy.set(Number(category.fileTypeTaxonomyId), category)
    }
  }
  const projects = [...(projectPage.list || [])].sort(
    (left, right) => Number(Boolean(right.productMasterId)) - Number(Boolean(left.productMasterId))
  )
  for (const project of projects) {
    const template = await api(page, `/dcc/project-codes/${project.id}/file-template`).catch(() => null)
    if (template) stats.templateProjects += 1
    for (const item of template?.items || []) {
      stats.templateItems += 1
      if (!item.valid || !item.fileTypeTaxonomyId || !item.fileName) continue
      stats.validItems += 1
      const category = categoryByTaxonomy.get(Number(item.fileTypeTaxonomyId))
      if (!category?.id) continue
      stats.categoryMatches += 1
      const directoryTree = await api(page, `/dcc/controlled-files/upload-directory-tree?categoryId=${category.id}`).catch(() => null)
      if (collectLeafDirectoryIds(directoryTree).length < 1) continue
      stats.directoryMatches += 1
      stats.routeMatches += 1
      return {
        project,
        templateItem: item,
        category,
        taxonomyPath: buildTaxonomyPath(taxonomies, item.fileTypeTaxonomyId),
        directoryTree
      }
    }
  }
  throw new Error(`No usable DCC project template/category/directory candidate was found for admin E2E; stats=${JSON.stringify(stats)}`)
}

function formItem(page, label) {
  return page.locator('.el-form-item').filter({ has: page.locator('.el-form-item__label').filter({ hasText: label }) }).first()
}

async function selectProject(page, project) {
  const item = formItem(page, 'DCC项目')
  await item.locator('.el-select').first().click()
  await item.locator('input').first().fill(project.projectCode || project.projectName)
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: project.projectCode || project.projectName }).first().click()
}

async function selectTemplateItem(page, scenario) {
  await formItem(page, '阶段').locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: scenario.templateItem.stageName }).first().click()
  await formItem(page, '文件类型').locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: scenario.templateItem.fileTypeName }).first().click()
  const fileItem = formItem(page, '文件列表')
  await fileItem.locator('input').first().click()
  await fileItem.locator('input').first().fill(scenario.templateItem.fileName)
  await page.locator('.el-autocomplete-suggestion li:visible').filter({ hasText: scenario.templateItem.fileName }).first().click()
}

async function selectDirectoryIfNeeded(page, scenario) {
  if (scenario.directoryTree.leafBinding) {
    await page.getByText('当前绑定目录已经是最后一层目录', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    return
  }
  const leafPath = firstDirectoryLeafPath(scenario.directoryTree)
  assert.ok(leafPath.length > 0, 'scenario must contain a selectable directory leaf path')
  const item = formItem(page, '提交目录')
  await item.locator('.el-cascader').first().click()
  for (const [index, segment] of leafPath.entries()) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).first()
    await node.waitFor({ state: 'visible', timeout: 30000 })
    if (index === leafPath.length - 1) {
      const selector = node.locator('.el-radio__input, .el-checkbox__input').first()
      if (await selector.count()) {
        await selector.click({ force: true })
      } else {
        await node.click()
      }
    } else {
      await node.click()
    }
    await page.waitForTimeout(200)
  }
  await page.keyboard.press('Escape').catch(() => undefined)
  await page.getByText('最终提交路径：', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function addUniqueTemplateItemViaUi(page, scenario, fileName) {
  await page.goto(`${BASE_URL}/mdm/project-code?projectCodeId=${scenario.project.id}`, { waitUntil: 'domcontentloaded' })
  try {
    await page.getByText('基础数据 / DCC项目代码', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  } catch (error) {
    throw new Error(
      `${error.message}; template setup url=${page.url()}; body=${(await page.locator('body').innerText().catch(() => '')).slice(0, 2000)}`
    )
  }
  const detailDrawer = page.locator('[data-testid="dcc-project-code-detail-drawer"]').first()
  await page.waitForTimeout(500)
  try {
    await detailDrawer.waitFor({ state: 'visible', timeout: 60000 })
  } catch (error) {
    throw new Error(
      `${error.message}; detail setup url=${page.url()}; body=${(await page.locator('body').innerText().catch(() => '')).slice(-2500)}`
    )
  }
  await detailDrawer.locator('[data-testid="dcc-project-file-template-edit"]').click()
  const dialog = page.locator('[data-testid="dcc-project-file-template-dialog"]').first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  const addButton = dialog.locator('[data-testid="dcc-project-file-template-add-item"]')
  await addButton.click()
  const newRow = dialog.locator('.el-table__body-wrapper tbody tr').last()
  await newRow.locator('input').last().fill(fileName)
  await newRow.locator('.el-cascader').click()
  for (const [index, segment] of scenario.taxonomyPath.entries()) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).last()
    await node.waitFor({ state: 'visible', timeout: 30000 })
    await node.click()
    if (index < scenario.taxonomyPath.length - 1) await page.waitForTimeout(200)
  }
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/dcc/project-codes/${scenario.project.id}/file-template`) &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '保存模板' }).click()
  const saveResponse = await saveResponsePromise
  assert.equal(saveResponse.ok(), true, `template save HTTP ${saveResponse.status()}`)
  const savePayload = await saveResponse.json()
  assert.ok([0, 200].includes(savePayload.code), `template save business code ${savePayload.code}: ${savePayload.msg || ''}`)
  const refreshedTemplate = await api(page, `/dcc/project-codes/${scenario.project.id}/file-template`)
  const templateItem = (refreshedTemplate.items || []).find((item) => item.fileName === fileName)
  assert.ok(templateItem, `new template item ${fileName} must be visible after UI save`)
  scenario.templateItem = templateItem
  return scenario
}

async function fillInput(page, label, value) {
  await formItem(page, label).locator('input').first().fill(value)
}

async function fillTextarea(page, label, value) {
  await formItem(page, label).locator('textarea').first().fill(value)
}

async function uploadFileByInput(page, inputLocator, filePath, purposeLabel) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/upload-preview') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await inputLocator.setInputFiles(filePath)
  const response = await responsePromise
  const payload = await response.json()
  assert.equal(response.ok(), true, `${purposeLabel} upload HTTP ${response.status()}: ${payload.msg || ''}`)
  assert.ok([0, 200].includes(payload.code), `${purposeLabel} upload business code ${payload.code}: ${payload.msg || ''}`)
  await page.getByText(path.basename(filePath), { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return payload.data
}

async function main() {
  assertPrerequisites()
  const id = runId()
  const fixtures = ensureFixtures(id)
  const launchOptions = {
    headless: process.env.DCC_ATTACHMENTS_E2E_HEADLESS !== 'false',
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const evidence = {
    status: 'FAIL',
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    runId: id,
    fixtures: Object.fromEntries(Object.entries(fixtures).map(([key, value]) => [key, path.basename(value)]))
  }
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'zh-CN' })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const scenario = await chooseScenario(page)
    evidence.scenario = {
      projectId: scenario.project.id,
      projectCode: scenario.project.projectCode,
      categoryId: scenario.category.id,
      categoryName: scenario.category.name,
      templateItemId: scenario.templateItem.id,
      fileName: scenario.templateItem.fileName,
      taxonomyPath: scenario.taxonomyPath
    }

    await addUniqueTemplateItemViaUi(page, scenario, `CODEX-DCC-ATTACHMENTS-${id}.docx`)
    evidence.scenario.templateItemId = scenario.templateItem.id
    evidence.scenario.fileName = scenario.templateItem.fileName
    await page.goto(`${BASE_URL}/dcc/controlled-file/upload`, { waitUntil: 'domcontentloaded' })
    await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible' })
    await selectProject(page, scenario.project)
    await selectTemplateItem(page, scenario)
    await selectDirectoryIfNeeded(page, scenario)
    const fileNumber = `CODEX-ATT-${id}`
    await fillInput(page, '文件编号', fileNumber)
    await fillInput(page, '初始版本号', 'A/1')
    await fillInput(page, '生效日期', todayString())
    await fillTextarea(page, '提交备注', `Codex attachment real E2E ${id}`)

    const sourceInput = formItem(page, '受控文件').locator('input[type="file"]').first()
    const controlledPdfInput = formItem(page, '图纸 PDF').locator('input[type="file"]').first()
    const attachmentInput = formItem(page, '普通附件').locator('input[type="file"]').first()
    const sourceUpload = await uploadFileByInput(page, sourceInput, fixtures.sourcePath, 'source')
    const controlledPdfUpload = await uploadFileByInput(page, controlledPdfInput, fixtures.controlledPdfPath, 'non-editable controlled PDF')
    const attachmentOneUpload = await uploadFileByInput(page, attachmentInput, fixtures.attachmentOnePath, 'attachment one')
    const attachmentTwoUpload = await uploadFileByInput(page, attachmentInput, fixtures.attachmentTwoPath, 'attachment two')

    let submitRequest = null
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('/admin-api/dcc/controlled-files/submit')) {
        submitRequest = request.postDataJSON()
      }
    })
    const submitResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/submit') &&
        response.request().method() === 'POST',
      { timeout: 90000 }
    ).catch((error) => error)
    await page.getByRole('button', { name: '创建受控文件' }).click()
    const submitResponse = await submitResponsePromise
    if (submitResponse instanceof Error) {
      const visibleMessages = await page
        .locator('.el-message:visible,.el-alert:visible,.el-form-item__error:visible,[data-testid="dcc-upload-route-readiness"]')
        .allInnerTexts()
        .catch(() => [])
      evidence.submitBlockedVisibleMessages = visibleMessages
      evidence.submitBlockedBodyText = await page.locator('body').innerText().catch(() => '')
      await page.screenshot({ path: path.join(OUTPUT_DIR, `${id}-submit-blocked.png`), fullPage: true }).catch(() => undefined)
      throw new Error(`${submitResponse.message}; visibleMessages=${JSON.stringify(visibleMessages)}`)
    }
    const submitPayload = await submitResponse.json()
    assert.equal(submitResponse.ok(), true, `submit HTTP ${submitResponse.status()}`)
    assert.ok([0, 200].includes(submitPayload.code), `submit business code ${submitPayload.code}: ${submitPayload.msg || ''}`)
    const controlledFileId = submitPayload.data
    assert.ok(controlledFileId, 'submit must return controlled file id')
    assert.equal(submitRequest.attachmentUploadTickets.length, 2, 'submit payload must include two attachment tickets')
    assert.ok(submitRequest.attachmentUploadTickets.every((ticket) => ticket.uploadTicket && ticket.sessionId), 'each attachment ticket must include uploadTicket and sessionId')

    const submittedDetail = await api(page, `/dcc/controlled-files/${controlledFileId}`)
    assert.equal((submittedDetail.attachments || []).length, 2, 'detail API must return two attachments after UI submit')
    const detailQuery = new URLSearchParams({
      handling: 'approval',
      from: 'approval-center',
      processInstanceId: String(submittedDetail.processInstanceId || ''),
      taskId: `codex-${id}`
    })
    await page.goto(`${BASE_URL}/dcc/controlled-file/detail/${controlledFileId}?${detailQuery}`, { waitUntil: 'domcontentloaded' })
    const attachmentRegion = page.locator('[data-testid="dcc-detail-attachments"]').first()
    try {
      await attachmentRegion.waitFor({ state: 'visible', timeout: 90000 })
    } catch (error) {
      evidence.detailBlockedUrl = page.url()
      evidence.detailBlockedBodyText = await page.locator('body').innerText().catch(() => '')
      await page.screenshot({ path: path.join(OUTPUT_DIR, `${id}-detail-blocked.png`), fullPage: true }).catch(() => undefined)
      throw error
    }
    await attachmentRegion.getByText(path.basename(fixtures.attachmentOnePath), { exact: false }).waitFor({ state: 'visible' })
    await attachmentRegion.getByText(path.basename(fixtures.attachmentTwoPath), { exact: false }).waitFor({ state: 'visible' })
    await attachmentRegion.getByRole('button', { name: '在线查看' }).first().click()
    await page.locator('.el-dialog:visible').filter({ hasText: path.basename(fixtures.attachmentOnePath) }).first().waitFor({ state: 'visible', timeout: 60000 })

    evidence.status = 'PASS'
    evidence.submitted = {
      controlledFileId,
      fileNumber,
      sourceUploadFileName: sourceUpload.fileName,
      controlledPdfUploadFileName: controlledPdfUpload.fileName,
      attachmentUploadFileNames: [attachmentOneUpload.fileName, attachmentTwoUpload.fileName],
      submitAttachmentTicketCount: submitRequest.attachmentUploadTickets.length,
      detailAttachmentNames: (submittedDetail.attachments || []).map((attachment) => attachment.fileName)
    }
    writeEvidence(evidence)
    console.log(`PASS: DCC controlled file attachments real E2E fileId=${controlledFileId} evidence=${EVIDENCE_PATH}`)
  } catch (error) {
    evidence.error = error.message
    writeEvidence(evidence)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
