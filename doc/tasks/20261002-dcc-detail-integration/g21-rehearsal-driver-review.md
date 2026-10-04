# G21 rehearsal driver 独立只读 Review

2026-10-03；最新状态 offline_review_pass_r01_r02_runtime_pending。本 Agent 未手工修改被审 tools/生产/正式 SQL/Root 文件。未执行 actual driver authorize 模式、连接 DB、运行服务/Git/Maven/types/build/E2E。现有tests的setUpClass会确定性重写本身的prepared-inputs输出，运行后内容仍是同一正式prepare结果，不是本Agent另行改写合同。

R01/R02原缺口与旧源码指纹留作历史证据；最新修复已独立复核为closed_offline，详末尾复核节。R03备份快照与当前源库的证据边界继续有效。

## 目标与 BDD

- Given 没有技术写 flag、未冻结依赖、异库或已存在 clone，When 准备/校验/演练入口，Then 在对应门禁拒绝，不能触发原库迁移或复用/删除 clone。
- Given 同一新 clone 恢复 protected 922 schema / 210 data table dump，When 首跑/重跑19原始 SQL，Then scope16旧表/17新表、same-connection environment、原SHA、历史 payload、exact33 seed/linkage、new19 ledger/原6、schema postflight 与每phase raw evidence均明确验证。
- Given 历史行数相同但 payload 改变，或首跑 schema FAIL，When 后续管线，Then FAIL 且不进入 repeat，不删 clone 或覆盖证据；原库 current facts 与 backup snapshot 必须区分。
- 允许26现有离线tests、AST/static与内存negative实验；所有 fixture 管线 PASS 只证明控制流，不能称 MySQL 演练。

## 证据

实际运行：

| 命令 | 结果 | 证据边界 |
| --- | --- | --- |
| `python -B doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-tests.py` | 26/26 PASS，3.616s | 真实离线prepare/validator + mocked transport管线；未连接MySQL |
| `python -B g21_mysql_support_test.py`（main task） | 5/5 PASS | 历史同count但hash变、删除/多行/列投影变明确拒绝 |
| `python -B g21_execution_materials_test.py`（main task） | 3/3 PASS | 正式19原文SHA、first append-only ledger、repeat无ledger写 |
| 独立纯内存negative（下述R01/R02） | 两项复现 | 调用真实validator/helper，未调用driver授权执行入口 |

没有将fixture的`ISOLATED_REHEARSAL_PASS`称为真实演练。真实恢复/SQL执行/当前源库数据hash、writer屏障、运行页面均未验证。

## R01 新配置部分复制字段未验证，可接受同33但错误的payload

**P2，已复现validator覆盖缺口，交backend Owner；不能以当前seed33证明完整新配置payload精确。**

正式 `IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql:225` / `:234` / `:240` 把旧info的`form_id/category/icon/description/simple_model/sort/allow_cancel_running_process/allow_withdraw_task/process_id_rule/auto_approval_type/print_template_setting`等复制到V4。driver `expected_seed_facts:265` / `seed_queries:296`没有采集或断言这些全部字段；其中`form_id`与`print_template_setting`根本不在当前正式新行SELECT。deployment的DERIVED_FROM_/ROOT/ENGINE_VERSION_、procdef的若干复制字段、model REV_也同属当前不完整投影，不将它们假定已通过。

`snapshot_original_rows`收集新行所有**原表列**的hash，`compare_original_rows`仅核对原有主键对应旧hash及新增主键数量，没有新的目标payload hash。`validate_seed_rows`只对采集的部分字段逐dict匹配。repeat比较只能证明新行与first相同，不能证明first的遗漏字段正确。

纯内存实验使用真实expected/seed_queries/validate_seed_rows及冻结support.compare_original_rows：7张seed表保留全部旧主键/摘要；各新增主键准确33。为6个新增info底层payload赋不同的错误`form_id`与`print_template_setting`，其hash按错误payload计算，但SQL SELECT投影仍是既有expected字段；实际结果`validate_seed_rows`返回33，history检查通过，重复同一错误hash也满足first/repeat equality。实验没有模拟数据库执行或宣称该受保护dump实际含这些错误值；它证明当前validator会遗漏这种wrong-source/wrong-target值。

