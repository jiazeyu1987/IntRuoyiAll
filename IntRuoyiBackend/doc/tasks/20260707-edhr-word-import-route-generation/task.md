# Task: eDHR Word 导入自动生成工艺路线

## Task Goal

在 `recognize-uploaded` 导入 Word 并填写批记录名称后，同一事务内生成批记录表单、工艺路线和工艺批记录路线；若首个工序不是固定的产品信息工序，则中止导入并回滚所有落库结果。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；所有中文文件读写必须显式 UTF-8，禁止使用默认 `Get-Content` / `Set-Content` / 重定向处理中文。
- 批记录 Word 表单识别：已读取 `docs/experience/batch-record-form-recognition.md`；不得按单个文件名、工序名样例或页面截图硬编码，产品信息与工序表单判断必须走解析结果的通用字段。
- 后端接口交付：需记录 API/数据契约、BDD、RED/GREEN 证据；失败路径必须 fail fast，不允许 fallback、吞异常或默认成功。
- 高风险动作：本任务不操作服务器、不发布、不做数据库真实环境写入；如后续执行真实 E2E，需先补 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少产品信息或路线生成失败必须直接报错并回滚。
- `是否从根因和长期维护角度解决`：是。新增独立路线生成服务，把 Word 解析表单到路线/用途绑定的编排集中处理。
- `是否存在临时补丁或绕过`：否。

## Milestone List

1. 已完成：创建任务文档与执行日志，记录 BDD 场景和门禁。
2. 已完成：补充 RED 测试，证明当前 Word 导入不会生成路线且缺少产品信息不会阻断。
3. 已完成：实现后端路线生成服务、导入结果扩展和错误码。
4. 已完成：补充最小前端类型/提示联动。
5. 已完成：运行目标测试与回归测试，记录 GREEN / REGRESSION。
6. 已完成：收尾清理预览并清理本任务附属产物。

## Expected Verification

- `mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest test`
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordReportControllerTest,MesProRouteUseConfigControllerPermissionTest test`
- 如时间和依赖允许，补跑受影响 eDHR 执行服务测试。

## Final Verification

- RED：`mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest test` 失败，原因符合预期：缺少路线生成服务、新错误码和导入结果路线字段。
- GREEN：`mvn.cmd -pl yudao-module-mes clean test "-Dtest=MesProBatchRecordReportServiceImplDbTest"` 通过，43 tests / 0 failures / 0 errors。
- REGRESSION：`mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProRouteUseConfigControllerPermissionTest,MesProEdhrBatchExecutionServiceTest,MesProWorkOrderMapperTest,MesProWorkOrderServiceImplTest" test` 通过，75 tests / 0 failures / 0 errors。
- FRONTEND：`pnpm.cmd ts:check` 默认 4GB Node 堆内存 OOM；设置 `NODE_OPTIONS=--max-old-space-size=8192` 后 `pnpm.cmd ts:check` 通过。
- REAL E2E：`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe node tests\e2e\edhr-word-template-import-real-flow.e2e.js` 使用真实 Word `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc` 和测试租户 `aoteman` 通过，生成 `routeCode=RT000002`、15 个报表、14 个路线工序、14 个工艺批记录路线绑定。

## Current Status

Completed.
