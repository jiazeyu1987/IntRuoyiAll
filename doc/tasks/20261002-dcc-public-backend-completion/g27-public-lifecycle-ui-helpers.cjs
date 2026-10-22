/* Incremental real-UI helpers only. Importing this module launches no browser or business action. */
const assert = require('node:assert/strict')
const TASK = /^20261001-dcc-integration[-_A-Za-z0-9]*$/
const escapeRegex = value => String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
const required = value => { assert.equal(typeof value, 'string'); assert.ok(value.trim()); return value }
async function uniqueVisible(locator) {
  await locator.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await locator.count(), 1, 'BLOCKED: business locator is not unique')
  return locator
}
async function clickReady(locator) {
  await uniqueVisible(locator)
  assert.equal(await locator.isEnabled(), true, 'BLOCKED: real business action disabled')
  await locator.click()
}
function item(scope, label) {
  // Element Plus label is a direct child of its real form item; the parent traversal keeps
  // the existing scope and avoids a scope-prefixed has locator being evaluated twice.
  return scope.locator('.el-form-item__label')
    .filter({ hasText: new RegExp(`^${escapeRegex(label)}[：:]?$`) }).locator('..')
}
async function fillItem(scope, label, value) {
  const input = item(scope, label).locator('input:not([type=hidden]),textarea').filter({ visible: true })
  await uniqueVisible(input)
  assert.equal(await input.isEnabled(), true)
  await input.fill(required(value))
}
async function choose(page, select, label) {
  await clickReady(select)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: new RegExp(`^${escapeRegex(required(label))}$`) })
  await clickReady(option)
  // A multi-select intentionally remains open; caller can select another option then close via Escape.
}
async function attributes(scope, value) {
  const fields = scope.locator('.dcc-project-attributes:visible')
  await uniqueVisible(fields)
  const markets = ['NMPA 国内', 'CE 欧盟', 'FDA', 'MADSAP', '其他', '不适用']
  assert.ok(Array.isArray(value.markets) && value.markets.length && value.markets.every(x => markets.includes(x)))
  assert.equal(new Set(value.markets).size, value.markets.length)
  const marketItem = item(fields, '目标市场')
  for (const name of markets) await marketItem.getByRole('checkbox', { name, exact: true }).setChecked(value.markets.includes(name))
  if (value.markets.includes('其他')) await fillItem(fields, '其他市场说明', value.otherMarket)
  for (const [label, key] of [['是否为注册人', 'licenseHolder'], ['是否为生产方', 'actualManufacturer'], ['文件转移', 'documentTransfer']]) {
    const allowed = key === 'documentTransfer' ? ['是', '否'] : ['是', '否', '不适用']
    assert.ok(allowed.includes(value[key]))
    await item(fields, label).getByRole('radio', { name: value[key], exact: true }).check()
  }
  if (value.documentTransfer === '是') await fillItem(fields, '转移至', value.transferTo)
}
function owned(marker, value) { assert.match(required(marker), TASK); assert.ok(required(value).includes(marker), 'BLOCKED: use task-owned asset') }
async function prepareProjectProduct(page, marker, values) {
  for (const key of ['projectName', 'projectCode', 'productCode', 'productName', 'creationReason']) owned(marker, values[key])
  await clickReady(page.getByTestId('dcc-project-product-create-open'))
  const dialog = page.getByRole('dialog', { name: '新建项目代码及产品', exact: true })
  await uniqueVisible(dialog)
  for (const [label, key] of [['项目名称', 'projectName'], ['项目代码', 'projectCode'], ['产品编码', 'productCode'], ['产品名称', 'productName'], ['新建申请原因', 'creationReason']]) await fillItem(dialog, label, values[key])
  await choose(page, item(dialog, '项目负责人').locator('.el-select'), values.leaderOptionLabel)
  await choose(page, item(dialog, '分类').locator('.el-select'), values.classification)
  await choose(page, item(dialog, '目录模板').locator('.el-select'), values.folderTemplateLabel)
  const reviewer = await item(dialog, '本次审核人').innerText()
  assert.ok(reviewer.includes(required(values.reviewerOptionLabel)) && !/尚未配置|不可用/.test(reviewer), 'BLOCKED: actual configured reviewer mismatch')
  await attributes(dialog, values.attributes)
  if (values.remark) await fillItem(dialog, '备注', values.remark)
  await clickReady(dialog.getByRole('button', { name: '确认申请信息', exact: true }))
  const confirmation = page.getByTestId('dcc-project-product-confirmation')
  await uniqueVisible(confirmation)
  const text = await confirmation.innerText()
  for (const key of ['projectName', 'projectCode', 'productCode', 'productName', 'folderTemplateLabel']) assert.ok(text.includes(values[key]))
  return { dialog, confirmation } // Root explicitly clicks 提交申请 or 返回修改; no implicit save here.
}
async function prepareCheckin(page, marker, values) {
  owned(marker, values.description)
  const dialog = page.getByRole('dialog', { name: '检入新版本', exact: true })
  await uniqueVisible(dialog)
  const summary = dialog.getByTestId('dcc-controlled-browser-checkin-working-only')
  await uniqueVisible(summary)
  assert.ok((await summary.innerText()).includes('工作'), 'BLOCKED: checkin must save a working iteration')
  assert.equal(await dialog.getByRole('radio', { name: /局部变更|换版变更/ }).count(), 0)
  if (values.sourcePath) {
    const input = dialog.getByTestId('dcc-controlled-browser-checkin-upload').locator('input[type=file]')
    assert.equal(await input.count(), 1)
    await input.setInputFiles(required(values.sourcePath))
  }
  await fillItem(dialog, '修改说明', values.description)
  if (values.remark) await fillItem(dialog, '检入备注', values.remark)
  await dialog.getByTestId('dcc-controlled-browser-checkin-need-training').setChecked(Boolean(values.needTraining))
  return dialog // Root verifies upload complete/no errors then explicitly confirms 检入.
}
async function prepareRevision(page, marker, values) {
  owned(marker, values.description)
  assert.ok(['局部变更', '换版变更'].includes(values.intent))
  const panel = page.getByTestId('dcc-revision-panel')
  await uniqueVisible(panel)
  await panel.getByRole('radio', { name: values.intent, exact: true }).check()
  await choose(page, item(panel, '正文小版本').locator('.el-select'), values.iterationVersion)
  await fillItem(panel, '变更说明', values.description)
  assert.ok((await item(panel, '目标受控版本').innerText()).includes(required(values.targetVersion)))
  return panel // Root fills actual parent attributes/effective date and explicitly submits.
}
async function sensitivePasswordStep(input, password, captureHooks, action) {
  assert.ok(captureHooks && typeof captureHooks.pause === 'function' && typeof captureHooks.resume === 'function', 'BLOCKED: explicit screenshot/trace/network capture suspension hooks required')
  await captureHooks.pause()
  try {
    await uniqueVisible(input)
    await input.fill(required(password))
    await action()
  } catch (_) {
    // Do not reflect a Playwright action error containing the input value into any task output.
    throw new Error('SENSITIVE_UI_ACTION_FAILED: inspect credential-free page state after clearing fields')
  } finally {
    if (await input.count()) {
      assert.equal(await input.count(), 1, 'BLOCKED: secret input became ambiguous; capture remains paused')
      if (await input.isVisible()) await input.fill('')
      assert.equal(await input.inputValue(), '', 'BLOCKED: secret field not cleared; capture remains paused')
    }
    await captureHooks.resume()
  }
}
async function signoffAssignment(page, section, marker, values, captureHooks) {
  owned(marker, values.reason)
  await uniqueVisible(section)
  assert.ok((await section.innerText()).includes(required(values.departmentLabel)))
  await choose(page, section.locator('.el-select').filter({ has: section.getByPlaceholder('选择本部门会签人') }), values.assigneeOptionLabel)
  await section.getByPlaceholder('指派意见', { exact: true }).fill(values.reason)
  await sensitivePasswordStep(section.getByPlaceholder('当前账号签名密码', { exact: true }), values.password, captureHooks,
    () => clickReady(section.getByRole('button', { name: '签名确认指派', exact: true })))
  // Root observes the returned authoritative current department/task; no assumed node advancement here.
}
async function signCurrentTask(page, dialog, marker, values, captureHooks) {
  owned(marker, values.fileNumber); owned(marker, values.reason)
  assert.ok(['approve', 'reject'].includes(values.mode))
  await uniqueVisible(dialog)
  const visible = await dialog.innerText()
  for (const value of [values.fileNumber, required(values.version), required(values.stageLabel)]) assert.ok(visible.includes(value), 'BLOCKED: exact displayed file/version/stage required')
  if (values.fileOwnerOptionLabel) {
    const owner = dialog.getByTestId('dcc-approval-file-owner-picker')
    await uniqueVisible(owner)
    await choose(page, owner.locator('.el-select'), values.fileOwnerOptionLabel)
  }
  await fillItem(dialog, values.mode === 'reject' ? '驳回原因' : '审批意见', values.reason)
  const input = item(dialog, '登录密码').locator('input[type=password]')
  await sensitivePasswordStep(input, values.password, captureHooks,
    () => clickReady(dialog.getByRole('button', { name: '确认签名', exact: true })))
  // Root refreshes the real UI and proves successor task/state/history; click success alone is not E2E PASS.
}
module.exports = Object.freeze({ uniqueVisible, clickReady, item, fillItem, choose, attributes,
  prepareProjectProduct, prepareCheckin, prepareRevision, sensitivePasswordStep, signoffAssignment, signCurrentTask })
