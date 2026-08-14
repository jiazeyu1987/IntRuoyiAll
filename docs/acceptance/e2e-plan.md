# DCC 未受控文件本地下载与自动归类 E2E 计划

## Purpose and Scope

本计划定义真实用户路径验收。E2E 必须通过 Playwright 操作真实前端页面，API 只用于最终状态核验或只读辅助检查。范围覆盖 NAS 管理页面、未受控文件统计、明细选择、目录写入能力检查、归档结果、待处理结果和无写请求边界。

## Evidence Reviewed

- `docs/e2e-rules.md`：E2E 必须使用 Playwright 真实页面路径，写入型数据必须任务自有、可追踪、可清理。
- `docs/login-access.md` 在实施前必须读取，用于确认租户、账号和登录前置。
- `docs/local-runtime.md` 在实施前必须读取，用于确认 `8081/48081` 或合法 worktree slot。
- `tests/e2e/dcc-nas-permission-real-data.e2e.js`：已有 NAS 管理真实路径和 NAS 转移任务核验结构。
- `tests/e2e/dcc-project-code-associated-unclassified-auto-classify-static.spec.js`：已有项目代码详情未分类归类合同。
- `doc/tasks/20260801-dcc-list-auto-classify-local-e2e/verification-report.md`：已有本机 DCC 项目代码只读真实 E2E 的目标链路错误归因样例。

## User Paths

### Path A: 只读统计和明细查看

1. 登录测试租户文控用户。
2. 打开 `系统管理 / NAS 管理`。
3. 验证 NAS 配置已加载且连接测试可用。
4. 点击 `统计未受控文件` 并确认固定目录。
5. 等待统计任务完成。
6. 打开未受控文件明细表。
7. 验证明细中的 NAS 路径、文件名、大小、`PENDING_RECOGNITION` 或识别后状态、待处理原因和报告下载入口。
8. 不点击处理确认，不产生 DCC 写请求。

### Path B: 识别成功文件下载并归档

1. 准备任务自有 NAS 样本文件，文件名和路径唯一命中一个启用 DCC 项目代码 item 和一个正式文件分类规则。
2. 登录具备 DCC 创建/元数据权限的文控用户。
3. 从 NAS 管理页完成统计。
4. 勾选目标样本文件。
5. 点击 `下载并归类`，验证预览中项目代码、item、文件分类和本地相对目录。
6. 选择本地测试根目录，并验证相对路径校验通过后才出现 import task 请求。
7. 等待本地写入回写 `LOCAL_WRITTEN` 后，再等待后端归档任务完成。
8. 打开 DCC 项目代码详情，验证关联文档三栏出现目标文件。
9. 打开受控文件详情，验证 `dccProjectCodeId`、文件分类、NAS source 和原始文件。
10. 清理任务自有受控文件和 NAS 样本。

### Path C: 无法唯一识别文件进入待处理

1. 准备任务自有 NAS 样本文件，文件名和路径不命中任何项目代码或命中多个分类规则。
2. 从 NAS 管理页统计并勾选该文件。
3. 点击 `下载并归类`。
4. 验证预览显示 `未分类/待处理` 和具体原因。
5. 选择本地测试根目录后，验证文件写入 `_未分类待处理` 相对目录。
6. 验证后端未创建受控文件，未写入 `dcc_controlled_file_nas_source` 当前 ACTIVE 映射。
7. 验证待处理明细可导出或在页面保留。

### Path D: 浏览器目录写入能力缺失

1. 在受控测试上下文中移除或禁用 `window.showDirectoryPicker`。
2. 打开 NAS 管理页并选择一条未受控明细。
3. 点击 `下载并归类`。
4. 验证页面显示不支持本地目录写入的明确错误。
5. 验证 import task 请求数、content 下载请求数、local-write-result 请求数和目标 DCC 写请求数均为 0，后端本地写入状态保持不变。

### Path E: 用户仅查看不下载

1. 完成未受控文件统计并打开明细表。
2. 点击处理入口后选择“不下载/仅查看”或取消确认。
3. 验证页面仍保留明细和报告下载入口。
4. 验证未创建 import 任务，未调用 content 下载接口，DCC 写请求数为 0。

### Path E2: 用户取消本地目录选择

1. 完成未受控文件统计、识别并勾选一条未受控明细。
2. 点击 `下载并归类` 并在目录选择授权窗口中取消。
3. 验证页面提示用户已取消本地目录选择或保持在预览状态。
4. 验证未创建 import task，未调用 content 下载接口，未回写 local-write-result，DCC 写请求数为 0。

