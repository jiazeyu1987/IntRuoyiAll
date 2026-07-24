const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')

assert(fs.existsSync(pagePath), '工艺路线用途配置组件必须存在。')
assert(
  !fs.existsSync(path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteUseConfigDialog.vue')),
  '基础工艺路线用途配置弹窗应已删除，避免继续暴露排产/批记录入口。'
)

const pageSource = fs.readFileSync(pagePath, 'utf8')

const assertNoBatchVisibleColumns = (source, sourceName) => {
  assert(
    !/<el-table-column[^>]*label="基础工序默认批记录"/.test(source),
    `${sourceName} 不得显示“基础工序默认批记录”列。`
  )

  const remarkColumns = [...source.matchAll(/<el-table-column\b[^>]*label="备注"[^>]*>/g)]
  for (const column of remarkColumns) {
    assert(
      !column[0].includes('v-if="configType === \'BATCH\'"'),
      `${sourceName} 批记录配置不得单独显示旧批记录专属“备注”列。`
    )
  }
}

assert(
  pageSource.includes('label="批记录表单"'),
  '工艺流程批记录配置必须保留批记录表单绑定列。'
)
assert(
  pageSource.includes('v-model="report.batchRecordReportId"'),
  '批记录表格绑定列必须继续绑定多批记录报表。'
)
assert(
  pageSource.includes('placeholder="槽位"') &&
    pageSource.includes('placeholder="记录类型"') &&
    pageSource.includes('placeholder="校验策略"') &&
    pageSource.includes('placeholder="必填策略"') &&
    pageSource.includes('placeholder="权限范围"'),
  '工艺流程批记录配置必须继承旧批记录路线的槽位、记录类型、校验策略、必填策略和权限范围配置能力。'
)
assert(
  pageSource.includes('ProRouteFlowConfigApi.saveBatchRecordConfig'),
  '工艺流程批记录配置必须使用新的工艺流程批记录配置保存接口。'
)

assertNoBatchVisibleColumns(pageSource, '工艺流程批记录配置页面')

console.log('PASS: MES batch route config hides default batch record and remark columns')
