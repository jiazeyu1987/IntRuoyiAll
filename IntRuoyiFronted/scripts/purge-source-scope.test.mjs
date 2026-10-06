import assert from 'node:assert/strict'
import fsPromises from 'node:fs/promises'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const args = process.argv.slice(2)
const option = (name) => {
  const index = args.indexOf(name)
  assert.ok(index >= 0 && args[index + 1], `Required option: ${name}`)
  return path.resolve(args[index + 1])
}
const configPath = option('--config')
const frontendRoot = option('--frontend-root')
const requireFromFrontend = createRequire(path.join(frontendRoot, 'package.json'))
const ts = requireFromFrontend('typescript')
const requireFromPlugin = createRequire(requireFromFrontend.resolve('vite-plugin-purge-icons'))
const coreEntry = requireFromPlugin.resolve('@purge-icons/core')
const { Extract } = requireFromPlugin('@purge-icons/core')
const fsExtra = createRequire(coreEntry)('fs-extra')
const source = await fsPromises.readFile(configPath, 'utf8')
const ast = ts.createSourceFile(configPath, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const calls = []
const visit = (node) => {
  if (ts.isCallExpression(node) && ts.isIdentifier(node.expression) && node.expression.text === 'PurgeIcons') calls.push(node)
  ts.forEachChild(node, visit)
}
visit(ast)
assert.equal(calls.length, 1, 'Exactly one real PurgeIcons configuration is required')
assert.equal(calls[0].arguments.length, 1, 'PurgeIcons must declare source-only content')
const optionsNode = calls[0].arguments[0]
assert.ok(ts.isObjectLiteralExpression(optionsNode), 'PurgeIcons options must be explicit')
assert.equal(optionsNode.properties.length, 1, 'Only content changes; icon fetch/generation semantics stay unchanged')
const contentNode = optionsNode.properties[0]
assert.ok(ts.isPropertyAssignment(contentNode) && contentNode.name.getText(ast) === 'content')
assert.ok(ts.isArrayLiteralExpression(contentNode.initializer))
const content = contentNode.initializer.elements.map((node) => {
  assert.ok(ts.isStringLiteral(node), 'Content patterns must be literal paths')
  return node.text
})

await test('real PurgeIcons Extract reads only configured source files and preserves dynamic source icons', async () => {
  const previousCwd = process.cwd()
  const taskDir = path.dirname(fileURLToPath(import.meta.url))
  const fixtureRoot = await fsPromises.mkdtemp(path.join(taskDir, '.purge-source-scope-r1-'))
  const fixture = {
    'index.html': '<span class="ep:home-filled"></span>',
    'src/pages/Current.vue': '<Icon :icon="editing ? \'ep:edit\' : \'ep:view\'" />',
    'src/menu.ts': "export const menu = { icon: 'ep:delete' }",
    'src/widget.tsx': "export const widget = <Icon icon={'ep:circle-check'} />",
    'src/widget.jsx': "export const widget = <Icon icon={'ep:plus'} />",
    'src/action.js': "export const icon = 'ep:check'",
    'src/help.html': '<i class="ep:question-filled"></i>',
    'src/nested/help.pug': "Icon(icon='ep:info-filled')",
    'dist/assets/Old.js': "const stale = 'ep:close'",
    'dist-test/assets/CategoryUploadSizePolicyDialog-DUxpnCmB.js': "const stale = 'ep:warning'",
    'dist-intruoyi-test/assets/Old.js': "const stale = 'ep:refresh'",
    'tests/stale.test.ts': "const stale = 'ep:star'",
    'test/stale.js': "const stale = 'ep:setting'",
    'doc/tasks/stale.ts': "const stale = 'ep:document'",
    'yudao-ui-admin-vue3/src/old.vue': '<Icon icon="ep:upload" />',
    'node_modules/package/index.js': "const dependency = 'ep:download'",
    'src/node_modules/package/index.js': "const dependency = 'ep:search'"
  }
  const originalRead = fsExtra.readFile
  const observedReads = []
  try {
    for (const [relative, text] of Object.entries(fixture)) {
      const file = path.join(fixtureRoot, relative)
      await fsPromises.mkdir(path.dirname(file), { recursive: true })
      await fsPromises.writeFile(file, text)
    }
    process.chdir(fixtureRoot)
    fsExtra.readFile = function (...readArgs) {
      const relative = path.relative(fixtureRoot, path.resolve(String(readArgs[0]))).replaceAll('\\', '/')
      observedReads.push(relative)
      return Reflect.apply(originalRead, this, readArgs)
    }
    const icons = await Extract({ content })
    assert.deepEqual(icons.sort(), ['ep:home-filled', 'ep:edit', 'ep:view', 'ep:delete', 'ep:circle-check', 'ep:plus', 'ep:check', 'ep:question-filled', 'ep:info-filled'].sort())
    assert.deepEqual(observedReads.sort(), ['index.html', 'src/pages/Current.vue', 'src/menu.ts', 'src/widget.tsx', 'src/widget.jsx', 'src/action.js', 'src/help.html', 'src/nested/help.pug'].sort(),
      'Generated builds, tests, docs, source copies and dependencies must never be read by the actual extractor')
    console.log(JSON.stringify({ configuredContent: content, realNativeReadFiles: observedReads.length, extractedSourceIcons: icons.length, generatedOrTestFilesRead: 0, networkCallsRequired: false }))
  } finally {
    fsExtra.readFile = originalRead
    process.chdir(previousCwd)
    const resolvedFixture = path.resolve(fixtureRoot)
    assert.ok(path.dirname(resolvedFixture) === path.resolve(taskDir) && path.basename(resolvedFixture).startsWith('.purge-source-scope-r1-'))
    await fsPromises.rm(resolvedFixture, { recursive: true, force: true })
  }
})
