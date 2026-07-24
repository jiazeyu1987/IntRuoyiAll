# 执行日志

## 2026-05-26 初始化

BDD: Gate1 NAS ACL 读取能力验证 -> Given 当前 NAS 浏览服务只支持目录枚举和文件读取 / When 开始实现权限快照能力 / Then 必须先用失败测试证明 ACL 读取接口缺失，再最小实现 ACL 能力检测与读取；如果 SMBJ 或 NAS 账号无法读取 ACL，必须 fail fast 并记录 BLOCKER。

RED: Gate1 尚未开始 -> FAIL, 尚未写入 NAS ACL 读取能力 RED 测试，不能进入 Gate2。

## 2026-05-26 Gate1 RED

- task id: T1
- changed paths:
  - `yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/file/NasBrowserServiceImplTest.java`
- implemented behavior: 新增 NAS ACL 读取期望测试，覆盖路径规范化、SecurityDescriptor 到 DTO 的字段保留、ACE 顺序、ALLOW/DENY、inherited 标记、accessMask 和 ACL 读取失败错误码。
- validation commands:
  - `mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test`
- validation results:
  - `RED: mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test -> FAIL, testCompile 找不到 FILE_NAS_ACL_READ_FAILED、NasAclReadResult、NasAclAce、NasBrowserService#readDirectoryAcl(String)、NasBrowserServiceImpl#toAclReadResult(String, SecurityDescriptor)，符合 Gate1 RED 预期。`
- covered acceptance ids:
  - AC-01
  - AC-02
  - AC-07
- known risks or blockers: 真实 NAS ACL 读取尚未验证，Gate1 不能放行到 Gate2。

## 2026-05-26 Gate1 GREEN

- task id: T2
- changed paths:
  - `yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/enums/ErrorCodeConstants.java`
  - `yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/NasBrowserService.java`
  - `yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/NasBrowserServiceImpl.java`
  - `yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/NasAclAce.java`
  - `yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/NasAclReadResult.java`
- implemented behavior: 最小实现 NAS 目录 ACL 读取接口、ACL DTO、权限读取错误码、SecurityDescriptor 到 DTO 的字段保留，以及 SMBJ `DiskShare#getSecurityInfo` OWNER/GROUP/DACL 读取；ACL 读取失败明确映射到 `FILE_NAS_ACL_READ_FAILED`，未进入 Gate2、快照表、任务、身份映射、恢复预览、前端或 SQL。
- validation commands:
  - `mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test`
- validation results:
  - `RED: mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test -> FAIL, testCompile 找不到 FILE_NAS_ACL_READ_FAILED、NasAclReadResult、NasAclAce、NasBrowserService#readDirectoryAcl(String)、NasBrowserServiceImpl#toAclReadResult(String, SecurityDescriptor)，符合 T1 RED 预期。`
  - `GREEN: mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test -> PASS, Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。`
  - `REGRESSION: mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test -> PASS, 现有 NAS 列表、连接测试、目录树和文件读取测试保持通过。`
GREEN: mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test -> PASS, Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。
- covered acceptance ids:
  - AC-01
  - AC-02
- known risks or blockers: T2 未做真实 NAS 环境验证，留待 T3；无阻塞影响当前 Gate1 GREEN 单测通过。

## 2026-05-26 Gate1 真实 NAS 验证与 reviewer 放行

- task id: T3
- changed paths:
  - `doc/tasks/20260526-nas-permission-snapshot-restore-implementation/test-report.md`
- implemented behavior: 独立测试 agent 复核 Gate1 GREEN，主 reviewer 在测试服务器 backend 容器内使用当前 `app.jar` 的 SMBJ 依赖编译临时探针，读取真实测试 NAS 配置并对业务目录执行 `DiskShare#getSecurityInfo`。
- validation commands:
  - `mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test`
  - `mvn -pl yudao-module-infra test`
  - `ssh root@172.30.30.58`，在 `intruoyi-backend` 容器 `/tmp/nas-acl-probe` 临时编译运行 SMBJ ACL 探针，随后删除临时目录。
- validation results:
  - `GREEN: mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test -> PASS, Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。`
  - `REGRESSION: mvn -pl yudao-module-infra test -> PASS, Tests run: 197, Failures: 0, Errors: 0, Skipped: 10。`
  - `GREEN: TC-G1-REAL-NAS -> PASS, 测试服 NAS 配置为 server=172.30.30.4、share=质量体系文件、username=ceshi、path=1. QMS documents；SMBJ 成功认证并读取 SecurityDescriptor，ownerSidPresent=true，groupSidPresent=true，daclPresent=true，aceCount=124，inheritedAceCount=23，firstAce type=ACCESS_ALLOWED_ACE_TYPE，flags=[CONTAINER_INHERIT_ACE, OBJECT_INHERIT_ACE]，trusteeSidPresent=true。`
  - `CLEANUP: ssh root@172.30.30.58 "docker exec intruoyi-backend sh -lc 'rm -rf /tmp/nas-acl-probe'" -> PASS。`
- covered acceptance ids:
  - AC-01
  - AC-02
  - AC-07
- known risks or blockers: `#recycle` 目录 ACL 读取返回 `STATUS_ACCESS_DENIED`，说明后续正式快照采集必须对被选中目录及子目录 fail fast 记录 blocker，不能把不可读目录视为成功；Gate1 读取能力本身已通过。Gate2 尚未开始，不能进入正式恢复开发。

## 2026-05-26 Gate2 RED

BDD: DCC 运行时详情必须叠加目录查询权限 -> Given 普通用户拥有文件类别 VIEW 权限但目录 QUERY 授权目录集合不包含该文件目录 / When 用户打开受控文件详情 / Then 系统必须抛出 `CONTROLLED_FILE_ACCESS_DENIED`，不得仅凭类别 VIEW 放行详情。

BDD: DCC 运行时预览必须叠加目录预览权限 -> Given 普通用户拥有文件类别 VIEW 权限且文件为 ACTIVE 并有 publishedFileId，但目录 PREVIEW 授权目录集合不包含该文件目录 / When 用户读取预览文件 / Then 系统必须拒绝并记录 `ACCESS_DENIED` 审计。

BDD: DCC 运行时下载必须叠加目录下载权限 -> Given 普通用户拥有文件类别 DOWNLOAD 权限且文件为 ACTIVE 并有 publishedFileId，并已确认下载警告，但目录 DOWNLOAD 授权目录集合不包含该文件目录 / When 用户下载文件 / Then 系统必须拒绝并记录 `ACCESS_DENIED` 审计。

BDD: DCC 详情动作权限必须来自目录访问规则和类别权限交集 -> Given 普通用户类别 VIEW/DOWNLOAD 均允许且目录 QUERY 允许，但目录 PREVIEW/DOWNLOAD 不允许 / When 用户查看受控文件详情 / Then 返回详情中的 `canPreview` 与 `canDownload` 必须为 false。

- task id: Gate2-RED
- changed paths:
  - `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
  - `doc/tasks/20260526-nas-permission-snapshot-restore-implementation/execution-log.md`
- validation commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test`
- validation results:
  - `RED: mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test -> FAIL, Tests run: 22, Failures: 4, Errors: 0, Skipped: 0。失败用例为 getControlledFile_deniedWhenDirectoryCanQueryFalseEvenWithCategoryView、readPreviewFile_activeFileDeniedWhenDirectoryCanPreviewFalseEvenWithCategoryView、readDownloadFile_activeFileDeniedWhenDirectoryCanDownloadFalseEvenWithCategoryDownload、getControlledFile_setsCanPreviewAndCanDownloadFromDirectoryAccessRules。失败原因分别为详情未抛 CONTROLLED_FILE_ACCESS_DENIED、预览未抛 CONTROLLED_FILE_ACCESS_DENIED、下载未抛 CONTROLLED_FILE_ACCESS_DENIED、详情 canPreview 仍为 true，均指向当前生产实现未在详情和二进制读取中叠加目录 QUERY/PREVIEW/DOWNLOAD enforcement。`
- covered acceptance ids:
  - Gate2-DCC-DETAIL-QUERY
  - Gate2-DCC-PREVIEW-DIRECTORY
  - Gate2-DCC-DOWNLOAD-DIRECTORY
  - Gate2-DCC-DETAIL-ACTION-FLAGS
- known risks or blockers: Gate2 当前处于 RED，不能进入正式恢复开发；需实现 agent 在生产代码中补齐目录 QUERY/PREVIEW/DOWNLOAD enforcement 后再运行 GREEN。

## 2026-05-26 Gate2 GREEN

- task id: Gate2-GREEN
- changed paths:
  - `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`
  - `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
  - `doc/tasks/20260526-nas-permission-snapshot-restore-implementation/execution-log.md`
- implemented behavior: 在受控文件详情、预览与下载运行时权限中叠加目录访问规则；普通用户详情需类别 VIEW 与目录 QUERY 同时满足；预览需既有类别/状态规则与目录 PREVIEW 同时满足；下载需既有类别/状态规则与目录 DOWNLOAD 同时满足，`INT/RE` 系统记录开放逻辑也不能绕过目录 DOWNLOAD。目录管理员/访问规则管理员继续通过 `hasDirectoryManagementPermission` 管理放行。未实现 NAS 权限快照表、身份映射、恢复预览、恢复应用、前端或 SQL。
- test adaptation: 仅为既有正向单测补齐目录授权默认 mock 与目录 ID fixture，使旧用例显式满足新增目录权限前置条件；未削弱 Gate2 RED 新增断言。
- validation commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test`
  - `mvn -pl yudao-module-dcc test`
- validation results:
  - `RED: mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test -> FAIL, Tests run: 22, Failures: 4, Errors: 0, Skipped: 0；详情、预览、下载与详情动作权限未叠加目录 QUERY/PREVIEW/DOWNLOAD。`
  - `GREEN: mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test -> PASS, Tests run: 22, Failures: 0, Errors: 0, Skipped: 0。`
  - `REGRESSION: mvn -pl yudao-module-dcc test -> PASS, Tests run: 227, Failures: 0, Errors: 0, Skipped: 0。`
- covered acceptance ids:
  - Gate2-DCC-DETAIL-QUERY
  - Gate2-DCC-PREVIEW-DIRECTORY
  - Gate2-DCC-DOWNLOAD-DIRECTORY
  - Gate2-DCC-DETAIL-ACTION-FLAGS
- known risks or blockers: 无。

## 2026-05-27 Gate2 reviewer 放行

