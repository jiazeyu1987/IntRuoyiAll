# 验证报告：可信时间最小闭环

## Result

PASS：P1-P5 实现、定向回归、远程只读核查和本地真实 Playwright E2E 均通过独立验证；远程移除阈值与重启尚待目标及 `PROD` 确认。

## Implemented

- 正式签名展示时间固定使用服务器 `signedAt`；用户选择时间独立作为业务发生时间。
- Runtime Control 巡检新增正式服、审查服 chrony、系统同步、服务器 UTC 和数据库 UTC 证据。
- 阈值缺失、命令失败、无选中源、Leap/NTP/Stratum/UTC 异常以及 Last/RMS 偏差超限均失败关闭。
- 指定巡检 ID 可导出固定三文件 ZIP：HTML 摘要、原始 JSON、SHA-256 清单。
- 运行控制台显示可信时间状态并提供只读指定巡检的导出按钮。

## Verification

- MES 签名服务：13/13 PASS。
- 批执行时间相关方法：2/2 PASS。
- Infra 可信时间、巡检、导出、装配与术语合同：40/40 PASS；术语扩展相关回归 77/77 PASS。
- 前端静态合同、Node 语法和 `pnpm ts:check`：PASS。
- backend/frontend evidence validators、PowerShell AST、`git diff --check`：PASS。
- branch runtime port guard：PASS，slot 27，前端 8161，后端 48161。
- 真实页面巡检 ID 3：正式服 Last/RMS `-0.312/0.571 ms`，审查服 `-0.232/0.186 ms`，两项时间源、Leap、服务器/数据库 UTC 和检查时间完整且均为 PASS。
- 页面真实下载 ZIP：固定三文件、HTML/JSON SHA-256、两台主机和两项可信时间状态均校验通过；整体 `NO_GO` 原样保留。
- 页面探针及最近操作区域不再显示 `Backup`、`备份服`、`备份服务器`、`备用服务器`；历史审计原值只做展示投影，不改写存储。
- 测试阶段未配置偏差阈值时仍采集 Last/RMS，`maxOffsetMillis=null`，只跳过数值超限判断；其它可信时间门禁不变，配置正数阈值时仍恢复超限阻断。
- P5 独立回归：后端 32/32 与完整相关 36/36 PASS，前端静态合同及 `pnpm ts:check` PASS。
- 真实页面巡检 ID 4：正式服 Last/RMS `-1.225/0.630 ms`、审查服 `-0.820/0.302 ms`，两项 PASS；唯一导出按钮生成的 ZIP 三文件与 SHA-256 通过，JSON 两项 `maxOffsetMillis=null` 且 Last/RMS 完整。

## Real Environment Read-Only Evidence

- 正式服：chronyd active，选中 `139.199.214.202`，Stratum 3，Last/RMS 偏差约 `0.432/0.682 ms`，Leap Normal，系统与 NTP 正常，服务器和数据库 UTC 可读取。
- 审查服：chronyd active，选中 `139.199.214.202`，Stratum 3，Last/RMS 偏差约 `-0.034/0.228 ms`，旧版 NTP enabled/synchronized 均为 yes，服务器和数据库 UTC 可读取。
- 两台 chrony 配置均为 root 所有 0644；时间命令无 setuid；现有 backend/mysql/minio 容器均非 privileged 且未增加 capability。

## Boundaries

- 未修改 SQL、schema、历史数据或历史签名哈希。
- 已进行获授权的远程只读时间核查；未修改 chrony、未重启远程服务、未写数据库。
- 已在正式服、审查服运行目录的 `.env` 配置显式阈值 `100 ms`，均保留任务前备份；未重启或发布远程应用，未写远程数据库。
- 当前代码尚未发布到远程服务器；真实 E2E 使用任务 worktree 的 8161/48161 运行态，时间采集读取两台服务器真实只读状态。
- 初始实现提交 `34e915a29` 与真实环境/术语闭环提交 `3d6ea3ba4` 均已推送至 `origin/codex/timestamp_20260907`；未合并或删除 worktree。
- task-closeout-cleanup preview 已通过删除范围识别：仅计划删除两个临时技能 evidence 文件；apply 因主工作区其它任务脏改动及非快进合并条件而安全阻断，未执行删除、合并或 worktree 移除。
- 本任务本地 8161/48161 验收服务已按归属停止并释放端口；共享 MySQL、Redis、MinIO 依赖保持运行。
- 用户请求的远程阈值移除和服务器重启未执行：目标环境尚未明确，且 `172.30.30.57/59` 的重启按项目规则必须取得 `PROD` 明文确认。
- P5 实现提交 `18a776ff9` 已推送至 `origin/codex/timestamp_20260907`。

## Int Main Merge Verification

- `int_main` 与 `codex/timestamp_20260907` 非快进关系，已用 `--no-ff --no-commit` 预检并解决 3 个冲突文件。
- 冲突处理原则：主线统一电子签名服务优先；可信时间分支的业务发生时间进入统一签名命令的 `businessOccurredAt/businessTimeZone`，不覆盖服务器正式签署时间。
- PASS：MES 冲突方法与归档渲染定向回归。
- PASS：Infra 可信时间、巡检、导出、接口和 Spring 装配相关回归。
- PASS：前端可信时间导出静态合同、活动执行签名选择静态合同、`pnpm ts:check`。
- PASS：`git diff --check` 与 branch runtime port guard。
- 已知主线前置条件阻塞：`MesProEdhrBatchExecutionServiceTest` 全类会因 H2 测试表 `mes_pro_edhr_batch_execution_origin` 缺失失败；本次只按冲突方法与可信时间范围判断合并闭环。
