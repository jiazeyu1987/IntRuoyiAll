# 20260803 DCC 文件分发与旧版回收真实 E2E

## Task Goal

在 `E:\IntRuoyi` 对 DCC 文控“文件分发/旧版回收”进行一轮完整真实 Playwright E2E 验证：受控文件发布或升版后可登记纸质/电子受控副本分发；升版后旧版 V1 可触发并完成回收/确认，形成分发、签收/发放、回收追溯；新版 V2 可继续分发，旧版 V1 不再作为当前有效文件误用。

## Milestones

- [x] M1 规则与前置确认：读取 AGENTS、E2E、登录、前端、运行态、数据库、PowerShell、任务收尾与 Playwright 技能规则，确认本机前后端、浏览器、非 admin 账号和密码注入方式。
- [x] M2 BDD 与验证边界：记录 Given/When/Then，限定只做真实页面 E2E，API/DB 仅最终只读核验，不用 admin、API-only 或 SQL/API 改业务状态。
- [x] M3 V1/V2 文件链路：可用真实页面链路 `CODX-DCC-DIST-906104-DISTTENANT120260802195305` 已完成 V1 `SUPERSEDED` / V2 `ACTIVE`；当前新建候选 `DISTTENANT1202608030005` 仍因 `PRINT` 动作投影/权限缺口阻塞，未用于 PASS。
- [x] M4 分发登记与签收/发放：真实页面证据确认 V1/V2 纸质分发记录、接收人、发放人和发放时间；本轮重新打开详情页完成追溯复验。
- [x] M5 旧版回收与新版继续分发：真实页面证据确认 V1 已回收、V2 已确认发放且仍可继续回收/分发追溯。
- [x] M6 旧版不误用与只读核验：真实受控浏览只返回 V2 当前有效；最终只读 DB 核验分发、接收、回收和版本状态。
- [x] M7 报告与收尾：已输出 `verification-report.md`，记录文件 ID、V1/V2 状态、分发记录 ID、回收记录 ID、责任人、页面路径、PASS 证据和未用于 PASS 的阻塞候选。

## Expected Verification

- Playwright 真实页面操作覆盖登录、分发、签收/确认发放、升版、旧版回收、V2 继续分发和 V1 不误用。
- API/DB 只用于最终只读核验，不插入、不更新分发、接收、回收、审批或版本状态。
- `verification-report.md` 记录 PASS 或 E2E BLOCKED 的证据、影响和页面路径。

## Current Status

completed

## Verification Summary

- PASS chain: `CODX-DCC-DIST-906104-DISTTENANT120260802195305`，V1 `2054545668044070297` 为 `SUPERSEDED`，V2 `2054545668044070302` 为 `ACTIVE`，master 当前有效版本指向 V2。
- 分发规则来源按用户确认口径固定为 `文控权限 > 分发规则`；真实页面 `/dcc/controlled-file/categories?tab=distribution-rules` 显示类别 `DCC_OTHER_TEMPLATE_900250 / 其他` 的分发部门为 `质量体系部`，只读响应记录规则 ID `106`、部门 ID `253`。
- 分发与回收：V1 纸质分发记录 `4341` 已 `RECOVERED`，接收人 `panhaitao`，发放/回收责任人 `wangsiyu`；V2 纸质分发记录 `4344` 已 `ACKNOWLEDGED`，接收人 `panhaitao`，发放责任人 `wangsiyu`。
- 当前复验脚本 `dcc-rule-trace-current-verify.cjs` 通过真实页面打开分发规则页、V2 分发追溯页、V1 回收追溯页和受控浏览页，并用只读 DB 完成对账；证据为 `current-rule-trace-verification.json`。
- 未使用 admin，未通过 SQL/API 插入或更新分发、接收、回收、审批、发布或版本状态。

## Blocked Candidate Summary

- Initial blocker at `2026-08-03 00:31:33 +08:00`: backend `http://127.0.0.1:48081` refused connection during real Playwright continuation; this was later recovered.
- Current primary blocker after recovery: file `CODX-DCC-DIST-906104-DISTTENANT1202608030005`, V1 ID `2054545668044070310`, status `PENDING_MATRIX_APPROVAL`, active task `批准` assigned to `zhaomingyu / 424`; real approval detail shows no approval action button and alerts `受控打印动作投影缺失` / missing `PRINT` permission.
- Secondary blockers: tenant `芋道源码` existing paper V1/V2 candidate has category `907233` without `DISTRIBUTE`, so real paper acknowledgement returns `1080000049`; tenant `测试租户` candidate has V2 `READY_TO_PUBLISH` but real publish dialog reports no published `PUBLISH` business approval policy and returns `系统异常`.
- No current-run distribution, receipt/issue, recovery, or old-version non-misuse PASS was produced from this fresh candidate; it was not used as the final PASS chain.
- No admin account was used and no SQL/API inserted or updated distribution, receipt, recovery, approval, publish, or version status.

## Resume Update

- Resumed at `2026-08-03 00:56:04 +08:00` after user confirmed backend should be started/continued.
- Backend `48081` is healthy and owned by Java process `48940` running `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-170535.jar`; tenant lookup for `芋道源码` returned tenant ID `1`.
- Updated only the task-owned Playwright script so upload approval continuation reads the current active upload/BPM assignee from the real runtime database as read-only evidence, then performs each approval through the real page.

## 经验门禁

- DCC 文控审批处理入口门禁：升版/发布必须从真实 DCC/BPM 页面完成，禁止 viewer-only、API-only 或 SQL 改状态。
- DCC 分发与旧版回收门禁：分发/发放/回收必须通过真实详情页完成，先确认类别 `DISTRIBUTE` 和发布 `APPROVE` 前置，升版后再回收 V1，最终证明受控浏览只返回 V2 当前有效。
- Playwright 浏览器可执行文件门禁：若 Playwright 缓存缺失，优先使用本机 Chrome/Edge 显式路径并记录来源。
- Playwright 目标链路与外部资源异常归因门禁：目标 DCC/分发/回收链路错误必须阻塞；非目标外部资源异常需单独归因。
- Element Plus 下拉选择门禁：选择部门/岗位/人员/使用地点必须按可见业务文本定位真实选项，不用隐藏值或数组下标替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本任务是验证任务，严格使用真实页面和最终只读核验。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260803-dcc-distribution-recovery-e2e/current-rule-trace-verification.json
- doc/tasks/20260803-dcc-distribution-recovery-e2e/dcc-rule-trace-current-verify.cjs
- doc/tasks/20260803-dcc-distribution-recovery-e2e/dcc-paper-chain-prepare-e2e.cjs
- doc/tasks/20260803-dcc-distribution-recovery-e2e/dcc-distribution-recovery-e2e.cjs
- doc/tasks/20260803-dcc-distribution-recovery-e2e/file-type-probe.json
- doc/tasks/20260803-dcc-distribution-recovery-e2e/paper-chain-full-result.json
- doc/tasks/20260803-dcc-distribution-recovery-e2e/paper-chain-testtenant-result.json
- doc/tasks/20260803-dcc-distribution-recovery-e2e/paper-issue-recovery-final-result.json
- doc/tasks/20260803-dcc-distribution-recovery-e2e/paper-issue-recovery-wangsiyu-blocked.json
- doc/tasks/20260803-dcc-distribution-recovery-e2e/project-option-probe.json
- doc/tasks/20260803-dcc-distribution-recovery-e2e/screenshots/
