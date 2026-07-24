# 执行日志：发布展柜产品矩形布局

- CHECK: 上一后端任务状态 -> PASS，`doc/tasks/20260606-showroom-hall-canvas-layout/task.md` 当前状态为 `completed`。
- BDD: 手动发布携带展柜矩形布局 -> Given 展柜产品已保存合法矩形布局 / When 用户执行手动发布展厅 / Then 发布输出中的对应产品包含 `layoutX/layoutY/layoutWidth/layoutHeight`。
- BDD: 发布阶段拒绝非法布局 -> Given 展柜产品布局字段缺失、非数字或宽高无效 / When 后端组装 Website 发布包 / Then 发布失败并指出非法布局，不得输出默认布局。
- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleaseWebsiteIndexAssemblyTest test` -> FAIL，expected reason：新增布局断言读取到 `null`，缺失布局污染后发布未抛异常。
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleaseWebsiteIndexAssemblyTest test` -> PASS，7 tests passed。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseWebsiteIndexAssemblyTest,ShowroomReleasePublisherServiceTest,ShowroomReleaseDocumentApiTest" test` -> PASS，13 tests passed。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260606-showroom-publish-hall-layout\backend-api-evidence.md` -> PASS。
