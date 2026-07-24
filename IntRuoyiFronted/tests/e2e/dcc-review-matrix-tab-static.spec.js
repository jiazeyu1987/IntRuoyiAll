const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const categoryPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const reviewMatrixTable = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:review-matrix-tab:static'],
  'node tests/e2e/dcc-review-matrix-tab-static.spec.js',
  'package.json 必须提供 e2e:dcc:review-matrix-tab:static 脚本'
)

assert.ok(
  categoryPage.includes('label="DCC审阅矩阵"') || categoryPage.includes('label="审阅矩阵"'),
  '类别页必须提供审阅矩阵页签'
)
assert.ok(
  categoryPage.includes('<CategoryReviewMatrixTable />'),
  '审阅矩阵页签必须挂接独立的矩阵表格组件'
)

for (const token of [
  '第 1 / 4 层文控继续固定',
  'data-testid="dcc-review-matrix-table"',
  'getCategoryReviewMatrixRows',
  'deleteCategoryApprovalMatrix',
  'CategoryMatrixDialog',
  "row.configured ? '编辑' : '新增'",
  "@click=\"openMatrixDialog(row, 'preview')\""
]) {
  assert.ok(reviewMatrixTable.includes(token), `审阅矩阵页签必须具备：${token}`)
}

for (const label of [
  '类别编码',
  '类别名称',
  '审核规则',
  '批准规则',
  '操作'
]) {
  assert.ok(reviewMatrixTable.includes(`label="${label}"`), `审阅矩阵表格必须显示 ${label} 列`)
}

for (const removedLabel of [
  'label="可查阅主体"',
  'label="待审预览主体"',
  'label="下载规则"',
  'label="启用状态"',
  'label="当前状态/风险"',
  'label="当前版本"',
  'label="生效时间"',
  'label="备注"',
  'label="审核会签岗位"',
  'label="批准岗位"'
]) {
  assert.ok(!reviewMatrixTable.includes(removedLabel), `审阅矩阵表格不得继续显示 ${removedLabel} 列`)
}

for (const queryLabel of ['类别编码', '类别名称']) {
  assert.ok(reviewMatrixTable.includes(`label="${queryLabel}"`), `审阅矩阵查询区必须显示 ${queryLabel}`)
}

for (const removedQueryToken of [
  'label="启用状态"',
  'placeholder="请选择启用状态"',
  'label="是否已配置"',
  'placeholder="请选择配置状态"',
  'configured: true',
  'queryParams.configured = true',
  'v-model="queryParams.configured"',
  'active?: boolean',
  'configured?: boolean',
  'ACTIVE_STATUS_OPTIONS',
  'formatBooleanLabel',
  'getBooleanTagType'
]) {
  assert.ok(!reviewMatrixTable.includes(removedQueryToken), `审阅矩阵查询区不得继续保留旧筛选或启用状态逻辑：${removedQueryToken}`)
}

for (const actionLabel of ['新增', '编辑', '删除', '预览']) {
  assert.ok(reviewMatrixTable.includes(actionLabel), `审阅矩阵操作区必须包含 ${actionLabel}`)
}

for (const compactToken of [
  'class="review-matrix-table review-matrix-table--compact"',
  'class="matrix-cell-ellipsis"',
  'formatStageRuleSummary(row.rules, \'SIGNOFF\')',
  'formatStageRuleSummary(row.rules, \'APPROVAL\')'
]) {
  assert.ok(reviewMatrixTable.includes(compactToken), `审阅矩阵总览必须保留紧凑行高契约：${compactToken}`)
}

for (const compactStyle of [
  '.review-matrix-table--compact :deep(.el-table__header .el-table__cell)',
  '.review-matrix-table--compact :deep(.el-table__body .el-table__row)',
  'height: 52px;',
  'padding: 7px 10px;',
  'white-space: nowrap;',
  'text-overflow: ellipsis;',
  'max-width: 100%;'
]) {
  assert.ok(reviewMatrixTable.includes(compactStyle), `审阅矩阵总览必须实现紧凑表格样式：${compactStyle}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(reviewMatrixTable),
  '审阅矩阵页签不得引入 mock、fallback、降级或吞异常'
)

const reviewMatrixDialog = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue'
)

for (const dialogToken of [
  'data-testid="dcc-review-matrix-rule-editor"',
  '自动解析人员',
  '新增规则',
  'label="阶段"',
  'label="主体标签"',
  'label="标记"',
  'label="主体类型"',
  'label="对应部门"',
  'label="主体集合"',
  'placeholder="请选择系统用户"',
  'placeholder="请选择系统角色"',
  'placeholder="请选择系统岗位"',
  'placeholder="请选择 DCC 岗位"',
  "value=\"SIGNOFF\"",
  "value=\"APPROVAL\"",
  "value=\"ROLE\"",
  'dcc-review-matrix-effective-user-groups',
  'dcc-review-matrix-unresolved-rules',
  'applyDepartmentAutoMatchToViewMatrixRules'
]) {
  assert.ok(reviewMatrixDialog.includes(dialogToken), `审阅矩阵编辑器必须具备：${dialogToken}`)
}

assert.ok(
  reviewMatrixDialog.includes('getDepartmentTreeForRule(row)') &&
    reviewMatrixDialog.includes('resolveDepartmentCompanyRootId') &&
    reviewMatrixDialog.includes('resolveReviewMatrixCompanyRootId') &&
    reviewMatrixDialog.includes('resolveRuleDepartmentId') &&
    reviewMatrixDialog.includes('getCompanyChildDepartmentTree') &&
    !reviewMatrixDialog.includes(':data="departmentTree"'),
  '审阅矩阵编辑器对应部门必须按规则裁剪部门树，首层从部门开始且不能继续直接绑定整棵部门树'
)

assert.ok(
  reviewMatrixDialog.includes('findExactMatchedDepartmentIdByLabel') &&
    reviewMatrixDialog.includes('resolveCategoryCompanyRootId') &&
    reviewMatrixDialog.includes('uniqueCompanyRootIds') &&
    reviewMatrixDialog.includes('return []'),
  '审阅矩阵编辑器必须按既定优先级推断唯一公司上下文，无法唯一推断时返回空树而不是混出多家公司'
)

for (const removedDialogToken of [
  '第二层审核会签岗位不能为空',
  '第三层批准岗位必须恰好选择 2 个',
  'formData.signoffPositionIds',
  'formData.approvalPositionIds',
  'placeholder="请选择第二层审核会签岗位"',
  'placeholder="请选择第三层批准岗位（恰好 2 个）"',
  '<el-table-column label="备注"',
  'value="●"',
  'row.remark',
  'remark: normalizeText(rule.remark)',
  'label="岗位集合"'
]) {
  assert.ok(!reviewMatrixDialog.includes(removedDialogToken), `审阅矩阵编辑器不得继续保留旧岗位数组契约：${removedDialogToken}`)
}

assert.ok(
  reviewMatrixTable.includes("return `${main} ▲`.trim()") ||
    !reviewMatrixTable.includes('rule.marker ?'),
  '审阅矩阵摘要必须统一输出固定的 ▲ 标记，而不是继续读取旧 marker'
)

console.log('PASS: DCC review matrix tab static contract')
