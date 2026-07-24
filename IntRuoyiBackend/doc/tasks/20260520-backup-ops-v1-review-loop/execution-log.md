# 执行日志：实现 IT 友好型备份恢复脚本 V1 并走评审闭环

BDD: IT 友好型备份恢复脚本骨架 -> Given 已完成的 `it-friendly-backup-design.md` 已定义入口、manifest、报告、配置和错误处理规则 When 在 `ruoyi-vue-pro/script/backup-ops` 中实现第一阶段脚本骨架 Then 必须先以 RED 测试定义静态契约，再实现脚本和模块，并最终由独立 reviewer 按逻辑层、易用性层和 UI 层判断是否放行
BDD: 控制台查看最近日志目录 -> Given 控制台入口和运行时脚本都依赖 `backup-ops.config.json` 的 `console.logRoot` When 操作员选择 `9. 查看最近日志目录` Then 控制台必须解析并打开与 PowerShell 运行时相同的日志根目录；若真实配置缺失则必须明确报错并停止，不能创建或打开错误的 `%SCRIPT_DIR%logs`
RED: reviewer report round 1 -> FAIL, `00-备份恢复控制台.bat` 将 `9. 查看最近日志目录` 硬编码到 `%SCRIPT_DIR%logs`，与运行时 `console.logRoot` 不一致
GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q -> PASS
GREEN: powershell parser check for `scripts\backup-ops.ps1` and `actions\Resolve-BackupOpsLogRoot.ps1` -> PASS
GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\actions\Resolve-BackupOpsLogRoot.ps1 -ConfigPath D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.config.example.json -> PASS, 输出 `D:\IntRuoyi-BackupOps\logs`
GREEN: launcher default-config parity check -> PASS, 当 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.config.json` 缺失时，新的日志目录解析返回非零并阻止打开错误目录
GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\scripts\backup-ops.ps1 -Mode backup-now -ConfigPath D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.config.example.json -SecretsPath D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.secrets.example.json -NonInteractive -> PASS，按设计返回 `EXIT_CODE=2`，并落 `_blocked.log`、`_blocked.report.json`、`_blocked.report.md`
RED: reviewer report round 2 -> FAIL, `D:\IntRuoyi-BackupOps\logs\202605\20260520_122905_backup-now_blocked.report.md` 中中文标题/标签为乱码，且 `结果`、`结束时间` 字段输出了字面 PowerShell 表达式而非实际值，release gate 未通过
RED: reviewer report round 3 -> FAIL, `rollback-app` 与 `restore-data` 抽样验证分别返回 `INTBK-5003` / `INTBK-3002`；根因是 `Get-BackupOpsRollbackTags` / `Get-BackupOpsRestoreCandidates` 返回空输出流后，`Select-BackupOpsImageTag` / `Select-BackupOpsRestorePoint` 的 mandatory `Candidates` 形参触发 `Cannot bind argument to parameter 'Candidates' because it is null.`，流程未进入设计中的 `blocked` 分支
BDD: rollback-app / restore-data 空候选阻断 -> Given phase-1 的候选发现尚未接线且可能返回空集合 When 操作员执行 `rollback-app` 或 `restore-data`，无论是否显式传入 `SelectedImageTag` / `SelectedBackupId` Then 脚本都必须返回受控 `blocked` 结果，不能暴露 PowerShell 参数绑定异常；无显式选择值时还必须给出清晰的业务阻断文案
RED: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q -> FAIL, 新增 `rollback-app` / `restore-data` 抽样回归用例先复现了 `Cannot bind argument to parameter 'Candidates' because it is null.`；随后补加“无显式选择值”回归时，又暴露 `Write-Output -NoEnumerate @()` 与 `@(...)` 叠加后把空数组包成单个空元素，导致空白候选和属性访问异常
GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q -> PASS, `14 passed`
GREEN: powershell sample verification -> PASS, `rollback-app -SelectedImageTag release-test -NonInteractive` 返回 `EXIT_CODE=2`、`INTBK-5002`，已进入 phase-1 受控 `blocked`，不再出现 `Candidates` 绑定异常
GREEN: powershell sample verification -> PASS, `restore-data -SelectedBackupId 20260520_123000 -NonInteractive` 返回 `EXIT_CODE=2`、`INTBK-5002`，已进入 phase-1 受控 `blocked`，不再出现 `Candidates` 绑定异常
GREEN: powershell empty-candidate verification -> PASS, `rollback-app -NonInteractive` 返回 `EXIT_CODE=2`、`INTBK-5001`，文案为 `未找到有效的回滚 IMAGE_TAG。`
GREEN: powershell empty-candidate verification -> PASS, `restore-data -NonInteractive` 返回 `EXIT_CODE=2`、`INTBK-1004`，文案为 `未选择有效恢复点。`
RED: reviewer report round 4 -> FAIL, `restore-data` 生成的 Markdown 报告中 `pre-restore 快照` 标签仍在 Windows PowerShell 5.1 运行时输出为乱码 `蹇収`，关键恢复审计信息不可读，release gate 未通过
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-backup-ops-v1-review-loop --mode preview` -> PASS，预览仅保留 `task.md` 与 `execution-log.md`
GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q -> PASS，重新复核结果 `14 passed`
GREEN: powershell sample verification -> PASS，`backup-now -NonInteractive`、`rollback-app -SelectedImageTag release-test -NonInteractive`、`restore-data -SelectedBackupId 20260520_123000 -NonInteractive` 均返回受控 `blocked`；`restore-data` Markdown 报告中的 `pre-restore 快照` 已可读，`rollback-app` 报告已持久化 `IMAGE_TAG`
GREEN: powershell empty-candidate verification -> PASS，`rollback-app -NonInteractive` 与 `restore-data -NonInteractive` 均返回受控 `blocked`，不再触发 `Candidates` 参数绑定异常
RED: reviewer report round 2 -> FAIL，空候选阻塞分支虽然不再崩溃，但 `rollback-app -NonInteractive` 与 `restore-data -NonInteractive` 的控制台/Markdown 报告仍只输出单行结论，缺少设计文档 `8.7 阻塞文案` 要求的 `原因 / 建议动作`，release gate 未通过
GREEN: powershell empty-candidate verification -> PASS，`rollback-app -NonInteractive` 现返回 `INTBK-5001`，并输出 `原因：当前未找到任何可回滚的 IMAGE_TAG 候选。` 与 `建议动作：请先完成一次备份或同步备份元数据，确认存在可回滚 IMAGE_TAG 后再重试。`
GREEN: powershell empty-candidate verification -> PASS，`restore-data -NonInteractive` 现返回 `INTBK-1004`，并输出 `原因：当前未找到任何可用恢复点候选。` 与 `建议动作：请先完成一次可恢复备份或同步备份元数据，确认存在恢复点后再重试。`
GREEN: reviewer report follow-up round 3 -> PASS，run `20260520T051316Z-ea13ef` 的独立 reviewer 判定 logic / usability / UI 全部通过，`final_decision: pass`
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-backup-ops-v1-review-loop --mode preview` -> PASS，预览仅保留 `task.md` 与 `execution-log.md`
