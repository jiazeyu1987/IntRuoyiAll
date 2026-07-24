# 任务：智能排产四条路线默认值补齐 前端实现

## 任务目标

- 在前端正式入口暴露参与排产的默认值维护能力。
- 让排产员可以在工作台、工艺排产路线和资源维护页补齐 4 条目标路线所缺数据。
- 移除仍然掩盖缺配置的前端默认值与隐藏逻辑。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-srm-nas-locator\task.md`
- 状态：`BLOCKED`
- 处理说明：用户切换到 MES 智能排产需求，已先阻塞上一前端任务。

## 本轮授权

- 用户于 `2026-06-28` 在当前任务中明确授权：允许使用 `芋道源码/admin` 的真实登录会话，对 `tenant_id=1` 下本任务指定的 4 条路线执行正式补数。
- 前端真实操作范围仅限登录预检、读取鉴权上下文和调用现有 MES 正式接口；不得切换到其他租户或旁路写库。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 新增或调整的工作台/路线配置界面必须保持 IntPP 紧凑工作台样式。
  - 前端不得通过隐藏默认值掩盖缺配置；需要用户维护的排产值必须看得见、改得了、存得下。
  - 若进入真实登录或真实 E2E，必须先记录 `GREEN: experience-preflight -> PASS` 并先跑官方登录预检。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。让默认值走正式前端配置入口与正式 API，不再依赖代码常量。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工作台前端可维护默认排产值 -> Given 排产员打开排产员工作台 / When 需要设置路线补齐默认值 / Then 默认产能模式、默认有限产能、默认夜班规则和默认人工资源值必须在工作台前端可维护。`
- `BDD: 工艺排产路线可用默认值补齐缺项 -> Given 某条路线当前没有完整 SCHEDULE 配置 / When 排产员打开工艺排产路线并应用默认值 / Then 前端必须把正式值写回用途配置和排产策略，而不是依赖后端偷偷补数。`
- `BDD: 前端不再隐藏排产默认值 -> Given 当前页面仍存在 ` + "`10.5`" + ` 一类隐式默认值 / When 用户维护排产数据 / Then 页面必须去掉这类隐藏默认值，改为显式读取正式配置或显式暴露缺项。`

## 里程碑

1. M1：建立前端任务文档与执行日志。`COMPLETED`
2. M2：写前端静态 RED 测试。`COMPLETED`
3. M3：实现工作台和路线页正式维护入口，移除隐藏默认值。`COMPLETED`
4. M4：跑前端静态回归并回填证据。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-smart-scheduling-four-risk-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-route-use-config-display-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-shift-hours-static.spec.js`

## 当前阻塞

- 无。

## 完成结果

- 排产员工作台已暴露正式默认排产字段：默认智能排产用途、默认产能模式、默认有限产能、默认无限公式、默认夜班、默认人工人数和默认单人产能。
- 工艺排产路线页已提供“应用工作台默认值”，能把缺失 `SCHEDULE` 用途/排产项显式回填为工作台正式默认值。
- 资源大表已提供“应用默认人工”，并允许前端维护 `singleStandardHourlyCapacity`，不再只能依赖后端补值。
- `RouteProcessList.vue` 已移除隐式 `10.5` 默认班次小时，页面缺项会显式暴露而不是悄悄吞掉。
- 静态回归均通过，且配合后端正式接口已完成 `tenant_id=1` 下 4 条路线的真实补数。