- task id: Gate2-REVIEW
- reviewer scope: 主 reviewer 复核 GREEN 子 agent 改动、任务日志、目标测试和 DCC 模块回归；确认本 gate 只覆盖受控文件运行时详情、预览、下载，不声明覆盖目录管理详情、上传目录树或正式快照恢复能力。
- validation commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test`
  - `mvn -pl yudao-module-dcc test`
- validation results:
  - `GREEN: mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test -> PASS, Tests run: 22, Failures: 0, Errors: 0, Skipped: 0。`
  - `REGRESSION: mvn -pl yudao-module-dcc test -> PASS, Tests run: 227, Failures: 0, Errors: 0, Skipped: 0。`
- review decision:
  - logic_status: pass。详情、预览、下载均在既有类别/状态规则基础上叠加目录 `QUERY/PREVIEW/DOWNLOAD`；`INT/RE` 下载开放逻辑不能绕过目录 `DOWNLOAD`；缺失授权集合不会被默认放行。
  - usability_status: pass。详情响应中的 `canPreview`、`canDownload` 与实际二进制接口保持一致，前端按钮不会展示已知会被后端拒绝的操作。
  - ui_status: pass。本阶段无 UI 改动。
  - final_decision: pass。
- known risks or blockers: 无 Gate2 blocker；目录管理接口与上传目录树不是本 gate 的放行范围，正式开发若依赖这些入口必须另行写 RED/GREEN。

## 2026-05-27 正式开发第一波 T3 schema RED

BDD: BDD-NAS-ACL-02 Deduplicated Snapshot Storage -> Given 同一任务中多个目录拥有相同规范化 ACL 或任务续跑再次遇到已采集目录 / When 后端写入 NAS 权限快照 / Then runtime SQL 与 test SQL 必须提供 ACL 快照批次、目录快照、descriptor 去重、ACE 顺序、身份映射及幂等唯一键，重复写入不得产生冲突数据。

BDD: BDD-NAS-ACL-05 Restore Apply -> Given 用户确认应用权限恢复且恢复过程必须可审计 / When 后端生成并执行 DCC 权限恢复计划 / Then runtime SQL 与 test SQL 必须提供恢复计划、恢复计划项、恢复日志及唯一键，执行失败不得被默认成功覆盖。

RED: mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSchemaTest test -> FAIL, Tests run: 1, Failures: 1, Errors: 0, Skipped: 0；`DccNasPermissionSchemaTest.mysqlRuntimeAndTestSchemaShouldContainNasAclSnapshotRestoreTables` 断言失败，runtime mysql schema 与 dcc test schema 均缺少 `dcc_nas_acl_snapshot`、`dcc_nas_acl_directory_snapshot`、`dcc_nas_acl_descriptor`、`dcc_nas_acl_ace`、`dcc_nas_acl_identity_mapping`、`dcc_nas_acl_restore_plan`、`dcc_nas_acl_restore_plan_item`、`dcc_nas_acl_restore_log`，因此尚无法验证这些表的 `tenant_id`、`deleted`、`create_time`、`update_time` 与关键唯一键/索引。

SUBAGENT: T3 DCC SQL/test-schema test agent -> 写入 `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccNasPermissionSchemaTest.java` 与本执行日志；只新增 RED schema 测试并使用真实仓库 SQL 文件作为断言输入，未写生产代码、未写 runtime SQL、未改 test SQL。

## 2026-05-27 正式开发第一波 T4 schema GREEN

BDD: BDD-NAS-ACL-02 Deduplicated Snapshot Storage -> Given 同一任务中多个目录拥有相同规范化 ACL 或任务续跑再次遇到已采集目录 / When 后端写入 NAS 权限快照 / Then runtime SQL 与 test SQL 提供 ACL 快照批次、目录快照、descriptor 去重、ACE 顺序、身份映射及幂等唯一键，重复写入不得产生冲突数据。

BDD: BDD-NAS-ACL-05 Restore Apply -> Given 用户确认应用权限恢复且恢复过程必须可审计 / When 后端生成并执行 DCC 权限恢复计划 / Then runtime SQL 与 test SQL 提供恢复计划、恢复计划项、恢复日志及唯一键，执行失败不得被默认成功覆盖。

RED: mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSchemaTest test -> FAIL, Tests run: 1, Failures: 1, Errors: 0, Skipped: 0；runtime mysql schema 与 dcc test schema 均缺少 8 张 NAS ACL 快照/恢复持久化表。

GREEN: mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSchemaTest test -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。

GREEN: mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest test -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0。

GREEN: mvn -pl yudao-module-dcc "-Dtest=DccNasPermissionSchemaTest,DccBaseSchemaTest" test -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。

SUBAGENT: T4 DCC NAS 权限快照/恢复持久化 GREEN -> 新增 8 张 runtime MySQL 幂等表、base schema 覆盖、H2 test schema 覆盖、test clean 清理顺序、8 个 DO 与对应 Mapper；仅完成持久化/schema 基础，未实现 NAS ACL 快照采集 service、controller、恢复计划生成或恢复执行逻辑。

## 2026-05-27 正式开发第一波 T4 reviewer 放行

REVIEW: T4 schema reviewer -> PASS, 主 reviewer 复核 SQL、DO/Mapper、T3 RED 测试、任务日志与 database schema evidence；确认本波只交付持久化/schema 基础，不包含快照采集 service、controller、恢复计划生成或恢复执行逻辑。

GREEN: mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSchemaTest test -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。

GREEN: mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest test -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0。

GREEN: python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260526-nas-permission-snapshot-restore-implementation/database-schema-evidence.md -> PASS, Database schema evidence is valid。

REGRESSION: mvn -pl yudao-module-dcc test -> PASS, Tests run: 228, Failures: 0, Errors: 0, Skipped: 0。

GREEN: python -m pytest script/tests/test_dcc_nas_acl_snapshot_restore_sql.py -> PASS, 2 passed；覆盖新增 MySQL 迁移脚本的幂等建表、非破坏性 SQL、8 张 NAS ACL 快照/恢复表、关键唯一键，以及 base schema 同步。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅 PowerShell/Git 提示工作区文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 正式开发第二波 T5 service RED

BDD: BDD-NAS-ACL-01 完整快照采集 -> Given 一个 DCC NAS 转移目录任务项被处理，且 `nasBrowserService.readDirectoryAcl(nasPath)` 返回包含 ownerSid、groupSid、controlFlags、DACL 和至少一条 ACE 的 `NasAclReadResult` / When `processWaitingTasks()` 执行并解析出 DCC directoryId / Then 必须调用 `DccNasPermissionSnapshotCaptureService.captureDirectorySnapshot(taskId, itemId, nasPath, resolvedDirectoryId, acl)` 保存原始 NAS ACL 快照，并且目录项只有在快照采集完成后才能标记 COMPLETED。

BDD: BDD-NAS-ACL-06 读取失败阻断 -> Given 一个 DCC NAS 转移目录任务项处理时 `nasBrowserService.readDirectoryAcl(nasPath)` 抛出明确异常 / When `processWaitingTasks()` 执行 / Then 目录项必须标记 FAILED，failureStage 必须明确为 `acl`，不得调用 snapshot capture，不得插入子目录或文件任务项，也不得继续用父 DCC 权限当作 NAS 权限恢复结果。

RED: mvn -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest test -> FAIL, testCompile 失败；`DccControlledFileNasTransferServiceTest` 找不到 `DccNasPermissionSnapshotCaptureService`，证明当前 DCC 模块尚未提供 NAS ACL 快照采集 service 契约，也未在 `DccControlledFileNasTransferServiceImpl` 中集成目录 ACL 读取/保存。该单模块命令同时找不到 Gate1 新增的 `NasAclReadResult` 与 `NasAclAce`，说明当前 DCC 单模块测试依赖解析尚未看到上游 infra ACL DTO 产物；未使用 fallback/mock success 绕过。

SUBAGENT: T5 DCC NAS 权限快照采集 service RED -> 扩展 `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileNasTransferServiceTest.java`，新增目录 ACL 完整快照采集与 ACL 读取失败阻断两个 RED 场景；只写 RED 测试与本执行日志，未写生产代码、未改 SQL、未创建 service 实现。

## 2026-05-27 T5 RED reviewer contract normalization

REVIEW-FIX: T5 RED 契约归属修正 -> 根据 reviewer 反馈，测试显式 import `cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionSnapshotCaptureService`；NAS ACL 快照采集 service 的边界固定为 `service.permission`，不得因测试同包隐式解析而落入 `service.file`。

RED: mvn -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest test -> FAIL, testCompile 失败；`cn.iocoder.yudao.module.dcc.service.permission` 包不存在且找不到 `DccNasPermissionSnapshotCaptureService`，同时单模块本地依赖仍找不到上游 infra 的 `NasAclReadResult/NasAclAce` 产物，未隐藏 DCC 缺失契约。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 中 `yudao-module-infra` SUCCESS，`yudao-module-dcc` testCompile 仅剩 2 个错误：`cn.iocoder.yudao.module.dcc.service.permission` 包不存在，以及找不到 `DccNasPermissionSnapshotCaptureService`；证明 RED 剩余失败聚焦于 DCC 缺少 `service.permission` 快照采集 service 契约。

SUBAGENT: T5 RED reviewer fix -> 只修正测试 import 包边界并追加执行日志；未写生产代码、未改 SQL、未创建 service 实现。

## 2026-05-27 T5 persistence-focused service RED

BDD: BDD-NAS-ACL-02 快照采集持久化 -> Given 一个 DCC NAS 转移任务、目录任务项、当前 NAS 配置，以及包含 ownerSid、groupSid、controlFlags、DACL 状态和一条 ACE 的 `NasAclReadResult` / When `DccNasPermissionSnapshotCaptureServiceImpl.captureDirectorySnapshot(taskId, taskItemId, nasPath, dccDirectoryId, acl)` 执行 / Then 必须写入 `dcc_nas_acl_snapshot` 采集批次头、规范化 descriptor、descriptor 下 ACE，以及关联 transferTaskId、transferTaskItemId、dccDirectoryId、nasPath、pathHash、itemName、descriptorId 和 SUCCESS 状态的目录快照。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionSnapshotCaptureServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 中 `yudao-module-infra`、`yudao-module-system`、`yudao-module-bpm` 等上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败；错误包含 `DccNasPermissionSnapshotCaptureServiceImplTest` 找不到 `DccNasPermissionSnapshotCaptureServiceImpl`，同时既有转移集成 RED 仍找不到 `service.permission.DccNasPermissionSnapshotCaptureService`，证明当前缺少快照采集 service 接口与实现，尚未满足 header/descriptor/ACE/directory snapshot 持久化行为。

SUBAGENT: T5 DCC NAS 权限快照采集持久化 RED -> 新增 `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/permission/DccNasPermissionSnapshotCaptureServiceImplTest.java`；只写 Mockito RED 单测与本执行日志，未写生产代码、未改 SQL、未创建 service 实现。

## 2026-05-27 正式开发第二波 T6 service GREEN implementation

BDD: BDD-NAS-ACL-01 完整快照采集 -> Given 一个 DCC NAS 转移目录任务项被处理且 NAS ACL 可读取 / When `processWaitingTasks()` 执行并解析出 DCC directoryId / Then 在目录项标记 COMPLETED 和插入子项前调用 `DccNasPermissionSnapshotCaptureService.captureDirectorySnapshot(...)` 保存 ACL 快照。

BDD: BDD-NAS-ACL-06 读取失败阻断 -> Given 目录任务项处理时 `nasBrowserService.readDirectoryAcl(nasPath)` 抛出异常 / When `processWaitingTasks()` 执行 / Then 目录项标记 FAILED、`failureStage = acl`，不调用 snapshot capture，不列举目录、不插入子项。

BDD: BDD-NAS-ACL-02 快照采集持久化 -> Given 一个转移任务、目录任务项、当前 NAS 配置和包含 owner/group/controlFlags/DACL/ACE 的 `NasAclReadResult` / When `captureDirectorySnapshot(...)` 执行 / Then 创建或复用快照头与 descriptor，并写入 ACE 和目录快照成功记录。

IMPLEMENTATION: T6 DCC NAS 权限快照采集 GREEN -> 新增 `cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionSnapshotCaptureService` 契约和 `DccNasPermissionSnapshotCaptureServiceImpl`；使用 `NasSettingsService#getRequiredNasConfig()` 持久化 server/share；以 task 生成稳定 `snapshotKey` 并创建/复用快照头；规范化 ACL descriptor 并按 `descriptorHash` 创建/复用 descriptor；新 descriptor 写入 ACE 行；按 `snapshotId + pathHash` 创建/更新目录快照；目录转移处理改为先读取 ACL，读取失败 fail fast 到 `acl`，成功时在目录事务中先解析目录、再保存快照、再完成目录项和插入子项。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 7, Failures: 0, Errors: 0, Skipped: 0。

BLOCKER: mvn -pl yudao-module-dcc test -> FAIL, Tests run: 0；Surefire/JUnit 测试发现阶段失败，`NoClassDefFoundError: cn/iocoder/yudao/module/infra/service/file/NasAclReadResult`。影响：单模块回归依赖当前本地解析的 `yudao-module-infra` 产物，该产物未包含 Gate1 已提交的 ACL DTO；使用 `-am` 的目标验证可从 reactor 编译最新 infra 并通过。

GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅 PowerShell/Git 提示工作区文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 review-fix-loop worker round 1

BDD: BDD-NAS-ACL-02 descriptor 去重修复 -> Given 两个不同 NAS 目录拥有相同 owner/group/controlFlags/DACL/ACE 内容 / When `captureDirectorySnapshot(...)` 分别采集两个路径 / Then descriptor hash 不得包含目录路径，必须复用同一个 descriptor，且不得重复插入 ACE 行。

BDD: BDD-NAS-ACL-02 路径一致性 fail fast -> Given 调用方传入的 `nasPath` 与 `acl.path()` 规范化后不一致 / When `captureDirectorySnapshot(...)` 执行 / Then 必须在任何 snapshot/descriptor/ACE/directory snapshot insert/update 前抛错，且不得写入任何快照行。

