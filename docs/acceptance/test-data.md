# DCC 未受控文件本地下载与自动归类测试数据

## Purpose and Scope

本文件定义实现和验收所需的测试数据、样本文件、租户边界、清理方式和阻塞条件。数据必须任务自有、可追踪、可清理。不得使用生产租户数据、admin 基线数据或无关真实业务记录验证写入路径。

## Evidence Reviewed

- `docs/database-rules.md#DCC 文件类别规则种子门禁`：类别规则缺失、歧义或插入不完整必须 fail fast。
- `docs/e2e-rules.md#DCC 文控审批处理入口门禁`：DCC 写入链路必须走真实页面和正式处理入口。
- `docs/e2e-rules.md#Element Plus 表格选择门禁`：写入前必须按可见业务唯一文本核对表格选中集合。
- `doc/tasks/20260728-dcc-nas-product-code-unified/verification-report.md`：DCC/NAS 写入使用 DCC 项目代码，不再使用产品主数据。
- `doc/tasks/20260731-dcc-file-category-rules/verification-report.md`：OQ/PQ 和图纸扩展名分类规则已有可维护规则样例。

## Required Test Data

### 基础租户和账号

- 测试租户：实施时使用已授权测试租户。
- 用户：具备 NAS 查询、DCC 受控文件查询、DCC 创建、DCC 元数据更新和项目代码详情查看权限的文控测试用户。
- 角色缓存：如果直接通过 DB 补角色，必须刷新精确 `user_role_ids:{userId}` 缓存；推荐走正式角色分配。

### DCC 项目代码 item

- `projectName`: `CODEx 未受控导入测试项目`
- `projectCode`: `CODEx-UCF-<YYYYMMDDHHmmss>`
- `status`: `ENABLE`
- item 定义：当前系统中 item 使用 `dcc_project_code.id`。
- 清理：测试完成后删除或停用任务自有项目代码，前提是无非任务文件关联。

### 文件分类树和规则

- 需要至少一个正式 DCC 文件分类树路径，例如 `技术文档 / 设计开发 / OQ`。
- 需要至少一个 `dcc_file_category_match_rule` 唯一命中样本文件名或扩展名。
- 需要一个无匹配样本，用于 `UNCLASSIFIED_PENDING`。
- 需要一个歧义样本，可通过两个同分规则构造，用于 `AMBIGUOUS`。
- 需要记录每个识别样本的预期 `classificationReason`、候选摘要条数和 `expectedLocalRelativePath`；候选摘要只记录候选 id、代码/名称、规则 id 和匹配依据，不记录文件内容或凭据。
- 规则、分类树和项目代码创建完成后必须记录任务自有 run id；若缺少正式分类树，只能验证待处理路径，不得临时硬编码扩展名分类。

### NAS 样本文件

- 成功归档样本：`1. QMS documents/CODEx-UCF-<id>/OQ/CODEx-UCF-<id>-OQ-report.pdf`
- 待处理样本：`1. QMS documents/CODEx-UCF-<id>/unknown/no-project-random-file.pdf`
- 歧义样本：`2.DHF/CODEx-UCF-<id>/ambiguous/CODEx-UCF-<id>-shared-rule.pdf`
- 文件变化样本：`2.DHF/CODEx-UCF-<id>/changed/CODEx-UCF-<id>-changed-after-scan.pdf`，统计后必须修改大小或修改时间再执行处理。
- 签名失效样本：复用文件变化样本或构造旧 `sourceSignature`，用于验证 import-selected/content/local-write-result 拒绝过期快照。
- 跨任务绑定样本：准备两个 task id 下的不同 audit file，用于验证 content 和 local-write-result 不能通过猜测 `auditFileId` 跨任务读取或回写。
- 路径冲突样本：准备两个规范化后会落到同一 `local_relative_path` 的文件，用于验证 `downloadStatus=LOCAL_WRITE_FAILED`、`localWriteErrorCode=LOCAL_PATH_COLLISION` 且不覆盖。
- 本地已有目标样本：在本地测试根目录预置同名目标文件，用于验证系统阻塞而不是覆盖、截断或自动改名。
- 路径过长样本：构造超过允许长度的目标相对路径，用于验证 `localWriteErrorCode=LOCAL_PATH_TOO_LONG`。
- 大文件样本：准备超过 JSON/base64 安全承载阈值但低于产品允许上限的任务自有文件，用于验证 content 接口二进制、流式或分块传输。
- 非法路径样本：构造含 `..`、盘符、Windows 保留名或非法字符的目标相对路径；若真实 NAS 不允许这些字符，可在前端路径生成单元/静态合同中构造，并断言错误码而不是新增下载状态。
- 幂等冲突样本：复用同一 `idempotencyKey`，分别提交原始 `auditFileId/sourceSignature/localRelativePath` 与改动后的任一字段，用于验证 `request_hash` 不一致时冲突。
- 幂等顺序样本：使用同一组合法 `auditFileId/sourceSignature/localRelativePath` 以不同 `selectedFiles` 顺序提交，用于验证规范化 `request_hash` 对顺序不敏感；另准备重复 audit id 请求，用于验证重复 id 在 hash 前失败。
- 本地路径篡改样本：把后端预览的 `expectedLocalRelativePath` 改成其它项目代码、其它分类、错误 `_未分类待处理` 前缀或本地绝对路径，用于验证后端拒绝 `import-selected`。
- 混合无效选择样本：同一请求同时包含一个合法 audit file 和一个跨 task、未识别、签名过期、已绑定或已归档 audit file，用于验证 `import-selected` 整体拒绝且合法文件也不进入 `SELECTED`。
- local-write-result 重放样本：复用已成功本地写入并归档的任务自有 audit file，重复提交相同 `LOCAL_WRITTEN` 回写并提交一次冲突终态，用于验证不重复归档且冲突不覆盖。
- 分页显式选择样本：准备超过一页的未受控明细，记录第一页被勾选的 audit file ids 和未勾选页 ids，用于验证首版只处理显式 ids。
- 取消目录选择样本：使用任一未受控明细即可，验证取消后无 import task、无 content 请求、无 local-write-result 回写，且后端 audit 明细状态保持不变。
- 所有样本路径必须包含 task id 或唯一时间戳。
- 文件内容可为小型 PDF/TXT 测试文件，但必须通过真实 NAS 写入方式准备。

