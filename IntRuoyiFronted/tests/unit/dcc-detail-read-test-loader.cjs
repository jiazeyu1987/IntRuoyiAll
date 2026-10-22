const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const root = path.resolve(__dirname, '../..')
function load(file, dependencies = {}) {
  const c = { exports: {}, TextEncoder, TextDecoder, btoa, atob,
    require: id => { if (!(id in dependencies)) throw Error(id); return dependencies[id] } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return c.exports
}
const body = load('src/api/dcc/controlledFile/bodyReadContext.ts')
module.exports = { body, ...load('src/views/dcc/controlled-file/detail/detail-read-context.ts', {
  '@/api/dcc/controlledFile/bodyReadContext': body
}) }
