# 电子文控系统受控文件上传流程开发计划

## Purpose and Scope

本文把 PRD 拆成可实施的 BDD + 严格 TDD 开发计划。计划覆盖后端契约、数据模型、前端表单、审批流转、文控确认、下发范围、测试与验收证据。本文不执行开发，仅定义开发顺序、测试先行路径和门禁。

## Evidence Reviewed

- PRD：`doc/tasks/20260715-electronic-doc-control-upload-flow-prd-plan/prd.md`。
- 原始需求证据：`电子文控系统推进计划及需求表.xlsx` 的 `推进计划!A3:E8`、`需求!C2:R20`。
- 当前上传页：`yudao-ui-admin-vue3/src/views/dcc/controlled-file/upload/index.vue`。
- 当前提交模型：`yudao-ui-admin-vue3/src/views/dcc/controlled-file/upload/submitter.ts`。
- 当前后端提交服务：`ruoyi-vue-pro/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java`。
- 当前 DCC 基础表：`ruoyi-vue-pro/sql/mysql/20260513_dcc_base_schema.sql`。

## Development Principles

- 每个行为先写 BDD 场景和失败测试，再做最小实现，最后跑回归。
- 不用 mock 成功替代真实 DCC 业务规则；缺少数据、审批矩阵、部门接收人或解密能力时记录 blocker。
- 不引入兜底或静默降级；文件编号、版本链、下发部门解析失败时必须阻塞并给出明确错误。
- 前端只负责交互校验和可见反馈；后端是版本链、变更方式、下发范围和权限校验的最终裁决点。

## Milestone 0 - 契约与数据口径确认

### BDD Target

Given 现有 DCC 模块已经有受控文件、版本链、下发、培训和上传临时文件表，When 开发开始前确认契约，Then 必须明确本次新增字段、接口和迁移是否影响发布链路。

### RED

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadCurrentVersionContractTest" test`
- 预期失败：当前没有“按文件编号查询现行版本信息”的后端契约测试。

### Implementation

- 只读核对真实库表结构后确定同编号匹配口径。
- 设计受控文件提交 DTO 扩展：`changeType`、`currentActiveControlledFileId`。
- 设计文控归档确认 DTO 扩展：`selectedDistributionDepartmentIds`。
- 如需持久化变更方式，新增 `dcc_controlled_file.change_type` 与必要索引；如下发范围使用现有 `dcc_controlled_file_distribution`，则在文控最终确认阶段写入待下发部门记录。
- 更新 migration/manifest/菜单或发布契约测试，避免发布链路漏项。

### GREEN

- 后端契约测试通过。
- SQL/migration 契约测试通过。

### Regression

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest,DccProtectionSchemaTest" test`

## Milestone 1 - 文件编号现行版本查询与校验

### BDD Target

Given 申请人输入文件编号，When 系统存在唯一现行有效版本，Then 页面和提交接口都能关联该版本并展示现行信息；当不存在或冲突时按规则阻塞。

### RED

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileCurrentVersionLookupServiceTest" test`
- 预期失败：当前只按历史文件名称带出当前版本号，不能按文件编号返回完整现行版本信息。

### Implementation

- 新增或扩展后端查询接口，例如 `GET /dcc/controlled-files/current-version?fileNumber=...`。
- 返回字段：文件编号、文件名称、现行版本号、状态、当前文件 ID、目录路径、原版本文件路径、产品信息、是否修改中。
- 服务层按租户隔离查询 `dcc_controlled_file_master` 和当前有效 `dcc_controlled_file`；重复现行版本返回明确错误。
- 提交接口复用同一服务校验，防止前端绕过。

### GREEN

- 唯一命中、未命中、重复命中、修改中四类服务测试通过。
- 控制器参数校验和权限测试通过。

### Regression

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileMapperTest" test`

## Milestone 2 - 变更方式与版本链提交

### BDD Target

Given 申请人选择新建、升版或作废，When 提交受控文件，Then 后端根据变更方式校验现行版本、版本号、源文件和原版本路径。

### RED

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileChangeTypeSubmissionServiceTest" test`
- 预期失败：当前提交 DTO 没有变更方式，无法区分新建、升版和作废规则。

### Implementation

- 扩展 `DccControlledFileSubmitReqVO`、前端 `ControlledFileSubmitReqVO` 和 `UploadFormDraft`。
- 新建：同编号不得存在现行有效版本。
- 升版：必须有关联现行版本，新版本号必须高于当前版本。
- 作废：必须有关联现行版本，提交后进入作废审批/文控确认，不产生新现行文件。
- 保留现有 `validateVersionChain` 的严格性，并补充变更方式上下文。

### GREEN

- 新建成功、升版成功、作废成功、版本过低失败、无现行版本失败、同编号修改中失败全部通过。

### Regression

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceImplTest" test`

## Milestone 3 - 下发部门勾选与归档下发

### BDD Target

