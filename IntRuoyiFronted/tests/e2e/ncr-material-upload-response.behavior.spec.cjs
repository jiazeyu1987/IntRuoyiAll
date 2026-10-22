const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/api/mes/pro/edhr/nonconformanceReview.ts'), 'utf8')
const ast = ts.createSourceFile('api.ts', source, ts.ScriptTarget.Latest, true)
const body = ast.statements.filter(node => !ts.isImportDeclaration(node))
  .map(node => node.getText(ast).replace(/^export /, '')).join('\n')
const js = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const createApi = upload => Function('request', `${js}; return uploadNonconformanceReviewMaterial`)({ upload })

test('NCR upload returns the formal file from the upload response envelope', async () => {
  const material = { fileId: '9001', url: '/files/review.pdf', fileName: 'review.pdf' }
  const file = new File(['review evidence'], 'review.pdf', { type: 'application/pdf' })
  let calls = 0
  const upload = createApi(async options => {
    calls++
    assert.equal(options.url, '/mes/pro/edhr-nonconformance-review/70/materials/upload')
    assert.equal(options.data.get('file').name, 'review.pdf')
    return { code: 0, data: material, msg: '' }
  })
  assert.equal(await upload('70', file), material)
  assert.equal(calls, 1)
})

test('NCR upload propagates rejection without a synthetic file or retry', async () => {
  const failure = new Error('upload rejected')
  let calls = 0
  const upload = createApi(async () => { calls++; throw failure })
  await assert.rejects(upload('70', new File(['x'], 'review.pdf')), error => error === failure)
  assert.equal(calls, 1)
})