BDD: BDD-NAS-ACL-02 canonical path key -> Given NAS 路径包含大小写差异或中文目录名 / When 计算目录快照 pathHash / Then 必须使用带版本/范围的稳定 canonical path key 进行大小写归一，同时保留有效中文 NAS 路径；若路径包含 `..` traversal，必须 fail fast 且不写入任何快照行。

BDD: BDD-NAS-ACL-02 snapshot header 元数据 -> Given 目录快照首次插入或幂等重跑更新已有目录快照 / When 目录快照写入成功 / Then snapshot header 的 total/snapshotted/failed counts 必须与成功采集结果保持一致。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, Tests run: 12, Failures: 6, Errors: 0, Skipped: 0；新增 reviewer blocker 回归测试证明当前实现仍存在 descriptor hash 包含路径、pathHash 大小写敏感、`nasPath` 与 `acl.path()` 不一致未 fail fast、`..` 被静默折叠、snapshot header 未更新等问题。

IMPLEMENTATION: review-fix-loop worker round 1 -> 移除 descriptor JSON 中的 NAS path；新增 `canonicalNasPath`/`pathHash` helper，以 `DCC_NAS_PATH_KEY_V1:NAS_ACL_V1:<case-folded canonical path>` 计算目录 pathHash，拒绝 `..` traversal，并在读取 task/config 前验证 `nasPath` 与 `acl.path()` canonical key 一致；目录快照 insert/update 成功后更新 snapshot header counts，幂等 SUCCESS 重跑不重复增加计数。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 12, Failures: 0, Errors: 0, Skipped: 0。

GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅 PowerShell/Git 提示工作区文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 review-fix-loop worker round 2

BDD: BDD-NAS-ACL-02 task item path 一致性 fail fast -> Given 转移任务项 `taskItem.nasPath = "3.DMR/02.图纸"`，调用参数 `nasPath = "3.DMR/01.图纸"`，且 `acl.path = "3.DMR/01.图纸"` / When `captureDirectorySnapshot(...)` 执行 / Then 必须在 NAS 配置读取及任何 snapshot/descriptor/ACE/directory snapshot insert/update 前抛错，防止一个 `transfer_task_item_id` 绑定到另一个 NAS 路径的 ACL 快照。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotCaptureServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, Tests run: 7, Failures: 1, Errors: 0, Skipped: 0；新增 `captureDirectorySnapshot_rejectsTaskItemPathMismatchBeforeConfigLookupOrAnyInsertOrUpdate` 失败，原因是当前实现未校验 `taskItem.nasPath` 与已验证的请求/ACL canonical key，一路进入 NAS config 校验并抛出 `nas config required`，证明仍可能在配置可用时继续写入错误目录快照。

IMPLEMENTATION: review-fix-loop worker round 2 -> 在 `captureDirectorySnapshot(...)` 加载任务项并校验 task 归属后、NAS config lookup 之前，canonicalize `taskItem.getNasPath()`，并要求其 canonical key 与已验证的 `nasPath`/`acl.path()` canonical key 一致；不一致时抛出 `taskItem.nasPath must match nasPath after normalization`。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。

GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅 PowerShell/Git 提示工作区文件下次 Git 触碰时 LF 会替换为 CRLF。

REVIEW: review-fix-loop round 3 independent reviewer -> PASS, logic_status=pass, usability_status=pass, ui_status=not_applicable, blocking_issues=[]；允许 T5/T6 service 采集与目录转移集成进入提交。

REGRESSION: mvn -pl yudao-module-dcc -am test -> PASS, reactor 上游模块与 `yudao-module-dcc` 均 SUCCESS；`yudao-module-dcc` Tests run: 237, Failures: 0, Errors: 0, Skipped: 0。该命令使用 reactor 最新 `yudao-module-infra` 代码，覆盖直接单模块命令因本地 stale infra artifact 导致的 `NasAclReadResult` 发现问题。

## 2026-05-27 正式开发第三波 T7 Identity Mapping RED

BDD: BDD-NAS-ACL-03 显式身份映射创建 -> Given NAS ACL 中的 trustee SID/name 已由管理员明确选择映射到 DCC USER 主体 / When 保存身份映射 / Then 必须保存 SID hash、mappingStatus=MAPPED、mappingMethod=MANUAL、target subject、mappedByUserId 和 verifiedAt 等可审计字段，不得按名称模糊匹配或写入默认主体。

BDD: BDD-NAS-ACL-03 身份映射 fail fast -> Given 同一 NAS SID 已 active 映射到一个 DCC 主体、或 targetSubjectType 不是 USER/DEPT/ROLE/POSITION、或目标 DCC 主体不存在/停用 / When 保存身份映射 / Then 必须抛出明确异常且不得写入新映射，不得 fallback 到名称匹配、默认主体或静默未映射。

BDD: BDD-NAS-ACL-04 未映射主体清单 -> Given 一个快照任务已采集 ACE trustee SID，且部分 SID 已存在 active mapping / When 查询 `listUnmappedPrincipals(taskId)` / Then 只返回未映射 SID，并包含 aceCount 与 firstNasPath；已有 active mapping 的 SID 不得返回。

SUBAGENT: T7 Identity Mapping RED test agent -> 已写入 BDD 场景；下一步仅新增身份映射 RED 测试并运行指定 Maven 命令，不写生产代码、不创建 service 实现、不改 SQL。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 中 `yudao-module-infra`、`yudao-module-system`、`yudao-module-bpm` 等上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 2 个错误：找不到 `DccNasPrincipalMappingServiceImpl` 与 `DccNasPrincipalMappingService`；证明当前缺少身份映射 service 契约/实现，尚未满足显式 SID 到 DCC USER/DEPT/ROLE/POSITION 映射、冲突/未知类型/目标缺失或停用 fail fast、以及未映射 SID 清单行为。

SUBAGENT: T7 Identity Mapping RED test agent -> 新增 `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/permission/DccNasPrincipalMappingServiceTest.java` 并追加本执行日志；只写身份映射 RED 测试与日志，未写生产代码、未创建 service 实现、未改 SQL。

## 2026-05-27 T7 RED reviewer contract fix

REVIEW-FIX: T7 RED 契约边界修正 -> 根据 reviewer 反馈，将 `DccNasPrincipalMappingServiceTest` 中的 `AdminUserMapper/DeptMapper/RoleMapper/PostMapper` system DAL mock 修正为 `AdminUserApi/DeptApi/RoleApi/PostApi` system API mock；USER 正向映射验证 `adminUserApi.validateUserList`，ROLE/DEPT/POSITION 目标校验分别验证 `roleApi.validRoleList`、`deptApi.validateDeptList`、`postApi.validPostList`，API 抛错时断言不 insert；未知 `targetSubjectType` 仍要求在任何 API lookup/insert 前 fail fast。未写生产代码、未改 SQL、未创建 service。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 中 `yudao-module-infra`、`yudao-module-system`、`yudao-module-bpm` 等上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 2 个错误：找不到 `DccNasPrincipalMappingServiceImpl` 与 `DccNasPrincipalMappingService`；证明修正后 RED 仍聚焦于 DCC 缺少身份映射 service 契约/实现，且不再引入 system DAL 耦合。

## 2026-05-27 正式开发第三波 T8 Identity Mapping GREEN implementation

IMPLEMENTATION: T8 DCC NAS 主体映射 GREEN -> 新增 `cn.iocoder.yudao.module.dcc.service.permission.DccNasPrincipalMappingService` 契约和 `DccNasPrincipalMappingServiceImpl`；`saveMapping(...)` 对 SID trim 后 uppercase 并计算 SHA-256 sidHash，限定 targetSubjectType 只能为 USER/DEPT/ROLE/POSITION，分别通过 `AdminUserApi.validateUserList`、`DeptApi.validateDeptList`、`RoleApi.validRoleList`、`PostApi.validPostList` 验证目标主体；同 sidHash 已有 active MAPPED 且主体不同则抛出明确 conflict 异常并阻止 insert，同主体则返回既有映射；新映射写入 MAPPED/MANUAL、source SID/domain/account、DCC subject、mappedByUserId、verifiedAt。`listUnmappedPrincipals(taskId)` 基于 SUCCESS directory snapshot 与 ACE trusteeSidHash 汇总 SID、sidHash、aceCount、firstNasPath，并排除 active MAPPED mapping。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 19, Failures: 0, Errors: 0, Skipped: 0。

RISK: 未解决风险无；本轮按 sub agent 范围未改 SQL、controller、前端、恢复计划，未提交，由主 reviewer 继续放行。

## 2026-05-27 review-fix-loop T7/T8 worker round 1

BDD: BDD-NAS-ACL-04 shared descriptor 未映射主体计数 -> Given 两个 SUCCESS `DccNasAclDirectorySnapshotDO` 共享同一个 `descriptorId`，同一个未映射 SID 的 ACE 存在于该 descriptor，且另有一个 FAILED 目录快照引用同 descriptor、一个 active MAPPED SID 也存在于 ACE 中 / When 查询 `listUnmappedPrincipals(taskId)` / Then 未映射 SID 必须按每个 SUCCESS 目录引用累计 `aceCount = 2`，`firstNasPath` 取第一次出现的成功目录，FAILED 目录不计入，active MAPPED SID 不返回。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, Tests run: 7, Failures: 1, Errors: 0, Skipped: 0；新增 `listUnmappedPrincipals_countsSharedDescriptorAceForEachSuccessfulDirectorySnapshot` 失败，断言 `aceCount` 期望 2 实际为 1，证明当前 `listUnmappedPrincipals` 按 distinct descriptor 聚合后只计算了一次 ACE，未按两个 SUCCESS directory snapshot 引用累计。

IMPLEMENTATION: review-fix-loop T7/T8 worker round 1 -> `listUnmappedPrincipals(taskId)` 保持按 SUCCESS directory snapshot 查询，读取 ACE 时仍按 distinct descriptorId 批量查询；聚合阶段改为按每个 SUCCESS directory snapshot 引用展开 descriptor ACE，并在内存中再次排除非 SUCCESS directory snapshot，确保共享 descriptor 的 ACE 按成功目录引用计数，FAILED 目录不计入，active MAPPED 过滤逻辑保持不变。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 7, Failures: 0, Errors: 0, Skipped: 0。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 20, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅 PowerShell/Git 提示任务文档文件下次 Git 触碰时 LF 会替换为 CRLF。

REVIEW: review-fix-loop T7/T8 round 2 independent reviewer -> PASS, logic_status=pass, usability_status=pass, ui_status=pass, blocking_issues=[]；确认共享 descriptor 计数按 SUCCESS directory snapshot 引用累计、FAILED 目录被排除、active MAPPED SID 被过滤，`saveMapping(...)` 仍保持 SID/类型/目标主体/冲突 fail fast，允许 T7/T8 身份映射服务进入提交。

REGRESSION: mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 7, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 20, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am test -> PASS, reactor 上游模块与 `yudao-module-dcc` 均 SUCCESS；`yudao-module-dcc` Tests run: 244, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅 PowerShell/Git 提示任务文档文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 正式开发第四波 T9 Restore service RED

BDD: BDD-NAS-ACL-04 恢复预览只读生成可恢复计划 -> Given 快照任务状态为 CAPTURED，存在 SUCCESS 目录快照、ALLOW ACE，并且 ACE trustee SID 有 active MAPPED 身份映射 / When 调用 `DccNasPermissionRestoreService.preview(taskId)` / Then 返回 `canRestore=true`、`restoreMode=REPLACE_DIRECTORY_RULES`、目录数/规则数和示例规则，且不得写恢复计划/计划项/日志，也不得改写 DCC 目录运行时访问规则。

BDD: BDD-NAS-ACL-04 未映射 SID 阻断恢复预览 -> Given SUCCESS 目录快照中的 ACE trustee SID 没有 active MAPPED 身份映射 / When 调用 `preview(taskId)` / Then 返回 `canRestore=false`，blocker code/message 必须指向未映射 SID，且不得写任何 DCC 运行时目录访问规则。

BDD: BDD-NAS-ACL-06 DENY 或不可映射 ACE 阻断恢复预览 -> Given SUCCESS 目录快照中存在 `ACCESS_DENIED_ACE_TYPE` 或 accessMask 无法映射到 DCC `canQuery/canPreview/canDownload` / When 调用 `preview(taskId)` / Then 返回 `canRestore=false`，blocker 必须指向不可恢复语义，不得把 DENY 或特殊权限降级成 allow 型 DCC 规则。

