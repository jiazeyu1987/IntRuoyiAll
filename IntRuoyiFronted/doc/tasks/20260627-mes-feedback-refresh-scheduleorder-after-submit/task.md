# 任务：MES 报工成功后自动刷新排产工单

## 任务目标

- 在 MES 报工页面确认报工成功后，自动刷新对应的排产工单列表，避免用户手动切到排产工单页再点刷新。
- 保持现有后端接口合同不变，不新增额外报工提交链路。
- 仅在真实报工提交成功后触发刷新，不在失败、取消或纯本地编辑时误刷新。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端相关任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-feedback-pending-batch-confirm\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成待归属页整批确认流程；本次继续补“确认报工成功后自动刷新排产工单”的跨页面状态同步。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本轮只做本机前端代码、静态契约与任务文档，不进入真实登录写入或 Playwright 长链路。
  - 保持 IntPP 操作台式样，不新增与本需求无关的视觉改造。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。报工失败时仍应原样暴露错误，不做静默刷新或伪成功。
- `是否从根因和长期维护角度解决`：是。通过页面间统一事件同步成功状态，而不是依赖用户手动刷新。
- `是否存在临时补丁或绕过`：否。不通过定时轮询或强制整页 reload 解决。

## BDD 场景

- `BDD: 整批确认报工成功后自动刷新排产工单 -> Given 用户在报工页成功确认当前导入批次 / When 页面切回正式报工 tab 并发出成功提示 / Then 排产工单页若已打开，应自动重新拉取列表以显示最新进度。`
- `BDD: 报工失败或取消时不刷新排产工单 -> Given 用户取消确认或后端返回失败 / When 报工流程结束 / Then 不发送排产工单刷新事件，也不触发多余列表刷新。`

## 里程碑

1. M1：创建任务包并定位报工成功链路与排产工单刷新入口。`COMPLETED`
2. M2：实现成功事件派发与排产工单页监听刷新。`COMPLETED`
3. M3：补静态测试并回写证据。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-feedback-schedule-order-refresh-static.spec.js`

## 最终验证结果

- `node tests/e2e/mes-feedback-schedule-order-refresh-static.spec.js` -> PASS
