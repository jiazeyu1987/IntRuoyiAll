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
  ['brand logo image', 'login-page__brand-logo'],
  ['login page logo asset', '@/assets/imgs/logo.png'],
  ['empty decorative logo alt', '<img alt=""']
]) {
  assertNotContains(loginPage, token, label)
}

for (const [label, token] of [
  ['brand container remains', 'login-page__brand'],
  ['platform title remains', '瑛泰数字化平台'],
  ['platform title binding remains', '{{ platformTitle }}'],
  ['current white medical background remains', 'login-interventional-medical-bg.png'],
  ['login panel class remains', 'login-page__panel']
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

console.log('PASS: login top-left logo is removed while title and account login remain')
