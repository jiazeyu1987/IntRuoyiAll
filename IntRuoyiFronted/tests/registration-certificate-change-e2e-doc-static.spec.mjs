import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const docs = [
  ['change', 'e2e_test/registration/biangeng/registration-certificate-change-e2e-acceptance.md'],
  ['qa change', 'docs/qa/registration-certificate-change-e2e-acceptance.md'],
  ['upload', 'e2e_test/registration/upload/registration-certificate-upload-e2e-acceptance.md']
]

for (const [name, path] of docs) {
  const text = readFileSync(join(root, path), 'utf8')
  assert.match(text, /下载授权有效期：普通用户下载申请审批通过后的有效期为 24 小时/, `${name} doc must use 24-hour grant wording`)
  assert.doesNotMatch(text, /3\s*天|三天|72\s*小时/, `${name} doc must not keep stale 3-day wording`)
  assert.match(text, /缺少项目代码的注册证(?:或变更批件)?文件也允许普通用户申请下载/, `${name} doc must allow missing project code download requests`)
  assert.match(text, /项目代码为空时审批摘要允许为空，下载文件名保留空项目代码段/, `${name} doc must define empty project-code approval and filename behavior`)
}

console.log('registration certificate change/upload E2E docs use 24-hour optional-project-code download rules')
