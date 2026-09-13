# DCC-STATIC-027 清空文件编号身份同步验证报告

## Bug

正式文件基础信息修改允许 `fileNumber` 清空，但 Master 的 `fileNumber` / `normalizedFileNumber` 可能因 MyBatis Plus `updateById` 跳过 `null` 更新而继续保留旧身份。旧编号因此仍可能命中新文件或占用冲突检查。

## Expected

清空编号时，当前文件行和 Master 权威身份投影必须在同一事务内同步清空；旧编号不得再命中当前文件，也不得继续占用冲突检查。普通 `OLD -> NEW` 改编号必须继续同步 Master，NEW 可读，OLD 不可读。

## Reproduction

BDD: DCC-STATIC-027 clear file number identity -> Given 已发布正式文件的 Master 保存旧 `fileNumber` 和 `normalizedFileNumber`，When 文控通过基础信息修改将文件编号清空或改为空白，Then 当前文件行和 Master 权威身份均清空编号投影，旧编号查询不得命中新文件，旧编号不再占用冲突检查。

BDD: DCC-STATIC-027 rename file number identity still works -> Given 已发布正式文件编号为 OLD，When 文控通过基础信息修改将编号改为 NEW，Then Master 权威身份同步为 NEW，NEW 可命中当前版本，OLD 不得命中且不得占用冲突检查。

## Root Cause

`DccControlledFileMetadataUpdateServiceImpl` 原先用 `controlledFileMasterMapper.updateById(...)` 更新 Master。当 `fileNumber` 为空白时，规范化值为 `null`；MyBatis Plus 默认更新策略会跳过该字段，导致数据库旧的 `normalized_file_number` 残留。该问题属于“对象内存值为空，但数据库没有实际清空”的写入语义错误。

## Fix

新增 `DccControlledFileMasterMapper.updateMetadataIdentity(...)` 显式 SQL，明确写入所有身份字段，包括 `normalized_file_number = #{normalizedFileNumber}`；服务层检查更新行数必须为 1，更新失败抛出既有业务异常。正式文件行继续保存空白 `fileNumber`，与既有“编号可选”接口契约一致。

## RED:

`node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-027-clear-file-number-identity-contract.spec.cjs` -> FAIL。

失败原因：旧实现仍调用 `updateById`，静态合同在实现前按预期拒绝该空值更新路径。

## GREEN:

`node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-027-clear-file-number-identity-contract.spec.cjs` -> PASS。

`mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataUpdateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；18 tests run，0 failures，0 errors。

## Verification

- 静态合同 PASS：显式 Mapper 方法、关键 SQL 字段和可清空编号边界均满足。
- 服务单测 PASS：正常改编号、清空编号、分类/目录错误、Master 更新失败等 18 个测试通过。
- 回归测试 PASS：`DccControlledFileMetadataUpdateControllerTest` 2 个测试，以及旧 Master 身份与活跃文件不一致时拒绝读取的 workflow 测试 1 个测试通过。
- `git diff --check` PASS：无 whitespace error。
- 本次未执行 E2E、服务启动/重启、数据库写入或远程操作，符合任务约束。

## Blockers

提交/推送授权 blocker 已解除。实现提交已从 `4cd77e18a` rebase 为 `207600902`，收尾记录提交已从 `d0feb6002` rebase 为 `6d337c286`，最终收尾记录提交为 `73ea976ab`。任务分支已 fast-forward 合并到本地 `int_main`；主工作区原有并行改动已恢复且未混入本任务。任务状态为 `completed`。`int_main` 未推送，因为其原本已包含其他未推送任务提交；本任务分支已推送到 `origin`。
