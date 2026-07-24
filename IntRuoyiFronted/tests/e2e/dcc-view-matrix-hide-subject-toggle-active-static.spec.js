const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const table = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'),
  'utf8'
)
const api = fs.readFileSync(path.join(root, 'src/api/dcc/controlledFile/fileCategories.ts'), 'utf8')

assert.ok(
  !table.includes('label="可查阅主体"') && !table.includes('formatSubjects('),
  '查看矩阵总览表不得显示可查阅主体列'
)

assert.ok(
  !table.includes('data-testid="dcc-view-matrix-active-toggle"') &&
    !table.includes('toggleCategoryActive') &&
    !table.includes('updateFileCategory') &&
    !table.includes('getFileCategoryList') &&
    !table.includes('activeToggleLoadingId'),
  '查看矩阵总览不得继续保留启用状态按钮和启停更新逻辑'
)

assert.ok(
  !table.includes('label="启用状态"') &&
    !table.includes("{{ row.active ? '启用' : '禁用' }}"),
  '查看矩阵总览不得继续显示启用状态列'
)

assert.ok(
  api.includes('sort: number') &&
    api.includes('export const updateFileCategory'),
  '类别 API 合同仍需保留正式更新能力，避免影响其他页面'
)

assert.ok(
  !table.includes('find((item) => item.id === row.categoryId)') &&
    !table.includes('sort: currentCategory.sort') &&
    !table.includes('sort: row.sort'),
  '查看矩阵移除启停能力后不得继续保留启停排序补丁逻辑'
)

console.log('dcc view matrix hide subject toggle active static contract PASS')
