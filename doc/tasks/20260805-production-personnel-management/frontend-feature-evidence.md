# Frontend Feature Evidence

## Feature

生产组长工作台新增独立“生产人员档案”tab：列表只展示已关联当前生产组长的员工，支持正式工姓名远程搜索关联、临时工手动录入显示名和电子签名密码、启禁用、修改显示名、重置临时工签名密码，以及操作追溯列表。

## Acceptance

- 列表范围：`UnifiedListTemplate` 使用 `table-key="mes.processPool.teamLeader.productionPersonnel"`，数据来自 scoped 后端列表接口。
- 正式工新增：`data-team-leader-formal-employee-select` 使用 `searchFormalEmployeeCandidatesForSelect` 远程方法，不调用 `/system/user/page`。
- 临时工新增：`data-team-leader-temporary-employee-form` 包含 `temporaryEmployeeForm.displayName` 和 `temporaryEmployeeForm.signaturePassword`。
- 生产填写选择：`frontlineDeviceEmployeeContext.ts` 使用 `employee.systemUserId || employee.employeeProfileId`，并优先显示 `employee.displayName || employee.employeeName`。
- 追溯：`data-team-leader-personnel-audit-list` 展示审计记录。

## BDD

- BDD: 生产人员列表 -> Given 当前登录用户是生产组长；When 打开生产人员档案 tab；Then 只展示当前组长关联员工。
- BDD: 正式工搜索 -> Given 输入姓名关键字；When 下拉远程搜索；Then 只从后端 scoped 候选中选择正式工。
- BDD: 临时工新增 -> Given 输入显示名和签名密码；When 提交；Then 创建临时生产人员档案且不创建登录账号。
- BDD: 员工卡片候选 -> Given 临时工没有系统用户 ID；When 进入生产填写；Then 使用档案 ID 作为候选 ID 并显示档案显示名。

## RED / GREEN

- RED: `node tests/e2e/production-personnel-management-static.spec.cjs` -> FAIL, `employee management tab must use the standard UnifiedListTemplate.`
- GREEN: `node tests/e2e/production-personnel-management-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:production-personnel-management:real:check` -> PASS。
- GREEN: `pnpm e2e:production-personnel-management:real` -> PASS，真实页面覆盖正式工关联、临时工新增、重复名拒绝、绑定、密码重置、禁用和审计。
- GREEN: pnpm install --frozen-lockfile --offline --ignore-scripts --reporter append-only -> PASS，依赖链接恢复且未修改 lockfile。
- GREEN: pnpm ts:check -> PASS。

## Verification

- 前端静态合同已覆盖标准列表、正式工远程搜索、临时工表单、审计列表、禁止全系统用户列表、生产填写候选显示名快照。
- `pnpm ts:check` -> PASS。
- `pnpm e2e:production-personnel-management:real` -> PASS，使用 worktree slot 1 的 `8082/48082` 成对运行态完成真实页面验收。

## Blockers

- 无当前 blocker。
