# 20260909-epassword-compliance-hardening

## Task Goal

整改电子签名合规审查中剩余高风险项：禁止正式电子签名使用用户手动选择的签名时间，确保非密码验证的草稿/模拟记录不会被当作正式电子签名证据，补强多人审批签名顺序证据，并为定期合规审查能力补充可验证约束。

## Milestones

1. 建立 BDD/TDD 场景和当前风险 RED 测试。
2. 后端整改正式电子签名时间、身份验证和审批顺序约束。
3. 必要时同步前端交互，避免正式签名入口暴露用户选择签名时间。
4. 补充定期合规审查和导出/查询口径测试。
5. 执行后端、前端和必要 E2E 验证。

## Expected Verification

- 后端定向单元/集成测试覆盖正式电子签名必须使用服务器时间、必须密码重认证、内容绑定和审批顺序元数据。
- 前端静态或 E2E 验证正式签名页面不允许用户选择签名时间。
- 合规审查记录创建校验 SOP、培训证据、负责人、计划时间和截止时间。
- 若执行 E2E，必须通过 Playwright 使用真实前端、真实测试账号和任务自有数据，不用 API 代替验收动作。

## Design Constraint Checks

- 不引入 fallback、兼容绕路或模拟成功。
- 正式电子签名统一走统一签名内核。
- 签名时间只允许系统/可信时间生成；用户输入时间只能作为业务发生时间，不得命名或展示为签名时间。
- 草稿、模拟、登录会话保存记录不得进入正式电子签名证据口径。
- 多级审批签名顺序必须有可审计的流程/节点顺序证据或显式失败。

## Current Status

ready_for_closeout

已创建 worktree `D:\IntRuoyiWorktree\20260909_epassword`，分支 `codex/20260909_epassword`，端口槽位 `35`，前端 `8210`，后端 `48210`。

已完成正式电子签名时间、BPM 审批上下文、模拟/草稿签名模式和前端正式签名入口整改；已 rebase 到最新 `int_main`，当前提交 `1d5130f8d`；rebase 后定向后端测试、前端静态契约、历史 E2E 语法检查、`pnpm ts:check`、端口门禁和 `git diff --check` 通过。

本轮继续补齐 4.10 定期电子签名合规审查的制度与设计证据，新增 `docs/security/electronic-signature-periodic-compliance-review-sop.md` 和 `docs/security/security-privacy-compliance-review.md`；安全合规证据校验和 `git diff --check` 已通过。尚未完成收尾清理和主线融合：收尾预览此前被主工作区 `E:\IntRuoyi` 的无关 dirty 状态阻塞。
