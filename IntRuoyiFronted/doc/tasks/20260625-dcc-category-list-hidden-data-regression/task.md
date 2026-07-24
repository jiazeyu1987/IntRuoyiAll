# 任务：DCC 文件类别列表数据被默认隐藏回归修复

## 任务目标

修复 `src/views/dcc/controlled-file/categories/index.vue` 中 DCC 文件类别 `类别列表` 页签把真实文件类别默认隐藏的问题：

- 保持类别列表继续展示真实 `getFileCategoryList()` 返回的主数据。
- 保留审批摘要列的真实展示，但不再以“审核/批准岗位已配置”作为列表可见前置条件。
- 不修改后端接口、不删除任何类别主数据、不引入 fallback。

## 当前状态

status: completed

## Current Status

completed

## 上一相关任务检查

- 已检查上一前端任务 `doc/tasks/20260625-showroom-base-workbook-import/task.md`。
- 该任务已于 `2026-06-25 20:24 +08:00` 标记为 `blocked`，原因是用户在当前线程切换到更高优先级的 DCC 类别列表数据丢失回归排查；允许先处理本任务。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - DCC 类别列表属于密集操作台表格，修复时必须保持 IntPP 生产列表风格，不新增卡片式重构或营销化结构。
  - 本任务仅做本机前端源码、静态验证和本机数据库只读排查，不执行真实 E2E、服务器写入、发布、备份、恢复或高风险动作，因此不触发 `experience-preflight`。
  - 必须直接暴露真实列表数据，不得用 mock、空成功、兼容兜底或吞异常掩盖类别主数据与审批摘要的真实状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。直接移除错误的默认可见性过滤，不增加兜底分支。
- `是否从根因和长期维护角度解决`：是。类别列表应展示类别主数据，审批矩阵是否已配置只能作为摘要信息而不是列表可见门槛。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 类别列表显示真实类别主数据 -> Given 后端返回真实 DCC 文件类别列表 / When 用户打开 DCC 文件类别 的 类别列表 页签 / Then 页面必须显示真实类别主数据，而不能因审批摘要为空直接隐藏整行。`
- `BDD: 审批摘要为空时显式显示占位 -> Given 某个文件类别尚未配置审核或批准岗位 / When 用户查看该类别行的审批摘要 / Then 摘要列应显示 '-' 占位，而不是把类别从列表默认移除。`
- `BDD: 查询条件只影响查询字段 -> Given 用户输入类别编码、类别名称或启用状态筛选 / When 类别列表重新计算可见行 / Then 只按查询条件过滤，不再附带审批摘要是否存在的隐式过滤。`

## 里程碑

1. M1：创建任务文档、补记前置任务状态、记录经验门禁与 BDD。`DONE`
2. M2：编写回归静态测试 RED，明确类别列表不应再依赖审批摘要过滤。`DONE`
3. M3：最小修复类别列表过滤逻辑。`DONE`
4. M4：运行定向静态验证与回归证据校验。`DONE`

## 预期验证

- `node tests/e2e/dcc-category-governance-summary-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260625-dcc-category-list-hidden-data-regression/bug-regression-evidence.md`

## Cleanup Keep

- `doc/tasks/20260625-dcc-category-list-hidden-data-regression/task.md`
- `doc/tasks/20260625-dcc-category-list-hidden-data-regression/execution-log.md`
- `doc/tasks/20260625-dcc-category-list-hidden-data-regression/bug-regression-evidence.md`

## 最终验证结果

- `node tests/e2e/dcc-category-governance-summary-static.spec.js`：PASS
- `node tests/e2e/dcc-review-matrix-tab-static.spec.js`：PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260625-dcc-category-list-hidden-data-regression/bug-regression-evidence.md`：PASS
- 本机只读核验：PASS，确认 `/dcc/file-categories` 后端接口未删数据，问题来自前端默认过滤。