BDD: BDD-NAS-ACL-05 显式应用恢复只创建可审计恢复任务 -> Given 最新预览 planHash 已生成且无 blocker / When 应用命令携带相同 `planHash` 与 `idempotencyKey` / Then 创建 restore plan、plan item、restore log 或 WAITING 恢复任务审计记录，不在 HTTP/service 同步调用中直接批量写入所有目录规则；旧 `planHash` 必须 fail fast，且不得写运行时目录访问规则。

SUBAGENT: T9 Restore service RED test agent -> 已写入 BDD 场景；下一步仅新增恢复预览/应用 service RED 测试并运行指定 Maven 命令，不写生产代码、不创建 service 实现、不改 SQL、不提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 2 个错误：找不到 `DccNasPermissionRestoreServiceImpl` 与 `DccNasPermissionRestoreService`；证明当前缺少恢复预览/应用 service 契约与实现，尚未满足 preview 只读计划生成、未映射 SID blocker、DENY/不可映射 ACE blocker、匹配最新 planHash 的显式恢复应用和可审计 WAITING 恢复任务行为。失败未暴露测试语法、现有 mapper/API import 或 SQL 依赖问题。

SUBAGENT: T9 Restore service RED test agent -> 新增 `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/permission/DccNasPermissionRestoreServiceTest.java` 并追加本执行日志；只写恢复预览/应用 service RED 测试，未写生产代码、未创建 service 实现、未改 SQL、未提交。

REVIEW: review-fix-loop T9 RED round 1 independent reviewer -> FAIL, logic_status=fail, usability_status=pass, ui_status=not_applicable；blockers 为测试将 `dcc_nas_acl_restore_plan.status` 错误断言为 `WAITING`、错误要求 restore log 写入 `CREATE_RESTORE_TASK/WAITING`、DENY 与 unsupported accessMask blocker 断言过松。T9 RED 不放行，必须先修正测试契约。

REVIEW-FIX: T9 RED 测试契约修正 -> 将 API `ApplyResult.status()` 保持为 `WAITING`，但恢复计划持久化状态修正为合法状态 `READY`；移除 `CREATE_RESTORE_TASK/WAITING` restore log 写入断言，恢复日志保留给后续单项 VALIDATE/APPLY/VERIFY 执行证据；拆分 DENY 与 unsupported accessMask blocker 测试，分别要求 `DCC_NAS_ACL_DENY_UNSUPPORTED` 与 `DCC_NAS_ACL_SPECIAL_MASK_UNSUPPORTED`。仍只修改 RED 测试和任务文档，未写生产代码、未改 SQL、未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 2 个错误：找不到 `DccNasPermissionRestoreServiceImpl` 与 `DccNasPermissionRestoreService`；证明修正后的 T9 RED 仍聚焦于恢复 service 契约缺失，未引入测试语法、mapper/API import 或 SQL 依赖问题。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅 PowerShell/Git 提示任务文档文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 正式开发第四波 T10 Restore service GREEN implementation

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, `yudao-module-dcc` testCompile 仅 2 个错误：找不到 `DccNasPermissionRestoreServiceImpl` 与 `DccNasPermissionRestoreService`，确认 T9 RED 仍聚焦于恢复 service 契约缺失。

IMPLEMENTATION: T10 Restore service 最小 GREEN -> 新增 `cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionRestoreService` 与 `DccNasPermissionRestoreServiceImpl`；`preview(taskId)` 只读取状态为 `CAPTURED` 的已保存 raw ACL snapshot 与 `SUCCESS` directory snapshot，不重新读 NAS；ALLOW 且已确认可恢复 accessMask 通过 active `MAPPED` identity mapping 生成 `REPLACE_DIRECTORY_RULES` 预览规则，首版映射为 `canQuery=true, canPreview=true, canDownload=true`；未映射 SID、DENY ACE、不可恢复 accessMask 分别返回 `DCC_NAS_PRINCIPAL_UNMAPPED`、`DCC_NAS_ACL_DENY_UNSUPPORTED`、`DCC_NAS_ACL_SPECIAL_MASK_UNSUPPORTED` blocker；`planHash` 使用稳定 SHA-256。`apply(command)` 重新 preview 并校验 `restoreMode=REPLACE_DIRECTORY_RULES`、无 blocker、`planHash` 匹配后，仅插入 `dcc_nas_acl_restore_plan` READY 与 `dcc_nas_acl_restore_plan_item` WAITING 队列，`validationSummaryJson` 包含 planHash，`plannedOperationsJson` 包含 subjectId 与 canDownload；未写 restore log，未写 `dcc_directory_access_rule`，未改 SQL/schema/controller/frontend。

GREEN-ITERATION: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 生产代码行为断言已执行到测试阶段，但 Mockito strictness 报告测试预置的 `snapshotMapper.selectById`、`identityMappingMapper.selectOne`、`directoryAccessRuleMapper.selectListByDirectoryId` stub 未被触达；随后实现补齐快照 byId 校验、单 SID active mapping 校验以及目录运行时规则只读读取，并把运行时规则纳入 planHash 负载，仍不写运行时规则。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 25, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 T9/T10 restore service reviewer blocker repair

BDD: BDD-NAS-ACL-05 restore plan lookup 必须批量化 -> Given 恢复快照包含多个目录、多个 SID hash 和多个 DCC directoryId / When `preview(taskId)` 或 `apply(command)` 构建恢复计划 / Then 身份映射只能通过分块 `IN sidHash` 批量读取，运行时目录规则只能通过分块 `IN directoryId` 批量读取，不得调用每 SID `identityMappingMapper.selectOne(...)` 或每目录 `directoryAccessRuleMapper.selectListByDirectoryId(...)`。

BDD: BDD-NAS-ACL-05 restore API 业务失败必须稳定错误码 -> Given 快照未就绪、恢复计划有 blocker、planHash 过期、restoreMode 不支持、或同一 idempotencyKey 被不同请求复用 / When 调用 restore preview/apply / Then 对外 API 业务失败必须抛 `ServiceException` 并携带 DCC restore 专用 error code，不得以裸 `IllegalStateException` 表达。

BDD: BDD-NAS-ACL-05 restore apply 幂等 -> Given 同一 `taskId + idempotencyKey` 已经创建过 restore plan / When 重复提交完全一致的 apply 请求 / Then 返回既有 `ApplyResult` 且不新增 restore plan item；When 同 key 携带不同 `planHash` 或不同请求体 / Then 抛 `DCC_NAS_PERMISSION_RESTORE_IDEMPOTENCY_CONFLICT`，且不写 restore plan item、restore log 或运行时目录规则。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, Tests run: 9, Failures: 5, Errors: 4, Skipped: 0；失败覆盖 `identityMappingMapper.selectOne(...)` 被调用 3 次、restore 业务失败仍抛 `IllegalStateException`、重复 idempotency 请求仍再次 `insert` restore plan，以及 Mockito strictness 暴露旧 per-directory runtime rule stub 路径。

IMPLEMENTATION: replacement repair worker -> `DccNasPermissionRestoreServiceImpl` 移除每 SID/per-directory 查询，改为 500 条一组分块 `selectList(... in sidHashes/directoryIds ...)` 并按 `directoryId` 分组；restore snapshot not ready、blocked、plan stale、unsupported mode、idempotency conflict 改用 `ServiceExceptionUtil.exception(...)` 与 DCC restore ErrorCodeConstants；apply 写入前先按 `planKey(taskId,idempotencyKey)` 查询既有 plan，校验 `validationSummaryJson` 中的 `idempotencyRequestHash`，一致则返回既有结果，不一致则 fail fast 冲突且不继续构建/写入计划；测试补充 apply 路径 N+1 verify、同 key 不同 planHash 冲突、同 key 不同请求体冲突。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 9, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 29, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档文件下次 Git 触碰时 LF 会替换为 CRLF。

REVIEW: review-fix-loop T9/T10 round 2 independent reviewer -> PASS, logic_status=pass, usability_status=pass, ui_status=not_applicable, blocking_issues=[]；确认第 1 轮 blocker 均已修复：identity mapping 与 runtime directory rule 查询不再 N+1，restore 业务失败使用 ErrorCodeConstants + ServiceException，apply 幂等语义覆盖相同请求复用和不同请求冲突，execution-log 顺序保持 RED -> GREEN，preview/apply 仍不写运行时目录规则或 restore log。

REGRESSION: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 9, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 29, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am test -> PASS, reactor 上游模块与 `yudao-module-dcc` 均 SUCCESS；`yudao-module-dcc` Tests run: 253, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档文件下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected before slice commit；当前 worktree 仍有本切片生产代码/测试待提交，未执行 apply、未删除任何文件。

## 2026-05-27 T10b Restore controller/API RED

BDD: BDD-NAS-ACL-07 恢复预览 API 返回恢复计划合同 -> Given 一个已采集 NAS ACL 快照任务已经具备 restore service 预览结果 / When 管理端调用 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore/preview` / Then controller 必须调用 `DccNasPermissionRestoreService.preview(taskId)`，通过 `CommonResult` 返回 `taskId`、`canRestore`、`planHash`、`restoreMode`、`directoryCount`、`ruleCount`、`blockers` 与 `sampleRules`，不得返回裸对象或省略 blocker/sample rule 合同字段。

BDD: BDD-NAS-ACL-08 显式应用恢复 API 使用当前登录人 -> Given 管理端提交恢复申请体包含 `idempotencyKey`、`planHash`、`restoreMode` 与 `changeReason` / When 调用 `POST /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore` / Then controller 必须构造 `ApplyRestoreCommand(taskId, idempotencyKey, planHash, restoreMode, changeReason, getLoginUserId())` 调用 service，并通过 `CommonResult` 返回 `restoreId`、`status` 与目录/规则计数。

BDD: BDD-NAS-ACL-09 恢复应用请求体必填字段校验 -> Given 管理端提交应用恢复请求 / When `idempotencyKey`、`planHash` 或 `restoreMode` 为空 / Then reqVO 必须通过 `@NotBlank` 暴露校验错误，不得把缺失字段静默替换为默认值、空字符串或 fallback restore mode。

SUBAGENT: T10b RED test agent -> 已新增 controller/API 合同 RED 测试；下一步运行指定 Maven 命令，预期当前因缺少 `DccNasPermissionRestoreController` 与恢复 controller VO 在 testCompile 阶段失败。不写生产代码、不创建 controller/VO、不改 service、不暂存提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 5 个错误：找不到 `DccNasPermissionRestoreController`、`DccNasPermissionRestoreApplyReqVO`、`DccNasPermissionRestoreApplyRespVO`、`DccNasPermissionRestorePreviewRespVO`，以及测试字段注入处的 `DccNasPermissionRestoreController` 类型；证明当前缺少恢复预览/应用 HTTP controller 与 VO/API 合同，尚未满足 CommonResult 返回、preview 字段映射、apply command 携带当前登录用户、以及 reqVO `@NotBlank` 校验合同。失败未暴露测试语法、service 契约、mapper/API import 或 SQL 依赖问题。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 主任务复跑确认 testCompile 仅 5 个缺失类型错误，RED 聚焦 controller/VO/API 合同缺失。

IMPLEMENTATION: T10b Restore controller/API 最小 GREEN -> 新增 `DccNasPermissionRestoreController`，挂载 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore/preview` 与 `POST /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore`；新增 `DccNasPermissionRestorePreviewRespVO`、`DccNasPermissionRestoreApplyReqVO`、`DccNasPermissionRestoreApplyRespVO`。controller 只做 service result 到 VO 的字段映射，以及将 `taskId`、请求体和 `getLoginUserId()` 组装为 `ApplyRestoreCommand`；未改 restore service、SQL/schema、frontend、restore log 或运行时目录规则写入。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 32, Failures: 0, Errors: 0, Skipped: 0。

REVIEW: review-fix-loop T10b round 2 independent reviewer -> PASS, logic_status=pass, usability_status=pass, ui_status=pass, blocking_issues=[]；确认第 1 轮 blocker 已修复：preview API 暴露 `runtimeEnforcementReady=true` 与 `runtimeEnforcementBlocker=null`，service/VO/test 一致，controller 仍只做 API 映射与 service delegation，未写 mapper、restore log 或运行时目录规则。

