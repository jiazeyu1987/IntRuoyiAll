const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const baseUrl = (process.env.MES_ROUTE_PRODUCT_STANDARD_LIST_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const required = (name) => {
  assert.ok(process.env[name], `缺少真实页面验证配置：${name}`)
  return process.env[name]
}

async function panelSize(page) {
  return page.locator('.route-flow-graph-designer__panel').evaluate((panel) => {
    const detail = panel.querySelector('[data-flow-panel="selected-field-detail"]')
    return { width: panel.getBoundingClientRect().width, client: detail.clientWidth, scroll: detail.scrollWidth }
  })
}

async function checkWidth(page) {
  const size = await panelSize(page)
  assert.equal(size.width, 260, '面板应与生产组长共用 260px 宽度')
  assert.ok(size.scroll <= size.client + 1, '关联产品内容不能横向溢出')
}

async function main() {
  assert.ok(['localhost', '127.0.0.1', '[::1]'].includes(new URL(baseUrl).hostname), '真实验证只允许本机')
  const tenant = required('MES_ROUTE_PRODUCT_STANDARD_LIST_TENANT')
  const username = required('MES_ROUTE_PRODUCT_STANDARD_LIST_USERNAME')
  const password = required('MES_ROUTE_PRODUCT_STANDARD_LIST_PASSWORD')
  const browser = await chromium.launch({
    headless: process.env.MES_ROUTE_PRODUCT_STANDARD_LIST_HEADED !== '1',
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
  })
  try {
    const page = await browser.newPage({ viewport: { width: 1680, height: 900 } })
    page.setDefaultTimeout(30000)
    await page.goto(`${baseUrl}/login`)
    const login = page.locator('form.login-form')
    await login.getByRole('combobox').fill(tenant)
    await page.getByRole('option', { name: tenant, exact: true }).click()
    await login.getByRole('textbox', { name: '请输入用户名' }).fill(username)
    await login.getByRole('textbox', { name: '请输入密码' }).fill(password)
    await login.getByRole('button', { name: '登录', exact: true }).click()
    await page.waitForURL((url) => !url.pathname.includes('/login'))
    await page.goto(`${baseUrl}/mes/pro/route`)
    await page.getByRole('button', { name: '编辑', exact: true }).first().click()
    await page.getByRole('button', { name: '工序开始', exact: true }).click()
    await page.getByRole('button', { name: '查看生产组长字段明细', exact: true }).click()
    assert.equal((await panelSize(page)).width, 260)
    await page.getByRole('button', { name: '设置关联产品', exact: true }).click()
    const list = page.locator('.route-product-list')
    await list.waitFor()
    await page.waitForFunction(() => {
      const root = document.querySelector('.route-product-list')
      return root && !root.querySelector('.el-loading-mask') &&
        !!root.querySelector('.route-product-list__row, .route-product-list__empty, .el-alert')
    })
    assert.equal(await list.locator('.el-alert').count(), 0, '关联产品必须加载成功')
    assert.equal(await list.locator('.unified-list-template, .el-table, .el-pagination').count(), 0)
    await checkWidth(page)
    await page.getByRole('button', { name: '最大化', exact: true }).click()
    await checkWidth(page)
    await page.getByRole('button', { name: '恢复', exact: true }).click()
    assert.ok(await list.locator('.route-product-list__row').count(), '当前路线没有关联产品，无法核验菜单及编码跳转；需准备真实绑定数据')
    const code = await list.locator('.route-product-list__code').first().innerText()
    const more = list.getByRole('button', { name: `维护产品 ${code}`, exact: true }).first()
    assert.ok(await more.isEnabled(), '需使用已有产品绑定的可编辑草稿版本核验维护菜单')
    await more.click()
    await page.getByRole('menuitem', { name: '编辑', exact: true }).click()
    const edit = page.getByRole('dialog', { name: '编辑产品', exact: true })
    await edit.waitFor()
    await edit.getByRole('button', { name: '取 消', exact: true }).click()
    await more.click()
    await page.getByRole('menuitem', { name: '复制', exact: true }).click()
    const copy = page.getByRole('dialog', { name: '复制产品', exact: true })
    await copy.waitFor()
    await copy.getByRole('button', { name: '取 消', exact: true }).click()
    assert.equal(await list.getByRole('button', { name: `删除关联产品 ${code}`, exact: true }).count(), 1)
    await list.locator('.route-product-list__code').first().click()
    await page.waitForURL((url) => url.searchParams.has('openDetailId'))
    assert.equal(new URL(page.url()).searchParams.get('code'), code)
    console.log('mes-route-product-lightweight-list-real PASS (只读验证，未提交业务变更)')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
