# 任务：DCC 基础数据迁入全局基础数据子入口

## 任务目标

- 调整 DCC 菜单种子，使 `项目代码` 与 `产品目录` 挂到全局 `基础数据` 菜单下作为两个独立页面入口。
- 保持产品目录接口与项目代码接口权限复用现状，不新增数据库业务表，只迁移菜单结构和路由挂载关系。
- 保证动态菜单返回结构能驱动前端显示全局基础数据子入口。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 backend 相关任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-dcc-basic-data-product-catalog-tab\task.md`
- 状态：`COMPLETED`
- 处理：上一个后端任务已完成产品目录接口；当前只在其基础上调整菜单种子与 schema 契约。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\agent-memory\project-error-prevention.md`
- 适用强制门禁：
  - 菜单结构调整必须走正式 SQL 种子与 schema 测试，不允许只靠手改库或前端假路由。
  - 不新增 fallback 菜单，不保留旧 `DCC基础数据` 页面级菜单和新全局基础数据子入口并行的长期双结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。正式迁移到全局基础数据子入口，不保留旧页面 tab 入口作为长期兜底。
- `是否从根因和长期维护角度解决`：是。通过 system_menu 种子和 schema 测试明确菜单树层级。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: DCC 菜单挂到全局基础数据下 -> Given DCC 菜单 SQL 种子执行完成 / When 动态菜单返回基础数据树 / Then DCC项目代码 与 DCC产品目录 作为全局基础数据子菜单返回。`
- `BDD: 项目代码和产品目录为独立页面菜单 -> Given 菜单树加载到前端 / When 用户点击两个子菜单 / Then 各自指向独立 component/path，不再共享同一个页面内 tab 壳。`

## 里程碑

1. M1：建立后端任务台账并补 DCC schema RED 断言。`COMPLETED`
2. M2：调整菜单 SQL 种子与断言。`COMPLETED`
3. M3：运行 DCC schema 定向验证并回写证据。`COMPLETED`

## 预期验证

- `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test`
