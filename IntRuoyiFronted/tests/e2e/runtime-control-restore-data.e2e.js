const {
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

async function assertNotVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    if (await locator.nth(index).isVisible()) {
      throw new Error(`${label} should not be visible`)
    }
  }
}

runRuntimeControlE2E('runtime control restore data uses target environment and server candidate picker', async ({ page }) => {
  const dialog = await openOperationDialog(page, '恢复数据')

  await assertVisible(dialog.locator('text=恢复集候选').first(), '恢复集候选')
  await assertVisible(dialog.locator('text=恢复目标').first(), '恢复目标')
  await assertVisible(dialog.getByRole('radio', { name: '测试服' }), '测试服恢复目标')
  await assertVisible(dialog.getByRole('radio', { name: '备份服务器' }), '备份服务器恢复目标')
  await assertVisible(dialog.locator('.candidate-picker').first(), '候选清单')
  await assertNotVisible(dialog.locator('input[placeholder="输入 PROD"]'), '测试服恢复 PROD 确认')
  await dialog.locator('.el-radio-button').filter({ hasText: '备份服务器' }).click()
  await assertVisible(dialog.locator('input[placeholder="输入 PROD"]').first(), '备份服务器恢复 PROD 确认')
  await assertNotVisible(dialog.locator('input[placeholder="例如 20260524_174058"]'), '手填备份输入项')
  await assertNotVisible(dialog.getByRole('radio', { name: '正式服' }), '正式服恢复目标')
  await assertNotVisible(dialog.locator('text=发布范围'), '发布范围')
}).catch((error) => {
  console.error(error)
  process.exitCode = 1
})
