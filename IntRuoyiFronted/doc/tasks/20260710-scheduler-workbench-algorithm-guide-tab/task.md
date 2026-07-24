# 排产员工作台算法说明页签

## 任务目标

- 在“排产员工作台”增加一个“排产逻辑”页签。
- 用不需要编程知识也能理解的短句和流程，说明系统如何选择工单、安排先后、分配资源、处理冲突并生成结果。
- 说明内容必须以当前真实排产实现为准，不修改排产接口、权限、数据或算法。

## 里程碑

1. [已完成] 读取项目经验、前端交付规范并核对前序任务状态。
2. [已完成] 定位排产员工作台页面、现有页签和排产算法真实规则。
3. [已完成] 记录 BDD 场景并新增失败的前端静态契约测试。
4. [已完成] 实现通俗、紧凑、可扫描的“排产逻辑”页签。
5. [已完成] 运行目标测试、相关回归、类型检查和真实页面验证。
6. [已完成] 更新任务证据、执行收尾清理并准备独立提交本任务改动。

## 预期验证

- 页面存在“排产逻辑”页签，进入后不触发新增接口或业务写入。
- 普通用户可按清晰顺序理解排产输入、优先顺序、资源安排、冲突处理和输出结果。
- 内容避免代码名、类名、数据库名和算法术语堆砌；每条说明保持简短。
- 页签布局符合现有排产员工作台和统一前端样式，在常见桌面宽度下无重叠或横向溢出。
- 原有工作台页签、查询、设置、导入导出和冒烟测试能力保持不变。
- 静态契约测试先 RED 后 GREEN，类型检查和真实页面只读验证通过。

## 经验门禁

- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`；PowerShell 5.1 不使用 `&&`，中文文件使用 UTF-8 明确读写，代码和文档修改使用 `apply_patch`。
- 项目经验：已读取 `docs/experience-index.md` 与 `docs/agent-memory/project-error-prevention.md`；排产说明必须基于当前源码和现有业务口径，不得凭历史记忆或用兜底文案掩盖未知规则。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；采用紧凑、可扫描、浅灰蓝边框和蓝色激活态，不做独立视觉体系。
- 前端交付：已读取 `frontend-feature-delivery`、`frontend-contract.md`；保持现有路由、API、权限和状态归属，执行 BDD、RED、GREEN 和真实用户路径验证。
- 前序任务：前端仓最新任务 `20260710-edhr-batch-process-card-density` 已标记 completed；当前工作区存在其他任务改动，本任务不得覆盖或提交无关文件。
- 真实 E2E：执行前必须读取 `docs/login-access.md`，运行官方登录预检，并只使用本机测试租户进行只读验证。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；说明内容直接映射当前排产规则，并在工作台内形成固定、可维护的业务说明入口。
- 是否存在临时补丁或绕过：否。

## 当前状态

completed

## Current Status

completed

## 最终验证

- `node tests/e2e/mes-scheduler-workbench-algorithm-guide-tab-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- `node tests/e2e/mes-scheduler-workbench-process-wip-unified-list-template-static.spec.js` -> PASS。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- 官方登录预检进入本机测试租户 `/mes/pro/scheduler-workbench` -> PASS。
- 真实页面点击“排产逻辑”后关键说明全部可见，无横向溢出，MES 写请求为 0 -> PASS。
- 前端交付证据校验与 task-closeout preview -> PASS。
- 已知独立问题：当前 HEAD 的策略设置静态测试缺少默认夜班表单绑定，本任务未修改该功能。

## Cleanup Candidates

- `doc/tasks/20260710-scheduler-workbench-algorithm-guide-tab/algorithm-guide.png`
- `doc/tasks/20260710-scheduler-workbench-algorithm-guide-tab/frontend-feature-evidence.md`
- `doc/tasks/20260710-scheduler-workbench-algorithm-guide-tab/verify-algorithm-guide.cjs`
