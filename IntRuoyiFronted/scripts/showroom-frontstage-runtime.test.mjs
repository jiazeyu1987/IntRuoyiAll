import assert from 'node:assert/strict'
import test from 'node:test'

const adminApiBaseUrl = process.env.INT_RUOYI_ADMIN_API_BASE || 'http://127.0.0.1:48081/admin-api'
const publicBaseUrl = new URL('/', adminApiBaseUrl).toString().replace(/\/$/, '')
const tenantId = process.env.INT_RUOYI_TENANT_ID || '1'
const username = process.env.INT_RUOYI_USERNAME || 'admin'
const password = process.env.INT_RUOYI_PASSWORD || 'admin123'

const login = async () => {
  const response = await fetch(`${adminApiBaseUrl}/system/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'tenant-id': tenantId
    },
    body: JSON.stringify({ username, password })
  })
  assert.equal(response.status, 200, 'login http status')
  const payload = await response.json()
  assert.equal(payload.code, 0, `login failed: ${payload.msg || 'unknown error'}`)
  assert.ok(payload.data?.accessToken, 'login accessToken missing')
  return payload.data.accessToken
}

const requestDisplay = async (token, path) => {
  const response = await fetch(`${publicBaseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': tenantId
    }
  })
  const payload = await response.json()
  return { response, payload }
}

const assertCommonResultOk = (payload, context) => {
  assert.equal(payload.code, 0, `${context}: ${payload.msg || 'request failed'}`)
  assert.ok(payload.data && typeof payload.data === 'object', `${context}: data missing`)
}

const token = await login()

test('showroom website-config aggregate endpoint is exposed in the live runtime', async () => {
  const { response, payload } = await requestDisplay(token, '/showroom/display/website-config')
  assert.equal(response.status, 200, 'website-config http status')
  assertCommonResultOk(payload, 'website-config endpoint')
  assert.ok(payload.data.company && typeof payload.data.company === 'object', 'company block missing')
  assert.ok(typeof payload.data.company.name === 'string' && payload.data.company.name.trim(), 'company name missing')
  assert.ok(Array.isArray(payload.data.company.bilingualPublicFields), 'company bilingualPublicFields missing')
  assert.ok(typeof payload.data.company.subtitleZh === 'string', 'company subtitleZh missing')
  assert.ok(typeof payload.data.company.audioZhUrl === 'string', 'company audioZhUrl missing')
  assert.ok(Array.isArray(payload.data.showrooms), 'showrooms missing')
  assert.ok(payload.data.showrooms.length > 0, 'showrooms should not be empty')

  const firstShowroom = payload.data.showrooms[0]
  assert.ok(Array.isArray(firstShowroom.products), 'showroom products missing')
  assert.ok(firstShowroom.products.length > 0, 'showroom products should not be empty')

  const firstProduct = firstShowroom.products[0]
  assert.ok(Array.isArray(firstProduct.bilingualPublicFields), 'product bilingualPublicFields missing')
  assert.ok(typeof firstProduct.subtitleZh === 'string', 'product subtitleZh missing')
  assert.ok(typeof firstProduct.audioZhUrl === 'string', 'product audioZhUrl missing')
  assert.equal(
    firstProduct.bilingualPublicFields.some((field) => field.fieldCode === 'registration_certificate'),
    false,
    'advanced product field should stay excluded from website-config'
  )
})
