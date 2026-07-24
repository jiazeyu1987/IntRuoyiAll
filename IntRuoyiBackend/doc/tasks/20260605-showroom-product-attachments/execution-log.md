# 执行日志：展厅产品附件版本快照与发布

- BDD: 产品基础附件上传校验 -> Given 管理端上传展厅产品附件 / When 文件扩展名、MIME、大小或类型不符合规则 / Then 后端必须拒绝请求，不创建附件文件记录。
- BDD: 保存草稿生成 revision 附件快照 -> Given 产品基础资料包含附件列表 / When 保存草稿 / Then 新 revision 应保存该列表的附件快照，后续修改草稿不得改写旧 revision。
- BDD: 发布只使用当前 revision 附件 -> Given 产品存在草稿 revision 和已发布 revision / When 手动发布展厅 / Then release 只能包含产品当前已发布 revision 的附件。
- BDD: 发布产物包含附件资产 -> Given 已发布产品 revision 有图片、视频、文本附件 / When 组装展厅 release / Then product-detail document 与 manifest assets 必须包含这些附件。
- BDD: 附件文件缺失发布失败 -> Given 已发布 revision 引用的附件文件缺失或不可读 / When 手动发布展厅 / Then 发布必须 fail-fast，不生成成功 release。

## TDD 记录

- RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductAttachmentTest,ShowroomReleaseWebsiteIndexAssemblyTest" test` -> FAIL，预期原因：附件策略、附件模型、附件 mapper 与 release 附件输出尚未实现。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductAttachmentTest,ShowroomReleaseWebsiteIndexAssemblyTest" test` -> PASS，7 tests passed。
- REGRESSION: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductAttachmentTest,ShowroomReleaseWebsiteIndexAssemblyTest" test` -> PASS，2026-06-05 20:47，7 tests passed。
