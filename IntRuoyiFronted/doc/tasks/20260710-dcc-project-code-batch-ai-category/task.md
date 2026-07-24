# 任务：DCC项目代码批量 AI 分类按钮

## 任务目标

在 `基础数据 / DCC项目代码` 列表页工具栏导出按钮右侧新增 `批量AI分类` 按钮。点击后忽略当前筛选条件，分页拉取全部 DCC 项目代码，并按项目逐个串行复用现有关联文档 AI 分类链路；空项目计入进度，失败项目继续后续并最终汇总。

## 里程碑

1. 已完成：创建任务目录并记录 BDD、设计约束、经验门禁和预期验证。
2. 已完成：新增批量 AI 分类静态契约并取得 RED。
3. 已完成：实现列表工具栏按钮、项目级进度、串行分类和失败汇总。
4. 已完成：运行证据校验和收尾预览。
5. 待完成：按当前工作区可分离情况提交本任务改动或记录提交阻塞。

## 预期验证

- `pnpm.cmd e2e:dcc:project-code-batch-ai-category:static`
- `pnpm.cmd e2e:dcc:project-code-associated-three-column:static`
- `pnpm.cmd e2e:dcc:project-code-ai-category-permission:static`
- `pnpm.cmd ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260710-dcc-project-code-batch-ai-category/frontend-feature-evidence.md`

## BDD 场景

- BDD: 列表页触发批量 AI 分类 -> Given 用户进入基础数据 / DCC项目代码列表页 When 点击工具栏 `批量AI分类` Then 系统忽略当前筛选条件分页拉取全部项目代码，并显示项目级进度条。
- BDD: 项目和文件串行分类 -> Given 全部项目代码包含多个项目 When 批量 AI 分类运行 Then 系统先完成当前项目全部候选文件 AI 分类，再进入下一个项目，不并发处理。
- BDD: 空项目计入进度 -> Given 某个项目没有待分类关联文件 When 批量 AI 分类检查该项目 Then 该项目计入已处理并统计为无待分类。
- BDD: 失败继续并汇总 -> Given 某个项目或文件 AI 分类失败 When 批量任务运行 Then 系统记录失败项目、失败文件和后端错误，继续后续项目，结束后展示失败汇总。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文文件读写使用 UTF-8 路径，不使用默认编码重定向。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；按钮和进度条保持运营台紧凑工具栏风格。
- DCC 批量识别与运行态防错：已读取 `docs/agent-memory/project-error-prevention.md`；列表/详情关联文件分页不使用超后端契约的大页大小，批量动作明确进度与失败口径。
- 前端交付契约：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；记录目标、非目标、BDD、RED/GREEN、权限、加载、错误与验证证据。
- 真实 E2E：本轮默认先做静态契约与类型检查；如后续执行真实 E2E，需先读取 `docs/login-access.md` 并跑登录 preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。失败继续是用户明确选择的业务策略，失败项目会显式记录并汇总提示。
- 是否从根因和长期维护角度解决：是。复用现有关联文档 AI 分类 API 和权限，不新增重复后端契约。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED：已修复按钮不可见问题。前端权限判断与后端统一为两个权限同时满足；本机测试租户已补齐权限菜单和角色授权，真实登录后 `批量AI分类` 在导出按钮右侧可见。

## 回归 BDD

- BDD: 有权限用户可见批量 AI 分类入口 -> Given 用户拥有 DCC 项目代码或受控文件更新权限 When 进入基础数据 / DCC项目代码列表页 Then 工具栏导出按钮右侧显示 `批量AI分类`。
- BDD: 无权限用户不暴露批量写入入口 -> Given 用户不具备 DCC 项目代码和受控文件更新权限 When 进入列表页 Then 不显示 `批量AI分类`，避免越权触发批量写操作。

## 验证记录

- RED: `pnpm.cmd e2e:dcc:project-code-batch-ai-category:static` -> FAIL，缺少工具栏 `批量AI分类` 按钮。
- GREEN: `pnpm.cmd e2e:dcc:project-code-batch-ai-category:static` -> PASS。
- GREEN: `pnpm.cmd e2e:dcc:project-code-associated-three-column:static` -> PASS。
- GREEN: `pnpm.cmd e2e:dcc:project-code-ai-category-permission:static` -> PASS。
- RED: `pnpm.cmd ts:check` -> FAIL，Node 默认 4GB 堆内存 OOM。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260710-dcc-project-code-batch-ai-category/frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260710-dcc-project-code-batch-ai-category --mode preview` -> PASS，delete 为空。

## Current Status

completed

## 最终验证结果

- RED: `pnpm.cmd e2e:dcc:project-code-batch-ai-category:static` -> FAIL，缺少工具栏 `批量AI分类` 按钮。
- GREEN: `pnpm.cmd e2e:dcc:project-code-batch-ai-category:static` -> PASS。
- GREEN: `pnpm.cmd e2e:dcc:project-code-associated-three-column:static` -> PASS。
- GREEN: `pnpm.cmd e2e:dcc:project-code-ai-category-permission:static` -> PASS。
- RED: `pnpm.cmd ts:check` -> FAIL，Node 默认 4GB 堆内存 OOM。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- GREEN: 前端 evidence 校验 -> PASS。
- GREEN: task-closeout preview -> PASS，未发现 blocked，delete 为空。
- RED: 真实可见性探针 -> FAIL，测试租户缺少两个更新权限，按钮数量为 0。
- RED: 两项权限静态契约 -> FAIL，前端使用任一权限匹配，与后端 AND 权限契约不一致。
- GREEN: 前端改为 `canRunAiCategory` 同时校验两个权限。
- GREEN: 本机测试租户权限补齐事务 -> PASS，新增两个权限菜单和 6 条角色菜单授权，其他租户新增记录为 0。
- GREEN: 真实可见性探针 -> PASS，按钮数量为 1 且可见。
- GREEN: 本任务相关 ESLint 与 TypeScript 检查 -> PASS。
- GREEN: task-closeout preview/apply -> PASS，一次性探针和临时证据已清理。
- REGRESSION: `e2e:dcc:project-code-basic-data:static` 因工作区既有标准列表模板改造仍断言旧筛选表单而失败，不属于本次修复范围。

## 提交状态

- COMMITTED：本任务改动已随提交 `任务: 增加DCC项目代码批量AI分类` 单独提交。
- COMMITTED：按钮可见性与权限语义回归修复已按任务范围单独提交。

## Cleanup Candidates

- `doc/tasks/20260710-dcc-project-code-batch-ai-category/bug-regression-evidence.md`
- `doc/tasks/20260710-dcc-project-code-batch-ai-category/database-schema-evidence.md`
- `doc/tasks/20260710-dcc-project-code-batch-ai-category/runtime-visibility-probe.mjs`
- `output/playwright/dcc-project-code-batch-ai-category-visibility.png`

## Cleanup Keep

- `doc/tasks/20260710-dcc-project-code-batch-ai-category/frontend-feature-evidence.md`
