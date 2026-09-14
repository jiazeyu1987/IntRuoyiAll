# 20260905 Registration Download Simulated E2E Verify

## Task Goal

按照 `e2e_test/registration/download/registration-certificate-download-e2e-acceptance.md`，在独立 worktree 中使用 Playwright 操作真实前端页面，模拟或复用可追踪数据，逐项验证注册证下载 E2E。用户在 2026-09-05 明确追加要求：进行代码修复，修复之后再次进行 E2E 验证；随后明确要求从验收文件删除 E2E-6 验证并继续其他用例。

## Milestones

- [x] 读取项目规则、验收文档和 E2E 前置条件
- [x] 识别并复用独立 worktree 运行目录
- [x] 确认 worktree 前端 `8158`、后端 `48158` 在线
- [x] 执行 E2E-1 到 E2E-5 当前有效注册证下载链路
- [x] 执行 E2E-7 变更文件申请、审批、下载链路
- [x] 扫描 E2E-8/E2E-9 所需 OLD/组合样本页面可达性
- [x] 删除 E2E-6 验收用例及关联断言
- [x] 分析 E2E-8/E2E-9 未完全通过或阻塞原因
- [x] 输出 verification-report.md、result JSON 和截图/下载证据
- [x] 修复下载授权有效期、变更批件待申请入口和 OLD 详情打开阻塞
- [x] 运行定向 RED/GREEN 回归
- [x] 修复后再次执行注册证下载真实前端 E2E 验证
- [x] 补齐变更申请 BPM 摘要字段并运行定向回归

## Expected Verification

- 使用 Playwright 操作真实前端页面，账号路径为“芋道源码 / chudongchuan”和“芋道源码 / wanglixuan”。
- API/DB 只读核验不得替代页面动作；本轮未用 API/DB 造数，页面动作均从真实前端触发。
- 逐项记录当前验收范围内 E2E-1、E2E-2、E2E-3、E2E-4、E2E-5、E2E-7、E2E-8、E2E-9 的 PASS / FAIL / BLOCKED、证据位置和失败原因。
- 下载产物保存到任务目录下的 `e2e-artifacts/**/downloads/`，截图保存到 `e2e-artifacts/**/screenshots/`。

## Current Status

completed

当前验收范围已通过：E2E-1、E2E-2、E2E-3、E2E-4、E2E-5、E2E-7、E2E-8、E2E-9 均通过真实前端 Playwright 验证；E2E-6 已按用户要求从下载验收文档中删除。任务实现、验证、cleanup、fast-forward 合并和分支推送已完成。

## Design Constraints Check

- 已读取 `docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md` 和 `docs/branch-runtime-ports.md`。
- 遵守禁止 fallback、禁止 API-only 替代真实页面路径、禁止记录密码或 token。
- 本轮允许修改注册证下载链路相关业务源码、回归测试和任务文档；无关脏改动保留原状，未回滚、未提交。
- 已修复并验证 24 小时授权、变更批件仅审批生效后可申请/下载、OLD 详情公司范围内可打开且下载仍需授权。
- 已修复 OLD 详情变更履历跨版本串显，并补齐 OLD 变更批件 `_已失效` 前端命名兜底。
- 已补齐变更申请 BPM 摘要字段：证件编号、分类、产品名称、所属企业名称，并通过 `DccRegistrationCertificateChangeServiceTest` 回归。
- E2E-6 已按用户要求从当前下载验收 E2E 范围移除。
- E2E-8/E2E-9 已通过任务自有真实前端链路创建 OLD 与同版本变更批件组合样本，未通过 SQL、接口或数据库伪造样本。

## Cleanup Keep

- doc/tasks/20260905-registration-download-simulated-e2e-verify/task.md
- doc/tasks/20260905-registration-download-simulated-e2e-verify/execution-log.md
- doc/tasks/20260905-registration-download-simulated-e2e-verify/verification-report.md
- doc/tasks/20260905-registration-download-simulated-e2e-verify/final-result.json
- doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-e2e.cjs
- doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-change-and-old-e2e.cjs
- doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-change-file-e2e-8-9.cjs
- doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-old-combo-e2e.cjs

