const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(read('package.json'))
const component = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const extractBlock = (selector) => {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = component.match(new RegExp(`${escapedSelector} \\{([\\s\\S]*?)\\n\\}`))
  assert.ok(match, `${selector} style block should exist`)
  return match[1]
}

test('route flow process node text stays vertically centered', () => {
  assert.equal(
    packageJson.scripts['e2e:mes:route-flow-node-text-center:static'],
    'node tests/e2e/mes-route-flow-node-text-center-static.spec.js',
    'package.json should expose the node text alignment static check'
  )

  const processTemplate = component.match(
    /<template #node-route-process="\{ data \}">([\s\S]*?)<\/template>/
  )
  assert.ok(processTemplate, 'route process node template should exist')

  assert.match(
    processTemplate[1],
    /'has-flags': data\.routeNode\.keyFlag \|\| data\.routeNode\.checkFlag/,
    'process node should only reserve the flag row when a tag is actually visible'
  )
  assert.match(
    processTemplate[1],
    /<span\s+v-if="data\.routeNode\.keyFlag \|\| data\.routeNode\.checkFlag"\s+class="route-flow-graph-designer__node-flags"/,
    'empty flag container should not keep title text pushed upward'
  )

  const nodeBlock = extractBlock('.route-flow-graph-designer__node')
  assert.match(nodeBlock, /grid-template-rows:\s*auto;/, 'default node grid should have one centered row')
  assert.match(nodeBlock, /align-content:\s*center;/, 'node content group should be vertically centered')
  assert.doesNotMatch(
    nodeBlock,
    /grid-template-rows:\s*1fr\s+auto;/,
    'default node grid must not keep an empty second row'
  )

  const hasFlagsBlock = extractBlock('.route-flow-graph-designer__node.has-flags')
  assert.match(
    hasFlagsBlock,
    /grid-template-rows:\s*auto\s+auto;/,
    'flagged nodes should center the name and visible tags as a compact group'
  )

  const sortBlock = extractBlock('.route-flow-graph-designer__node-sort')
  assert.match(sortBlock, /grid-row:\s*1;/, 'sort badge should not create an implicit second row')

  const flaggedSortBlock = extractBlock('.route-flow-graph-designer__node.has-flags .route-flow-graph-designer__node-sort')
  assert.match(flaggedSortBlock, /grid-row:\s*1\s*\/\s*3;/, 'sort badge spans the visible name and tag rows only')

  const flagsBlock = extractBlock('.route-flow-graph-designer__node-flags')
  assert.doesNotMatch(flagsBlock, /min-height:\s*22px;/, 'empty flags must not reserve vertical space')
  assert.match(flagsBlock, /align-items:\s*center;/, 'visible tags should align cleanly inside the centered group')

  const boundaryBlock = extractBlock('.route-flow-graph-designer__boundary-node')
  assert.match(boundaryBlock, /align-items:\s*center;/, 'boundary node text should remain vertically centered')
  assert.match(boundaryBlock, /justify-content:\s*center;/, 'boundary node text should remain horizontally centered')
})