REGRESSION: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 32, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am test -> PASS, reactor 上游模块与 `yudao-module-dcc` 均 SUCCESS；`yudao-module-dcc` Tests run: 256, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档和 Java 文件下次 Git 触碰时 LF 会替换为 CRLF。

CONTRACT: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected before T10b slice commit；preview 保留 `task.md` 与 `execution-log.md`，但因当前 worktree 仍有本切片待提交的 controller/VO/service/test 改动，未执行 apply、未删除任何文件。

## 2026-05-27 T10b Restore controller/API reviewer blocker repair

REVIEW: review-fix-loop T10b round 1 independent reviewer -> FAIL, logic_status=fail, usability_status=fail, ui_status=pass；blocking issue 为恢复预览 API 缺少后端设计要求的 `runtimeEnforcementReady` 与 `runtimeEnforcementBlocker` 字段，当前 service preview result、preview resp VO 和 controller test 均未暴露或断言该合同。

BDD: BDD-NAS-ACL-10 恢复预览 API 必须暴露 DCC runtime enforcement readiness -> Given Gate2 已验证 DCC 运行时详情、预览、下载目录权限 enforcement ready / When 管理端调用恢复预览 API / Then preview response 必须返回 `runtimeEnforcementReady=true` 与 `runtimeEnforcementBlocker=null`，不得省略字段、不得使用 fallback、不得写运行时目录规则或 restore log。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 1 个错误：`DccNasPermissionRestoreService.PreviewResult` 构造器缺少 `runtimeEnforcementReady` 与 `runtimeEnforcementBlocker` 参数；证明当前恢复预览 service/VO/API 合同尚未覆盖 runtime enforcement readiness 字段。

IMPLEMENTATION: T10b reviewer blocker repair -> 扩展 `DccNasPermissionRestoreService.PreviewResult` 增加 `runtimeEnforcementReady` 与 `runtimeEnforcementBlocker`；`DccNasPermissionRestoreServiceImpl.preview(...)` 基于 Gate2 已通过的运行时目录权限 enforcement 固定返回 `true/null`；`DccNasPermissionRestorePreviewRespVO` 映射并返回这两个字段；controller 仍只做 service delegation 与 VO mapping，未写 `dcc_directory_access_rule`、未写 restore log、未新增 fallback。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 32, Failures: 0, Errors: 0, Skipped: 0。

## 2026-05-27 T10c Restore execution service RED

BDD: BDD-NAS-ACL-11 恢复执行按目录替换规则并写审计日志 -> Given 已有 READY restore plan、WAITING restore plan item，且 `plannedOperationsJson` 包含 `directoryId`、`expectedCurrentRuleHash`、`expectedAfterHash` 与目标 `replaceDirectoryRules` / When 后台执行 `DccNasPermissionRestoreExecutionService.processWaitingRestorePlans()` / Then 服务必须先校验当前 `dcc_directory_access_rule` hash，再为该目录写入 VALIDATE、APPLY、VERIFY 三类 `dcc_nas_acl_restore_log`，用计划规则替换该目录旧规则，将 item 标记为 VERIFIED，并在全部 item 完成后将 plan 标记为 COMPLETED 且更新完成/失败计数。

BDD: BDD-NAS-ACL-12 当前目录规则 hash 不匹配时阻断恢复 -> Given WAITING restore item 中的 `expectedCurrentRuleHash` 来自预览时刻，但当前 `dcc_directory_access_rule` 已在预览后被修改 / When 后台执行恢复服务 / Then 服务必须 fail fast，不删除、不插入目录访问规则，将 item 标记为 FAILED 或 BLOCKED，将 plan 标记为 FAILED，并写入包含明确 `DCC_NAS_ACL_RESTORE_CURRENT_HASH_MISMATCH` errorCode/errorMessage 的 VALIDATE restore log。

SUBAGENT: T10c RED test agent -> 已新增恢复执行 service RED 测试；测试契约选择 `processWaitingRestorePlans()` 作为 scheduler/job 友好的后台入口，覆盖 READY plan + WAITING item 的按目录执行、VALIDATE/APPLY/VERIFY 日志、运行时目录规则替换、item/plan 状态更新，以及预览后 hash mismatch 阻断。不写生产代码、不创建 service interface/impl、不改 SQL/schema、不提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 2 个错误：找不到 `DccNasPermissionRestoreExecutionServiceImpl` 与 `DccNasPermissionRestoreExecutionService`。该失败聚焦于 T10c 恢复执行 service 契约/实现缺失，尚未满足等待恢复计划的执行、hash 校验、VALIDATE/APPLY/VERIFY 审计日志、目录规则替换和 hash mismatch fail-fast 行为；未暴露测试语法、import、schema 或既有用例破坏问题。

SUBAGENT: T10c RED test agent -> 新增 `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/permission/DccNasPermissionRestoreExecutionServiceTest.java` 并追加本执行日志；只写 RED 测试与任务日志，未修改 `src/main/...`、未改 SQL/schema、未提交。

REVIEW-FIX: T10c RED 测试契约修正 -> 根据复核意见，成功路径 `processWaitingRestorePlans_replacesDirectoryRulesAndWritesValidateApplyVerifyLogs` 新增 `DccDirectoryAccessRuleMapper` 写调用顺序断言：必须先出现 `delete*` 旧规则删除/替换写调用，再出现 `insert*` 目标规则写调用，避免只追加新规则也通过 BDD-NAS-ACL-11。hash mismatch 失败路径继续保留 `assertNoWriteCalls(directoryAccessRuleMapper)`，确保不删除、不插入、不更新目录规则。仍只修改 RED 测试与任务日志，未写生产代码、未改 SQL/schema、未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仍仅 2 个错误：找不到 `DccNasPermissionRestoreExecutionServiceImpl` 与 `DccNasPermissionRestoreExecutionService`。复跑证明本次契约修正未引入测试语法、mapper API、schema 或既有用例破坏问题；RED 仍聚焦于 T10c 恢复执行 service 契约/实现缺失。

## 2026-05-27 T10c Restore execution service GREEN

IMPLEMENTATION: T10c Restore execution service 最小 GREEN -> 新增 `DccNasPermissionRestoreExecutionService` 与 `DccNasPermissionRestoreExecutionServiceImpl`；执行入口 `processWaitingRestorePlans()` 查询 READY restore plan 与 WAITING item，解析 `plannedOperationsJson` 的 `directoryId`、`expectedCurrentRuleHash`、`expectedAfterHash`、`replaceDirectoryRules`，使用与 RED 测试一致的 canonical directory rule hash 校验当前运行时规则。hash 匹配时写 VALIDATE SUCCESS、删除该目录旧 `dcc_directory_access_rule`、插入目标规则、写 APPLY SUCCESS、重读并校验 after hash、写 VERIFY SUCCESS、将 item 标记 VERIFIED 并更新 plan COMPLETED 与 completed/failed directory count；hash 不匹配时只写 VALIDATE FAILED restore log，将 item/plan 标记失败并记录 `DCC_NAS_ACL_RESTORE_CURRENT_HASH_MISMATCH`，不对 `dcc_directory_access_rule` 发起 delete/insert/update。同步补齐 `DccNasPermissionRestoreServiceImpl.apply(...)` 生成真实可执行 item 的 `directoryId`、`expectedCurrentRuleHash`、`expectedAfterHash` 与 item `expectedAfterHash`；apply 仍只创建 plan/item，不写 restore log，不写运行时规则。

GREEN-ITERATION: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, `yudao-module-dcc` compile 暴露 `RestoreItemResult` record 组件与静态工厂同名导致 accessor 非 public；修正为 `succeeded` 组件和 `success()` 工厂后复跑。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档与 `DccNasPermissionRestoreServiceImpl.java` 下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected before reviewer/commit；preview 保留 `task.md` 与 `execution-log.md`，但因当前 worktree 仍有本切片待复核的 production/test 改动，未执行 apply、未删除任何文件。

REVIEW-FIX: T10c reviewer test hardening -> 根据第二轮复核意见补强 RED/test contract：`DccNasPermissionRestoreExecutionServiceTest` 增加 `TransactionTemplate` mock 并同步执行 callback，成功、hash mismatch 和双 WAITING item 场景均断言每个 WAITING item 至少进入一次 `transactionTemplate.execute(...)`，防止整个 `processWaitingRestorePlans()` 用一个大事务覆盖所有目录项；`DccNasPermissionRestoreServiceTest` 新增 preview planHash 敏感性测试，固定同一快照、同一 mapped SID、同一目录规则权限字段，仅改变当前 `DccDirectoryAccessRuleDO.changeReason` 时要求两次 preview `planHash` 不同。仍只修改测试和任务日志/状态，未写生产代码、未改 SQL/schema、未提交。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 通过并进入测试阶段；Tests run: 13, Failures: 4, Errors: 0, Skipped: 0。失败聚焦于两项行为契约：`DccNasPermissionRestoreExecutionServiceTest` 3 个用例均显示 `transactionTemplate.execute(<any>)` wanted but not invoked、实际 0 次调用，说明当前实现仍未按目录项短事务执行；`DccNasPermissionRestoreServiceTest.preview_planHashChangesWhenRuntimeDirectoryRuleChangeReasonChanges` 断言失败，两个 preview 返回相同 `sha256:b7f6a43e26a1308e44ccd44d77ad3ebe5afc5ef546dab414fb5565c8393bb8e6`，说明当前 planHash 仍未纳入运行时目录规则 `changeReason`。未出现测试语法、import、schema 或既有用例破坏问题。

IMPLEMENTATION: T10c reviewer test hardening GREEN -> `DccNasPermissionRestoreExecutionServiceImpl` 移除外层整批 `@Transactional`，新增 Spring 注入的 `TransactionTemplate`，在 `processPlan(...)` 中对每个 WAITING item 调用 `transactionTemplate.execute(status -> processItem(...))` 执行，缺失 `TransactionTemplate` 不做 fallback；`DccNasPermissionRestoreServiceImpl.planHashRuntimeRules(...)` 的 runtime directory rule payload 纳入 `changeReason`，与 canonical directory rule payload 语义保持一致。未改 SQL/schema/controller/frontend，未提交。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 36, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档、`DccNasPermissionRestoreServiceImpl.java` 与 `DccNasPermissionRestoreServiceTest.java` 下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected before reviewer/commit；preview 保留 `task.md` 与 `execution-log.md`，但因当前 worktree 仍有本切片待复核的 production/test 改动，未执行 apply、未删除任何文件。

REVIEW-FIX: T10c restore execution data model contract RED -> 根据第三轮复核意见修正/补强 `DccNasPermissionRestoreExecutionServiceTest`：恢复日志成功状态按 `data-model.md` 合同改为 `SUCCEEDED`，不再接受旧的 `SUCCESS`；hash mismatch 的 VALIDATE FAILED 日志要求 `beforeHash` 记录实际当前目录规则 hash，`expectedAfterHash` 保持计划目标 hash，未 APPLY 时 `actualAfterHash=null`，并要求 `errorMessage` 同时包含 stale expected current hash 与 `directoryId`；新增断点续跑计数测试，Given 同一 READY plan 下已有一个 VERIFIED item 与一个 WAITING item，When 成功处理 WAITING item，Then plan COMPLETED 的 `validationSummaryJson` 必须累计 `completedDirectoryCount=2`、`failedDirectoryCount=0`。仍只修改测试与任务日志/状态，未写生产代码、未改 SQL/schema、未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 通过并进入测试阶段；Tests run: 4, Failures: 3, Errors: 0, Skipped: 0。失败聚焦于第三轮契约：成功日志断言期望 `SUCCEEDED` 但实际为 `SUCCESS`；hash mismatch VALIDATE FAILED 断言期望 `beforeHash` 为 actual current hash `sha256:ff6a2f4a7c9006600dc2d45c21655455f0a18895d1e78f616473c6ab3da732c8`，实际为 stale expected hash `sha256:3de7ec20d30fdea41d4e264d957e8d6657bf65c4b95521b139eb7379e9f16df1`；断点续跑计数断言 `validationSummaryJson` 包含 `"completedDirectoryCount":2` 失败，说明当前实现仍只统计本轮处理的 1 个 WAITING item。未出现测试语法、import、schema 或既有用例破坏问题。

## 2026-05-27 T10c restore execution data model contract GREEN

