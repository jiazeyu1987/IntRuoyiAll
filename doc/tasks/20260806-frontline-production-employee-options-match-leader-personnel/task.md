# 一线生产员工弹窗匹配生产组长人员管理

## Task Goal

让一线生产填写页点击“员工”后弹出的候选员工，与当前生产组长“人员管理”列表中的人员保持一致；不得从全量用户、设备默认候选或前端本地过滤兜底。

## Milestones

- [x] 创建任务记录并读取前端实施、收尾和人员候选范围门禁。
- [x] 确认一线生产员工弹窗和生产组长人员管理的正式取数来源。
- [x] 先补静态合同 RED，锁定一线生产员工弹窗复用生产组长人员管理列表来源。
- [x] 实现最小正式修复，保持现有切换员工和提交链路不变。
- [x] 执行 GREEN 与相邻回归验证，记录结果。
- [x] 按用户追加要求执行只读真实页面 E2E：生产组长人员管理列表 vs 一线生产员工弹窗。

## Expected Verification

- `node tests\e2e\edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs`
- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs`
- `node tests\e2e\team-leader-workbench-static.spec.cjs`
- `node tests\e2e\frontline-team-config-static.spec.cjs`
- `node tests\e2e\production-personnel-management-static.spec.cjs`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm ts:check`
- `git diff --check -- <task-owned files>`
- `node --check doc\tasks\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup-real-e2e.cjs`
- `node doc\tasks\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup-real-e2e.cjs`

## Current Status

ready_for_closeout - 真实 E2E 已通过：生产组长人员管理启用人员、runtime config employees 与一线生产员工弹窗候选均为 8 人，集合 hash 完全一致；剩余 closeout 仅涉及仓库级提交/推送与任务产物清理边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；候选范围必须来自生产组长人员管理正式列表，接口缺失时阻塞。
- `是否从根因和长期维护角度解决`：是；运行配置员工改为当前生产组长启用的生产人员档案列表，弹窗和切换校验使用同一正式来源。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Frontend feature delivery：保持现有 API wrapper、状态所有权和用户可见行为，先记录 BDD，再执行 RED/GREEN/REGRESSION。
- MES 生产人员档案正式工重复关联门禁：候选查询与提交校验必须同范围；禁止让前端加载全系统用户后本地过滤，禁止默认空列表或兜底成功。
- 前端静态契约隔离门禁：如全量 `pnpm ts:check` 或既有大合同被无关历史问题阻塞，必须记录无关 blocker，并用本任务专用静态合同证明当前行为。

## Cleanup Keep

- doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/frontline-production-employee-popup-real-e2e.cjs
- doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/frontline-production-employee-popup-evidence.md
- doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/restart-frontline-employee-runtime.ps1
