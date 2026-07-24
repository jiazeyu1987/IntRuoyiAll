# 任务：MES 手动重排预览后应用按钮仍不可点击回归修复

## 任务目标

- 修复 `/mes/pro/scheduleorder` 手动重排抽屉中“已成功生成预览，但应用重排按钮仍不可点击”的前端回归。
- 保持“参数真正变化后必须重新预览”的门禁不变，但不能把刚生成的有效预览误判为已失效。
- 不修改后端重排接口协议、不移除排产前检查门禁、不通过放宽校验绕过预览上下文绑定。

## 当前状态

已完成。

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\task.md`
- 状态：`BLOCKED`
- 处理说明：用户在当前线程切换到更高优先级的 MES 手动重排按钮缺陷，本次先暂停展厅语音弹框任务，再处理当前回归。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本轮只做前端源码、静态回归、类型检查和缺陷证据校验，不做真实登录或写入型 E2E。
  - 手动重排抽屉继续沿用现有 IntPP 紧凑操作台样式，只修复可用性回归，不做无关视觉改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。保持真实预览上下文校验，只修复误判条件。
- `是否从根因和长期维护角度解决`：是。统一预览记录与失效判定的请求字段口径，避免同一预览被前端自己判成过期。
- `是否存在临时补丁或绕过`：否。不会简单去掉 `replanPreviewStale` / `preflightStale` 门禁。

## BDD 场景

- `BDD: 预览刚生成时应用按钮保持可用 -> Given 用户未修改重排范围、开始时间、产能模式和手工锁定开关 / When 页面成功生成一次重排预览 / Then 前端不得把该预览立即判为“预览参数已变化”，应用重排按钮保持可点击。`
- `BDD: 仅业务原因输入不应使预览失效 -> Given 用户已经生成有效的重排预览 / When 用户只填写或修改重排原因 / Then 预览仍视为有效，应用时继续复用当前预览上下文并附带最新业务原因。`
- `BDD: 真正影响预览结果的参数变化仍要求重新预览 -> Given 用户已经生成有效的重排预览 / When 用户修改开始时间、产能模式、手工锁定开关或重排范围 / Then 页面继续提示“预览参数已变化，请重新预览后再应用”，且应用按钮不可点击。`

## 里程碑

1. M1：创建缺陷任务包并补 RED 静态回归。
2. M2：最小修复预览失效判定逻辑。
3. M3：运行 GREEN 静态验证、类型检查与缺陷证据校验。

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-protected-task-readable-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-apply-disabled-regression\bug-regression-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-protected-task-readable-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-apply-disabled-regression\bug-regression-evidence.md` -> PASS
