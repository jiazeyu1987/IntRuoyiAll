# 任务：DCC 访问规则已绑定目录列表接口与整组删除

## 任务目标

- 为 DCC 访问规则页新增“已绑定目录列表”接口，供前端一次性获取存在访问规则的目录路径摘要。
- 新增删除目录整组访问规则接口，语义为删除某目录下全部访问规则。
- 不修改现有单目录读取/保存访问规则接口契约，不引入 fallback、静默过滤或兼容降级。

## 当前状态

IN_PROGRESS

## 当前剩余阻塞

- 访问规则目录摘要接口、整组删除接口与本次 `/access-rule-directories` 路由冲突回归已完成代码修复，控制器定向回归已通过。
- 但完整后端目标验证仍被仓库现有 `DccDirectoryAdminServiceImplTest` 的 H2 schema 初始化问题阻塞：`src/test/resources/sql/create_tables.sql` 第 50 条 `dcc_controlled_file_batch_recognition_task` 建表语句当前会导致 Spring 上下文初始化失败。
- 该阻塞与本次 DCC 访问规则功能无直接关系；在 H2 基线恢复前，不能把完整后端验证标记为全绿。

## 前一任务检查

- 后端前一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260625-dcc-route-node-stage-type-schema-drift\task.md`
- 状态：`已完成`
- 处理：上一后端任务已完成，不阻塞本次 DCC 访问规则接口扩展。
- 当前后端仓库存在 MES 相关未归属脏改；本任务仅修改 DCC 目录访问规则相关代码、测试与本任务文档，不覆盖其他改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中说明：
  - 本次仅做本机源码与定向单元/控制器测试，不执行真实 E2E、数据库 schema 变更、服务器联调、发布、备份恢复或远程写入，因此当前不触发 `experience-preflight`。
- 适用强制门禁：
  - 后端接口必须 fail-fast；若访问规则引用了不存在目录，列表接口直接抛明确错误，不静默过滤脏数据。
  - 不得通过兼容返回空列表或默认成功掩盖真实数据异常。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。目录缺失或非法状态直接报错，不返回伪空结果。
- `是否从根因和长期维护角度解决`：是。显式补充“已绑定目录列表”查询与整组删除接口，避免前端使用 N 次请求拼装列表。
- `是否存在临时补丁或绕过`：否。不会把新语义塞进旧接口 query 参数里做隐式复用。

## BDD 场景

- `BDD: 管理端可一次获取存在访问规则的目录路径摘要 -> Given 多个目录存在访问规则 and 目录树层级完整 When 调用 access-rule-directories 接口 Then 返回按目录树前序排序的目录摘要列表，包含 id、name、directoryPath。`
- `BDD: 访问规则引用不存在目录时列表接口 fail-fast -> Given dcc_directory_access_rule 中存在 directory_id 指向已删除目录 When 调用 access-rule-directories 接口 Then 接口直接报明确错误，不静默过滤该条规则。`
- `BDD: 删除目录整组访问规则后该目录不再出现在摘要列表 -> Given 某目录存在多条访问规则 When 调用删除整组访问规则接口 Then 该目录所有访问规则被删除，随后摘要列表不再包含该目录。`
- `BDD: 现有单目录访问规则读取与替换接口保持契约不变 -> Given 前端继续调用 /{id}/access-rules GET 与 PUT When 读取或保存规则 Then subjectType、subjectId、canQuery、canPreview、canDownload、active、changeReason 字段语义不变。`

## 里程碑

1. M1：创建后端任务文档并记录门禁、BDD 场景与 RED/GREEN 计划。`COMPLETED`
2. M2：补控制器/服务 RED 回归，锁定新列表与整组删除契约。`COMPLETED`
3. M3：实现新接口、服务逻辑与 fail-fast 校验。`COMPLETED`
4. M4：运行定向单测并补齐证据。`IN_PROGRESS`

## 预期验证

- `mvn -pl yudao-module-dcc -Dtest=DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-dcc-access-rule-bound-directory-list\backend-api-evidence.md`

## Cleanup Keep

- `doc/tasks/20260626-dcc-access-rule-bound-directory-list/task.md`
- `doc/tasks/20260626-dcc-access-rule-bound-directory-list/execution-log.md`
- `doc/tasks/20260626-dcc-access-rule-bound-directory-list/backend-api-evidence.md`
