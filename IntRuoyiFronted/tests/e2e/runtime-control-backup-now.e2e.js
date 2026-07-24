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

async function assertTextEquals(locator, expected, label) {
  await assertVisible(locator, label)
  const actual = ((await locator.textContent()) || '').trim()
  if (actual !== expected) {
    throw new Error(`${label} should be ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`)
  }
}

async function assertInputValueEquals(locator, expected, label) {
  await assertVisible(locator, label)
  const actual = await locator.inputValue()
  if (actual !== expected) {
    throw new Error(`${label} should be ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`)
  }
}

async function assertNoVisibleText(root, text, label) {
  const matches = root.getByText(text, { exact: true })
  const count = await matches.count()
  for (let index = 0; index < count; index += 1) {
    if (await matches.nth(index).isVisible()) {
      throw new Error(`${label} should not be visible`)
    }
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

runRuntimeControlE2E('runtime control backup now requires PROD confirmation', async ({ page, requests }) => {
  const getBlockedPostCount = await blockRuntimeControlActions(page)
  const dialog = await openOperationDialog(page, '立即备份')

  await assertTextEquals(dialog.locator('.el-dialog__title').first(), '立即备份', 'dialog title')
  await assertInputValueEquals(
    dialog.locator('.el-form-item').filter({ hasText: '动作' }).locator('input').first(),
    '立即备份',
    'operation action'
  )
  await assertNoVisibleText(dialog, '发布范围', '发布范围')

  await fillDialogReason(dialog, '验证立即备份未输入 PROD 不应提交')
  await assertInputValueEquals(
    dialog.locator('input[placeholder="输入 PROD"]').first(),
    '',
    'production confirmation'
  )

  await dialog.locator('button:has-text("确认执行")').click()
  await assertVisible(page.getByText('生产相关操作必须输入 PROD', { exact: true }), 'PROD warning')
  await assertNoOperationRequest(requests, page, '立即备份未输入 PROD')

  if (getBlockedPostCount() !== 0) {
    throw new Error('立即备份未输入 PROD should not reach the route-level action POST guard')
  }
}).catch((error) => {
  console.error(error)
  process.exitCode = 1
})
