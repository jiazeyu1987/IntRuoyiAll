# Task: DCC 文件类别列表列调整与删除操作

## Goal

调整 DCC 文件类别列表页：
- 隐藏“上级类别、排序、类别说明、备注”
- `审核`、`批准` 列显示该文件类别审批矩阵第 2 层和第 3 层的岗位配置
- 对固定岗位 `900335 / 900336` 显示真实岗位名，不显示 `岗位#900335 / 岗位#900336`
- 列表页 `分发`、`培训` 统一显示为 `必需`
- 保留并验证 `删除` 按钮入口

## Scope

- 仅修改 DCC 文件类别列表页前端展示逻辑和本任务验证脚本
- 不改删除按钮行为
- 不新增 fallback 或 mock 数据
- 使用真实本地运行时 `http://127.0.0.1:8081`

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-mes-pro-route-edit-delete-enable/task.md`
- Status before this task: blocked by user reprioritization on 2026-05-17
- Impact: does not block this DCC list refinement

## BDD

BDD: 文件类别列表隐藏非必要列 -> Given 用户打开 DCC 文件类别列表页 / When 列表加载完成 / Then 表格中不再显示“上级类别、排序、类别说明、备注”。

BDD: 文件类别列表显示矩阵岗位配置 -> Given 某文件类别已经配置固定四层审批矩阵 / When 用户查看该类别所在行 / Then `审核` 列显示审批矩阵第二层岗位配置，`批准` 列显示审批矩阵第三层岗位配置，而不是解析后的审批人。

BDD: 固定岗位显示真实岗位名 -> Given 某文件类别的第三层审批矩阵配置包含 `900335 / 900336` / When 用户查看该类别所在行 / Then `批准` 列显示 `编制部门负责人 / 授权代表`，而不是 `岗位#900335 / 岗位#900336`。

BDD: 列表页分发和培训统一显示为必需 -> Given 用户查看任意文件类别所在行 / When 列表渲染完成 / Then `分发` 和 `培训` 两列均显示 `必需`。

BDD: 文件类别列表删除入口保留可用 -> Given 用户具有文件类别管理权限 / When 用户查看该类别所在行 / Then `删除` 按钮仍可见并能打开真实确认框。

## Milestones

- [x] M1: 识别历史实现仍按解析审批人展示
- [x] M2: 将列表页改为按矩阵岗位 id 映射岗位名称展示
- [x] M3: 增加 `900335 / 900336` 固定岗位真实名称兜底
- [x] M4: 将列表页 `分发/培训` 统一显示为 `必需`
- [x] M5: 更新真实 Playwright 验证并完成 GREEN

## Expected Verification

- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-file-category-list-columns-actions\scripts\verify-dcc-file-category-list-columns-actions.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260517-dcc-file-category-list-columns-actions/frontend-feature-evidence.md`

## Current Status

Completed.

## Final Verification

- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-file-category-list-columns-actions\scripts\verify-dcc-file-category-list-columns-actions.mjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260517-dcc-file-category-list-columns-actions/frontend-feature-evidence.md` -> PASS
- Live result:
  - route: `http://127.0.0.1:8081/dcc/controlled-file/categories`
  - headers: `类别编码, 类别名称, 绑定目录, 启用状态, 分发, 培训, 审核, 批准, 创建时间, 操作`
  - target row: `INTAUTH-2 / 生产用设备清单`
  - `审核 = 编制人直接主管 / QA / QMS / 生产`
  - `批准 = 编制部门负责人 / 授权代表`
  - table contains no `岗位#900335`, no `岗位#900336`, and no `关闭`
  - `审批矩阵` dialog opens successfully
  - `删除` confirmation dialog opens successfully

## Blockers

None.