IMPLEMENTATION: T10c data model contract hardening GREEN -> `DccNasPermissionRestoreExecutionServiceImpl` 将 restore log 成功状态从 `SUCCESS` 改为 data model 合同的 `SUCCEEDED`；hash mismatch 的 VALIDATE FAILED 日志改为 `beforeHash=actualCurrentHash`、`actualAfterHash=null`，errorMessage 保留 stale expected current hash 与 `directoryId`；`processPlan(...)` 改为先查询 plan 下 item 并过滤 WAITING 执行，每个 WAITING item 仍通过 `TransactionTemplate.execute(...)` 短事务处理，完成后重读 plan item 并叠加本轮已处理状态统计 `VERIFIED` 与 `FAILED/BLOCKED`，使断点续跑时已有 VERIFIED item 与本轮成功 item 一并累计到 validationSummaryJson。hash mismatch 路径仍不写 `dcc_directory_access_rule`，未改 SQL/schema/controller/frontend，未提交。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 14, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 37, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档、`DccNasPermissionRestoreServiceImpl.java` 与 `DccNasPermissionRestoreServiceTest.java` 下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected before reviewer/commit；preview 保留 `task.md` 与 `execution-log.md`，但因当前 worktree 仍有本轮 production/test 改动且用户要求不提交，未执行 apply、未删除任何文件。

## 2026-05-27 T10c review-fix-loop round 3 reviewer PASS

REVIEW: .review-fix-loop/runs/20260527-nas-permission-restore-execution-t10c/review/report-round-3.md -> PASS, logic_status=pass, usability_status=pass, ui_status=pass, final_decision=pass。独立 reviewer 确认 round 1 blocker、主 reviewer 补充 blocker 与 round 2 秒级精度 blocker 均已修复：READY/stale EXECUTING 原子 claim/reclaim、item 边界 lease refresh CAS、空/不一致 item fail fast、claim 后异常审计、failed item 计数一致、claim/reclaim/refresh timestamp 秒级归一化均有代码与测试约束；未发现新阻塞。

GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

REGRESSION: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 17, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 27, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 50, Failures: 0, Errors: 0, Skipped: 0；`DccControlledFileNasTransferServiceTest.processWaitingTasks_truncatesLongTaskFailureMessage` 仍会打印预期 ERROR 日志，但 Surefire 结果为 PASS。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示部分已修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 后端提交与收尾

COMMIT: git commit -m "任务: 补齐NAS权限恢复API接口" -> PASS, commit `91c31a4457`。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED, preview 保留 `task.md` 与 `execution-log.md`，列出历史任务附属文档为可清理项，但因当前任务分支不能 fast-forward merge 到 `int_main` 且主后端 worktree 有脏改动而阻塞；未执行 apply、未删除任何文件。

## 2026-05-27 T10c review-fix-loop round 1 lease refresh RED

BDD: BDD-NAS-ACL-17 长时间 EXECUTING restore plan 必须在 item 边界刷新执行 lease -> Given 一个包含多个 WAITING/APPLIED item 的 restore plan 已被当前调度实例 claim/reclaim 为 EXECUTING / When 后台逐个处理可执行 item / Then 每个可执行 item 边界必须通过 `refreshExecutingPlanLease(planId, currentLeaseStartedAt, refreshedAt)` 原子 CAS 刷新 lease，避免活跃执行者超过 stale 阈值后被下一轮调度误 reclaim。

BDD: BDD-NAS-ACL-18 lease refresh 失败必须停止本轮执行 -> Given 当前调度实例准备处理下一个 restore plan item 但 `refreshExecutingPlanLease(...)` 返回 0，说明 lease 已被其他执行者更新或不再匹配 / When 后台执行 `processWaitingRestorePlans()` / Then 本轮必须立即停止处理该 plan，不得继续写目录规则、restore log 或 plan final 状态，避免并发重复执行。

REVIEW-FIX: T10c stale EXECUTING active runner blocker RED -> 根据主 reviewer 打回意见补强 `DccNasPermissionRestoreExecutionServiceTest`：新增多 item 长计划 lease refresh 链路断言，要求每个可执行 item 边界刷新 lease 且下一次 refresh 的 current lease 等于上一次 refreshed lease；新增 refresh 第二次返回 0 的断言，要求只处理第一个 item，不处理第二个 item，不写 plan final 状态。仍只修改测试与任务日志/状态，未写生产代码、未改 SQL/schema、未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 4 个错误，均为 `DccNasAclRestorePlanMapper.refreshExecutingPlanLease(Long, LocalDateTime, LocalDateTime)` 找不到符号。该失败聚焦于长 EXECUTING plan 缺少 item 边界 CAS lease refresh 合同；未出现测试语法、import、schema 或既有用例破坏问题。

## 2026-05-27 T10c review-fix-loop round 1 lease refresh GREEN

IMPLEMENTATION: T10c stale EXECUTING active runner blocker GREEN -> `DccNasAclRestorePlanMapper` 新增 `refreshExecutingPlanLease(Long planId, LocalDateTime currentStartedAt, LocalDateTime refreshedAt)` default 方法，使用 `id/status='EXECUTING'/startedAt=currentStartedAt` 原子 CAS 更新 `startedAt=refreshedAt`；`DccNasPermissionRestoreExecutionServiceImpl` 在 READY claim 或 stale EXECUTING reclaim 成功后维护当前 lease timestamp，并在每个 WAITING/APPLIED 可执行 item 执行前刷新 lease。refresh 返回 0 时立即停止本轮 plan 处理，不进入后续 item transaction，不继续写目录规则、restore log 或 plan final 状态；READY claim 与 stale EXECUTING reclaim 原子行为保持不变。未改 SQL/schema/controller/frontend，未提交。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 16, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 26, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 49, Failures: 0, Errors: 0, Skipped: 0；`DccControlledFileNasTransferServiceTest.processWaitingTasks_truncatesLongTaskFailureMessage` 仍会打印预期 ERROR 日志，但 Surefire 结果为 PASS。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示部分已修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 T10c review-fix-loop round 2 lease precision RED

BDD: BDD-NAS-ACL-20 lease CAS timestamp 必须匹配 schema 秒级精度 -> Given `dcc_nas_acl_restore_plan.started_at` 在 MySQL/H2 中为秒级 `datetime` / When 恢复执行服务 claim READY plan、reclaim stale EXECUTING plan 或 refresh EXECUTING lease / Then 进入 mapper 的 lease timestamp 必须统一归一化到秒级，`getNano()==0`，避免真实数据库截断后 CAS where 匹配不到。

BDD: BDD-NAS-ACL-21 claim/reclaim 后首个 refresh 必须使用同一秒级 lease -> Given 当前执行者刚成功 claim 或 reclaim 一个 plan / When 处理第一个 WAITING/APPLIED item 前刷新 lease / Then refresh 的 current lease 必须等于上一轮写入 DB 的秒级 lease，不能使用未归一化的 Java 纳秒时间。

REVIEW-FIX: T10c round 2 lease precision RED -> 根据 round 2 reviewer blocker 补强 `DccNasPermissionRestoreExecutionServiceTest`：READY 多 item lease refresh 测试捕获 `claimReadyPlan(...)` 与 `refreshExecutingPlanLease(...)` 参数，要求 claimed/current/refreshed lease 均为秒级且首个 refresh current lease 等于 claimed lease；新增 stale EXECUTING reclaim 测试，要求 `reclaimExecutingPlan(...)` 的 current/reclaimed lease 均为秒级，首个 refresh current lease 等于 reclaimed lease。仍只修改测试与任务日志，未写生产代码、未改 SQL/schema、未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 通过并进入测试阶段；Tests run: 17, Failures: 2, Errors: 0, Skipped: 0。失败用例为 `processWaitingRestorePlans_refreshesExecutingLeaseBeforeEachExecutableItem` 与 `processWaitingRestorePlans_usesSecondPrecisionLeaseForStaleReclaimAndFirstRefresh`，均断言 lease `getNano()==0` 失败，证明当前 claim/reclaim/refresh 仍把 `LocalDateTime.now()` 纳秒值传入 mapper。

## 2026-05-27 T10c review-fix-loop round 2 lease precision GREEN

IMPLEMENTATION: T10c round 2 lease precision GREEN -> `DccNasPermissionRestoreExecutionServiceImpl` 统一通过 `leaseNow()` 生成秒级 lease timestamp，并对 stale EXECUTING plan 的既有 `startedAt` 使用 `normalizeLeaseTimestamp(...)` 后再进入 `reclaimExecutingPlan(...)` CAS；`PlanClaim.startedAt()`、每轮 `currentLeaseStartedAt` 与 `refreshExecutingPlanLease(...)` 的 `refreshedAt` 均使用 `truncatedTo(ChronoUnit.SECONDS)`，与 MySQL/H2 `datetime` 秒级精度一致。未改 SQL/schema/controller/frontend，未提交。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 17, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 27, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 50, Failures: 0, Errors: 0, Skipped: 0；`DccControlledFileNasTransferServiceTest.processWaitingTasks_truncatesLongTaskFailureMessage` 仍会打印预期 ERROR 日志，但 Surefire 结果为 PASS。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示部分已修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 T10c review-fix-loop round 1 prerequisite count RED

BDD: BDD-NAS-ACL-19 可定位 prerequisite item 失败时 plan 审计摘要必须同步计数 -> Given restore plan 中存在可定位的异常 item，例如 item status 不受支持 / When 后台将该 item 标记为 FAILED 并将 plan 置为 FAILED / Then plan `validationSummaryJson` 必须同步包含 `completedDirectoryCount=0` 与 `failedDirectoryCount=1`，不得按旧内存状态把 failed count 记为 0。

REVIEW-FIX: T10c prerequisite failure count RED -> 根据主 reviewer 打回意见补强 `DccNasPermissionRestoreExecutionServiceTest.processWaitingRestorePlans_failsIncompletePlanWithoutEligibleItems`，要求 unsupported item status 被写为 FAILED 后，plan failure summary 同步记录 `"failedDirectoryCount":1` 与 `"completedDirectoryCount":0`。仍只修改测试与任务日志/状态，未写生产代码、未改 SQL/schema、未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 通过并进入测试阶段；Tests run: 16, Failures: 1, Errors: 0, Skipped: 0。失败用例为 `processWaitingRestorePlans_failsIncompletePlanWithoutEligibleItems`，断言 plan `validationSummaryJson` 包含 `"failedDirectoryCount":1` 失败，说明当前 `failPlanPrerequisite(...)` 已更新 item FAILED 但 plan summary 仍按旧内存 item status 统计为 failed=0。

## 2026-05-27 T10c review-fix-loop round 1 prerequisite count GREEN

IMPLEMENTATION: T10c prerequisite failure count GREEN -> `DccNasPermissionRestoreExecutionServiceImpl.failPlanPrerequisite(...)` 在可定位 `failedItem` 时，更新 item FAILED 后将该 item id 纳入 `processedStatuses` 并按 `ITEM_STATUS_FAILED` 参与 `countItems(...)`，确保 plan failure `validationSummaryJson` 的 `failedDirectoryCount` 与已写 item FAILED 状态一致。未改 SQL/schema/controller/frontend，未提交。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 16, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 26, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示部分已修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

## 2026-05-27 T10c review-fix-loop round 1 worker RED

BDD: BDD-NAS-ACL-14 已 claim 的 EXECUTING restore plan 必须可安全续跑 -> Given 调度实例已将 READY plan claim 为 EXECUTING 后中断，或已有 VERIFIED item 且仍有 WAITING/APPLIED item 未完成 / When 后台再次执行 `processWaitingRestorePlans()` / Then 服务必须通过原子 stale reclaim 防止并发重复执行，并继续处理可恢复 item，不得让 EXECUTING plan 永久跳过。

BDD: BDD-NAS-ACL-15 restore plan item 前置条件不完整不得默认完成 -> Given 已 claim 的 restore plan 没有 item、`validationSummaryJson.directoryCount` 与 item 数不一致，或 item 状态不能证明所有目录已 VERIFIED / When 后台执行恢复 / Then plan 必须 fail fast 到 FAILED 并记录明确 failureCode/failureMessage；可定位到异常 item 时同步写 item FAILED/blockReason，不得标记 COMPLETED。

