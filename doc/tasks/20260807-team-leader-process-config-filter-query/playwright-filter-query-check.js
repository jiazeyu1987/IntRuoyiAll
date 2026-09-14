async (page) => {
  const result = {
    url: '',
    fields: [],
    requests: [],
    responses: [],
    writes: [],
    pageErrors: [],
    consoleErrors: [],
    visibleRows: 0
  }
  page.on('pageerror', (error) => result.pageErrors.push(error.message || String(error)))
  page.on('console', (message) => {
    if (message.type() === 'error') result.consoleErrors.push(message.text())
  })
  page.on('request', (request) => {
    const url = request.url()
    if (!url.includes('/mes/pro/process-pool/team-leader/process-config/list')) return
    result.requests.push(`${request.method()} ${url}`)
    if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      result.writes.push(`${request.method()} ${url}`)
    }
  })

  if (page.url().includes('/login')) {
    await page.getByRole('button', { name: '登录' }).click()
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  }
  await page.goto('http://127.0.0.1:8081/mes/pro/process-pool/team-leader', {
    waitUntil: 'domcontentloaded'
  })
  result.url = page.url()

  const filter = page.locator('[data-table-key="mes.processPool.teamLeader.processConfig"]')
  await filter.waitFor({ state: 'visible', timeout: 60000 })
  const addCondition = filter.getByRole('button', { name: '新增筛选条件' })
  const removeCondition = filter.getByRole('button', { name: '删除当前筛选条件' })

  while (await removeCondition.isEnabled().catch(() => false)) {
    await removeCondition.click()
  }

  await addCondition.click()
  const fieldSelect = filter.locator('.table-multi-filter__field-select')
  await fieldSelect.click()
  result.fields = await page.locator('.el-select-dropdown__item:visible').allTextContents()
  await page.getByRole('option', { name: '工艺路线' }).click()
  await filter.getByPlaceholder('请输入路线编码或名称').fill('R-PCU')

  const singleResponsePromise = page.waitForResponse((response) => {
    const url = response.url()
    return url.includes('/mes/pro/process-pool/team-leader/process-config/list')
      && url.includes('routeKeyword=R-PCU')
  }, { timeout: 60000 })
  await filter.getByRole('button', { name: '查询' }).click()
  const singleResponse = await singleResponsePromise
  const singleBody = await singleResponse.json().catch(() => undefined)
  result.responses.push({
    name: 'single',
    status: singleResponse.status(),
    code: singleBody?.code,
    message: singleBody?.msg || singleBody?.message,
    rowCount: Array.isArray(singleBody?.data) ? singleBody.data.length : undefined
  })

  await addCondition.click()
  await fieldSelect.click()
  await page.getByRole('option', { name: '映射设备' }).click()
  await filter.getByPlaceholder('请输入设备编码或名称').fill('RLR0807M-001-01')

  const combinedResponsePromise = page.waitForResponse((response) => {
    const url = response.url()
    return url.includes('/mes/pro/process-pool/team-leader/process-config/list')
      && url.includes('routeKeyword=R-PCU')
      && url.includes('deviceKeyword=RLR0807M-001-01')
  }, { timeout: 60000 })
  await filter.getByRole('button', { name: '查询' }).click()
  const combinedResponse = await combinedResponsePromise
  const combinedBody = await combinedResponse.json().catch(() => undefined)
  result.responses.push({
    name: 'combined',
    status: combinedResponse.status(),
    code: combinedBody?.code,
    message: combinedBody?.msg || combinedBody?.message,
    rowCount: Array.isArray(combinedBody?.data) ? combinedBody.data.length : undefined
  })

  const resetResponsePromise = page.waitForResponse((response) => {
    const url = response.url()
    return url.includes('/mes/pro/process-pool/team-leader/process-config/list')
      && !url.includes('routeKeyword=')
      && !url.includes('processKeyword=')
      && !url.includes('lossReasonKeyword=')
      && !url.includes('deviceKeyword=')
      && !url.includes('parameterKeyword=')
  }, { timeout: 60000 })
  await filter.getByRole('button', { name: '重置' }).click()
  const resetResponse = await resetResponsePromise
  const resetBody = await resetResponse.json().catch(() => undefined)
  result.responses.push({
    name: 'reset',
    status: resetResponse.status(),
    code: resetBody?.code,
    message: resetBody?.msg || resetBody?.message,
    rowCount: Array.isArray(resetBody?.data) ? resetBody.data.length : undefined
  })

  result.visibleRows = await page.locator('[data-team-leader-process-config-table] .el-table__body tbody tr').count()
  return result
}
