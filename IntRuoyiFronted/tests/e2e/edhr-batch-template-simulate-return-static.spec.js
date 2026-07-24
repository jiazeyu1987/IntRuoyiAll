const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const templatePage = read('src/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue')
const batchRecordPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const simulatePage = read('src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue')

const assertIncludes = (content, token, message) => {
  assert.ok(content.includes(token), message)
}

assertIncludes(
  templatePage,
  "returnTo: route.fullPath",
  'eDHR 模板说明页进入模拟填写时必须透传当前来源 fullPath'
)
assertIncludes(
  templatePage,
  "returnLabel: '返回模板说明'",
  'eDHR 模板说明页进入模拟填写时必须透传对应返回文案'
)

assertIncludes(
  batchRecordPage,
  "returnTo: route.fullPath",
  '电子批记录页进入模拟填写时必须透传当前来源 fullPath'
)
assertIncludes(
  batchRecordPage,
  "returnLabel: '返回批记录表单'",
  '电子批记录页进入模拟填写时必须透传对应返回文案'
)

assertIncludes(
  simulatePage,
  '<el-button link type="primary" @click="handleBack">',
  '模拟填写页左上角必须渲染返回按钮'
)
assertIncludes(simulatePage, '返回', '模拟填写页返回按钮文案必须包含 返回')
assertIncludes(
  simulatePage,
  "const returnTo = computed(() => String(route.query.returnTo || '').trim())",
  '模拟填写页必须读取来源路由 returnTo 查询参数'
)
assertIncludes(
  simulatePage,
  "const returnLabel = computed(() => String(route.query.returnLabel || '').trim())",
  '模拟填写页必须读取来源文案 returnLabel 查询参数'
)
assertIncludes(
  simulatePage,
  "const backButtonLabel = computed(() => returnLabel.value || '返回')",
  '模拟填写页必须根据来源文案生成返回按钮文案'
)
assertIncludes(
  simulatePage,
  'const router = useRouter()',
  '模拟填写页必须接入路由实例处理返回'
)
assertIncludes(
  simulatePage,
  'const handleBack = async () => {',
  '模拟填写页必须提供统一返回处理函数'
)
assertIncludes(
  simulatePage,
  'if (returnTo.value) {',
  '模拟填写页必须优先按来源路由返回'
)
assertIncludes(
  simulatePage,
  'await router.push(returnTo.value)',
  '模拟填写页必须跳回来源 fullPath'
)

console.log('PASS: eDHR batch template simulate return static contract')
