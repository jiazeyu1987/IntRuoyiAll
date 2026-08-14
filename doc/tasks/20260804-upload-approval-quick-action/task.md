# 上传审批列表快速审批

## Task Goal

在审批中心待办列表的“上传审批”行操作区增加“审批”按钮，点击后可直接打开现有审批确认弹窗并完成审批；保留现有“处理/打开”进入详情审批入口，不改变详情审批能力。

## Milestones

- [x] M1：确认审批中心列表、DCC 上传审批识别条件、现有审核弹窗与 API 契约。
- [x] M2：以专用静态契约完成 BDD 与 RED，锁定“审批”按钮作用域及详情入口保留要求。
- [x] M3：实现最小行内快速审批入口并完成 GREEN、相邻回归和 DCC 后端定向单测。
- [x] M4：通过真实 Playwright 页面路径验证按钮可见、弹窗可用且详情入口仍存在。
- [ ] M5：完成证据归档、任务清理、提交和推送。当前剩余：选择性提交/推送仍未执行，且需避开当前共享工作区无关并行改动。

## Expected Verification

- `node tests/e2e/approval-center-upload-quick-review-static.spec.js`
- `node tests/e2e/approval-center-review-action-static.spec.js`
- `node yudao-module-dcc/src/test/js/dcc-approval-task-adapter-quick-review-static.spec.cjs`
- `mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm ts:check`
- Playwright 真实审批中心待办页面验证：目标 DCC 上传审批行同时显示“审批”和现有详情入口；点击“审批”打开现有审批确认弹窗。
- cleanup 前已执行 frontend/backend evidence validator，并将 PASS 结论归档到 `execution-log.md` 与 `verification-report.md`。

## Applicable Gates

- DCC 文控审批处理入口门禁：必须从真实审批中心 DCC 行验证当前审批动作，不能用 API-only、只读 viewer 或 BPM 原生行代替 DCC 上传审批。
- DCC 文控批准资料门禁：最终文控批准需要盖章 PDF、存入路径及下发范围，不得用空值或快速审批替代模块专属资料。
- 前端静态契约隔离门禁：使用任务专用最小静态契约证明当前需求 RED/GREEN，不修改无关大契约掩盖历史失败。
- 严格无 fallback：不新增兼容分支、默认成功或错误吞噬；不支持快速审批的任务继续按现有详情入口处理。
- 现有行为保护：新增行内入口不得删除或改写现有“处理/打开”详情审批路径。

## Current Status

ready_for_closeout

- M1-M4 已完成：列表快速审批入口、DCC provider 快速审核委托、最终文控批准保留模块处理均已实现，并通过定向契约、JUnit、`pnpm ts:check` 和真实 Playwright 页面验证。
- 运行态证据：当前 48081 已加载基于 `backend-runtime-control-20260805-qa-regulation-dcc-status-20260805-003532.jar` 生成的最小热补丁 Jar `backend-runtime-control-20260805-upload-approval-quick-action-hotpatch.jar`；仅替换内嵌 DCC 模块的 `DccApprovalTaskAdapter.class`，保留并行任务其它模块内容，热补丁 Jar SHA256 为 `A8A109A10F57A2B373BA14D0F36E9E5FB3C01799DAF54525E36AD0948B545020`。
- M5 部分完成：`task-closeout-cleanup` preview/apply 已通过并清理任务临时文件；选择性提交和推送仍待处理，需避免混入当前共享工作区大量无关并行改动。
- Cleanup evidence：`task_closeout.py --task-id 20260804-upload-approval-quick-action --mode preview/apply` 均通过，保留 `task.md`、`execution-log.md`、`verification-report.md` 和最终 E2E 脚本/结果/截图，删除失败截图、临时 class-inspect jar、已归档 frontend/backend evidence。

## Cleanup Keep

- doc/tasks/20260804-upload-approval-quick-action/artifacts/approval-center-upload-quick-review-real.e2e.cjs
- doc/tasks/20260804-upload-approval-quick-action/artifacts/approval-center-upload-quick-review-real-result.json
- doc/tasks/20260804-upload-approval-quick-action/artifacts/approval-center-upload-quick-review-real.png

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；复用统一审批中心现有审核弹窗和正式提交 API，仅补齐缺失的列表入口。
- 是否存在临时补丁或绕过：否；生产代码未引入临时补丁，运行态热补丁仅用于在共享 48081 上保留并行 Jar 内容并加载本任务已验证的 DCC class。
