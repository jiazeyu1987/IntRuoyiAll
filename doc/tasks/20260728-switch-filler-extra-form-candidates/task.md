# 修复粗洗工序附加表单填写人无法切换

## Task Goal

wangxin 在粗洗工序辅助填写时，“切换填写人”必须同时覆盖正式 `MAIN` 批记录表单和附加表单/表单槽位；点击附加表单候选后，必须通过后端 `task/open` 切换到对应任务、填写人和表单上下文。

## Milestones

1. 建立 BDD/RED 证据，证明当前执行详情 `assistSwitchTasks` 对附加表单候选覆盖不足。
2. 修复后端执行详情切换快照，复用正式填写人来源链路，覆盖活动 workTask、过程表单规则、工序规则和路线绑定候选源。
3. 核对前端弹窗不排除附加表单任务，并确保切换后使用 `openTask` 返回 query 刷新上下文。
4. 完成静态合同、后端定向测试、编译、前端校验和真实 E2E 证据。

## Expected Verification

- `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`
- 新增静态合同和目标 JUnit 覆盖附加表单候选。
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`
- `node IntRuoyiFronted/tests/e2e/edhr-switch-filler-selectability-static.spec.js`
- `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue src/api/mes/pro/feedback/index.ts --format stylish`
- `pnpm ts:check`
- 使用 `int_main` 本地前后端与账号 `wangxin` 执行真实 Playwright E2E。

## Current Status

blocked

阻断原因：原后端目标 JUnit/Maven 编译阻塞已解除并复验通过；真实 wangxin E2E 仍未找到可验证的附加表单切换样本，
`real-e2e-evidence.md` 记录 `no_wangxin_extra_form_switch_sample_found`。因此真实页面闭环仍未 GREEN，不能标记完成。

## 经验门禁

- `AGENTS.md#工艺路线三类配置术语契约`：批记录表单、表单槽位、工序开始必须分开建模和验证；本任务只补齐表单槽位填写人切换，不把附件负责人当作表单候选。
- `docs/backend-development.md#eDHR 详情回填门禁`：详情接口 `fillableUsers` 缺失时必须补后端正式数据链路，禁止前端文案或当前登录人兜底。
- `docs/backend-development.md#切换填写人快照读取边界`：切换填写人必须来自执行详情快照，禁止弹窗打开时重新拉全量批次详情。
- `docs/frontend-development.md#前端 Route Query ID 比较门禁`：切换后 URL query、active 高亮和表单上下文必须使用同一 ID 语义。
- `docs/frontend-development.md#切换填写人 FormCenter 槽位导航门禁`：FormCenter 槽位切换后必须用 `openTask` 运行态模板快照渲染，禁止依赖模板管理查询权限。
- `docs/e2e-rules.md#eDHR 工作任务 FormCenter 动态表单夹具门禁`：真实 E2E 必须证明 FormCenter 运行态表单由真实页面和 `task/open` 上下文打开，不得 API-only 或管理权限绕过。
- `docs/e2e-rules.md#worktree--int_main-运行态-url-门禁`：真实 E2E 使用 `E:\IntRuoyi` 的 `8081/48081` 成对入口。
- `docs/powershell-memory.md#脏工作区基线门禁`：实施前存在既有脏改动，必须独立基线提交，不能混入本任务实现。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，补齐后端执行详情切换快照的正式候选来源。
- 是否存在临时补丁或绕过：否。
