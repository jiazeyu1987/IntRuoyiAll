# 20260802 DCC 原版发布当前验证 Execution Log

## User Intent

- 只验证 DCC 文控“原版发布”主场景。
- 不使用 admin，不使用 API-only/SQL 改状态，不 mock 上传成功。
- MinIO blocker 解除后恢复后台运行并继续完成原版上传、四级审批/签名、发布生效、受控浏览验证。

## BDD

- BDD: DCC 原版发布完整链路 -> Given 本机前端、后端、MinIO 均可用且密码仅通过环境变量注入，When 非 admin 上传人通过真实 DCC 上传页创建任务自有文件并由四级非 admin 审批/签名账号逐节点处理，Then V1.0 原版文件应 `ACTIVE`，master 当前有效版本指向该 V1.0，受控浏览可见并可打开当前有效版 viewer。
- BDD: DCC 签核追溯页面可见 -> Given 任务自有原版文件已通过真实页面完成四级审批/签名并发布生效，When 非 admin DCC 查看账号从受控浏览文件编号进入详情页并打开签核追溯，Then 页面应可见上传人、审批/签名人、签名时间、签名方式、证据状态、审批意见、hash、盖章/发布文件 ID，并可导出追溯记录。

## Preflight

- 已读取 `AGENTS.md`、`docs/local-runtime.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/database-rules.md`、`docs/worktree-restrictions.md`、`docs/experience-index.md` 和 Playwright skill。
- MinIO/object storage 前置已在独立任务 `doc/tasks/20260802-local-minio-object-storage-e2e-preflight/` 验证通过。

## RED / GREEN

- RED: 原始上传 E2E -> FAIL，`upload-preview` 连接 `127.0.0.1:9000` 被拒绝；未产生业务文件状态。
- GREEN: MinIO 恢复前置 -> PASS，`http://127.0.0.1:9000/minio/health/ready` 返回 HTTP `200`，upload-preview 真实页面前置验证 `PASS`。
- GREEN: runtime precheck -> PASS，前端 `http://127.0.0.1:8081/` 返回 HTTP `200`，后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`，MinIO ready HTTP `200`。
- GREEN: final resume script syntax -> PASS，`node --check doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-final-resume.cjs`。
- GREEN: final Playwright resume -> PASS，非 admin `wangsiyu` 从真实受控浏览页搜索 `CODX-DCC-ORIG-20260802101521`，打开当前有效版 viewer。
- GREEN: traceability script syntax -> PASS，`node --check doc/tasks/20260802-dcc-original-release-e2e-current/traceability-entry-real-check.cjs`。
- GREEN: real Playwright traceability -> PASS，`traceability-entry-real-check-result.json` 记录 `status=PASS`、`browserTraceabilityFieldPass=true`、`operationLogResponseCode=0`、`dccWriteRequests=0`。
- GREEN: page-visible trace fields -> PASS，`签核追溯` 区显示上传人 `彭云凤 (pengyunfeng)`、四级审批/签名人 `赵海辰/赵杰/赵明玉/王思雨`、签名时间、`PASSWORD`、`已校验`、证据 hash 和文件 ID `9198354916366`。
- GREEN: lifecycle approval comments -> PASS，生命周期区显示四级审批意见 `E2E V1.0 ... 同意 20260802101521` 与责任人。
- GREEN: traceability export -> PASS，导出 `signature-trace-export-20260802102108-trace-fields-final4.csv`，字段包含角色、上传人/四级审批人、签名时间、签名方式、证据状态、文件哈希、盖章文件，缺失 token 为空。
- GREEN: readonly DB final state -> PASS，文件 `2054545668044070287` 为 `V1.0` / `NEW` / `ACTIVE`，master `2054545668044062896` 指向该文件。
- GREEN: four-level approval/signature evidence -> PASS，审批完成任务数 `4`，有效签名数 `4`，`passwordVerified=1`。
- GREEN: target-link hygiene -> PASS，最终 `e2e-result.json` 记录 `targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- GREEN: secret hygiene -> PASS，任务目录扫描未发现密码明文。

