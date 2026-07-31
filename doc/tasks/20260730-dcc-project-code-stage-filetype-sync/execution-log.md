# Execution Log

## User Intent

用户要求实施“测试服 DCC 项目代码阶段/文件类型映射计划”：在测试服务器 `172.30.30.58` 上，将“文控权限/类别列表”中启用文件类别的阶段-文件类型关系，应用到全部 DCC 项目代码详情的关联文件分组。

## BDD

BDD: 测试服全局文件分类同步 -> Given 测试服类别列表已有启用类别和阶段映射，When 执行全局文件分类，Then DCC 项目代码详情的阶段列表与文件类型列表按类别规则聚合，不再把可识别文件留在“未分类文件类型”。

## TDD / Verification Evidence

- RED: `show-int-ruoyi-test-status.bat` -> PASS，测试服运行目录存在，backend/frontend/OnlyOffice HTTP 200，后端健康 HTTP 200。
- RED: 登录 `测试租户/aoteman` -> PASS，`doc_control=true`，DCC 查询/更新权限均为 true；但启用且绑定 `fileTypeTaxonomyId` 的类别规则数量为 0，项目代码 124 个，候选文件 1 个，目标阶段/文件类型为空。
- RED: 登录 `芋道源码/admin` -> PASS，类别规则完整，启用且绑定 `fileTypeTaxonomyId` 的类别 60 条；项目代码 117 个，候选项目 93 个，候选文件 15028 个；但 `doc_control=false`，查询 FILE_CATEGORY 最新任务返回 403。
- RED: `芋道源码` 租户角色只读核对 -> PASS，`doc_control` 角色存在且已分配给 `wangsiyu`、`zhaohaichen`；当前任务缺少这些账号凭据，不允许修改角色或绕过权限。
- RED: 用户补充 `admin` 登录凭据后复核 -> `芋道源码/admin` 登录 PASS，但 `doc_control=false`，FILE_CATEGORY 最新任务接口仍返回 403；`测试租户/admin` 登录失败，提示账号密码不正确。密码未记录。
- RED: 用户补充 `芋道源码/zhaohaichen` 凭据后复核 -> PASS，`doc_control=true`，DCC 查询/更新权限通过，启用且绑定 `fileTypeTaxonomyId` 类别 60 条，执行前项目代码 117 个、候选项目 93 个、候选文件 15028 个。
- GREEN: `POST /admin-api/dcc/controlled-files/batch-recognition/tasks` taskId=35 -> COMPLETED，`totalCount=14990`、`successCount=6292`、`failedCount=0`、`conflictCount=0`、`ambiguousCount=1207`、`unclassifiedCount=7491`。
- GREEN: blocked -> 批量任务完成但不满足验收，因 `ambiguousCount/unclassifiedCount` 非零且复扫仍有 `candidateTotal=8736`；任务保持 blocked，不宣称完成。

## Milestone Updates

- 2026-07-30: 创建任务目录和基础任务文档，记录测试服授权范围、无 fallback/no SQL 约束、BDD/RED/GREEN 验证路径。
- 2026-07-30: 测试服健康检查通过，容器和 HTTP 健康均正常。
- 2026-07-30: 完成只读权限与候选影响面预检；因同一租户内权限与类别规则前置条件不能同时满足，任务进入 blocked，未调用 `/admin-api/dcc/controlled-files/batch-recognition/tasks`。
- 2026-07-30: 按项目经验沉淀要求补充同租户权限/规则门禁摘要；本轮未新增长期经验文档，原因是当前阻塞仍属于本任务的具体环境前置条件，后续若复发再合并到 `docs/login-access.md` 或 DCC 专项门禁。
- 2026-07-30: 使用用户补充的 admin 凭据只读复核，确认仍未解除 `doc_control` 前置权限阻塞。
- 2026-07-30: 用户补充 `芋道源码/zhaohaichen` 凭据；准备重新执行只读复核、候选扫描、官方批量分类提交与轮询。密码不写入日志。
- 2026-07-31: 使用 `芋道源码/zhaohaichen` 执行官方批量分类任务 `35`；任务完成但出现 `ambiguousCount=1207`、`unclassifiedCount=7491`，未满足 GREEN。
- 2026-07-31: 复扫项目代码候选仍为 8736 个，导出 `task-35-ambiguous-recognition-records.xlsx` 与 `task-35-unclassified-recognition-records.xlsx`，保存 `task-35-final-verification.json`。
- 2026-07-31: 继续执行只读失败明细分析，新增 `failure-analysis.md`；确认歧义集中于 OQ/PQ 与“工序卡/作业指导书”同分，未识别集中于图纸/零配件/记录表/参数组等名称；源码核对显示当前规则只基于文件名、标题、文件编号匹配类别名和内置别名，不使用目录阶段线索。
- 2026-07-31: 新增 `remediation-plan.md`，将下一轮正式修复限定为需额外授权的类别别名/评分规则改造；本任务不执行代码、SQL、类别改名或元数据批量写入。
- 2026-07-31: 按项目经验沉淀规则做只读匹配检查，未找到合适的既有 DCC 批量分类长期经验文档；未获用户明确授权前不新建长期经验文档。

## Command Intent Log

- done: 测试服健康预检，使用官方状态脚本，只读检查容器、backend/frontend/OnlyOffice HTTP 状态。
- done: 登录与权限预检，使用测试服 API 登录，不记录密码、token、Authorization 或 cookie。
- done: 启用类别规则与候选影响面只读导出，分别核对 `测试租户/aoteman` 与 `芋道源码/admin` 口径。
- done: 官方批量分类任务提交与轮询，使用 `recognitionType=FILE_CATEGORY`、`scope=GLOBAL`、`existingRecordPolicy=OVERWRITE_ALL`、`syncFileNameTitle=false`、`workerCount=5` 创建任务 `35`。
- done: 异常明细导出，按任务 `35` 导出 `AMBIGUOUS` 与 `UNCLASSIFIED` 识别记录。
- done: 失败明细只读归因分析，解析导出 Excel 和现有后端识别逻辑，未修改测试服数据、类别规则或代码。
- done: 后续修复计划记录，仅作为任务内建议，不作为本任务授权范围内的实施动作。

## Blockers

- BLOCKED: 官方任务 `35` 已完成但仍有 `ambiguousCount=1207`、`unclassifiedCount=7491`，复扫仍有 8736 个候选；需要基于导出明细补充正式类别匹配规则、拆解歧义类别或人工处理未识别文件后，再按同一官方链路重跑。当前计划不允许直接 SQL、角色修改、per-file API 绕过或把歧义/未识别当作成功。
- BLOCKED: 当前系统无数据级别文件类别别名字段；若不允许代码变更，只能调整正式类别名称或文件元数据后再重跑，不能自动执行。若允许代码变更，需要另开正式改造任务并补充 BDD/TDD。
