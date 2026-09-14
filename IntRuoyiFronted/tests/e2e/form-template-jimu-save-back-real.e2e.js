const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.FORM_TEMPLATE_JIMU_SAVE_BACK_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.FORM_TEMPLATE_JIMU_SAVE_BACK_E2E_TENANT || '芋道源码',
  username: process.env.FORM_TEMPLATE_JIMU_SAVE_BACK_E2E_USERNAME || 'admin',
  password: process.env.FORM_TEMPLATE_JIMU_SAVE_BACK_E2E_PASSWORD || 'admin123',
  headed: process.env.FORM_TEMPLATE_JIMU_SAVE_BACK_E2E_HEADED === '1',
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
}

function assertPrerequisites() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${config.baseUrl}`
  )
  assert.ok(config.password, '缺少 FORM_TEMPLATE_JIMU_SAVE_BACK_E2E_PASSWORD')
}

function unwrapCacheValue(raw) {
  if (!raw) return null
  let current = raw
  for (let index = 0; index < 4; index += 1) {
    if (typeof current !== 'string') return current
    try {
      current = JSON.parse(current)
    } catch {
      return current
    }
    if (current && typeof current === 'object' && Object.prototype.hasOwnProperty.call(current, 'v')) {
      current = current.v
    }
  }
  return current
}

async function authHeaders(page) {
  const cache = await page.evaluate(() =>
    Object.fromEntries(
      Array.from({ length: localStorage.length }, (_, index) => {
        const key = localStorage.key(index)
        return [key, localStorage.getItem(key)]
      })
    )
  )
  const accessToken = unwrapCacheValue(cache.ACCESS_TOKEN)
  const tenantId = unwrapCacheValue(cache.tenantId)
  const visitTenantId = unwrapCacheValue(cache.visitTenantId)
  assert.ok(accessToken, 'ACCESS_TOKEN missing after real login')
  assert.ok(tenantId, 'tenantId missing after real login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function apiGet(page, apiPath) {
  const headers = await authHeaders(page)
  const response = await page.evaluate(
    async ({ url, requestHeaders }) => {
      const result = await fetch(url, { headers: requestHeaders })
      return {
        ok: result.ok,
        status: result.status,
        json: await result.json()
      }
    },
    {
      url: `${config.baseUrl}/admin-api${apiPath}`,
      requestHeaders: headers
    }
  )
  assert.ok(response.ok && response.json.code === 0, `GET ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

function parseTemplateSchema(template) {
  assert.ok(template?.jimuSchemaJson, '模板版本缺少 jimuSchemaJson')
  const wrapper = JSON.parse(template.jimuSchemaJson)
  assert.equal(typeof wrapper.sheetLayoutJson, 'string', '模板版本缺少正式 sheetLayoutJson')
  const layout = JSON.parse(wrapper.sheetLayoutJson)
  return {
    wrapper,
    layout,
    sheetLayoutJson: wrapper.sheetLayoutJson
  }
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 120000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 120000 })
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 120000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 120000, waitUntil: 'commit' })
}

async function findTemplateCandidate(page) {
  let firstUsableCandidate = null
  let firstUsableCandidatePageNo = 1
  for (let pageNo = 1; pageNo <= 20; pageNo += 1) {
    const data = await apiGet(page, `/form-center/template-pool?pageNo=${pageNo}&pageSize=50`)
    const rows = Array.isArray(data?.list) ? data.list : []
    const candidates = rows.filter(
      (row) =>
        row.status !== 'OBSOLETE' &&
        row.status !== 'PENDING_APPROVAL' &&
        typeof row.designerReportId === 'string' &&
        row.designerReportId.startsWith('FORMTPL:') &&
        typeof row.jimuSchemaJson === 'string' &&
        row.jimuSchemaJson.trim()
    )
    const draft = candidates.find((row) => row.status === 'DRAFT')
    if (draft) {
      return { candidate: draft, pageNo }
    }
    if (!firstUsableCandidate && candidates.length > 0) {
      firstUsableCandidate = candidates[0]
      firstUsableCandidatePageNo = pageNo
    }
    if (rows.length < 50) break
  }
  if (firstUsableCandidate) {
    return { candidate: firstUsableCandidate, pageNo: firstUsableCandidatePageNo }
  }
  throw new Error('模板列表中未找到可用于 Jimu 保存回写验证的模板')
}

function normalizeDesignerUrl(rawUrl) {
  const url = new URL(rawUrl)
  return {
    pathname: url.pathname,
    mode: url.searchParams.get('mode'),
    reportMode: url.searchParams.get('reportMode'),
    reportId: url.searchParams.get('reportId'),
    templateId: url.searchParams.get('templateId'),
    versionNo: url.searchParams.get('versionNo')
  }
}

