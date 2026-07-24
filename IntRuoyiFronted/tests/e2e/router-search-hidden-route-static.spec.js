const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(repoRoot, 'src/components/RouterSearch/index.vue'),
  'utf8'
)

assert.match(
  component,
  /function\s+isSearchableRoute/,
  'RouterSearch must centralize the visible-menu route gate.'
)
assert.match(
  component,
  /!route\.meta\?\.hidden/,
  'Top menu search candidates must exclude hidden edit/detail routes.'
)
assert.match(
  component,
  /function\s+resolveSearchableHistoryRecord/,
  'Saved search history must be normalized before display.'
)
assert.match(
  component,
  /activeMenu/,
  'Hidden history records with an activeMenu must be canonicalized to the visible menu entry.'
)
assert.match(
  component,
  /function\s+routePathMatches/,
  'History normalization must match dynamic route patterns such as /edit/:id.'
)
assert.match(
  component,
  /const\s+resolvedPath\s*=\s*resolveSearchablePath\(path\)/,
  'Click handling must resolve the selected path before routing.'
)
assert.match(
  component,
  /router\.push\(\{\s*path:\s*resolvedPath\s*\}\)/,
  'RouterSearch must navigate to the normalized visible menu path.'
)

console.log('router search hidden route static contract passed')
