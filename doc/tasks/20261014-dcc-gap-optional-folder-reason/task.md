# G12 移除额外文件夹登记说明必填

## Current Status
blocked — 软件验收已由Root正式接受：M14真实exit0、23 reactor SUCCESS，G48声明18+原父类27=45/45，0F/E/skip；与M12其余963 PASS合计有效47份class报告1008/1008，11865输入0漂移。F02 38/38、T04类型、B01构建及2429生产输入0漂移均据Root回执通过。全部生产/tests已归还冻结，无待实施/验证的软件事项；唯一未授权收尾是Git，不标记completed。清理和总报告由Root统一办理，未要求的E2E/部署不是软件失败或阻塞。

## 任务目标
普通上传在规定必填完整、项目文件夹有效时，不因登记说明为空被拒绝。保留可选说明及 500 字长度校验，不编造默认原因；保持真实项目归属、操作者、归档事实与审计事务。

## 里程碑
1. 核对 int_qms、当前源码及相邻测试，记录修改前 SHA256。
2. 前端行为测试先 RED，修改 upload/index.vue 与 upload/submitter.ts 后 GREEN。
3. 运行定向相邻回归，记录后端及审计接入依赖，提交 review-ready 并归还前端文件。
4. A01/A02/M02后端两服务及独立审计接入已完成；A37唯一fixture修正后，Root M14正确fixture覆盖45/45并与M12组成1008/1008有效最终报告，全软件域正式接收；文件/tests/验证票已归还冻结，仅Git收尾未授权。

## Expected Verification
- 独立 Node 行为单测：实际 payload builder、save/submit service、页面冻结函数，覆盖空/缺省/空白说明、500/501 字、有效说明、无效/跨项目文件夹和外来评审。
- 相邻上传属性、文件夹上下文、单项目文件夹、确认清单与安全测试；独立Node已执行57/57，Maven仅按Root唯一票执行。
- 后端M02验证已完成：空说明真实归档，500/501，错误租户/项目/文件夹/权限拒绝，真实actor/before/after及空串原因，审计/晚失败精确DB快照回滚与重试，当前派生存储5专属方法回归。
- 不执行 E2E、服务操作、真实业务库写入，单测结果不称运行验收。

## BDD
- Given 规定必填完整且项目文件夹有效，When 未填写或仅空白登记说明保存/提交，Then 前端冻结及载荷构建成功，不生成虚假说明。
- Given 有效项目文件夹，When 登记说明规范化后长 500 字，Then 保留真实说明；When 超过 500 字，Then 明确报错并零传输。
- Given 缺少文件夹或文件夹不属于当前项目/已失效/尚在加载，When 提交，Then 拒绝，不以空说明放宽归属校验。
- Given 后端普通上传，When 无说明归档，Then 记录真实操作者、精确文件与项目文件夹及真实归档事实；说明保持实际未填写，无虚假默认原因（M02真实H2 PASS）。

## 设计约束检查
- 仅 G12；前端授权范围为 IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue、submitter.ts 及新增专属测试。
- A01授予两个placement服务及精确所属测试；A02追加wrapper原因锚点、旧原因断言、新upload-bind policy条目；M01 RED及M02 GREEN/回归已完成；其它Workflow/API/policy/公共审计服务不写。A04前三个共享前端文件已归G01。
- 最新 coordination.md 覆盖历史明确文件归属；共享前端按 G12 → Root → G01 → G03 → G06 交还。
- 无 fallback、吞异常、默认原因；不改组织/权限/版本/签名/事务/幂等合同。
- 禁止子 Agent、Git 提交推送、切分支、worktree、E2E、重启、远端及真实库写入。

## 修改前 SHA256
实际分支：int_qms。工作区已有大量并行改动，仅处理授权资产。

| 文件 | SHA256 |
|---|---|
| IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue | F0A8BE6C29DAFD096B97D2179FEA1EAD37F513DD5FBDB546F33E19C29845B652 |
| IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts | 5D80D0061B6A8FBC606872EBC4DB406421F7A78AA838B32261AD878C882D6DE6 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicUploadPlacementService.java | 44D0EB527994C36506AF6C1E30AE9EF24F121D4292F374C480C55F2555E4A947 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/folder/DccProjectFilePlacementService.java | 4C8A62C8271CC649C0E8C4683A9AE9FE42D61F5B64AC3BA11B88EC0255C442E1 |

## Cleanup Keep
- doc/tasks/20261014-dcc-gap-optional-folder-reason/review-ready.md
- doc/tasks/20261014-dcc-gap-optional-folder-reason/changed-files.md
- doc/tasks/20261014-dcc-gap-optional-folder-reason/integration-notes.md
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-receipt.md
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-source-manifest.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-process-receipt.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-red-junit.xml
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-red-maven.log
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-red-r2-source-manifest.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-red-r2-process-receipt.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-red-r2-junit.xml
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-red-r2-maven.log
- doc/tasks/20261014-dcc-gap-optional-folder-reason/a02-frontend-release-hashes.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m01-post-run-source-stability.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-receipt.md
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-source-manifest.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-source-stability.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-process-receipt.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-maven.log
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-attempt1-junit.xml
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-r2-source-manifest.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-r2-source-stability.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-r2-process-receipt.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-r2-maven.log
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-green-r2-junit.xml
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-regression-source-manifest.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-regression-source-stability.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-regression-process-receipt.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-regression-maven.log
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-regression-junit.xml
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-release-hashes.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/m02-final-receipt.json
- doc/tasks/20261014-dcc-gap-optional-folder-reason/a37-fixture-handoff.json
