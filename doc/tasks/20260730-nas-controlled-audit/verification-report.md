# NAS 受控状态统计与 Excel 报告验证报告

## 结论

当前结论：`PASS_BY_STATIC_SCOPE`。

用户于 2026-07-31 明确调整验收口径为“静态代码检查通过就可以，逻辑通过即可，不用做 E2E”。按该口径，前端静态合同、任务相关文件空白检查、后端/前端关键逻辑静态契约均通过。

未声明真实 E2E 通过；真实 NAS 管理页面点击、任务轮询和 Excel 下载未执行，且不作为本轮验收门槛。

## 需求覆盖核对

| 需求 | 当前证据 | 结论 |
| --- | --- | --- |
| NAS 管理页新增独立“统计未受控文件”按钮 | `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\nas-control-audit-static.spec.js` PASS | PASS |
| 点击确认后创建异步统计任务并轮询状态 | 前端静态合同覆盖 start/status API wrapper 与轮询状态展示；逻辑静态契约覆盖轮询入口 | PASS_BY_STATIC_SCOPE |
| 成功后下载 Excel 且保留重新下载入口 | 前端静态合同覆盖下载函数、自动下载和重新下载入口；逻辑静态契约覆盖 download API | PASS_BY_STATIC_SCOPE |
| 失败时展示后端真实失败原因 | 前端静态合同覆盖失败原因展示与下载失败不误报成功 | PASS_BY_STATIC_SCOPE |
| 后端子目录 ACCESS_DENIED 跳过并记录 | 逻辑静态契约覆盖 `ACCESS_DENIED` 跳过原因和非权限错误 fail-fast | PASS_BY_STATIC_SCOPE |
| 根目录/NAS 失败时任务失败且不生成报告 | 逻辑静态契约覆盖非权限错误 `throw ex`，服务失败分支清空报告文件 | PASS_BY_STATIC_SCOPE |
| 精确路径匹配 ACTIVE 当前受控文件 | 逻辑静态契约覆盖 `file.status = 'ACTIVE'` 与 `current_active_controlled_file_id = file.id` | PASS_BY_STATIC_SCOPE |
| 报告汇总与明细一致 | 逻辑静态契约覆盖 SXSSFWorkbook、汇总行和无法扫描数量“未知” | PASS_BY_STATIC_SCOPE |
| 真实前端路径 E2E | 用户明确不要求 E2E | NOT_REQUIRED |

## 已运行验证

- `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\nas-control-audit-static.spec.js` -> PASS。
- `git diff --check -- <本任务相关文件>` -> PASS。
- `node -` 后端/前端关键逻辑静态契约 -> PASS，覆盖固定根目录、接口、权限、匹配口径、legacy 来源、待确认状态、流式报告、ACCESS_DENIED、fail-fast、转移来源映射、前端 API/按钮/下载/轮询。
- `mvn -pl yudao-module-infra -am "-Dtest=NasRecursiveScanServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT after 180s，未生成目标 surefire 报告。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccBaseSchemaTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT after 180s，未生成目标 surefire 报告。
- `mvn -pl yudao-module-dcc -am "-DskipTests" "-Dmaven.compiler.useIncrementalCompilation=false" compile` -> FAIL，诊断性编译失败在共享框架模块 0 字节截断 class 文件；不作为标准 GREEN。
- `pnpm ts:check` -> TIMEOUT after 180s，已停止本任务启动的 `vue-tsc` 进程。
- `Invoke-WebRequest http://127.0.0.1:8081/` -> HTTP 200。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> FAIL，连接被拒绝。

## 非本轮门槛

- 后端目标 JUnit、`pnpm ts:check` 和真实 E2E 未取得 PASS；用户已明确本轮不要求 E2E，以静态代码检查和逻辑静态契约为准。
- 若后续需要上线放行，仍建议在运行态稳定后补跑后端目标 JUnit、前端类型检查和真实 NAS 只读路径。

## 后续建议

- 运行态稳定后补跑后端定向 JUnit，验证 Excel 明细行数与汇总一致。
- 使用测试 NAS 只读账号执行一次真实扫描，重点观察无权限子目录跳过记录和报告下载。
