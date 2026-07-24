const successCodes = new Set(['0', '200'])

const assertCondition = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const hasOwn = (value, key) => Object.prototype.hasOwnProperty.call(value, key)

const summarize = (value) => {
  const text = typeof value === 'string' ? value : JSON.stringify(value)
  return String(text).slice(0, 800)
}

const requireFunction = (ctx, name) => {
  const value = ctx?.[name]
  assertCondition(typeof value === 'function', `E2E ctx 缺少必需函数：${name}`)
  return value
}

const requireEnv = (env, key, label) => {
  const value = env?.[key]
  assertCondition(
    typeof value === 'string' && value.trim(),
    `缺少${label}环境变量：${key}，无法使用真实账号执行后台内容审批链路`
  )
  return value.trim()
}

const waitForApiData = async (page, waitForJsonResponse, urlPart, action, label) => {
  const { payload } = await waitForJsonResponse(page, urlPart, action, 30000)
  return assertApiEnvelope(payload, label)
}

const buildFrontendUrl = (frontendBase, path) => {
  assertCondition(
    typeof frontendBase === 'string' && frontendBase.trim(),
    'E2E ctx 缺少 frontendBase，无法进入真实前端入口'
  )
  return `${frontendBase.replace(/\/$/, '')}${path}`
}

const assertPath = async (page, path) => {
  await page.waitForURL((url) => url.pathname === path, { timeout: 30000 })
}

const assertApiEnvelope = (json, label) => {
  assertCondition(
    json && typeof json === 'object' && !Array.isArray(json),
    `${label} 未返回标准 JSON 对象：${summarize(json)}`
  )
  assertCondition(
    hasOwn(json, 'code'),
    `${label} 响应缺少 code 字段，不能确认真实接口成功：${summarize(json)}`
  )
  assertCondition(
    successCodes.has(String(json.code)),
    `${label} 接口失败 code=${json.code} msg=${json.msg || json.message || '无'}`
  )
  assertCondition(
    hasOwn(json, 'data'),
    `${label} 响应缺少 data 字段，不能确认工作台数据来自实时接口：${summarize(json)}`
  )
  return json.data
}

const assertObjectData = (value, label) => {
  assertCondition(
    value && typeof value === 'object' && !Array.isArray(value),
    `${label} data 必须是对象，实际为：${summarize(value)}`
  )
  assertCondition(
    Object.keys(value).length > 0,
    `${label} data 为空对象，缺少可验证的真实后台数据`
  )
  return value
}

const assertArrayData = (value, label) => {
  assertCondition(Array.isArray(value), `${label} data 必须是数组，实际为：${summarize(value)}`)
  return value
}

/**
 * BDD: 后台公司/产品内容审批链路 -> Given 真实编辑人账号登录后台, When 进入
 * `/showroom/company` 与 `/showroom/product`, Then 公司和产品工作台必须
 * 从实时接口渲染数据，并暴露审批中心、版本历史、编辑、提交和审批入口。
 *
 * RED: 若真实账号、实时接口数据、路由工作台、编辑入口、提交入口或审批入口缺失，
 * 本用例必须失败，并抛出明确缺口；不得把静态文案、空数据或不可点击文本当作通过。
 *
 * GREEN: 真实 UI 提供公司/产品编辑提交链路，审批中心可进入真实审批动作，版本历史
 * 可追溯，且页面展示的数量/版本来自本次捕获的接口响应。
 */