async function openTemplateJimuEditor(page) {
  const targetResult = await findTemplateCandidate(page)
  const sourceRow = targetResult.candidate
  await page.goto(`${config.baseUrl}/mdm/form-center/template?pageNo=${targetResult.pageNo}&pageSize=50`, {
    waitUntil: 'commit',
    timeout: 120000
  })
  await page.getByText('表单模板', { exact: false }).first().waitFor({ state: 'visible', timeout: 120000 })
  const row = page
    .locator('.el-table__body-wrapper tbody tr.el-table__row')
    .filter({ hasText: sourceRow.templateName })
    .filter({ hasText: sourceRow.versionNo })
    .first()
  await row.waitFor({ state: 'visible', timeout: 120000 })
  await row.scrollIntoViewIfNeeded()
  await row.click()

  const editButton = page.locator('.form-template-preview__actions').getByRole('button', { name: '编辑' }).first()
  await editButton.waitFor({ state: 'visible', timeout: 120000 })
  const navigationPromise = page.waitForURL(
    (url) =>
      url.pathname === '/mdm/form-center/template' &&
      url.searchParams.get('mode') === 'designer' &&
      url.searchParams.get('reportMode') === 'edit' &&
      url.searchParams.get('templateId') === String(sourceRow.templateId) &&
      Boolean(url.searchParams.get('versionNo')) &&
      String(url.searchParams.get('reportId') || '').startsWith('FORMTPL:'),
    { timeout: 120000, waitUntil: 'commit' }
  )
  await editButton.click()
  await navigationPromise

  const routeInfo = normalizeDesignerUrl(page.url())
  const editableVersion = await apiGet(
    page,
    `/form-center/templates/${sourceRow.templateId}/versions/${encodeURIComponent(routeInfo.versionNo)}`
  )
  assert.equal(editableVersion.status, 'DRAFT', 'Jimu 编辑器保存回写只能进入草稿模板版本')

  const iframe = page.locator('iframe[src*="/jmreport/index/"]').first()
  await iframe.waitFor({ state: 'visible', timeout: 120000 })
  const iframeHandle = await iframe.elementHandle()
  const frame = iframeHandle ? await iframeHandle.contentFrame() : undefined
  assert.ok(frame, '未找到表单模板 Jimu 编辑 iframe')
  await frame.waitForLoadState('domcontentloaded', { timeout: 120000 })
  await frame.waitForFunction(
    () => {
      const xsRef = window.xs
      const vmRef = window.vm
      const apiRef = window.api || window.eval('typeof api !== "undefined" ? api : undefined')
      const axiosRef = window.axios || window.eval('typeof axios !== "undefined" ? axios : undefined')
      return Boolean(xsRef?.sheet?.data?.rows?._ && xsRef.getSaveData && vmRef?.designerObj && apiRef?.saveReport && axiosRef?.request)
    },
    { timeout: 120000 }
  )

  return {
    sourceRow,
    editableVersion,
    routeInfo,
    frame
  }
}

async function prepareCanvasAddCell(frame, marker) {
  return await frame.evaluate(async ({ markerText }) => {
    const xsRef = window.xs
    if (!xsRef?.getSaveData || !xsRef?.loadData) {
      throw new Error('Jimu x-spreadsheet runtime is not ready')
    }
    const original = await xsRef.getSaveData()
    const working = JSON.parse(JSON.stringify(original))
    working.rows = working.rows || {}
    const rowIndexes = Object.keys(working.rows)
      .filter((key) => key !== 'len' && Number.isInteger(Number(key)))
      .map((key) => Number(key))
    const targetRow = rowIndexes.length > 0 ? Math.max(...rowIndexes) + 1 : 0
    const targetCol = 0
    working.rows[targetRow] = working.rows[targetRow] || { height: 36, cells: {} }
    working.rows[targetRow].cells = working.rows[targetRow].cells || {}
    working.rows[targetRow].cells[targetCol] = {
      ...(working.rows[targetRow].cells[targetCol] || {}),
      text: markerText
    }
    const currentRowsLen = Number(working.rows.len || 0)
    if (!Number.isFinite(currentRowsLen) || currentRowsLen <= targetRow) {
      working.rows.len = targetRow + 1
    }
    xsRef.loadData(working)
    await new Promise((resolve) => window.setTimeout(resolve, 100))
    const modified = await xsRef.getSaveData()
    if (!JSON.stringify(modified.rows || {}).includes(markerText)) {
      throw new Error('新增单元格没有进入 Jimu 保存数据')
    }
    return {
      original,
      modified,
      targetRow,
      targetCol
    }
  }, { markerText: marker })
}

