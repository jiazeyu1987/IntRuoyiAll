const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/system/role/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  /class="permission-role-toolbar"/.test(source),
  '权限角色页必须提供稳定的工具栏类名，便于后续约束查询区与按钮区排布。'
)

assert.ok(
  /permission-role-toolbar__filters/.test(source),
  '权限角色页必须提供独立的查询字段容器，避免字段和按钮继续挤在同一个 inline form 流里。'
)

assert.ok(
  /permission-role-toolbar__actions/.test(source),
  '权限角色页必须提供独立的操作按钮容器，避免按钮组直接跟在日期字段后面被遮挡。'
)

assert.ok(
  /permission-role-toolbar__actions[\s\S]*?搜索[\s\S]*?重置[\s\S]*?新增权限角色[\s\S]*?导出[\s\S]*?批量删除/.test(
    source
  ),
  '权限角色页操作区必须完整承载搜索、重置、新增、导出和批量删除按钮。'
)

assert.ok(
  /\.permission-role-toolbar\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-direction:\s*column;[\s\S]*gap:\s*12px;/.test(
    source
  ),
  '权限角色工具栏外层必须改为纵向分组布局，避免所有控件继续挤在同一行。'
)

assert.ok(
  /\.permission-role-toolbar__filters\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*repeat\(auto-fit,\s*minmax\(240px,\s*1fr\)\);[\s\S]*gap:\s*12px 16px;/.test(
    source
  ),
  '权限角色查询字段区必须使用可自适应的 grid 布局，保证日期区在宽度收窄时自然换行。'
)

assert.ok(
  /\.permission-role-toolbar__actions\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-wrap:\s*wrap;[\s\S]*gap:\s*10px 12px;/.test(
    source
  ),
  '权限角色操作按钮区必须允许换行，并保持明确间距。'
)

console.log('PASS: role management toolbar layout static contract')
