import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd().endsWith('IntRuoyiFronted') ? join(process.cwd(), '..') : process.cwd()
const doc = readFileSync(
  join(root, 'e2e_test/registration/upload/registration-certificate-upload-e2e-acceptance.md'),
  'utf8'
)

assert.match(
  doc,
  /上传人字段按系统用户真实姓名展示；本账号当前预期显示为 `王立轩`，并在证据中另行记录登录身份标签为 `wanglixuan`。/,
  'upload E2E doc must distinguish visible real name from login account label'
)
assert.match(
  doc,
  /页面中的用户字段优先按真实姓名展示；账号名仅作为登录身份标签和审计证据记录/,
  'execution notes must prevent treating real-name display as an account mismatch'
)
assert.doesNotMatch(
  doc,
  /上传人显示 `wanglixuan`/,
  'upload E2E doc must not require the visible uploader field to display the login account'
)

console.log('PASS: registration certificate upload E2E doc treats visible real names as correct')
