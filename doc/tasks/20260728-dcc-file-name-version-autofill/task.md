# DCC 文件名称下拉与版本默认值联动

## Task Goal

在 DCC 受控文件上传链路中，选择 DCC 项目和文件分类后，文件名称支持从当前系统已有同项目同分类文件下拉选择，也支持手动输入。手动输入新文件名时版本号默认 `V1.0`；选择已有文件名时版本号默认按当前版本大版本递增，例如 `V1.0 -> V2.0`；生效日期默认当天。

## Milestones

1. [completed] 梳理上传页、文件分类、DCC 项目和既有文件查询接口契约。
2. [completed] 补 RED 静态/后端契约，锁定文件名称下拉、手输、版本默认值和生效日期规则。
3. [completed] 实现后端已有文件名称选项查询和前端表单联动。
4. [completed] 运行目标前后端验证和必要回归。
5. [blocked] 更新证据、收尾并准备提交推送。

## Expected Verification

- 前端静态契约覆盖：选择项目和分类后文件名称为可搜索下拉输入；已有文件选中时版本默认大版本 +1；手动输入时版本默认 `V1.0`；生效日期默认当天。
- 后端目标测试覆盖：按 `dccProjectCodeId + fileTypeTaxonomyId` 查询已有文件名称和当前版本，且结果不使用产品主数据作为来源。
- 目标前端类型检查或任务专用静态 E2E 通过。
- 目标后端 Maven 测试通过。

## Current Status

blocked

## Completed Work

- 前端上传页文件名称保留 `el-autocomplete`，支持下拉选择已有文件和手动输入新文件名。
- 文件名称选项接口改为按 `dccProjectCodeId + fileTypeTaxonomyId` 获取当前系统已有文件，响应包含 `fileName/currentVersionNo/controlledFileId/fileNumber`。
- 手动输入文件名称默认版本号 `V1.0`；选择已有文件名称默认下一大版本，例如 `V1.0 -> V2.0`。
- 生效日期在上传表单创建和文件名称联动重置时默认当天。

## Verification Result

- PASS: `pnpm e2e:dcc:upload-name-version-autofill:static`
- PASS: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest" test`
- PASS: `pnpm e2e:dcc:upload-project-taxonomy-revision:static`
- PASS: `pnpm e2e:dcc:upload-product-autofill:static`
- PASS: `pnpm e2e:dcc:upload-current-version:static`
- PASS: `mvn -pl yudao-module-dcc -am "-DskipTests" compile`
- PASS: `pnpm ts:check`

## Blocker

- Git closeout is blocked: current `HEAD` is `f56fc825 chore: baseline dirty workspace before loss form switch fix`, and that single ahead commit contains this DCC task plus unrelated MES, eDHR, pressure-pump, and deleted docx changes. Per task ownership rules, this mixed commit cannot be pushed as this DCC task's clean implementation commit without user direction or a separate Git cleanup plan.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 DCC 项目和文件分类正式数据源提供可选文件名称，并由前端显式区分选择已有文件与手动输入。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 适用门禁：严格无 fallback；接口失败必须暴露，不使用默认成功或空数据掩盖。
- 适用门禁：前端静态契约隔离；若全量检查受并行无关改动阻塞，使用本任务聚焦静态契约记录 RED/GREEN。
- 适用门禁：DCC 上传类别权限投影；本任务不放宽类别权限，上传提交仍由后端既有类别权限校验 fail-fast。
