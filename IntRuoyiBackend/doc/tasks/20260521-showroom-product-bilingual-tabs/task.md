# 任务：展厅产品基础/详细信息双语 Tab 与英文语音编辑（后端）

## Goal

为 showroom 产品后台编辑链路补齐真实英文持久化字段、产品英文翻译接口与英文讲解稿发布约束。基础信息与详细信息的英文内容必须真实落库；产品发布与生成语音必须使用当前 revision 已保存的 `ZH / EN` 讲解稿，不得在发布阶段静默重新翻译中文覆盖用户手改英文。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\dal\dataobject\content\ShowroomProductRevisionDO.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\model\ShowroomProductDraft.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\model\ShowroomProductRevision.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\service\ShowroomPersistentContentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\foundation\meta\ShowroomFieldCatalog.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\foundation\meta\ShowroomFieldDisplaySupport.java`
- 与本任务直接相关的 showroom 测试与文档

## Non-Scope

- 不改动前台 `ShowroomDisplayController` / `app-config` 的公开契约。
- 不新增 fallback、兼容旧虚拟字段或默认成功路径。
- 不改变产品 `incomplete / publish required` 规则，除非验证证明现有规则已被英文字段破坏。
- 不顺带修改公司双语链路。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\task.md`
- Status before this task: `Completed with commit-boundary blocker on 2026-05-21`
- Impact: 上一任务已显式记录 controller / runtime 并行脏改动边界；本次继续在同一模块叠加产品双语字段与翻译接口，不回退批量封面模式与并发逻辑。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在公司语音/封面相关未提交改动。
- Impact: 本任务只允许修改 showroom 产品双语字段、翻译接口、发布与讲解稿约束、相关测试与本任务文档，不能覆盖无关并行改动。

## Milestones

- [x] M1：创建任务文档并确认上一同仓任务状态。
- [ ] M2：先补 RED，锁定产品英文字段持久化、翻译接口、发布使用当前 EN 讲解稿、缺少 EN 讲解稿时 fail-fast 的后端行为。
- [ ] M3：完成 schema、DO / model / service / controller / runtime 最小实现。
- [ ] M4：运行 showroom 定向单测与集成测试并记录 GREEN。
- [ ] M5：更新后端证据、执行 closeout preview，并准备同仓提交边界。

## Expected Verification

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-bilingual-tabs\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-bilingual-tabs --mode preview`

## Current Status

Blocked on 2026-05-21.

## Assumptions

- 新增英文产品字段先只服务后台编辑、审批与回显，前台后续另起任务消费。
- 英文字段默认可选，不纳入当前发布必填；`nameEn` 仍保持现有强制项。
- 产品翻译接口直接调用现有 Codex 翻译能力，缺少中文输入时显式失败。

## Completed Work

- 已在正式 schema 与单测 schema 中加入 `target_market_en / pipeline_layout_en / indication_content_en / core_selling_points_en / model_specification_en / registration_certificate_en / clinical_effect_en / fim_status_en`。
- 已补齐 `ShowroomProductRevisionDO`、`ShowroomPersistentContentService`、`ShowroomFieldCatalog`、`ShowroomFieldDisplaySupport` 的英文字段持久化与回读映射。
- 已新增 `/showroom/product/translate-fields-to-en` 路由与 runtime 实现。
- 已把 `generateProductNarrationAudio` / `publishProduct` 切到 `sourceRevisionId + 当前 revision 双语讲解稿` 链路，并删除旧的发布时临时翻译 fallback 方法。
- 已同步更新本任务相关后端测试口径与后端证据文档。
- 已对本机 live MySQL `127.0.0.1:23306/ruoyi-vue-pro.showroom_product_revision` 执行最小补列，补齐 8 个产品英文列，消除运行时 `Unknown column 'target_market_en'`。

## Blockers And Impact

- Blocker: 当前 `yudao-module-showroom` 主源码在进入本次新用例前即编译失败，无法跑 Maven 级 GREEN。
- Impact: 本次后端实现已落到目标文件并做过源码核对，但尚不能给出 Maven 单测/集成测试级放行。

## Final Verification Result

- BLOCKED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomFoundationContractTest,ShowroomPersistentContentServiceTest,ShowroomHttpApiIntegrationTest#productFieldTranslationShouldTranslateChineseFieldsAndNarrationIntoEnglishDrafts+publicityPublishProductShouldFailWhenCurrentRevisionEnglishNarrationMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- BLOCKED: `mvn clean -pl yudao-module-showroom -am "-DskipTests" compile`
- PASS: `rg -n "target_market_en|pipeline_layout_en|registration_certificate_en|clinical_effect_en|fim_status_en|product/translate-fields-to-en|sourceRevisionId|requireProductNarrationPairForRevision|translateProductFieldsToEn" ...`，源码层确认 schema、DO、字段目录、翻译路由和当前 revision 双语讲解稿链路已经写入目标文件。
- PASS: live `SHOW COLUMNS FROM showroom_product_revision`，当前表结构已包含本次 8 个英文列，且 `row_count=301` 未变化。
- PASS: 真实登录后 `GET http://127.0.0.1:48081/admin-api/showroom/product/page?pageNo=1&pageSize=1` 返回 `code=0`，不再复现 `Unknown column 'target_market_en'`。
