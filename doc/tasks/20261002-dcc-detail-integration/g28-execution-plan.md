# G28 新增3张sidecar表：单迁移准备与执行边界

当前 `prepared_for_root_review_not_authorized_not_executed`。本批仅自有g28工具/合同/离线测试/记录；不改生产Java/正式SQL/41源码seal/现有G21/G23helper，不执行Maven、真实DB、服务、对象恢复或Git。Root唯一真实数据库执行Owner。用户尚未批准缺正文恢复和质量审计策略；新增这1个DDL也没有具体授权。

## 精确部署范围

仅执行正式 `sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql` rawSHA `621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a`。3表70列、8个DATETIME(6)、2个完整varbinary1024 STORED生成键、6个secondary索引（另3PRIMARY）、7个enforced CHECK。完整metadata closure8；前7项只核实际事实，不重播旧19、seed/catalog或旧ledger。

新空表DDL与历史名称登记、缺失对象恢复、26项审计配置/质量签名各自独立。该工具不含历史名称INSERT、GXP策略配置、培训/受控/业务动作，更不替代E2E页面操作。

## 实际前置证据和剩余

`plan`离线重算Root既有SOURCE19成功回执/raw材料/19逐个完成marker及旧行保护证明，以及Root真实只读readiness原文件SHA：source ruoyi-vue-pro、UUID92ca05d0-aec8-11f1-a944-02b4e226a5ef、MySQL8.0.40、InnoDB16384pagesize、dynamic、3新表/该ledger均0。它们说明准备基础，**不能充当未来执行时fresh备份或特定新增DDL授权**。

Root首先具体审查/获得该1migration用户授权回执：status SPECIFIC_USER_DDL_AUTHORIZATION_RECEIVED；questionId/实际answer；migrationId+当前SQLSHA；allowedDatabases精确[已有任务clone,source]；max1ledger；保留历史；不允许借机名称登记、对象恢复、审计配置。旧19授权或CLIflag都不算此授权。

工具只接受SOURCE ruoyi-vue-pro或Root显式选择的**已存在** dcc_intqms_g18_rehearsal；不会CREATE/DROP/restore/rename clone。Root核槽位/归属/稳定writer窗口后，先该已存在隔离库首次/重复成功，再给SOURCE执行请求附全部实际clone journal/raw哈希证明。不能自动换一个新clone绕过已有部分DDL。

## 只读采集与exact schema合同

`g28-schema-capture.sql`全SELECT，包含当前runtime、**所有环境**该migrationledger、3表engine/charset/collation/rowFormat、70列ordinal/type/nullable/default/extra/charset/collation/precision、2生成表达式、index列完整顺序/noSUBPART/unique/visible/BTREE、CHECK语义/enforced。表都存在才单独运行`g28-schema-row-counts.sql`（全SELECT）证明3表全部空，避免缺表时静默尝试count。

preflight：全不存在且无任何环境ledger才FIRST_REQUIRED；任意partial、existing错shape、无/错hash/status/envledger拒绝；全部正确且合法ledger才REPEAT_ALLOWED。postflight全部70shape/8micro/2生成AST/7CHECK AST/9完整索引及empty严格验证。CHECK lexer保留字符串原大小写/下划线，只识别literal外charset introducer；不把LEGACY_GROUP改成LEGACY。生成cast(binary)和convert(using binary)按已有frozen G21 AST同义；其它函数不相同。

fresh 7前置必须绑定exact IDs及旧19ledger exactID/SHA/APPLIED/test/notdeleted；复用G21 post19完整schema实际facts/environment/validator原文件并重新执行validator，与正式collector query/tool/contract SHA及actualSELECT capture UUID/hash回执绑定，不能只给“7项满足”boolean。

## fresh backup / baseline / writer资料

`g28-request-template.json`目前全部必需freshproof为null且specificAuthorized=false。未来Root实际收集protected descriptors(path/rawsha/bytes)，固定DB/UUID、actualRead及捕获epoch。900秒仅技术freshness，不把它解释成用户授权过期。

