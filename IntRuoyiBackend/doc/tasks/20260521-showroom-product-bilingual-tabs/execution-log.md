# 执行日志：展厅产品基础/详细信息双语 Tab 与英文语音编辑（后端）

BDD: 产品 revision 持久化并返回英文字段 -> Given 用户保存产品基础信息或详细信息草稿 / When 后端写入并再次读取产品 detail / Then `showroom_product_revision` 必须持久化新增 `*_en` 字段，并在产品 detail / 审批 / diff 回显中原样返回。

BDD: 产品翻译接口基于当前中文草稿返回英文结果 -> Given 用户提交当前产品中文名称、中文字段和可选中文讲解稿 / When 调用 `POST /showroom/product/translate-fields-to-en` / Then 后端必须调用真实翻译能力返回 `nameEn`、各 `*_en` 字段和可选 `narrationScriptEn`，缺少至少一段可翻译中文内容时必须显式失败。

BDD: 产品生成语音使用当前英文讲解稿 -> Given 当前 revision 已存在中文讲解稿和用户手改后的英文讲解稿 draft / When 调用产品语音生成链路 / Then 后端必须直接使用当前 EN 讲解稿生成英文语音，不得重新翻译中文覆盖英文草稿。

BDD: 产品发布要求当前 revision 同时具备中英文讲解稿 -> Given 用户尝试发布产品当前 revision / When 当前 revision 缺少英文讲解稿或英文讲解稿为空 / Then 后端必须 fail-fast，显式报出当前 revision 缺少英文讲解稿，不得发布时再自动补翻译。

BLOCKED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomFoundationContractTest,ShowroomPersistentContentServiceTest,ShowroomHttpApiIntegrationTest#productFieldTranslationShouldTranslateChineseFieldsAndNarrationIntoEnglishDrafts+publicityPublishProductShouldFailWhenCurrentRevisionEnglishNarrationMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL-FAST，当前工作区 `yudao-module-showroom` 主源码在执行本次新用例前就已编译失败，错误集中在 `ShowroomPersistentContentService.java` 引用现有 DO 的 lombok getter/setter 解析失败。

BLOCKED: `mvn clean -pl yudao-module-showroom -am "-DskipTests" compile` -> FAIL-FAST，`ShowroomPersistentContentService.java` 在主源码编译阶段就报大量 `getId / getCurrentRevisionId / setStatus / builder` 不可见，说明阻塞不在测试代码而在当前工作区的 backend 主源码编译链。

GREEN: `rg -n "target_market_en|pipeline_layout_en|registration_certificate_en|clinical_effect_en|fim_status_en|product/translate-fields-to-en|sourceRevisionId|requireProductNarrationPairForRevision|translateProductFieldsToEn" ...` -> PASS，源码核对已确认 schema、DO、字段目录、显示标签、产品翻译路由与当前 revision 双语讲解稿链路均已落地。

RED: live `SHOW COLUMNS FROM showroom_product_revision` -> FAIL，真实运行库只有 `target_market / pipeline_layout / registration_certificate / indication_content / core_selling_points / model_specification / cover_image / clinical_effect / fim_status`，缺少全部本次 `*_en` 列，前端页面报 `Unknown column 'target_market_en' in 'field list'`。

GREEN: live `ALTER TABLE showroom_product_revision ADD COLUMN target_market_en TEXT NULL AFTER target_market, ADD COLUMN pipeline_layout_en TEXT NULL AFTER pipeline_layout, ADD COLUMN registration_certificate_en TEXT NULL AFTER registration_certificate, ADD COLUMN indication_content_en TEXT NULL AFTER indication_content, ADD COLUMN core_selling_points_en TEXT NULL AFTER core_selling_points, ADD COLUMN model_specification_en TEXT NULL AFTER model_specification, ADD COLUMN clinical_effect_en TEXT NULL AFTER clinical_effect, ADD COLUMN fim_status_en VARCHAR(255) NULL AFTER fim_status` -> PASS，本机 `23306` 运行库已补齐本次 8 个产品英文列。

GREEN: live `SHOW COLUMNS FROM showroom_product_revision` -> PASS，结果已包含 `target_market_en / pipeline_layout_en / registration_certificate_en / indication_content_en / core_selling_points_en / model_specification_en / clinical_effect_en / fim_status_en`，且总行数保持 `301`。

GREEN: 真实登录后 `GET /admin-api/showroom/product/page?pageNo=1&pageSize=1` -> PASS，返回 `code=0` 与真实产品数据，不再复现 `Unknown column 'target_market_en'`。
