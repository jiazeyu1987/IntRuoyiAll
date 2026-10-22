# Verification Report

## Result
PASS: 用户给出的 5 条分析中，1、2、3、5 经源码核对成立并已修复；第 4 条不按缺陷处理，原因是当前产品边界明确为上传页创建 WORKING 受控文件，再由浏览页显式提交审批。

## Fixed Findings
- P1 驳回事件覆盖已生效状态：新增流程实例、流程定义、审批中状态和数据库 CAS 更新保护；ACTIVE/READY_TO_PUBLISH/FINALIZATION_FAILED 等已越过驳回窗口的延迟事件只作为重放忽略，不再写 REJECTED。
- P1 升版孤立版本：检出时锁 master 后扫描同 master 其它版本；存在 WORKING、审批中、待发布、终结中等未完成版本时，以 CONTROLLED_FILE_WORKFLOW_IN_PROGRESS 阻断。
- P1 分配范围绕过查看权限：受控浏览在 active assignment 命中后仍执行 canAccessQuery；分配范围只收窄候选，不再单独授予文件名可见性。
- P2 升版权限只允许原申请人：后端允许项目 OWNER 对 ACTIVE/SUPERSEDED 正式基线发起检出升版，工作迭代仍限定 requester；前端检出按钮改为读取 MAJOR_REVISION 动作投影并保留 requester 工作迭代能力。

## Not Changed
- P1 上传流程闭环：未修改。当前 upload/index.vue 调 createWorkingControlledFile，后端接口语义为创建 WORKING 受控文件；页面提示到浏览页提交审批。这与当前“上传时禁止走升版路线、版本变更通过检入检出、工作迭代显式送审”的代码边界一致，不应自动送审。

## Verification
- RED: 任务新增 DccControlledFileFinalizationServiceImplTest / DccControlledFileQueryServiceTest / dcc-static-014-browser-major-revision-owner-action-contract.spec.cjs 回归，覆盖驳回重放、驳回流程错配、驳回 CAS、未完成版本阻断、项目 OWNER 检出和前端按钮投影。
- GREEN: C:\IntRuoyiAll-int_main\IntRuoyiBackend 下执行 `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，结果 BUILD SUCCESS，195 tests, 0 failures, 0 errors, 0 skipped。
- GREEN: C:\IntRuoyiAll-int_main 下执行 `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-014-browser-major-revision-owner-action-contract.spec.cjs`，结果 DCC-STATIC-014 browser major revision owner action contract PASS。
- REGRESSION: `git diff --check` 通过，仅输出 CRLF 工作区提示，无 whitespace error。

## Scope
- 未执行 E2E、未启动/重启服务、未写数据库、未操作远程服务器。
