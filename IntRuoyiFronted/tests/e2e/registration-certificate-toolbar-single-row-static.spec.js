import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const frontendRoot = process.env.FRONTEND_ROOT || process.cwd()
const pagePath = path.resolve(
  frontendRoot,
  'src/views/dcc/registration-certificate/index/index.vue'
)
const pageSource = fs.readFileSync(pagePath, 'utf8')

const currentListStart = pageSource.indexOf('<UnifiedListTemplate')
const currentListEnd = pageSource.indexOf('</UnifiedListTemplate>', currentListStart)
assert.ok(currentListStart >= 0 && currentListEnd > currentListStart, '注册证当前列表必须使用统一列表模板。')

const currentListSource = pageSource.slice(currentListStart, currentListEnd)
assert.ok(
  currentListSource.includes('query-form-test-id="registration-certificate-current-filter-form"'),
  '当前列表静态合同必须定位到注册证当前列表。'
)

assert.match(
  currentListSource,
  /class="registration-certificate-current-list"/,
  '注册证当前列表必须有页面级样式类，避免影响老证列表。'
)
assert.match(
  currentListSource,
  /\bsingle-line-toolbar\b/,
  '注册证当前列表顶部筛选条件、上传注册证和显示字段控件必须启用单行工具栏。'
)

const actionsStart = currentListSource.indexOf('<template #actions>')
const actionsEnd = currentListSource.indexOf('</template>', actionsStart)
assert.ok(actionsStart >= 0 && actionsEnd > actionsStart, '注册证当前列表必须保留工具栏操作插槽。')
const actionsSource = currentListSource.slice(actionsStart, actionsEnd)
assert.match(actionsSource, /上传注册证/, '单行工具栏必须保留上传注册证按钮。')

const styleSource = pageSource.match(/<style scoped>[\s\S]*<\/style>/)?.[0] || ''
assert.match(
  styleSource,
  /\.registration-certificate-current-list\.unified-list-template--single-line-toolbar[\s\S]*:deep\(\.unified-list-template__query-form\)[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto;/,
  '注册证当前列表单行工具栏必须把筛选区和右侧按钮组放入同一行。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-list\.unified-list-template--single-line-toolbar[\s\S]*:deep\(\.unified-list-template__multi-filter\)[\s\S]*min-width:\s*0;/,
  '注册证当前列表筛选主列必须允许收缩，避免按钮换行。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-list\.unified-list-template--single-line-toolbar[\s\S]*:deep\(\.table-multi-filter\)[\s\S]*\.registration-certificate-current-list\.unified-list-template--single-line-toolbar[\s\S]*:deep\(\.table-multi-filter__tabs-empty\)[\s\S]*min-width:\s*0;/,
  '注册证当前列表空筛选条和筛选容器都必须允许收缩。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-list\.unified-list-template--single-line-toolbar[\s\S]*:deep\(\.unified-list-template__toolbar\)[\s\S]*white-space:\s*nowrap;/,
  '注册证当前列表右侧上传和显示字段按钮不得在宽屏工具栏内换行。'
)

console.log('PASS: registration certificate toolbar single-row static contract')