Given 流程进入文控归档前确认，When 文控勾选文件下发部门并确认归档，Then 系统按最终部门范围生成下发记录和接收人任务。

### RED

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileDistributionScopeServiceTest" test`
- 预期失败：当前下发主要依赖类别默认规则，文控归档确认没有按文件选择下发部门的契约。

### Implementation

- 文控确认模型新增 `selectedDistributionDepartmentIds`。
- 后端文控最终确认时校验部门存在、启用、当前租户可见。
- 电子下发部门需能解析接收人；无法解析时阻塞并返回具体部门。
- 使用 `dcc_controlled_file_distribution` 记录最终下发部门；归档时复用 `DccControlledFileFinalizationServiceImpl` 的既有下发记录派发逻辑。
- 文控节点负责最终确认部门范围；调整需要写入留痕。

### GREEN

- 多部门下发、空部门阻塞、禁用部门阻塞、部门无接收人阻塞、纸质下发记录保留全部通过。

### Regression

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccDistributionReceiptControllerTest,DccPaperDistributionControllerTest" test`

## Milestone 4 - 前端表单与交互

### BDD Target

Given 申请人在上传页填写受控文件明细，When 输入文件编号、选择变更方式并上传文件，Then 页面必须展示现行版本、必填校验、审批路线预览和明确错误反馈；Given 流程进入文控节点，When 文控准备归档，Then 页面必须提供下发部门勾选和最终确认。

### RED

- `node tests/e2e/dcc-controlled-file-upload-current-version-static.spec.js`
- `node tests/e2e/dcc-controlled-file-upload-distribution-scope-static.spec.js`
- 预期失败：当前页面没有文件编号现行版本详情面板、变更方式单选、文控归档前下发部门勾选区。

### Implementation

- 在“文件信息/受控文件明细”区域增加变更方式、文件编号搜索/选择、现行版本信息面板。
- 文件编号或类别变化时清空旧关联并重新查询。
- 下发范围在文控归档确认区使用部门树多选，支持默认部门预选和最终确认态。
- 错误反馈必须显示在字段或表单区域，不允许空 `catch` 或静默失败。
- 保持统一前端列表/表单风格和现有 DCC 页面风格。

### GREEN

- 静态契约测试通过。
- `pnpm ts:check` 通过。

### Regression

- `node tests/e2e/dcc-controlled-file-upload-static.spec.js`
- `node tests/e2e/dcc-controlled-file-training-record-static.spec.js`

## Milestone 5 - 培训记录与文控确认链路

### BDD Target

Given 文件选择需要培训，When 审批到文控上传受控版本前，Then 申请人必须上传培训记录 PDF，上传后才能进入文控节点。

### RED

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileTrainingRecordWorkflowTest" test`
- 预期失败：缺少新流程组合下的培训记录、文控确认、下发范围联动测试。

### Implementation

- 复核现有 `uploadTrainingRecord` 接口，补充变更方式和下发范围场景。
- 文控确认阶段校验上传确认、存入路径、原版本作废确认。
- 升版/作废成功后旧版本状态更新为 `SUPERSEDED` 或 `OBSOLETE`，新版本激活或作废流程结束。

### GREEN

- 需要培训、无需培训、培训记录非 PDF、文控确认缺失、作废确认缺失全部测试通过。

### Regression

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest" test`

## Milestone 6 - 真实 E2E 验收

### BDD Target

Given 测试租户具备 DCC 类别、目录、审批路线、部门接收人和测试文件，When 申请人完成真实上传审批链路，Then 页面、接口和数据库状态一致。

### RED

- `node tests/e2e/dcc-controlled-file-upload-current-version-real.spec.js`
- 预期失败：未实现前端交互和后端契约前，E2E 无法完成文件编号现行版本关联与部门下发。

### Implementation

- 使用测试租户真实登录，按页面路径操作，不通过接口绕过上传、审批、下发部门勾选或文控确认。
- 测试数据必须带任务标识，支持收尾清理。
- API 仅用于最终只读核验业务结果。

### GREEN

- 真实页面上传、审批、培训记录、文控确认、归档下发路径通过。

### Regression

- `pnpm ts:check`
- `mvn.cmd -pl yudao-module-dcc -am test`
- 受影响 E2E 全量回归。

## Release and Documentation Gates

- 若新增或修改 SQL、菜单、脚本、Dockerfile 或构建产物命名，必须补 migration/manifest/产物契约验证。
- 未完成测试服真实运行态验证和 mark-tested 前，不推进正式服或备份服。
- 实现提交和收尾记录分开提交，且只提交本任务文件。

## Open Engineering Questions

- 文件编号唯一性应在数据库层加唯一约束，还是先以服务层强校验加历史数据清理报告推进。
- 部门下发范围是否完全由文控节点最终确认，还是允许从类别默认规则预填后由文控调整。
- 作废流程是否创建新 `dcc_controlled_file` 记录，还是直接对现行版本发起作废审批。
