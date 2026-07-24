const {
  assertNoOperationRequest,
  openOperationDialog,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

async function assertVisible(locator, label) {
  try {
    await locator.waitFor({ state: 'visible', timeout: 5000 })
  } catch (error) {
    throw new Error(`${label} should be visible. ${error.message}`)
  }
}

async function assertRadioChecked(dialog, label) {
  const radio = dialog.locator('.el-radio-button').filter({ hasText: label }).first()
  await assertVisible(radio, `${label} radio`)

  const checked = await radio.locator('input[type="radio"]').evaluate((input) => input.checked)
  if (!checked) {
    throw new Error(`${label} radio should be selected by default`)
  }
}

async function blockRuntimeControlActions(page) {
  let blockedPostCount = 0
  await page.route('**/infra/runtime-control/actions**', async (route) => {
    if (route.request().method() === 'POST') {
      blockedPostCount += 1
      await route.abort('blockedbyclient')
      return
    }
    await route.continue()
  })
  return () => blockedPostCount
}

runRuntimeControlE2E('runtime control publish test safety dialog', async ({ page, requests }) => {
  const getBlockedPostCount = await blockRuntimeControlActions(page)
  const dialog = await openOperationDialog(page, '部署发布包到测试服')

  await assertVisible(dialog.locator('text=发布范围').first(), '发布范围')
  await assertRadioChecked(dialog, '只发代码')

  await dialog.locator('.el-radio-button').filter({ hasText: '带数据发布' }).first().click()
  await assertVisible(
    dialog.locator('text=覆盖目标环境数据库和文件对象').first(),
    '带数据发布风险提示'
  )

  await dialog.locator('button:has-text("确认执行")').click()
  await assertNoOperationRequest(requests, page, '部署发布包到测试服缺少操作原因')

  if (getBlockedPostCount() !== 0) {
    throw new Error('部署发布包到测试服缺少操作原因 should not reach the route-level action POST guard')
  }
}).catch((error) => {
  console.error(error)
  process.exitCode = 1
})
