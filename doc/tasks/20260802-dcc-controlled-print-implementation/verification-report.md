# Verification Report

## Status

E2E BLOCKED

## Summary

本报告用于记录 DCC 受控打印功能补齐后的 RED/GREEN、真实 Playwright E2E、权限阻断和只读核验证据。

## Print Evidence

- 打印记录 ID：未生成；真实页面 E2E 未进入打印动作
- 文件编号：CODX-DCC-ORIG-20260802101521
- 文件版本：V1.0 / ACTIVE / 当前有效版本
- 打印人：未执行页面打印，未产生打印人
- 份数：未执行页面打印，未产生份数
- 审批人：不适用，当前补齐范围按直接受控打印设计
- 审批/打印状态：未执行；目标设计状态为 `DIRECT_PRINTED`

## PASS / BLOCKED

- 当前结论：E2E BLOCKED
- 已通过的实现验证：前端静态合同、`pnpm ts:check`、后端静态合同、DCC Maven 定向合同、SQL migration policy gate、本地 DB 表/菜单只读核验。
- 后端运行态证据：最新生产 jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-213049.jar` 启动失败，日志报 `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: SHOWROOM`。
- 本机恢复证据：已恢复旧可运行 jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-170535.jar`，`http://127.0.0.1:48081/actuator/health` 返回 `UP`，监听 PID `64208`。
- 阻断项：当前无法在 `48081` 加载包含 DCC 受控打印新功能的后端 jar；继续真实 Playwright E2E 会变成旧 jar 验证，不能证明受控打印新功能。
- 合规说明：未使用 admin 账号完成业务打印，未通过 API-only/SQL 创建打印记录，未使用旧 jar 冒充受控打印 PASS。
