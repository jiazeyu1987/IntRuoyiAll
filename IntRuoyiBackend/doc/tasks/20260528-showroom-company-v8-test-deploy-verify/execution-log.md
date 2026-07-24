# 执行日志：发布并验证公司版本 V8 查看修复

BDD: 测试服代码发布后公司 V8 可查看 -> Given 测试服 `芋道源码/admin` 查看公司历史 V8 / When 前端调用 `/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8` / Then 接口不应返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`，而应返回 V8 详情并暴露 V9 当前内容阻塞项。

BDD: 发布不得覆盖测试服业务数据 -> Given 测试服数据库存在 V9 双语讲解重复候选 / When 发布本次代码修复 / Then 不执行数据库同步、不清空远端 MySQL、不写入 `showroom_version_bundle` 或讲解版本数据。

INFO: 任务开始前确认上一代码修复提交为 `c716918154`；当前存在其他未提交改动，本任务不纳入。

RED: 发布前只读接口验证 -> FAIL, 测试服 `GET /admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8&siteKey=yingtai-showroom&stage=TEST` 返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`，与用户浏览器堆栈一致。

INFO: 发布脚本检查 -> `publish-int-ruoyi.ps1` 支持 `-SkipDatabaseSync -SkipMinioSync`，该路径不执行数据库 dump/import，不清空远端 MySQL 数据目录，不执行 MinIO mirror。

BLOCKED: 测试服代码发布 -> 本机发布前置条件缺失：`DCC_SIGNATURE_EVIDENCE_HMAC_SECRET=false`，`DCC_SIGNATURE_EVIDENCE_KEY_VERSION=false`；本地 Docker 可用且 SSH 到 `172.30.30.58` 可用，但未运行 `int-ruoyi-mysql` 容器，脚本无法读取 `MYSQL_ROOT_PASSWORD`。影响：不能安全生成远端 `.env`，不能按 fail-fast 发布脚本执行测试服代码发布。

GREEN: task-closeout-cleanup 预览 -> PASS, `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-showroom-company-v8-test-deploy-verify --mode preview` 返回 `status: ready`，保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞、无警告。

INFO: 用户要求“帮我添加”后，从测试服现有 `/opt/intruoyi/runtime/.env` 读取 DCC 签名证据配置键名并写入 Windows User 环境变量；日志未输出密钥值。

INFO: 发布前置恢复 -> 本地 `int-ruoyi-mysql` 与 `docker-minio-1` 容器已运行；发布命令通过显式参数传入 User 环境变量中的 DCC 配置，保持 fail-fast，不使用默认成功值或 mock。

INFO: 测试服只读数据确认 -> `showroom_company id=1 tenant_id=1 current_revision_id=9`；`showroom_company_revision` 存在 revision 8 与 9；`showroom_version_bundle` 对 `COMPANY:1` 仅存在 revision 7 与 8，revision 9 缺 bundle；V9 存在 ZH/EN 重复发布讲解候选，因此不自动回填、不改业务数据。

INFO: 脏主仓库发布尝试 -> 使用主仓库发布 `20260528_company_v8_clean_c716918154` 时，后端启动失败于无关 runtime-control WIP：`RuntimeOpsCandidateServiceImpl: No default constructor found`。影响：主仓库当前混入其他任务改动，不能作为本任务发布源。

INFO: 干净 worktree 发布源 -> 创建成对 worktree `D:\ProjectPackage\Int\IntRuoyi-company-v8-deploy`，后端分支 `codex/showroom-company-v8-deploy-clean` 基于 `c716918154`，前端分支同名基于 `64f728913`，仅纳入本任务所需修复。

GREEN: 测试服代码发布 -> PASS, 使用干净 worktree 执行 `publish-int-ruoyi.ps1 -Environment test -SkipDatabaseSync -SkipMinioSync -Tag 20260528_company_v8_worktree_c716918154`，远端后端、admin 前端、展厅前端健康检查通过。

RED: 发布后真实 V8 接口验证 -> FAIL, `GET /admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8&siteKey=yingtai-showroom&stage=TEST` 不再返回原始 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`，但返回顶层 `SHOWROOM_TARGET_NOT_FOUND: company revision not found`；原因是辅助快照诊断 `diagnoseMissingBundle` 对当前发布指针引用的缺失 revision 仍会抛异常并中断历史版本详情。

BDD: 当前内容 revision 缺失不阻断历史公司版本查看 -> Given 选中的公司历史版本已有 bundle 且当前内容 revision 指向不存在数据 / When 查询历史公司版本详情 / Then 返回选中历史版本详情，并把当前内容缺失记录为 `CURRENT_CONTENT` blocker。

RED: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentRevisionIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增回归在实现前抛出 `SHOWROOM_TARGET_NOT_FOUND: company revision not found`。

GREEN: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentRevisionIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 新增回归通过。

GREEN: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest,ShowroomVersionBundleServiceTest,ShowroomVersionCenterBackfillContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 13 tests passed。

GREEN: 测试服最终代码发布 -> PASS, 使用干净 worktree 执行 `publish-int-ruoyi.ps1 -Environment test -SkipDatabaseSync -SkipMinioSync -Tag 20260528_company_v8_auxfix_c716918154`，远端 `48081`、`8081`、`8083` 健康检查通过。

GREEN: 真实 `芋道源码/admin` API 验证 -> PASS, `GET /admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8&siteKey=yingtai-showroom&stage=TEST` 返回 `code=0`、`selectedRevisionId=8`、`selectedRevisionNo=8`、`currentContentRevisionId=9`、`currentContentVersionIsNull=true`、`blockerCount=4`。阻塞项包含 `CURRENT_CONTENT` 的 V9 双语讲解重复候选，以及 `CURRENT_RELEASE` 的 `SHOWROOM_TARGET_NOT_FOUND`；顶层不再返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`。

GREEN: 主仓库补丁同步后新增回归 -> PASS, `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentRevisionIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test`。

GREEN: 主仓库补丁同步后版本中心回归 -> PASS, `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest,ShowroomVersionBundleServiceTest,ShowroomVersionCenterBackfillContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，13 tests passed。
