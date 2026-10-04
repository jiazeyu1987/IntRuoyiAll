# G23 源测试库升级工具只读安全审查

状态：ready_for_closeout，G23 源升级工具只读审查已完成，供 Root Review。共享任务仍 in_progress。四项首轮问题已在 Owner 停写版本复核修复；当前无真实 clone PASS，源升级保持 PREPARED_BLOCKED。本文不授权数据库写入，不是应用就绪报告。

## 范围与证据边界

Root 仅授权审查并写本文件。G22 工具、批准模板、SQL、hash 范围和任务 `.gitattributes` 保持冻结；G21 candidate manifest 未重扫、未 stage。没有修改生产、backend 工具/tests、Root 文档或其它任务状态。未运行 actual flag、数据库、服务、Maven/build、Git 或 E2E。仅允许读取源码/既有证据、纯内存负例和离线门禁。

先与 backend Owner 协调；Owner 表明源码可审，但完整 clone/backup/prerequisite 负例及接口说明仍在补。以下首轮锚点来自 `doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-driver.py` 原始 SHA `672ad21c46d1e7a7c86aef58649b279dcaf6115d2ac533a56aed4af3a35fcdd0`，Owner 持续写入期间不把该指纹当最终版本。

审查依据为 AGENTS、`docs/database-rules.md`、`docs/task-closeout-rules.md`、`docs/release-backup-restore.md`；任务分工明确由 Root 独占运行/数据/Git。参考冻结 G21 driver、Root `g21_mysql_support.py` / `g21_migration_scope.py` / `g21_execution_materials.py` 和 detail 正式 postflight validator，全部只读。

BDD：Given 真实隔离克隆的首次/重复执行完整原始证据，When 准备源库升级，Then 缺任一 artifact、33 seed、19 ledger、schema、历史行和环境证明均阻断，不能用 PASS 字符串或测试夹具代替。

BDD：Given 同一已排除全部 writer 的源库窗口，When 收集 fresh900 的 25 依赖、全原列 baseline16+ledger、源环境和新备份，Then 它们应绑定同一真实目标与时间顺序；旧 G18 备份改名、过期采集重新盖时间、列子集不能成为源执行前置。

BDD：Given 已获明确源库写授权并满足完整前置，When 任一真实 SQL/检查失败，Then 首错停止保留实际证据，不执行源恢复/自动重跑/清库，不声称 DDL 已原子回滚；即使全部迁移检查通过也只报告源迁移范围，不报告 application ready。

## 已确认的正向边界

| 检查 | 源码锚点 | 首轮结果 |
| --- | --- | --- |
| 缺真实 clone 保持准备态且零 client | `prepare:184` | 真实入口纯内存探针 PASS，返回 PREPARED_BLOCKED_MISSING_REAL_REHEARSAL_RECEIPT / writeConnectionAllowed=false / databaseWritesExecuted=false；client、subprocess.run/Popen 均不调用 |
| 源写技术 flag 不能证明授权 | `upgrade:217`，docstring | 除 technical gate 外还要求 Root 记录实际用户写授权及引用；模板 false/null。这些字段记录会话批准，不是独立的批准系统，Root 仍须保留原始授权事实 |
| 固定源库、容器、server UUID、MySQL 版本 | `prepare:189` / `require_writer_free:174` | 硬绑定当前本机测试目标，禁止切 clone/外库 |
| 缺 fresh source 输入阻断 | `prepare:195` | clone 已验证后仍需 proof、new backup、baseline、environment、writer exclusion 五项 |
| clone 源码及全目录 artifact 防漂移 | `g21_module:40` / `artifact_map:57` / `verify_clone_pass:69` | 固定 G21 SHA、保护目录相对路径、逐文件 SHA、目录文件完整集合；非真实/test fixture 声明拒绝 |
| 首次/重复克隆部分实质复算 | `verify_clone_pass:81-135` | 原 dump SHA、原19SQL bytes+同会话 environment、markers、历史快照比较、33 raw facts、正式 schema validator 从 raw schema 重算 |
| fresh 窗口 | `fresh:34` | 明确900秒、过去/未来越界拒绝；不是授权期限或自动延长 |
| writer 事实覆盖全连接/事务/events | `writer_query:181` | 不限 DB=源库，不漏 DB=NULL 的连接；全事务和启用事件；瞬时零值不能替代 Root 持续停写窗口 |
| 源执行前再次读当前 baseline/候选 ledger/新表/环境 | `upgrade:229-243` | source16+ledger 当前值比较、目标新表须不存在、候选19台账须不存在，环境必须同冻结源环境；马上再验输入与 writer |
| source 实际后置与失败边界 | `upgrade:245-263` | 尝试前记录 partialDdlCommitPossible；单次原19SQL，无自动恢复；实际 after snapshot、history、seed33、ledger19、新17空、source schema；终态明确 NOT_APPLICATION_READINESS |
| source postflight 不借 clone PASS | `source_postflight:209` | 正式 validator 的 allowedDatabases 包含 source+clone，但此处强制返回 source+当前 UUID；已核对，无“永远固定 clone”的假缺陷 |

