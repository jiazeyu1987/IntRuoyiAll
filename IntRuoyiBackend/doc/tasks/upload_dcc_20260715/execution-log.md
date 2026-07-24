# Execution Log

INFO: experience-index -> matched docs/powershell-memory.md, docs/worktree-memory.md, docs/login-access.md, D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md.

BDD: 同编号现行版本自动关联 -> Given 同文件编号存在唯一 ACTIVE 现行版本, When 申请人在提交页输入该编号或后端收到升版/作废提交, Then 系统返回/校验该现行版本并阻止冲突版本链。

BDD: 变更方式提交校验 -> Given 申请人选择新建/升版/作废, When 提交受控文件, Then 后端按变更方式校验现行版本存在性、版本号递增和修改中冲突。

BDD: 文控部门下发范围 -> Given 流程进入文控最终确认, When 文控勾选多个下发部门, Then 后端按最终部门范围生成下发记录和电子接收人任务，部门无接收人时失败。

GREEN: experience-preflight -> PASS, 已读取 PowerShell、worktree、login/E2E、前端样式门禁；本阶段仅进行本机 worktree 代码与测试。

RED: mvn -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_docControlApprovalPersistsSelectedDistributionDepartments test -> EXPECTED FAIL, 请求 VO/服务尚未支持 selectedDistributionDepartmentIds 按部门生成下发记录。

GREEN: mvn.cmd -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_docControlApprovalPersistsSelectedDistributionDepartments test -> PASS, 文控所选部门解析为部门下发记录和接收人。

REGRESSION: mvn.cmd -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest test -> PASS, 75 tests，覆盖文件编号版本链、新建/升版/作废、图纸 PDF、培训记录、文控审批、NAS 无审批导入、撤回重提。

REGRESSION: mvn.cmd -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest,DccExternalFileReviewServiceImplTest,DccMapperXmlValidityTest" test -> PASS, 82 tests。

REGRESSION: mvn.cmd -pl yudao-module-mes "-Dtest=MesMdWorkstationServiceDependencyContractTest,MesWmMaterialStockServiceDependencyContractTest" test -> PASS, 2 tests。

GREEN: python -X utf8 script/tests/test_release_sql_idempotency_contract.py -> PASS, SQL 发布幂等契约通过。

GREEN: worktree-real-e2e-backend-path -> PASS, 测试租户 aoteman 真实页面提交并审批 `CODEX-DCC-DEPT-20260716010719`；最终状态 ACTIVE，后端按第 4 节点 selectedDistributionDepartmentIds 生成下发记录和接收人。
