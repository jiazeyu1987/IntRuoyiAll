# 任务：测试服 DMR 文件夹完整转移到 DCC

## 任务目标

在测试服务器 `172.30.30.58` 的真实 NAS 配置下，将 DMR 文件夹转移到 DCC 目录体系，并确认 DMR 下所有可读取子文件夹和子文件均已成功迁移。用户已在 2026-05-30 明确授权：没有权限读取的 DMR 目录可以跳过，但必须记录跳过路径；可读取范围内若真实迁移或完整性校验失败，必须在本地按 BDD + 严格 TDD 修复代码，使用运行控制台构建发布包，部署到测试服务器后复测，直到确认可读取范围迁移成功。

## 前置任务检查

- 后端最近任务 `20260530-runtime-control-candidate-directory-filter` 状态为 `Completed`。
- 历史相关任务 `20260525-dcc-nas-active-task-stuck` 状态为 `completed`，但当时任务 `1` 最终存在 `FAILED/FILE/submit=5`，本次不能只以任务终态为准，必须按 DMR 源树逐项核对。
- 当前后端仓库存在与本任务无关的未提交改动：`script/deploy/publish-int-ruoyi.ps1`、`script/tests/test_publish_int_ruoyi_to_test_tooling.py`、`doc/tasks/20260529-showroom-release-truth-refactor/`、`yudao-module-showroom/output/imagegen/three-way-stopcock-1-list-card-native.png`。本任务不回退这些改动，若需要提交只提交本任务直接产生的文件。

## BDD 场景

- BDD: DMR 可读源树必须进入 DCC -> Given 测试服 NAS 中存在 DMR 文件夹及全部当前账号可读取的子目录/子文件 / When 发起或恢复 DMR 到 DCC 的真实转移任务 / Then 每个可读取 DMR 子目录和子文件都必须在 DCC 目标记录或受控文件中可核对，失败数必须为 0。
- BDD: 无权限目录按用户授权跳过并记录 -> Given 当前 NAS 配置账号对部分 DMR 子目录返回权限不足 / When 执行 DMR 迁移和完整性校验 / Then 这些目录可以跳过，但必须在任务证据中列出具体路径，不得把跳过项计入成功迁移。
- BDD: 失败文件必须暴露并驱动修复 -> Given 任一 DMR 文件或目录转移失败 / When 读取转移任务、失败报告、后端日志和 DCC 目标数据 / Then 必须记录具体失败项和原因，并先补失败回归测试再修复，不得跳过失败项或声明成功。
- BDD: 修复后必须通过测试服真实复测 -> Given 本地修复已通过目标回归 / When 使用运行控制台构建发布包并部署到测试服 / Then 再次执行真实 DMR 转移和 NAS/DCC 完整性校验，直到失败项为 0。

## 里程碑

- [x] M1：建立任务文档并确认前置任务状态。
- [x] M2：读取测试服 NAS/DCC/转移任务当前状态，建立 DMR 源树基线。
- [x] M3：按用户授权跳过无权限目录，发起或恢复 DMR 转移任务并持续跟踪到终态。
- [x] M4：逐项核对 DMR 源树与 DCC 目标结果，确认是否存在失败或缺失。
- [x] M5：若失败，执行本地 RED/GREEN 修复、运行控制台构建发布包、部署测试服并复测。
- [x] M6：完成最终验证、cleanup 预览、任务文档收尾和必要提交。

## 预期验证

- 测试服 `/opt/intruoyi/runtime` 服务健康检查通过。
- 测试服数据库中 DMR NAS 转移任务无 `WAITING`、`RUNNING` 项；除用户授权跳过的 ACL 目录外，不允许存在 `FAILED` 项。
- DMR 可读取源树文件总数、目录总数与 DCC 目标导入结果逐项一致。
- 若产生代码修复，目标 Maven/脚本回归、运行控制台构建发布包、测试服部署和真实复测均记录为 GREEN。
- 本次因 Docker Hub 基础镜像拉取失败，标准发布脚本未完成 Docker 镜像构建；已记录失败，并使用测试服现有 backend 镜像作为基础层替换新 jar 的补丁镜像完成测试服代码部署，未同步数据库或 MinIO。

## 当前状态

已完成。用户已明确授权无权限目录可以跳过；测试租户 `122`、账号 `aoteman`、分类 `906103/技术文件-DMR` 的真实 `3.DMR` 转移任务 `4` 已完成，可读取范围内文件和目录逐项核对一致。

## Current Status

completed

## 当前证据

- 测试服服务健康：`intruoyi-backend`、`intruoyi-frontend`、`intruoyi-mysql` 均运行，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 测试服 NAS 配置：`172.30.30.4` / `质量体系文件` / 用户 `ceshi`。
- DCC 现有 NAS 转移任务：仅有 `1. QMS documents` 的 3 次历史任务；没有 `3.DMR` 专属转移任务。
- DCC 目录现状：已存在顶层 `2.DHF`，未看到顶层 `3.DMR`。
- DMR 源树只读计数：临时只读 CIFS 挂载统计到可访问部分 `directories=2734`、`files=16507`、`file_bytes=21341542591`。
- DMR 源树权限阻塞：
  - `3.DMR/11.作废文件` -> 权限不够。
  - `3.DMR/10.产品技术要求/导管类/注册版（来自注册）——按产品申请只读权限` -> 权限不够。
