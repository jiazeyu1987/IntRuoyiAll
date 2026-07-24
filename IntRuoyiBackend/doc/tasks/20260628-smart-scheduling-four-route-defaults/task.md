# 任务：智能排产四条路线默认值补齐 后端实现

## 任务目标

- 把 MES 排产默认值从后端隐式常量收口到正式配置读取。
- 支撑前端维护默认排产值，并让 4 条目标路线补齐后可参与排产。
- 禁止后端继续写死 `hourlyCapacity=1` 或类似排产数值。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-srm-nas-locator\task.md`
- 状态：`BLOCKED`
- 处理说明：用户切换到 MES 智能排产需求，已先阻塞上一后端任务。

## 本轮授权

- 用户于 `2026-06-28` 在当前任务中明确授权：允许通过现有 MES 正式接口对 `芋道源码/admin` 所在 `tenant_id=1` 的 4 条目标路线执行真实补数。
- 后端写入范围仅限工作台正式默认排产配置、路线 `SCHEDULE` 用途配置、路线排产配置和缺失人工资源数据；禁止扩展写入无关租户或业务表。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 涉及 tenant `1` 路线配置、排产策略或资源绑定写入前，必须先记录 `GREEN: experience-preflight -> PASS`。
  - PowerShell/SQL/Markdown 中文读写必须显式 UTF-8。
  - 本轮后端改动必须让正式配置缺失时显式暴露前置，而不是悄悄回退到隐藏默认值。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式工作台配置读取与正式数据补齐路径替代隐藏默认值。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 后端创建或补齐排产配置时不再写死数值 -> Given 路线工序缺少 SCHEDULE 用途配置或排产策略 / When 系统执行正式补齐逻辑 / Then 读取值必须来自正式配置存储或显式请求数据，不得再写死小时产能、班次小时或人工数量。`
- `BDD: 四条目标路线补齐后通过路线前置 -> Given 当前 4 条目标路线存在缺失用途配置、排产策略或人工资源 / When 后端按正式默认值完成补齐 / Then 当前入池/预检不再因为缺排产策略或缺用途配置而阻断。`

## 里程碑

1. M1：建立后端任务文档与执行日志。`COMPLETED`
2. M2：写后端 RED 单测与静态契约。`COMPLETED`
3. M3：实现正式默认配置读取、隐藏默认值移除与必要数据补齐。`COMPLETED`
4. M4：跑后端验证并回填证据。`COMPLETED`

## 预期验证

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProRouteServiceImplTest,MesProSchedulerWorkbenchServiceImplTest,MesProRouteScheduleConfigServiceTest test`

## 当前阻塞

- 无。

## 完成结果

- 工作台正式默认排产配置已扩展并持久化到 `infra_config`，后端不再依赖隐藏 `hourlyCapacity=1` 等硬编码默认值。
- `MesProRouteServiceImpl` 自动补齐、`MesProRouteScheduleConfigServiceImpl` 保存规范化和夜班规则校验均已按正式默认配置收口。
- 通过正式接口在 `tenant_id=1` 为 4 条目标路线创建了 `ROUTE_EDIT` eDHR 对象级权限作用域，解除 `admin` 保存路线排产用途时的对象级权限阻塞。
- 通过正式接口补齐后端真实数据：
  - `route_id=900021`：`SCHEDULE` 工序配置 `30` 条，排产配置 `30` 条。
  - `route_id=900022`：`SCHEDULE` 工序配置 `21` 条，排产配置 `21` 条。
  - `route_id=900025`：`SCHEDULE` 工序配置 `24` 条，排产配置 `24` 条。
  - `route_id=900026`：`SCHEDULE` 工序配置 `26` 条，排产配置 `26` 条。
- 真实库只读复核确认：这 4 条路线的排产配置均为 `capacity_mode='FINITE_HOURLY'`、`hourly_capacity=30`、`night_shift_enabled=0`。
