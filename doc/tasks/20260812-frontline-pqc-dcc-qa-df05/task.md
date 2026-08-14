# 20260812 Frontline PQC DCC QA DF05

## Task Goal

完成 DF05 - DCC directly reads QA regulation relation：复用 mes_qa_inspection_regulation.dcc_project_code_id 作为唯一 DCC-QA 关系，QA 管理页和 DCC 项目代码列表直接基于该关系保存、发布、读取状态和跳转；不创建 DCC 侧绑定表，不通过产品、路线或 MES 工序推算 QA 规程。

## Milestones

- M1 规则与合同确认：completed；已读取 DF05 所需项目规则、监督 dev-plan/test-plan、DF05 设计包和共享接口合同。
- M2 BDD/RED：completed；已记录 DCC 直接管理 QA、DCC 列表批量 QA 状态、后端 resultType 禁止旧枚举的 Given/When/Then，并取得行为 RED。
- M3 GREEN：completed；已按现有页面和服务最小改造前端 API/页面与后端 save/publish 校验。
- M4 Regression：completed；两个 Node 静态合同和 MesQaInspectionRegulationServiceTest 已通过，补充了变更范围和禁止项扫描。
- M5 Closeout：ready；已记录验证证据；未提交、未合并、未删除 worktree、未启动服务、未修改共享业务数据。

## Expected Verification

- node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs
- node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs
- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

## Applicable Gates

- DCC-QA 唯一关系：只使用 regulation.dccProjectCodeId；禁止新增 DCC 侧 QA 绑定表或通过产品、路线、MES 工序推算。
- 前端合同：QA 管理保存一个 DCC payload，DCC 列表当前页一次批量 project-statuses，并处理无权限和过期响应。
- 结果类型合同：QA 页面只允许 BOOLEAN/NUMERIC/TEXT，并保留 SaveReq 完整 item 字段。
- PowerShell/Maven：所有 Maven -D 参数整体加双引号，保留 -pl yudao-module-mes -am。

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以现有正式 DCC ID 字段作为唯一关系，移除产品/路线推算入口。
- 是否存在临时补丁或绕过：否。