最小修复：由backend Owner补正式SQL所有确定性复制业务字段的目标断言；可在clone baseline独立采集旧V3完整明确投影及source事实合法性，再在新V4逐映射核对，明确只排除新ID/model/process身份、creator/updater和动态时间。需要当前源事实目标时同步Owner合同与新receipt；不要放宽精确比较或修改冻结Root支持模块。增加same33、samefirst/repeat但wrong form_id/print_template/REV/derived-link negative，且保持NULL与空串不同。

## R02 snapshot读取的raw与私有错误没有完整保留

**P2，源码和helper异常实验已核实；原始错误/每phase raw证据要求未全部满足。**

driver `rehearse:216` 与`:230`直接把clone传给`support.snapshot_original_rows`。Root支持模块 `g21_mysql_support.py:100` 内两类column/hash SELECT直接调用`mysql.read`，没有经过driver `protected_read:138`。因此baseline/first/repeat留了最终解析JSON，未留此部分SELECT原stdout及SQL receipt。其它identity/schema/seed/ledger/new-table SELECT与执行/restore阶段确实分别留raw，不能按这些存在推断snapshot原读也已留。

用纯内存read transport抛`MysqlFailure(1, b'REVIEW_ONLY exact hash-select failure')`，真实helper向外保留`private_error`，而`str(exc)`只有通用“Read-only MySQL command failed...”消息。driver outer `:246`写`str(exc)`，没有保存该`private_error`；它不会调用protected_read的`:147`保存分支，故真实snapshot SELECT错误的精确stderr会丢失。没有真实DB故障注入。

最小修复：不改冻结support，在driver给snapshot使用只读recording adapter（其read调用protected_read），按baseline/first/repeat + table + columns/rows唯一stem保存stdout、sqlSha、factsSha及私有stderr，禁止覆盖。新增模拟hash SELECT失败时具体private_error存在且repeat未发生，以及first/repeat两套raw完整且不可覆盖测试。

## 已有门禁通过及仍需真实环境的边界

| 审查项 | 源码与已跑证据 | 结论 |
| --- | --- | --- |
| 无flag不连接 | rehearse第一条require，现有no-authorization Mock零调用；prepare/validate将subprocess.run/Popen patch为Mock且零调用 | 离线门禁PASS；flag明确不是实际用户授权 |
| 固定新clone/原库保护 | SOURCE/CLONE固定；存在clone时create/restore前停止。source仅identity/existence/default SELECT和精确固定CREATE DATABASE；restore/19脚本仅clone transport | 控制流PASS，不自动复用/删clone/恢复原库 |
| restore922/210与16/17 | prepare验证3个backup哈希/gzip、922 CREATE TABLE唯一名/210 data section唯一名/新17未存在；restore后exact922名；SQL scope由正式hash44闭包确定16旧/17新 | 离线范围PASS；真正恢复的数据行/正确连接仍需实际MySQL |
| 原SQL及environment | manifest/contract/support/postflight依赖全hash，composer原raw bytes与prepared first/repeat重算相等；同连接environment SELECT前缀接原材料；输出environment与冻结facts完全相等 | 离线PASS；prefix不改正式SQL，真正同connection环境待runtime |
| first/repeat与历史 | before冻结完整原列，after只哈希原投影；旧ID/hash逐行，新增exact数量，repeat新行hash等于first；equal count changed payload测试拒绝 | 原历史保护逻辑PASS；R01是新payload目标遗漏，不混淆旧历史保护 |
| exact33/linkage/XML | 7seed表主键/tenant/identity/XML/hash/部分copied fields/template exact；不同tenant/editor XML/executor/params拒绝 | 部分精确合同PASS；R01未覆盖字段不能称完整新配置payloadPASS |
| new19/原6ledger | first普通INSERT19，无upsert；repeat原SQL不含ledger；id新增集合恰19、SHA/file/environment/status/op/release/deleted/tenant精确；整张原ledger所有旧rowhash保持，postflight再核原6SHA | 离线逻辑PASS，不伪造旧APPLIED或重写base/catalog |
| 首跑schemaFAIL不repeat | run_postflight需exit0+真实JSONstatus+clone/UUID；首跑任何异常跳outer，现有fixture证明没有repeat write | 离线控制流PASS；实际schema postflight须首跑及重跑分别PASS |
| 各phase raw | execute/restore stdout/stderr独立xb，不覆盖；seed/ledger/schema等protected_read分phase | 部分PASS；snapshot raw/error缺口见R02 |

