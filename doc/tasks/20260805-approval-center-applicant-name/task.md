# 审批中心申请人显示姓名

## Task Goal

审批中心“申请人”列优先显示用户姓名，不再在已有姓名数据时显示 `用户 #<id>`。

## Milestones

- [x] M1：保存当前共享分支既有脏改动基线，确认申请人姓名字段来源。
- [x] M2：新增/更新聚焦静态合同并记录 RED。
- [x] M3：实现申请人姓名展示并记录 GREEN。
- [ ] M4：完成相邻回归、真实页面只读 E2E、提交、推送与收尾。

## Expected Verification

- 静态合同证明申请人列优先使用正式姓名字段，只有姓名缺失时才显示 `用户 #<id>`，再缺失时显示 `--`。
- 审批中心待办、已办、我发起的、抄送列表保持独立“申请人”列与现有列顺序。
- 不改审批中心 API、权限、分页、审批动作或 DCC 业务摘要逻辑。
- 运行聚焦静态合同、审批中心标准列表相邻合同、`pnpm ts:check`。
- 运行真实页面只读 E2E，确认“申请人”列展示姓名且目标写请求为 0。

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：当前 UI 展示修复必须先用聚焦静态合同 RED，再实现 GREEN。
- `docs/frontend-development.md#前端列表跨账号默认列布局统一门禁`：不改变本次已升级的审批中心 table key，避免再次扰动用户列配置。
- `docs/e2e-rules.md#playwright-目标链路与外部资源异常归因门禁`：真实页面只读 E2E 必须区分目标审批中心链路失败、页面错误和非目标资源异常。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：共享分支存在并行任务改动时，只能选择性提交本任务文件。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；优先使用正式姓名字段，保留现有 ID 可见兜底。
- 是否存在临时补丁或绕过：否。

## Current Status

blocked

申请人姓名展示实现、后端聚焦单测、前端静态合同、相邻静态回归和真实页面只读 E2E 已通过。收尾阻塞于共享工作区无关 `pnpm ts:check` 失败：`IntRuoyiFronted/src/components/UnifiedListTemplate/index.vue(339,8)` 的 `quick-filter-query | multi-filter-query` 与 `column-reset` emit 类型不匹配；该文件当前已有并行任务改动，非本任务变更范围，按提交门禁暂不提交/推送。

## Cleanup Keep

- doc/tasks/20260805-approval-center-applicant-name/approval-center-applicant-name-real.e2e.cjs
- doc/tasks/20260805-approval-center-applicant-name/e2e-artifacts/approval-center-applicant-name-result.json
