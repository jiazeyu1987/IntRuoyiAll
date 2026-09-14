import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const passwordMessage = '密码至少 8 位且必须包含大写字母、小写字母、数字和特殊字符'
const passwordEntryFiles = [
  'src/views/Login/components/RegisterForm.vue',
  'src/views/Login/components/ForgetPasswordForm.vue',
  'src/views/Profile/components/ResetPwd.vue',
  'src/views/system/user/UserForm.vue',
  'src/views/system/user/index.vue'
]

test('system password policy has one shared frontend rule source', () => {
  const policySource = readText('src/views/system/user/systemPasswordPolicy.ts')

  assert.match(policySource, /SYSTEM_PASSWORD_MIN_LENGTH\s*=\s*8/)
  assert.match(policySource, /SYSTEM_PASSWORD_MESSAGE/)
  assert.match(policySource, /isSystemPasswordStrong/)
  assert.match(policySource, /systemPasswordRule/)
  assert.match(policySource, /UPPERCASE_PATTERN/)
  assert.match(policySource, /LOWERCASE_PATTERN/)
  assert.match(policySource, /DIGIT_PATTERN/)
  assert.match(policySource, /SPECIAL_CHAR_PATTERN/)
  assert.match(policySource, /\[A-Z\]/)
  assert.match(policySource, /\[a-z\]/)
  assert.match(policySource, /\\d/)
  assert.match(policySource, /!@#/)
  assert.match(policySource, new RegExp(passwordMessage))
})

test('all new-password entry points use the shared rule and old messages are gone', () => {
  for (const relativePath of passwordEntryFiles) {
    const source = readText(relativePath)
    assert.match(source, /systemPasswordRule|isSystemPasswordStrong|SYSTEM_PASSWORD_MESSAGE/, `${relativePath} must reuse shared password policy`)
    assert.doesNotMatch(source, /5 和 20|4 到 16|4-16|6 到 20|密码长度为4到16位|min:\s*4,\s*max:\s*16/, `${relativePath} still contains old password limits`)
  }
})

test('login page keeps password as required-only so expired-password backend flow is reachable', () => {
  const loginSource = readText('src/views/Login/components/LoginForm.vue')

  assert.doesNotMatch(loginSource, /systemPasswordRule|isSystemPasswordStrong|SYSTEM_PASSWORD_MESSAGE/)
  assert.match(loginSource, /password:\s*\[\s*required\s*]/)
})

test('profile copy exposes the unified password rule', () => {
  const zhSource = readText('src/locales/zh-CN.ts')

  assert.match(zhSource, new RegExp(passwordMessage))
  assert.doesNotMatch(zhSource, /长度在 6 到 20 个字符/)
})
