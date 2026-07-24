# 任务：测试服 DHF 文件夹完整转移到 DCC

## 任务目标

在测试服务器 `172.30.30.58` 的真实 NAS 配置下，将 DHF 文件夹转移到 DCC 目录体系，并确认 DHF 下所有当前账号可读取的子文件夹和子文件均已成功迁移。用户已明确授权：没有权限读取的 DHF 目录直接跳过，但必须记录跳过路径；可读取范围内若真实迁移或完整性校验失败，必须在本地按 BDD + 严格 TDD 修复代码，构建发布包，部署到测试服务器后复测，直到确认可读取范围迁移成功。

## 前置任务检查

- 上一个同类任务 `20260530-dcc-dmr-nas-transfer-completion` 已标记 `completed`，并已提交 `9439c41765 任务: 完成DMR迁移修复验证`。
- 当前后端仓库仍存在与本任务无关的未提交改动：`script/deploy/publish-int-ruoyi.ps1`、`script/tests/test_publish_int_ruoyi_to_test_tooling.py`、`doc/tasks/20260529-showroom-release-truth-refactor/`、`yudao-module-showroom/output/imagegen/three-way-stopcock-1-list-card-native.png`。本任务不回退这些改动，提交时只包含 DHF 任务直接产生的文件。

## BDD 场景

- BDD: DHF 可读源树必须进入 DCC -> Given 测试服 NAS 中存在 DHF 文件夹及全部当前账号可读取的子目录/子文件 / When 发起或恢复 DHF 到 DCC 的真实转移任务 / Then 每个可读取 DHF 子目录和子文件都必须在 DCC 目标记录或受控文件中可核对，失败数必须为 0。
- BDD: 无权限目录按用户授权跳过并记录 -> Given 当前 NAS 配置账号对部分 DHF 子目录返回权限不足 / When 执行 DHF 迁移和完整性校验 / Then 这些目录可以跳过，但必须在任务证据中列出具体路径，不得把跳过项计入成功迁移。
- BDD: 失败文件必须暴露并驱动修复 -> Given 任一可读取 DHF 文件或目录转移失败 / When 读取转移任务、失败报告、后端日志和 DCC 目标数据 / Then 必须记录具体失败项和原因，并先补失败回归测试再修复，不得跳过失败项或声明成功。
- BDD: 修复后必须通过测试服真实复测 -> Given 本地修复已通过目标回归 / When 构建后端发布包并部署到测试服 / Then 再次执行真实 DHF 转移和 NAS/DCC 完整性校验，直到可读取范围内失败项为 0。

## 里程碑

- [x] M1：建立任务文档并确认前置任务状态。
- [x] M2：读取测试服 NAS/DCC/转移任务当前状态，建立 DHF 可读源树基线。
- [x] M3：按用户授权跳过无权限目录，发起或恢复 DHF 转移任务并持续跟踪到终态。（真实任务 `5` 已完成）
- [x] M4：逐项核对 DHF 源树与 DCC 目标结果，确认是否存在失败或缺失。
- [x] M5：若失败，执行本地 RED/GREEN 修复、构建发布包、部署测试服并复测。（已修复跨租户分类 code 冲突、长文件名 schema 容量不足、长 NAS 源路径 remark 容量不足）
- [x] M6：完成最终验证、cleanup 预览、任务文档收尾和必要提交。

## 预期验证

- 测试服 `/opt/intruoyi/runtime` 服务健康检查通过。
- 测试服数据库中 DHF NAS 转移任务无 `WAITING`、`RUNNING` 项；除用户授权跳过的 ACL 目录外，不允许存在 `FAILED` 项。
- DHF 可读取源树文件总数、字节数、目录总数与 DCC 目标导入结果逐项一致。
- 若产生代码修复，目标 Maven/脚本回归、后端构建发布包、测试服部署和真实复测均记录为 GREEN。

## 当前状态

最终验证已通过。DHF 真实测试租户转移任务 `5` 已完成，三轮非 ACL 失败均已按 RED/GREEN 修复并部署测试服；NAS 源树与 DCC 目标记录逐项对账一致，未发现需要跳过的无权限目录。

## Current Status

completed

## 当前证据

