# 20260802 DCC 文件分发与旧版回收真实 E2E

## Task Goal

在 `E:\IntRuoyi` 通过真实 Playwright 页面验证 DCC 文控“文件分发/旧版回收”链路：已生效受控文件可登记纸质/电子受控副本分发，接收人可签收/确认；升版后旧版 V1 进入需回收/可回收链路并可完成回收确认；新版 V2 可继续分发，旧版不可继续作为有效文件误用。

## Milestones

- [x] M1 规则与前置确认：读取 AGENTS、E2E、登录、前端、运行态、数据库、PowerShell 与 Playwright 技能规则，确认前后端入口、非 admin 账号、浏览器和任务数据前置。
- [x] M2 BDD 与验证边界：记录 Given/When/Then，限定只验证分发和旧版回收，不修复其它场景，不用 API-only、SQL 改状态或 admin 绕过。
- [x] M3 真实页面分发入口：通过真实 DCC/BPM 页面准备过任务自有 V1/V2 纸质分发记录，并按用户要求改查具备分发规则的类别。
- [x] M4 接收人签收：部分通过。后端恢复后，非 admin `wangsiyu` 在真实 V1 追溯详情页完成“确认纸质发放”，接收责任人 `panhaitao` 已记录；完整接收/回收链路仍因 V2 发布权限阻塞。
- [ ] M5 升版后旧版回收：阻塞。当前可分发规则类别链路无法把 V2 发布为 `ACTIVE`，V1 仍未进入可回收的 `SUPERSEDED` 当前链路，无法通过真实页面生成回收责任人与时间。
- [ ] M6 新旧版有效性验证：阻塞。此前旧类别链路曾证明 V2-only 受控浏览，但当前用户要求切换到有分发规则类别后，V2 未发布生效，不能把旧证据冒充当前链路 PASS。
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

- E2E BLOCKED：按用户要求改用具备分发规则的类别后，当前可用的 `906104 / 其他` 在租户 `芋道源码` 下有分发规则 `106` 和 `DISTRIBUTE` 角色规则；后端恢复后，真实页面已完成 V1 纸质发放登记，但 `APPROVE` 仍只授予 user ID `1`，非 admin `wangsiyu` 无法在真实页面提交 V2 发布申请。
- 只读扫描结论：租户 `122` 的 `900347 / Codex Local DCC Category` 存在非 admin `aoteman` 同时具备 `APPROVE / DISTRIBUTE / UPLOAD` 和分发规则，但该租户没有已发布的 DCC `PUBLISH / READY_TO_PUBLISH` 业务审批策略；全库分离发布人与分发人的组合也没有同时满足“发布策略 + 分发规则 + 非 admin APPROVE + 非 admin DISTRIBUTE”。
- 运行态：用户通知后端已启动后复查 `48081` health 为 `UP`，并完成 V1 分发动作；后续阻塞归因为权限/测试数据，不再是后端不可用。
- 影响：当前链路无法继续完成真实页面 V2 发布生效、旧版回收确认、回收记录 ID、回收责任人与回收时间；不能声明“分发 + 回收” PASS。
- 未执行绕过：未使用 admin 账号，未用 API-only/SQL 插入或修改分发、签收、回收、版本状态、权限或审批策略，也未修复无关 MES mapper。

## Cleanup Keep

- doc/tasks/20260802-dcc-distribution-recovery-e2e/dcc-distribution-recovery-e2e.cjs
- doc/tasks/20260802-dcc-distribution-recovery-e2e/paper-chain-result.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/paper-issue-recovery-final-result.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/controlled-browser-paper-v1-v2-probe.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/blocked-readonly-db-verification.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/tenant1-current-blocked-readonly-db-verification.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/candidate-permission-scan-20260802-210906.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/tenant1-post-v1-ack-readonly-db-verification.json
- doc/tasks/20260802-dcc-distribution-recovery-e2e/publish-blocked-after-backend-up.json
