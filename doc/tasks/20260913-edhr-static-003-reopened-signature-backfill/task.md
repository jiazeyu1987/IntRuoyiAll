# EDHR-STATIC-003 Reopened Signature Backfill

## Task Goal

修复重新打开的 EDHR-STATIC-003：旧分配已有关联 `reviewId`，但对应 APPROVED 复核缺少 `reviewSignatureSnapshotJson` / 签名字段时，生产组长保持原分配数量并重新本人签名确认，必须补齐同一复核证据并满足后续完工读取合同。

## Milestones

- [x] 读取项目规则、收尾规则、后端规则、PowerShell 编码规则、电子签名 ADR、缺陷与独立复核证据。
- [x] 建立任务文档与 BDD/TDD 记录。
- [x] 增加旧记录缺签名且数量不变的 RED 回归。
- [x] 最小修改 `MesReportAllocationCommandService`，统一分配复核写入与完工读取合同。
- [x] 执行定向非 E2E 静态/单元验证、`git diff --check` 和变更范围静态审查。
- [x] 收尾前标记 `ready_for_closeout`，运行清理预览/应用后标记 `completed`。
- [x] 按用户授权将任务分支融合进 `int_main`，解决主干冲突并推送远端主干。

## Expected Verification

- RED：定向回归在旧 APPROVED 复核已有 `reviewId` 但缺少签名字段时失败，证明数量不变分支不会补签。
- GREEN：同一定向回归通过，验证原分配引用不变、复核签名字段补齐、反馈批准人与确认时间仍满足下游同一证据检查。
- REGRESSION：重复确认不得重复签名或新建复核。
- 收尾静态检查：`git diff --check`、变更范围审查、UTF-8 文档读取验证。
- 按任务范围不执行 E2E、不启动服务、不写数据库；2026-09-13 用户已授权本任务 Git 提交/推送与融合进 `int_main`。

## Current Status

completed

## Design Constraints Check

- 禁止 fallback、兼容绕过、吞异常或默认成功。
- 缺签名 APPROVED 旧复核只有在本人提供有效签名证据的重新确认路径中补齐，不允许缺密码产生正式 APPROVED 签名复核。
- 只改 EDHR-STATIC-003 必需范围；共享逻辑变更保持最小，并在验证报告记录风险。
- 证据来源：`E:\IntRuoyi\docs\bugs\20260912-edhr-90-step-static-audit.md` 中 EDHR-STATIC-003，以及 `E:\IntRuoyi\doc\tasks\20260913-edhr-fix-independent-audit\verification-report.md` 的 R003。
