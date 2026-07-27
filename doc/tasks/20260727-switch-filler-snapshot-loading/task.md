# 20260727 Switch Filler Snapshot Loading

## Task Goal

修复批处理表单中“切换填写人”弹窗每次打开重新加载全量批次详情导致耗时过长的问题，改为使用批次执行创建后固定的填写人快照生成候选列表。

## Milestones

- [x] 建立任务证据并记录快照加载 BDD/TDD 验证要求
- [x] 定位“切换填写人”重复加载全量批次详情的根因
- [x] 增加最小回归测试，先复现失败再修复
- [x] 实施后端执行详情快照和前端快照读取修复并运行定向验证
- [x] 完成后端模块编译复验与收尾状态更新

## Expected Verification

- 定向静态测试先 RED 后 GREEN，覆盖“切换填写人使用执行详情快照且不再调用批次详情接口”的行为。
- 受影响前后端代码通过相关定向验证。
- 若真实页面 E2E 前置条件不足，记录缺失前置条件和影响，不用 API-only 或 mock 代替真实路径。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在批次执行创建后固定的任务/填写人快照处提供轻量读取，避免每次切换重算全量详情。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### eDHR 详情回填门禁

- Trigger: eDHR、批次详情、动态表单、损耗单、工艺路线绑定、填写人、`fillableUsers`、`routeBindingId`、配置页有值但详情接口为空。
- Preflight check: 先同时核对配置接口/表中的来源字段、执行任务快照字段、详情接口组装链路和既有优先级，不得只改前端显示文案。
- Blocker: 若详情任务没有可追溯的绑定 ID、快照字段或正式规则来源，必须阻塞并补齐后端数据链路；不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- Verification: 新增回归测试覆盖执行详情可提供填写人快照，并确认前端弹窗不再触发全量批次详情加载。
- Forbidden action: 禁止前端把 `未配置` 改成配置页名称、禁止把角色/部门 ID 当用户 ID、禁止用空列表兜底掩盖缺失来源。
- Evidence: `docs/backend-development.md#edhr-详情回填门禁`。

### 切换填写人快照读取边界

- Trigger: eDHR 批次执行填写页、“切换填写人”、协助填写人、`assistSwitchTasks`、`candidateUserSnapshot`、弹窗打开耗时过长。
- Preflight check: 批次执行创建后填写人固定时，切换候选必须来自执行详情返回的任务/填写人快照，不在弹窗打开时重新拉取或重算全量批次详情；传统批记录打开链路必须按 `batchExecutionId + taskId` 隔离 active 执行记录。
- Blocker: 执行详情缺少可追溯任务快照、活动工作任务缺少 `candidateUserSnapshot`、或 active 执行记录未按批次任务隔离时，必须补齐后端详情链路；不得从当前登录人、角色、部门或空列表推断候选填写人。
- Verification: 运行快照静态合同、前端 ESLint/`pnpm ts:check` 与 MES reactor compile。
- Forbidden action: 禁止在切换填写人弹窗打开时调用全量 `getEdhrBatchExecution` 作为性能问题的替代方案；禁止用前端缓存、空列表兜底或吞异常掩盖缺失快照。
- Evidence: `docs/backend-development.md#切换填写人快照读取边界`。
