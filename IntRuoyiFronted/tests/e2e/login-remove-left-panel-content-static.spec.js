const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const loginPagePath = 'src/views/Login/Login.vue'
const loginFormPath = 'src/views/Login/components/LoginForm.vue'

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const loginPage = readUtf8(loginPagePath)
const loginForm = readUtf8(loginFormPath)

for (const [label, token] of [
  ['left panel namespace', '__left'],
  ['left panel desktop hide class', 'lt-xl:hidden'],
  ['left panel illustration', 'login-box-bg.svg'],
  ['left panel background', 'login-bg.svg'],
  ['welcome text binding', "t('login.welcome')"],
  ['message text binding', "t('login.message')"],
  ['left panel transition animation', 'animate__bounceInLeft'],
  ['left panel dark background class', 'bg-gray-500'],
  ['top-left brand logo class', 'login-page__brand-logo'],
  ['top-left logo asset', '@/assets/imgs/logo.png']
]) {
  assertNotContains(loginPage, token, label)
}

for (const [label, token] of [
  ['login form render', '<LoginForm'],
  ['forget password form render', '<ForgetPasswordForm'],
  ['sso authorization form render', '<SSOLoginVue'],
  ['login platform title remains', '瑛泰数字化平台']
]) {
  assertContains(loginPage, token, label)
}

for (const [label, token] of [
  ['tenant selector remains', 'prop="tenantName"'],
  ['username field remains', 'prop="username"'],
  ['password field remains', 'prop="password"'],
  ['remember me remains', "t('login.remember')"],
  ['forget password remains', "t('login.forgetPassword')"],
  ['primary login button remains', "t('login.login')"],
  ['captcha component remains', '<Verify']
]) {
  assertContains(loginForm, token, label)
}

console.log('PASS: login left panel content is removed while the account login path remains')
