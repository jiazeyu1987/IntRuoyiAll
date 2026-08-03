# 20260803 DCC 文件分发与旧版回收真实 E2E

## Task Goal

在 `E:\IntRuoyi` 对 DCC 文控“文件分发/旧版回收”进行一轮完整真实 Playwright E2E 验证：受控文件发布或升版后可登记纸质/电子受控副本分发；升版后旧版 V1 可触发并完成回收/确认，形成分发、签收/发放、回收追溯；新版 V2 可继续分发，旧版 V1 不再作为当前有效文件误用。

## Milestones

- [x] M1 规则与前置确认：读取 AGENTS、E2E、登录、前端、运行态、数据库、PowerShell、任务收尾与 Playwright 技能规则，确认本机前后端、浏览器、非 admin 账号和密码注入方式。
- [x] M2 BDD 与验证边界：记录 Given/When/Then，限定只做真实页面 E2E，API/DB 仅最终只读核验，不用 admin、API-only 或 SQL/API 改业务状态。
- [ ] M3 任务自有 V1/V2 文件链路：已通过真实页面创建任务自有 V1 并完成部分审批；后端恢复后继续验证，但因当前类别缺 `PRINT` 动作投影/权限导致 zhaomingyu 真实页面无审批按钮，未达到 V1 `ACTIVE` 和 V2 `ACTIVE`。
- [ ] M4 分发登记与签收/发放：通过真实页面登记并确认受控副本分发，记录接收/发放责任人、份数、用途和页面路径。
- [ ] M5 旧版回收与新版继续分发：V2 生效后通过真实页面完成 V1 回收/确认回收，并验证 V2 仍可继续分发。
- [ ] M6 旧版不误用与只读核验：验证受控浏览只返回 V2 当前有效；只读 API/DB 核验分发记录、签收/发放记录、回收记录和版本状态。
- [x] M7 报告与收尾：已输出 `verification-report.md`，记录文件 ID、V1/V2 状态、分发记录 ID、回收记录 ID、责任人、页面路径和 BLOCKED 前置缺口。

## Expected Verification

- Playwright 真实页面操作覆盖登录、分发、签收/确认发放、升版、旧版回收、V2 继续分发和 V1 不误用。
- API/DB 只用于最终只读核验，不插入、不更新分发、接收、回收、审批或版本状态。
- `verification-report.md` 记录 PASS 或 E2E BLOCKED 的证据、影响和页面路径。

## Current Status

blocked

## Blocked Summary

- Initial blocker at `2026-08-03 00:31:33 +08:00`: backend `http://127.0.0.1:48081` refused connection during real Playwright continuation; this was later recovered.
- Current primary blocker after recovery: file `CODX-DCC-DIST-906104-DISTTENANT1202608030005`, V1 ID `2054545668044070310`, status `PENDING_MATRIX_APPROVAL`, active task `批准` assigned to `zhaomingyu / 424`; real approval detail shows no approval action button and alerts `受控打印动作投影缺失` / missing `PRINT` permission.
- Secondary blockers: tenant `芋道源码` existing paper V1/V2 candidate has category `907233` without `DISTRIBUTE`, so real paper acknowledgement returns `1080000049`; tenant `测试租户` candidate has V2 `READY_TO_PUBLISH` but real publish dialog reports no published `PUBLISH` business approval policy and returns `系统异常`.
- No current-run distribution, receipt/issue, recovery, or old-version non-misuse PASS was produced.
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
