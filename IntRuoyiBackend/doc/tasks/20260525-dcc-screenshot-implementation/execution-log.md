# DCC 截图需求实现执行日志

BDD: 首版默认规则实现截图需求 -> Given 用户授权 Codex 按合理默认规则关闭待确认项 / When 进入 DCC 截图需求实现 / Then 每个默认规则必须写入任务文档，并通过 RED -> GREEN -> REGRESSION 验证后才可提交。

GREEN: 实现 worktree 创建 -> PASS，后端和前端均从 `task/20260525-dcc-requirements-analysis` 创建同名分支 `task/20260525-dcc-screenshot-implementation`。

GREEN: 任务文档创建 -> PASS，已创建 request-analysis、PRD、dev-plan、test-plan、task-state、execution-log、test-report。

## T1 - 后端受控文件元数据与下载基础

BDD: T1 后端基础字段与下载行为 -> Given 申请人提交 DCC 受控文件或用户下载 DCC 文件 / When 图纸源文件缺 PDF、产品编号非法、未确认非受控提醒、体系记录下载或现行文件存在待审批新版本 / Then 系统必须 fail fast 或返回正确状态，不得绕过审计和权限。

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败：缺少 `drawingPdfFileId/productCode/needTraining/processType/modifying` 字段、下载确认参数和新错误码。

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，64 tests passed，覆盖 AC-01、AC-02、AC-03、AC-04、AC-05。

REGRESSION: `python -m pytest script/tests/test_dcc_screenshot_t1_sql.py` -> PASS，确认 T1 SQL 迁移包含新增字段，满足仓库脚本测试门禁。

Changed paths:

- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryService.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImplTest.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePreviewDownloadApiTest.java`
- `script/tests/test_dcc_screenshot_t1_sql.py`
- `sql/mysql/20260525_dcc_screenshot_t1.sql`

Implemented behavior:

- 受控审批提交支持源文件、图纸 PDF、14 位产品编号、是否培训和流程类型字段。
- 图纸源文件缺少 PDF 时拒绝提交。
- 产品编号不满足 14 位字母数字时拒绝提交。
- 下载接口要求 `nonControlledWarningConfirmed=true`，未确认则拒绝并记录 `WARNING_UNCONFIRMED`。
- `fileNumber` 前 6 位为 `INT/RE` 且状态为 `ACTIVE` 的文件允许登录用户下载，仍要求下载确认与留痕。
- 查询响应返回 `modifying`、`currentActiveVersionNo` 和新增字段。

## T2 - 前端上传、列表和下载入口

BDD: T2 前端上传和下载入口 -> Given 申请人上传 DCC 受控文件或用户下载受控文件 / When 填写产品编号、培训要求、图纸 PDF 或触发下载 / Then 前端必须先校验并按后端契约提交，下载必须先确认非受控提醒。

RED: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> FAIL，预期失败：缺少产品编号、培训要求、图纸 PDF 上传、修改中标识和下载确认契约。

GREEN: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS，4 tests passed。

REGRESSION: `node scripts/dcc-controlled-file-download-auth.test.mjs` -> PASS，3 tests passed；`node scripts/dcc-controlled-browser-version-selector.test.mjs` -> PASS，3 tests passed。

GREEN: `pnpm ts:check` -> PASS，前端类型检查通过。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260525-dcc-screenshot-implementation/frontend-feature-evidence.md` -> PASS。

Changed paths:

- `src/api/dcc/controlledFile/workflow.ts`
- `src/views/dcc/controlled-file/upload/index.vue`
- `src/views/dcc/controlled-file/upload/submitter.ts`
- `src/views/dcc/controlled-file/mine/index.vue`
- `src/views/dcc/controlled-file/browser/index.vue`
- `src/views/dcc/controlled-file/detail/index.vue`
- `scripts/dcc-screenshot-t2-frontend.test.mjs`
- `doc/tasks/20260525-dcc-screenshot-implementation/frontend-feature-evidence.md`

Implemented behavior:

- 上传表单提交产品编号、是否需要培训、源文件和图纸 PDF。
- 图纸源文件未上传配套 PDF 时前端阻止提交。
- 列表和详情显示 `修改中` 标识。
- 下载入口统一先展示非受控文件提醒，再调用带确认参数的后端下载接口。

## T3 - 后端 DCC 流程动作与第四节点门禁

BDD: T3 DCC 流程动作和第四节点门禁 -> Given 审批人处理 DCC 受控文件任务 / When 执行回退、转交、加签、申请人选择会签人或第四节点文控审批 / Then 所有动作必须经 DCC 封装接口校验、签名留痕并同步 BPM；第四节点缺受控章 PDF 或必需培训记录时必须 fail fast。

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileTaskActionApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败：缺少 DCC 回退/转交/加签 VO 和服务接口、第四节点附件字段、培训记录字段和新错误码。

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileTaskActionApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，55 tests passed，覆盖 DCC 封装动作、申请人会签人选择和第四节点门禁。

