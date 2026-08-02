# 20260802 DCC 原版上传 E2E

## Task Goal

验证 DCC 文控“新文件原版上传”真实业务链路：使用 5 个非 admin 账号，上传一个新的 V1.0 受控文件，依次完成四级 DCC 审批，并核验数据库中该原版文件生效。

## Milestones

- [x] 建立任务目录与 BDD/验证门禁记录
- [x] 裁剪升版 E2E 脚本为原版上传专用链路
- [x] 执行真实 Playwright 前端路径上传与四级审批
- [x] 只读核验最终数据库状态与任务证据
- [x] 敏感信息扫描、QA 证据校验与收尾清理

## Expected Verification

- `node --check doc/tasks/20260802-dcc-upload-original-e2e/dcc-upload-original-e2e.cjs` 通过。
- Playwright 通过真实前端页面完成 `/dcc/controlled-file/upload` 新文件上传。
- `pengyunfeng` 作为上传人，`zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 依次完成真实 DCC 审批处理态操作。
- 只读 DB 核验同一 `file_number` 仅存在 V1.0 行，`change_type=NEW`，状态为 `ACTIVE`，master 当前生效文件指向该 V1.0。
- 只读 DB 核验上传审批历史任务完成数不少于 4。
- 任务目录敏感信息扫描不包含明文密码或认证令牌关键字。

## Applicable Gates

- DCC 文控审批处理入口门禁：审批必须从真实审批中心进入 DCC 处理态，不得降级为 viewer 只读或 API-only。
- Playwright 浏览器可执行文件门禁：使用本机 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 作为浏览器可执行文件。
- Element Plus 上传控件门禁：真实上传后必须看到文件列表或目标上传接口，不得仅用 `input.files` 证明上传成功。
- QA Evidence Contract：报告必须包含范围、矩阵、测试数据、RED/GREEN、阻塞项、CI/发布建议。
- Cleanup Keep 门禁：需要长期保留的 E2E 脚本与结果文件必须写入 `Cleanup Keep`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务不改生产逻辑，仅验证正式页面链路和落库状态。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- E:\IntRuoyi\doc\tasks\20260802-dcc-upload-original-e2e\task.md
- E:\IntRuoyi\doc\tasks\20260802-dcc-upload-original-e2e\execution-log.md
- E:\IntRuoyi\doc\tasks\20260802-dcc-upload-original-e2e\verification-report.md
- E:\IntRuoyi\doc\tasks\20260802-dcc-upload-original-e2e\dcc-upload-original-e2e.cjs
- E:\IntRuoyi\doc\tasks\20260802-dcc-upload-original-e2e\e2e-result.json

## Current Status

completed
