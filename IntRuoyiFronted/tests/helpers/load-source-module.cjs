const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

// Execute real TypeScript helpers under Node; callers supply every external dependency explicitly.
module.exports = (file, dependencies = {}) => {
  const exports = {}
  const source = fs.readFileSync(path.resolve(__dirname, '../..', file), 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const resolve = id => {
    if (Object.hasOwn(dependencies, id)) return dependencies[id]
    throw new Error(`Unmapped source module dependency: ${id}`)
  }
  new Function('exports', 'require', code)(exports, resolve)
  return exports
}