## Final Evidence

- `e2e-result.json`: `status=PASS`，`runId=20260802101521-final-resume`。
- `traceability-entry-real-check-result.json`: `status=PASS`，`runId=20260802102108-trace-fields-final4`。
- 受控浏览路径：`/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-ORIG-20260802101521&pageNo=1&pageSize=20`。
- Viewer 路径：`/dcc/controlled-file/detail/2054545668044070287?viewer=1&from=browser...`。
- 签核追溯路径：`/dcc/controlled-file/detail/2054545668044070287?traceability=1&from=browser...`。
- Published/stamped file ID：`9198354916366` / `9198354916366`。
- Traceability export：`signature-trace-export-20260802102108-trace-fields-final4.csv`。

## Blockers

- RESOLVED: MinIO/object storage `127.0.0.1:9000` blocker 已解除。
- BLOCKED: Git closeout 未执行；当前 `int_main` ahead `origin/int_main` 且工作区存在大量非本任务改动，不能安全提交/推送本任务收尾。

## Viewer Linkage Fix

- BDD: 受控浏览 viewer 发布链路信息可见 -> Given 非 admin 授权账号从受控浏览打开当前有效版 viewer，When 页面加载文件预览和右侧详情，Then viewer 必须展示受控浏览入口、最终目录路径、publishedFileId、stampedFileId 和 master 当前生效版本，且这些信息来自 `fileDetail` 与目录路径计算结果。
- RED: `node tests/e2e/dcc-controlled-browser-viewer-linkage-static.spec.js` -> FAIL，预期原因：`viewerMode` 模板未渲染 `data-testid="dcc-detail-controlled-browser-linkage"`，发布/盖章文件链路信息只存在于非 viewer 详情路径。
- Fix: 在 `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` 的 viewer 右侧详情栏渲染受控浏览链路卡片，复用 `controlledBrowserDirectoryPath`、`fileDetail?.publishedFileId`、`fileDetail?.stampedFileId`、`fileDetail?.currentActiveVersionNo` 与 `openControlledBrowserLocation`，未修改权限、后端状态或测试数据。
- GREEN: `node tests/e2e/dcc-controlled-browser-viewer-linkage-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-governance-ux-static.spec.js` -> PASS。
- GREEN: `node --check doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs` -> PASS。
- GREEN: `node doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs` -> PASS，密码仅通过环境变量注入且未写入报告；结果文件 `e2e-result-after-linkage-fix.json` 记录授权账号 `wangsiyu` `browserTotal=1`、viewer 打开当前 `V1.0 ACTIVE`、目录 `4.Ohter`、`publishedFileId=9198354916366`、`stampedFileId=9198354916366`，低权限账号 `pengyunfeng` `browserTotal=0`。
- GREEN: `node doc/tasks/20260802-dcc-original-release-e2e-current/dcc-original-release-e2e-current.cjs` -> PASS，本轮复跑结果文件 `e2e-result-controlled-browser-final-rerun.json`，`runId=20260802104623`，授权账号 `wangsiyu` `browserTotal=1`，低权限账号 `pengyunfeng` `browserTotal=0`，`targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- GREEN: `pnpm ts:check` -> PASS。

## Project Experience Consolidation

- GREEN: project-experience-consolidation -> PASS，已更新 `docs/e2e-rules.md`，新增 `真实 E2E 主链路与扩展诊断产物隔离门禁`。
- GREEN: experience index -> PASS，已更新 `docs/experience-index.md`，新增 `e2e-result 被覆盖`、`viewer linkage 可选断言`、`traceability 可选诊断` 和同任务目录多个 Playwright 脚本共享结果文件等关键词路由。
- GREEN: project-experience-consolidation viewer linkage -> PASS，已在 `docs/e2e-rules.md#dcc-文控审批处理入口门禁` 补充 viewer 模式必须渲染最终目录路径和 `publishedFileId/stampedFileId`，并同步 `docs/experience-index.md` 关键词。
