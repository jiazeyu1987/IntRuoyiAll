/* Root-executed real UI runner. Import/validate-only starts no browser or network. */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const H = require('./g27-public-lifecycle-ui-helpers.cjs')
const HELPER_SHA = '37f8c1e482dc1b5ee0f693d6467a0f4945c158023a6bc8dbaffb0f413316bef0'
const MODES = ['project-product', 'upload-submit', 'native-task', 'training', 'distribution', 'version', 'relations', 'reference', 'obsolete']
const SUPPORTED = new Set(['project-product', 'upload-submit', 'native-task', 'training'])
const MARKER = /^20261001-dcc-integration[-_A-Za-z0-9]+$/
const fail = code => { const e = new Error(code); e.safeCode = code; throw e }
function text(v, key) { if (typeof v !== 'string' || !v.trim() || v.length > 512 || /[\x00-\x1f]/.test(v)) fail(`INVALID_${key.toUpperCase()}`); return v }
function own(c, v, key) { if (!text(v, key).includes(c.taskMarker)) fail(`NOT_TASK_OWNED_${key.toUpperCase()}`) }
function reason(v) { if (text(v, 'reason').length > 500) fail('REASON_OVERFLOW') }
function noSecrets(v) { if (Buffer.isBuffer(v) || ArrayBuffer.isView(v)) fail('SERIALIZED_RUNTIME_BYTES'); if (Array.isArray(v)) return v.forEach(noSecrets); if (v && typeof v === 'object') for (const [k, value] of Object.entries(v)) { if (/password|secret|token|cookie|credential|storageState|pageHandle|browserHandle/i.test(k)) fail('SERIALIZED_CREDENTIAL_FIELD'); noSecrets(value) } }
function file(p) { text(p, 'filePath'); if (!path.isAbsolute(p) || !fs.existsSync(p) || !fs.statSync(p).isFile()) fail('ACTUAL_FILE_REQUIRED') }
function attrs(a) { if (!a || !Array.isArray(a.markets) || !a.markets.length || new Set(a.markets).size !== a.markets.length || !a.markets.every(x => ['NMPA 国内', 'CE 欧盟', 'FDA', 'MADSAP', '其他', '不适用'].includes(x)) || !['是', '否', '不适用'].includes(a.licenseHolder) || !['是', '否', '不适用'].includes(a.actualManufacturer) || !['是', '否'].includes(a.documentTransfer)) fail('INVALID_EXPLICIT_ATTRIBUTES'); if (a.markets.includes('其他')) text(a.otherMarket, 'otherMarket'); if (a.documentTransfer === '是') text(a.transferTo, 'transferTo') }
function validate(c, mode) {
  noSecrets(c)
  if (!MODES.includes(mode) || c?.mode !== mode) fail('INVALID_MODE')
  if (!SUPPORTED.has(mode)) fail(`BLOCKED_UNIMPLEMENTED_${mode.toUpperCase()}`)
  if (!MARKER.test(c.taskMarker || '')) fail('INVALID_TASK_MARKER')
  const common = ['mode', 'taskMarker', 'frontendUrl', 'tenant', 'actorDisplayLabel', 'menuLabels', 'postconditions']
  const fields = { 'project-product': ['action', 'projectCode', 'productCode', 'reason', 'expectedSavedStatus', 'projectName', 'productName', 'leaderOptionLabel', 'folderTemplateLabel', 'reviewerOptionLabel', 'classification', 'attributes', 'remark'], 'upload-submit': ['projectName', 'projectOptionLabel', 'folderName', 'registrationReason', 'stageLabel', 'typeLabel', 'templateFileName', 'approverText', 'fileNumber', 'versionNo', 'effectiveDate', 'needTraining', 'departmentLabels', 'filePath', 'defaultAttributes', 'attributes', 'expectedSavedStatus'], 'native-task': ['action', 'projectName', 'folderName', 'versionView', 'fileNumber', 'versionNo', 'reason', 'stageLabel', 'expectedTaskId', 'expectedBpmId', 'historyRoundLabel', 'openActionButton', 'dialogTitle', 'decision', 'ownerRequired', 'ownerOptionLabel', 'departmentLabel', 'assigneeOptionLabel', 'stampedPdfPath', 'confirmedDirectoryLabel'], training: ['projectName', 'folderName', 'versionView', 'fileNumber', 'versionNo', 'filePath', 'expectedBpmId'] }
  const allowed = new Set([...common, ...fields[mode]]); if (Object.keys(c).some(k => !allowed.has(k))) fail('UNKNOWN_CONTRACT_FIELD')
  const u = new URL(text(c.frontendUrl, 'frontendUrl'))
  if (u.protocol !== 'http:' || !['127.0.0.1', 'localhost'].includes(u.hostname) || u.port !== '8067' || u.username || u.password || u.pathname !== '/' || u.search || u.hash) fail('INVALID_FRONTEND_SLOT')
  for (const k of ['tenant', 'actorDisplayLabel']) text(c[k], k)
  if (!Array.isArray(c.menuLabels) || !c.menuLabels.length) fail('ACTUAL_MENU_CHAIN_REQUIRED'); c.menuLabels.forEach(x => text(x, 'menuLabel'))
  if (!Array.isArray(c.postconditions) || !c.postconditions.length) fail('VISIBLE_POSTCONDITIONS_REQUIRED')
  for (const p of c.postconditions) { if (!p || Object.keys(p).some(k => !['scope', 'text', 'exact'].includes(k)) || typeof p.exact !== 'boolean') fail('INVALID_POSTCONDITION_FIELDS'); text(p.text, 'postconditionText'); if (!['body', 'detail', 'history', 'training', 'project-records'].includes(p.scope)) fail('INVALID_POSTCONDITION_SCOPE') }
  if (mode === 'project-product') {
    if (!['create', 'review', 'approve'].includes(c.action)) fail('INVALID_PROJECT_ACTION')
    for (const k of ['projectCode', 'productCode', 'reason']) own(c, c[k], k)
    reason(c.reason)
    text(c.expectedSavedStatus, 'expectedSavedStatus')
    if (!['PENDING_REVIEW', 'PENDING_APPROVAL', 'COMPLETED', 'WRITE_FAILED', 'REJECTED'].includes(c.expectedSavedStatus) || !c.postconditions.some(p => p.scope === 'project-records' && p.text === c.expectedSavedStatus && p.exact === true)) fail('FORMAL_SAVED_POSTCONDITION_REQUIRED')
    if (c.action === 'create') { for (const k of ['projectName', 'productName']) own(c, c[k], k); for (const k of ['leaderOptionLabel', 'folderTemplateLabel', 'reviewerOptionLabel', 'classification']) text(c[k], k); attrs(c.attributes) }
    const expected = { create: 'PENDING_REVIEW', review: 'PENDING_APPROVAL', approve: 'COMPLETED' }; if (c.expectedSavedStatus !== expected[c.action]) fail('PROJECT_EXPECTED_SUCCESS_STATUS_MISMATCH')
  } else {
    own(c, c.fileNumber, 'fileNumber'); text(c.versionNo, 'versionNo')
    if (mode === 'upload-submit') {
      for (const k of ['projectName', 'registrationReason']) own(c, c[k], k)
      reason(c.registrationReason)
      for (const k of ['projectOptionLabel', 'folderName', 'stageLabel', 'typeLabel', 'templateFileName', 'approverText']) text(c[k], k)
      if (!/^[A-Z]\/1$/.test(c.versionNo) || !/^\d{4}-\d{2}-\d{2}$/.test(c.effectiveDate || '')) fail('EXPLICIT_INITIAL_VERSION_DATE_REQUIRED')
      const date = new Date(`${c.effectiveDate}T00:00:00Z`); if (!Number.isFinite(date.getTime()) || date.toISOString().slice(0, 10) !== c.effectiveDate) fail('INVALID_EFFECTIVE_CALENDAR_DATE')
      if (typeof c.needTraining !== 'boolean' || !Array.isArray(c.departmentLabels) || !c.departmentLabels.length) fail('EXPLICIT_TRAINING_DEPARTMENTS_REQUIRED')
      c.departmentLabels.forEach(x => text(x, 'departmentLabel')); file(c.filePath); own(c, path.basename(c.filePath), 'sourceFilename'); attrs(c.attributes); attrs(c.defaultAttributes)
      text(c.expectedSavedStatus, 'expectedSavedStatus'); if (c.defaultAttributes.markets.includes('其他')) text(c.defaultAttributes.otherMarket, 'defaultOtherMarket')
      if (!c.postconditions.some(p => p.text.includes(c.fileNumber)) || !c.postconditions.some(p => p.text === c.expectedSavedStatus && p.exact === true)) fail('UPLOAD_SAVED_IDENTITY_STATUS_POSTCONDITIONS_REQUIRED')
    } else {
      own(c, c.projectName, 'projectName'); for (const k of ['folderName', 'versionView']) text(c[k], k)
      if (mode === 'training') { file(c.filePath); own(c, path.basename(c.filePath), 'trainingFilename'); text(c.expectedBpmId, 'expectedBpmId') }
      else { if (!['sign', 'assign'].includes(c.action)) fail('INVALID_NATIVE_ACTION'); own(c, c.reason, 'reason'); reason(c.reason); text(c.stageLabel, 'stageLabel'); text(c.expectedTaskId, 'expectedTaskId'); text(c.expectedBpmId, 'expectedBpmId'); text(c.historyRoundLabel, 'historyRoundLabel'); if (!/^[A-Za-z0-9_-]+$/.test(c.expectedTaskId) || !/^[A-Za-z0-9_-]+$/.test(c.expectedBpmId)) fail('INVALID_EXACT_TASK_BPM_STRINGS'); if (c.action === 'sign') { for (const k of ['openActionButton', 'dialogTitle']) text(c[k], k); if (!['approve', 'reject'].includes(c.decision)) fail('INVALID_SIGNATURE_DECISION'); if (c.stampedPdfPath) file(c.stampedPdfPath); if (typeof c.ownerRequired !== 'boolean') fail('EXPLICIT_OWNER_REQUIREMENT'); if (c.ownerRequired) text(c.ownerOptionLabel, 'ownerOptionLabel') } else { for (const k of ['departmentLabel', 'assigneeOptionLabel']) text(c[k], k) } }
      if (mode === 'training' && !c.postconditions.some(p => p.scope === 'training' && p.text.includes(path.basename(c.filePath)))) fail('CURRENT_TRAINING_RECORD_POSTCONDITION_REQUIRED')
      if (mode === 'native-task' && (!c.postconditions.some(p => p.scope === 'history' && p.text.includes(c.expectedBpmId)) || !c.postconditions.some(p => p.scope === 'history' && p.text === c.actorDisplayLabel))) fail('ACTUAL_SIGNED_HISTORY_POSTCONDITION_REQUIRED')
    }
  }
  return c
}
const escape = v => v.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
async function settle(page) { const m = page.locator('.el-loading-mask:visible'); while (await m.count()) await m.last().waitFor({ state: 'hidden', timeout: 45000 }) }
async function click(page, l) { await H.clickReady(l); await settle(page) }
async function menus(page, labels) { for (const name of labels) { const l = page.locator('.el-menu-item:visible,.el-sub-menu__title:visible').filter({ hasText: new RegExp(`^${escape(name)}$`) }); await H.uniqueVisible(l); if (!(await l.locator('..').getAttribute('class') || '').includes('is-opened')) await l.click() } await settle(page) }
async function row(table, values) { let l = table.locator('.el-table__body-wrapper tr:visible'); for (const v of values) l = l.filter({ hasText: v }); return H.uniqueVisible(l) }
async function errors(page) { if (await page.locator('.el-alert--error:visible,.el-message--error:visible').count()) fail('VISIBLE_BUSINESS_ERROR') }
async function post(page, c) {
  const scopes = { body: page.locator('body'), detail: page.getByTestId('dcc-detail-formal-version-facts'), history: page.getByTestId('dcc-detail-application-history'), training: page.getByTestId('dcc-detail-training-record-evidence'), 'project-records': page.getByRole('dialog', { name: /项目申请与审批|新建项目代码及产品/ }) }
  for (const p of c.postconditions) { const l = scopes[p.scope]; await H.uniqueVisible(l); await l.getByText(p.text, { exact: p.exact === true }).waitFor({ state: 'visible', timeout: 45000 }) } await errors(page)
}
async function selectedHistory(page, c) { const history = page.getByTestId('dcc-detail-application-history'); await H.uniqueVisible(history); const select = history.locator('.el-select'); if (await select.getByRole('combobox').isEnabled()) { await H.choose(page, select, c.historyRoundLabel); await settle(page) } else { const value = await select.getByRole('combobox').inputValue(); if (value !== c.historyRoundLabel && !(await select.innerText()).includes(c.historyRoundLabel)) fail('LOCKED_HISTORY_ROUND_MISMATCH') } if (!(await history.innerText()).includes(c.expectedBpmId)) fail('ACTUAL_HISTORY_BPM_MISMATCH') }
async function project(page, c, state) {
  if (c.action === 'create') {
    await H.prepareProjectProduct(page, c.taskMarker, { ...c, creationReason: c.reason }); state.step = 'PROJECT_CONFIRMATION'; state.mutationAttempted = true
    await click(page, page.locator('.el-message-box:visible').getByRole('button', { name: '提交申请', exact: true }))
    await page.reload(); await settle(page); await menus(page, c.menuLabels); await click(page, page.getByTestId('dcc-project-product-records-open'))
  } else {
    await click(page, page.getByTestId('dcc-project-product-records-open')); const dialog = page.getByRole('dialog', { name: '项目申请与审批', exact: true }); const l = await row(dialog, [c.projectCode, c.productCode]); const pending = page.waitForEvent('dialog')
    await l.getByRole('button', { name: c.action === 'review' ? '审核通过' : '批准通过', exact: true }).click(); const prompt = await pending
    if (prompt.type() !== 'prompt' || prompt.message() !== '请输入通过意见') { await prompt.dismiss(); fail('UNEXPECTED_NATIVE_PROMPT') }
    state.mutationAttempted = true; await prompt.accept(c.reason); await settle(page); await page.reload(); await settle(page); await menus(page, c.menuLabels); await click(page, page.getByTestId('dcc-project-product-records-open'))
  }
  const records = page.getByRole('dialog', { name: /项目申请与审批|新建项目代码及产品/ }); const saved = await row(records, [c.projectCode, c.productCode]); await saved.getByText(c.expectedSavedStatus, { exact: true }).waitFor({ state: 'visible' })
  await post(page, c); state.step = 'PROJECT_SAVED_ROW_DOM_VERIFIED'
}
async function defaultAttrs(scope, a) { for (const m of ['NMPA 国内', 'CE 欧盟', 'FDA', 'MADSAP', '其他', '不适用']) if (await H.item(scope, '目标市场').getByRole('checkbox', { name: m, exact: true }).isChecked() !== a.markets.includes(m)) fail('DEFAULT_MARKET_MISMATCH'); for (const [label, k] of [['是否为注册人', 'licenseHolder'], ['是否为生产方', 'actualManufacturer'], ['文件转移', 'documentTransfer']]) if (!await H.item(scope, label).getByRole('radio', { name: a[k], exact: true }).isChecked()) fail('DEFAULT_ATTRIBUTES_MISMATCH'); if (a.markets.includes('其他') && await H.item(scope, '其他市场说明').locator('input').inputValue() !== a.otherMarket) fail('DEFAULT_OTHER_MARKET_MISMATCH'); if (a.documentTransfer === '是' && await H.item(scope, '转移至').locator('input').inputValue() !== a.transferTo) fail('DEFAULT_TRANSFER_MISMATCH') }
async function upload(page, c, state) {
  const f = page.locator('.upload-form:visible'); await H.uniqueVisible(f); await H.choose(page, H.item(f, 'DCC项目').locator('.el-select'), c.projectOptionLabel); await settle(page)
  const a = H.item(f, '申请属性'); await defaultAttrs(a, c.defaultAttributes); await H.attributes(a, c.attributes)
  await click(page, H.item(f, '项目文件夹').locator('.el-select')); await click(page, page.locator('.el-select-dropdown:visible .el-tree-node__content').filter({ hasText: new RegExp(`^${escape(c.folderName)}$`) }))
  await H.fillItem(f, '登记说明', c.registrationReason); await H.choose(page, H.item(f, '阶段').locator('.el-select'), c.stageLabel); await settle(page); await H.choose(page, H.item(f, '文件类型').locator('.el-select'), c.typeLabel); await settle(page)
  const name = f.getByTestId('dcc-upload-project-template-file-list').locator('input'); await H.uniqueVisible(name); await name.fill(c.templateFileName); await click(page, page.locator('.el-autocomplete-suggestion:visible li').filter({ hasText: c.templateFileName }))
  await H.fillItem(f, '文件编号', c.fileNumber); await H.fillItem(f, '初始版本号', c.versionNo); await H.fillItem(f, '生效日期', c.effectiveDate)
  const source = H.item(f, '受控文件').locator('input[type=file]'); if (await source.count() !== 1) fail('AMBIGUOUS_SOURCE_INPUT'); state.artifactUploadAttempted = true; await source.setInputFiles(c.filePath); await settle(page); await errors(page)
  const d = f.getByTestId('dcc-upload-signoff-departments').locator('.el-select'); for (const x of await f.getByTestId('dcc-upload-signoff-departments').locator('.el-tag__close').all()) await x.click()
  for (const name of c.departmentLabels) { await H.choose(page, d, name); await d.press('Escape') }
  await f.getByTestId('dcc-upload-need-training').setChecked(c.needTraining); await settle(page); if (!(await f.getByTestId('dcc-upload-matrix-approvers').innerText()).includes(c.approverText)) fail('MATRIX_APPROVER_MISMATCH')
  await click(page, f.getByRole('button', { name: '创建受控文件', exact: true })); const confirmation = page.getByTestId('dcc-upload-submit-confirmation'); await H.uniqueVisible(confirmation); const facts = await confirmation.innerText()
  for (const value of [c.fileNumber, c.versionNo, c.effectiveDate, c.folderName, path.basename(c.filePath), c.approverText, ...c.departmentLabels]) if (!facts.includes(value)) fail('EXACT_UPLOAD_CONFIRMATION_MISMATCH')
  const codes = { 'NMPA 国内': 'NMPA', 'CE 欧盟': 'CE', 'FDA': 'FDA', 'MADSAP': 'MADSAP', '其他': '其他', '不适用': '不适用' }; for (const value of [...c.attributes.markets.map(m => codes[m]), `注册人：${c.attributes.licenseHolder}`, `生产方：${c.attributes.actualManufacturer}`, `文件转移：${c.attributes.documentTransfer}`, c.needTraining ? '需要培训' : '不需要培训']) if (!facts.includes(value)) fail('UPLOAD_ATTRIBUTE_TRAINING_CONFIRMATION_MISMATCH')
  state.step = 'UPLOAD_CONFIRMED_IDENTITY'; state.mutationAttempted = true; await click(page, page.locator('.el-message-box:visible').getByRole('button', { name: '确认提交', exact: true })); await post(page, c); await page.reload(); await settle(page); await post(page, c); const saved = await row(page.locator('.el-table:visible'), [c.fileNumber, c.versionNo]); await saved.getByText(c.expectedSavedStatus, { exact: true }).waitFor({ state: 'visible' }); state.step = 'UPLOAD_PERSISTED_DOM_VERIFIED'
}
async function openFile(page, c) {
  const panel = page.getByTestId('dcc-project-browser'); await H.uniqueVisible(panel); const aside = panel.locator('aside'); await aside.getByPlaceholder('项目名称或编码').fill(c.projectName); await click(page, aside.getByRole('button', { name: '查找项目', exact: true })); await click(page, await row(aside, [c.projectName])); await click(page, aside.locator('.el-tree-node__content').filter({ hasText: new RegExp(`^${escape(c.folderName)}$`) })); await H.choose(page, panel.getByTestId('dcc-project-browser-version-view'), c.versionView); await settle(page); const l = await row(panel.locator('main > .el-table'), [c.fileNumber, c.versionNo]); await click(page, l.getByRole('button', { name: '操作面板', exact: true })); await H.uniqueVisible(page.getByTestId('dcc-detail-formal-version-facts')); if (!(await page.getByTestId('dcc-detail-formal-version-facts').innerText()).includes(c.fileNumber)) fail('DETAIL_IDENTITY_MISMATCH')
}
async function pause(context, capture) { if (capture.recording) await context.tracing.stop({ path: path.join(capture.directory, `segment-${++capture.segment}.zip`) }); capture.recording = false; capture.allowScreenshot = false }
async function resume(page, context, capture) { for (const input of await page.locator('input[type=password]').all()) if (await input.inputValue()) fail('SECRET_NOT_CLEARED_CAPTURE_OFF'); await context.tracing.start({ screenshots: true, snapshots: true, sources: false }); capture.recording = true; capture.allowScreenshot = true }
async function sensitive(page, context, capture, input, password, action) {
  await pause(context, capture)
  try { await H.uniqueVisible(input); await input.fill(password); await action() } catch (_) { fail('SENSITIVE_ACTION_FAILED_NO_RAW_DIAGNOSTICS') }
  finally { const n = await input.count(); if (n > 1) fail('AMBIGUOUS_SECRET_INPUT_CAPTURE_OFF'); if (n === 1) { await input.fill(''); if (await input.inputValue()) fail('SECRET_NOT_CLEARED_CAPTURE_OFF') } }
  await resume(page, context, capture)
}
async function native(page, context, c, state, capture, password) {
  const todo = await row(page.locator('.el-table:visible'), [c.fileNumber, c.versionNo, c.stageLabel]); await click(page, todo.locator('[data-approval-action="view"]')); await H.uniqueVisible(page.getByTestId('dcc-detail-formal-version-facts')); if (!(await page.getByTestId('dcc-detail-formal-version-facts').innerText()).includes(c.expectedBpmId)) fail('NATIVE_BPM_CONTEXT_MISMATCH'); const reached = new URL(page.url()); if (reached.searchParams.get('taskId') !== c.expectedTaskId || reached.searchParams.get('processInstanceId') !== c.expectedBpmId) fail('NATIVE_NAVIGATION_TASK_CONTEXT_MISMATCH')
  if (c.action === 'assign') {
    const input = page.getByPlaceholder('当前账号签名密码', { exact: true }); await H.uniqueVisible(input); const section = input.locator('xpath=ancestor::section[1]'); if (!(await section.innerText()).includes(c.departmentLabel)) fail('DEPARTMENT_CONTEXT_MISMATCH'); await H.choose(page, section.locator('.el-select'), c.assigneeOptionLabel); await section.getByPlaceholder('指派意见').fill(c.reason); state.mutationAttempted = true; await sensitive(page, context, capture, input, password, () => section.getByRole('button', { name: '签名确认指派', exact: true }).click())
  } else {
    await click(page, page.getByRole('button', { name: c.openActionButton, exact: true })); const dialog = page.getByRole('dialog', { name: c.dialogTitle, exact: true }); await H.uniqueVisible(dialog); const facts = await dialog.innerText(); for (const v of [c.fileNumber, c.versionNo, c.stageLabel]) if (!facts.includes(v)) fail('SIGNED_TASK_CONTEXT_MISMATCH')
    if (!facts.includes(c.expectedTaskId)) fail('NATIVE_TASK_ID_MISMATCH')
    const owner = dialog.getByTestId('dcc-approval-file-owner-picker'); if (c.ownerRequired) await H.choose(page, owner.locator('.el-select'), c.ownerOptionLabel); else if (await owner.count()) fail('UNEXPECTED_OWNER_REQUIREMENT')
    await H.fillItem(dialog, c.decision === 'reject' ? '驳回原因' : '审批意见', c.reason)
    if (c.stampedPdfPath) { const input = H.item(dialog, '盖章 PDF').locator('input[type=file]'); if (await input.count() !== 1) fail('STAMP_INPUT_AMBIGUOUS'); state.artifactUploadAttempted = true; await input.setInputFiles(c.stampedPdfPath); await settle(page) }
    if (c.confirmedDirectoryLabel) await H.choose(page, dialog.getByTestId('dcc-doc-control-confirmed-directory').locator('.el-select'), c.confirmedDirectoryLabel)
    state.mutationAttempted = true; await sensitive(page, context, capture, H.item(dialog, '登录密码').locator('input[type=password]'), password, () => dialog.getByRole('button', { name: '确认签名', exact: true }).click())
  }
  await settle(page); await selectedHistory(page, c); await post(page, c); await page.reload(); await settle(page); await selectedHistory(page, c); await post(page, c); state.step = 'NATIVE_TASK_SAVED_DOM_VERIFIED'
}
async function training(page, c, state) { await openFile(page, c); if (!(await page.getByTestId('dcc-detail-formal-version-facts').innerText()).includes(c.expectedBpmId)) fail('TRAINING_BPM_MISMATCH'); await click(page, page.getByRole('button', { name: '上传培训记录', exact: true })); const dialog = page.getByRole('dialog', { name: '上传培训记录', exact: true }); await H.uniqueVisible(dialog); const input = H.item(dialog, '培训记录').locator('input[type=file]'); if (await input.count() !== 1) fail('TRAINING_INPUT_AMBIGUOUS'); state.artifactUploadAttempted = true; await input.setInputFiles(c.filePath); await settle(page); await dialog.getByText(path.basename(c.filePath), { exact: true }).waitFor({ state: 'visible' }); state.mutationAttempted = true; await click(page, dialog.getByRole('button', { name: '确认上传', exact: true })); await post(page, c); await page.reload(); await settle(page); await post(page, c); state.step = 'TRAINING_CURRENT_ROUND_SAVED_DOM_VERIFIED' }
async function login(page, c, username, password) { await page.goto(new URL('/login', c.frontendUrl).href); const f = page.locator('.login-form:visible'); await H.uniqueVisible(f); const t = f.locator('.el-select:visible'); await click(page, t); const tenant = t.locator('input'); await tenant.fill(c.tenant); await tenant.press('Enter'); await f.getByPlaceholder(/用户名|账号/).fill(username); await f.locator('input[type=password]').fill(password); const remember = f.getByRole('checkbox', { name: /记住/ }); if (await remember.count()) await remember.uncheck(); await f.getByRole('button', { name: '登录', exact: true }).click(); await page.waitForURL(u => !u.pathname.includes('/login'), { timeout: 45000 }); await settle(page); for (const input of await page.locator('input[type=password]').all()) if (await input.inputValue()) fail('LOGIN_SECRET_NOT_CLEARED'); await page.getByText(c.actorDisplayLabel, { exact: true }).waitFor({ state: 'visible' }) }
async function run(c, credentials) {
  validate(c, c.mode)
  if (credentials?.authorization !== 'ROOT_CURRENT_REAL_UI_ACTIONS_AUTHORIZED') fail('ROOT_ACTUAL_UI_AUTHORIZATION_REQUIRED')
  const { username, password, signaturePassword } = credentials
  if (!username || !password || c.mode === 'native-task' && !signaturePassword) fail('RUNTIME_CREDENTIALS_REQUIRED')
  if (JSON.stringify(c).includes(password) || signaturePassword && JSON.stringify(c).includes(signaturePassword)) fail('SECRET_VALUE_IN_CONTRACT')
  const helpers = fs.readFileSync(path.join(__dirname, 'g27-public-lifecycle-ui-helpers.cjs')); const actualHelperHash = require('node:crypto').createHash('sha256').update(helpers).digest('hex'); if (actualHelperHash !== HELPER_SHA) fail('FROZEN_HELPER_DRIFT')
  const r = createRequire(path.resolve(__dirname, '../../../IntRuoyiFronted/package.json')); const { chromium } = r('playwright')
  const directory = path.join(__dirname, 'e2e-artifacts', `g29-${c.mode}-${Date.now()}`); fs.mkdirSync(directory, { recursive: true }); const state = { mode: c.mode, taskMarker: c.taskMarker, status: 'RUNNING', step: 'BEFORE_LOGIN', mutationAttempted: false, artifactUploadAttempted: false, naturalMutationRequests: [], actualGoalComplete: false }; const capture = { directory, segment: 0, recording: false, allowScreenshot: false }; let browser, context, page
  try {
    browser = await chromium.launch({ headless: true }); context = await browser.newContext({ timezoneId: 'Asia/Shanghai' }); page = await context.newPage(); await login(page, c, username, password)
    await context.tracing.start({ screenshots: true, snapshots: true, sources: false }); capture.recording = true; capture.allowScreenshot = true
    page.on('request', request => { if (request.method() !== 'GET') state.naturalMutationRequests.push({ method: request.method(), pathname: new URL(request.url()).pathname }) })
    await menus(page, c.menuLabels); state.step = 'ACTUAL_MENU_OPENED'
    if (c.mode === 'project-product') await project(page, c, state); else if (c.mode === 'upload-submit') await upload(page, c, state); else if (c.mode === 'native-task') await native(page, context, c, state, capture, signaturePassword); else await training(page, c, state)
    state.status = 'PASS_SCOPED_REAL_UI_DOM_AND_RELOAD'; if (capture.allowScreenshot) await page.screenshot({ path: path.join(directory, 'final.png'), fullPage: true })
  } catch (e) { state.status = state.mutationAttempted || state.artifactUploadAttempted ? 'STOPPED_AFTER_ACTION_VERIFY_REAL_STATE_NO_RETRY' : 'BLOCKED_BEFORE_CONFIRMED_ACTION'; state.errorCode = e?.safeCode || 'REAL_UI_FAILURE_SANITIZED' }
  finally {
    try { if (context && capture.recording) await context.tracing.stop({ path: path.join(directory, `segment-${++capture.segment}.zip`) }) } catch (_) { state.status = 'EVIDENCE_CAPTURE_FAILED_SANITIZED'; state.errorCode = 'TRACE_FINALIZATION_FAILED' }
    try { if (browser) await browser.close() } catch (_) { state.status = 'BROWSER_CLOSE_FAILED_SANITIZED'; state.errorCode = 'BROWSER_FINALIZATION_FAILED' }
    fs.writeFileSync(path.join(directory, 'result.json'), JSON.stringify(state, null, 2))
  }
  return state
}
function load(p, mode) { return validate(JSON.parse(fs.readFileSync(p, 'utf8')), mode) }
async function main() { const [mode, p, option] = process.argv.slice(2); const c = load(p, mode); if (option === '--validate-only') { process.stdout.write(JSON.stringify({ status: 'VALID_INPUT_NO_BROWSER', mode })); return } const result = await run(c, { username: process.env.DCC_G29_USERNAME, password: process.env.DCC_G29_PASSWORD, signaturePassword: process.env.DCC_G29_SIGNATURE_PASSWORD, authorization: process.env.DCC_G29_UI_AUTHORIZATION }); process.stdout.write(JSON.stringify(result)); if (result.status !== 'PASS_SCOPED_REAL_UI_DOM_AND_RELOAD') process.exitCode = 1 }
module.exports = Object.freeze({ MODES, SUPPORTED, validate, run, load })
if (require.main === module) main().catch(e => { process.stdout.write(JSON.stringify({ status: 'BLOCKED_BEFORE_BROWSER', errorCode: e?.safeCode || 'INVALID_CONTRACT_SANITIZED' })); process.exitCode = 2 })
