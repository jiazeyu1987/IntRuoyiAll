# G04 可复核与文件归还请求

## A32 + R09 最新正式归还（2026-10-15）
本轮协助已实现并通过开发验证，正式归还Root：upload/index.vue、upload/submitter.ts、upload/approval-matrix.ts、R09 upload/signoff-departments.ts及本轮所属测试。本线程从本回执起停止四文件/测试写入；G03可由Root登记后接后端/Panel最终集成。原BE Derivation/Resolver/G05权限未修改，旧G04 39/76不重跑。

- A32：CUSTOM真实上传handler可SAVE未完整配置草稿，不要求public after/RouteID/baseline；PREVIEW/RESERVED canonical/context/file/round/两账号/唯一会签名单/精确prepared身份比对；服务端token/digest/主管proof原样保留，改输入旧选择失效。首次非空自定义部门复制下发一次，后续独立。
- 实际submit/save/confirmation覆盖完整两账号与文控显示、编制/主管/属性/源/多附件/关联/培训/下发全员；全部上传完成才可提交。取消保填写且零写请求，晚变化拒绝；切项目/类型确认后清旧custom/票据/原关联，取消保留原值。
- R09：PUBLIC原组guard同时接受登记V1/V2，derivation仍DCC_PUBLIC_MATRIX_NATIVE_V1，所有原组/规则/候选断言保留；未知PUBLIC合同不能走旧扁平投影。明确APPLICATION_CUSTOM + custom derivation仍按独立本次配置展示，不把共用V2流程合同冒充PUBLIC矩阵来源。
- fresh开发回归：260/260 PASS，0skip；全部dcc-upload-*.test.cjs及G03当前真实Panel两项消费者回归；真实Vue整页script/template编译与局部正式接口typecheck包含在单元测试内。四份生产源运行前后hash全部稳定；不是全类型/工程build/E2E/后端HMAC/资格验收。

最终四生产SHA256见a32-upload-after.json（15份生产/测试全部已核MATCH）；改前4源hash见a32-upload-before.json。可复现完整命令/260项统计/loghash在a32-r09-verification.json。原39/76与final-source-manifest.json为原G04归还时历史证据，不应用于本轮已获授权变化的helper。

G03接入注意：state.selection现在携同一正式selectedSignoffDepartmentIds绑定信息，payload用第三参数核对actual两账号/名单；G03自拼selection不得丢该字段。scope可传requesterId/attributeRound以加强保存身份的结构相等核验。具体最小type/Panel建议已在integration-notes.md的A32段，不并写其公共文件。本线程只读执行当前G03 Panel恢复/保存/确认2项PASS，未改Panel/测试；后端真实freeze/rework和工程集成仍G03/Root负责。

## 软件阶段结论
2026-10-15：授权修复与开发验证已完成，可由 Root review 后接管 Resolver/派生/FE helper 及专属测试。G04 从本报告起停止写入这些生产文件与测试，供 G01/G03 依 Root 顺序接入。工程 Git 收尾未授权，不标 completed；Maven/部署/E2E/真实签名办理均未声称通过。

## 正式归还回执（2026-10-15）
### Root 接收 A13
Root 已正式核验归还回执并接受默认 union/explicit roster 最小修复；Root 另实跑前端2文件15项PASS（Root回执证据，不计入本线程26项结果）。Resolver由A13正式转G01进行V2接入，Derivation与signoff-helper由Root保护。G04全部源码/测试继续冻结，不再复跑大测试。本报告的8份hash是归还时指纹，转交后的变化与后续manifest复核由Root记录，不能用归还时hash推断后续源码未变。软件Review已接受；完整reactor及工程收尾由Root统一，私有39/76与FE26的验证边界保持。

Root已反馈R02最小null union及explicit删除修复review接受、待最终manifest核对。本线程再次核对final-source-manifest.json的8份文件全部hash匹配，正式归还DccControlledFileApprovalRouteAssigneeResolver.java、DccPublicMatrixNativeDerivation.java及所属专属测试；同批FE signoff-departments.ts与所属测试也已停写。G04不再持写锁、不等待M02；Root可登记转交G01/G03，具体接入顺序仍由Root决定。无需再跑本线程私有大测试，完整reactor由Root最终票统一。

改前hash保留在baseline.json，最终hash及复核证据保留；业务RED、依赖class编译阻塞和自有fixture失败已分别记录，不合并冒称业务失败或完整工程通过。

## 已实现
- 首次 selected=null 从正式启用 SIGNOFF/DEPT 来源完整集合带出默认名单，包括原 ◎/历史▲非必选部门；不把 null 当空、不从人员目录推断默认部门。
- 显式本次名单可删全部原默认后选择新部门，至少一项；空提示为“请至少选择一个会签部门”。不回填已删部门。
- 最终选入部门全部 ALL/100/requireAllApprovals=true；同负责人保留各部门身份，原生技术 HMAC绑定实际名单与正式主管来源。
- 公共原节点/marker/required/候选事实不改；批准/文控规则及资格守卫不弱化。FE仅允许未参与的SIGNOFF来源原组，其他必需组仍必须参与；下发默认复制独立保存。

## 本轮实际证据
|阶段|命令|结果|
|---|---|---|
|删必选 RED|run-private-tests.ps1 -Phase red|31 tests，5预期失败；FE helper 5项2预期失败|
|默认optional RED|run-private-tests.ps1 -Phase default-red|39 tests，4预期失败，包含当前Workflow.PUBLIC预检实际 blocker|
|GREEN|run-private-tests.ps1 -Phase green|39/39 PASS，0 skipped|
|BE相邻回归|run-private-tests.ps1 -Phase regression|76/76 PASS，0 skipped，显式编译来源hash前后一致|
|FE回归|三个专属/相邻Node测试，完整命令见verification-report.md|26/26 PASS|

私有runner完整路径：doc/tasks/20261014-dcc-gap-removable-signoff/run-private-tests.ps1。测试执行当前派生、Resolver、Workflow、Readiness、Token及FrozenApprovalSignatures源码；其余依赖读现有classpath，清单见private-verification-manifest.json，不能推及整个reactor源码。

## 最终生产 SHA256（接管前核对）
- BE/service/route/DccPublicMatrixNativeDerivation.java：d93a7e17c0f0aad81533d45e46480799895e0bbb44a9fa246e51e79e5ebbe4e1
- BE/service/file/DccControlledFileApprovalRouteAssigneeResolver.java：505527cf5f369afd23078881c2cda5e09b3d0cac413ae872fe9b69a2ec85834a
- FE/upload/signoff-departments.ts：c7e891e82468706cdb0597f14c6c7f529710d59939b32b9573a4c97800b8c5ac

以上BE/FE简称相对于对应模块Java根/controlled-file视图根；完整路径及8个生产/测试hash见final-source-manifest.json。若接管时变化，重新review当前源码，不按旧PASS认定。

## 依赖与边界
- G01新执行合同/延迟确认、G03其他类型自定义矩阵不在G04实现范围；后续Root唯一票负责reactor/集成验证。
- B/C完成证据门禁已实测：缺增补C、拒绝/无签名/错轮次/错版本、缺指派及同负责人重复B任务证据均拒绝。该证据为隔离开发合同，不是真实签名服务或真实页面运行验收。
- 未修改upload/index.vue、核心Workflow、公共fixture、DTO、公共批准规则或真实业务库。Git收尾禁止，不自动completed。
