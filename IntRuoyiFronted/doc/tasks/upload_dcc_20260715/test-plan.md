# 电子文控系统受控文件上传流程测试计划

## Purpose and Scope

本文定义受控文件上传流程的 BDD 场景、严格 TDD 顺序、真实 E2E 路径、测试数据、回归命令和证据模板。范围覆盖申请人上传、文件编号现行版本自动关联、变更方式、部门下发范围、培训记录、文控确认、升版/作废和异常路径。

## Evidence Reviewed

- PRD：`doc/tasks/20260715-electronic-doc-control-upload-flow-prd-plan/prd.md`。
- 开发计划：`doc/tasks/20260715-electronic-doc-control-upload-flow-prd-plan/development-plan.md`。
- 当前上传页、提交模型、后端提交接口、DCC 基础表和现有测试目录。

## Feature Scenarios

- BDD: 新建受控文件 -> Given 同文件编号不存在现行有效版本, When 申请人选择新建并上传可编辑源文件, Then 系统提交审批并记录文件编号、版本和源文件。
- BDD: 升版自动关联现行版本 -> Given 同文件编号存在唯一现行有效版本 V1.0, When 申请人输入该编号并选择升版 V1.1, Then 页面展示现行版本信息并提交升版审批。
- BDD: 作废自动关联原版本路径 -> Given 同文件编号存在现行有效版本, When 申请人选择作废, Then 系统自动关联原版本路径并要求文控后续作废确认。
- BDD: 图纸源文件同步 PDF -> Given 申请人上传 DWG 或 SolidWorks 源文件, When 未上传 PDF, Then 系统阻止提交；When 上传真实 PDF, Then 允许继续。
- BDD: 部门下发范围 -> Given 流程进入文控归档前确认, When 文控勾选多个启用部门并最终确认归档, Then 系统生成对应部门下发记录和电子接收人任务。
- BDD: 培训记录 -> Given 申请人选择需要培训, When 流程到达培训记录节点, Then 申请人必须上传培训记录 PDF 后才能进入文控节点。
- BDD: 回退后再次提交 -> Given 审批人回退流程至申请人, When 申请人修改明细并再次提交, Then 原流程继续流转而不是创建新流程。

## Failure Scenarios

- BDD: 新建编号冲突 -> Given 同文件编号已有现行有效版本, When 申请人选择新建提交, Then 系统阻止并提示改用升版。
- BDD: 升版无现行版本 -> Given 同文件编号没有现行有效版本, When 申请人选择升版提交, Then 系统阻止并提示按新建发起。
- BDD: 版本号不递增 -> Given 现行版本为 V1.0, When 申请人提交 V1.0 或更低版本, Then 系统阻止提交并显示版本错误。
- BDD: 同编号修改中 -> Given 同文件编号已有未完成流程, When 申请人再次提交, Then 系统显示修改中并阻止重复发起。
- BDD: 下发部门无接收人 -> Given 文控勾选部门无法解析接收人, When 文控确认归档, Then 系统阻塞并显示具体部门。
- BDD: 审批路线无处理人 -> Given 文件类别审批矩阵缺失, When 申请人预览路线或提交, Then 系统阻止并提示维护审批路线。
- BDD: 文控处理失败 -> Given 受控章或 PDF 转换失败, When 文控归档, Then 文件进入 `FINALIZATION_FAILED`，不得激活或下发。

## Boundary Scenarios

- BDD: 文件编号大小写和空格 -> Given 输入前后空格或大小写差异, When 查询现行版本, Then 系统按明确规范归一化并展示真实文件编号。
- BDD: 类别变更清空关联 -> Given 已关联现行版本, When 申请人更改文件类别或目录, Then 页面清空原关联并要求重新查询。
- BDD: 多部门混合介质 -> Given 部门下发包含电子和纸质介质, When 文控归档, Then 电子部门生成接收人任务，纸质部门生成待发放记录。
- BDD: 培训和下发联动 -> Given 类别要求培训且文控选择下发部门, When 归档完成, Then 培训任务继承下发部门接收人范围。

## TDD Sequence

