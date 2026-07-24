# 任务：DCC 单文件基础信息维护后端

## 任务目标

为 DCC 受控文件增加文控角色直改基础信息的后端能力。文控系统角色 code 为 `doc_control` 的账号可以修改单个文件的产品名称、文件名称、产品编号、文件编号、文件类别和受控目录；接口不启动 BPM、不生成审批任务、不改变文件状态、不移动真实文件存储位置。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-nas-menu-garbled-title/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改 DCC 受控文件元数据、数据库迁移、接口和测试。

## BDD 场景

- BDD: 文控直改单文件基础信息 -> Given 用户具备系统角色 `doc_control` 且文件存在 / When 调用单文件基础信息维护接口 / Then 后端更新产品名称、文件名称、产品编号、文件编号、文件类别和受控目录，不启动 BPM、不改变状态。
- BDD: 非文控账号被拒绝 -> Given 用户没有系统角色 `doc_control` / When 调用单文件基础信息维护接口 / Then 请求被拒绝，不更新受控文件。
- BDD: 目录必须位于类别绑定范围 -> Given 文控选择的受控目录不在目标文件类别绑定目录范围内 / When 保存基础信息 / Then 后端明确失败，不更新文件。
- BDD: 文件链冲突必须失败 -> Given 目标类别和文件名已存在不兼容的文件编号、版本或当前有效文件 / When 保存基础信息 / Then 后端明确失败，不合并或降级处理。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：新增 RED 后端测试，覆盖接口、权限、字段持久化和冲突失败。
- [x] M3：实现数据库迁移、DO/VO/ReqVO、服务和控制器接口。
- [x] M4：运行目标后端测试和相关 DCC 回归测试。
- [x] M5：记录证据、运行收尾预览并提交本任务后端改动。

## Expected Verification

- RED/GREEN：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- GREEN：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- GREEN：backend/database evidence validators。
- GREEN：`git diff --check`。
- GREEN：`python -m pytest script/tests/test_dcc_sql_scripts.py -q`。
- GREEN：task-closeout-cleanup 预览。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少 `doc_control` 角色、目标类别、目录绑定或文件链前置条件时直接失败。
- `是否从根因和长期维护角度解决`：是。新增明确接口和元数据服务，维护文件主档链路一致性，不通过前端隐藏或审批权限冒充文控。
- `是否存在临时补丁或绕过`：否。不修改真实文件路径，不启动审批流程，不用 mock 成功。

## 当前状态

completed

## 当前证据

- RED：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，原因符合预期：缺少 `DccControlledFileMetadataUpdateReqVO`、`DccControlledFileMetadataUpdateService`、`CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED`、`selectByCategoryIdAndFileName`、`productName` DO/VO 字段。
- GREEN：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，60 tests。
- GREEN：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，116 tests。
- GREEN：backend-api evidence validator -> PASS。
- GREEN：database-schema evidence validator -> PASS。
- GREEN：`git diff --check` -> PASS，仅 CRLF normalization warnings。
- GREEN：task-closeout-cleanup preview -> PASS，无 blocked cleanup paths；本轮仅执行预览并保留任务记录。
- BLOCKED：跨仓正向 Playwright 前置条件探测 -> BLOCKED，本机测试租户启用角色列表缺少角色 code `doc_control`，无法临时赋予 `测试租户/aoteman` 后验证后端 metadata 保存路径。
- BLOCKED：复制 `芋道源码` 文控角色模板探测 -> BLOCKED，源租户角色分页接口未找到 `code=doc_control` 模板角色，无法按复制方案补齐测试租户前置条件。
- GREEN：本机 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` 应用 `sql/mysql/20260604_dcc_controlled_file_product_name.sql` -> PASS，`dcc_controlled_file.product_name` 字段存在。
- GREEN：跨仓正向 Playwright -> PASS，测试租户角色 `910217/doc_control` 生效，`PUT /dcc/controlled-files/{id}/metadata` 保存成功，随后恢复文件 productName 与用户原角色。
- GREEN：测试租户恢复与编码复核 -> PASS，角色 `910217/doc_control` 名称为 `文控` 且 UTF-8 hex 为 `E69687E68EA7`；`测试租户/aoteman` 角色恢复为 `111,910209`；测试文件 `2054545668044046254` 的 `product_name` 恢复为空。
- GREEN：`python -m pytest script/tests/test_dcc_sql_scripts.py -q` -> PASS，8 passed，覆盖 `product_name` 在 base schema、runtime repair、独立迁移和测试 schema 的同步声明。

## 阻塞

- 无未解决阻塞。`芋道源码` 租户角色写入请求因受保护租户边界未执行；正向可写验证已在本机测试租户完成。