需要原表完整ordered columns、exactHEX主键→每旧行hash和aggregate/count，固定7表：File、Master、claim、ownership、DCC signature、正式system_electronic_signature、infra_release_migration。保留全部old19账本行及其它原ledger，不删除/更新/补猜旧值。

fresh备份2gzip：同7表schema以及完整oldrows。每个实际dump命令有sealed回执、sourceUUID/DB/container/exit0/无凭据落盘/readonly固定profile、writer≤baseline≤dump开始≤结束≤备份完成；gzipSHA/bytes、uncompressedbytes、完整结束marker一致。schema精确7CREATE；rows精确7data markers且使用`--skip-extended-insert`单行INSERT，数量等baseline实际count。空gzip/漏表/漏行/路由USE/CREATE DATABASE/SOURCE/shell均拒绝。备份aggregate绑定实际baseline；该工具不运行mysqldump，不假造将来fresh证据。

真实执行临写前重读当前schema及transactions0/otherConnections0/enabledEvents0、旧19ledger exactpayload，重新快照保护表与preparedbaseline一一相等。无真实writer窗口即GUARD_FAILED_NO_DDL_REACHED/writeAttempted=false，不报告“已试写”。

## 精确首跑与重跑材料／失败边界

`g28-source-first.sql`：sameconnection envelope SELECT→正式新SQL**原始完整bytes**→仅1条plain INSERT新migrationledger→完成SELECT。repeat：sameconnection SELECT→同正式新SQLrawbytes→完成SELECT，ledger0DML。没有UPSERT/UPDATEchecksum、没有old19重播、没有额外guard存储过程/其他schema对象。Root固定CLI `--database`、新源库UUID即时只读校验，same-session envelope输出再核SQLSHA/实际DB/UUID/version/page/rowFormat与complete markers；没有--force、不吞error、不默认成功。

driver journal在protected新目录记录原baseline、每phase原materials/SQLSHA/rawstdout/stderr/actualexit、exactpostflight/newledger/fulloriginalAfter hashes。clonerun proof重新核phase materials**必须等于compose(phase,该真实operationId,该DB)**；不能拿SELECT1和自洽SHA/伪PASSlabelling代替本DDL。新ledgerreleaseTag/operation/file/hash/status/env/tenant/creator/updater/time/errorNULL精确，原旧rows保持且只有ledger新增1。repeat post/schema/payload全部与first一致。

MySQLDDL隐式commit；任意SQL/postflight/history/error失败即停止，保留现状和protected raw证据，不自动retry/restore/drop/cleanup、也不宣称事务ROLLBACK恢复DDL。Root另评估具体修复/恢复方案并核权限。孤立数据库真实first/repeat尚未运行，offlinefake pipeline不是MySQL语法或MVCC证明。

## 离线验证结果与命令

有效RED：新schema/driver不存在时测试明确FAIL；独立review实际复现falsecloneproof与CHECK字面LEGACY_GROUP被误归一，各RED后修复GREEN。当前自有16driver+11schema PASS；独立5adversarial PASS（对应owner独立报告）。fakepipeline覆盖真实函数first/repeat、首次SQLerror无repeat、postflight/旧行/错SQLSHA失败、临写writer漂移零writes、完整dump/空dump/漏row拒绝；与真实执行严格区分。

```powershell
python -B -X utf8 doc/tasks/20261002-dcc-detail-integration/g28-schema-test.py
python -B -X utf8 doc/tasks/20261002-dcc-detail-integration/g28-driver-test.py
python -B -X utf8 doc/tasks/20261002-dcc-detail-integration/g28-driver.py plan
```

Root未来满足具体授权+全部actualfreshproof后，才能由Root在新protectedrequest调用execute技术flag。当前禁止调用；不询问用户新权限（由Root统一具体询问），此交付仅ready_for_root_review。
