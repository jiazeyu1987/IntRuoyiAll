# 任务：DCC 访问规则仅显示手动保存目录

## 任务目标

- 将 `GET /dcc/directories/access-rule-directories` 的语义从“存在任意访问规则行的目录”收紧为“在访问规则页显式保存过且当前仍有访问规则的目录”。
- 在 `dcc_file_directory` 引入目录级手动绑定标记 `access_rule_manually_bound`，避免用 `changeReason` 或父子规则内容相等性猜测来源。
- 保持现有访问规则行结构、访问规则读写接口路径和响应结构不变，不引入 fallback、兼容分支或静默降级。

## 当前状态

COMPLETED

## 前一任务检查

- 后端上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-role-management-split-rename-navigation\task.md`
- 状态：`COMPLETED`
- 处理：上一后端任务已完成，不阻塞本次 DCC 目录手动绑定标记改造。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 本轮优先完成本机 schema、service、controller 与定向回归，不做服务器写入或远端联调。
  - 若后续进入真实登录或真实 E2E，第一条登录相关命令必须先执行 `node scripts/preflight/login-preflight.mjs ...`。
  - 涉及真实 E2E 或登录后写入前，`execution-log.md` 必须先记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不存在基于 `changeReason`、父目录规则比较或空列表默认成功的兼容推断。
- `是否从根因和长期维护角度解决`：是。通过目录级显式持久化标记建模“手动保存目录”来源，避免污染访问规则行契约。
- `是否存在临时补丁或绕过`：否。不做历史回填，不用前端启发式过滤伪造结果。

## BDD 场景

- `BDD: 手动保存目录列表只返回显式绑定目录 -> Given 多个目录存在访问规则且仅部分目录被标记为 access_rule_manually_bound When 调用 access-rule-directories 接口 Then 只返回被标记且当前仍有规则的目录路径摘要。`
- `BDD: 继承目录默认不进入手动保存列表 -> Given 新子目录从父目录克隆了访问规则但未经过访问规则页显式保存 When 调用 access-rule-directories 接口 Then 该子目录不得出现在左侧目录摘要列表中。`
- `BDD: 访问规则页显式保存会建立手动绑定 -> Given 目录当前存在或新建访问规则草稿 When 调用 PUT /{id}/access-rules 成功 Then 目录 access_rule_manually_bound 必须被写为 true。`
- `BDD: 删除整组规则会清除手动绑定 -> Given 某目录已被手动保存且存在访问规则 When 调用 DELETE /{id}/access-rules Then 该目录全部规则被删除且 access_rule_manually_bound 必须被写为 false。`
- `BDD: 访问规则引用不存在目录时仍需 fail-fast -> Given dcc_directory_access_rule 中存在指向不存在目录的记录 When 调用 access-rule-directories 接口 Then 服务直接抛出明确错误，不静默过滤。`

## 里程碑

1. M1：创建任务文档、更新请求命令记录并补 RED 骨架。`COMPLETED`
2. M2：补 schema / service / controller RED 回归与 migration 合同测试。`COMPLETED`
3. M3：实现目录手动绑定字段、后端语义收紧与自动目录默认值。`COMPLETED`
4. M4：运行 GREEN 定向验证、补齐 evidence，并为前端真实验收提供后端基线。`COMPLETED`

## 预期验证

- `mvn -pl yudao-module-dcc -am "-Dtest=DccDirectoryAdminServiceImplTest,DccDirectoryControllerTest,DccControlledFileNasTransferServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest script/tests/test_dcc_directory_access_rule_manual_binding_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-dcc-access-rule-manual-bound-list\database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-dcc-access-rule-manual-bound-list\backend-api-evidence.md`

## 最终验证结果

- PASS：`mvn -pl yudao-module-dcc -am "-Dtest=DccDirectoryAdminServiceImplTest,DccControlledFileNasTransferServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS：`python -X utf8 -m pytest script/tests/test_dcc_directory_access_rule_manual_binding_sql.py -q`
- 说明：后端定向验证已完成，真实页面验收由前端任务继续执行。

## Cleanup Keep

- `doc/tasks/20260626-dcc-access-rule-manual-bound-list/task.md`
- `doc/tasks/20260626-dcc-access-rule-manual-bound-list/execution-log.md`
- `doc/tasks/20260626-dcc-access-rule-manual-bound-list/database-schema-evidence.md`
- `doc/tasks/20260626-dcc-access-rule-manual-bound-list/backend-api-evidence.md`
