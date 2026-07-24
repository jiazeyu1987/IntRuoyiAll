# 20260613 NAS 本地文件夹导入 DCC 后端与数据库实施

## 任务目标

新增 `POST /dcc/controlled-files/local-folder-import`，接收本地文件夹上传的文件和相对路径，复用 NAS 转移任务的 DCC 目录创建、类别绑定、受控文件提交、轮询和失败报告能力。通过 `source_type` 区分 `NAS` 与 `LOCAL_FOLDER`，本地导入不读取 NAS、不采集 NAS ACL。

## 前置任务检查

- 最近后端任务文档：`20260612-runtime-control-server-host-defaults/task.md`，状态 `COMPLETED`。
- 本任务限定在 `yudao-module-dcc`、DCC MySQL schema、DCC 测试 schema 和本任务证据文件。

## 里程碑

1. M1 文档与审计：创建任务文档，确认现有 NAS 转移 service/controller/mapper/schema 和上传落库能力。
2. M2 RED：新增后端 API/service/schema 测试，复现缺少本地导入契约与 schema 字段。
3. M3 GREEN：新增 schema 字段、VO/controller/service 行为和本地来源任务处理。
4. M4 REGRESSION：运行目标 Maven 测试与证据校验。
5. M5 收尾：记录验证证据，运行收尾清理预览；真实 E2E 通过后再提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ruoyi-vue-pro/doc/tasks/20260613-nas-local-folder-import/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ruoyi-vue-pro/doc/tasks/20260613-nas-local-folder-import/database-schema-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本地导入不回退到 NAS 读取，路径、文件、类别绑定或任务前置条件不满足时 fail fast。
- `是否从根因和长期维护角度解决`：是；以正式 `source_type` 和 `source_file_id` 扩展现有任务模型，避免平行任务系统。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：BLOCKED_E2E。
- 已完成：M1-M4。已新增本地导入 VO、controller endpoint、service 分支、`source_type` / `source_file_id` schema 字段、运行时 SQL、base schema、test schema、后端 service/controller/schema 测试；目标 Maven 测试已通过。
- 剩余阻塞：M5 真实 Playwright E2E 尚未完成。后端 48081 已恢复可访问，但测试租户缺少启用的 DCC 模板类别 `其他`，小型目录探针在业务前置处 fail fast；用户指定目录 `E:\Downloads\1. QMS documents` 包含 962 个文件、约 843.72MB，Playwright filechooser 未能把该目录内容注入浏览器 input（`input.files.length=0`），未进入前端 300MB 校验和后端导入接口。因此未提交本任务改动。

## Cleanup Keep

- doc/tasks/20260613-nas-local-folder-import/backend-api-evidence.md
- doc/tasks/20260613-nas-local-folder-import/database-schema-evidence.md
