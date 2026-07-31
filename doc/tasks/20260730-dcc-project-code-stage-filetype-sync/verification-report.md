# Verification Report

## Result

BLOCKED。官方批量分类任务已执行并完成，但未满足 GREEN 条件；仍存在歧义与未识别文件，DCC 项目代码详情仍有可识别候选未聚合到目标阶段/文件类型。

## Evidence

- 测试服健康：`172.30.30.58` runtime、data、backend、frontend、OnlyOffice 均通过状态脚本只读检查；backend/frontend HTTP 200。
- `测试租户/aoteman`：登录成功，`doc_control=true`，DCC 查询/更新权限通过；启用类别 64 条，但绑定 `fileTypeTaxonomyId` 的类别为 0 条；项目代码 124 个，候选文件 1 个，无法得出目标阶段/文件类型。
- `芋道源码/admin`：登录成功，类别规则完整，启用且绑定 `fileTypeTaxonomyId` 的类别 60 条，阶段分布为 `INPUT=7, PLAN=3, OUTPUT=25, VERIFICATION=5, VALIDATION=12, TRANSFER=8`；候选文件 15028 个；但 `doc_control=false`，批量任务权限接口返回 403。
- `芋道源码` 租户只读角色核对：存在 `doc_control` 角色，已分配用户为 `wangsiyu`、`zhaohaichen`；当前任务没有这些账号凭据。
- 用户补充 `admin` 凭据后复核：`芋道源码/admin` 仍为 `doc_control=false` 且批量任务权限接口返回 403；`测试租户/admin` 登录失败。密码未记录。
- 用户补充 `芋道源码/zhaohaichen` 凭据后复核：`doc_control=true`，DCC 查询/更新权限通过，启用且绑定 `fileTypeTaxonomyId` 类别 60 条。
- 官方任务 `35`：`COMPLETED`，`totalCount=14990`，`successCount=6292`，`failedCount=0`，`conflictCount=0`，`ambiguousCount=1207`，`unclassifiedCount=7491`。
- 任务后复扫：项目代码 117 个，仍有候选项目 93 个、候选文件 8736 个，样本仍出现 `未分类 / 未分类文件类型`。
- 已导出阻塞明细：`task-35-ambiguous-recognition-records.xlsx`、`task-35-unclassified-recognition-records.xlsx`；终态核验 JSON 为 `task-35-final-verification.json`。

## Executed

- 已调用 `POST /admin-api/dcc/controlled-files/batch-recognition/tasks` 创建任务 `35`。
- 已轮询至终态并复扫候选影响面。
- 已导出 `AMBIGUOUS` 与 `UNCLASSIFIED` 识别记录。
- 未直接写 SQL、未修改角色、未跨租户搬运类别规则、未改代码、未使用 per-file API 批量绕过。

## Required Next Step

若继续执行，需要基于导出的歧义/未识别明细补充正式类别匹配规则、拆解同分歧义类别或人工处理未识别文件；处理后再使用同一官方批量分类链路重跑并复查候选数为 0。
