# R2 A→B 迟到分支调查

结论：已证实 runner 的首测时点判定错误，校准又在默认 5 秒截止点截断观察；现有证据未证明产品缺陷。原第四轮首测 BLOCKED、一次校准 FAIL、整体 FAIL/HOLD 均保持原样，调查不补写 E2E PASS。

本轮仅离线读取两份原 trace、当前源码并执行定向单测。未启动浏览器，未重跑六个已 PASS 主分支，未修改产品、权限、浮层、服务或共享测试，无构建、重启、Git 提交推送、数据库操作或清理。

## 原始时序

时间取自同一 trace 的单调时钟，单位毫秒；不跨两次运行比较单调时钟。

| 运行 | 事件 | 时点 |
| --- | --- | ---: |
| 首测 | 原生点击 B 的只读记录完成 | 94126.395 |
| 首测 | `waitForURL(B)` 完成 | 94141.670 |
| 首测 | A 下载 response 事件结束 | 97845.582 |
| 首测 | A 下载网络记录完成 | 97847.411 |
| 首测 | B 培训附件存在断言通过 | 98174.572 |
| 首测 | 不含 A 培训附件断言通过 | 98199.022 |
| 校准 | `waitForURL(B)` 完成 | 32819.605 |
| 校准 | B 附件断言开始，默认 timeout=5000 | 32819.819 |
| 校准 | B 附件断言失败 | 37993.271 |
| 校准 | 空详情截图采集结束 | 38120.775 |

首测实际满足“B 路由先切换、A 响应约 3.7 秒后到、最终 B 附件显示正确、没有原生下载”的可见顺序。runner 却等两个 B 附件断言完成后才记 `changedAt=1792479906100`，再与更早到达的 A 响应 `1792479905741` 比较，因而给出 BLOCKED。该字段代表目标数据就绪，不能代替路由身份切换时间。原始结果可供 Root 复核，但本调查不改写其状态。

校准将路由时点移到正确位置，但仍沿用未显式指定 timeout 的附件断言；Playwright expect 的本次实际值为 5000ms。断言失败时 A 下载和一条 B 详情读取在 trace 中尚无响应，随后 runner 开始保存 trace、关闭会话，后续结果被截断。另有一条 B 详情读取已在约 4.35 秒完成；两条读取不能混为一条。HAR 中 `status=-1/time=-1` 表示记录尚未完成，不能证明产品主动取消、请求失败或死锁，也不能证明 B 以后一定恢复。

## 当前产品代码核对

- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue:5220` 的 `reloadAll` 增加详情读取序列并先清空旧详情；`loadData:4917` 等待详情及辅助列表，再在 `isCurrentDetailLoad:4910` 核对序列、文件 ID、完整路由及账号/租户后发布数据。路由 watcher 位于 `7506`。当前 index.vue SHA 与原冻结证明一致。
- 同文件模板 `:49`、`:58` 要求当前详情身份有效，才挂载只读基本信息与 `ControlledFileBrowseRecords`。因此读取未完成时，空详情壳与记录到的 DOM 一致；仅 5 秒空壳不能证明永久卡住。
- `ControlledFileAttachments.vue:68` 在文件或指纹改变时增加下载代次；`:71` 卸载也增加代次；`:99` 下载响应返回后，在 `:100` 核对代次与文件指纹，再决定是否执行 `downloadByData`。旧下载的 finally 也只清理自己的当前代次状态。该组件不写父详情序列，也不取消 B 的读取。
- `workflow.ts` 的 `downloadControlledFileAttachmentWithName` 使用独立 axios 二进制请求，没有把 B 详情的 AbortSignal/取消器交给旧下载；当前 axios 请求超时配置是 30000ms，而不是 runner 的 5000ms。
- `ControlledFileBrowseRecords.vue:345` 原生 `openVersion` 只执行既有 traceability 路由跳转；元数据读取有独立 generation。`AppView.vue` 按 path 缓存页面，详情的全路由 watcher 未因缓存停用自动停止，可能解释两条 B 读取；trace 无组件实例身份，故这只是重复读取的解释，不作为本分支缺陷定论，不开展缓存/导航重构。

结论边界：源码与单测支持“旧响应被隔离”，没有证据链证明它导致 B 不加载。校准的 B 最终状态和 A 响应完成状态未被观察到，仍不能断言产品完全无缺陷。未证实产品缺陷，因此没有伪造生产 RED，也没有修改共享前端；不需要安排新的共享前端改动。

## 定向验证与原始错误

1. 原样运行 `node --test tests/unit/dcc-detail-route-read.test.cjs tests/unit/dcc-detail-authority-late-response.test.cjs tests/unit/dcc-attachment-download-disposal.test.cjs`：退出 1，32 项中 22 PASS、10 FAIL。10 项均在 VM 宿主缺少 `approvalProgressNotice` 时中断，未到达待验断言。真实 index.vue 已在 3115 行声明该 ref，不能把宿主 ReferenceError 当产品 RED。原日志 `r2-switch-investigation-focused-tests.log` 保留。
2. 附件下载隔离的原 14 项已原样通过，包括卸载后迟到字节不保存、旧响应不释放新下载锁和身份变更。
3. 任务内 `r2-switch-investigation-harness.cjs` 仅在内存中给两个既有宿主增加一个 `vue.ref('')`，仍执行当前源码与原断言，不改原测试文件。18 项全部 PASS，退出 0；日志为 `r2-switch-investigation-calibrated-tests.log`。该结果是单元级证据，不能替代真实页面。
4. `python -X utf8 doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation-analyze.py`：退出 0。直接从原 ZIP 提取并断言路由/响应顺序、5 秒等待和未完成请求，生成 `r2-switch-investigation-evidence.json`，包含原 trace SHA256 及当前源码指纹。

## 后续最小 runner 方案（未执行）

仅当 Root 再放行该分支时，建立独立证据目录，只操作同一 A/1、B/1 和 ViewDownload 身份，保留全部既有结果。无需改产品或重新跑六主分支。

- 在确认前，为 A 文件 2054545668044084233 的附件 65 精确注册 request/response 监听；独立记录请求、路由身份切换、响应头到达、响应字节完成和 B 附件就绪时点。
- `waitForURL(B)` 后立即记 `routeChangedAt`，响应监听同时记录当前页面 URL。B 附件加载采用已有详情 helper 的显式 60000ms 等待，并监测页面明确错误；不能把该等待完成时点当切换时点。
- 无论目标附件断言成功或失败，都在 finally 保存 response、download 事件和时点；未收到原请求响应不能计“迟到隔离 PASS”。保证观察完旧响应或记录明确请求失败/超时后再关闭会话。
- 判定必须同时满足：旧请求真实发出；B 路由已切换且旧响应随后返回；原生下载为零；B 附件正确且不混入 A。响应先于切换为 BLOCKED；未完成读取或可见错误原样记录，不重试掩盖。

HOLD，供 Root 审阅。原 E2E handoff 仍为 `rerun-20261020-r2-round4-handoff.json`，本次调查入口为 `r2-switch-investigation-handoff.json`。
