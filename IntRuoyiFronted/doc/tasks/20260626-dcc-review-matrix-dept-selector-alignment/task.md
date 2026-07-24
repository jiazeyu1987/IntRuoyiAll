# 任务：DCC 审阅矩阵部门选择器对齐查看矩阵

## Current Status

completed

## 任务目标

- 修复 `src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue` 中 `DEPT` 规则的 `对应部门` 选择器。
- 对齐 `CategoryViewMatrixDialog.vue` 的部门树口径：首层直接显示部门，不显示公司根节点；公司上下文无法唯一定位时返回空树，不混出多家公司部门。
- 不改变审阅矩阵的其他主体类型、预览、保存 payload、自动解析人员和后端接口。

## 当前状态

status: completed

## 上一相关任务检查

- 已检查前端上一相关任务 `D:\\ProjectPackage\\Int\\IntRuoyi\\yudao-ui-admin-vue3\\doc\\tasks\\20260625-dcc-review-matrix-owner-role-triangle-alignment\\task.md`，状态为 `completed`，允许继续本任务。
- 当前前端仓存在其他业务脏改；本次只修改审阅矩阵/查看矩阵部门树相关组件、定向 E2E 与本任务文档。

## 经验门禁

- 来源：`D:\\ProjectPackage\\Int\\IntRuoyi\\docs\\experience-index.md`
- 命中文档：
  - `D:\\ProjectPackage\\Int\\IntPP\\FRONTEND_STYLE.md`
  - `D:\\ProjectPackage\\Int\\IntRuoyi\\docs\\login-access.md`
  - `D:\\ProjectPackage\\Int\\IntRuoyi\\docs\\agent-memory\\project-error-prevention.md`
- 适用强制门禁：
  - 页面与弹窗必须保持现有 IntPP 运维台风格，只做部门树行为修复。
  - 必须先补 RED 静态/真实回归，再做最小实现。
  - 真实 Playwright E2E 前必须先跑官方 `login-preflight.mjs`，并在执行日志写入 `experience-preflight`。
  - 不得通过 fallback、mock、静默空树补全外的兜底逻辑掩盖公司上下文缺失。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。公司上下文无法唯一推断时直接返回空树。
- `是否从根因和长期维护角度解决`：是。抽取共享部门树裁剪 helper，统一查看矩阵与审阅矩阵口径。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 审阅矩阵选择部门首层直接显示部门 -> Given 管理员编辑审阅矩阵中的 DEPT 规则 When 打开对应部门选择器 Then 首层节点直接是部门而不是公司根节点。`
- `BDD: 公司上下文无法唯一推断时不混合多公司 -> Given 当前规则无法唯一定位公司上下文 When 打开对应部门选择器 Then 不显示公司根节点，也不混合多家公司部门。`
- `BDD: 查看矩阵复用共享 helper 后行为不回归 -> Given 管理员打开查看矩阵维护弹窗 When 查看对应部门选择器 Then 仍只显示当前公司下一层部门，且不显示公司根节点。`

## 里程碑

1. M1：创建任务文档并记录门禁。`DONE`
2. M2：补静态/真实 RED 断言。`DONE`
3. M3：实现共享 helper 并接入两个弹窗。`DONE`
4. M4：执行静态、类型检查与真实 E2E，补齐证据。`DONE`

## 完成记录

- 审阅矩阵 `DEPT` 选择器已改为按规则裁剪部门树，首层直接显示部门，不再显示公司根节点。
- 查看矩阵与审阅矩阵已复用共享 `departmentTreeScope.ts` helper，统一“跳过虚拟顶级部门、只保留公司下一层部门”的口径。
- 真实 E2E 已验证：样本部门的公司根节点不会出现在首层可见选项中，目标部门仍可正常选择、预览、保存与回读。

## 预期验证

- `node tests/e2e/dcc-review-matrix-tab-static.spec.js`
- `node tests/e2e/dcc-view-matrix-independent-source-static.spec.js`
- `pnpm ts:check`
- `node ..\\scripts\\preflight\\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /dcc/controlled-file/categories --target-text 审阅矩阵`
- `node tests/e2e/dcc-review-matrix-tab-real.e2e.js`
