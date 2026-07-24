const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const unifiedTemplatePath = path.join(root, 'src/components/UnifiedListTemplate/index.vue')
const quickFilterPath = path.join(root, 'src/components/TableQuickFilter/index.vue')

const unifiedTemplateSource = fs.readFileSync(unifiedTemplatePath, 'utf8')
const quickFilterSource = fs.readFileSync(quickFilterPath, 'utf8')

assert.match(
  unifiedTemplateSource,
  /import \{ nextTick \} from 'vue'/,
  'UnifiedListTemplate must defer auto search until parent state has updated.'
)
assert.match(
  unifiedTemplateSource,
  /@update:state="handleQuickFilterStateUpdate"/,
  'UnifiedListTemplate must own quick-filter state updates so it can scope select auto search.'
)
assert.match(
  unifiedTemplateSource,
  /const emit = defineEmits/,
  'UnifiedListTemplate must keep an emit handle for ordered update/query emission.'
)
assert.match(
  unifiedTemplateSource,
  /const isQuickFilterSelectValueChange = \(state: QuickFilterState\) =>[\s\S]*props\.selectedFilterDefinition\?\.type === 'select'[\s\S]*'value' in state[\s\S]*state\.fieldKey === props\.quickFilterState\.fieldKey[\s\S]*state\.operator === props\.quickFilterState\.operator[\s\S]*state\.value !== props\.quickFilterState\.value/,
  'UnifiedListTemplate must auto search only for right-side select value changes.'
)
assert.match(
  unifiedTemplateSource,
  /const handleQuickFilterStateUpdate = async \(state: QuickFilterState\) => \{[\s\S]*const shouldAutoSearch = isQuickFilterSelectValueChange\(state\)[\s\S]*emit\('update:quickFilterState', state\)[\s\S]*if \(shouldAutoSearch\) \{[\s\S]*await nextTick\(\)[\s\S]*emit\('quick-filter-query'\)[\s\S]*\}/,
  'UnifiedListTemplate must emit latest state before triggering the auto search query.'
)
assert.match(
  quickFilterSource,
  /v-else-if="selectedDefinition\?\.type === 'select'"[\s\S]*@update:model-value="updateValue"/,
  'TableQuickFilter select value changes must stay as generic state updates.'
)
assert.doesNotMatch(
  quickFilterSource,
  /updateSelectValue/,
  'TableQuickFilter must not reintroduce component-wide select auto search.'
)

console.log('PASS: unified list template select auto-search static contract')
