# 20260802 DCC 升版修订发布真实 E2E

## Task Goal

通过真实 Playwright 前端操作验证 DCC 文控“升版/修订发布”完整链路：任务自有或安全测试的 ACTIVE V1.0 受控文件升版为 V2.0，上传新版文件、提交审批、多人非 admin 审核/签名、生效发布，并最终证明 V1.0 失效、V2.0 成为 master 当前版本且受控浏览默认落位到 V2.0。

## Milestones

- [x] M1 规则与运行前置确认：读取 AGENTS、E2E、登录、前端、收尾、本机运行、数据库和编码规则，确认前后端入口、Playwright 浏览器、MinIO、非 admin 账号环境变量前置。
- [x] M2 BDD 与可执行 E2E 入口确认：记录 Given/When/Then，并确认真实页面脚本、输入文件、输出目录和敏感信息策略。
- [x] M3 真实前端链路执行：登录上传/修订发起人账号，创建任务自有 ACTIVE V1.0，页面发起升版并上传 V2.0，提交审批。
- [x] M4 多节点审批签名：使用多个非 admin 审批/签名账号，在真实审批页面逐节点完成 DCC 审批和发布 BPM 审批。
- [x] M5 受控浏览与版本历史验证：页面验证 V2.0 当前有效、V1.0 旧版失效，受控浏览默认打开 V2.0，版本信息弹窗可追溯 V1.0/V2.0，提交备注展示变更原因。
- [x] M6 最终只读核验与报告：只读 DB 核验 master 指针、版本状态、审批任务、签名证据、published/stamped 文件 ID，输出 verification-report.md。
- [ ] M7 收尾清理：已执行 task-closeout-cleanup preview/apply，仅删除失败重试 `chain-result.json`，保留正式 E2E 证据；Git 提交/推送收尾被当前工作区大量非本任务改动和 `ahead 1` 状态阻塞，未混入提交。

## Expected Verification

- `node --check doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs`
- 真实 Playwright E2E 运行通过，证据 JSON 与截图位于本任务目录。
- 只读 DB 核验 V1.0 `SUPERSEDED`、V2.0 `ACTIVE`、master 当前版本指向 V2.0。
- 审批任务和电子签名证据显示多个非 admin 审批/签名账号真实完成。
- 受控浏览默认只展示/打开 V2.0 当前有效版，published/stamped 文件 ID 与 V2.0 一致，版本信息可追溯 V1.0/V2.0，变更原因在详情提交备注可见。
- 清理仅影响本任务自有临时 artifacts，不直接 SQL/API 修改文件状态、master 指针或审批状态。

## Final Verification Summary

- Result: `PASS`.
- File number: `CODX-DCC-REV-FULL-20260802-20260802091213`.
- V1: `2054545668044070271`, `V1.0`, `SUPERSEDED`.
- V2: `2054545668044070272`, `V2.0`, `ACTIVE`.
- Master: `2054545668044062882`, current active controlled file `2054545668044070272`.
- Publish instance: `437`, status `EFFECTIVE`.
- Final evidence: `doc/tasks/20260802-dcc-revision-publish-real-e2e/e2e-result.json` and `verification-report.md`.

## Applicable Gates

- DCC 文控审批处理入口门禁：审批必须从真实 DCC/BPM 页面进入非只读处理态，禁止 viewer-only、API-only 或 SQL 修改状态；发布 BPM 节点如需 `APPROVE_USER_SELECT` 必须页面选择下一审批人。
- Playwright 浏览器可执行文件门禁：若 Playwright 缓存缺失，优先使用本机 Chrome/Edge 显式路径并记录来源。
- Element Plus 上传控件门禁：`setInputFiles` 后必须看到上传列表或目标上传请求，不能只凭 `input.files` 判定上传成功。
- Playwright artifact 收尾门禁：不得提交包含登录态、密码、token 或原始页面快照的 `.playwright-cli`、trace、截图、视频；仅清理本任务归属 artifacts。
- 本机运行态 URL 门禁：本轮使用 `int_main` 主运行态 `8081/48081`，必须确认端口归属和后端 health，不静默换端口。
- 数据库只读核验门禁：最终只读核验前先确认 schema/表来源，不执行状态修复 SQL，不记录连接密钥。

## Current Status

ready_for_closeout

## Closeout Evidence

- `task-closeout-cleanup --mode preview` -> ready, blocked `<none>`, delete only `chain-result.json`.
- `task-closeout-cleanup --mode apply` -> applied, deleted only `chain-result.json`.
- Project experience consolidation updated existing rule docs: `docs/e2e-rules.md` and `docs/task-closeout-rules.md`.
- Git closeout not performed: `git status --short --branch` shows `int_main...origin/int_main [ahead 1]` plus many unrelated modified/untracked files outside this task. Per task ownership rules, these were not staged, committed, reverted, or pushed.

## Cleanup Keep

- doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs
- doc/tasks/20260802-dcc-revision-publish-real-e2e/e2e-result.json
- doc/tasks/20260802-dcc-revision-publish-real-e2e/browser-current-v2.png
- doc/tasks/20260802-dcc-revision-publish-real-e2e/detail-version-history.png
- doc/tasks/20260802-dcc-revision-publish-real-e2e/stamped-approval-sample.pdf

## Cleanup Candidates

- doc/tasks/20260802-dcc-revision-publish-real-e2e/chain-result.json

## Residual Task-Owned Data

- Final PASS data retained for audit traceability: `CODX-DCC-REV-FULL-20260802-20260802091213`.
- Failed retry data retained without SQL/API cleanup due task constraints: `CODX-DCC-REV-FULL-20260802-20260802091853`, controlled file `2054545668044070274`, status `PENDING_MATRIX_REVIEW`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本任务是独立真实 E2E 验证，不修改生产代码；验证脚本仅修正响应匹配和真实 UI 入口断言。
- `是否存在临时补丁或绕过`：否。
