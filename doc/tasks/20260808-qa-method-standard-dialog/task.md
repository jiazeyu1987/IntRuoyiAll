# QA 检验方法与接收标准弹框

## Task Goal

点击截图中的“检验方法”和“接收标准”信息卡时，用可关闭的弹框展示对应完整数据；弹框排版清晰、美观，并保持现有页面数据来源和卡片展示逻辑。

## Milestones

- [x] 建立任务文档与 BDD 验收口径。
- [x] 定位目标前端组件、数据字段和现有展示契约。
- [x] 先补聚焦静态合同并取得 RED。
- [x] 实现检验方法 / 接收标准详情弹框与视觉排版。
- [x] 运行 GREEN、类型检查和结构校验。

## Expected Verification

- 聚焦静态合同：点击“检验方法”和“接收标准”卡片会打开对应详情弹框，弹框可关闭，且详情内容来自正式卡片数据字段。
- `pnpm --dir IntRuoyiFronted ts:check` 或等效前端类型检查。
- `git diff --check`。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用当前正式 QA/PQC 页面数据字段并补充稳定弹框交互。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 命中 `docs/backend-development.md#MES PQC 项目级检验快照门禁`：检验方法、接收标准、上下限、单位和精度必须来自发布 QA 规程 / PQC 项目级快照；本任务仅改前端展示弹框，不改提交载荷、不使用 mock、默认值或 `rawPayload` 作为替代来源。
- 命中 `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：本任务新增聚焦静态合同作为 RED/GREEN 证据，并明确未把静态合同冒充真实页面 E2E。

## Cleanup Candidates

- doc/tasks/20260808-qa-method-standard-dialog/frontend-feature-evidence.md

## Closeout Evidence

- Cleanup preview: PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，删除临时 `frontend-feature-evidence.md`。
- Cleanup apply: PASS，`frontend-feature-evidence.md` 已删除；当前为主工作区 `int_main`，未执行 worktree merge/remove。
- Final verification: PASS，详见 `verification-report.md`。
