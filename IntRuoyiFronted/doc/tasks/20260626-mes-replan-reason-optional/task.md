# 任务：MES 手动重排应用取消业务原因必填

## 任务目标

- 取消 `/mes/pro/scheduleorder` 手动重排抽屉中“应用重排”对业务原因的必填拦截。
- 保持排产前检查、预览上下文、阻断问题校验等既有门禁不变。
- 不扩大到冻结、解冻、调序或自动排产发布等其他仍需原因的操作。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 frontend 相关任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-apply-disabled-regression\task.md`
- 状态：`已完成`
- 处理说明：上一任务修的是“预览生成后应用按钮仍不可点击”；本次继续处理同一抽屉里的“理由必填”行为变更。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本轮只做前端源码、静态合同、类型检查与证据校验，不做真实登录或写入型 E2E。
  - 重排抽屉继续沿用 IntPP 紧凑运维界面风格，不做无关视觉改版。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。直接取消前端本地必填校验，不增加兜底逻辑。
- `是否从根因和长期维护角度解决`：是。让前端交互与更新后的后端 apply 合同保持一致，避免一端放开、一端阻断。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 手动重排应用不再要求填写业务原因 -> Given 用户已生成有效重排预览且排产前检查无阻断 / When 用户不填写业务原因直接点击应用重排 / Then 前端不得再提示“请填写本次重排的业务原因”，而应继续发起 apply 请求。`
- `BDD: 业务原因仍可选填并随 apply 一起提交 -> Given 用户已生成有效重排预览 / When 用户填写业务原因后点击应用重排 / Then 前端仍应把最新 reason 一并提交给后端。`
- `BDD: 其他 apply 门禁保持不变 -> Given 用户未完成有效预览或排产前检查存在阻断 / When 用户点击应用重排 / Then 页面仍应继续拦截对应前置问题，不因本次变更放宽其他校验。`

## 里程碑

1. M1：创建任务包并补前端 RED 静态合同。
2. M2：最小修改重排抽屉 apply 校验逻辑。
3. M3：运行 GREEN 静态验证、类型检查与证据校验。

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-reason-optional\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-reason-optional\frontend-feature-evidence.md` -> PASS

## 阻塞与影响

- 当前无外部阻塞；前端已与更新后的后端 contract 对齐。
