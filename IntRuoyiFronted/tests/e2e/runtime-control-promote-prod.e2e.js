const {
  assertNoOperationRequest,
  fillDialogReason,
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

runRuntimeControlE2E('runtime control promote production requires PROD confirmation', async ({
  page,
  requests
}) => {
  const getBlockedPostCount = await blockRuntimeControlActions(page)
  const dialog = await openOperationDialog(page, '上线已验证发布包')

  await assertVisible(dialog.locator('text=发布范围').first(), '发布范围')
  await assertRadioChecked(dialog, '只发代码')

  await fillDialogReason(dialog, '安全 E2E 验证：未输入 PROD 时不得上线正式服')

  const prodConfirmInput = dialog.locator('input[placeholder="输入 PROD"]').first()
  await assertVisible(prodConfirmInput, '生产确认输入框')
  const prodConfirmValue = await prodConfirmInput.inputValue()
  if (prodConfirmValue !== '') {
    throw new Error(`生产确认输入框 should be empty before submit, got: ${prodConfirmValue}`)
  }

  await dialog.locator('button:has-text("确认执行")').click()
  await assertVisible(page.locator('text=生产相关操作必须输入 PROD').first(), '生产确认提示')
  await assertNoOperationRequest(requests, page, '上线已验证发布包未输入 PROD')

  if (getBlockedPostCount() !== 0) {
    throw new Error('上线已验证发布包未输入 PROD should not reach the route-level action POST guard')
  }
}).catch((error) => {
  console.error(error)
  process.exitCode = 1
})