GREEN: `python -m pytest script\tests\test_dcc_screenshot_t1_sql.py script\tests\test_dcc_screenshot_t3_sql.py` -> PASS，3 tests passed，覆盖 T1/T3 SQL 和 schema 字段。

REGRESSION: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileTaskActionApiTest,DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，77 tests passed。

REGRESSION: `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests passed。首次完整模块测试暴露既有 NAS transfer DO 未进入基础 schema，已补齐幂等建表并复验通过。

REGRESSION: `mvn -pl yudao-module-dcc -am test` -> PASS，211 tests passed。

Changed paths:

- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileApproveTaskReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileCreateSignTaskReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileReturnTaskReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileTransferTaskReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowService.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileTaskActionApiTest.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImplTest.java`
- `yudao-module-dcc/src/test/resources/sql/create_tables.sql`
- `script/tests/test_dcc_screenshot_t3_sql.py`
- `sql/mysql/20260513_dcc_base_schema.sql`
- `sql/mysql/20260525_dcc_screenshot_t3.sql`

Implemented behavior:

- 新增 DCC 专用 `return-task`、`transfer-task`、`sign-task` 接口，统一走 DCC 任务校验、电子签名和 BPM 原生动作。
- 回退后同步 DCC 状态到目标审批节点，并记录“有流程回退，需处理”原因。
- 转交后同步当前节点 route snapshot 处理人，转交用户后续可继续通过 DCC 接口审批。
- 加签后同步当前节点 route snapshot 处理人集合，避免加签用户被 DCC 业务校验挡住。
- 提交时支持申请人选择会签人，只覆盖当前流程实例的 `MATRIX_REVIEW` 快照和 BPM 变量，不改审批矩阵配置。
- 第四节点文控审批前必须上传受控章 PDF；若文件要求培训，则必须同时上传申请人培训记录。
- 详情响应返回 `trainingRecordFileId`，便于前端显示和后续复用。
- 基础 schema 和测试 schema 补齐 T1/T3 字段，并补齐已有 NAS transfer task 建表，使 DCC schema 门禁保持一致。

## T4 - 前端流程动作与第四节点页面

BDD: T4 前端流程动作和第四节点页面 -> Given 审批人在现有 DCC 详情页处理任务 / When 执行回退、转办、加签或第四节点批准 / Then 前端必须调用 DCC 专用接口、收集登录密码，并在第四节点上传盖章 PDF/培训记录；BPM 通用按钮不得绕过 DCC。

GREEN: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> PASS，4 tests passed。

REGRESSION: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS，4 tests passed。

REGRESSION: `node scripts/dcc-controlled-file-download-auth.test.mjs` -> PASS，3 tests passed。

REGRESSION: `node scripts/dcc-controlled-browser-version-selector.test.mjs` -> PASS，3 tests passed。

REGRESSION: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。

Commit: frontend `9fdd0a02 任务: 实现DCC流程动作前端入口`。

## T5 - 发放回执与打印导出

BDD: 电子发放接收人签收 -> Given 受控文件已完成电子发放且当前用户是接收人 / When 接收人在详情页输入登录密码和签收意见 / Then 系统记录接收人的签收时间、意见和电子签名留痕；所有接收人签收后分发状态变为已确认。

BDD: 非接收人不能签收电子发放 -> Given 当前用户不是该电子发放记录的接收人 / When 调用电子发放签收接口 / Then 系统拒绝操作且不写入签名留痕。

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccDistributionReceiptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败：缺少电子发放签收 VO、服务、错误码、签收意见字段和签收接口。

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccDistributionReceiptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests passed。

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccPaperDistributionAckServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败：缺少纸质发放回收状态、回收人/回收时间字段和回收服务接口。

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccPaperDistributionAckServiceTest,DccDistributionReceiptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，9 tests passed，覆盖电子发放签收、非接收人拒绝签收、纸质发放确认和纸质回收。

BDD: 外来文件评审流程类型 -> Given 申请人提交外来文件评审 / When 选择外来文件评审流程类型并查询流程列表 / Then 系统必须复用受控文件审批提交链路保存合法流程类型，并支持按流程类型筛选，不得新建独立流程模块。

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败：缺少流程类型枚举、分页筛选字段、非法流程类型错误码和查询过滤。

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，69 tests passed，覆盖外来文件流程类型保存、非法类型 fail fast、分页查询 processType 过滤和下级目录查询条件保留。

