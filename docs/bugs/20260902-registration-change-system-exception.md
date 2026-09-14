# 注册证变更提交系统异常

## Bug

用户在注册证列表点击“变更”，填写批准日期、选择多项变更内容并上传变更批件文件后，点击“确认”时前端弹框显示“系统异常”。

## Expected

有效的变更申请应写入待审批申请并进入正式审批链路；服务端失败时应返回明确业务错误，不应由数据库异常透出为泛化“系统异常”。

## Reproduction

- 页面路径：`http://127.0.0.1:8081` 注册证管理列表，打开证件 `33333333` 的“变更”弹框。
- 输入：批准日期 `2027-09-23`，变更内容包含“产品名称”和“结构组成”，产品名称 `5555555A`，结构组成 `123`，上传变更批件文件。
- 运行态日志：`/admin-api/dcc/registration-certificates/990819202/changes` 抛出 MySQL 错误 `The value specified for generated column 'selected_item_count' in table 'dcc_registration_certificate_change' is not allowed.`

## Root Cause

`dcc_registration_certificate_change.selected_item_count` 在正式 MySQL 迁移中是 `GENERATED ALWAYS AS (JSON_LENGTH(selected_change_types_json)) STORED` 生成列。`DccRegistrationCertificateChangeService` 的 `insertChange` 和 `insertPendingChange` 仍显式 INSERT `selected_item_count`，MySQL 禁止写入生成列，异常被全局处理成“系统异常”。

## Regression Test

新增 `DccRegistrationCertificateChangeSqlContractTest#changeInsertSqlDoesNotWriteGeneratedSelectedItemCount`，静态锁定 `dcc_registration_certificate_change` INSERT SQL 不允许包含生成列 `selected_item_count`。

## RED

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateChangeSqlContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: INSERT SQL 包含 `selected_item_count`。

## GREEN

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateChangeSqlContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateChangeServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 10。
GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateChangeControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2。
GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateBpmIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 13。
GREEN: `node .\tests\registration-certificate-change-confirm-feedback-static.spec.mjs` in `IntRuoyiFronted` -> PASS。
GREEN: `node .\tests\registration-certificate-change-dialog-static.spec.mjs` in `IntRuoyiFronted` -> PASS。
GREEN: `node .\IntRuoyiFronted\tests\registration-certificate-change-approval-upload-static.spec.mjs` in repo root -> PASS。

## Verification

Verification: 定向后端回归、Controller multipart 绑定回归、审批相邻集成回归和前端静态合同均通过；本轮未重启 `int_main` 后端，因此页面刷新前需先部署/重启后端。

## Risk And Regression Scope

风险集中在注册证变更事实表写入。修复只移除生成列的显式 INSERT，保留 `selected_change_types_json`、审批申请、批件文件、变更项、幂等键和审批流启动链路。

## Blockers And Follow-up

- 本轮未重启 `int_main` 后端；页面要看到修复效果，需要后端重新构建并重启到 48081。
- 未执行真实页面写入 E2E；当前验证为日志复现、后端回归和前端静态合同。
