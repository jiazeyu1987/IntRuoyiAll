completed

# 任务：DCC 审阅矩阵负责人/角色/三角标记前端对齐

## Current Status

completed

## 任务目标

在不改变审阅矩阵接口 URL 与整体 JSON 结构的前提下，完成前端审阅矩阵负责人/角色/三角标记对齐：

- 审阅矩阵规则表删除行内 `备注` 列，仅保留矩阵级 `备注`。
- 标记列只保留 `▲`，所有摘要、编辑器、预览和旧数据回显都统一显示 `▲`。
- `主体类型` 新增 `ROLE`，并按主体类型切换真实选择控件：
  - `USER`：用户下拉
  - `DEPT`：部门树
  - `ROLE`：系统角色下拉
  - `POST`：系统岗位下拉
  - `DCC_POSITION`：DCC 岗位下拉
- 切换主体类型时清理不适用的 `subjectId / subjectName / subjectDepartmentPath`。
- 预览表把“岗位集合”改成“主体集合”，列表摘要统一输出 `标签 ▲`。

## 当前状态

status: completed

## 上一相关任务检查

- 已检查前端上一任务 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260625-dcc-category-list-hidden-data-regression\task.md`，状态为 `completed`，允许继续本任务。
- 当前前端仓只修改审阅矩阵相关代码与本任务文档，不覆盖其他用户改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 审阅矩阵页签、表格与弹窗必须保持 IntPP 运维台风格，不做无关布局重构。
  - 本阶段先做本机源码、静态测试与真实 E2E；真实 E2E 前必须先记录 `experience-preflight`。
  - Playwright 登录与写入验证只允许本机 `测试租户/aoteman`，若登录或权限异常必须直接阻塞。
  - 不得用 mock 数据、占位成功、空 catch 或静默 toast 掩盖真实后端错误。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。旧 `●` 只做规范化，不保留编辑选项或兼容双轨。
- `是否从根因和长期维护角度解决`：是。审阅矩阵编辑器与摘要统一真实主体模型，避免前端继续假设“部门树/岗位数组”。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 审阅矩阵编辑器只显示 ▲ 标记 -> Given 管理员打开审阅矩阵编辑弹窗 When 查看规则表 Then 标记列只能选择 ▲，旧数据中的 ● 也显示为 ▲。`
- `BDD: 审阅矩阵编辑器删除行内备注列 -> Given 管理员编辑某条规则 When 查看规则表 Then 不再存在行内备注输入列，但矩阵顶部备注输入继续保留。`
- `BDD: 审阅矩阵按主体类型切换真实选择器 -> Given 管理员切换 USER/DEPT/ROLE/POST/DCC_POSITION When 选择主体 Then 页面展示对应选择控件并同步清理不适用字段。`
- `BDD: 审阅矩阵摘要与预览统一负责人语义 -> Given 审阅矩阵存在规则 When 查看列表摘要和预览阶段 Then 摘要统一显示 标签 ▲，预览列名显示主体集合。`

## 里程碑

1. M1：创建任务文档并记录前置门禁。`DONE`
2. M2：补静态 RED 测试，锁定前端合同。`DONE`
3. M3：实现编辑器、摘要、预览与类型扩展。`DONE`
4. M4：执行静态验证与真实 Playwright E2E。`DONE`

## 阻塞记录

- `2026-06-25 20:46 +08:00`：用户切换为更高优先级需求“删除 DCC 文件类别列表红框中的审批摘要列”。当前任务尚未进入 RED 实施阶段，先阻塞并让位给新任务。
- `2026-06-25 23:40 +08:00`：真实 Playwright E2E 已通过登录前置并扫描真实样本，但测试租户当前未找到存在有效成员的系统角色，按无 mock 原则阻塞 ROLE 审阅矩阵保存/回读验证。
- `2026-06-26 00:23 +08:00`：上述 ROLE 样本阻塞已解除；确认旧后端运行包重启后，测试租户可找到可用部门负责人和角色样本，真实 E2E 完成保存与回读。

## 预期验证

- `node tests/e2e/dcc-review-matrix-tab-static.spec.js`
- `node tests/e2e/dcc-review-matrix-tab-real.e2e.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260625-dcc-review-matrix-owner-role-triangle-alignment/frontend-feature-evidence.md`

## 完成记录

- 审阅矩阵编辑器已按 `USER / DEPT / ROLE / POST / DCC_POSITION` 切换真实选择控件。
- 规则表内备注列已删除，规则标记统一为 `▲`，预览列名已改为“主体集合”。
- 最终验证：
  - `node tests/e2e/dcc-review-matrix-tab-static.spec.js` -> PASS
  - `node --check tests/e2e/dcc-review-matrix-tab-real.e2e.js` -> PASS
  - `node tests/e2e/dcc-review-matrix-tab-real.e2e.js` -> PASS，证据曾写入 `dcc-review-matrix-tab-real-evidence.json`
