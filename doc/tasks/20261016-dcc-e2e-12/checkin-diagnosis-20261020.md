# R2 PUBLIC 驳回后检入阻塞：只读核对与最小修复提案

## 结论与证据边界

原 R2 结果保持不变：正向两份、驳回四份的主合同通过；PUBLIC 重提 BLOCKED，CUSTOM 重提 NOT_RUN。原驳回文件未再操作，未重复检入、签名或提交。

本次已确认测试夹具不满足现有“同一源文件名检入”约束。原上传文件是 `DCC241020C12R2R2PUBLICREJC1.pdf`，替换文件却是 `DCC241020C12R2R2PUBLICREJC1REWORK.pdf`。后端明确拒绝此类文件名变化，并复用同一个请求无效错误。这是直接的夹具缺陷，也是本次拒绝的强候选原因；不能将其直接归因为前端丢失修改说明。

错误文案校正：源码及原截图实际为“检入请求缺少真实文件或修改说明”，含“或”字。早期交接中的“检入请求缺少真实文件修改说明”是不精确转录；原截图、trace、首次失败及 R2 BLOCKED 均保留。

该次 trace 的 `trace.network` 为 **0 字节、0 条记录**，录制配置为 `snapshots: false`。原 runner 仅另存自然请求的 method/path/status。因此没有可供核实的实际请求 JSON，不能宣称已从线上报文确认 changeDescription 的值，也不能唯一定位服务端抛错行。当前结论区分页面事实、当前代码事实与运行时推断，不读取接口响应 JSON 作通过依据。

## 前后端字段链

|环节|当前代码事实|代码位置|
|---|---|---|
|页面输入|修改说明以 `v-model="checkinForm.changeDescription"` 绑定，状态初始化为空字符串|`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:1074`、`:1371`|
|页面提交|`submitCheckin` 先 trim 该字段，空值时直接提示“请输入修改说明”并 return；同一个局部变量放入 payload，调用 `checkinControlledFile(baseId, payload)`|同文件 `:2216`、`:2247`、`:2283`、`:2305`|
|API wrapper|`ControlledFileCheckinReqVO.changeDescription` 为 string；wrapper 校验附件清单及 id 后原样将 data 传给 request.post；Axios 包装保留 data|`IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts:965`、`:2314`；`IntRuoyiFronted/src/config/axios/index.ts:8`|
|后端入口|Controller 使用 `@RequestBody DccControlledFileCheckinReqVO`，随后传给三参数 checkinControlledFile|`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java:620`|
|修改说明校验|读取本次 `reqVO.getChangeDescription()`，不从旧审批矩阵、旧变更说明或冻结快照补值；空值抛请求无效错误|`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:746`、`:755`|
|源文件身份校验|从已持久化的 SourceOriginalFileName / 已封存名称证据取得 verifiedSourceName，拿它与本次 SOURCE ticket 的 bound.fileName 精确比较；不同则抛同一个请求无效错误|同文件 `:825`、`:845`；`DccControlledFileNameClaimService.java:237`|
|其他复用场景|MINOR/正式变更意图校验、缺附件清单或培训布尔值、某些重放载荷校验等，也复用该错误；不能仅凭提示断言说明为空|`DccControlledFileQueryServiceImpl.java:1525`、`:1739`；`ErrorCodeConstants.java:330`|

页面 guard 已放行并自然发出唯一一次 checkin 请求，结合修改说明字段 DOM 有值，支持“页面提交路径接受了非空说明”的判断；仍不冒充报文证据。后续申请轮次/矩阵继承发生在源文件校验之后，不能将这次失败称为冻结矩阵不一致。

## 对应原始证据

- 原文件：fileId `2054545668044084346`，masterId `2054545668044079270`；BPM `880311ed-cc48-11f1-ada5-b082e25ec548`；账号 `DCC241016C12R3L1`。
- 初始上传：`runs/20261019-r3/rerun-20261020-r2-round2-cal3/session.json`，step 132，原 basename 不含 REWORK。
- 失败会话：`runs/20261019-r3/rerun-20261020-r2-round2-continuation2/session.json`，step 13 的两 textarea 均有值，且已上传 REWORK 文件；step 14 唯一一次提交；step 15 等待弹窗关闭超时。自然请求仅记录 POST/HTTP200，不能作为业务成功依据。
- 错误画面：`rerun-20261020-r2-round2/checkin-after-click-35.jpeg`；来源 `runs/20261019-r3/rerun-20261020-r2-round2-continuation2/leader1-context1-1-trace.zip`，画面时间本地 `2026-10-20 13:48:33.505+08:00`。
- 持久状态：`rerun-20261020-r2-round2-inspection` step 4 显示 L1 仍检出；`rerun-20261020-r2-round2-terminal-inspection` step 4 显示原 A/1 已驳回、版本历史一条、无后继版本、内容哈希前缀 `48f52e7b6801`。
- 原正文与 REWORK 正文哈希不同，已由封存断言核实；本问题不是“正文内容没有变化”的证据。

## 最小 BDD

- Given：同一任务自有文件已驳回，原审批及矩阵证据冻结，编制人通过正式入口检出；新正文与原正文内容不同，但保持同一源文件 basename。
- When：编制人上传此新正文、填写真实修改说明并仅提交一次检入。
- Then：生成同一 Master 下的新 WORKING 小版本并释放本次检出；旧驳回版本/BPM/冻结证据保留。新工作版本随后才可准备重提，沿用本次原保存矩阵并核对人员可用性。检入完成不等于后续重提或会签已通过。
- 负例边界：缺少说明、正文无实际变化或替换件文件名不同必须具体阻止，不得通过放宽源身份校验让改名夹具通过。

## 本线程拥有范围内的最小 TDD 提案（尚未实现）

只涉及 `doc/tasks/20261016-dcc-e2e-12/` 下新建的夹具生成器及其离线断言；不改原始 R2 上传文件、脚本队列、截图、trace 或正式业务对象。

1. RED：用已封存的原文件及 REWORK 替换件证明“供检入的 basename 必须与原件一致”这一夹具合同失败，同时断言二者内容哈希确实不同。该 RED 是测试数据合同失败，不是产品回归。
2. GREEN：将改正文文件放到新的任务自有子目录（例如 `rework/`），仍命名为 `DCC241020C12R2R2PUBLICREJC1.pdf`，只改变正文。让夹具生成器在输出前同时校验 basename 相等、内容哈希不同、输出路径在任务目录内。
3. REGRESSION：离线断言保留原文件哈希、两份正向及四份驳回的原报告状态、原证据文件；拒绝同内容与异名替换件。此阶段不登录页面、不上传、不检入。
4. 新 E2E 仅由 Root 单独放行，并先确认原检出残留如何处理；本次不操作原文件，也不复跑 CUSTOM 已通过链。不得把离线 GREEN 当作检入或重提 E2E PASS。

“提出修复”不等于获得共享源码写入范围：本次未改产品代码、未新增/执行产品测试。若 Root 希望把通用错误拆为具体“源文件名不一致”提示，需先协调 `DccControlledFileQueryServiceImpl.java`、`ErrorCodeConstants.java` 及共享回归测试的归属，再另写独立 RED/GREEN；不以此扩大当前修复范围。

## 状态与限制

只读诊断和提案完成，修复尚未实施，线上命中分支因未录请求体/栈仍未唯一确认。保持 HOLD、UI 执行门禁关闭，原 R2 交接状态不变。未启动 browser/context/runner，未 API/DB 代办、未 Maven/build/runtime 操作、未提交推送、未清理、未子 Agent。
