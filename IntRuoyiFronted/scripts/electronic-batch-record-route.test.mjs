import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const backendBase = process.env.INT_RUOYI_ADMIN_API_BASE || 'http://127.0.0.1:48081/admin-api'
const tenantId = process.env.INT_RUOYI_TENANT_ID || '122'
const username = process.env.INT_RUOYI_USERNAME || 'aoteman'
const password = process.env.INT_RUOYI_PASSWORD || 'admin123'
const expectedRoutePath = '/mes/pro/batch-record-template'
const expectedMenuName = '电子批记录'

async function login() {
  const response = await fetch(`${backendBase}/system/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'tenant-id': tenantId
    },
    body: JSON.stringify({ username, password })
  })
  assert.equal(response.status, 200, `login_http_status_expected_200_actual_${response.status}`)
  const payload = await response.json()
  assert.equal(payload.code, 0, `login_code_expected_0_actual_${payload.code}_msg_${payload.msg || ''}`)
  const accessToken = payload?.data?.accessToken
  assert.ok(accessToken, 'login_access_token_missing')
  return accessToken
}

async function fetchPermissionInfo(accessToken) {
  const response = await fetch(`${backendBase}/system/auth/get-permission-info`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'tenant-id': tenantId
    }
  })
  assert.equal(
    response.status,
    200,
    `permission_info_http_status_expected_200_actual_${response.status}`
  )
  const payload = await response.json()
  assert.equal(
    payload.code,
    0,
    `permission_info_code_expected_0_actual_${payload.code}_msg_${payload.msg || ''}`
  )
  return payload?.data || {}
}

function findMenuItem(menus) {
  const stack = [...menus]
  while (stack.length > 0) {
    const item = stack.pop()
    if (!item || typeof item !== 'object') {
      continue
    }
    if (String(item.path || '') === 'batch-record-template' || String(item.name || '') === expectedMenuName) {
      return item
    }
    stack.push(...(Array.isArray(item.children) ? item.children : []))
  }
  return null
}

function resolveViewPath(componentPath) {
  const normalized = String(componentPath || '').trim()
  assert.ok(normalized, 'electronic_batch_record_menu_component_missing')
  const root = process.cwd()
  return [
    path.join(root, 'src', 'views', `${normalized}.vue`),
    path.join(root, 'src', 'views', `${normalized}.tsx`)
  ]
}

test('electronic batch record menu target resolves to a real frontend view file', async () => {
  const accessToken = await login()
  const permissionInfo = await fetchPermissionInfo(accessToken)
  const menus = Array.isArray(permissionInfo.menus) ? permissionInfo.menus : []
  const menuItem = findMenuItem(menus)
  assert.ok(menuItem, 'electronic_batch_record_menu_not_found')
  assert.equal(
    `/mes/pro/${String(menuItem.path || '').trim()}`,
    expectedRoutePath,
    'electronic_batch_record_menu_path_unexpected'
  )
  const candidates = resolveViewPath(menuItem.component)
  const existing = candidates.filter((candidate) => fs.existsSync(candidate))
  assert.ok(
    existing.length > 0,
    `electronic_batch_record_view_missing component=${menuItem.component} candidates=${candidates.join(',')}`
  )
})

test('electronic batch record execution permissions allow the live page API', async () => {
  const accessToken = await login()
  const permissionInfo = await fetchPermissionInfo(accessToken)
  const permissions = new Set(Array.isArray(permissionInfo.permissions) ? permissionInfo.permissions : [])
  for (const permission of [
    'mes:pro-batch-record-execution:query',
    'mes:pro-batch-record-execution:create',
    'mes:pro-batch-record-execution:update'
  ]) {
    assert.ok(permissions.has(permission), `edhr_execution_permission_missing_${permission}`)
  }

  const response = await fetch(`${backendBase}/mes/pro/batch-record-execution/page?pageNo=1&pageSize=1`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'tenant-id': tenantId
    }
  })
  assert.equal(response.status, 200, `edhr_execution_page_http_status_expected_200_actual_${response.status}`)
  const payload = await response.json()
  assert.equal(
    payload.code,
    0,
    `edhr_execution_page_code_expected_0_actual_${payload.code}_msg_${payload.msg || payload.message || ''}`
  )
})
