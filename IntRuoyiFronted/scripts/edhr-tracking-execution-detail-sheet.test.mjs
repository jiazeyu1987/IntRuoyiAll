import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const extractTrackingParamsBlock = (source) => {
  const match = source.match(/<aside class="edhr-page-shell__tracking-params">([\s\S]*?)<\/aside>/)
  assert.ok(match, '执行详情必须存在追踪只读参数栏')
  return match[1]
}

test('BDD: 追踪列表查看进入只读分屏详情', () => {
  const trackingPage = readText('src/views/mes/pro/edhr/TrackingPage.vue')
  const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(
    trackingPage,
    /viewMode:\s*'tracking'/,
    '追踪列表查看执行详情时必须携带 viewMode=tracking'
  )
  assert.match(
    executionPage,
    /isTrackingReadonlyMode/,
    '执行详情必须识别追踪入口只读模式'
  )
  assert.match(
    executionPage,
    /edhr-page-shell__tracking-detail/,
    '追踪模式必须渲染左参数右表单的分屏容器'
  )
  assert.match(
    executionPage,
    /edhr-page-shell__tracking-params/,
    '追踪模式左侧必须提供业务和审计参数区'
  )
  assert.match(
    executionPage,
    /EdhrExecutionReadonlyForm/,
    '追踪模式右侧必须复用真实模板表格组件'
  )
  assert.match(
    executionPage,
    /trackingReadonlyFormViewModel/,
    '执行详情必须从当前执行详情组装只读表单 view model'
  )
  assert.match(
    executionPage,
    /cellValuesJson:\s*JSON\.stringify\(execution\.value\.cellValues/,
    '只读表单必须消费后端真实 cellValues'
  )
  assert.match(
    executionPage,
    /:signature-records="signatureRows"/,
    '只读表单必须消费真实签名记录以渲染签名单元格'
  )
})

test('BDD: 追踪只读详情不展示模拟字段表单或 JSON renderer', () => {
  const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')

  assert.match(
    executionPage,
    /v-if="isTrackingReadonlyMode"/,
    '追踪只读详情主体必须由 isTrackingReadonlyMode 显式控制'
  )
  assert.match(
    executionPage,
    /v-else[\s\S]{0,240}<el-descriptions :column="2" border class="edhr-page-shell__summary"/,
    '普通详情主体必须保留在追踪只读模式之外'
  )
  assert.match(
    executionPage,
    /v-if="!isTrackingReadonlyMode[^"]*"[\s\S]{0,420}保存变更/,
    '追踪只读模式不得显示字段保存按钮'
  )
  assert.match(
    executionPage,
    /v-if="!isTrackingReadonlyMode[^"]*"[\s\S]{0,420}提交执行/,
    '追踪只读模式不得显示提交按钮'
  )
  assert.match(
    executionPage,
    /<ExecutionRenderer\s+v-if="!isTrackingReadonlyMode"/,
    'JSON 快照 renderer 只能保留给非追踪入口'
  )
  assert.match(
    executionPage,
    /<el-tabs\s+v-if="!isTrackingReadonlyMode"\s+class="edhr-page-shell__audit-tabs"/,
    '旧追踪和签名 tab 只能保留给非追踪入口，追踪入口主体必须是左参右表'
  )
})

test('BDD: 追踪只读详情缺少真实模板时失败暴露', () => {
  const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')
  const readonlyForm = readText('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')
  const apiSource = readText('src/api/mes/pro/feedback/index.ts')

  assert.match(
    apiSource,
    /sheetLayoutJson\?:\s*string/,
    '执行详情类型必须声明后端返回的 sheetLayoutJson'
  )
  assert.match(
    executionPage,
    /trackingReadonlyFormRenderError/,
    '追踪只读详情必须在真实模板缺失时显示明确错误'
  )
  assert.match(
    executionPage,
    /缺少电子批记录模板布局，无法按原模板展示填写结果/,
    '追踪只读详情缺少 sheetLayoutJson 时必须 fail fast'
  )
  assert.match(
    executionPage,
    /缺少电子批记录单元格填写值，无法按原模板展示填写结果/,
    '追踪只读详情缺少 cellValues 时必须 fail fast'
  )
  assert.doesNotMatch(
    executionPage,
    /trackingReadonlyFormRenderError[\s\S]{0,600}ExecutionRenderer/,
    '追踪只读详情模板缺失时不得降级到 JSON renderer'
  )
  assert.match(
    readonlyForm,
    /overflow-x:\s*auto/,
    '真实表格组件需要横向滚动以避免右侧分栏裁切'
  )
  assert.match(
    readonlyForm,
    /min-width:\s*960px/,
    '真实表格组件需要稳定最小宽度以接近原始表单比例'
  )
})

test('BDD: 追踪执行参数按截图规则隐藏字段、格式化日期并允许换行', () => {
  const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')
  const paramsBlock = extractTrackingParamsBlock(executionPage)

  for (const hiddenLabel of ['cellValuesHash', 'fieldAuditRevision', 'fieldAuditHeadHash', '最后事件']) {
    assert.doesNotMatch(
      paramsBlock,
      new RegExp(`label="${hiddenLabel}"`),
      `追踪执行参数栏不应展示 ${hiddenLabel}`
    )
  }

  assert.match(
    paramsBlock,
    /label="提交时间"[\s\S]{0,160}formatTrackingReadonlyDate\(execution\.submittedAt\)/,
    '提交时间必须使用年月日格式化函数展示'
  )
  assert.match(
    paramsBlock,
    /label="审批时间"[\s\S]{0,160}formatTrackingReadonlyDate\(execution\.approvedAt\)/,
    '审批时间必须使用年月日格式化函数展示'
  )
  assert.match(
    paramsBlock,
    /label="关闭时间"[\s\S]{0,160}formatTrackingReadonlyDate\(execution\.closedAt\)/,
    '关闭时间必须使用年月日格式化函数展示'
  )
  assert.match(
    executionPage,
    /const formatTrackingReadonlyDate[\s\S]*formatDate\([^,]+,\s*'YYYY年M月D日'\)/,
    '追踪参数日期 formatter 必须输出年月日格式'
  )
  assert.match(
    executionPage,
    /\.edhr-page-shell__tracking-param-list\s*:deep\(\.el-descriptions__content\)[\s\S]*white-space:\s*normal[\s\S]*overflow-wrap:\s*anywhere/s,
    '追踪参数内容单元格必须允许长文本换行'
  )
  assert.doesNotMatch(
    executionPage,
    /edhr-page-shell__tracking-hash/,
    '追踪参数不应保留 hash 单行省略样式'
  )
})