async function nativeSaveDesignerData(frame, designerData) {
  return await frame.evaluate(async ({ payloadData }) => {
    const vmRef = window.vm
    const apiRef = window.api || window.eval('typeof api !== "undefined" ? api : undefined')
    const axiosRef = window.axios || window.eval('typeof axios !== "undefined" ? axios : undefined')
    const currentSheetId = window.eval('typeof currentSheetId !== "undefined" ? currentSheetId : undefined')
    const excelConfigId = window.eval('typeof excel_config_id !== "undefined" ? excel_config_id : undefined')
    if (!vmRef?.designerObj || !apiRef?.saveReport || !axiosRef?.request) {
      throw new Error('Jimu save runtime is not ready')
    }
    const saveData = JSON.parse(JSON.stringify(payloadData))
    delete saveData.designerObj
    const reportId = vmRef.designerObj.id || saveData.excel_config_id || excelConfigId
    saveData.excel_config_id = reportId
    saveData.sheetId = currentSheetId || saveData.sheetId
    saveData.querySetting = vmRef.querySetting
    saveData.fillFormStyle = vmRef.fillFormStyle
    saveData.pyGroupEngine = Boolean(vmRef.pyGroupEngine)
    saveData.isViewContentHorizontalCenter = Boolean(vmRef.isViewContentHorizontalCenter)
    saveData.submitHandlers = vmRef.submitHandlersData
    saveData.queryFormSetting = vmRef.queryFormSetting
    saveData.updateCount = vmRef.designerObj.updateCount
    const designerObj = {
      ...vmRef.designerObj,
      id: reportId
    }
    const response = await axiosRef.request({
      url: apiRef.saveReport,
      method: 'post',
      data: JSON.stringify({
        designerObj,
        ...saveData
      }),
      headers: {
        'Content-Type': 'application/json;charset=UTF-8'
      }
    })
    if (!response.data || response.data.success !== true) {
      throw new Error(`Jimu native save failed: ${JSON.stringify(response.data)}`)
    }
    if (response.data.result?.updateCount != null) {
      vmRef.designerObj.updateCount = response.data.result.updateCount
    }
    return {
      success: response.data.success,
      updateCount: response.data.result?.updateCount
    }
  }, { payloadData: designerData })
}

async function main() {
  assertPrerequisites()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(120000)
  page.setDefaultNavigationTimeout(120000)

  try {
    await login(page)
    const flow = await openTemplateJimuEditor(page)
    const beforeTemplate = await apiGet(
      page,
      `/form-center/templates/${flow.editableVersion.templateId}/versions/${encodeURIComponent(flow.editableVersion.versionNo)}`
    )
    const beforeSchema = parseTemplateSchema(beforeTemplate)
    const preservedWrapperKeys = [
      'cellRules',
      'assistRows',
      'signatureCellMarkers',
      'fillAssignments'
    ].filter((key) => Object.prototype.hasOwnProperty.call(beforeSchema.wrapper, key))

    const marker = `CODEx_JIMU_SAVE_BACK_${Date.now()}`
    const editResult = await prepareCanvasAddCell(flow.frame, marker)
    const saveResult = await nativeSaveDesignerData(flow.frame, editResult.modified)
    const afterSaveTemplate = await apiGet(
      page,
      `/form-center/templates/${flow.editableVersion.templateId}/versions/${encodeURIComponent(flow.editableVersion.versionNo)}`
    )
    const afterSaveSchema = parseTemplateSchema(afterSaveTemplate)
    assert.ok(
      afterSaveSchema.sheetLayoutJson.includes(marker),
      'Jimu 原生保存后，模板版本正式 sheetLayoutJson 未同步新增单元格'
    )
    for (const key of preservedWrapperKeys) {
      assert.deepEqual(
        afterSaveSchema.wrapper[key],
        beforeSchema.wrapper[key],
        `Jimu 原生保存只能替换 sheetLayoutJson，不能改动模板外层配置：${key}`
      )
    }

    const restoreResult = await nativeSaveDesignerData(flow.frame, editResult.original)
    const afterRestoreTemplate = await apiGet(
      page,
      `/form-center/templates/${flow.editableVersion.templateId}/versions/${encodeURIComponent(flow.editableVersion.versionNo)}`
    )
    const afterRestoreSchema = parseTemplateSchema(afterRestoreTemplate)
    assert.equal(
      afterRestoreSchema.sheetLayoutJson.includes(marker),
      false,
      '恢复保存后，临时新增单元格仍残留在模板版本 sheetLayoutJson'
    )
    for (const key of preservedWrapperKeys) {
      assert.deepEqual(
        afterRestoreSchema.wrapper[key],
        beforeSchema.wrapper[key],
        `恢复保存后模板外层配置不应变化：${key}`
      )
    }

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          username: config.username,
          templateId: flow.editableVersion.templateId,
          templateName: flow.editableVersion.templateName,
          sourceVersionNo: flow.sourceRow.versionNo,
          editableVersionNo: flow.editableVersion.versionNo,
          sourceStatus: flow.sourceRow.status,
          editableStatus: flow.editableVersion.status,
          reportId: flow.routeInfo.reportId,
          targetRow: editResult.targetRow,
          targetCol: editResult.targetCol,
          saveUpdateCount: saveResult.updateCount,
          restoreUpdateCount: restoreResult.updateCount,
          preservedWrapperKeys,
          markerVerifiedThenRemoved: true
        },
        null,
        2
      )
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
