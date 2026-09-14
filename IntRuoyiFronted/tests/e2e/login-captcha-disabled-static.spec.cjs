const assert = require('assert')
const fs = require('fs')
const path = require('path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const read = (...segments) => fs.readFileSync(path.join(...segments), 'utf8')

const loginForm = read(frontendRoot, 'src/views/Login/components/LoginForm.vue')
const loginTypes = read(frontendRoot, 'src/api/login/types.ts')
const backendAuthService = read(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/auth/AdminAuthServiceImpl.java'
)

assert(
  !loginForm.includes('<Verify'),
  'LoginForm must not render graphical Verify during account-password login.'
)
assert(
  !loginForm.includes('verify.value.show'),
  'LoginForm must submit directly instead of showing graphical captcha.'
)
assert(
  !/captchaEnable\s*:/.test(loginForm),
  'LoginForm must not depend on the global graphical captcha switch.'
)
assert(
  !loginForm.includes('loginDataLoginForm.captchaVerification'),
  'LoginForm must not inject captchaVerification into login payload.'
)
assert(
  !/captchaVerification\s*:/.test(loginForm),
  'LoginForm state must not require captchaVerification.'
)
assert(
  /captchaVerification\?\s*:\s*string/.test(loginTypes),
  'UserLoginVO.captchaVerification must be optional when login captcha is disabled.'
)

const loginMethodMatch = backendAuthService.match(
  /public AuthLoginRespVO login\(AuthLoginReqVO reqVO\) \{([\s\S]*?)\n    \}/
)
assert(
  loginMethodMatch,
  'AdminAuthServiceImpl.login method must be locatable.'
)
assert(
  !loginMethodMatch[1].includes('validateCaptcha(reqVO)'),
  'AdminAuthServiceImpl.login must not require graphical captcha validation.'
)
assert(
  /public AuthLoginRespVO register\(AuthRegisterReqVO registerReqVO\) \{[\s\S]*?validateCaptcha\(registerReqVO\)/.test(
    backendAuthService
  ),
  'Register captcha validation must remain scoped to register flow.'
)

console.log('GREEN: login-captcha-disabled-static -> PASS')
