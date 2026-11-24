# 提醒任务分钟调度发布合同

构建 JOB-3684c6abbf 在 migration-policy-and-history 阻断：数据迁移 `20261123_dcc_registration_certificate_reminder_minute_tick` 已列入 release-sources.json 的正式 migrations，但没有同名 target-preflight 文件。

新增同名只读前置 SELECT，与原 SQL 的断言一致：`infra_job` 中 handler_name=registrationCertificateReminderDailyJob、deleted=0 的行数必须恰好为1。保留任务的启用/暂停状态；本次不执行迁移或修改目标数据库。原 SQL 不改名、不改类型、不改字节；其固定提交 SHA256 为 `03da6954bc0f7e5472c5887ff5948e8f3d845ede28ea04294c2db49654528536`。

验证使用真实发布工具 `C:/SpugHandoff/release/source/ops/release/release_source_catalog.py` 和 `ops/spug/release-center/intruoyi_build.py`，未替换解析器或放宽门禁。正式检查在本轮专用固定提交 worktree `C:/SpugHandoff/release/tick-verify-f0806c2` 执行，core.autocrlf=false，验证源提交 f0806c282；无服务启动或端口占用，无新提交。首次长路径 worktree 创建失败并由 Git 清除，随后改用上述短路径。主工作区因换行转换导致的历史 checksum 差异不用于发布判断。

- 应用发布来源登记检查、真实 release_source_catalog：通过。
- 真实 migration_inputs：912条迁移、1230项打包资产；新增预检 readOnly、reference、SHA256 均绑定通过。
- 从本机发布状态 SQLite 以 mode=ro 读取已登记的 test/prod/backup ledgerRecords，执行 historical_integrity 与 target_input_plan：三环境通过，待执行的分钟调度迁移均具有必需预检。
- 负向验证：仅在内存中移除该迁移的预检绑定，三环境均被原 required target preflight missing 门禁拒绝。
- 新预检固定提交 SHA256：`829c49ba5e39ecd623f341e62a1056a8e2f5067e218d120429d881204f294471`。

上述是发布输入与登记台账合同验证，不是目标数据库 SELECT 的执行结果。维护线程须从最终提交重建，并在部署流程执行真实目标预检；目标行数不符必须阻断，不创建或猜测任务补齐。日志保留于主工作区 .runtime/minute-tick-*。本轮自建验证 worktree 验证后清理，不触碰维护线程的构建或预检 worktree。