## R03 原库current与backup snapshot只能分别验收

这是明确的验收边界，不指称driver越权写原库。fresh25 prerequisite proof绑定2026-10-03 05:49/06:02当前源库事实；clone从更早protected schema/data dumps恢复，snapshot-original-rows基线是**恢复后clone**。driver不比较当前源库的16表历史数据与backup，不在迁移前重新验证clone上的完整25前置；部分正式seed有内部守卫，postflight有结构/ledger守卫，不能当作全部前置数据重验。

因此未来`ISOLATED_REHEARSAL_PASS`最多证明该冻结backup在本次clone环境的迁移及指定合同；不证明当前源库业务数据已等价backup、不证明备份之后新增/变化的运行记录可迁移。Root计划真实源升级时必须独立冻结实际运行库/写者屏障、最新backup及完整前置、历史原列hash与目标冲突，不能复用旧clone成功替代。若本轮只批准clone演练，保留这个限制并不要求额外写源库或重播历史seed。

## 冻结源码指纹与交接

下列SHA256为本次审查raw bytes；变动后需重新跑受影响negative，不凭旧行号关闭问题。

| 文件 | SHA256 |
| --- | --- |
| backend own task/g21-rehearsal-driver.py | ef479cc2b44aa7355eb45d624c21cdc3531351e36b7a70ac0a8f07b588fd832a |
| backend own task/g21-rehearsal-contract.json | 4cadaff9b561a2e0a1ef5ddc83345b9552ebc2c5b9d6042d9b6cde750bd3a171 |
| backend own task/g21-rehearsal-prepared-inputs.json | 9928792f07877c20100d2e785065b2cbbd73b54dc8b14284ad2f00f52face532 |
| backend own task/g21-rehearsal-postflight-package.json | 6e165a1344778ef55bbd83699959f6f03592730727e879f2ec34d85d60f8d6fa |
| backend own task/g21-rehearsal-tests.py | d362e9eb006f67c3edcf41a8064e5efdded3f25781245c8228ef61e5c556eb34 |
| main task/g21_mysql_support.py | 6416a70ce24ea4fdec6abcbeb1f1a7746bd471533fff5c994b3c6ee08faf1331 |
| main task/g21_execution_materials.py | 9467c3ed5f3f49d22b14b14cdd988bc78790d76fd6bfef4009bc2ba18ce2a0f8 |
| main task/g21_migration_scope.py | 5fe9fe0bdcadec348a0f4ba3e4b0aab56efdc9df1b2f2fb6792b5382b754430d |

backend own task=`doc/tasks/20261002-dcc-public-backend-completion`（整合树）；main task=`doc/tasks/20261001-dcc-integration-unblock`（主管理树）。R01/R02已直接交Root和backend Owner；本报告不代Owner改工具，也不批准演练。建议先补两项覆盖并重新冻结driver/contract/prepared，再由Root最后完成具体数据库授权。

Owner已确认接受R01/R02：计划在恢复clone baseline读取6个exact V3 info/procdef完整业务copied投影的NULL/value+HEX digest，first/repeat绑定新V4 copy digest，保留正式constant/linkage守卫；不改冻结25proof及Root支持模块。snapshot使用RecordingMysql→protected_read。此消息是修复计划，不是已验证GREEN；本报告仍对应上表旧SHA。后续需复核Owner有效RED→GREEN和新指纹，且R03边界仍保留。

## R01/R02修复独立复核：closed_offline

Root指定新冻结driver与31 tests后再次复核；未另开风险、未改工具。实际命令 `python -B doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-tests.py` → **31/31 PASS，3.791s**。其中原26门禁仍通过、新5项覆盖完整copied投影、wrong copied digest、source6缺失/重复/tenant错、actual snapshot helper成功raw与私有错误；fixture仍非MySQL证据。

