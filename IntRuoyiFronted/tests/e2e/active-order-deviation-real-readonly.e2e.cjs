const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_E2E_BASE_URL || 'http://127.0.0.1:8094'
const PASSWORD = process.env.EDHR_E2E_PASSWORD || ''
const CHROME_EXECUTABLE =
  process.env.EDHR_E2E_CHROME_EXECUTABLE || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const RESULT_DIR = path.resolve(process.env.EDHR_E2E_RESULT_DIR || 'output/playwright/active-order-deviation-real-readonly')
const CASES = [
  {
    code: 'EDHRB-1790401592564',
    deviations: [
      'PC-202609-0001',
      'PC-202609-0002',
      'PC-202609-0003',
      'PC-202609-0004',
      'PC-202609-0005',
      'PC-202609-0006'
    ]
  },
  { code: 'EDHRB-1790529745273', deviations: [] }
]
const expectedInitiationFields = [
  '发现部门', '发现人', '发现时间', '发现地点', '产品名称', '产品规格', '设备或系统', '报告时间', '接收人',
  '偏差类别', '偏差描述', '紧急措施', '等级依据', '发起电子签名', '签名正文摘要'
]
const expectedHandlingFields = [
  '调查开始', '计划完成', '实际完成', '调查成员', '根因分析', '影响范围', '风险评估', '产品处置',
  '整改责任人', '整改期限', 'CAPA', '验证内容'
]

async function clickMenu(page, label) {
  const text = page.locator('.el-menu:visible').getByText(label, { exact: true }).first()
  await text.waitFor({ state: 'visible', timeout: 60000 })
  const target = text.locator(
    'xpath=ancestor-or-self::*[contains(@class, "el-menu-item") or contains(@class, "el-sub-menu__title")][1]'
  )
  const targetClass = (await target.getAttribute('class')) || ''
  const parentClass = (await target.locator('xpath=..').getAttribute('class')) || ''
  if (targetClass.includes('el-sub-menu__title') && parentClass.includes('is-opened')) return
  await target.click()
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenant = form.locator('input.el-select__input').first()
  await tenant.click()
  await tenant.fill('芋道源码')
  await page.getByText('芋道源码', { exact: true }).last().click()
  await form.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]').first().fill('admin')
  await form.locator('input[placeholder="请输入密码"]').first().fill(PASSWORD)
  await form.getByRole('button', { name: /^登录$/ }).click()
  await page.waitForURL(url => url.pathname === '/index', { timeout: 90000 })
}

async function openBatchList(page) {
  await clickMenu(page, 'MES 系统')
  await clickMenu(page, 'eDHR批记录')
  await clickMenu(page, '批次执行')
  await page.waitForURL(url => url.pathname === '/mes/pro/feedback/edhr-batch-execution', { timeout: 60000 })
  await page.locator('.el-table').first().waitFor({ state: 'visible', timeout: 60000 })
}