### Path F: 本地写入失败不归档

1. 使用任务自有可匹配样本完成统计和识别。
2. 在 Playwright 中让目录句柄或文件写入返回受控失败。
3. 点击 `下载并归类` 并选择测试目录。
4. 验证页面显示本地写入失败原因。
5. 验证后端状态为 `LOCAL_WRITE_FAILED`，记录具体 `localWriteErrorCode`，且没有创建 DCC 受控文件或 NAS 来源映射。

### Path F2: 本地目标已存在或路径过长时阻塞

1. 使用任务自有可匹配样本完成统计和识别。
2. 在本地测试根目录预置同名目标文件，或构造超过允许长度的目标相对路径。
3. 点击 `下载并归类` 并选择测试目录。
4. 验证页面显示 `LOCAL_PATH_COLLISION` 或 `LOCAL_PATH_TOO_LONG`。
5. 验证该阻塞发生在 `import-selected` 之前：无 import task、无 content 请求、无 local-write-result、后端 audit 下载状态不变。
6. 验证不覆盖本地已有文件、不截断路径、不调用 DCC 归档请求。

### Path G: NAS 文件变化后重新统计

1. 完成统计后修改、移动或重命名任务自有 NAS 样本文件。
2. 从旧 audit 明细发起下载并归类。
3. 验证 content 接口返回 `NAS_SOURCE_CHANGED` 或等价业务错误。
4. 验证页面提示重新统计，且旧 audit file 未进入本地写入成功或归档成功。

### Path H: 大文件使用二进制或分块下载

1. 准备任务自有大文件样本，大小超过 JSON/base64 承载安全阈值但低于产品允许上限。
2. 从 NAS 管理页统计、识别并选择该文件。
3. 选择本地目录后，监听 content 请求响应头、响应类型和写入过程。
4. 验证 content 响应为二进制流或明确分块协议，不出现 JSON/base64 文件内容字段。
5. 验证写入成功后再回写 `LOCAL_WRITTEN`；若模拟分块失败，必须回写 `LOCAL_WRITE_FAILED`。

### Path I: 跨任务或签名失效不能下载内容

1. 准备两个任务自有 audit task，分别生成不同 `auditFileId` 和 `sourceSignature`。
2. 通过页面为其中一个 task 创建合法 import task。
3. 使用另一个 task 的 `auditFileId`、过期 `sourceSignature` 或已归档 audit file 触发 content 请求。
4. 验证 content 请求返回明确绑定关系、权限或签名错误。
5. 验证不返回文件内容、不推进 `CONTENT_READY`、不回写 local-write-result、不触发 DCC 归档。

### Path J: 幂等键请求内容变化必须冲突

1. 使用任务自有样本完成统计、识别和目录授权预检。
2. 使用某个 `idempotencyKey` 和显式 `auditFileId/sourceSignature/localRelativePath` 创建 import task。
3. 再次使用相同 `idempotencyKey` 但改动任一 audit file、签名或本地相对路径。
4. 验证后端返回请求哈希不一致的冲突。
5. 验证原 import task 快照未改变，未创建第二个 import task 或第二个受控文件。

### Path K: 分页选择只处理显式勾选文件

1. 准备超过一页的任务自有未受控明细。
2. 在第一页勾选部分行并点击 `下载并归类`。
3. 验证确认文案显示当前选择数量，而不是全部筛选结果数量。
4. 验证 `import-selected` 请求只包含已勾选行的 `auditFileId`。
5. 验证未勾选页和未加载页的明细状态保持不变。

### Path L: 混合无效选择整体拒绝

1. 准备两个任务自有 audit file，一个已完成识别且签名有效，另一个属于其它 audit task、仍为 `PENDING_RECOGNITION` 或携带过期 `sourceSignature`。
2. 通过页面或受控请求一次性提交这两个 audit file 到 `import-selected`。
3. 验证后端返回明确错误码，且不创建 `NAS_UNCONTROLLED_IMPORT` 任务。
4. 验证两个 audit file 的 `downloadStatus`、import 绑定、content 请求数、local-write-result 请求数和 DCC 写请求数均未变化。
5. 使用相同合法选择但调换 `selectedFiles` 顺序重复提交，验证返回同一个 import task，不产生幂等冲突。

