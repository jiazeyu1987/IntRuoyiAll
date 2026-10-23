const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const loginForm = read('src/views/Login/components/LoginForm.vue')
const loginApi = read('src/api/login/index.ts')
const auth = read('src/utils/auth.ts')
const requiredPasswordChangeFormStart = loginForm.lastIndexOf(
  '<el-form',
  loginForm.indexOf('data-testid="required-password-change-form"')
)
const requiredPasswordChangeFormEnd = loginForm.indexOf('</el-form>', requiredPasswordChangeFormStart)
const requiredPasswordChangeForm = loginForm.slice(
  requiredPasswordChangeFormStart,
  requiredPasswordChangeFormEnd + '</el-form>'.length
)
const passwordChangeSubmit = loginForm.match(
  /const submitRequiredPasswordChange = async \(\) => \{[\s\S]*?\n\}/
)?.[0]

assert.match(loginForm, /AUTH_LOGIN_PASSWORD_CHANGE_REQUIRED|1002000011/)
assert.match(loginForm, /AUTH_LOGIN_PASSWORD_EXPIRED|1002000009/)
assert.match(loginForm, /changePasswordForm\.oldPassword/)
assert.match(loginForm, /changePasswordForm\.newPassword/)
assert.match(loginForm, /changePasswordForm\.confirmPassword/)
assert.match(loginForm, /LoginApi\.changePasswordBeforeLogin/)
assert.match(loginForm, /await LoginApi\.login[\s\S]*authUtil\.setToken/)
assert.match(loginForm, /password: newPassword, rememberMe: false/)
assert.match(loginForm, /let passwordChangeAttempted = false[\s\S]*passwordChangeAttempted = true/)
assert.match(loginForm, /if \(passwordChangeAttempted\)[\s\S]*changePasswordForm\.oldPassword = ''[\s\S]*changePasswordForm\.newPassword = ''[\s\S]*changePasswordForm\.confirmPassword = ''/)
assert.ok(requiredPasswordChangeForm, 'required password change form must exist')
assert.match(requiredPasswordChangeForm, /@submit\.prevent="submitRequiredPasswordChange"/)
assert.match(requiredPasswordChangeForm, /<XButton[\s\S]*?native-type="submit"[\s\S]*?\/>/)
assert.doesNotMatch(requiredPasswordChangeForm, /@click=|@keyup\.enter=/, 'click and Enter must use the form submit path only')
assert.ok(passwordChangeSubmit, 'required password change submit handler must exist')
assert.match(
  passwordChangeSubmit,
  /if \(passwordChangeSubmitting\.value\) return[\s\S]*passwordChangeSubmitting\.value = true[\s\S]*try \{/,
  'submit handler must synchronously guard repeated submits before its first await'
)
assert.match(passwordChangeSubmit, /finally \{[\s\S]*passwordChangeSubmitting\.value = false/)
assert.match(loginApi, /url:\s*'\/system\/auth\/change-password-before-login'[\s\S]*data/)
assert.match(loginApi, /changePasswordBeforeLogin[\s\S]*isToken:\s*false/)
assert.doesNotMatch(loginApi, /change-password-before-login\?[^'"`]*password/i)
assert.doesNotMatch(`${loginApi}\n${loginForm}\n${auth}`, /(?:localStorage|sessionStorage)\.(?:setItem|getItem)\([^)]*password/i)
const cacheWriter = auth.match(/const cacheSafeLoginForm = \(loginForm: LoginFormType\) => \{[\s\S]*?\n\}/)?.[0]
assert.ok(cacheWriter, 'login cache writer must exist')
assert.doesNotMatch(cacheWriter, /password/i, 'login cache writer must not persist a password')
assert.match(auth, /decryptedLoginForm\.password = ''/, 'legacy cached passwords must be removed')

console.log('PASS: required pre-login password change static contract')
