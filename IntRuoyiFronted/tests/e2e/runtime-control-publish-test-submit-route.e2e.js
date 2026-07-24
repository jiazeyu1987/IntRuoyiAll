const assert = require('node:assert/strict')
const {
  fillDialogReason,
  getRuntimeControlActionOrigin,
  openOperationDialog,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

const EXPECTED_ACTION_ORIGIN = getRuntimeControlActionOrigin()

function parsePayload(body) {
  assert.ok(body, 'runtime-control action request must include a JSON body')
  return JSON.parse(body)
}

runRuntimeControlE2E('runtime control publish test submits to backend action route', async ({ page }) => {
  let capturedRequest = null

  await page.route('**/infra/runtime-control/actions**', async (route) => {
    const request = route.request()
    capturedRequest = {
      method: request.method(),
      url: request.url(),
      body: request.postData()
    }
    await route.abort('aborted')
  })

  const dialog = await openOperationDialog(page, '部署发布包到测试服')
  await fillDialogReason(dialog, 'E2E route probe only')
  await page.getByRole('button', { name: '确认执行' }).click()
  await page.waitForTimeout(1000)

  assert.ok(capturedRequest, 'confirming publish-test should emit an action request')
  assert.equal(capturedRequest.method, 'POST')
  assert.equal(
    capturedRequest.url,
    `${EXPECTED_ACTION_ORIGIN}/admin-api/infra/runtime-control/actions`
  )

  const payload = parsePayload(capturedRequest.body)
  assert.equal(payload.action, 'publish-test')
  assert.equal(payload.reason, 'E2E route probe only')
  assert.equal(payload.publishScope, 'code-only')
})