async function inspectBatch(page, record) {
  const filter = page.locator('.table-multi-filter').first()
  const value = filter.locator('.table-multi-filter-field[data-filter-key="batchExecutionCode"] .table-multi-filter-field__value input').first()
  if (!(await value.isVisible().catch(() => false))) {
    await filter.getByRole('button', { name: '新增筛选条件' }).click()
  }
  await value.waitFor({ state: 'visible', timeout: 30000 })
  await value.fill(record.code)
  await filter.getByRole('button', { name: '查询', exact: true }).click()

  const row = page.locator('.el-table__body tr').filter({ hasText: record.code }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.locator('[data-edhr-batch-active-order-detail]').click()
  await page.waitForURL(url => url.pathname.endsWith('/active-order-detail'), { timeout: 60000 })
  await page.getByRole('tab', { name: '偏差', exact: true }).click()

  const pane = page.getByRole('tabpanel', { name: '偏差' })
  await pane.waitFor({ state: 'visible', timeout: 30000 })
  const details = []
  if (record.deviations.length) {
    for (const code of record.deviations) {
      await pane.getByText(code, { exact: true }).waitFor({ state: 'visible', timeout: 60000 })
    }
    for (const [code, expectedContent] of [
      ['PC-202609-0001', ['常规闭环', '当前版本电子签名证据', ...expectedHandlingFields]],
      ['PC-202609-0005', ['未处理', '尚未填写处理内容']],
      ['PC-202609-0006', [
        '转不合格审批关闭', '关联不合格评审', 'EDHR-NCR-20260926235142-900000001150',
        '关联不合格审批处置', '评审实际意见', 'QA处置签署人', '处置签名记录', '签名记录编号',
        '处置签署时间', '签名正文摘要', '让步放行'
      ]]
    ]) {
      const row = pane.locator('.el-table__body tr').filter({ hasText: code }).first()
      await row.getByRole('button', { name: /查看发起与处理/ }).click()
      const detail = page.getByRole('dialog', { name: /偏差详情/ })
      await detail.waitFor({ state: 'visible', timeout: 30000 })
      for (const text of expectedContent) {
        await detail.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
      }
      const detailText = await detail.innerText()
      for (const label of expectedInitiationFields) {
        assert.ok(detailText.includes(label), `${code} must show initiation field: ${label}`)
      }
      assert.match(detailText, /已签署 #\d+/, `${code} must show the initiation signature`)
      if (code === 'PC-202609-0001') {
        assert.match(detailText, /P6真实偏差发起/, 'normal deviation must show the initiated description')
        assert.match(detailText, /VALID/, 'normal deviation must show valid current signature evidence')
        assert.match(detailText, /CURRENT_VALID/, 'normal deviation must show valid signature history')
      }
      if (code === 'PC-202609-0006') {
        assert.match(detailText, /QA处置签署人\s+用户 #\d+/, 'NCR detail must show the QA signer')
        assert.match(detailText, /签名记录编号\s+\d+/, 'NCR detail must show the signature record id')
        assert.match(detailText, /处置签署时间\s+\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/, 'NCR detail must show the signed time')
        assert.match(detailText, /签名正文摘要\s+[a-f0-9]{64}/i, 'NCR detail must show the aggregate signature hash')
      }
      assert.equal(
        await detail.getByRole('button', { name: /开始处理|保存处理|签署当前节点|常规关闭|发起不合格审批/ }).count(),
        0,
        '活跃订单偏差追溯详情必须只读'
      )
      details.push({ deviationCode: code, visibleText: detailText.slice(0, 6000) })
      await detail.getByRole('button', { name: /Close this dialog|关闭/ }).first().click()
      await detail.waitFor({ state: 'hidden', timeout: 30000 })
    }
  } else {
    await pane.getByText('没有偏差', { exact: true }).waitFor({ state: 'visible', timeout: 60000 })
  }
  const text = await pane.innerText()
  assert.doesNotMatch(text, /暂无正式批记录/)
  await page.screenshot({ path: path.join(RESULT_DIR, `${record.deviations.length ? 'linked' : 'empty'}-deviation.png`), fullPage: true })
  return {
    batchExecutionCode: record.code,
    expectedDeviationCount: record.deviations.length,
    visibleText: text.slice(0, 1800),
    details: record.deviations.length ? details : []
  }
}

async function main() {
  assert.ok(PASSWORD, 'EDHR_E2E_PASSWORD must be supplied outside the test script')
  assert.ok(fs.existsSync(CHROME_EXECUTABLE), `Chrome executable not found: ${CHROME_EXECUTABLE}`)
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: CHROME_EXECUTABLE })
  const page = await browser.newPage()
  const pageErrors = []
  page.on('pageerror', error => pageErrors.push(error.message))

  try {
    await login(page)
    await openBatchList(page)
    const results = []
    results.push(await inspectBatch(page, CASES[0]))
    await page.getByRole('button', { name: '返回', exact: true }).click()
    await page.waitForURL(url => url.pathname === '/mes/pro/feedback/edhr-batch-execution', { timeout: 60000 })
    results.push(await inspectBatch(page, CASES[1]))
    assert.deepEqual(pageErrors, [], '活跃订单详情偏差页签不能产生未处理的页面异常')
    const report = { status: 'PASS', results, pageErrors }
    fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(report, null, 2)}\n`, 'utf8')
    console.log(JSON.stringify(report, null, 2))
  } catch (error) {
    await page.screenshot({ path: path.join(RESULT_DIR, 'failure.png'), fullPage: true }).catch(() => undefined)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch(error => {
  console.error(error.stack || error)
  process.exitCode = 1
})
