const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const loginPagePath = 'src/views/Login/Login.vue'
const loginFormPath = 'src/views/Login/components/LoginForm.vue'
const backgroundPath = 'src/assets/imgs/login-interventional-medical-bg.png'

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
const backgroundAbsolutePath = path.join(repoRoot, backgroundPath)

if (!fs.existsSync(backgroundAbsolutePath)) {
  throw new Error(`missing generated background asset: ${backgroundPath}`)
}

const backgroundBytes = fs.readFileSync(backgroundAbsolutePath)
const pngSignature = backgroundBytes.subarray(0, 8).toString('hex')
if (pngSignature !== '89504e470d0a1a0a') {
  throw new Error(`background asset must be a PNG: ${backgroundPath}`)
}
if (backgroundBytes.length < 500_000) {
  throw new Error(`background asset is too small for a premium login background: ${backgroundBytes.length}`)
}

for (const [label, token] of [
  ['medical background import', 'login-interventional-medical-bg.png'],
  ['platform title literal', '瑛泰数字化平台'],
  ['login root class', 'login-page'],
  ['brand class', 'login-page__brand'],
  ['login panel class', 'login-page__panel'],
  ['background style binding', 'loginBackgroundStyle']
]) {
  assertContains(loginPage, token, label)
}

for (const [label, token] of [
  ['old login page app title usage', 'appStore.getTitle'],
  ['old title import', "import { useAppStore } from '@/store/modules/app'"],
  ['old platform name literal', '瑛泰管理系统'],
  ['old dark background asset', 'login-medical-tech-bg.png']
]) {
  assertNotContains(loginPage, token, label)
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

console.log('PASS: login page uses the current medical background, platform title, and account login path')