## 已发送 Owner 的具体缺口

| ID | 首轮严重性/结论 | 可复核锚点与实际影响 | 所需修复或执行门禁 |
| --- | --- | --- | --- |
| G23-R01 | 高：原列完整性未验证 | `verify_source_snapshot:167` 只要 columns 非空；`upgrade:230-231` 把它原样传 `snapshot_original_rows(..., existing_columns)`。实际函数纯内存探针接受 `dcc_controlled_file.columns=['id']`；其余业务列既不进入当前值比较，也不进入后置旧行保护。 | 写前只读获取全部实际原列，要求精确集合/顺序匹配 baseline16+ledger，再捕获；任何漏列/多列/类型前置异常均零写入。 |
| G23-R02 | 高：fresh25 语义与时间链未闭合 | `verify_source_prerequisites:153-159` 复用的 `verify_prerequisite_receipt` 仅核对 proof status/count/hash 和捕获目标。未从 raw facts 重跑 structure/BPM 正式 validator；group.collectedAtUtc 没绑定 capture.capture.collectedAtUtc；`prepare` 未将新 receipt 的25 ID 精确比对实际19的 external dependency closure。 | 重算两个正式 validator，精确25依赖身份匹配；读取实际采集 receipt 时间并绑定 group 与 fresh900/同writer epoch，不能改聚合时间把旧采集变新。 |
| G23-R03 | 高：新备份“新”的证明不足 | `verify_new_backup:145-150` 只拒绝 G18 原 path，继承 `verify_backups` 校验gzip字节/hash/长度与 receipt 标记。复制旧 gzip 到新目录再写新 receipt 仍可满足这些条件；affectedTables 只是声明，未连实际 affected dump表/列。 | 校验新的真实 dump command/capture证据、3 artifact角色/范围、affected原16+ledger实际内容，以及 writer exclusion <= proof/baseline/backup <= completion 的同一窗口关系。不能只拒绝相同hash，因为真实无变化数据可能相同。 |
| G23-R04 | 中：clone 部分汇总未连原始事实 | `verify_clone_pass:116-125` ledger 读 proof.exactNewRows 并对追加keys，但不连同目录已有 phase-ledger19.facts.txt；new17-empty仅读汇总；初始922/最终939全表身份 raw 文件仅被hash密封而未复算。 | 用现有 raw ledger/count/table-identities重算声明，错误汇总/缺raw不得继续；签字或PASS字符串不能替代事实一致性。 |

以上为已保留的首轮历史，均在 Owner 持续实现阶段发出，不代表最终版本仍有这些缺陷。最终停写复核与各项关闭依据见本文末尾；运行前置仍未满足。