### 本地目录

- 本地测试根目录：`doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/local-download-e2e/<run-id>` 或系统临时目录下任务专用目录。
- 本地写入失败目录句柄：通过 Playwright stub `createWritable/write/close` 抛出受控错误，不使用真实权限破坏或手动锁文件冒充失败。
- 本地路径校验必须记录预期相对路径清单，并在成功路径完成后核对文件实际存在；失败路径不得留下半写入文件。
- 不得把本地绝对路径发送给后端。
- import task 只能在本地根目录授权成功、相对路径校验通过后创建；用户取消目录选择不得产生可清理的后端 import 任务。
- 目标已存在、路径过长、规范化冲突或非法路径在 `import-selected` 前被发现时，不应产生 `importTaskId`；若 import task 创建后才发生写入竞争或最终句柄写入失败，才记录任务 id 和 `LOCAL_WRITE_FAILED` 回写证据。
- 未创建 import task 的取消、浏览器不支持和本地路径预检失败路径只记录前端观察证据，不记录伪造的 `importTaskId`。
- 混合无效选择整体拒绝路径必须记录请求前后的 audit 明细状态、import 任务数量、content 请求数和 DCC 写请求数；合法文件不得因为同批存在无效文件而留下 `SELECTED` 或 import 绑定。
- local-write-result 重放路径必须记录第一次和第二次回写后的受控文件 id、ACTIVE NAS 来源映射数量和归档请求数量；重复回写不得产生新 id。
- E2E 完成后删除本任务本地测试目录。

## Reset Procedure

1. 删除或移动任务自有 NAS 样本文件。
2. 删除任务自有本地下载目录。
3. 撤回、删除或作废任务自有 DCC 受控文件，使用系统正式清理入口。
4. 核验本地写入失败、待处理和仅查看路径没有创建受控文件或 NAS 来源映射。
5. 删除任务自有 `dcc_controlled_file_nas_source` 映射，只有在正式清理入口无法覆盖且用户授权数据修复时才可执行 SQL。
6. 删除任务自有 audit/import 任务明细，或保留为任务证据并在文档中记录。
7. 恢复为测试创建的分类规则和项目代码，避免影响其它任务。
8. 清理报告必须列出 `auditTaskId`、`importTaskId`、项目代码、NAS 样本路径和本地测试目录；取消目录选择、浏览器不支持和预检失败路径应记录“无 importTaskId”。
9. 对跨任务、签名失效、混合无效选择、local-write-result 重放和已归档重复提交样本，只清理本任务自有数据；不得删除其它任务或其它租户的 audit/import 证据。
10. 若重复回写测试已经创建受控文件，只通过正式清理入口清理该一个任务自有受控文件；不得直接删除用于证明幂等的历史 audit/import 证据，除非清理授权明确要求。

## Data Ownership

- 所有写入文件名、项目代码、备注和 changeReason 必须包含 `20260802-dcc-uncontrolled-file-local-import-design` 或唯一 run id。
- 写入型 E2E 不得使用生产租户、真实业务项目代码或已有业务 NAS 文件。
- 失败后必须能通过 task id 精确定位残留数据。
- 真实密码、token、私钥和 NAS 凭据不得写入任务文档或日志。

## Test Blockers

- NAS share 不可写、样本目录无法创建、权限不足或文件写入后扫描不可见时，真实写入路径阻塞。
- DCC 文件分类树缺少正式目标分类时，成功归档路径阻塞；待处理路径仍可验证。
- 项目代码导入或创建权限缺失时，成功归档路径阻塞。
- 无法保证测试数据清理时，不得执行写入型 E2E。
