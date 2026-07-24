# Task: DCC 上传名称建议空指针修复

## Goal

修复 DCC 上传页切换文件类别后调用 `upload-name-options` 接口时出现的空指针问题，确保当历史主文件链的 `currentActiveControlledFileId` 为空时，接口仍返回可用结果而不是 500。

## Scope

- 先确认上一条后端任务已完成，再创建本任务记录。
- 为 `listUploadNameOptions` 补充严格回归测试，覆盖 `currentActiveControlledFileId` 全为空的场景。
- 做最小修复，不改接口形状，不引入 fallback 或 mock 成功。
- 验证修复后上传页不再因该接口 500 弹出 `系统内部错误`。

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-dcc-upload-leaf-directory-selection/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this bug fix.

## BDD

BDD: 当前激活文件编号为空时上传名称建议仍可返回 -> Given 某文件类别下存在历史主文件记录且其 `currentActiveControlledFileId` 为空 / When 前端请求 `/dcc/controlled-files/upload-name-options` / Then 后端应返回该文件名称及空版本号，而不是抛出 500。

## Milestones

- [x] M1: 创建任务文档、执行日志与缺陷证据。
- [x] M2: 添加 RED 回归测试复现空指针。
- [x] M3: 实现最小修复并运行定向测试。
- [x] M4: 回填证据并提交修复准备。

## Expected Verification

- `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

## Current Status

Completed.

## Final Verification Result

- `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileUploadNameOptionQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS
- 真实运行态校验：
  - `GET /admin-api/dcc/controlled-files/upload-name-options?categoryId=9` -> `code=0`
  - 返回 `currentVersionNo=null` 的记录不再触发 500
  - 前端真实页面在上传页重新选择 `图纸` 类别后，不再出现 `系统内部错误` toast
