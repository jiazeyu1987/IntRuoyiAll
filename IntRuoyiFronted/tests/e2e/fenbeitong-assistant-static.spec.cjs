const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const viewSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/erp/finance/fenbeitong-voucher/index.vue'),
  'utf8'
)
const loginApiSource = fs.readFileSync(path.join(frontendRoot, 'src/api/login/index.ts'), 'utf8')
const envTypes = fs.readFileSync(path.join(frontendRoot, 'types/env.d.ts'), 'utf8')
const testEnv = fs.readFileSync(path.join(frontendRoot, '.env.test'), 'utf8')
const prodEnv = fs.readFileSync(path.join(frontendRoot, '.env.prod'), 'utf8')

assert.match(viewSource, /defineOptions\(\{\s*name:\s*'ErpFenbeitongVoucher'\s*\}\)/)
assert.match(viewSource, /VITE_FENBEITONG_ASSISTANT_URL/)
assert.match(viewSource, /createFenbeitongAssistantTicket/)
assert.match(viewSource, /getFenbeitongAssistantStatus/)
assert.match(viewSource, /startFenbeitongAssistant/)
assert.match(viewSource, /buildAssistantAccessUrl/)
assert.match(viewSource, /auth\/callback/)
assert.match(viewSource, /<iframe[\s\S]*:src="assistantAccessUrl"/)
assert.match(viewSource, /启动助手/)
assert.doesNotMatch(viewSource, /FenbeitongVoucherApi/)
assert.doesNotMatch(viewSource, /fenbeitongAccessToken/)

assert.match(loginApiSource, /createFenbeitongAssistantTicket/)
assert.match(loginApiSource, /\/system\/auth\/fenbeitong-assistant-ticket/)
assert.match(loginApiSource, /getFenbeitongAssistantStatus/)
assert.match(loginApiSource, /\/system\/auth\/fenbeitong-assistant\/status/)
assert.match(loginApiSource, /startFenbeitongAssistant/)
assert.match(loginApiSource, /\/system\/auth\/fenbeitong-assistant\/start/)

assert.match(envTypes, /readonly VITE_FENBEITONG_ASSISTANT_URL: string/)
assert.match(testEnv, /^VITE_FENBEITONG_ASSISTANT_URL=http:\/\/\{hostname\}:18734\/$/m)
assert.match(prodEnv, /^VITE_FENBEITONG_ASSISTANT_URL=http:\/\/\{hostname\}:18734\/$/m)
const resolveBody = viewSource.match(/const assistantBaseUrl = computed\(\(\) => \{([\s\S]*?)\n\}\)/)[1]
const resolveUrl = new Function('configuredValue', 'window', resolveBody.replace('import.meta.env.VITE_FENBEITONG_ASSISTANT_URL', 'configuredValue'))
for (const host of ['172.30.30.58', '172.30.30.57', '172.30.30.59']) {
  assert.equal(resolveUrl('http://{hostname}:18734/', { location: { hostname: host } }), `http://${host}:18734/`)
}
assert.equal(resolveUrl('', { location: { hostname: '172.30.30.57' } }), '')
assert.equal(resolveUrl('http://127.0.0.1:18734/', { location: { hostname: 'localhost' } }), 'http://127.0.0.1:18734/')

console.log('Fenbeitong assistant frontend static contract passed')
