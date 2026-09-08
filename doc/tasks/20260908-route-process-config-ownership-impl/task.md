# 20260908-route-process-config-ownership-impl

## Goal
实现“工序配置可编辑内容迁移到工艺路线版本维护”的开发验证：路线版本承载需升版的工序生产配置，活跃订单冻结快照，一线生产和生产组长页面只读取快照，生产组长原“工序配置”页签删除。

## Milestones
- M1: 读取规则、既有文档与代码入口，补齐 BDD / RED 证据。
- M2: 增加路线版本工序损耗原因投影表与活跃订单统一生产配置快照字段。
- M3: 增加路线版本生产配置快照校验与发布冻结链路。
- M4: 调整 JSON / Word 识别同步到候选路线版本配置。
- M5: 调整前端：路线维护承接配置入口，生产组长删除工序配置页签。
- M6: 运行静态、单元、集成与必要页面验证。
- M7: 收尾、提交、推送。

## Expected Verification
- 后端定向测试覆盖新表、新字段、路线版本快照必填校验、发布冻结快照、一线提交只读快照。
- 前端类型检查和定向测试覆盖生产组长页签删除、路线配置入口显示和提交。
- 按项目规则记录 RED / GREEN / REGRESSION 证据。

## Current Status
ready_for_closeout

已完成数据库结构、路线版本生产配置 API、路线快照必填校验、活跃订单冻结快照字段、生产组长“工序配置”页签移除、工艺路线流转图生产配置编辑入口、JSON/Word 识别同步到候选路线版本、一线活跃订单运行配置读取冻结设备/参数/损耗快照与定向验证；本轮已执行运行库正式迁移、真实页面 E2E、实现提交和远端推送。cleanup apply 当前被阻塞：主工作区 `E:\IntRuoyi` 存在其它任务脏改动，且当前分支不能快进合并到 `int_main`，因此本任务保持 `ready_for_closeout`。

## Milestone Status
- M1: done，规则、文档、代码入口、BDD/RED 已记录。
- M2: done，新表、新列、DO、Mapper、H2 fixture 已实现并验证。
- M3: done，路线版本生产配置 API、快照必填校验、活跃订单冻结快照、一线读取冻结快照已实现并验证。
- M4: done，JSON/Word 识别同步已改为要求 schemaVersion=3、正式 routeProcessId、全量覆盖当前路线工序，并原子写入 DRAFT 候选路线版本生产配置快照。
- M5: done，生产组长页签已删除；旧版无页签布局也不会展示或加载“工序配置”模块；工艺路线流转图已提供候选版本生产配置编辑入口。
- M6: done，后端定向测试、旧控制器/报工分配回归、compile、前端类型检查、静态入口检查、evidence validator、diff check、端口守卫和真实页面 E2E 均已通过。
- M7: partially_done，已完成实现提交和推送；cleanup apply 因主工作区脏改动与非快进合并条件暂时阻塞。

## Design Constraints Check
- 不引入 fallback；路线版本配置缺失时 fail fast。
- 批记录、工序开始、表单槽位三条链路继续分离。
- 设备主数据仍不升版，只冻结映射与参数标准。
- 生产组长仅保留不需要升版的运行类维护内容。

## Cleanup Candidates
- doc/tasks/20260908-route-process-config-ownership-impl/route-production-config-real.e2e.cjs
- doc/tasks/20260908-route-process-config-ownership-impl/e2e-artifacts/
