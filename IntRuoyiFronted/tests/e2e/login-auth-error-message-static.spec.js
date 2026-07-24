const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const useLogin = read('src/views/Login/components/useLogin.ts')
const loginForm = read('src/views/Login/components/LoginForm.vue')

assert.match(
  useLogin,
  /message\.includes\('账号密码'\).*message\.includes\('密码不正确'\).*message\.includes\('用户不存在'\)/s,
  '登录认证失败分类必须把“用户不存在”归并到统一账号密码错误提示，覆盖用户名带空格导致的用户不存在场景。'
)

assert.match(
  useLogin,
  /return '账号或密码错误：请核对当前租户、账号和密码后重试。'/,
  '登录认证失败必须统一输出账号密码错误提示。'
)

assert.match(
  loginForm,
  /loginErrorMessage/,
  '登录页必须在表单内渲染统一错误提示，不能依赖后端原始错误文案透传。'
)

console.log('PASS: login auth error message static contract')
