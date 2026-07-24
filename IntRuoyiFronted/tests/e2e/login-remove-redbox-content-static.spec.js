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
  ['theme switch import or usage', 'ThemeSwitch'],
  ['locale dropdown import or usage', 'LocaleDropdown'],
  ['top-right control spacing shell', 'space-x-10px h-48px']
]) {
  assertNotContains(loginPage, token, label)
}

for (const [label, token] of [
  ['mobile login button', "t('login.btnMobile')"],
  ['qr code login button', "t('login.btnQRCode')"],
  ['register button', "t('login.btnRegister')"],
  ['other login divider', "t('login.otherLogin')"],
  ['beginner links divider', '萌新必读'],
  ['developer guide link', 'https://doc.iocoder.cn/'],
  ['video guide link', 'https://doc.iocoder.cn/video/'],
  ['interview guide link', 'https://www.iocoder.cn/Interview/good-collection/'],
  ['outsourcing consultation link', 'http://static.yudao.iocoder.cn/mp/Aix9975.jpeg'],
  ['social login icon list', 'socialList'],
  ['social login handler', 'doSocialLogin'],
  ['wechat social icon', 'ant-design:wechat-filled'],
  ['dingtalk social icon', 'ant-design:dingtalk-circle-filled'],
  ['github social icon', 'ant-design:github-filled'],
  ['alipay social icon', 'ant-design:alipay-circle-filled']
]) {
  assertNotContains(loginForm, token, label)
}

for (const [label, token] of [
  ['login form render', '<LoginForm'],
  ['forget password form render', '<ForgetPasswordForm'],
  ['sso authorization form render', '<SSOLoginVue']
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

console.log('PASS: login red-box content is removed while the account login path remains')
