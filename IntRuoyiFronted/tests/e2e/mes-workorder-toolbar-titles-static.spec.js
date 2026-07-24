const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = process.cwd()
const workOrderPagePath = path.resolve(root, 'src/views/mes/pro/workorder/index.vue')
const unifiedListTemplatePath = path.resolve(root, 'src/components/UnifiedListTemplate/index.vue')
const columnSettingsPath = path.resolve(root, 'src/components/UserTableColumnSettings/index.vue')
const quickFilterPath = path.resolve(root, 'src/components/TableQuickFilter/index.vue')

const workOrderSource = fs.readFileSync(workOrderPagePath, 'utf8')
const unifiedListTemplateSource = fs.readFileSync(unifiedListTemplatePath, 'utf8')
const columnSettingsSource = fs.readFileSync(columnSettingsPath, 'utf8')
const quickFilterSource = fs.readFileSync(quickFilterPath, 'utf8')

assert.doesNotMatch(
  workOrderSource,
  /:show-quick-filter-label="false"/,
  '生产工单列表不能隐藏快速过滤标题。'
)

assert.match(
  workOrderSource,
  /<UserTableColumnSettings[\s\S]*button-label="列筛选"[\s\S]*:columns="workOrderColumns"[\s\S]*:saving="workOrderColumnSaving"[\s\S]*:show-reset="false"[\s\S]*@change="saveWorkOrderColumnConfig"/,
  '生产工单列表的列设置入口必须显示为列筛选，并继续绑定原列配置保存逻辑。'
)

assert.match(
  workOrderSource,
  /<UnifiedListTemplate[\s\S]*@quick-filter-query="workOrderQuickFilter\.applyQuickFilter"/,
  '生产工单列表必须保留快速过滤查询事件。'
)

assert.match(
  unifiedListTemplateSource,
  /withDefaults\(defineProps<[\s\S]*showQueryForm:\s*true[\s\S]*showQuickFilterLabel:\s*true[\s\S]*showColumnSettings:\s*true[\s\S]*showColumnReset:\s*false/,
  'UnifiedListTemplate 必须默认开启查询和显示字段，但默认隐藏重置列。'
)

assert.match(
  quickFilterSource,
  /withDefaults\(defineProps<[\s\S]*showLabel:\s*true/,
  'TableQuickFilter 的快速过滤标题必须显式默认显示。'
)

assert.match(quickFilterSource, />\s*查询\s*</, '快速过滤组件必须渲染查询文案。')
assert.match(workOrderSource, />\s*重置\s*</, '生产工单列表工具区必须渲染重置文案。')

assert.match(
  columnSettingsSource,
  /buttonLabel\?:\s*string/,
  'UserTableColumnSettings 必须支持按钮文案定制。'
)

assert.match(
  columnSettingsSource,
  /\{\{\s*buttonLabel\s*\}\}/,
  'UserTableColumnSettings 入口按钮必须渲染可配置文案。'
)

assert.match(
  columnSettingsSource,
  /const buttonLabel = computed\(\(\) => props\.buttonLabel \|\| '显示字段'\)/,
  'UserTableColumnSettings 默认按钮文案必须保持显示字段，避免影响其它列表。'
)

assert.match(
  columnSettingsSource,
  /withDefaults\(defineProps<[\s\S]*showReset:\s*true[\s\S]*buttonLabel:\s*'显示字段'/,
  'UserTableColumnSettings 的重置按钮和入口文案必须显式声明默认值，避免可选 Boolean prop 运行态默认 false。'
)

console.log('PASS mes-workorder-toolbar-titles-static')
