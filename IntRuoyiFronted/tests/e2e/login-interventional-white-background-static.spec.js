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
  ['interventional medical background import', 'login-interventional-medical-bg.png'],
  ['interventional background variable', 'loginInterventionalMedicalBg'],
  ['light background color', 'background-color: #f6f9fc'],
  ['light integrated page overlay', 'rgba(255, 255, 255, 0.58)'],
  ['integrated panel background', 'rgba(255, 255, 255, 0.72)'],
  ['dark brand text for light background', 'color: #172033'],
  ['platform title literal', '瑛泰数字化平台'],
  ['login panel class', 'login-page__panel']
]) {
  assertContains(loginPage, token, label)
}

for (const [label, token] of [
  ['old dark medical background import', 'login-medical-tech-bg.png'],
  ['old dark background variable', 'loginMedicalTechBg'],
  ['old dark background color', 'background-color: #071426'],
  ['old dark overlay', 'rgba(3, 10, 28, 0.34)'],
  ['old white brand text', 'color: #fff']
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

console.log('PASS: login page uses the white interventional medical background and keeps account login')
