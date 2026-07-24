const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readOptionalSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}

const packageJson = JSON.parse(readSource('package.json'))
const categoryPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const viewMatrixTable = readOptionalSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'
)
const viewMatrixDialog = readOptionalSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixDialog.vue'
)
const departmentTreeScope = readOptionalSource(
  'src/views/dcc/controlled-file/categories/components/departmentTreeScope.ts'
)
const categoryApi = readSource('src/api/dcc/controlledFile/fileCategories.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')

assert.strictEqual(
  packageJson.scripts['e2e:dcc:view-matrix:static'],
  'node tests/e2e/dcc-view-matrix-independent-source-static.spec.js',
  'package.json must expose the independent view matrix static check'
)

assert.ok(categoryPage.includes('label="查看矩阵"'), '类别页必须提供 查看矩阵 页签')
assert.ok(
  categoryPage.includes('<CategoryViewMatrixTable />'),
  '查看矩阵页签必须挂接独立的查看矩阵表格组件'
)

for (const token of [
  'view-matrix',
  'activeTab = ref',
  "label=\"查看矩阵\"",
  'CategoryViewMatrixTable',
  'CURRENT_VIEW_MATRIX'
]) {
  assert.ok(categoryPage.includes(token) || detailPage.includes(token) || categoryApi.includes(token),
    `独立查看矩阵实现必须出现：${token}`)
}

assert.ok(
  categoryApi.includes('/dcc/file-categories/view-matrix') &&
    categoryApi.includes('/view-matrix/effective-preview') &&
    categoryApi.includes('/view-matrix/user-lookup'),
  '查看矩阵 API 必须独立于 review-matrix'
)

assert.ok(
  detailPage.includes("CURRENT_VIEW_MATRIX: '当前查看矩阵'"),
  '详情页必须把普通查阅原因来源切换为 当前查看矩阵'
)

assert.ok(
  viewMatrixTable,
  '查看矩阵页签必须提供 CategoryViewMatrixTable.vue 组件'
)

assert.ok(
  viewMatrixTable.includes('label="可查阅"'),
  '查看矩阵总览表必须把查阅规则列显示为“可查阅”'
)

assert.ok(
  viewMatrixTable.includes('getCategoryNameStatusClass(row)') &&
    viewMatrixTable.includes('resolveCategoryNameRecognitionStatus') &&
    viewMatrixTable.includes('getSimpleDeptList') &&
    viewMatrixTable.includes('resolveViewMatrixDepartmentRecognitionStatus') &&
    viewMatrixTable.includes('view-matrix-category-name--recognized-all') &&
    viewMatrixTable.includes('view-matrix-category-name--recognized-partial') &&
    viewMatrixTable.includes('view-matrix-category-name--recognized-none'),
  '查看矩阵总览类别名称必须复用部门初步对应口径显示绿/黄/红'
)

assert.ok(
  viewMatrixTable.includes("return rules.map(formatRuleSubject).join(' / ')") &&
    viewMatrixTable.includes("`${rule.subjectLabel || rule.subjectName || '-'} ${rule.marker || ''}`.trim()"),
  '查看矩阵总览可查阅列不得继续显示 G/D/O 等 Excel 来源列号'
)

for (const removedTableLabel of ['label="Excel 查阅规则"', 'label="待审预览主体"', 'label="当前状态/风险"']) {
  assert.ok(
    !viewMatrixTable.includes(removedTableLabel),
    `查看矩阵总览表不得继续展示旧列：${removedTableLabel}`
  )
}

for (const removedMatrixToken of [
  'label="启用状态"',
  'label="是否已配置"',
  'v-model="queryParams.active"',
  'v-model="queryParams.configured"',
  'data-testid="dcc-view-matrix-active-toggle"',
  'toggleCategoryActive',
  'getFileCategoryList',
  'updateFileCategory'
]) {
  assert.ok(
    !viewMatrixTable.includes(removedMatrixToken),
    `查看矩阵总览不得继续保留启用状态筛选/列逻辑：${removedMatrixToken}`
  )
}

for (const removedVisibleCopy of ['Excel 查阅规则', 'Excel 标记', 'Excel 文件', 'Excel 未映射']) {
  assert.ok(
    !viewMatrixTable.includes(removedVisibleCopy) && !viewMatrixDialog.includes(removedVisibleCopy),
    `DCC 查看矩阵用户可见文案不得显示：${removedVisibleCopy}`
  )
}

assert.ok(
  !viewMatrixTable.includes('view-matrix-risk-list'),
  '查看矩阵总览表移除风险列后不得保留对应风险列样式'
)

assert.ok(
  viewMatrixTable.includes('data-testid="dcc-view-matrix-table"') &&
    viewMatrixTable.includes('data-testid="dcc-view-matrix-edit"') &&
    viewMatrixTable.includes('data-testid="dcc-view-matrix-effective-preview"') &&
    viewMatrixTable.includes('data-testid="dcc-view-matrix-user-lookup"') &&
    viewMatrixDialog.includes('data-testid="dcc-view-matrix-rule-editor"') &&
    viewMatrixDialog.includes('data-testid="dcc-view-matrix-dialog-effective-users"') &&
    viewMatrixDialog.includes('refreshPreview') &&
    viewMatrixDialog.includes('data-testid="dcc-view-matrix-dialog-save"'),
  '查看矩阵表格必须提供总览、编辑、有效权限预览和按人反查入口；弹窗预览由自动刷新和保存前校验触发'
)