BDD: 密码强度与到期策略 -> Given 系统用户创建、重置或登录 / When 密码少于 8 位、缺少字母数字组合或密码超过 90 天未更新 / Then 系统必须拒绝弱密码或到期登录，并记录明确错误，不得静默放行。

RED: `mvn -pl yudao-module-system -am "-Dtest=AdminUserServiceImplTest,AdminAuthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败：缺少密码更新时间字段、密码策略、弱密码错误码、密码到期登录结果。

GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminUserServiceImplTest,AdminAuthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，62 tests passed，覆盖创建用户、重置密码、个人改密、导入初始化密码、钉钉导入初始化密码和到期登录拒绝。

GREEN: `python -m pytest script\tests\test_system_password_policy_sql.py` -> PASS，2 tests passed，确认 system_users 密码更新时间迁移和测试 schema 字段。

GREEN: `python -m pytest script\tests\test_dcc_screenshot_t1_sql.py script\tests\test_dcc_screenshot_t3_sql.py script\tests\test_dcc_screenshot_t5_sql.py script\tests\test_system_password_policy_sql.py` -> PASS，7 tests passed，覆盖 T1/T3/T5 DCC SQL 和系统密码策略 SQL。

REGRESSION: `mvn -pl yudao-module-system,yudao-module-dcc -am "-Dtest=AdminUserServiceImplTest,AdminAuthServiceImplTest,DccDistributionReceiptServiceImplTest,DccPaperDistributionAckServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，DCC 82 tests passed，System 62 tests passed。

REGRESSION: `mvn -pl yudao-module-system,yudao-module-dcc -am test` -> PASS，System module 482 tests run with 9 skipped and 0 failures/errors，DCC module 219 tests run with 0 failures/errors。

BDD: T5 前端发放回执入口 -> Given 用户进入 DCC 详情页查看发放记录 / When 电子接收人签收、文控确认纸质回收、用户导出或打印发放记录 / Then 页面必须调用 DCC 专用接口并展示签收、回收、导出和打印入口，不得绕过后端签名与发放状态。

RED: `node scripts\dcc-screenshot-t5-frontend.test.mjs` -> FAIL，预期失败：缺少电子签收 API、纸质回收 API、回执导出/打印入口和接收人签收状态展示。

GREEN: `node scripts\dcc-screenshot-t5-frontend.test.mjs` -> PASS，3 tests passed。

REGRESSION: `node scripts\dcc-screenshot-t4-frontend.test.mjs` -> PASS，4 tests passed；`node scripts\dcc-screenshot-t2-frontend.test.mjs` -> PASS，4 tests passed；`node scripts\dcc-controlled-file-download-auth.test.mjs` -> PASS，3 tests passed。

REGRESSION: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。

Commit: frontend `ea324d5d 任务: 实现DCC发放回执前端入口`。

Changed paths:

- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccDistributionReceiptController.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccPaperDistributionController.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileDistributionRecipientAckReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileDistributionRecipientStatusRespVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileDistributionStatusRespVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFilePageReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDistributionDO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDistributionRecipientDO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapper.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/DccControlledFileDistributionStatusEnum.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/DccControlledFileProcessTypeEnum.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccDistributionReceiptService.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccDistributionReceiptServiceImpl.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPaperDistributionAckService.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPaperDistributionAckServiceImpl.java`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/dataobject/user/AdminUserDO.java`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/enums/ErrorCodeConstants.java`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/enums/logger/LoginResultEnum.java`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/auth/AdminAuthServiceImpl.java`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserPasswordPolicy.java`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserServiceImpl.java`
- `src/api/dcc/controlledFile/workflow.ts`
- `src/views/dcc/controlled-file/detail/index.vue`
- `src/views/dcc/controlled-file/detail/presentation.ts`
- `script/tests/test_dcc_screenshot_t5_sql.py`
- `script/tests/test_system_password_policy_sql.py`
- `scripts/dcc-screenshot-t5-frontend.test.mjs`
- `sql/mysql/20260525_dcc_screenshot_t5.sql`
- `sql/mysql/20260525_system_password_policy.sql`

Implemented behavior:

- 电子发放接收人可在 DCC 详情页输入登录密码签收，后端记录接收人签收时间、意见和 `DISTRIBUTION_ACK` 电子签名。
- 非接收人签收、发放记录不属于当前文件、非电子发放等场景均 fail fast，避免越权写入回执。
- 所有电子接收人签收后，发放记录自动更新为 `ACKNOWLEDGED`。
- 纸质发放在确认发放后支持回收确认，记录回收人和回收时间，并将状态更新为 `RECOVERED`。
- 详情页展示电子接收人签收状态、意见、纸质回收状态，并提供发放记录导出和打印入口。
- 外来文件评审复用现有 DCC 受控文件审批链路，通过 `processType=EXTERNAL_REVIEW` 区分，不新建独立模块。
- 用户创建、重置、个人改密、导入和钉钉导入初始化密码均要求至少 8 位且包含字母和数字。
- 用户登录时密码更新时间为空或超过 90 天会拒绝登录并记录密码到期结果。

## T6 - 测试租户真实路径验证

BDD: 测试租户真实提交受控文件 -> Given 测试租户用户 `aoteman` 具备 DCC 上传、查询、审批任务入口和本地 DCC 分类/目录/四层路线前置数据 / When 通过前端 `http://127.0.0.1:8089/dcc/controlled-file/upload` 选择文件类别、上传真实文件并提交审批 / Then 系统必须生成 DCC 受控文件、四层审批快照、Flowable 流程实例和第一层待办，不得使用 mock 或默认成功。