## 实际离线探针

使用 `python -B` 在内存 import 真实 G23 driver，替换任何 subprocess.run/Popen 为立即抛错并给 transport_factory 失败哨兵。未创建临时目录或运行其测试文件（其测试会写自己的 prepared json，不在我写归属）。

- 真实 `prepare({'sourceDatabase':'ruoyi-vue-pro','clonePass':None})` 返回 blocked，factory/assertprocess 均零调用，PASS。
- 对真实 `verify_source_snapshot` 注入仅内存 read_json/envelope，完整 digest但 columns=['id'] 的旧表快照被接受，确认 G23-R01。该探针验证准备态漏洞，未伪造可执行 receipt，也未将它写入任何运行目录。
- 另外独立执行 5 组纯内存门禁：实际模板 missing clone 零client、fresh900边界（900秒允许/901秒与未来拒绝）、三种非零writer事实拒绝、仅PASS字符串且无artifact的clone拒绝、artifact路径逃逸拒绝。全部 PASS；run/Popen 均设置失败哨兵，无 actual flag、磁盘写或数据库调用。Owner仍写入中，这些边界将在最终版本确认，不能据此关闭R01–R04。

这里只证明源码/纯内存行为，不证明 clone 实际执行、源备份存在、source fresh25、实际无writer、源SQL成功或事务回滚。

## Root 执行前仍需独立确认

- 真实 clone 首次/重复验收产物必须存在并通过完整事实重算；当前缺失不能从22个或其它 offline fake pipeline PASS 推断满足。
- Root 必须确认源写已得到用户相应授权，且一次具体恢复方案/重跑范围另外 Review；当前工具无 source restore/自动重跑模式是正确边界，不能暗示无需恢复预案即可运行。
- writer 查询是瞬时检测。Root 需要真实服务/定时/外部SQL writer持续排除证据，持续覆盖 fresh采集、新备份、源升级、最终后置检查；工具不停止服务。
- 19个 DDL/seed/台账操作可能部分提交。首错后保留保护目录错误和 before/attempt证据，停止后续成功判断；不得说 ROLLBACK 回退全部 schema，也不得无授权重放或恢复源库。
- 本次源升级与 G22 的25审计动作/1新版质量登记是独立范围；即使源迁移终态 PASS，缺 GxP配置、权限、真实签名、UI流程验证等仍不等于应用就绪。

## 最终停写复核

Owner 明确停止 driver/tests 源码写入后，重新读取实际源码、输入合同、模板和离线结果，并在同一 driver SHA 上完成独立内存/AST复核。五个最终资产的原始 SHA 如下，源路径均位于 `doc/tasks/20261002-dcc-public-backend-completion/`。

| 文件 | 实际 SHA-256 |
| --- | --- |
| g23-source-upgrade-driver.py | 3afe3715a1fb00ef872ae01f3d3dc6a97fd70bfda1864bcbb80f25a7f3344ff5 |
| g23-source-upgrade-tests.py | ad917af0a3f9bc632e43f6fbe29c21edc9f028480022d8026b749f00c5e2cdaf |
| g23-source-upgrade-request-template.json | bc039479cb82e24a75f970efb3677931fe981181fb0df2281eb628e729075656 |
| g23-source-upgrade-input-contract.json | e5852e9601cfb2adc9bb82ecb4e150e85cbc98a856edd8d62f1de225f2aa000a |
| g23-source-upgrade-prepared-blocked.json | 97f60dcdcce82e0f92d560a420a94a281b824898eadc4a03b5f4fdc1db1bd03b |