assert.ok(
  viewMatrixDialog.includes('label="对应部门"') &&
    viewMatrixDialog.includes('subjectDepartmentPath'),
  '查看矩阵编辑弹窗必须展示由后端解析的对应部门'
)

assert.ok(
  viewMatrixDialog.includes('data-testid="dcc-view-matrix-dialog-effective-user-grid"') &&
    viewMatrixDialog.includes('view-matrix-user-grid') &&
    viewMatrixDialog.includes('grid-template-columns'),
  '查看矩阵编辑弹窗有效权限预览必须使用人名 grid 展示'
)

assert.ok(
  viewMatrixDialog.includes('data-testid="dcc-view-matrix-dialog-unresolved-rules"') &&
    viewMatrixDialog.includes('previewUnresolvedRules') &&
    viewMatrixDialog.includes('max-height') &&
    viewMatrixDialog.includes('overflow-y: auto'),
  '查看矩阵编辑弹窗必须区分未解析规则与显示不全，并为多分组预览提供滚动能力'
)

for (const removedPreviewCopy of [
  'label="来源规则"',
  'label="查阅标记"',
  'data-testid="dcc-view-matrix-dialog-risks"',
  'label="风险编码"',
  'label="说明"',
  'label="阻塞"'
]) {
  assert.ok(
    !viewMatrixDialog.includes(removedPreviewCopy),
    `查看矩阵编辑弹窗预览区不得继续展示：${removedPreviewCopy}`
  )
}

assert.ok(
  viewMatrixDialog.includes('getSimpleDeptList') &&
    viewMatrixDialog.includes('<el-tree-select') &&
    viewMatrixDialog.includes('handleDepartmentChange') &&
    viewMatrixDialog.includes('syncSameLabelDepartment'),
  '查看矩阵编辑弹窗必须支持选择对应部门，并把同名主体同步为同一部门'
)

assert.ok(
  viewMatrixDialog.includes(':data="getDepartmentTreeForRule(row)"') &&
    viewMatrixDialog.includes('resolveDepartmentCompanyRootId') &&
    viewMatrixDialog.includes('getCompanyChildDepartmentTree') &&
    !viewMatrixDialog.includes(':data="departmentTree"') &&
    departmentTreeScope.includes('department.id !== companyRootId') &&
    departmentTreeScope.includes('return []'),
  '查看矩阵编辑弹窗的对应部门必须从主体所属公司下一层开始选择，不能直接选择公司根节点'
)

assert.ok(
  departmentTreeScope.includes('findExactMatchedDepartmentIdByLabel') &&
    departmentTreeScope.includes('createDepartmentByIdMap') &&
    departmentTreeScope.includes('resolveDepartmentCompanyRootId'),
  '查看矩阵与审阅矩阵必须复用共享部门树裁剪 helper'
)

assert.ok(
  viewMatrixDialog.includes(':model-value="getDepartmentSelectValue(row)"') &&
    viewMatrixDialog.includes(':disabled="Boolean(departmentLoadError)"') &&
    !viewMatrixDialog.includes('v-model="row.subjectId"') &&
    !viewMatrixDialog.includes("row.subjectType !== 'DEPT'") &&
    !viewMatrixDialog.includes('view-matrix-dialog__department-path') &&
    !viewMatrixDialog.includes(':content="row.subjectDepartmentPath"'),
  '查看矩阵编辑弹窗对应部门不得显示额外路径提示，且非 DEPT 主体也必须可选择部门'
)

assert.ok(
  viewMatrixDialog.includes('resolveViewMatrixCompanyRootId(row)') &&
    viewMatrixDialog.includes('resolveCategoryCompanyRootId') &&
    viewMatrixDialog.includes('uniqueCompanyRootIds') &&
    !viewMatrixDialog.includes('if (!companyRootId) {') &&
    !viewMatrixDialog.includes('resolveDepartmentCompanyRootId(department.id) !== department.id'),
  '查看矩阵编辑弹窗不得在公司上下文缺失时混合所有公司的下一层部门，必须沿用当前文件类型唯一公司上下文'
)

for (const removedDialogLabel of ['label="来源文件"', 'label="行号"', 'label="列"', 'label="主体 ID"']) {
  assert.ok(
    !viewMatrixDialog.includes(removedDialogLabel),
    `查看矩阵编辑弹窗不得继续展示技术列：${removedDialogLabel}`
  )
}

assert.ok(
  !viewMatrixTable.includes('reviewMatrixAccessService') &&
    !viewMatrixTable.includes('/review-matrix'),
  '查看矩阵组件不得继续复用 review-matrix 旧契约'
)

assert.ok(
  categoryApi.includes("| 'ROLE'") && viewMatrixDialog.includes('value="ROLE"'),
  '查看矩阵必须支持把 Excel ▲ 主管及以上映射到系统角色主体'
)

console.log('dcc independent view matrix static contract PASS')
