# 20260802 DCC 文件分发与旧版回收真实 E2E

## Task Goal

在 `E:\IntRuoyi` 通过真实 Playwright 页面验证 DCC 文控“文件分发/旧版回收”链路：已生效受控文件可登记纸质/电子受控副本分发，接收人可签收/确认；升版后旧版 V1 进入需回收/可回收链路并可完成回收确认；新版 V2 可继续分发，旧版不可继续作为有效文件误用。

## Milestones

- [x] M1 规则与前置确认：读取 AGENTS、E2E、登录、前端、运行态、数据库、PowerShell 与 Playwright 技能规则，确认前后端入口、非 admin 账号、浏览器和任务数据前置。
- [x] M2 BDD 与验证边界：记录 Given/When/Then，限定只验证分发和旧版回收，不修复其它场景，不用 API-only、SQL 改状态或 admin 绕过。
- [x] M3 真实页面分发入口：通过真实 DCC/BPM 页面准备任务自有 V1->V2 纸质分发记录，并在正式追溯详情页看到 V2 “确认纸质发放”动作。
- [ ] M4 接收人签收：阻塞。当前类别缺少启用的 `DISTRIBUTE` 权限规则，非 admin `wangsiyu` 点击“确认纸质发放”后后端返回业务拒绝，无法登记 `panhaitao` 为纸质接收人。
- [ ] M5 升版后旧版回收：阻塞。V1 已是 `SUPERSEDED`，但因同一 `DISTRIBUTE` 权限缺口，旧版纸质分发无法先登记发放为 `ACKNOWLEDGED`，因此无法进入页面“确认回收”动作。
- [x] M6 新旧版有效性验证：真实受控浏览页面按文件编号只返回 V2 ACTIVE，V1 未作为当前有效文件展示。
- [x] M7 只读核验与报告：只读 DB/API 核验分发记录、版本状态、权限缺口并更新 `verification-report.md`。

## Expected Verification

- Playwright 真实页面证据覆盖登录、分发登记、分发记录展示、接收/签收、升版旧版回收、V2 继续分发、V1 不可有效分发。
- API/DB 仅用于最终只读核验，不插入、不改状态、不补数据。
- `verification-report.md` 记录文件 ID、V1/V2 状态、分发记录 ID、回收记录 ID、责任人、页面路径、阻塞项或最终 PASS。

## Current Status

blocked

## 经验门禁

- DCC 文控审批处理入口门禁：涉及升版发布时必须从真实 DCC/BPM 页面完成，禁止 viewer-only、API-only 或 SQL 改状态。
- Playwright 浏览器可执行文件门禁：若 Playwright 缓存缺失，优先使用本机 Chrome/Edge 显式路径并记录来源。
- Playwright 目标链路与外部资源异常归因门禁：目标 DCC/分发/回收链路错误必须阻塞；非目标外部资源异常需单独归因。
- Playwright 快照与 daemon 收尾门禁：artifact 仅写入当前任务目录，收尾前清理或脱敏任务自有快照/trace/截图。
- Element Plus 下拉选择门禁：选择部门/人员/岗位等必须按可见业务文本定位真实选项，不用隐藏值或数组下标替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本任务只做真实链路验证；若缺页面入口、权限、测试数据或运行态，记录 BLOCKED 和影响。
- `是否存在临时补丁或绕过`：否。

## Blocked Summary

- E2E BLOCKED：任务自有文件 `CODX-DCC-DIST-REC-DISTREC20260802173908` 已形成 V1 `SUPERSEDED` / V2 `ACTIVE` 和纸质分发记录，但类别 `过程检验规程`（categoryId `907233`）当前没有任何启用的 `DISTRIBUTE` 权限规则。
- 页面证据：非 admin `wangsiyu` 通过正式受控浏览追溯详情路径进入 V2，页面显示“分发状态”和“确认纸质发放”，点击后目标写接口返回业务错误 `1080000049 Current user cannot acknowledge this paper distribution`。
- 影响：无法通过真实页面完成纸质发放登记、接收责任人 `panhaitao` 落库、V1 旧版回收确认、回收责任人和回收时间生成；分发记录 `4323` / `4324` 均停留 `PENDING`。
- 已验证：真实受控浏览页面只返回 V2 `2054545668044070280` / `ACTIVE`，V1 `2054545668044070279` 不作为当前有效文件展示。
- 未执行绕过：未使用 admin 账号，未用 API-only/SQL 插入或修改分发、签收、回收、版本状态，也未补权限或改代码。

## Cleanup Keep

- doc/tasks/20260802-dcc-distribution-recovery-e2e/dcc-distribution-recovery-e2e.cjs
- doc/tasks/20260802-dcc-distribution-recovery-e2e/paper-chain-result.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/paper-issue-recovery-final-result.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/controlled-browser-paper-v1-v2-probe.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/blocked-readonly-db-verification.json