除Owner测试外，独立内存实验把`subprocess.run/Popen`全patch成立即raise，直接调用真实 `capture_copied_baseline/expected_seed_facts/validate_seed_rows/RecordingMysql`与冻结Root `snapshot_original_rows`，未调用driver authorize入口。结果：

- 手工按正式V4 SELECT逐字段核对，info完整27列、procdef完整10列和算法取值顺序精确相等。source是三key、两tenant的exact6 V3 native definition，capture检查id集合/tenant/64位hash/缺失重复；复制baseline在19 SQL之前，first/repeat目标同字段hash与baseline实际源摘要绑定。
- info27列：`model_type,category,icon,description,form_type,form_id,form_conf,form_fields,form_custom_create_path,form_custom_view_path,simple_model,visible,sort,start_user_ids,start_dept_ids,manager_user_ids,allow_cancel_running_process,allow_withdraw_task,process_id_rule,auto_approval_type,title_setting,summary_setting,process_before_trigger_setting,process_after_trigger_setting,task_before_trigger_setting,task_after_trigger_setting,print_template_setting`。
- procdef10列：`CATEGORY_,NAME_,KEY_,RESOURCE_NAME_,DGRM_RESOURCE_NAME_,DESCRIPTION_,HAS_START_FORM_KEY_,HAS_GRAPHICAL_NOTATION_,ENGINE_VERSION_,DERIVED_FROM_ROOT_`；旧`ID_`不混入相同复制hash，目标`DERIVED_FROM_`单独精确绑定V3 ID。
- 用独立NULL→N/value→V+UTF8 HEX编码计算源payload摘要，NULL与空串不同；target错`form_id`/`print_template_setting`/`DGRM_RESOURCE_NAME_`/`HAS_START_FORM_KEY_`导致同33、旧hash不变、first/repeat错误新行相同条件下**8次拒绝**。补model REV、procdef REV/DERIVED_VERSION、deployment derived/engine NULL、info creator等**6次fixed constant拒绝**，合计14次negative，不只是把hash随机改0。正常33投影通过。
- RecordingMysql用于真实冻结snapshot helper，baseline留columns+rows，first/repeat按原列留各自rows和read receipt。逐receipt事实文件SHA正确，repeat后所有baseline/first文件字节不变。重复same phase/table/sequence stem明确拒绝，已有first proof仍不变。
- 在成功columns读取后rows阶段抛真实冻结`MysqlFailure`，私有错误包含NULL与非UTF8字节；生成rows阶段的精确`.stderr.txt`与`.failure-receipt.json`，字节完全一致、stderr SHA正确，异常向外传递，没有仅保存通用消息。未真实注入MySQL故障。

修复后源码anchor：driver `RecordingMysql:133`、`protected_read:149`、baseline snapshot `:231`、baseline copied捕获`:234`、first/repeat snapshot`:246`、`copied_business_columns:281`、`copied_hash_sql:295`、`capture_copied_baseline:299`、`expected_seed_facts:315`、`seed_queries:347`、`verify_seed_additions:394`。官方V4 SQL及Root支持模块/25proof保持冻结未更改。

| 新复核文件 | SHA256 |
| --- | --- |
| backend own task/g21-rehearsal-driver.py | 86e9e8b5504f926dfd1d7ad529afe006393289ec811378232ded997c097e99a9 |
| backend own task/g21-rehearsal-tests.py | e29c15702fc5bf30f767aa517211b98c439cf9e0084a4cda01a1f91e411e1e79 |
| backend own task/g21-rehearsal-contract.json（未变） | 4cadaff9b561a2e0a1ef5ddc83345b9552ebc2c5b9d6042d9b6cde750bd3a171 |
| backend own task/g21-rehearsal-prepared-inputs.json（未变） | 9928792f07877c20100d2e785065b2cbbd73b54dc8b14284ad2f00f52face532 |

R01/R02在上述精确源码上closed_offline；不再把旧缺口当当前blocker。此复制hash证明**恢复backup的V3→新V4复制一致**，不证明当前源库所有表单/打印配置业务合法性，也不代表真实clone恢复/首跑/重跑已完成。Root可继续按本轮具体授权准备真实演练；R03边界及实际数据库/数据/ledger/schema验收仍由Root执行。
