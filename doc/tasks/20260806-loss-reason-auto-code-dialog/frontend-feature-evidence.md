# Feature

新增损耗原因弹窗在 create 模式删除截图红框内的手工维护字段：原因编码、启用状态和维护说明；保留原因名称输入、正式保存按钮和接口错误暴露。非目标范围：不调整生产组长工序配置权限、路线工序候选、已有损耗原因编辑表单或损耗原因列表展示。

## Acceptance

- 新增模式弹窗不渲染原因编码、启用状态、维护说明。
- 新增提交只校验损耗原因名称，不再校验或提交手工 `reasonCode`。
- 编辑模式仍可展示原因编码、启用状态、维护说明，避免破坏已有维护能力。
- 前端 API create 请求类型只要求 `routeProcessId` 和 `reasonName`。

## UI Entry Points

- 页面：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- 弹窗锚点：`data-loss-reason-edit-dialog`
- API wrapper：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`
- 目标静态合同：`IntRuoyiFronted/tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs`

## API Contracts And States

- Create payload：`{ routeProcessId, reasonName }`
- Update payload：仍保留 `{ reasonName, enabled, remark }`
- Loading/error：沿用现有 create/update API 调用和错误暴露，不新增吞异常或默认成功逻辑。

## BDD

- BDD: 新增损耗原因隐藏红框字段 -> Given 生产组长在工序配置行点击新增损耗原因, When 弹窗打开, Then 弹窗不展示原因编码、启用状态、维护说明字段，只要求填写原因名称。
- BDD: 新增损耗原因不提交手工编码 -> Given 用户填写损耗原因名称, When 点击保存损耗原因, Then 前端 create payload 不包含 `reasonCode`、`enabled`、`remark`，后端负责生成唯一编码。
- BDD: 编辑损耗原因保留维护字段 -> Given 用户编辑已有损耗原因, When 弹窗打开, Then 仍展示原因编码、启用状态、维护说明并走 update payload。

## TDD Evidence

- RED: `node tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs` -> FAIL, 旧实现新增弹窗仍要求/提交手工 `reasonCode`，且原因编码、启用状态、维护说明未按 create/edit 模式隔离。
- GREEN: `node tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs` -> PASS, 输出 `PASS: team leader loss reason create dialog hides manual fields and backend generates code`。
- GREEN: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS, 输出 `team-leader-process-config-unified-static PASS`。
- GREEN: `node tests/e2e/production-leader-function-tabs-static.spec.js` -> PASS, 输出 `PASS: production leader function tabs static contract`。

## Verification

- `pnpm ts:check` -> PASS，命令返回退出码 `0`。
- `git diff --check b9a752088^ b9a752088 -- <task-owned paths>` -> PASS，无空白错误输出。
- 响应式、无数据、权限和 loading 状态：本任务未改布局容器、权限判断、列表加载或错误处理，仅在 create 模式隐藏手工字段并收缩 create payload。
- 真实 Playwright 写入型 E2E：未执行；本任务使用最小静态合同覆盖截图字段删除、payload 和后端自动编号契约，不创建真实业务数据。

## Blockers

- 同模块 Maven 进程 `47148/49960` 正在运行其它任务 `yudao-module-mes` 测试，按 Maven 目标目录门禁未叠加后端 Maven；本任务后端行为由目标静态合同覆盖，标准 Maven 复验保留为空闲后续缺口。