export default {
  id: 'showroom-phase1-admin-content-approval',
  title: 'Phase 1 后台公司/产品内容审批链路',
  requiredEnv: [
    'SHOWROOM_E2E_TENANT_NAME',
    'SHOWROOM_E2E_EDITOR_USERNAME',
    'SHOWROOM_E2E_EDITOR_PASSWORD'
  ],
  async run(ctx) {
    const page = ctx?.page
    const env = ctx?.env
    const frontendBase = ctx?.frontendBase
    const loginAs = requireFunction(ctx, 'loginAs')
    const expectVisibleText = requireFunction(ctx, 'expectVisibleText')
    const failIfMissing = requireFunction(ctx, 'failIfMissing')
    const waitForJsonResponse = requireFunction(ctx, 'waitForJsonResponse')

    assertCondition(page && typeof page.goto === 'function', 'E2E ctx 缺少 Playwright page')

    requireEnv(env, 'SHOWROOM_E2E_TENANT_NAME', '真实 E2E 租户')
    requireEnv(env, 'SHOWROOM_E2E_EDITOR_USERNAME', '真实编辑人账号')
    requireEnv(env, 'SHOWROOM_E2E_EDITOR_PASSWORD', '真实编辑人密码')

    const company = assertObjectData(
      await waitForApiData(
        page,
        waitForJsonResponse,
        '/showroom/company/current',
        () => loginAs(page, 'EDITOR', '/showroom/company'),
        '公司当前版本接口 /showroom/company/current'
      ),
      '公司当前版本接口'
    )
    await assertPath(page, '/showroom/company')

    const productsFromCompanyLoad = assertArrayData(
      await waitForApiData(
        page,
        waitForJsonResponse,
        '/showroom/product/page',
        () =>
          page.goto(buildFrontendUrl(frontendBase, '/showroom/company'), {
            waitUntil: 'domcontentloaded',
            timeout: 30000
          }),
        '产品分页接口 /showroom/product/page'
      ),
      '产品分页接口'
    )
    const approvals = assertArrayData(
      await waitForApiData(
        page,
        waitForJsonResponse,
        '/showroom/approval/page',
        () =>
          page.goto(buildFrontendUrl(frontendBase, '/showroom/company'), {
            waitUntil: 'domcontentloaded',
            timeout: 30000
          }),
        '审批分页接口 /showroom/approval/page'
      ),
      '审批分页接口'
    )
    await assertPath(page, '/showroom/company')

    assertCondition(
      productsFromCompanyLoad.length > 0,
      '产品分页接口返回空数组，缺少可验证的真实产品数据'
    )

    await expectVisibleText(page, '展柜后台', 'showroom admin title')
    await expectVisibleText(
      page,
      '结构化内容、审批、指派和讲解资产统一管理',
      'showroom admin subtitle'
    )
    await failIfMissing(
      page.getByRole('tab', { name: '公司信息', selected: true }),
      '/showroom/company 未激活“公司信息”工作台'
    )
    await expectVisibleText(page, '公司结构化字段', 'company workbench row')
    await expectVisibleText(
      page,
      `当前版本：${company.revisionNo || '未发布'}，公司内容由接口实时加载`,
      'company revision text from live api'
    )

    await failIfMissing(
      page.getByRole('tab', { name: '审批中心' }),
      '缺少“审批中心”入口，无法进入后台审批链路'
    )
    await failIfMissing(
      page.getByRole('tab', { name: '版本历史' }),
      '缺少“版本历史”入口，无法确认内容版本追溯'
    )

    await page.getByRole('tab', { name: '审批中心' }).click()
    await expectVisibleText(
      page,
      '固定路线为编辑人提交、部门主管审核、企宣批准',
      'approval route description'
    )
    await expectVisibleText(page, `${approvals.length} 个待办`, 'approval count from live api')

    await page.getByRole('tab', { name: '版本历史' }).click()
    await expectVisibleText(
      page,
      '按字段保留旧值、新值、操作人和发布时间',
      'version history entry description'
    )

    const productsFromProductRoute = assertArrayData(
      await waitForApiData(
        page,
        waitForJsonResponse,
        '/showroom/product/page',
        () =>
          page.goto(buildFrontendUrl(frontendBase, '/showroom/product'), {
            waitUntil: 'domcontentloaded',
            timeout: 30000
          }),
        '产品路由实时接口 /showroom/product/page'
      ),
      '产品路由实时接口'
    )
    await assertPath(page, '/showroom/product')
    assertCondition(
      productsFromProductRoute.length > 0,
      '/showroom/product 实时接口返回空数组，缺少可验证的真实产品数据'
    )

    await failIfMissing(
      page.getByRole('tab', { name: '产品管理', selected: true }),
      '/showroom/product 未激活“产品管理”工作台'
    )
    await expectVisibleText(page, '产品详情表', 'product workbench row')
    await expectVisibleText(
      page,
      `${productsFromProductRoute.length} 个产品`,
      'product count from live api'
    )
    await expectVisibleText(
      page,
      '中文名称、英文名称为发布必填，其余字段允许资料未完善',
      'product required field description'
    )

    await page.goto(buildFrontendUrl(frontendBase, '/showroom/company'), {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await assertPath(page, '/showroom/company')

    await failIfMissing(
      page.getByRole('button', { name: /编辑公司|保存草稿/ }),
      '缺少真实公司编辑/保存入口：不能只展示“保存草稿”文本，必须提供可点击控件'
    )
    await failIfMissing(
      page.getByRole('button', { name: /提交审批|提交审核|提交/ }),
      '缺少真实公司提交审批入口：编辑人无法从公司工作台发起审批'
    )

    await page.goto(buildFrontendUrl(frontendBase, '/showroom/product'), {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await assertPath(page, '/showroom/product')

    await failIfMissing(
      page.getByRole('button', { name: /编辑字段|编辑产品|保存草稿/ }),
      '缺少真实产品编辑/保存入口：不能只展示“编辑字段”文本，必须提供可点击控件'
    )
    await failIfMissing(
      page.getByRole('button', { name: /提交审批|提交审核|提交/ }),
      '缺少真实产品提交审批入口：编辑人无法从产品工作台发起审批'
    )

    await page.getByRole('tab', { name: '审批中心' }).click()
    await failIfMissing(
      page.getByRole('button', { name: /审批|审核|批准|查看差异/ }),
      '缺少真实审批处理入口：审批中心不能只展示审批路线说明，必须提供可点击审批动作'
    )
  }
}
