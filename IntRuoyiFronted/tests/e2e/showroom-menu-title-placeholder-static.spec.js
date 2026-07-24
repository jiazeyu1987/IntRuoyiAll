const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routerHelper = readSource('src/utils/routerHelper.ts')
const menuTitleRenderer = readSource(
  'src/layout/components/Menu/src/components/useRenderMenuTitle.tsx'
)

assert.doesNotMatch(
  routerHelper,
  /data\.meta\s*=\s*\{\s*hidden:\s*meta\.hidden\s*\}/,
  'top-level single-page Layout shell must not drop the backend menu title'
)

assert.match(
  routerHelper,
  /data\.meta\s*=\s*\{[\s\S]*title:\s*meta\.title[\s\S]*icon:\s*meta\.icon[\s\S]*hidden:\s*meta\.hidden[\s\S]*\}/,
  'top-level single-page Layout shell must keep title and icon for sidebar rendering'
)

assert.doesNotMatch(
  menuTitleRenderer,
  /Please set title/,
  'menu title renderer must not expose the default English placeholder'
)

assert.match(
  menuTitleRenderer,
  /throw new Error\(`菜单标题缺失/,
  'menu title renderer must fail fast when a visible menu route has no title'
)
