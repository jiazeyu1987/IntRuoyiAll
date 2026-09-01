import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => readFileSync(path.join(root, relativePath), 'utf8')

const assertNoDefaultCredentials = (source, label) => {
  assert.doesNotMatch(
    source,
    /VITE_APP_DEFAULT_LOGIN_USERNAME|VITE_APP_DEFAULT_LOGIN_PASSWORD/,
    `${label} must not read build-time default username or password variables`
  )
  assert.match(source, /username:\s*''/, `${label} must initialize username as empty`)
  assert.match(source, /password:\s*''/, `${label} must initialize password as empty`)
  assert.match(source, /rememberMe:\s*false/, `${label} must not remember credentials by default`)
  assert.doesNotMatch(source, /username:\s*['"`]admin['"`]/, `${label} must not hardcode admin`)
  assert.doesNotMatch(source, /password:\s*['"`]admin123['"`]/, `${label} must not hardcode admin123`)
}

const loginForm = readSource('src/views/Login/components/LoginForm.vue')
const socialLogin = readSource('src/views/Login/SocialLogin.vue')
const auth = readSource('src/utils/auth.ts')
const env = readSource('.env')

assertNoDefaultCredentials(loginForm, 'LoginForm.vue')
assertNoDefaultCredentials(socialLogin, 'SocialLogin.vue')

const envCredentialAssignments = [...env.matchAll(/^VITE_APP_DEFAULT_LOGIN_(USERNAME|PASSWORD)\s*=\s*(.+)$/gm)]
  .map((match) => match[0])
assert.deepEqual(
  envCredentialAssignments,
  [],
  '.env must not define default login username or password'
)
assert.match(env, /^VITE_APP_DEFAULT_LOGIN_TENANT\s*=/m, '.env may keep a default tenant selector')
assert.doesNotMatch(
  auth,
  /OldDefaultTestTenantName|loginForm\.tenantName\s*===\s*OldDefaultTestTenantName/,
  'legacy default credential cleanup must not depend on one tenant only'
)
assert.match(
  auth,
  /isLegacyDefaultLoginForm/,
  'auth cache cleanup must explicitly remove legacy default credentials'
)
