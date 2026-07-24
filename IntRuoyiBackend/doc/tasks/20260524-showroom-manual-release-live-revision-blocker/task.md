# 任务：排查手动发布展厅 live product revision 缺失阻塞

## 任务目标

- 定位点击“手动发布展厅”时报错 `SHOWROOM_TARGET_NOT_FOUND: live product revision not found` 的真实根因。
- 判断问题属于后端代码缺陷还是测试/正式租户中的展厅产品 live 数据损坏。
- 在不污染非测试租户数据的前提下，给出可执行修复方案；若属于代码问题则补修复与回归测试。
- 按用户确认的统一策略，将坏产品从 release 组装阶段跳过，而不是让整次发布失败。

## 非目标

- 不修改前端按钮交互。
- 不静默绕过发布失败。
- 不直接改写正式租户或用户未授权的数据。

## 前序任务检查

- 已检查最近同仓任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-dcc-any-file-common-preview\task.md`
- 上一任务状态：`completed`
- 影响：上一任务已完成，不阻塞本次展厅发布阻塞排查。

## 里程碑

- [ ] M1：建立任务记录并确认报错堆栈与触发入口。
- [ ] M2：排查 `showroom_product.current_revision_id` 与 `showroom_product_revision` 的一致性，锁定具体租户/产品。
- [ ] M3：判断是否需要代码修复或数据修复；如为代码问题，按 TDD 补修复。
- [ ] M4：完成验证、更新任务文档和执行日志。

## 预期验证

- 后端日志定位 `publishRelease()` 抛错链路
- 数据核查 SQL：
  - `showroom_product.current_revision_id`
  - `showroom_product_revision.id`
- 如有代码修复：
  - `mvn -pl yudao-module-showroom ... test`

## 当前状态

状态：已完成

## Current Status

Completed

## Completed Work

- 已确认当前手动发布失败不是前端按钮问题，而是后端 release 组装阶段被一条坏产品卡住。
- 已通过数据库核查定位唯一阻塞对象：
  - `hall_01 / 心内介植入展厅`
  - `product_166 / 一次性使用射频房间隔穿刺针`
  - `current_revision_id = NULL`
  - `status = DRAFT_ONLY`
- 已将 release 组装策略统一调整为：
  - 跳过缺少 live revision 的产品
  - 跳过缺少必需发布素材的产品
  - 某个展厅过滤后没有可发布产品时，跳过该展厅
  - 仅当所有展厅/产品都不可发布时，才继续整体报错
- 已继续修复第二个运行态阻塞：
  - `showroom_release_asset` 中同 key asset 若已被逻辑删除，重新发布时改为复活复用
  - 并发或重入下若命中唯一键冲突，改为重新读取现有 asset，而不是整次发布失败
- 已补齐手动发布与版本中心重发相关回归测试，并完成 `yudao-server` 联编打包。

## Final Verification

- 数据核查 SQL -> PASS，当前唯一阻塞产品为 `product_166`，数据库中展厅映射本身仍是多产品：
  - `hall_01=25`
  - `hall_02=28`
  - `hall_03=27`
  - `hall_04=17`
  - `hall_05=10`
  - `hall_06=20`
  - `hall_07=11`
  - `hall_08=27`
- RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldSkipProductsWithoutLiveRevisionAndDropEmptyHalls" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，原逻辑仍在 `requireCurrentProductRevision()` 处直接抛错。
- GREEN: 同上命令 -> PASS，坏产品已被跳过，空展厅被过滤，发布继续成功。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest,ShowroomReleaseAutoPublishServiceTest,ShowroomVersionCenterServiceTest,ShowroomPersistentContentServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- RED: 真实前端手动发布 `POST /admin-api/showroom/release/publish` -> FAIL，报 `showroom_release_asset.uk_showroom_release_asset` 唯一键冲突，具体 asset 为 `product-2-preview/cd5aaa69...`。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldReuseLogicallyDeletedAssetRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS。
- GREEN: 真实前端发布复测 -> PASS，测试租户通过 `http://127.0.0.1:8081/showroom/company` 点击“手动发布展厅”后，请求成功完成，`current release` 已切换到 `20260524T163916Z-e03a7b68bf1a`。

## Note

- 旧的单产品 release `20260524T100623Z-316b86ad1758` 已被新的多产品 release `20260524T163916Z-e03a7b68bf1a` 替换。
- 发布后新 `current release` 为：
  - `documentCount = 166`
  - `assetCount = 506`
  - `installBytes = 669642357`