| Sequence | Behavior | RED Command | Expected Failures | Minimal Implementation | GREEN Command |
| --- | --- | --- | --- | --- | --- |
| 1 | 文件编号现行版本查询 | `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileCurrentVersionLookupServiceTest" test` | 查询服务和接口不存在 | 增加按文件编号查询服务、VO、Controller | 同命令 PASS |
| 2 | 变更方式提交校验 | `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileChangeTypeSubmissionServiceTest" test` | DTO 无 `changeType`，无法校验新建/升版/作废 | 扩展 DTO、服务校验、错误码 | 同命令 PASS |
| 3 | 下发部门范围 | `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileDistributionScopeServiceTest" test` | 文控确认无部门范围，归档只按类别规则 | 扩展文控确认字段，写入下发记录，接收人解析 | 同命令 PASS |
| 4 | 图纸 PDF 强校验 | `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileDrawingPdfSubmissionTest" test` | 组合场景覆盖不足 | 复用真实 PDF 内容校验并绑定图纸 PDF ticket | 同命令 PASS |
| 5 | 培训记录链路 | `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileTrainingRecordWorkflowTest" test` | 新流程组合下培训记录未覆盖 | 补齐状态流转和权限校验 | 同命令 PASS |
| 6 | 前端现行版本面板 | `node tests/e2e/dcc-controlled-file-upload-current-version-static.spec.js` | 页面缺少现行版本面板和变更方式 | 增加字段、接口调用、错误反馈 | 同命令 PASS |
| 7 | 前端部门勾选 | `node tests/e2e/dcc-controlled-file-upload-distribution-scope-static.spec.js` | 页面缺少部门树多选 | 增加部门树、提交 payload、文控确认态 | 同命令 PASS |
| 8 | 真实 E2E | `node tests/e2e/dcc-controlled-file-upload-current-version-real.spec.js` | 端到端路径未实现 | 使用真实页面完成上传、审批、文控确认 | 同命令 PASS |

## RED Commands

- 后端服务测试：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileCurrentVersionLookupServiceTest" test`
- 后端提交规则：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileChangeTypeSubmissionServiceTest" test`
- 后端下发范围：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileDistributionScopeServiceTest" test`
- 前端静态契约：`node tests/e2e/dcc-controlled-file-upload-current-version-static.spec.js`
- 前端部门勾选：`node tests/e2e/dcc-controlled-file-upload-distribution-scope-static.spec.js`

## Expected Failures

- 当前没有完整“按文件编号返回现行版本详情”的接口，现行版本查询测试应失败。
- 当前提交 DTO 没有变更方式字段，文控确认契约没有下发部门字段，对应契约测试应失败。
- 当前上传页按历史文件名称带出版本号，不按文件编号展示完整原版本信息，前端静态测试应失败。
- 当前下发范围主要依赖类别默认规则，文控按单个文件勾选部门的测试应失败。

## GREEN Commands

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileCurrentVersionLookupServiceTest,DccControlledFileChangeTypeSubmissionServiceTest,DccControlledFileDistributionScopeServiceTest" test`
- `node tests/e2e/dcc-controlled-file-upload-current-version-static.spec.js`
- `node tests/e2e/dcc-controlled-file-upload-distribution-scope-static.spec.js`
- `pnpm ts:check`

## Refactor Checks

- 后端校验逻辑必须集中在 DCC workflow/domain service，不散落在 Controller。
- 前端错误反馈必须可见，禁止空 `catch` 或只在 console 打印。
- 文件编号查询、提交校验、下发部门解析必须复用同一后端规则。
- SQL/DO/Mapper/VO/API 类型必须同步，避免接口字段只在前端存在。
- 不保留临时兼容分支；历史数据冲突以显式 blocker 或清理脚本处理。

## User Paths

- 申请人上传新建文件：登录测试租户 -> 文控中心 -> 受控文件提交 -> 选择类别/目录 -> 填写编号和版本 -> 上传源文件 -> 预览路线 -> 提交审批。
- 申请人升版文件：登录测试租户 -> 输入已有文件编号 -> 查看现行版本信息 -> 选择升版 -> 输入更高版本 -> 上传源文件和必要 PDF -> 提交审批。
- 申请人作废文件：登录测试租户 -> 输入已有文件编号 -> 查看原版本路径 -> 选择作废 -> 提交审批 -> 文控确认原版本移入作废文件夹。
- 培训记录补传：登录申请人账号 -> 待办 -> 上传培训记录 PDF -> 流程进入文控节点。
- 文控最终确认：登录文控账号 -> 审批任务 -> 预览受控版 -> 确认上传、路径、作废 -> 勾选下发部门 -> 完成归档。

