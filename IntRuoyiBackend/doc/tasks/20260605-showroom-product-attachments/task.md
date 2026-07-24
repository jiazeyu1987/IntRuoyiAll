# 任务：展厅产品附件版本快照与发布

## 任务目标

在展厅产品基础资料中支持图片、视频、文本类附件。附件随产品 revision 保存快照，只有已发布产品 revision 会进入手动发布展厅；发布产物必须包含附件资产与产品详情附件文档，缺少文件或文件不可读时直接失败。

## Previous Task Check

- 已检查同仓库未完成任务：`doc/tasks/20260605-dcc-product-name-recognition/task.md`。
- 处理：该任务已因用户切换当前优先级标记 `blocked`；本任务只修改展厅模块、展厅 SQL、展厅测试与本任务文档。

## BDD 场景

- BDD: 产品基础附件上传校验 -> Given 管理端上传展厅产品附件 / When 文件扩展名、MIME、大小或类型不符合规则 / Then 后端必须拒绝请求，不创建附件文件记录。
- BDD: 保存草稿生成 revision 附件快照 -> Given 产品基础资料包含附件列表 / When 保存草稿 / Then 新 revision 应保存该列表的附件快照，后续修改草稿不得改写旧 revision。
- BDD: 发布只使用当前 revision 附件 -> Given 产品存在草稿 revision 和已发布 revision / When 手动发布展厅 / Then release 只能包含产品当前已发布 revision 的附件。
- BDD: 发布产物包含附件资产 -> Given 已发布产品 revision 有图片、视频、文本附件 / When 组装展厅 release / Then product-detail document 与 manifest assets 必须包含这些附件。
- BDD: 附件文件缺失发布失败 -> Given 已发布 revision 引用的附件文件缺失或不可读 / When 手动发布展厅 / Then 发布必须 fail-fast，不生成成功 release。

## 里程碑

- [x] M1：检查前置任务状态，创建任务文档。
- [x] M2：新增 RED 后端/数据库/release 测试。
- [x] M3：实现附件表结构、上传接口和 revision 快照。
- [x] M4：实现 release 附件资产组装与缺失文件 fail-fast。
- [x] M5：运行验证、更新证据并收尾提交。

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductAttachmentTest test`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomReleaseWebsiteIndexAssemblyTest test`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260605-showroom-product-attachments/database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260605-showroom-product-attachments/backend-api-evidence.md`
- `git diff --check -- yudao-module-showroom sql/showroom doc/tasks/20260605-showroom-product-attachments`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。附件文件缺失、类型非法、大小超限、发布读文件失败均直接报错。
- `是否从根因和长期维护角度解决`：是。新增 revision 附件快照表并纳入 release contract，不通过草稿或前端状态绕过发布流程。
- `是否存在临时补丁或绕过`：否。不改受保护文件配置，不写 mock 文件，不在 release 中忽略缺失附件。

## 当前状态

completed

## Current Status

completed

## 完成记录

- 新增 `showroom_product_revision_attachment` SQL、DO、Mapper 与测试表清理。
- 新增展厅产品附件上传接口、校验策略、产品草稿/发布/详情 attachments contract。
- 保存 revision 时持久化附件快照；产品删除时同步清理附件快照。
- release 组装从已发布 revision 读取附件，写入 product-detail document 与 manifest assets；附件文件缺失时 fail-fast。
- 验证通过：`mvn -pl yudao-module-showroom "-Dtest=ShowroomProductAttachmentTest,ShowroomReleaseWebsiteIndexAssemblyTest" test`，7 tests passed。
