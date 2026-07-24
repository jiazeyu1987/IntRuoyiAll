import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const phaseScripts = [
  'approval-center-phase2-real.e2e.mjs',
  'approval-center-phase3-real.e2e.mjs',
  'approval-center-phase6-srm-real.e2e.mjs',
  'approval-center-phase8-mes-feedback-real.e2e.mjs'
]

for (const fileName of phaseScripts) {
  const source = fs.readFileSync(path.join(__dirname, fileName), 'utf8')

  assert.match(source, /pageTitle:\s*'审批中心'/, `${fileName} must wait for current title 审批中心`)
  assert.match(source, /APPROVAL_CENTER_E2E_BASE_URL\s*\|\|\s*'http:\/\/127\.0\.0\.1:8081'/,
    `${fileName} must default to main int_main frontend port 8081`)
  assert.match(source, /APPROVAL_CENTER_E2E_BACKEND_URL\s*\|\|\s*'http:\/\/127\.0\.0\.1:48081'/,
    `${fileName} must keep backend metadata aligned to main int_main port 48081`)
  assert.doesNotMatch(source, /startsWith\(config\.backendUrl\)/,
    `${fileName} must not require browser responses to directly hit backendUrl`)
  assert.match(source, /\/admin-api\/approval-center\//,
    `${fileName} must assert the current frontend proxy /admin-api approval-center path`)
  assert.doesNotMatch(source, /getByRole\('tab'/,
    `${fileName} must not use retired approval-center tab DOM; current navigation is route/sidebar based`)
  assert.match(source, /\.el-select-dropdown__item:visible/,
    `${fileName} must select the visible Element Plus tenant option instead of relying on Enter`)
  assert.doesNotMatch(source, /tenantInput\.press\('Enter'\)/,
    `${fileName} must not rely on Enter to select tenant; current login requires clicking the visible option`)
}