## Browser or Client Steps

- 使用 Playwright 打开 `http://localhost:8081`。
- 只通过真实前端登录测试租户账号操作，不用接口绕过提交、审批、上传或文控确认。
- 上传测试文件使用任务标识命名，例如 `DCC-TDD-20260715-001.docx`、`DCC-TDD-20260715-001.pdf`。
- 操作后截图或 trace 记录文件编号、现行版本面板、文控部门勾选、提交结果和任务状态。

## API Verification

- 只在最终核验使用只读 API 或数据库只读查询。
- 核验受控文件记录：文件编号、版本、变更方式、状态、源文件 ID、图纸 PDF ID、培训记录 ID。
- 核验版本链：新版本成为 current active，旧版本状态符合升版/作废规则。
- 核验下发记录：`dcc_controlled_file_distribution` 部门 ID 与文控页面勾选一致，电子下发接收人已生成。
- 核验审计：上传、预览、下载、审批、培训记录、文控确认、下发均有日志。

## Console and Log Checks

- 前端控制台不得出现未处理异常、接口 500、权限 403 或静默失败。
- 后端日志不得出现 finalization error、BPM 变量缺失、文件 ticket 绑定失败、部门接收人解析失败。
- 如果出现业务阻塞，页面必须显示具体原因，测试记录为预期失败或 blocker。

## Required Test Data

- 测试租户账号：申请人、会签人、批准人、文控人员、部门接收人；账号来源按 `docs/login-access.md` 执行。
- 文件类别：启用状态，绑定最终提交目录，配置审批路线、会签矩阵、查阅矩阵、默认或可选下发部门。
- 现行版本链：至少一个文件编号 `DCC-TDD-EXIST-001`，现行版本 `V1.0`，状态 `ACTIVE`，有可预览源文件和路径。
- 新建编号：至少一个未使用文件编号 `DCC-TDD-NEW-001`。
- 部门数据：至少两个启用部门，每个部门至少一个可接收电子下发的用户；另准备一个无接收人的部门用于失败场景。
- 测试文件：可编辑源文件 docx/xlsx，图纸类扩展名样本，真实 PDF 样本，非 PDF 伪装样本。

## Reset Procedure

- 所有 E2E 创建的数据必须带 `DCC-TDD-20260715` 标识。
- 测试完成后仅清理本任务创建的未生效流程、临时上传文件和测试下发记录。
- 不修改芋道源码租户数据；写入型 E2E 仅使用测试租户。
- 清理前记录文件编号、流程实例、文件 ID、下发记录 ID 和清理结果。

## Data Ownership

- 测试租户数据由当前任务拥有，必须可追踪、可清理。
- 文件类别、审批矩阵、部门接收人若是共享基线数据，测试只能读取或使用，不得随意覆盖。
- 如确需新增测试类别或部门绑定，必须在任务日志记录创建和清理策略。

## Evidence Log Template

- `BDD: <scenario> -> Given ..., When ..., Then ...`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `REGRESSION: <command> -> PASS`
- `E2E: <path> -> PASS, evidence=<screenshot/trace/api-readonly-check>`
- `BLOCKER: <gate> -> <missing precondition and impact>`

## Open Questions

- 同编号匹配范围：租户全局、文件类别内，还是目录内。
- 文控是否可以修改申请人选择的下发部门范围。
- 作废流程是否需要上传新源文件，还是只关联现行版本并审批作废。
- 解密文件方案是否作为本开发完成前置，还是单独阶段上线门禁。

## Test Blockers

- 缺少测试租户真实账号、审批矩阵或部门接收人时，无法执行真实 E2E。
- 缺少解密方案时，不能验收受控浏览上线。
- 同编号历史数据存在冲突且无清理口径时，不能验收自动关联现行版本。
