# Verification Report

## Verdict

PASS_WITH_ENVIRONMENT_GAP

PQC 生产放行 MVP 的代码、迁移、构建、类型、单元/合同测试和真实页面入口均通过。环境缺少待放行样本，因此未执行真实电子签名写入；未使用 mock、API-only 或直接 SQL 伪造业务通过。

## Verified Behavior

- 正式动态菜单入口为 `MES 系统 > eDHR批记录 > PQC生产放行`。
- 页面提供 `待放行`、`已放行`、`已作废`、`已返工`、`已让步放行` 五个页签。
- 待放行操作只保留 `放行`、`不合格审查`；通用工作待办改为进入专用页面，不再暴露旧拒绝动作。
- 直接放行必须校验当前 PQC 负责人电子签名并保存签名证据；签名失败不推进状态。
- 不合格审查可在尚未创建批次执行时按生产放行申请发起并冻结工单。
- QA 让步后回到待放行，PQC 签字完成后进入已让步放行；返工进入已返工；作废持续阻断工单和放行。
- 历史申请缺 PQC 待办 ID 时不会生成空 `IN ()` SQL，列表返回可用空态。

## Evidence

- 后端定向 JUnit：PASS，13 tests，0 failures，0 errors。
- 后端静态合同：PASS。
- 前端静态合同和 SP-2/工作待办/不合格评审相邻合同：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes -am -DskipTests compile`：PASS，24 modules。
- `mvn -pl yudao-server -am -DskipTests package`：PASS，30 modules。
- migration policy gate：PASS。
- `git diff --check`：PASS。
- Playwright：PASS，真实菜单、目标 URL、五页签和目标分页接口业务码均正确；截图为 `IntRuoyiFronted/output/playwright/pqc-production-release-mvp-completion/pqc-production-release.png`。

## Environment Gap

- 真实页面待放行行数为 0，电子签名和不合格评审写入动作未执行。
- Playwright 捕获一个外部头像 GET 连接拒绝；目标接口、页面脚本和业务操作不受影响。

## Runtime

- Worktree：`D:\IntRuoyiWorktree\pqc-production-release-mvp-completion-20260831`。
- Profile/slot：`int_main slot 56`。
- Frontend/backend：`8311/48311`。
- 本地验证库已幂等应用两条 20260831 迁移并回读目标状态。

## Integration

- 功能提交重放后为 `d9fe88557`。
- `int_main` 使用 `ff-only` 融合到 `d9fe88557`。
- 主工作区并行未提交改动未被暂存、覆盖或提交。
- 额外 worktree 和运行端口均已收口。
