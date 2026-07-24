const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const routePage = readSource('src/views/dcc/controlled-file/routes/index.vue')
const routeForm = readSource('src/views/dcc/controlled-file/routes/components/RouteForm.vue')
const routeApi = readSource('src/api/dcc/controlledFile/approvalRoutes.ts')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.equal(
  packageJson.scripts['e2e:dcc:route-operations:static'],
  'node tests/e2e/dcc-route-operations-static.spec.js',
  'package.json 必须提供流程路线操作面板静态契约脚本'
)

assert.match(
  routeApi,
  /export const deleteApprovalRoute = async \(routeId: number\)[\s\S]*request\.delete\(\{ url: `\/dcc\/approval-routes\/\$\{routeId\}` \}\)/,
  '流程路线前端 API 必须提供删除当前路线版本接口'
)

assert.match(
  routePage,
  /import RouteForm from '.\/components\/RouteForm\.vue'/,
  '流程路线页面必须复用现有路线表单组件'
)
assert.match(routePage, /const routeFormRef = ref/, '流程路线页面必须持有路线表单引用')
assert.match(routePage, /<RouteForm ref="routeFormRef" @success="handleRouteFormSuccess" \/>/, '流程路线页面必须挂载路线表单并刷新成功结果')

const actions = extractBetween(routePage, '<template #actions>', '</template>')
assert.match(actions, /handleQuery/, '流程路线工具栏必须保留查询路线操作')
assert.match(actions, /handleCreateRoute/, '流程路线工具栏必须提供新增路线操作')
assert.match(actions, />\s*新增路线\s*</, '流程路线工具栏必须显示“新增路线”按钮')

const routeTable = extractBetween(
  routePage,
  'data-user-table-key="dcc.controlledFile.routes.main"',
  '</el-table>'
)
assert.match(routeTable, /label="操作"/, '流程路线主表必须增加操作列')
assert.match(routeTable, /fixed="right"/, '流程路线操作列必须固定在右侧')
assert.match(routeTable, /handleEditRoute\(row\)/, '流程路线操作列必须支持修改当前行')
assert.match(routeTable, /handleDeleteRoute\(row\)/, '流程路线操作列必须支持删除当前行')
assert.match(routeTable, />\s*修改\s*</, '流程路线操作列必须显示“修改”')
assert.match(routeTable, />\s*删除\s*</, '流程路线操作列必须显示“删除”')

assert.match(
  routePage,
  /const handleCreateRoute = async \(\) => \{[\s\S]*routeFormRef\.value\?\.open\(\{[\s\S]*categories: categoryOptions\.value[\s\S]*users: users\.value[\s\S]*positions: positions\.value/,
  '新增路线必须打开表单并传入可选文件类别、用户和岗位'
)
assert.match(
  routePage,
  /const handleEditRoute = async \(row: ControlledFileApprovalRouteVO\) => \{[\s\S]*routeFormRef\.value\?\.open\(\{[\s\S]*category:[\s\S]*route: row[\s\S]*users: users\.value[\s\S]*positions: positions\.value/,
  '修改路线必须打开表单并传入当前行路线版本'
)
assert.match(
  routePage,
  /const handleDeleteRoute = async \(row: ControlledFileApprovalRouteVO\) => \{[\s\S]*await message\.delConfirm[\s\S]*await deleteApprovalRoute\(row\.id\)[\s\S]*await handleQuery\(false\)/,
  '删除路线必须确认后删除当前行 id 并刷新列表'
)
assert.doesNotMatch(
  routePage,
  /catch\s*\{\s*\}/,
  '流程路线新增、修改、删除不得使用空 catch 吞异常'
)

assert.match(routeForm, /<el-form-item label="文件类别" prop="categoryId">/, '路线表单必须支持选择文件类别')
assert.match(routeForm, /v-if="categorySelectable"/, '新增路线时文件类别必须可选')
assert.match(routeForm, /v-else[\s\S]*currentCategory\?\.name/, '编辑路线时文件类别必须固定展示')
assert.match(routeForm, /categoryId: undefined/, '路线表单模型必须包含 categoryId')
assert.match(routeForm, /categories\?: Array<ControlledFileCategoryVO & \{ id: number \}>/, '路线表单 open 入参必须支持候选文件类别')
assert.match(routeForm, /const categorySelectable = computed/, '路线表单必须根据打开模式控制文件类别选择')
assert.match(routeForm, /if \(!formData\.value\.categoryId\)[\s\S]*message\.warning\('请选择文件类别'\)/, '保存前必须显式校验文件类别')
assert.match(routeForm, /await saveApprovalRoute\(formData\.value\.categoryId/, '路线表单保存必须使用选择后的类别 id')

for (const forbidden of [/mock/i, /placeholder data/i, /fallback/i, /降级/, /吞异常/, /默认成功/]) {
  assert.doesNotMatch(routePage, forbidden, '路线操作页面不得引入 mock、fallback、降级、吞异常或默认成功')
  assert.doesNotMatch(routeApi, forbidden, '路线操作 API 不得引入 mock、fallback、降级、吞异常或默认成功')
}

console.log('PASS: DCC route operations static contract')
