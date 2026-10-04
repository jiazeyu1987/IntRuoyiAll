/* Task-owned real UI acceptance. Preparing this file does not execute E2E. */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const frontendRequire = createRequire(path.resolve(__dirname, '../../..', 'IntRuoyiFronted/package.json'))
const { chromium } = frontendRequire('playwright')
const required = name => {
  const value = process.env[name]
  if (!value || !value.trim()) throw new Error(`BLOCKED: missing runtime input ${name}`)
  return value.trim()
}
const mode = process.argv[2]
if (!['preflight', 'browse-cancel', 'upload-cancel', 'references-cancel', 'reminders-read'].includes(mode))
  throw new Error('Choose preflight, browse-cancel, upload-cancel, references-cancel or reminders-read')
const base = new URL(required('DCC_E2E_FRONTEND_URL'))
assert.ok(['127.0.0.1', 'localhost'].includes(base.hostname) && base.port === '8067', 'Only registered slot6 frontend 8067 is allowed')
const tenant = required('DCC_E2E_TENANT')
const username = required('DCC_E2E_USERNAME')
const password = required('DCC_E2E_PASSWORD')
const menuPaths = JSON.parse(required('DCC_E2E_MENU_PATHS_JSON'))
const taskMarker = required('DCC_E2E_TASK_MARKER')
assert.match(taskMarker, /^20261001-dcc-integration[-_A-Za-z0-9]*$/, 'Task-owned marker must identify this integration task')
const artifactDir = path.join(__dirname, 'e2e-artifacts', `${mode}-${Date.now()}`)
fs.mkdirSync(artifactDir, { recursive: true })
const evidence = { scope: mode, status: 'RUNNING', expectedFrontendPort: 8067, expectedBackendPort: 48067,
  slot: 6, calendarZone: 'Asia/Shanghai', reminderAdvanceDays: 7, activationCheck: 'each minute',
  steps: [], naturalRequests: [], observedIssues: [], screenshots: [] }