| ID | 最终结论 | 停写版本实际依据 |
| --- | --- | --- |
| G23-R01 | 代码门禁修复已复核 | `upgrade:283-285` 的 source-before 调用已去掉 existing_columns 参数，真实 helper 重新读取 information_schema 全部有序原列，再把完整 snapshot 与 sealed baseline 比较；source-after 仅在该精确前置成功后按已确认原列比较。Owner `test_omitted_original_source_column_list_cannot_hide_payload` 覆盖漏列零写；我的纯内存实际 helper 得到 id/title/status全列，AST确认生产调用无caller列参数、比较在write之前。原 `verify_source_snapshot` 的形状校验仍可接受非空子集，但该子集不再能通过写前完整发现；这不是单靠准备态判断放行。 |
| G23-R02 | 代码门禁修复已复核 | `verify_source_prerequisites:187-207` 与正式 bundle external25集合精确匹配，绑定 capture receipt SHA及采集时间相等，分别从实际 raw facts调用 structure.validate_facts / BPM.validate并对 sealed proof。`prepare:252-259` 另检查writer<=proof/baseline/environment<=backupStart<=backupEnd。Owner测试重封facts/hash但改BPM executor仍拒绝、重盖group时间仍拒绝；只靠status/hash不能绕过。 |
| G23-R03 | 代码门禁修复已复核，真实备份仍缺 | `verify_new_backup:148-185` 要求新直接子目录、旧G18路径拒绝、每项dumpCommandReceipt保护SHA/目标/captureId/exit/time/artifactSHA及固定readonly命令scope；gzip实际展开并核对schema完整922身份、data210、affected17，绑定source baseline+writer receipt SHA，prepare校验同window顺序。输入合同明确这些应由Root真实采集，不是脚本凭空证明过去发生过备份。离线模型不替代实际备份/可恢复性验收。 |
| G23-R04 | 代码门禁修复已复核 | `verify_clone_pass:86-88` 从raw复核restored922；`:120-134` 对17个逐表raw零值、最终939表集合、raw ledger19行与proof/实际追加keys逐项对齐；33seed/history/schema既有重算保留。Owner完整clone fixture测试调用真实正式schema/helper，重新封存rawledger/count/table错误仍拒绝。测试fixture仅存在临时测试目录，不是本机runtime clone PASS。 |

实际读到 Owner `g23-source-upgrade-offline-final.log`：27 tests / OK（1.585s）；本 Agent 没有重跑该整套测试，因为它会写backend自有 prepared文件且使用fake实际flag流水线。Owner结果只能表示离线行为回归，本报告没有把它记成我的实际DB执行。

本 Agent 在最终 driver SHA 上独立执行 6 组纯内存/AST检查，全部 PASS：实际缺clone模板零client/process；fresh900边界；三类writer非零拒绝；无artifact的PASS-only clone拒绝；相对路径逃逸拒绝；真实完整旧列snapshot helper+生产首次调用/精确比较接线。测试期间run/Popen与client设置立即失败哨兵，不启actualflag，不写临时文件，不连接DB。读取前后driver SHA相同。本文结构/四项历史与关闭映射/五资产hash/运行边界的离线结构验证 PASS。

当前正式模板 `clonePass/sourceProof/newBackupReceipt/sourceBaseline/sourceEnvironment/writerExclusionReceipt` 仍为 null，实际 prepared仍是 `PREPARED_BLOCKED_MISSING_REAL_REHEARSAL_RECEIPT`，writeConnectionAllowed/databaseWritesExecuted false。技术输入合同不是采集器运行记录；真实clone首次/重复、fresh900新源窗口、新dump命令与载荷、source实际before/after、writer持续排除和用户源写授权应由Root完成后重新验证。

审查结论仅关闭 R01–R04 的工具实现缺口。没有 source restore/自动重跑/服务控制路径，DDL可能部分提交和首错保留均被准确表达；不得将工具准备完成、27个离线测试或此审查用于宣布源库已升级、可原子回滚或 application ready。G22 frozen工具与批准范围、G21候选清单未改。最终交付仅本文件，Root 收口时按需要把本文件纳入既有任务 Keep/候选清单；本 Agent 遵守本批只写此文件的归属，未修改其它任务记录。