### Path M: 重复本地写入回写不重复归档

1. 使用任务自有可匹配样本完成统计、识别、目录授权、content 下载和首次 `LOCAL_WRITTEN` 回写。
2. 等待首次 DCC 归档完成并记录受控文件编号和 NAS 来源映射。
3. 重放完全相同的 local-write-result 请求。
4. 验证后端返回当前状态，不创建第二个受控文件、不新增第二条 ACTIVE NAS 来源映射。
5. 再提交冲突的 local-write-result，例如已成功后提交 `LOCAL_WRITE_FAILED`，验证返回明确冲突且原归档状态不变。

### Path N: Import task 创建后旧处理器不得自动归档

1. 使用任务自有可匹配样本完成统计、识别、本地目录授权和 `import-selected`。
2. 在调用 content 下载前等待一个旧 NAS transfer 轮询周期或显式触发任务状态刷新。
3. 验证 audit file 仍为 `downloadStatus=SELECTED`，处理项未进入 `CONTENT_READY/LOCAL_WRITTEN/ARCHIVED`。
4. 验证后端未读取该文件内容、未调用 DCC submit、未创建受控文件、未写入 ACTIVE NAS 来源映射。
5. 再继续 content 下载和 `LOCAL_WRITTEN`，验证只有回写后才允许进入正式归档链路。

### Path O: 归档元数据缺失时可见阻塞

1. 准备可匹配样本，但故意不配置正式 DCC 归档所需模板分类、生效日期、变更原因或等价元数据来源。
2. 完成统计、识别、目录授权、content 下载和本地写入成功回写。
3. 验证本地状态保持 `LOCAL_WRITTEN`，归档状态进入 `FAILED` 或明确阻塞状态，错误码为 `ARCHIVE_METADATA_REQUIRED` 或等价稳定错误码。
4. 验证系统未使用当前日期、旧 NAS transfer 任务值、空模板或默认模板创建 DCC 受控文件。
5. 补齐正式元数据来源后才允许通过显式重试或后续设计路径继续归档。

## Browser or Client Steps

- 使用 Chromium 或 Edge 执行本地目录写入路径。
- 本地测试根目录必须位于任务目录或专用临时目录，名称包含 task id。
- Playwright 需要捕获 `pageerror`、console error、目标 DCC 写请求、NAS 处理请求和本地写入结果回写请求。
- Element Plus 表格勾选必须按可见 NAS 路径唯一文本定位行，不能使用数组下标或表头全选。
- 确认框必须断言待处理数量、可归档数量和不可自动归类原因。
- 分页场景首版只验证显式勾选行；若页面出现“处理全部筛选结果”，E2E 必须先找到服务端 selection snapshot/token，否则该路径阻塞。
- 目录写入能力测试必须显式 stub 或驱动 `showDirectoryPicker`、`getFileHandle`、`createWritable`、`write` 和 `close` 的成功/失败路径。
- 本地相对路径断言必须覆盖 `/` 分隔、禁止 `..`、禁止盘符、禁止非法字符、禁止 Windows 保留名和规范化冲突。
- 本地相对路径断言必须覆盖目标文件已存在和路径过长；验证不覆盖、不截断、不自动改名。
- 请求顺序必须断言：目录授权和相对路径校验成功之前不得调用 `import-selected`；`LOCAL_WRITTEN` 回写之前不得出现正式 DCC 归档请求。
- `import-selected` 创建任务后、content 下载前必须有观察点断言旧 NAS transfer 轮询或 waiting processor 不会自动处理 `NAS_UNCONTROLLED_IMPORT`。
- 取消目录选择、浏览器不支持和本地路径预检失败必须断言后端状态不变化；这些路径不得用 `LOCAL_WRITE_FAILED` 冒充已创建处理任务后的失败。
- import task 创建后因目标文件并发出现或最终写入句柄失败时，才允许调用 local-write-result 回写 `LOCAL_WRITE_FAILED`；E2E 必须区分“创建前预检失败”和“创建后写入失败”。
- 成功归档路径必须先观察 `local-write-result=LOCAL_WRITTEN`，再观察 DCC 归档请求；不得只以最终列表出现文件证明时序正确。
- 归档路径必须验证正式 DCC submit 元数据来源；缺少模板分类、生效日期、变更原因或等价元数据时，只能看到可见阻塞或失败，不能看到默认成功。
- `import-selected` 混合合法与非法选择时必须断言整体无状态变化；不得把部分成功当作通过。
- 相同合法选择仅 `selectedFiles` 顺序不同必须断言返回原 import task；重复 id 必须断言失败而不是被前端或后端静默去重。
- local-write-result 重放必须断言 DCC 归档请求不会再次出现，且冲突结果不会覆盖既有终态。
- 大文件路径必须断言 content 接口使用二进制、流式或分块语义；不得用 JSON/base64 响应冒充文件下载。

