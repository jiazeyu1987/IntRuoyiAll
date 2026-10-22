# G29 R2 生成列备份解析修复

状态：离线修复及真实已保留备份只读解析PASS；没有重采、DDL或原目录改写。

Root第一次实际只读采集的两个mysqldump进程都exit0，但旧备份计数只识别`INSERT INTO table VALUES`。MySQL对含生成列的表明确列出全部非生成列：因此41,615条File与27条claim被旧计数器记为0，G28严格备份校验正确阻止生成可执行请求。

新文件`g29-driver-r2.py`、`g29-collector-r2.py`明确复制冻结前版，只更改独立parser链接、备份列投影/计数验证以及R2plan输出文件名。没有动态monkeypatch旧模块或跳过原安全校验。所有旧G28/G29文件、seal、失败目录仍保持原字节；生产41资产、正式SQL、G21helpers也保持原字节。旧sealed工具只作为可复查历史来源，不再建议后续实际采集使用。

`g29-dump-parser.py`只识别固定`mysqldump --skip-extended-insert`语法，不执行SQL。先从七表实际schema dump解析ordered columns和generated列，并要求与本次baseline全部列顺序一致；INSERT显式列名单必须等于全部非生成列，不能包含生成列、重复列、错顺序或未知列。省略列名单仅允许无生成列表；按引用与反斜杠转义识别单行字面值，逗号、括号和类SQL文本出现在字符串内不影响行边界，value arity必须等于实际insertColumns。多行值组、截断语句、第二语句、未知literal格式明确拒绝。

R2 collector在实际读取baseline后保存其column集合；顺序dump schema→data。data实时计数使用严格parser，所以新的真实dump命令回执会包含准确count；R2 driver `validate_backup`在可执行请求生成前重新解析完整schema+data、核exact全表marker/count/end及原有完整证据门禁。旧失败command记录的0值没有被改成成功。

只读实际旧备份得到：File41615、Master36685、claim27、ownership43、DCCsignature339、system_electronic_signature0、ledger529，全部等Root当次baseline。File排除1个`c_version_key`；claim排除3个`active_unique_flag/source_name_key/number_key`。该结果仅证明保留文件的语义正确，不能冒称采集已经成功、freshness仍有效或迁移已演练。

验证：7 parser（含实际旧备份只读RED旧计数0→GREEN41615/27）、13 collectorR2（含生成列全流程fakeadapter、Windows原字节/真实readreceipt耦合、失败zeroReady）、16 driverR2（原exact1DDL/首跑重跑/backup/guard/partialfail边界）PASS。R2 `plan`只产生`g29-r2-*`新材料，不覆盖任何`g28-*`旧seal资产。Root审查后另建新protected目录重新只读采集；不自动重试旧失败目录。新增DDL具体授权、对象恢复及质量批准仍由Root处理，本Agent未执行任何实际连接/采集/备份/DDL。
