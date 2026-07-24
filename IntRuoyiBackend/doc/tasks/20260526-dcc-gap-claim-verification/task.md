# DCC 截图需求缺口复核

## Task Goal

复核用户列出的 R05、R06、R07 和验证层缺口是否属实，结论必须基于当前 `int_main` 代码、任务记录、测试覆盖、运行库或真实页面证据。

## Milestones

- [x] M1：读取独立验证要求并建立复核记录。
- [x] M2：检查 R05 撤回后删除流程/重新提交入口。
- [x] M3：检查 R07 外来文件评审是否为完整业务流程。
- [x] M4：检查 R06 三个文件类别在运行库是否已配置。
- [x] M5：检查验证层是否已经重新跑真实 E2E/Maven 验证。
- [x] M6：形成逐条属实性结论。

## Expected Verification

- 代码证据：前后端当前 `int_main` 文件、API、路由、页面入口。
- 测试证据：DCC screenshot E2E 脚本、任务执行日志、当前可运行命令输出。
- 运行证据：必要时使用本地运行库 `127.0.0.1:23306/ruoyi-vue-pro` 只读查询，不修改非测试租户数据。

## Current Status

Completed.

## Final Verification Result

- R05 属实：当前系统有主动撤回能力，但未发现撤回后“删除流程 / 重新提交”二选一入口或后端状态流转。
- R07 属实：当前外来文件评审是复用受控文件上传页面与 `processType=EXTERNAL_REVIEW`，未形成独立完整的外来文件评审业务流程。
- R06 在当前本地测试租户不属实：租户 `122` 已确认存在 `体系文件`、`技术文件-DHF`、`技术文件-DMR` 三个未删除类别。
- 验证层部分属实：Maven 全量测试没有重跑；但 DCC Playwright 真实路径 E2E 已在当前 `int_main` 集成后重跑并通过。

详细证据见 `verification-report.md`。

## Cleanup Keep

- `doc/tasks/20260526-dcc-gap-claim-verification/verification-report.md`
