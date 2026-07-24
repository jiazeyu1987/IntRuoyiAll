# 任务：补齐 NAS 转移其他模板类别数据

## 任务目标

- 为 DCC 文件类别补齐真实启用类别 `其他`，供 `NAS 管理 -> 转移到 DCC` 作为默认模板类别。
- `其他` 的初始治理配置从启用的 `产品技术要求` 复制。
- 如果缺少启用的 `产品技术要求`，初始化必须失败，不创建空规则类别。

## 前序任务检查

- 后端最近可见任务中 `generate-product-020-showroom-cover` 已标记 Blocked，属于图片生成产物阻塞，和本次 DCC 后端/SQL 变更无共享代码路径。
- 本任务只修改 DCC 数据初始化脚本、相关测试和证据。

## BDD 场景

- BDD: 初始化其他模板类别 -> Given 运行库存在启用的 `产品技术要求` 类别及其治理规则 / When 执行初始化 SQL / Then 系统创建或补齐启用的 `其他` 类别，并复制描述、分发要求、培训要求、权限规则、分发规则、培训规则、审批路线和审批节点。
- BDD: 缺少源模板时失败 -> Given 运行库不存在启用的 `产品技术要求` / When 执行初始化 SQL / Then SQL 必须失败并提示缺失源模板，不得创建空治理规则的 `其他`。

## 里程碑

- [x] M1：建立任务文档并确认前序任务边界。
- [x] M2：新增先失败的数据初始化验证。
- [x] M3：新增幂等 MySQL 初始化脚本。
- [x] M4：运行目标测试、记录 RED/GREEN，并提交后端仓库改动。

## 预期验证

- RED：新增 SQL 内容验证在脚本不存在/不完整时失败。
- GREEN：SQL 内容验证通过。
- GREEN：DCC 类别/转移相关测试通过。
- GREEN：本地临时 MySQL 库演练通过，覆盖两个租户各自从 `产品技术要求` 复制到 `其他` 并重复执行不重复插入。
- GREEN：database schema evidence 校验通过。
- GREEN：task-closeout-cleanup 预览通过。

## 当前状态

- 状态：completed。
- 已完成：任务文档初始化；新增 SQL 内容验证；新增按租户幂等补齐 `其他` 类别的 MySQL 脚本；DCC Java 测试、SQL 静态测试、临时 MySQL 演练通过。
- 阻塞：无。

## Current Status

Completed.

## 最终验证

- RED：`node script/tests/dcc-other-template-sql.test.mjs` -> FAIL，目标 SQL 文件不存在。
- GREEN：`node script/tests/dcc-other-template-sql.test.mjs` -> PASS。
- GREEN：临时 MySQL 演练 -> PASS，在一次性库 `codex_dcc_other_template_20260526` 中为 `tenant_id=0` 和 `tenant_id=1` 分别生成 `其他`，复制权限、分发、培训、审批路线和节点，重复执行结果不变，并已删除临时库。
- GREEN：`mvn -pl yudao-module-dcc "-Dtest=DccFileCategoryAdminServiceImplTest,DccControlledFileNasTransferServiceTest" test` -> PASS。
