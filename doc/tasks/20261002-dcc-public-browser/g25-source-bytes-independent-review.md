# G25 source bytes 独立只读审查

状态 in_progress。Root 已获隔离首次/重复、成功后源19迁移的实际授权；G22 audit25+最多1登记仍以真实质量事实就绪为条件。用户提供 admin 账号，未提供实际批准时间/reference/签名依据，不能据账号或本次授权生成质量批准。

本批只读 Root main `g25_source_bytes.py` / tests、backend冻结 Java helper及真实 manifest，写自己 task 文档/负例。无 DB/GET/服务/Maven/build/Git；G22 frozen资产不改。

BDD: Given 实际冻结SDK extraction `{libs:[{name,jarEntry,sha256,bytes}]}`，When Root准备调用reader，Then 精确验证manifest SHA、每条相对filename/root和实际41jar/17class集合；不能用不匹配shape或classpath wildcard未seal文件。

BDD: Given sealed39 source version/file/SHA及当前源库，When比较对象字节，Then 当前version身份、sourceFileId、sourceSha256、原文件完整metadata/对象key应再次精确读取并比对；只有旧对象读出的SHA相等不能证明当前source绑定仍成立。

BDD: Given SDK stdout任意status/字段值，When保存proof，Then 所有状态值/类型/hash/errorCode均符合安全固定schema，不允许任意失败字符串或未知字段落盘，MATCH还需精确39/HTTP200/SHA/size/exit。

## 首轮具体发现

| ID | 结果 | 实际依据与风险 |
| --- | --- | --- |
| G25-R01 | 阻断 | Root main使用`extraction['libraries']`和item.path；真实manifest是libs/name/jarEntry，无libraries/path。准确交给Root，未执行GET。 |
| G25-R02 | 高 | fresh metadata SELECT仅runtime/config/infra_file；sealed version只取sourceFileId/SHA，不保留受控File/version/Master映射，也不再次核对当前版本sourceFileId/sourceSha。sourceSha当前漂移/改绑时仍按旧sha验证body并可MATCH。 |
| G25-R03 | 高 | verified_results非MATCH分支仅设all_match=False，对actualSha256/actualLength/http/errorCode可接受任意字符串；read.stdout随后原样落盘。实际SDK是safe allowlist，但wrapper仍要拒绝异常/被替换输出，不可声明任意输出都脱敏。 |
| G25-R04 | 中 | sourceAssets/classes/lib行缺准确root/path/file-set守护；libs wildcard加载所有jar，额外unsealed jar不在当前逐项hash循环。没有验证extraction rawhash==manifest.libsExtractionReceiptSha256。 |

Reader安全正向：冻结Java SHA c9291a59b708279c98c282b525e784db77cbf6dc02b68c8d25018cc07df474bb；actual config只stdin，StaticCredentialsProvider，无默认凭据；严格JSON/Long，loopback/pathstyle；只有GetObject，proxy禁用、retry none、transmission GET/host/scheme/port guard；64KiB流hash/length，body不持久化；SDK日志OFF，机器errorCode allowlist，异常message不反射。实际GET仅Root执行，backend50offline测试不是实际对象proof。

## 独立负例

见 `test_g25_source_bytes_independent.py`。仅import纯函数与自己的内存fixture，subprocess run/Popen 设失败哨兵，无actual config/凭据/对象key读取，不运行main或JavaSDK、不写Root文件。初轮失败记录待补，Root修复后在准确源码hash上复核。

## 新版批准SELECT边界

G22 policyVersion表只有approved_by/approved_at/reference/rawpolicyhash/coveragehash，没有signature外键。统一电子签名表提供actor/subject/version/contenthash和安全验证metadata，但签名的canonical_content_hash不必等于policy原始字节hash；不能猜它的module/action/subjectId或将admin所有历史签名关联到新版policy。本次只准备精确现有登记+可明确匹配新版本的签名候选metadata查询；候选仍需实际绑定与质量批准依据审核，不作为自动QA批准。
