# 20260802 DCC 文件分发与旧版回收真实 E2E

## Task Goal

在 `E:\IntRuoyi` 通过真实 Playwright 页面验证 DCC 文控“文件分发/旧版回收”链路：已生效受控文件可登记纸质受控副本分发，接收责任人被记录；升版后旧版 V1 进入旧版回收链路并可完成回收确认；新版 V2 可继续分发，旧版 V1 不再作为当前有效文件误用。

## Milestones

- [x] M1 规则与前置确认：读取 AGENTS、E2E、登录、前端、运行态、数据库、PowerShell 与 Playwright 技能规则，确认前后端入口、非 admin 账号、浏览器和任务数据前置。
- [x] M2 BDD 与验证边界：记录 Given/When/Then，限定只验证分发和旧版回收，不修复其它场景，不用 API-only、SQL 改业务状态或 admin 绕过。
- [x] M3 真实页面文件链路：使用任务自有文件号 `CODX-DCC-DIST-906104-DISTTENANT120260802195305` 完成 V1 上传、审批、培训、发布；V2 上传、审批、发布申请、发布审批、培训和正式下发。
- [x] M4 分发登记与接收责任：通过真实详情页对 V1/V2 纸质分发执行“确认纸质发放”，接收人 `panhaitao` 被记录，发放责任人为 `wangsiyu`。
- [x] M5 升版后旧版回收：V2 发布为 `ACTIVE` 后，V1 变为 `SUPERSEDED`，通过真实详情页完成 V1 “确认回收”。
- [x] M6 新旧版有效性验证：受控浏览按文件号只返回 V2 当前有效版本，V1 不再作为当前有效文件显示。
- [x] M7 只读核验与报告：只读 DB/API 核验分发记录、回收记录、责任人、版本状态、master 当前指针，并更新 `verification-report.md`。

## Expected Verification

- Playwright 真实页面证据覆盖登录、分发登记、分发记录展示、接收责任、升版旧版回收、V2 继续分发、V1 不可有效分发。
- API/DB 仅用于最终只读核验，不插入、不改业务分发/回收/版本/审批状态。
- `verification-report.md` 记录文件 ID、V1/V2 状态、分发记录 ID、回收记录 ID、责任人、页面路径和最终 PASS 证据。

## Current Status

completed

## 经验门禁

- DCC 文控审批处理入口门禁：涉及升版发布时必须从真实 DCC/BPM 页面完成，禁止 viewer-only、API-only 或 SQL 改状态。
- DCC 分发与旧版回收门禁：分发/发放/回收必须通过真实详情页完成，先确认类别 `DISTRIBUTE` 和发布 `APPROVE` 前置，升版后再回收 V1，最终证明受控浏览只返回 V2 当前有效。
- Playwright 浏览器可执行文件门禁：若 Playwright 缓存缺失，优先使用本机 Chrome/Edge 显式路径并记录来源。
- Playwright 目标链路与外部资源异常归因门禁：目标 DCC/分发/回收链路错误必须阻塞；非目标外部资源异常需单独归因。
- Playwright 快照与 daemon 收尾门禁：artifact 仅写入当前任务目录，收尾前清理或脱敏任务自有快照/trace/截图。
- Element Plus 下拉选择门禁：选择部门/人员/岗位等必须按可见业务文本定位真实选项，不用隐藏值或数组下标替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本任务只做真实链路验证；用户授权后补齐 `wangsiyu` 已持有 E2E 分发角色的类别 `APPROVE` 前置，未改业务状态。
- `是否存在临时补丁或绕过`：否。权限补齐为用户明确授权的测试前置；业务分发、签收/发放、回收、发布和版本状态均通过真实页面完成。

## Final PASS Summary

- Permission prerequisite: real page probe confirmed `wangsiyu` can see `文控权限 / 审阅矩阵`; existing role `dcc_distribute_e2e` / `910431` already assigned to `wangsiyu` / `910250`; task-authorized category `906104` `APPROVE` rule ID `2624` added only as permission prerequisite.
- File chain: V1 ID `2054545668044070297` final `SUPERSEDED`; V2 ID `2054545668044070302` final `ACTIVE`; master `2054545668044062904` current active points to V2.
- Distribution: V1 distribution `4341` and V2 distribution `4344` were handled through real traceability detail pages by non-admin `wangsiyu`.
- Recovery: V1 distribution `4341` final `RECOVERED`; recoveredBy `wangsiyu`; recoveredAt `2026-08-02 23:30:08`.
- Non-misuse: controlled browser query returned only V2 `ACTIVE`; V1 was not visible as current effective file.

## Evidence Files

- `verification-report.md`
- `execution-log.md`
- `permission-setup-wangsiyu-approve-role.json`
- `paper-chain-tenant1-training-resume.json`
- `paper-issue-recovery-final-result.json`
- `final-pass-readonly-db-verification.json`