let browser, context, page, traceStarted = false
const redact = value => String(value).split(password).join('[REDACTED]')
const screenshot = async label => {
  const filename = path.join(artifactDir, `${String(evidence.screenshots.length + 1).padStart(2, '0')}-${label}.png`)
  await page.screenshot({ path: filename, fullPage: true })
  evidence.screenshots.push(path.basename(filename))
}
const settle = async () => {
  await page.waitForLoadState('domcontentloaded')
  const masks = page.locator('.el-loading-mask:visible')
  while (await masks.count()) await masks.last().waitFor({ state: 'hidden', timeout: 45000 })
}
const expectVisible = async locator => {
  await locator.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await locator.count(), 1, 'Expected exactly one visible business target')
}
const openMenu = async key => {
  const labels = menuPaths[key]
  assert.ok(Array.isArray(labels) && labels.length && labels.every(label => typeof label === 'string' && label.trim()), `BLOCKED: supply actual visible menu label chain for ${key}`)
  for (const label of labels) {
    const item = page.locator('.el-menu-item:visible, .el-sub-menu__title:visible').filter({ has: page.getByText(label, { exact: true }) })
    await expectVisible(item)
    const submenuClass = await item.locator('..').getAttribute('class')
    if (!(submenuClass || '').includes('is-opened')) await item.click()
  }
  await settle()
  evidence.steps.push({ action: 'open menu', key, labels, pathname: new URL(page.url()).pathname })
}
const closeDialog = async (dialog, button = '取消') => {
  const cancel = dialog.getByRole('button', { name: button, exact: true })
  await expectVisible(cancel); await cancel.click(); await dialog.waitFor({ state: 'hidden' })
}
const chooseSelect = async (select, label) => {
  await expectVisible(select); await select.click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: new RegExp(`^${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`) })
  await expectVisible(option); await option.click(); await settle()
}
const formItem = (scope, label) => scope.locator('.el-form-item').filter({ has: scope.locator('.el-form-item__label').filter({ hasText: label }) })
const fillItem = async (scope, label, value) => {
  const item = formItem(scope, label)
  const input = item.locator('input:not([type=hidden]),textarea').filter({ visible: true })
  await expectVisible(input); await input.fill(value)
}
const businessMutationsSince = (offset, pattern) => evidence.naturalRequests.slice(offset).filter(item => item.method !== 'GET' && pattern.test(item.pathname))
const assertNoApplicationWrite = offset => assert.equal(businessMutationsSince(offset, /\/controlled-files\/submit$|\/project-product-requests(?:\/|$)|\/project-file-references(?:\/batch|\/cancel)?$/).length, 0, 'Cancel/read scenario must not create an application/reference write')
const selectedProjectFolder = async () => {
  const projectName = required('DCC_E2E_PROJECT_NAME'), folderName = required('DCC_E2E_FOLDER_NAME')
  assert.ok(projectName.includes(taskMarker), 'Use an existing UI-created task-owned project')
  const panel = page.getByTestId('dcc-project-browser'); await expectVisible(panel)
  await panel.getByPlaceholder('项目名称或编码').fill(projectName)
  await panel.getByRole('button', { name: '查找项目', exact: true }).click(); await settle()
  const projectRow = panel.locator('aside .el-table__body-wrapper tr').filter({ has: page.getByText(projectName, { exact: true }) })
  await expectVisible(projectRow); await projectRow.click(); await settle()
  const folder = panel.locator('aside .el-tree-node__content').filter({ has: page.getByText(folderName, { exact: true }) })
  await expectVisible(folder); await folder.click(); await settle()
  evidence.steps.push({ action: 'select project folder', projectName, folderName })
  return panel
}
const readBusinessAlerts = async scope => {
  const messages = await scope.locator('.el-alert--error:visible').allTextContents()
  assert.equal(messages.length, 0, `BLOCKED: visible business errors: ${messages.join(' | ')}`)
}
const login = async () => {
  await page.goto(new URL('/login', base).href)
  const form = page.locator('.login-form:visible'); await expectVisible(form)
  const tenantSelect = form.locator('.el-select:visible')
  await expectVisible(tenantSelect); await tenantSelect.click()
  const tenantInput = tenantSelect.locator('input:visible'); await expectVisible(tenantInput)
  await tenantInput.fill(tenant); await tenantInput.press('Enter')
  await form.getByPlaceholder(/用户名|账号/).fill(username)
  await form.locator('input[type=password]').fill(password)
  const remember = form.getByRole('checkbox', { name: /记住/ })
  if (await remember.count()) await remember.uncheck()
  await form.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 45000 })
  assert.equal(await page.getByTestId('required-password-change-form').count(), 0, 'BLOCKED: account requires formal password change')
  await context.tracing.start({ screenshots: true, snapshots: true, sources: false })
  traceStarted = true
}
async function preflight() {
  const targets = {
    users: ['用户'], departments: ['部门'], roles: ['角色'],
    signature: ['电子签名'], positions: ['审批角色'], routes: ['新增路线'],
    fileTypes: ['DCC文件分类'], projectCodes: ['项目'], productCatalog: ['产品'],
    upload: ['受控文件提交'], browser: ['项目文件与引用'], workbench: ['DCC 工作台']
  }
  const offset = evidence.naturalRequests.length
  for (const [key, titles] of Object.entries(targets)) {
    await openMenu(key)
    for (const title of titles) assert.ok((await page.locator('body').innerText()).includes(title), `BLOCKED: ${key} entry text missing ${title}`)
    await readBusinessAlerts(page); await screenshot(`preflight-${key}`)
    if (key === 'productCatalog') {
      const create = page.getByTestId('dcc-project-product-create-open'); await expectVisible(create); await create.click(); await settle()
      const dialog = page.getByRole('dialog', { name: '新建项目代码及产品', exact: true }); await expectVisible(dialog)
      for (const field of ['项目负责人', '目录模板', '本次审核人']) assert.ok((await dialog.innerText()).includes(field), `BLOCKED: project creation lacks ${field}`)
      await screenshot('project-create-preconditions'); await closeDialog(dialog)
      const reviewer = page.getByTestId('dcc-project-reviewer-config-open')
      if (await reviewer.isVisible()) {
        await reviewer.click(); const config = page.getByRole('dialog', { name: '项目及产品创建审核人', exact: true }); await expectVisible(config)
        assert.ok(!(await config.innerText()).includes('尚未配置'), 'BLOCKED: project reviewer is not configured')
        await screenshot('reviewer-configuration'); await closeDialog(config)
      } else evidence.observedIssues.push('Current account cannot maintain project reviewer; doc_control + update must be verified separately')
    }
  }
  assertNoApplicationWrite(offset)
  evidence.steps.push({ action: 'preflight only', boundary: 'visible entry existence; no account/signature/matrix/template write validation' })
}
async function browseCancel() {
  await openMenu('browser'); const panel = await selectedProjectFolder(); const offset = evidence.naturalRequests.length
  const version = panel.getByTestId('dcc-project-browser-version-view')
  assert.ok((await version.innerText()).includes('最新受控版本'))
  for (const choice of ['全部版本', '工作小版本', '审批中 · 会签', '历史版本 · 已作废', '最新受控版本']) {
    await chooseSelect(version, choice); await readBusinessAlerts(panel); await screenshot(`view-${choice.replace(/[ ·]/g, '-')}`)
  }
  const number = required('DCC_E2E_FILE_NUMBER')
  const row = panel.locator('main > .el-table .el-table__body-wrapper tr').filter({ has: page.getByText(number, { exact: true }) })
  await expectVisible(row)
  await row.getByTestId('dcc-project-file-relations').click(); await settle()
  const relations = page.getByRole('dialog', { name: '文件关联', exact: true }); await expectVisible(relations); await readBusinessAlerts(relations)
  const link = relations.getByRole('button', { name: '关联', exact: true })
  const selector = page.getByRole('dialog', { name: '文件选择器 · 关联文件', exact: true })
  if (await selector.isVisible() || await link.isVisible()) {
    if (!await selector.isVisible()) await link.click()
    await expectVisible(selector)
    await expectVisible(selector.getByTestId('current-file')); await expectVisible(selector.getByTestId('dcc-selector-project-list'))
    await screenshot('relation-cross-project-window'); await closeDialog(selector)
  } else evidence.observedIssues.push('Current source is read-only for relation modification; not a write-path PASS')
  await closeDialog(relations, '关闭')
  await row.getByTestId('dcc-project-reference-usage').click(); await settle()
  const usage = page.getByRole('dialog', { name: '引用使用明细', exact: true }); await expectVisible(usage); await readBusinessAlerts(usage)
  assert.ok((await usage.innerText()).includes('全部引用项目')); assert.ok((await usage.innerText()).includes('可查看引用项目'))
  await screenshot('reference-usage'); await closeDialog(usage, '关闭')
  assertNoApplicationWrite(offset)
}
async function uploadCancel() {
  await openMenu('upload')
  const form = page.locator('.upload-form:visible'); await expectVisible(form)
  const projectName = required('DCC_E2E_PROJECT_NAME'); assert.ok(projectName.includes(taskMarker))
  await chooseSelect(formItem(form, 'DCC项目').locator('.el-select'), required('DCC_E2E_PROJECT_OPTION_LABEL'))
  const folderName = required('DCC_E2E_FOLDER_NAME')
  const folderSelect = formItem(form, '项目文件夹').locator('.el-select'); await folderSelect.click()
  const folderNode = page.locator('.el-select-dropdown:visible .el-tree-node__content').filter({ has: page.getByText(folderName, { exact: true }) })
  await expectVisible(folderNode); await folderNode.click()
  await fillItem(form, '登记说明', `${taskMarker} UI acceptance placement`)
  await chooseSelect(formItem(form, '阶段').locator('.el-select'), required('DCC_E2E_TEMPLATE_STAGE'))
  await chooseSelect(formItem(form, '文件类型').locator('.el-select'), required('DCC_E2E_TEMPLATE_TYPE'))
  const filename = required('DCC_E2E_TEMPLATE_FILE_NAME')
  await form.getByTestId('dcc-upload-project-template-file-list').locator('input').fill(filename)
  const suggestion = page.locator('.el-autocomplete-suggestion:visible li').filter({ hasText: filename }); await expectVisible(suggestion); await suggestion.click()
  const number = required('DCC_E2E_FILE_NUMBER'); assert.ok(number.includes(taskMarker))
  await fillItem(form, '文件编号', number); await fillItem(form, '初始版本号', 'A/1')
  const effectiveDate = required('DCC_E2E_EFFECTIVE_DATE'); assert.match(effectiveDate, /^\d{4}-\d{2}-\d{2}$/)
  await fillItem(form, '生效日期', effectiveDate)
  const inputFile = path.resolve(required('DCC_E2E_UPLOAD_FILE')); assert.ok(fs.existsSync(inputFile), 'Provide a real task-owned supported source file')
  const source = formItem(form, '受控文件').locator('input[type=file]')
  await expectVisible(formItem(form, '受控文件')); assert.equal(await source.count(), 1); await source.setInputFiles(inputFile)
  await settle(); await readBusinessAlerts(form)
  await expectVisible(page.getByTestId('dcc-upload-matrix-approvers'))
  const offset = evidence.naturalRequests.length
  await form.getByRole('button', { name: '创建受控文件', exact: true }).click()
  const confirm = page.getByTestId('dcc-upload-submit-confirmation'); await expectVisible(confirm)
  const summary = await confirm.innerText()
  for (const expected of [projectName, folderName, path.basename(inputFile), number, 'A/1', effectiveDate, '会签部门', '批准人', '受控日期由系统记录']) assert.ok(summary.includes(expected), `Confirmation lacks ${expected}`)
  await screenshot('upload-complete-confirmation')
  await page.locator('.el-message-box:visible').getByRole('button', { name: '取消', exact: true }).click()
  assert.equal(await formItem(form, '文件编号').locator('input').inputValue(), number)
  assertNoApplicationWrite(offset)
  evidence.observedIssues.push('Upload preview created real temporary assets; cancellation proved zero application submission, not zero temporary storage writes. Clean only through the existing UI leave/remove path.')
  await openMenu('browser')
  await expectVisible(page.getByTestId('dcc-project-browser'))
  evidence.steps.push({ action: 'leave upload through real browser menu', boundary: 'official route-leave temporary session cleanup; no direct cleanup interface call' })
}
async function referencesCancel() {
  await openMenu('browser'); const panel = await selectedProjectFolder(); const offset = evidence.naturalRequests.length
  const reference = panel.getByRole('button', { name: '引用', exact: true }); await expectVisible(reference); await reference.click()
  const selector = page.getByRole('dialog', { name: '文件选择器 · 引用文件', exact: true }); await expectVisible(selector)
  await expectVisible(selector.getByTestId('dcc-selector-project-list')); await screenshot('reference-chooser'); await closeDialog(selector)
  assertNoApplicationWrite(offset)
  evidence.steps.push({ action: 'reference chooser cancel', boundary: 'no saved reference/cancel write and no counterfeit project-leader assertion' })
}
async function remindersRead() {
  await openMenu('workbench'); const panel = page.getByTestId('dcc-workflow-pending-distribution-list'); await expectVisible(panel)
  const offset = evidence.naturalRequests.length
  await panel.getByRole('button', { name: '刷新待处置列表', exact: true }).click(); await settle(); await readBusinessAlerts(panel)
  const dates = await panel.locator('.el-table__body-wrapper tr td:nth-child(5)').allTextContents()
  assert.deepEqual([...dates].sort(), dates, 'Preset effective dates must be ascending')
  await panel.getByRole('checkbox', { name: '仅显示到期、逾期及按配置临期事项', exact: true }).check(); await settle(); await readBusinessAlerts(panel)
  await screenshot('workbench-reminders')
  assert.equal(businessMutationsSince(offset, /workflow-lifecycle\/.*\/distribute$/).length, 0)
  evidence.observedIssues.push(dates.length ? 'Read/ordering only; actual due boundary, seven-day configuration and distribution completion need separate UI lifecycle sequence' : 'No task-owned pending distribution rows; no reminder/business execution PASS claimed')
}
;(async () => {
  try {
    browser = await chromium.launch({ headless: process.env.DCC_E2E_HEADED !== 'yes' })
    context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, timezoneId: 'Asia/Shanghai' })
    page = await context.newPage()
    page.on('request', request => { const url = new URL(request.url()); if (url.origin === base.origin) evidence.naturalRequests.push({ method: request.method(), pathname: url.pathname }) })
    page.on('pageerror', error => evidence.observedIssues.push(redact(error.message)))
    await login()
    await ({ preflight, 'browse-cancel': browseCancel, 'upload-cancel': uploadCancel, 'references-cancel': referencesCancel, 'reminders-read': remindersRead })[mode]()
    evidence.status = 'PASS_SCOPED_UI_ONLY'
  } catch (error) {
    evidence.status = 'BLOCKED_OR_FAILED'; evidence.failure = redact(error.message)
    process.exitCode = 1
  } finally {
    if (traceStarted) await context.tracing.stop({ path: path.join(artifactDir, 'trace.zip') })
    fs.writeFileSync(path.join(artifactDir, 'result.json'), JSON.stringify(evidence, null, 2), 'utf8')
    await browser?.close()
    process.stdout.write(`${evidence.status}: ${mode}; evidence ${artifactDir}\n`)
  }
})()
