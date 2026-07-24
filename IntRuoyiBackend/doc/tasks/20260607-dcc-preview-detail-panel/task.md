# 任务：DCC 基础信息维护权限收紧

## 任务目标

配合前端受控预览右侧详情面板，将 DCC 单文件基础信息维护和产品名称识别的后端授权收紧为仅 `doc_control` 角色。`super_admin` 不再通过这些接口获得文控维护能力。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260606-showroom-product-management-e2e/task.md`
- 状态：`blocked`
- 处理：上一任务是展厅产品管理 E2E 验证，已记录阻塞为展柜公司信息缺失；本任务为 DCC 后端权限收紧，不依赖该展厅验证结果。当前后端仓库存在多项无关 dirty/untracked 文件，本任务不得暂存或提交。

## BDD 场景

- BDD: 文控可维护基础信息 -> Given 用户具备 `doc_control` / When 调用 `PUT /dcc/controlled-files/{id}/metadata` / Then 后端允许进入正式校验并按现有规则更新元数据。
- BDD: 超管不能维护基础信息 -> Given 用户仅具备 `super_admin` 且不具备 `doc_control` / When 调用基础信息维护接口 / Then 后端拒绝，不读取文件、不写数据库。
- BDD: 文控可识别产品名称 -> Given 用户具备 `doc_control` / When 调用 `POST /dcc/controlled-files/{id}/recognize-product-name` / Then 后端允许进入正式识别流程。
- BDD: 超管不能识别产品名称 -> Given 用户仅具备 `super_admin` 且不具备 `doc_control` / When 调用产品名称识别接口 / Then 后端拒绝，不读取文件、不调用 Codex CLI。

## Milestones

- [x] M1：确认上一后端任务状态并建立本任务文档。
- [x] M2：更新 RED controller/service 权限测试。
- [x] M3：收紧 controller 和 service 权限实现。
- [x] M4：运行后端目标测试。
- [x] M5：记录证据、收尾预览并提交本任务后端改动。

## Expected Verification

- RED/GREEN：`mvn -pl yudao-module-dcc -am -Dtest=DccControlledFileMetadataUpdateControllerTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileProductNameRecognitionControllerTest,DccControlledFileProductNameRecognitionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- GREEN：backend API evidence validator。
- GREEN：task-closeout-cleanup 预览。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。权限不足直接拒绝，不静默改用超管、权限点或其他身份。
- `是否从根因和长期维护角度解决`：是。controller 注解和 service 防线同时收紧，避免前端隐藏入口但 API 仍可绕过。
- `是否存在临时补丁或绕过`：否。不新增兼容分支，不保留 `super_admin` 后门。

## 当前状态

completed

## 当前证据

- RED：`mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataUpdateControllerTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileProductNameRecognitionControllerTest,DccControlledFileProductNameRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，controller 仍是 `hasAnyRoles('doc_control','super_admin')`，service 仍调用 `hasAnyRoles(userId, doc_control, super_admin)`。
- GREEN：同一 Maven 命令 -> PASS，13 tests。
- GREEN：backend-api evidence validator -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，delete none，blocked none。

## Cleanup Keep

- `doc/tasks/20260607-dcc-preview-detail-panel/task.md`
- `doc/tasks/20260607-dcc-preview-detail-panel/execution-log.md`
- `doc/tasks/20260607-dcc-preview-detail-panel/backend-api-evidence.md`
