const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const qs = require('qs')

for (const method of ['getRegistrationCertificatePage', 'getRegistrationCertificateOldIndexPage']) {
  test(`${method} binds typed filters as Spring Map parameters`, async () => {
    let captured
    const api = {}
    const source = ts.transpileModule(fs.readFileSync('src/api/dcc/registrationCertificate/index.ts', 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
    new Function('exports', 'require', source)(api, id => {
      if (id === '@/config/axios') return { default: { get: async config => { captured = config; return { total: 0, list: [] } } } }
      if (id === 'qs') return { default: qs }
      if (id === '@/utils') return { generateUUID: () => 'test-only' }
      throw new Error('Unexpected dependency: ' + id)
    })
    const params = { pageNo: 1, pageSize: 20, certificateType: 'FDA', fieldFilters: { kpNumber: 'E2E-K1030-NOT-EXIST', ownProduct: 'false', productNameEn: 'Product & Description' }, listingStart: '2026-10-02' }
    await api[method](params)
    const wire = new URLSearchParams(captured.paramsSerializer(captured.params))
    assert.equal(wire.get('fieldFilters[kpNumber]'), params.fieldFilters.kpNumber)
    assert.equal(wire.get('fieldFilters[ownProduct]'), 'false')
    assert.equal(wire.get('fieldFilters[productNameEn]'), 'Product & Description')
    assert.equal(wire.get('fieldFilters.kpNumber'), null)
    assert.equal(wire.get('listingStart'), '2026-10-02')
    assert.equal(wire.get('certificateType'), 'FDA')
    assert.equal(wire.get('pageNo'), '1')
    assert.equal(captured.url, method === 'getRegistrationCertificatePage' ? '/dcc/registration-certificates/page' : '/dcc/registration-certificates/old-index/page')
    assert.deepEqual(params.fieldFilters, { kpNumber: 'E2E-K1030-NOT-EXIST', ownProduct: 'false', productNameEn: 'Product & Description' })
  })
}