BDD: BDD-NAS-ACL-16 item 处理异常必须转为可审计失败 -> Given WAITING item 的 `plannedOperationsJson` 缺失、restoreMode 不支持、replaceDirectoryRules 非法，或 transaction/mapper 在 item 边界抛错 / When 后台执行恢复 / Then 服务必须转换为 FAILED/BLOCKED 审计结果，能定位 item 时更新 item blockReason/plan failure，能写 action 边界时写 append-only restore log；validation 失败后不得写 `dcc_directory_access_rule`。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 2 个错误：`DccNasAclRestorePlanMapper.reclaimExecutingPlan(java.lang.Long, java.time.LocalDateTime, java.time.LocalDateTime)` 找不到符号。该失败聚焦于 review-fix-loop round 1 blocker：已 claim 的 EXECUTING plan 缺少安全 stale reclaim/续跑 mapper 合同；新增 RED 同时覆盖空 item、directoryCount 不一致、未知 item 状态、plannedOperationsJson 缺失、unsupported restoreMode、invalid replaceDirectoryRules 和 transaction failure 的审计失败契约，尚未写生产代码、未改 SQL/schema/controller/frontend、未提交。

## 2026-05-27 T10c review-fix-loop round 1 worker GREEN

IMPLEMENTATION: T10c review-fix-loop round 1 worker GREEN -> `DccNasPermissionRestoreExecutionServiceImpl` 扩展恢复执行状态机：`processWaitingRestorePlans()` 选择 READY plan 与超过 30 分钟 lease 的 stale EXECUTING plan；READY 仍通过 `claimReadyPlan(...)` 原子 claim，EXECUTING 通过新增 `reclaimExecutingPlan(planId, currentStartedAt, reclaimedAt)` 使用旧 `startedAt` CAS 式续租，避免并发重复处理。处理前校验 plan item 必须存在、`validationSummaryJson.directoryCount` 必须等于 item 数、item 状态必须为 WAITING/APPLIED/VERIFIED/FAILED/BLOCKED 且不能已有失败状态；不能证明全部 VERIFIED 时不得 COMPLETED，改为 FAILED 并写明确 `DCC_NAS_ACL_RESTORE_PLAN_ITEM_PREREQUISITE_INVALID`。WAITING item 的 plannedOperations/restoreMode/replaceDirectoryRules/transaction 异常统一转 `DCC_NAS_ACL_RESTORE_ITEM_PROCESSING_FAILED`，更新 item blockReason、plan failure，并在 VALIDATE 边界写 FAILED restore log；APPLIED item 只做 VERIFY 续跑，不 delete/insert 运行时目录规则。validation 失败路径继续不写 `dcc_directory_access_rule`；未改 SQL/schema/controller/frontend，未提交。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 14, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 24, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 47, Failures: 0, Errors: 0, Skipped: 0；`DccControlledFileNasTransferServiceTest.processWaitingTasks_truncatesLongTaskFailureMessage` 仍会打印预期 ERROR 日志，但 Surefire 结果为 PASS。

## 2026-05-27 T10c restore execution concurrent claim contract RED

BDD: BDD-NAS-ACL-13 并发恢复执行必须原子 claim READY plan -> Given 两个调度实例可能同时扫描到同一个 READY restore plan / When 后台执行 `processWaitingRestorePlans()` / Then 每个 READY plan 必须先通过 `DccNasAclRestorePlanMapper.claimReadyPlan(planId, startedAt)` 原子 claim 到 EXECUTING；claim 成功才查询/处理 plan items，claim 返回 0 时必须跳过，不查询 plan items、不进入 `TransactionTemplate`、不写 `dcc_directory_access_rule`、不写 `dcc_nas_acl_restore_log`、不更新 plan final 状态，防止两个调度实例并发恢复同一 plan。

REVIEW-FIX: T10c concurrent restore plan claim RED -> 根据第四轮复核意见补强 `DccNasPermissionRestoreExecutionServiceTest`：成功、hash mismatch、短事务、断点续跑等现有用例均 mock `restorePlanMapper.claimReadyPlan(RESTORE_ID, any(LocalDateTime.class))` 返回 1 并 verify 至少调用一次；新增 claim 失败用例，Given `selectList` 返回 READY plan 但 `claimReadyPlan` 返回 0，When `processWaitingRestorePlans()`，Then 不查询 plan items、不调用 `transactionTemplate.execute(...)`、不写目录规则、不写 restore log、不更新 plan final 状态。测试明确要求 mapper 提供 `claimReadyPlan(Long planId, LocalDateTime startedAt)` 原子 claim 合同。仍只修改测试与任务日志/状态，未写生产代码、未改 SQL/schema、未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` testCompile 失败且仅 3 个错误，均为 `DccNasPermissionRestoreExecutionServiceTest` 引用的 `DccNasAclRestorePlanMapper.claimReadyPlan(java.lang.Long, java.time.LocalDateTime)` 找不到符号。该失败聚焦于并发恢复 plan 缺少 READY->EXECUTING 原子 claim mapper 合同；未出现测试语法、import、schema 或既有用例破坏问题。

SUBAGENT: T10c RED/test contract agent -> 已完成第四轮并发 claim RED 契约补强；本轮仅触碰测试与任务文档，生产代码、SQL/schema、提交均未执行。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档、`DccNasPermissionRestoreServiceImpl.java` 与 `DccNasPermissionRestoreServiceTest.java` 下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected；preview 保留 `task.md` 与 `execution-log.md`，但因当前 worktree 仍有 production/test/doc 待集成改动且用户要求不提交、不清理，未执行 apply、未删除任何文件。

## 2026-05-27 T10c restore execution concurrent claim GREEN

IMPLEMENTATION: T10c concurrent claim GREEN -> `DccNasAclRestorePlanMapper` 新增 `claimReadyPlan(Long planId, LocalDateTime startedAt)` default 方法，使用 MyBatis-Plus `LambdaUpdateWrapper` 以 `id=planId and status='READY'` 原子更新 `status='EXECUTING'` 与 `startedAt=startedAt` 并返回 update 行数；`DccNasPermissionRestoreExecutionServiceImpl.processPlan(...)` 在查询 item 前先 claim，返回 0 时直接 skip，不查询 plan item、不进入 `TransactionTemplate`、不写 restore log、目录规则或 plan final 状态。claim 成功后继续按现有 WAITING item 短事务执行和 COMPLETED/FAILED 逻辑处理；未改 SQL/schema/controller/frontend，未提交。

GREEN: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 15, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 38, Failures: 0, Errors: 0, Skipped: 0；`DccControlledFileNasTransferServiceTest.processWaitingTasks_truncatesLongTaskFailureMessage` 仍会打印预期 ERROR 日志，但 Surefire 结果为 PASS。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档、`DccNasAclRestorePlanMapper.java`、`DccNasPermissionRestoreServiceImpl.java` 与 `DccNasPermissionRestoreServiceTest.java` 下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected before reviewer/commit；preview 保留 `task.md` 与 `execution-log.md`，但因当前 worktree 仍有本轮 production/test 改动且用户要求不提交，未执行 apply、未删除任何文件。

## 2026-05-27 T10c final main verification

REGRESSION: mvn -pl yudao-module-dcc -am test -> PASS, reactor 上游模块与 `yudao-module-dcc` 均 SUCCESS；`yudao-module-dcc` Tests run: 274, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示部分已修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected before commit/merge；preview 保留 `task.md` 与 `execution-log.md`，列出可清理的任务附属文档，并因当前 linked worktree 尚未提交本轮 production/test 改动且无法 fast-forward merge 到 `int_main` 而阻塞；未执行 apply、未删除任何文件。

COMMIT: git commit -m "任务: 完成NAS权限恢复执行服务" -> PASS, commit `76050bd2a8`；TDD compliance hook passed。

## 2026-05-27 T10d backend API surface RED setup

BDD: BDD-NAS-ACL-22 权限快照摘要和明细必须有真实后端接口 -> Given NAS 转移任务已有权限快照批次、目录快照和 ACE / When 前端或 reviewer 查询 `permission-snapshot` 摘要与 `permission-snapshot/items` 明细 / Then 后端必须返回真实 taskId、snapshotStatus、目录快照数、ACE 数、blocker 明细和 restoreSupported，不得返回 mock、空成功或让前端自行拼接数据。

BDD: BDD-NAS-ACL-23 NAS 主体映射必须通过后端接口显式查询和保存 -> Given 已保存 raw ACL 中存在未映射 SID / When 前端查询未映射主体或保存 `sourceSid -> DCC USER/DEPT/ROLE/POSITION` 映射 / Then controller 必须委托 `DccNasPrincipalMappingService`，保留真实 SID、影响 ACE 数和首个 NAS 路径，保存时携带当前登录用户，不得按名称猜测或默认映射。

BDD: BDD-NAS-ACL-24 恢复任务必须可查询执行状态 -> Given 恢复 POST 已创建 READY/EXECUTING/COMPLETED/FAILED restore plan / When 前端查询 `permission-restore/{restoreId}` / Then 后端必须返回 restoreId、taskId、status、directoryCount、ruleCount、completed/failedDirectoryCount、failureMessage、startedAt、completedAt，供 UI 轮询展示，不得只返回创建响应后失联。

SUBAGENT: T10d backend API RED/test agent -> 准备启动；写入 controller/service 契约 RED 测试和本日志，不写生产代码、不改 SQL/schema、不提交。

## 2026-05-27 T10d backend API surface RED

BDD: BDD-NAS-ACL-22 权限快照摘要和明细必须有真实后端接口 -> Given NAS 转移任务已有权限快照批次、目录快照和 ACE / When 前端或 reviewer 查询 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot` 与 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot/items?pageNo&pageSize&status` / Then controller 必须有权限注解并委托 `DccNasPermissionSnapshotQueryService`，响应 VO 必须包含摘要计数字段、restoreSupported、分页 item 和 blocker 明细。

BDD: BDD-NAS-ACL-23 NAS 主体映射必须通过后端接口显式查询和保存 -> Given 已保存 raw ACL 中存在未映射主体 / When 前端查询 `GET /dcc/nas-permission/principals/unmapped?taskId=...` 或保存 `PUT /dcc/nas-permission/principal-mappings` / Then controller 必须返回 sourceAuthority/sourceSid/sourceName/aceCount/firstNasPath，保存时必须把当前登录用户、active 与 changeReason 放入 `SaveMappingCommand`，不得默认映射或按名称猜测。

BDD: BDD-NAS-ACL-24 恢复任务必须可查询执行状态 -> Given 恢复 POST 已创建 restore plan / When 前端轮询 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore/{restoreId}` / Then controller 必须返回 restoreId、taskId、status、directoryCount、ruleCount、completedDirectoryCount、failedDirectoryCount、lastFailureMessage、startedAt 和 completedAt。

SUBAGENT: T10d backend API surface RED/test agent -> 新增 `DccNasPermissionSnapshotControllerTest`、`DccNasPrincipalMappingControllerTest`，并扩展 `DccNasPermissionRestoreControllerTest`；仅修改测试与任务文档，未写 `src/main`、SQL/schema、前端或 pom，未提交。

