const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')

module.exports = (crypto = webcrypto) => {
  const exports = {}
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/utils/uuid.ts'), 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code, { exports, crypto, Uint8Array, Error })
  return exports
}