BDD: 测试租户真实审批第一节点 -> Given 提交后的文件处于 `PENDING_DOC_CONTROL_REVIEW` 且当前用户是第一层待办处理人 / When 用户从 `DCC审批任务` 进入详情页，输入登录密码并点击“审核通过” / Then 系统必须校验电子签名权限和登录密码，记录签名留痕，并把文件推进到 `PENDING_MATRIX_REVIEW`。

PRECONDITION: 本地测试租户 122 初始缺少 DCC 受控文件 BPM 流程定义和 `dcc:controlled-file:review` / `dcc:controlled-file:approve` 菜单权限 -> FAIL FAST，上传提交返回 `流程定义不存在`，审批动作返回 `Access Denied`；影响是本地真实路径无法启动或推进审批。处理方式仅限本地测试租户：复用现有 `dcc-controlled-file-approval.bpmn` 给租户 122 写入 Flowable 流程定义和 `bpm_process_definition_info`，新增测试租户按钮权限并清理 Redis 权限缓存，未改源码、未改芋道源码租户数据。

GREEN: `curl.exe --max-time 5 ... http://127.0.0.1:8089/` and `http://127.0.0.1:48089/actuator/health` -> PASS，frontend=200，backend=200。

GREEN: Playwright 真实页面入口验证 -> PASS，`/dcc/controlled-file/upload`、`/browser`、`/mine`、`/distribution` 均可在测试租户打开，无 Access Denied、无接口错误、无控制台错误。

GREEN: Playwright 真实上传提交 -> PASS，测试租户 `aoteman/admin123` 在 `DCC受控上传` 选择 `Codex Local DCC Category`，上传 `codex-local-trial-225424.txt`，提交后跳转 `DCC我的文件`；接口 `/admin-api/dcc/controlled-files/upload-preview` 和 `/admin-api/dcc/controlled-files/submit` 均返回 200，提交返回文件 id `2054545668044044040`。

GREEN: 数据库落库核验 -> PASS，`dcc_controlled_file` 记录 `COD-225424` 状态为 `PENDING_DOC_CONTROL_REVIEW`，`process_instance_id=a563e477-5849-11f1-a0ef-00155d615441`；`dcc_controlled_file_route_snapshot` 生成 `DOC_CONTROL_REVIEW`、`MATRIX_REVIEW`、`MATRIX_APPROVAL`、`DOC_CONTROL_APPROVAL` 四层快照；`act_ru_task` 生成第一层 `文控审核` 待办，处理人为用户 `113`，流程定义为测试租户 `dcc-controlled-file-approval:1:codex122`。

GREEN: Playwright 第一层审批 -> PASS，用户从 `DCC审批任务` 进入详情，输入登录密码并点击“审核通过”，`/admin-api/dcc/controlled-files/2054545668044044040/approve-task` 返回 200 和 `data=true`；详情页刷新后状态变为 `待会签审核`。

GREEN: 审批落库核验 -> PASS，`dcc_controlled_file.status=PENDING_MATRIX_REVIEW`，`act_ru_task` 当前待办为 `审核会签` / `MATRIX_REVIEW` / assignee `113`，`dcc_controlled_file_signature` 写入第一层 `APPROVE` 电子签名留痕，comment=`Codex T6 approves first stage`。

CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-dcc-screenshot-implementation --mode preview` -> BLOCKED for apply/merge only，保留任务目录全部正式文档且无待删除文件；阻塞原因为主 worktree `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 非干净状态且当前分支不能快进合并到 `int_main`。本轮未执行 apply、未删除 worktree，保留 8089/48089 给用户试用。