- 第三轮复查：测试服后端健康，数据库中 `selected_nas_paths_json LIKE '%3.DMR%'` 的任务数为 0，`nas_path LIKE '3.DMR%'` 的任务条目数为 0；上述两个目录仍返回 `权限不够`。
- 真实转移创建：`POST /admin-api/dcc/controlled-files/nas-transfer`，`tenant-id=122`，`selectedNasPaths=["3.DMR"]`，`templateCategoryId=906103`，返回任务 `4`，状态 `WAITING`，`remainingPendingCount=1`。
- 本地修复验证：`DccControlledFileNasTransferServiceTest` 9 个用例通过，覆盖长 `fileNumber` 限长和 NAS child path 本批去重。
- 测试服部署：补丁镜像标签 `20260530_dmr_nas_fix_1052`，后端健康检查 `UP`，任务 `4` 的旧代码 `submit/directory` 失败项已重置为待重试，两个 ACL 无权限项保留为跳过证据。
- 数据库排序规则修复：新增 `20260530_dcc_exact_nas_identifier_collation.sql`，将 `dcc_controlled_file_nas_transfer_task_item.nas_path`、`dcc_controlled_file_master.file_name`、`dcc_controlled_file.file_name` 调整为 `utf8mb4_bin` 精确比较，覆盖 ASCII `I` 与罗马数字 `Ⅰ` 等 NAS 路径/文件名差异。
- 本地 schema 回归：`DccBaseSchemaTest#mysqlSchemaShouldUseBinaryCollationForExactNasIdentifiers` 已 RED/GREEN；`DccBaseSchemaTest,DccControlledFileNasTransferServiceTest` 共 16 个用例通过。
- 测试服数据库迁移：上述三列在测试服均已验证为 `utf8mb4_bin`；任务 `4` 的重复键 `directory` 失败项已重置并继续处理，当前失败列表仅剩两个用户授权跳过的 ACL 目录。
- S3 路径修复：`S3FileClient` 对原始对象 key 不再执行 URL path decode，保留文件名中的 `%` 与 `+` 字面字符；完整 URL 输入仍会去 query 并 decode path。
- 本地 `%` 文件名回归：`S3FileClientPathTest` 已 RED/GREEN；`S3FileClientPathTest,DccBaseSchemaTest,DccControlledFileNasTransferServiceTest` 共 18 个用例通过。
- 测试服二次补丁部署：后端 jar 重新构建成功，补丁镜像标签 `20260530_dmr_s3_percent_fix_1136` 已部署；`failure_stage=submit` 文件项已重置，后端健康 `UP`，任务 `4` 已恢复 `RUNNING`。
- 重复目录名修复：`buildDirectoryPath` 不再用 `LinkedHashSet` 去重目录片段，保留 `3.DMR/FQC-001/FQC-001` 这类真实层级；目标单测与受影响回归均通过。
- 测试服三次补丁部署：后端 jar 重新构建成功，补丁镜像标签 `20260530_dmr_repeated_dir_fix_1200` 已部署；14 个 `submit` 失败项重置并全部完成，后端健康 `UP`。
- 任务终态：任务 `4` 状态 `COMPLETED`，完成目录 `2732`，完成文件 `16507`，剩余待处理 `0`，仅保留两个用户授权跳过的 `acl` 目录失败。
- 逐路径完整性：临时只读 CIFS 挂载现读源树，NAS 可读文件 `16507`、字节 `21341542591`；DCC 文件记录 `16507`、二进制唯一路径 `16507`、字节 `21341542591`；缺失文件、额外文件、大小不一致均为 `0`。
- 目录完整性：NAS 可见目录 `2734`，其中授权跳过 ACL 目录 `2`，可迁移目录 `2732`；DCC 已完成目录 `2732`；缺失目录、额外目录、ACL 跳过不一致均为 `0`。
- 管理 API 最终验证：`GET /admin-api/dcc/controlled-files/nas-transfer/tasks/4` 返回 `status=COMPLETED`、`createdDirectoryCount=2732`、`createdFileCount=16507`、`remainingPendingCount=0`，失败列表仅包含两个授权跳过 ACL 目录。
- 脚本级 SQL 门禁：`python -X utf8 -m pytest script/tests/test_dcc_nas_acl_snapshot_restore_sql.py -q` 通过 3 个用例，覆盖精确 NAS 标识符排序规则 SQL 合同。

## 跳过范围与影响

- 当前账号无法列出上述目录内部内容，因此不能建立完整 DMR 源树基线。
- 用户已明确允许跳过无权限目录，因此本任务后续验收范围为“当前 NAS 配置账号可读取的 DMR 子文件夹和子文件全部迁移成功，并明确列出跳过项”。
- 不能将无权限跳过项声明为已迁移成功。
