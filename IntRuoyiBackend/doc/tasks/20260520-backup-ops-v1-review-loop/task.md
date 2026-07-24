# 任务：实现 IT 友好型备份恢复脚本 V1 并走评审闭环

## 目标

基于 `D:\ProjectPackage\Int\IntRuoyi\docs\recovery\it-friendly-backup-design.md`，在 `ruoyi-vue-pro` 仓库内落地第一阶段可开发的备份恢复脚本骨架与入口文件，并通过独立 reviewer 放行闭环判断是否可继续进入后续联调实施。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-backup-ops-v1-review-loop\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.review-fix-loop\**`

## 非范围

- 不直接连接正式/测试服务器执行真实备份
- 不修改现有业务模块代码
- 不把恢复入口嵌入前端业务系统
- 不实现第二阶段副本、binlog PITR、MinIO replication

## 上一任务检查

- 上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-codex-bilingual-narration\task.md`
- 状态：`completed`
- 说明：同仓库上一条最新任务已完成，本任务可继续在脚本与测试范围内推进。

## 里程碑

- [x] M1：创建任务文档并确认实现边界。
- [x] M2：先写 RED 测试，定义脚本目录、入口、配置、结果模型和报告输出契约。
- [x] M3：实现 `backup-ops` 脚本骨架与首版模块拆分。
- [x] M4：运行 GREEN 验证并初始化 review-fix-loop run。
- [x] M5：由独立 reviewer 评审，不通过则修复后复审。

## 预期验证

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q`
- `powershell -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\scripts\backup-ops.ps1',[ref]$null,[ref]$null)"`
- `python C:\Users\BJB110\.codex\skills\review-fix-loop\scripts\render_status.py --cwd D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --json`

## 当前状态

Completed. 当前 phase-1 IT 友好型备份恢复脚本骨架已通过新的独立 reviewer 放行。

- 2026-05-20：round 2 独立 reviewer 复核确认，round 1 的两个阻塞项均已修复：`9. 查看最近日志目录` 已按运行时配置解析 `console.logRoot`，`blocked` 路径也会落 `_blocked.log`、`.report.json` 与 `.report.md`。
- 2026-05-20：round 2 新增 release blocker：`script/backup-ops/scripts/modules/Infra/ReportOps.psm1` 生成的 markdown 报告内容仍不可放行。实际产物 `D:\IntRuoyi-BackupOps\logs\202605\20260520_122905_backup-now_blocked.report.md` 存在中文乱码，并将 `status`、`completedAt` 渲染为字面 PowerShell 表达式而非实际值。
- 2026-05-20：round 3 worker 已按 reviewer 阻塞项完成修复，`Get-BackupOpsRollbackTags` / `Get-BackupOpsRestoreCandidates` 现在会输出可绑定空数组，`Select-BackupOpsImageTag` / `Select-BackupOpsRestorePoint` 可安全接收空候选并优先短路显式选择值，`RollbackApp` / `RestoreData` use-case 也改为直接消费空数组对象，避免把空集合包成单个空元素。
- 2026-05-20：新增 4 条回归验证覆盖 `rollback-app` / `restore-data` 的抽样示例和“无显式选择值”空候选路径；当前验证结果为 `python -m pytest ... -> 14 passed`，并已人工复核四条 PowerShell 命令均返回受控 `blocked` 与预期文案。
- 2026-05-20：round 4 独立 reviewer 复核仍未放行。逻辑层已通过，但 `restore-data` 生成的 Markdown 报告中 `pre-restore 快照` 标签仍出现乱码 `蹇収`，导致关键恢复审计信息不可读。
- 2026-05-20：根据 `review-fix-loop` 最大 4 轮规则，本次 run 已标记为 `blocked`，阻塞原因为：`restore-data markdown report still emits unreadable pre-restore snapshot label under Windows PowerShell 5.1`。
- 2026-05-20：针对上述编码阻塞项重新初始化 run `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.review-fix-loop\runs\20260520T051316Z-ea13ef` 并继续复审；编码问题、`IMAGE_TAG` 持久化和空候选绑定异常均已修复。
- 2026-05-20：follow-up run 第 2 轮独立 reviewer 发现 `rollback-app -NonInteractive` 与 `restore-data -NonInteractive` 的空候选 blocked 文案仍缺少 `原因 / 建议动作` 合同。
- 2026-05-20：继续定向修复空候选 operator-facing blocked 文案与回归测试后，follow-up run 第 3 轮独立 reviewer 判定 `pass`，logic / usability / UI 三层均通过。

## 最终验证结果

- RED：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q` 在新增 `script/backup-ops` 契约测试后首次失败，缺少入口、模块、配置样例和脚本骨架。
- GREEN：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q`
  - 当前结果：`14 passed`
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\scripts\backup-ops.ps1 -Mode backup-now -ConfigPath ...config.example.json -SecretsPath ...secrets.example.json -NonInteractive`
  - 当前结果：受控 `blocked`，产出 `_blocked.log`、`_blocked.report.json`、`_blocked.report.md`
- GREEN：`rollback-app` / `restore-data` 的显式选择值和空候选路径均已不再触发 `Candidates` 参数绑定异常，改为返回业务级 `blocked`
- FAIL：上一条 run 的第 4 轮独立 reviewer 判定 `restore-data` Markdown 报告中的 `pre-restore 快照` 标签存在乱码，release gate 未通过
- GREEN：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q`
  - 当前复核结果：`14 passed`
- GREEN：`backup-now -NonInteractive`、`rollback-app -SelectedImageTag release-test -NonInteractive`、`restore-data -SelectedBackupId 20260520_123000 -NonInteractive`
  - 当前复核结果：均返回受控 `blocked`，且产物中不再出现中文乱码或丢失 `IMAGE_TAG`
- GREEN：`rollback-app -NonInteractive`、`restore-data -NonInteractive`
  - 当前复核结果：均返回受控 `blocked`，不再触发 `Candidates` 参数绑定异常
- PASS：follow-up run `20260520T051316Z-ea13ef` 第 3 轮独立 reviewer 判定 `pass`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-backup-ops-v1-review-loop --mode preview`
  - 结果：`ready`，仅保留 `task.md` 与 `execution-log.md`

## Blockers

- 无当前阻塞项。第一轮 run 已按上限规则保留为 `blocked` 审计记录，但 follow-up run 已通过放行。

## 结果说明

- review-fix-loop run：
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.review-fix-loop\runs\20260520T034611Z-1a052a`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.review-fix-loop\runs\20260520T051316Z-ea13ef`
- 最终结论：`completed`
- 提交状态：未创建 Git 提交。原因是当前仓库存在大量与本任务无关的已跟踪/未跟踪改动，无法安全形成只包含本任务的独立提交。