- 上一个 DMR 迁移任务已完成并提交。
- 已确认测试服访问说明位于 `docs/server-access.md`，测试服 IP 为 `172.30.30.58`，运行目录为 `/opt/intruoyi/runtime`。
- 已确认测试环境登录账号为租户 `测试租户`、用户 `aoteman`、密码 `admin123`。
- 测试服后端健康：`http://127.0.0.1:48081/actuator/health` 返回 `UP`，当前镜像标签 `20260530_dmr_repeated_dir_fix_1200`。
- 测试服 NAS 配置：`172.30.30.4` / `质量体系文件` / 用户 `ceshi`，密码未输出。
- DHF NAS 根路径确认：`2.DHF`。
- DCC 模板分类确认：`906102 / 技术文件-DHF`。
- 转移前数据库未查到 `selected_nas_paths_json LIKE '%DHF%'` 的历史任务，未查到已存在的 `DHF` DCC 目录。
- DHF 源树只读计数：`directories=2207`、`files=15064`、`file_bytes=81270765861`、`max_depth=11`。
- DHF 源树权限阻塞：无，当前账号未遇到 ACL 拒绝目录。
- 真实转移创建：`POST /admin-api/dcc/controlled-files/nas-transfer`，`tenant-id=122`，`selectedNasPaths=["2.DHF"]`，`templateCategoryId=906102`，返回任务 `5`，状态 `WAITING`，`remainingPendingCount=1`。
- 真实失败修复：任务 `5` 暴露跨租户 `dcc_file_category.code` 全局唯一键冲突；已新增 RED/GREEN 测试 `categoryCodeOf_separatesSameNasPathAcrossTenants`，将 NAS 分类编码 hash 输入改为包含 `tenantId` 与完整目录路径。
- 真实失败修复：任务 `5` 暴露 DHF 长文件名超过 `dcc_controlled_file_master.file_name` 的 `varchar(128)` 限制；已新增 RED/GREEN schema 合同测试 `mysqlSchemaShouldSupportLongNasFileNames`，并新增非破坏性迁移 `20260530_dcc_long_file_name_length.sql` 将 DCC 文件名/标题扩展到 `varchar(256)`。
- 真实失败修复：任务 `5` 暴露完整 NAS 源路径审计 remark 超过 `dcc_controlled_file.remark varchar(255)` 限制；已新增 RED/GREEN schema 合同测试 `mysqlSchemaShouldSupportLongNasTransferSourceRemarks`，并新增非破坏性迁移 `20260530_dcc_long_nas_source_remark.sql` 将 `dcc_controlled_file.remark` 扩展到 `varchar(1024)`。
- 本地回归：`S3FileClientPathTest,DccBaseSchemaTest,DccControlledFileNasTransferServiceTest` 共 20 个受影响用例通过；`script/tests/test_dcc_nas_acl_snapshot_restore_sql.py` 5 个脚本级 SQL 合同测试通过。
- 测试服部署：后端 jar 重新构建成功，补丁镜像标签 `20260530_dhf_tenant_category_fix_1240` 已部署；`failure_stage=submit` 文件项已重置，后端健康 `UP`，任务 `5` 已恢复运行。
- 测试服部署：长文件名修复补丁镜像标签 `20260530_dhf_long_filename_fix_1358` 已部署；测试服列定义验证为 `dcc_controlled_file_master.file_name varchar(256)`、`dcc_controlled_file.file_name varchar(256)`、`dcc_controlled_file.title varchar(256)`；81 个 submit 失败项已重置，后端健康 `UP`，任务 `5` 已恢复运行。
- 测试服部署：长 NAS 源路径 remark 修复补丁镜像标签 `20260530_dhf_long_remark_fix_1515` 已部署；测试服列定义验证为 `dcc_controlled_file.remark varchar(1024)`；6 个 submit 失败项已重置，后端健康 `UP`，任务 `5` 已恢复运行。
- 任务终态：任务 `5` 状态 `COMPLETED`，任务项计数 `COMPLETED/DIRECTORY=2207`、`COMPLETED/FILE=15064`，`FAILED=0`、`WAITING/RUNNING=0`。
- 最终完整性对账：只读重新扫描 DHF 源树 `directories=2207`、`files=15064`、`file_bytes=81270765861`、`denied_count=0`、`stat_error_count=0`；DCC 文件记录 `15064`、唯一源路径 `15064`、字节 `81270765861`；完成目录 `2207`、唯一目录 `2207`；缺失、额外、重复、大小不一致、失败和等待均为 `0`。
- API 验收：测试租户真实登录后查询 `/admin-api/dcc/controlled-files/nas-transfer/tasks/5` 返回 `code=0`、`status=COMPLETED`、`remainingPendingCount=0`、`lastFailureMessage=null`。
- 收尾清理：`task-closeout-cleanup` preview/apply 已执行，仅保留本任务 `task.md` 与 `execution-log.md`，删除同任务目录下中间产物。

## 跳过范围与影响

- DHF 中任何当前 NAS 配置账号无法读取的目录可以跳过，但必须记录具体路径。
- 本次只读源树扫描未发现无权限目录或文件，因此跳过范围为空。
- 无权限跳过项不能声明为已迁移成功。
