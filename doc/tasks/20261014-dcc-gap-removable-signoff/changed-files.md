# G04 最终文件清单

## A32 + R09 当前增量（前轮已接收记录保留在下方）
只改4份新增获授前端生产源，原BE全部停写：
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue：CUSTOM草稿不要求公共after；当前编制人scope身份；确认注入真实文控账号；保存期间上下文变化拒绝；项目/类型切换清旧配置/预检票据/原关联；旧PUBLIC/DELAYED/附件处理链保留。
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts：payload核对current custom两账号/唯一部门与正式selection绑定；partial/null草稿SAVE保留，不要求PublicRouteID；CUSTOM确认行展示文控/来源/本次矩阵及完整原有业务事实。
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/approval-matrix.ts：CUSTOM PREVIEW/RESERVED canonical/context/文件轮次/实际选择与精确prepared身份校验；selection局部扩展正式selectedSignoffDepartmentIds，供实际payload核对，不生成token、不认证服务器HMAC、不推断人员。
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/signoff-departments.ts（R09）：PUBLIC原组guard接受登记V1/V2，custom明确来源按其配置展示；派生V1/其余原组/规则校验不改。

专属新增：tests/unit/dcc-upload-a32-fixture.cjs、dcc-upload-a32-contract.test.cjs。所属/受影响现测试更新：dcc-upload-application-page、g03-page、g03-custom-state、signoff-departments、ic05-application-options、runtime-approval-groups（含R09）、w03-safety、project-folder-context、g12-optional-folder-reason。全部都在IntRuoyiFronted/tests/unit/，不修改共享dcc-upload-test-loader.cjs。G03 DetailPanel/test只读消费回归，不写。

改前3源hash与G03 upload-three-handback.json全MATCH，R09 helper改前hash另留；见a32-upload-before.json。最终4生产+11测试精确路径/hash见a32-upload-after.json，15/15已核MATCH。a32-r09-verification.json为260项完整命令/统计/稳定性证据；临时日志留供Rootreview，非生产资产。

## 生产：仅三份已授权文件
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccPublicMatrixNativeDerivation.java：project(null)取正式全部会签部门默认；显式名单移除原必选保留要求，补具体非法/空原因；原公共规则及ALL会签保留。
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolver.java：显式空部门列表在目录读取前以指定提示拒绝。
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/signoff-departments.ts：指定空提示；仅 SIGNOFF 原必需组允许本次不参与，其他原组守卫不变。

## 专属测试
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicMatrixNativeDerivationTest.java：更新不可删与历史可选默认阻断过期预期，增移除/增补/空/非法身份、混合及全可选默认、公共零变化。
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolverTest.java：现有专属测试增加公共来源实际名单解析/同负责人多部门/空提示。
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRemovableSignoffContractTest.java：新增6项当前PUBLIC Workflow/预检/Resolver/技术HMAC/最终证据门禁软件合同，替身端口明确。
- IntRuoyiFronted/tests/unit/dcc-upload-removable-signoff.test.cjs：新增任务专属五项实际 helper 行为测试。
- IntRuoyiFronted/tests/unit/dcc-upload-signoff-departments.test.cjs：空提示精确断言；新增实际页面首次默认A/B及后续B/C独立下发回归。
- runtime-approval-groups.test.cjs 为只读回归，未改共享 fixture。

## 任务记录
所有自有记录与runner位于doc/tasks/20261014-dcc-gap-removable-signoff/：baseline.json改前SHA256，final-source-manifest.json为8份生产/测试最终hash，private-verification-manifest.json为各阶段编译源hash/统计。BE相邻76/76与FE26/26已通过。phase目录为可再生私有输出。

## 只读编译/回归
当前Workflow、Readiness、Token、FrozenApprovalSignatures、BPM创建DTO仅编译进入自有目录，未修改；FrozenApprovalSignaturesTest、NativeSupervisorSourceTest、RouteReadinessServiceTest只读回归。其余依赖classpath不作为完整源码证明。

## 范围外
没有改 upload/index.vue、Workflow、公共 fixture、VO/DO/API/SQL/BPMN，也未写其他 G 编号资产；当前工作区预存差异不属于本任务。
