const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('./load-secure-uuid.cjs')

module.exports = (crypto = webcrypto) => {
  const exports = {}
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/utils/permissionAudit.ts'), 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code, { exports, crypto, Uint8Array, Error, require: id => {
    if (id === './uuid') return loadUuid(crypto)
    throw new Error(`Unexpected permission audit dependency: ${id}`)
  } })
  return exports
}
