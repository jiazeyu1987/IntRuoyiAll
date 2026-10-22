import assert from 'node:assert/strict'
import fs from 'node:fs'

const loginForm = fs.readFileSync('src/views/Login/components/LoginForm.vue', 'utf8')
const profile = fs.readFileSync('src/views/Profile/Index.vue', 'utf8')
const types = fs.readFileSync('src/api/login/types.ts', 'utf8')

assert.match(types, /passwordChangeRequired\?: boolean/)
assert.match(loginForm, /res\.passwordChangeRequired/)
assert.match(loginForm, /path: '\/user\/profile'/)
assert.match(loginForm, /tab: 'resetPwd'/)
assert.match(profile, /route\.query\.tab === 'resetPwd'/)

console.log('forced password change login contract: PASS')