RED: mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, PowerShell 未加引号的逗号参数首先被解析器拦截为 `Missing argument in parameter list`，未进入 Maven；随后使用等价 Maven 参数 `mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 进入 reactor。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, reactor 上游模块均 SUCCESS，`yudao-module-dcc` 在 testCompile 阶段失败且失败聚焦于后端 API surface 缺口：找不到 `DccNasPermissionSnapshotController`、`DccNasPermissionSnapshotSummaryRespVO`、`DccNasPermissionSnapshotItemRespVO`、`DccNasPermissionSnapshotQueryService`、`DccNasPrincipalMappingController`、`DccNasUnmappedPrincipalRespVO`、`DccNasPrincipalMappingSaveReqVO`、`DccNasPrincipalMappingRespVO`、`DccNasPermissionRestoreStatusRespVO`。该 RED 明确要求实现 snapshot summary/items、unmapped principal 查询、principal mapping 保存，以及 restore status 查询接口。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示任务文档和 `DccNasPermissionRestoreControllerTest.java` 下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected；preview 保留 `task.md` 与 `execution-log.md`，但当前仅完成 T10d RED 且用户要求不提交、不清理，未执行 apply、未删除任何文件。

## 2026-05-27 T10d backend API surface GREEN

IMPLEMENTATION: T10d backend API surface GREEN -> 新增 `DccNasPermissionSnapshotController`、`DccNasPrincipalMappingController`、snapshot summary/item VO、principal mapping req/resp VO、unmapped principal VO 和 restore status VO；`DccNasPermissionRestoreController` 增加 `GET /{restoreId}` 并委托 `DccNasPermissionRestoreService.getStatus(taskId, restoreId)`。新增 `DccNasPermissionSnapshotQueryService`/Impl 只读查询现有 snapshot、directory snapshot、ACE、identity mapping 表，按真实 collect failure、DENY ACE、unsupported accessMask 和 unmapped SID 构造 blocker；未返回 mock/固定成功，未改 SQL/schema、前端或 pom，未提交。

IMPLEMENTATION: T10d principal mapping command GREEN -> `DccNasPrincipalMappingService.SaveMappingCommand` 扩展 `sourceAuthority/sourceSid/sourceName/active/changeReason/operatorUserId`，保留旧构造器以兼容既有 service 契约；controller 保存映射时传入当前登录用户。`UnmappedPrincipal` 扩展 `sourceAuthority/sourceSid/sourceName`，服务仍从真实 ACE trustee SID 与 SID hash 聚合影响 ACE 数和首个 NAS 路径，不按名称猜测或默认映射。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 9, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 26, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示本轮修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected；preview 保留 `task.md` 与 `execution-log.md`，列出历史任务附属文档为可清理项，但因当前 worktree 仍有本轮 production/test/doc 待 reviewer 集成改动且用户要求禁止提交/清理，未执行 apply、未删除任何文件。

## 2026-05-27 T10d main reviewer no-fallback repair RED

BDD: BDD-NAS-ACL-25 principal mapping command 不得保留兼容构造器 -> Given 前端保存 NAS principal mapping 只使用当前设计的 sourceAuthority/sourceSid/sourceName/active/changeReason/operatorUserId 契约 / When reviewer 检查 service record command / Then `SaveMappingCommand` 与 `UnmappedPrincipal` 只能暴露 canonical constructor，不得保留旧参数顺序的兼容 shim，避免后续调用方误用旧语义。

BDD: BDD-NAS-ACL-26 restore status 必须读取审计摘要计数 -> Given restore plan 的 `validationSummaryJson` 缺少 `completedDirectoryCount` 或 `failedDirectoryCount` / When 查询 restore status / Then service 必须 fail fast 报告缺失审计字段，不得用 plan item 当前状态临时补数或静默返回成功。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, Tests run: 15, Failures: 3, Errors: 0, Skipped: 0。失败集中在三点：restore apply 新建 plan 的 `validationSummaryJson` 未写入 `"completedDirectoryCount":0`；`getStatus` 缺少 completed/failed 审计计数时未抛异常；`SaveMappingCommand` 暴露 3 个 constructor 而不是 1 个 canonical constructor。

## 2026-05-27 T10d main reviewer no-fallback repair GREEN

IMPLEMENTATION: T10d no-fallback repair GREEN -> 删除 `DccNasPrincipalMappingService.SaveMappingCommand` 与 `UnmappedPrincipal` 的旧兼容构造器和旧别名 accessor；controller 改为显式构造当前 canonical command，service impl 直接读取 `sourceAuthority/sourceSid/sourceName/accountName/accountType/operatorUserId`。restore apply 新建 `validationSummaryJson` 时写入 `completedDirectoryCount=0` 与 `failedDirectoryCount=0`；restore status 查询改为只读取审计摘要中的 directory/rule/completed/failed count，缺字段立即抛 `IllegalStateException`，不再从 plan item 状态补默认值。未改 SQL/schema、前端或 pom，未提交。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 22, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest,DccNasPrincipalMappingServiceTest,DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 58, Failures: 0, Errors: 0, Skipped: 0；`DccControlledFileNasTransferServiceTest.processWaitingTasks_truncatesLongTaskFailureMessage` 仍会打印预期 ERROR 日志，但 Surefire 结果为 PASS。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示部分已修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

REGRESSION: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

SUBAGENT: review-fix-loop round 1 independent reviewer -> 已启动，run id `20260527T002924Z-704d69`，reviewer agent `019e66d6-ea21-71c1-ba74-6e2a679e43b4`，等待放行单。

## 2026-05-27 T10d review-fix-loop round 1 worker repair

REVIEW: review-fix-loop round 1 independent reviewer -> FAIL。阻塞项：`PUT /dcc/nas-permission/principal-mappings` 会把 `accountType=null` 写入 NOT NULL schema；restore status 对执行中 plan 只读 summary，无法返回真实 item 进度；snapshot query service 缺少 mapper-derived 服务级 TDD 证据。

BDD: BDD-NAS-ACL-27 principal mapping save 必须携带 schema-required accountType -> Given 管理员显式保存 NAS SID 到 DCC subject 的映射 / When controller 构造 `SaveMappingCommand` 且 service 即将 insert `dcc_nas_acl_identity_mapping` / Then command 必须包含非空 `accountType`，service 在持久化前 fail fast，不能把 null 写入 `account_type`。

BDD: BDD-NAS-ACL-28 restore status 对非最终 plan 必须返回当前 item 进度 -> Given restore plan 处于 `EXECUTING` 且 plan items 已有 `VERIFIED/FAILED/WAITING` / When 前端轮询 `GET /permission-restore/{restoreId}` / Then 返回的 completed/failed count 必须来自当前 item 状态，不能只返回创建时 summary 的 `0/0`。

BDD: BDD-NAS-ACL-29 snapshot query service 必须由真实 mapper 数据推导摘要和明细 -> Given snapshot、directory snapshot、ACE、identity mapping mapper 返回 CAPTURED、collect failure、DENY、unsupported mask 和 unmapped principal 数据 / When 查询 summary/items / Then 结果必须包含真实计数、blocker、分页和 fail-fast 行为，不依赖 controller mock。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, testCompile 缺少 `DccNasPrincipalMappingSaveReqVO#setAccountName(String)` 与 `setAccountType(String)`，证明当前 API request/command 尚未携带 schema-required account type。

IMPLEMENTATION: T10d review-fix-loop round 1 worker GREEN -> `DccNasPrincipalMappingSaveReqVO` 新增 `accountName` 与 `@NotBlank accountType`，controller 显式传入 canonical `SaveMappingCommand`，`DccNasPrincipalMappingServiceImpl` 在查询目标主体和 insert 前用 `requireAccountType(...)` fail fast；restore status 对 `COMPLETED/FAILED` final plan 继续读取 summary 审计计数，对非最终 plan 查询当前 restore plan item 并统计 `VERIFIED` 为 completed、`FAILED/BLOCKED` 为 failed；新增 `DccNasPermissionSnapshotQueryServiceImplTest` 覆盖 CAPTURED happy path、collect failure blocker、DENY/unsupported mask blocker、unmapped principal blocker、pagination/status filter、snapshot-not-ready fail-fast，并修复 descriptorId 为空目录 item 查询的 NPE 风险。未改 SQL/schema、前端或 pom，未提交。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 36, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 66, Failures: 0, Errors: 0, Skipped: 0；`DccControlledFileNasTransferServiceTest.processWaitingTasks_truncatesLongTaskFailureMessage` 仍会打印预期 ERROR 日志，但 Surefire 结果为 PASS。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示本轮修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

REGRESSION: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

SUBAGENT: review-fix-loop round 1 worker -> 已完成并写入 `.review-fix-loop/runs/20260527T002924Z-704d69/worker/result-round-1.md`；主 reviewer 已复跑 66-test 宽回归和 backend API evidence 校验，准备进入 round 2 independent review。

## 2026-05-27 T10d review-fix-loop round 2 PASS

REVIEW: review-fix-loop round 2 independent reviewer -> PASS。`logic_status=pass`、`usability_status=pass`、`ui_status=pass`、`blocking_issues=None`、`required_changes=None`、`final_decision=pass`。reviewer 确认 round 1 三个 blocker 均关闭：principal mapping save 已通过 `@NotBlank accountType` 与 service `requireAccountType(...)` 避免写入 NOT NULL null；restore status 对非最终 plan 从当前 item 状态统计 completed/failed，对 final plan 继续保持 summary fail-fast；snapshot query service 已有 mapper-derived 服务级测试覆盖 summary/items/blockers/pagination/fail-fast。

GREEN: review-fix-loop round 2 reviewer command `mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 36, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: review-fix-loop round 2 reviewer `git diff --check` -> PASS, 无 whitespace error；仅提示 LF/CRLF working-copy warnings。

SUBAGENT: review-fix-loop round 2 independent reviewer -> 已完成并写入 `.review-fix-loop/runs/20260527T002924Z-704d69/review/report-round-2.md`；run 状态已更新为 `passed`。

## 2026-05-27 T10d final main verification

REGRESSION: mvn -pl yudao-module-dcc -am test -> PASS, reactor 上游模块与 `yudao-module-dcc` 均 SUCCESS；整体 Tests run: 290, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260526-nas-permission-snapshot-restore-implementation\backend-api-evidence.md -> PASS, Backend API evidence is valid。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示本轮修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。

CLOSEOUT-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-nas-permission-snapshot-restore-implementation --mode preview -> BLOCKED as expected；preview 保留 `task.md` 与 `execution-log.md`，列出历史任务附属文档为可清理项，但因当前 linked worktree 不能 fast-forward merge 到 `int_main` 且当前 T10d 改动尚未提交而阻塞；未执行 apply、未删除任何文件。

## 2026-05-27 T10d review-fix-loop round 1 worker RED/GREEN

BDD: BDD-NAS-ACL-27 principal mapping save 不得写入缺失 accountType -> Given `dcc_nas_acl_identity_mapping.account_type` 为 NOT NULL / When 管理端调用 `PUT /dcc/nas-permission/principal-mappings` 保存显式 SID 映射 / Then 请求与 command 必须携带 `accountType`，service 在持久化前 fail fast 校验，不得把 null 写入 schema。

BDD: BDD-NAS-ACL-28 restore status 对非最终计划必须暴露真实轮询进度 -> Given restore plan 仍为 EXECUTING 且当前 plan item 已有 VERIFIED/FAILED/WAITING 状态 / When 前端轮询 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore/{restoreId}` / Then status API 必须从当前 item 状态返回 completed/failed directory counts，不得沿用 stale summary 造成 0/0 假进度。

BDD: BDD-NAS-ACL-29 snapshot query service 必须从 mapper 数据推导 summary/items -> Given mapper 返回 snapshot、directory snapshot、ACE 与 identity mapping 数据 / When 查询 summary 或 items / Then service 必须基于真实 mapper 数据计算 CAPTURED、collect failure、DENY/unsupported mask、unmapped principal、分页/status filter 和 snapshot-not-ready fail fast。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, `yudao-module-dcc` testCompile 失败且聚焦于 `DccNasPrincipalMappingSaveReqVO` 缺少 `setAccountName(String)` 与 `setAccountType(String)`；该 RED 证明 principal mapping save API 尚不能携带 schema-required `accountType`，未进入生产实现。

IMPLEMENTATION: T10d review-fix-loop round 1 worker GREEN -> `DccNasPrincipalMappingSaveReqVO` 新增 `accountName` 与 `@NotBlank accountType`，controller 将二者放入 canonical `SaveMappingCommand`，`DccNasPrincipalMappingServiceImpl` 在持久化前显式校验 `accountType`，缺失时抛 `IllegalArgumentException("accountType required")` 且不调用目标主体校验或 insert。`DccNasPermissionRestoreServiceImpl#getStatus(...)` 先 fail fast 校验 `directoryCount/ruleCount`，最终 plan 继续读取 summary 中的 completed/failed counts，非最终 plan 从当前 plan item 状态计算 VERIFIED 为 completed、FAILED/BLOCKED 为 failed。`DccNasPermissionSnapshotQueryServiceImpl` 补齐 descriptorId 缺失目录的 ACE 空列表处理，并新增 service-level 测试覆盖 CAPTURED happy path、collect failure、DENY/unsupported mask、unmapped principal、pagination/status filter 和 snapshot-not-ready。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionSnapshotControllerTest,DccNasPrincipalMappingControllerTest,DccNasPrincipalMappingServiceTest,DccNasPermissionRestoreControllerTest,DccNasPermissionRestoreServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 36, Failures: 0, Errors: 0, Skipped: 0。

REGRESSION: git diff --check -> PASS, 无 whitespace error；仅提示部分已修改文本文件下次 Git 触碰时 LF 会替换为 CRLF。
