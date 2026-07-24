# 任务：展厅产品集合维护后端保存修复

## 目标

修复展厅产品集合保存时的后端唯一键冲突，使展厅管理页可以在不手工维护顺序的前提下稳定保存当前展厅包含的产品集合。

## 前置任务检查

- 参考上一个后端展厅任务：`ruoyi-vue-pro/doc/tasks/20260519-showroom-excel-init-import/task.md`
- 启动前状态：已完成。
- 影响：可独立开展本次 showroom hall product mapping 保存修复。

## 缺陷摘要

- 前端将“维护产品”改为只维护产品集合后，真实 `PUT /admin-api/showroom/hall/update-product-mapping` 使用当前原样映射保存也会返回唯一键冲突。
- 当前影响：前端即使不改动产品集合，也无法保存现有展厅产品配置。

## 里程碑

- [x] M1：创建后端任务文档并记录真实 blocker。
- [x] M2：补充失败测试，覆盖“原样替换 hall 产品映射也必须成功”的保存语义。
- [x] M3：修复 showroom hall product 替换逻辑。
- [x] M4：运行后端验证并记录结果。

## 预期验证

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomPersistentContentServiceTest,ShowroomHttpApiIntegrationTest" test`

## 当前状态

已完成：hall 产品映射替换逻辑已改为按 hall 物理清理旧关系后重建，新回归测试通过。

## 当前结论

- 已确认根因：`showroom_hall_product` 继承 `BaseDO` 默认逻辑删除，而表上对 `(hall_id, product_id)` 建了唯一键；`replaceHallProductMappings` 之前先逻辑删再插入，同一套映射原样保存也会撞唯一索引。
- 已完成最小修复：新增 `ShowroomHallProductMapper.deleteByHallIdForce`，在替换 hall 产品映射时改为按 `hall_id` 物理删除旧关系，再按新的产品集合重建。
- 已完成验证：新增单测 `hallMappingsShouldAllowSavingTheSameProductSetTwice` 通过，证明同一展厅同一套产品集合可连续保存。
- 已完成运行时验证：重打 `yudao-server.jar` 后，本地 `48081` 已切换到新 runtime copy，并通过真实 API 验证 `update-product-mapping` 原样保存返回 `code=0`。

## 最终验证

- PASS：`mvn -pl yudao-module-showroom -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ShowroomPersistentContentServiceTest#hallMappingsShouldAllowSavingTheSameProductSetTwice" test`
- PASS：`mvn -pl yudao-server -am "-Dmaven.test.skip=true" package`
- PASS：认证后对 `PUT /admin-api/showroom/hall/update-product-mapping` 发送 hall 当前原样映射，返回 `code=0`

## 风险与约束

- 不得通过删除唯一键、改动 live schema 约束或引入 fallback 绕开问题。
- 只允许最小修复现有替换语义，确保 hall 产品映射的原样保存和集合更新都可成功。
