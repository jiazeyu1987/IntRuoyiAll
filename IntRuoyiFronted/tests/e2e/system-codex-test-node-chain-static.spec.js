const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/system/codexTestManagement/index.ts')
const page = read('src/views/system/codex-test-management/index.vue')

assert.match(
  api,
  /interface CodexTestCaseVO[\s\S]*nodeChainName\??:\s*string[\s\S]*nodeChainSort\??:\s*number/,
  '测试项类型必须暴露节点串名称和串内序号。'
)
assert.match(
  api,
  /interface CodexTestCasePageReqVO[\s\S]*nodeChainName\??:\s*string/,
  '测试项分页查询必须支持节点串筛选。'
)
assert.match(
  api,
  /interface CodexTestNodeChainOptionVO[\s\S]*name:\s*string[\s\S]*project:\s*CodexTestProject[\s\S]*nodeCount:\s*number/,
  '节点串选项必须包含名称、项目和节点数量。'
)
assert.match(api, /\/system\/codex-test-case\/node-chain-options/)
assert.match(page, /getCodexTestNodeChainOptions/)
assert.match(
  page,
  /label:\s*'节点串'[\s\S]*queryParamKey:\s*'nodeChainName'[\s\S]*nodeChainFilterOptions/,
  '测试管理页必须支持按节点串快速筛选。'
)
assert.match(
  page,
  /<el-form-item\s+class="codex-test-node-chain-filter"\s+label="串行路线">[\s\S]*v-model="queryParams\.nodeChainName"[\s\S]*aria-label="串行路线"[\s\S]*class="!w-220px"[\s\S]*placeholder="全部串行路线"[\s\S]*@change="handleNodeChainFilterChange"[\s\S]*nodeChainFilterOptions/,
  '测试管理页必须在测试租户右侧、执行按钮左侧提供带可见标签的串行路线下拉。'
)
assert.match(
  page,
  /async function handleNodeChainFilterChange\(\)[\s\S]*queryParams\.pageNo = 1[\s\S]*await getCaseList\(\)/,
  '切换串行路线后必须回到第一页并刷新测试项列表。'
)
assert.match(page, /\{\s*key:\s*'nodeChain',\s*label:\s*'节点串'/)
assert.match(
  page,
  /<el-table-column[\s\S]*label="节点串"[\s\S]*prop="nodeChainName"[\s\S]*第 \{\{ row\.nodeChainSort \}\} 节点/,
  '测试管理表格必须展示节点串名称和串内序号。'
)
assert.match(
  page,
  /<el-form-item label="节点串" prop="nodeChainName">[\s\S]*allow-create[\s\S]*filterable/,
  '测试项表单必须允许选择或创建节点串。'
)
assert.match(
  page,
  /<el-form-item label="串内序号" prop="nodeChainSort">[\s\S]*v-model="caseForm\.nodeChainSort"/,
  '测试项表单必须维护串内序号。'
)
assert.match(page, /function enforceNodeChainExecutionControl/)
assert.match(
  page,
  /caseForm\.defaultExecutionMode = 'SEQUENTIAL'[\s\S]*caseForm\.parallelSafe = false/,
  '节点串测试项必须锁定为顺序执行且不允许并行。'
)
assert.match(
  page,
  /:disabled="Boolean\(caseForm\.nodeChainName\)"/,
  '节点串配置存在时必须禁用冲突执行控件。'
)

console.log('PASS: Codex test node chain static contract')
