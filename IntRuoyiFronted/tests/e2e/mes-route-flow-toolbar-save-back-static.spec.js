const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const graphComponent = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const assertOrder = (content, left, right, label) => {
  const leftIndex = content.indexOf(left)
  const rightIndex = content.indexOf(right)
  if (leftIndex === -1 || rightIndex === -1 || leftIndex >= rightIndex) {
    throw new Error(`${label} order invalid`)
  }
}

assertIncludes(
  graphComponent,
  'data-flow-action="back-route-list"',
  'toolbar back button stable selector'
)
assertIncludes(graphComponent, '@click="handleRequestBack"', 'toolbar back button delegates controlled back handler')
assertIncludes(graphComponent, '返回', 'toolbar back copy')
assertIncludes(graphComponent, 'data-flow-action="save-route-flow"', 'toolbar save stable selector')
assertIncludes(graphComponent, '@click="handleRequestSubmit"', 'toolbar save delegates controlled submit handler')
assertIncludes(graphComponent, "emit('request-submit')", 'toolbar save handler keeps submit emit')
assertIncludes(graphComponent, "'request-back': []", 'graph component emits back contract')
assertIncludes(formContent, '@request-back="handleFlowGraphBackRequest"', 'route form handles graph back with draft guard')
assertIncludes(
  graphComponent,
  'data-flow-action="toggle-route-flow-maximize"',
  'toolbar maximize stable selector'
)
assertNotIncludes(
  graphComponent,
  'data-flow-action="generate-linear-draft"',
  'toolbar auto generation selector removed'
)

const toolbarTemplate = graphComponent.slice(
  graphComponent.indexOf('<div class="route-flow-graph-designer__toolbar">'),
  graphComponent.indexOf('<div v-loading="loading"', graphComponent.indexOf('<div class="route-flow-graph-designer__toolbar">'))
)
const normalizedToolbarTemplate = toolbarTemplate.replace(/\r\n/g, '\n')

assertOrder(
  toolbarTemplate,
  'data-flow-action="back-route-list"',
  'class="route-flow-graph-designer__route-name"',
  'back button must stay in original save position before route name'
)
assertOrder(
  toolbarTemplate,
  '自动布局',
  'data-flow-action="save-route-flow"',
  'save button must stay to the right of flow graph actions'
)
assertOrder(
  normalizedToolbarTemplate,
  'data-flow-action="save-route-flow"',
  '</div>\n    </div>',
  'save button must remain inside toolbar actions'
)

console.log('mes-route-flow-toolbar-save-back-static PASS')
