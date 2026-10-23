# 本地开发审计策略

`yudao.gxp-audit.local-development-policy` 默认关闭。显式开启后，仅允许单独的 `local` Spring profile，且主、从 MySQL 数据源必须使用 `localhost`、`127.0.0.1`、`127.0.0.2` 或 `[::1]`。非本地 profile（包括与 `local` 混用）及远端数据库会导致启动失败。

本机启动脚本启用方式：

```powershell
& .\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main -LocalDevelopmentAuditPolicy
```

直接启动时可传入 `--yudao.gxp-audit.local-development-policy=true`，或在 `local` 配置下设置环境变量 `INTRUOYI_LOCAL_DEVELOPMENT_AUDIT_POLICY=true`。脚本每次显式传入开关值；不带参数即关闭，不依赖上次启动状态。

本地模式使用包内通过 schema 校验的策略，允许 `DRAFT`、尚未正式激活或与数据库正式版本不同的策略参与调试。启动日志明确输出本地模式、原策略版本、状态和完整策略哈希。审计写入和权限变更使用同一策略源，事件的策略版本为 `LOCAL-DEV-<完整策略 SHA-256>`。它不表示正式批准，不写入或替换正式策略版本、操作及激活记录，也不修改历史事件。

真实身份、租户、操作存在性、修改原因、签名要求、事务、账本锁、哈希链和幂等校验继续执行；已有账本水位仍是前置条件。未知操作和审计写入失败仍会报错。该配置不修复业务规则或旧测试夹具问题。

策略内容变化会生成不同的开发版本；旧请求的幂等键不能用于重放不同版本的业务动作。保留运行包及其策略文件，以便用事件中的完整哈希核对开发策略。

恢复正式模式：停止本地服务，完成策略审查并通过现有维护工具正式激活，然后不带 `-LocalDevelopmentAuditPolicy` 重启。直接启动时删除或关闭对应属性/环境变量。本地模式下禁止调用正式激活服务；正式维护工具的批准要求保持有效。关闭本地模式后，原有启动匹配校验自动恢复，开发事件仍保留其明确标识。
