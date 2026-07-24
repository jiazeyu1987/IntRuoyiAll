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

runRuntimeControlE2E('runtime control rollback uses server candidate picker only', async ({ page }) => {
  const dialog = await openOperationDialog(page, '回滚版本')

  await assertVisible(dialog.locator('text=回滚目标').first(), '回滚目标')
  await assertVisible(dialog.locator('.el-radio-button:has-text("测试服")').first(), '测试服回滚目标')
  await assertVisible(dialog.locator('.el-radio-button:has-text("备份服务器")').first(), '备份服务器回滚目标')
  await assertNotVisible(dialog.locator('.el-radio-button:has-text("正式服")'), '正式服回滚目标')
  await assertVisible(dialog.locator('text=版本候选').first(), '版本候选')
  await assertVisible(dialog.locator('.candidate-picker').first(), '候选清单')
  await assertNotVisible(dialog.locator('input[placeholder="例如 20260524_035800"]'), '手填版本输入项')
  await assertNotVisible(dialog.locator('text=发布范围').first(), '发布范围')
}).catch((error) => {
  console.error(error)
  process.exitCode = 1
})
