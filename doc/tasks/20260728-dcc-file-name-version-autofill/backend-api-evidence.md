# Backend API Evidence

## Scope

- Endpoint: `GET /dcc/controlled-files/upload-name-options`
- Service: `DccControlledFileQueryService#listUploadNameOptions`
- Contract: 请求必须提供 `dccProjectCodeId` 和 `fileTypeTaxonomyId`，响应返回已有文件名称、当前版本号、当前活动文件 ID 和文件编号。

## Data Contract

- Query source: 当前系统 DCC 受控文件数据。
- Filter: DCC 项目代码 ID + 文件分类 ID 及其启用子分类。
- Status: 只返回 `ACTIVE` 且 `CONTROLLED_FILE` 流程的当前文件记录。
- Response: `fileName/currentVersionNo/controlledFileId/fileNumber`。

## Acceptance

- 接口必须按 `dccProjectCodeId + fileTypeTaxonomyId` 查询文件名称选项。
- 接口必须返回当前活动文件 ID 和文件编号，供前端选择历史文件后锁定升版目标。
- 无匹配文件时返回空列表，不返回默认选项。
- 项目代码或文件分类无效时 fail-fast。

## Validation

- `dccProjectCodeId` 缺失或不存在时 fail-fast。
- DCC 项目未启用时 fail-fast。
- `fileTypeTaxonomyId` 缺失、非启用路径或不足三级时 fail-fast。
- 不使用产品主数据作为查询来源。

## BDD

- `BDD: 文件名称选项按项目和分类过滤 -> Given 系统存在不同 DCC 项目或不同分类的文件 / When 用户选择某一 DCC 项目和文件分类 / Then 下拉只显示该组合下已有文件名称。`

## RED / GREEN

- `RED: mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest" test -> FAIL, 旧接口仍只接受 categoryId，响应缺少 controlledFileId/fileNumber。`
- `GREEN: mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest" test -> PASS, 5 tests / 0 failures / 0 errors。`
- `GREEN: mvn -pl yudao-module-dcc -am "-DskipTests" compile -> PASS。`

## Verification

- Target tests: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest" test` -> PASS.
- Compile regression: `mvn -pl yudao-module-dcc -am "-DskipTests" compile` -> PASS.

## Blockers

- Git closeout blocked by mixed ahead commit `f56fc825`; backend implementation itself已通过验证。