## API Verification

- `GET /admin-api/dcc/controlled-files/nas-control-audit/{auditTaskId}`：核验统计任务状态和计数。
- `GET /admin-api/dcc/controlled-files/nas-control-audit/{auditTaskId}/files`：核验明细分页、关键词过滤、分类/下载/归档状态过滤、稳定排序和仅返回当前租户数据。
- `POST /admin-api/dcc/controlled-files/nas-control-audit/{auditTaskId}/import-selected`：核验未识别、重复 auditFileId、跨 task/tenant、签名不匹配或已归档明细均被拒绝。
- `POST /admin-api/dcc/controlled-files/nas-control-audit/{auditTaskId}/import-selected`：核验相同 `idempotencyKey` 但请求哈希不同会冲突，且客户端提交的 `localRelativePath` 必须与后端识别快照生成的期望路径一致。
- `POST /admin-api/dcc/controlled-files/nas-control-audit/{auditTaskId}/import-selected`：核验任一选中项无效时整个请求无状态变化，且同一合法选择不同顺序返回原任务。
- `POST /admin-api/dcc/controlled-files/nas-control-audit/{auditTaskId}/import-selected`：核验响应和落库任务不要求旧 NAS transfer 的任务级 `templateCategoryId`、`effectiveDate`、任务级项目代码或分类字段。
- `GET /admin-api/dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}`：核验处理任务完成、待处理和失败数量。
- `GET /admin-api/dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}`：在 content 前核验 import task 未被旧 processor 推进到内容下载、本地写入或归档状态。
- local-write-result API：核验只保存相对路径、写入状态和错误原因，不保存本地绝对路径；回写必须匹配 import task 快照和 `sourceSignature`。
- local-write-result API：核验相同终态结果重复回写幂等、冲突终态结果被拒绝，且不会重复触发归档。
- content API：核验当前用户、租户、import task、audit file 和 source signature 绑定；禁止跨任务、跨租户或猜测 ID 下载 NAS 文件。
- DCC 项目代码关联文件分页 API：只读核验目标文件出现在正确项目代码下。
- DCC 受控文件详情 API：只读核验 `fileTypeTaxonomyId`、`fileTypeLevel1-3`、`dccProjectCodeId`、`productMasterId=null`。
- 数据库只读核验仅在页面和 API 证据不足时使用，并且必须记录租户和任务自有数据范围。

## Console and Log Checks

- `pageErrors` 必须为空。
- 本机前端、后端和 DCC 目标链路 HTTP 错误必须为 0。
- 外部头像、CDN 或非目标资源异常可单独记录，但不能影响目标控件和断言。
- DCC 写请求数量必须与路径期望一致：只读路径为 0，取消/仅查看路径为 0，待处理路径不创建受控文件，本地写入失败路径为 0，成功归档路径只创建任务自有文件。
- 在 Path N 的 content 前等待窗口内，DCC 写请求和 ACTIVE NAS 来源映射新增数量必须为 0。
- 后端日志不得出现被吞掉的 NAS 读取失败、分类失败或本地写入结果失败。
- 后端日志不得出现 `NAS_UNCONTROLLED_IMPORT` 被旧 NAS transfer processor 自动处理的记录。

## Test Blockers

- 前端入口、动态菜单、角色权限、NAS 配置或目标按钮缺失时阻塞。
- 本地浏览器不支持目录写入能力时，成功下载到目录路径阻塞；可执行 fail-fast 路径验收。
- 缺少任务自有 NAS 样本、可写测试租户、项目代码、分类规则或清理授权时，写入型 E2E 阻塞。
- 如果 Playwright 无法真实驱动本地目录授权，必须记录浏览器能力阻塞，不能改用 API-only 或 ZIP 下载冒充通过。
- 正式归档元数据来源未配置时，成功归档路径阻塞；此时只能验证 `LOCAL_WRITTEN` 后的 `ARCHIVE_METADATA_REQUIRED` 或等价阻塞状态。
