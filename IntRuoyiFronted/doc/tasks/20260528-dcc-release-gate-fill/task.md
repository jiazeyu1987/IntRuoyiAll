# DCC Release Gate E2E 补齐

## 任务目标

修正 `tests/e2e/dcc-controlled-file-protection.e2e.js` 与当前 DCC 上传页真实 UI 的合同不匹配：上传页固定使用 `SOURCE` 目的，不提供可填写的 purpose 控件，E2E 不能强制要求不存在的前端控件，也不能静默跳过 purpose 约束。

## 里程碑

- M1：复现上传 E2E 对 `*_PURPOSE_SELECTOR` / `*_PURPOSE_VALUE` 的错误强制依赖。
- M2：增加回归测试，证明固定 purpose 模式必须被显式配置。
- M3：最小修改 E2E 脚本和 env 示例，支持显式 `*_FIXED_PURPOSE_VALUE`。
- M4：运行 RED/GREEN/REGRESSION，并把剩余真实 release gate 前置记录回根任务。

## 预期验证

- `node tests\e2e\dcc-controlled-file-protection.contract.test.js`
- `node --check tests\e2e\dcc-controlled-file-protection.e2e.js`
- `node tests\e2e\dcc-controlled-file-protection.e2e.js`

## 当前状态

已完成前端 E2E 合同修正。`*_FIXED_PURPOSE_VALUE` 模式已覆盖当前固定 `SOURCE` 上传页；未显式配置 purpose 模式时仍 fail fast。2026-05-30 融合最新 `int_main` 后，DCC 前端合同和脚本语法继续通过，eDHR 新增前端覆盖合同与发布覆盖 gate 也通过。按用户要求，本轮不发布到服务器，仅做本地验证；本地 `8081 -> 48081` 完整 DCC 浏览器 E2E TC-E2E-001 至 TC-E2E-017 已通过。

## Cleanup Keep

- `doc/tasks/20260528-dcc-release-gate-fill/verification-report.md`
