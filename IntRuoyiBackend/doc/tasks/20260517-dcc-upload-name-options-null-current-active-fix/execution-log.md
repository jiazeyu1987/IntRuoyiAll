# Execution Log: DCC 上传名称建议空指针修复

BDD: 当前激活文件编号为空时上传名称建议仍可返回 -> Given 某文件类别下存在历史主文件记录且其 `currentActiveControlledFileId` 为空 / When 前端请求 `/dcc/controlled-files/upload-name-options` / Then 后端应返回该文件名称及空版本号，而不是抛出 500。

RED: `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> FAIL，`listUploadNameOptions_allCurrentActiveIdsNull_returnsNullVersions` 因 `Map.of().get(null)` 触发 `NullPointerException`。

GREEN: `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS

GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS

GREEN: 真实接口校验 -> PASS
- `GET /admin-api/dcc/controlled-files/upload-name-options?categoryId=9`
- 返回 `code=0`
- 返回数据中 `currentVersionNo` 可为 `null`

GREEN: 前端真实页面复验 -> PASS
- 上传页重新选择 `图纸` 类别
- `upload-name-options` 返回 `code=0`
- 页面 toast 列表为空
