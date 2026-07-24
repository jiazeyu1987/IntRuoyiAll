# 任务：产品预览图统一使用封面

## 任务目标

- 将展厅产品前台预览图的后端契约统一为产品 `cover_image`。
- 产品缺少封面时发布和运行时展示配置必须失败并暴露明确错误，不再使用产品 preview asset 兜底。
- 展厅/大厅等非产品对象继续使用独立 preview asset。

## 非目标

- 不修改数据库结构。
- 不删除历史 preview asset 数据。
- 不修改前端仓库；前端存在未完成任务 `20260525-showroom-company-editable-fields`，本次仅记录为后续文案调整阻塞。

## 前置任务检查

- 最近后端任务已完成，后端 worktree 干净。
- 前端仓库存在 in-progress 任务和未提交改动，本任务不触碰前端文件。

## 里程碑

- [x] M1：建立任务记录并确认前置状态。
- [x] M2：补充产品缺封面但有 preview asset 时发布失败的 RED 测试。
- [x] M3：收紧发布组装、运行时 display 配置和版本中心 bundle 逻辑。
- [x] M4：执行目标测试、证据校验和 closeout 预览。
- [x] M5：提交本任务后端改动。

## BDD 场景

- BDD: 产品前台预览图直接使用封面 -> Given 已发布产品存在 `cover_image` / When 发布展厅前台配置 / Then 产品卡片和详情的 `previewImageUrl` 来自该封面。
- BDD: 产品缺封面不得使用 preview asset 兜底 -> Given 已发布产品没有 `cover_image` 但存在 PRODUCT preview asset / When 发布展厅前台配置 / Then 发布失败并提示产品 `cover_image` 缺失。
- BDD: 产品版本中心不要求单独 preview asset -> Given 已发布产品有封面和双语讲解 / When 创建产品版本 bundle / Then bundle 可创建且 `releasePreviewAssetVersionId` 为空。

## 预期验证

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseSourceSnapshotSelectionTest,ShowroomProductCoverBatchTaskServiceTest" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest,ShowroomReleaseSourceSnapshotSelectionTest,ShowroomProductCoverBatchTaskServiceTest,ShowroomReleaseAdminPublishIntegrationTest" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260525-showroom-product-preview-cover-source/backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-showroom-product-preview-cover-source --mode preview`

## 当前状态

- 状态：completed
- 已完成：产品发布包、前台展示、版本中心 bundle、封面批任务均改为产品预览图直接使用 `cover_image`；PRODUCT preview asset 不再作为产品预览图来源或新版本 bundle 必填项。
- 验证结果：目标回归全部通过；全模块 `mvn -pl yudao-module-showroom test` 额外回归发现非本任务阻塞，见 `execution-log.md`。
- 阻塞与影响：前端文案调整被前端未完成任务 `20260525-showroom-company-editable-fields` 阻塞；不影响本次后端契约收紧。全模块回归还存在既有测试夹具缺 Bean 与 SQL 工作目录问题，不影响本次目标验证。

## Cleanup Keep

- `doc/tasks/20260525-showroom-product-preview-cover-source/backend-api-evidence.md`
