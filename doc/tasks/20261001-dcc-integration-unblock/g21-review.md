# G21 前置证明与执行准备

当前完整目标仍in_progress。上一目标turn有150测试/导航两缺口修复及同源构建收据，本turn实际准备严格执行工具、25真实只读前置证明及后续隔离验收；不是等待状态重复。数据库升级问题仍待用户答复，所有当前工具均未执行DDL/DML、restore或服务启动。

## 已实际验证

- 原通用发布planner按真实账本/45包生成40个APPLY，超出19范围；g21-whitelist-red.py有效失败保存真实额外ID。task新范围模块只允许19实际SQL、25外部依赖和44最小闭包，核对原始SHA/正式metadata/顺序/实际16原表+17新表、ledger追加边界及三份备份；8项强负向/正向测试PASS。
- detail提供17项结构合同：41表、562必要列、110索引及6原ledger事实；13离线负向测试PASS。Root真实7SELECT采1251事实，最终validator SATISFIED_READ_ONLY_FACTS_NOT_APPLIED，17项全部PASS。
- backend提供8项BPM/policy/template精确事实合同：Root真实22SELECT采264事实，最新26离线负向测试PASS。MySQL generated expression显示字面量escaped quote而protected DDL为常规quote，初次validator明确拒绝；仅规范同义文字分隔符，字段/状态/顺序仍严格，最新fresh v2 validator8项PASS。旧rejected log保留，不改schema或掩盖错误。
- 25项source database/server UUID一致，查询/事实/合同/validator/收据原hash合并g21-prerequisite-runtime-receipt.json；只证明当前所需事实，不声称无登记旧SQL已APPLIED。
- 19 first/repeat执行输入已在保护目录生成并固定SHA，操作IDdcc-g21-local-20261003-0610。首跑只追加本任务19ledger；repeat raw19脚本无ledger写。compose3项测试PASS；历史逐原列行hash支持5项测试PASS，能发现同count但旧正文/签名修改，33配置允许新增必须独立核验。

## 继续并行项

backend正准备仅任务隔离库driver，explicit授权旗标缺失时不写；detail正准备19项最终MySQL schema postflight合同，首跑/重跑同shape及generated/index不能靠同名即通过；Root保留总Review/捕获/执行权限。upload已经交精确Git候选/非任务排除/force-add路径，继续准备25审计配置范围，生产代码冻结。

后续实际交付：postflight精确26表/463列/85索引合同12tests及前置13回归PASS；driver初26test/prepare validate通过，独立Review R01（未全验复制字段）/R02（snapshot错误raw未保护）已实际复现并回派。最新driver复核全部info27/procdef10 V3复制字段及常量、RecordingMysql保每phase原读/精确stderr/hash；Owner与Root31test PASS，Rootvalidate准备输入PASS。独立Review final仍继续，不把31离线当MySQL演练。Root支持模块和25proof未改。

最终独立Review已关闭R01/R02（offline）：31测试及独立14次wrong copied-field/constants拒绝，真实冻结snapshot helper的三阶段raw/错误bytes/hash/不覆盖全部通过。Root再次31test/validate exit0、5工具asset指纹匹配，g21-verification-receipt.json封存prepared-only结论。当前所有数据库写入仍未获批，真实MySQL不填PASS。

source升级工具作为后续独立G23在准备：只能真实clone PASS+完整first/repeat/历史/33seed/19ledger/schema原始收据、source fresh25事实/新备份/原行hash/writer preflight和explicit本机写许可齐全后进入固定ruoyi-vue-pro；缺clone真实证明时只能输出blocked准备态，不能从假fixture生成准入证据。与25审计配置互不混入。

## 新核实的运行缺口

只读真实tenant1目前26个DCC audit operations只1个publish active，25新增业务动作缺正式策略。GxpAuditServiceImpl.append缺策略明确报错，这是原库配置缺口，会阻断项目/目录/引用/受控/生效；不能mock、吞异常或降级。新25配置须先给可Review payload/保留旧策略/只tenant1/首次重复与冲突验证，再单列用户授权；当前19SQL问题不默认包含它。

g21-runtime-plan.json固定slot6 8067/48067、禁止Flowable自动DDL、关闭全库startup sync及无关workers、内存Quartz独立名、正式确认Shanghai/7天；activation在真实正式任务页创建，每分钟配置，不在SQL偷偷启用。源runtime配置秘密仅从既有配置读取，本文和工具不导出其值。该JSON尚未实际启动，不宣称所有runtime前置已满足。

当前shell没有签名HMAC/keyVersion环境，应用资源也未配置；正式旧本机restart脚本包含既有本机测试key注入。启动slot6时只能在任务进程内读取该既有本机配置并按正式property注入，不将值写进参数/日志/本报告，不新生成key或覆盖旧签名。只读历史盘点：91条VALID使用dcc-hmac-v1，6条VALID使用另一历史测试key版本，242条HISTORICAL_UNBOUND未记key；这只是现存事实，不把未知历史key补成当前key，也不改历史签名。任务新签名仍须真实UI验签证明。

另外实际tenant1有25条旧active name claim；C正式加source_original_file_name保历史NULL，正式countUnresolvedNames门禁会阻止新上传。detail只读核对claim→Master→真实sourceFile/infra_file原名与哈希的证据，准备mapping/歧义列表，不以normalized/title/受控PDF推原完整名，不绕门禁，不夹带19SQL回填。数据核验完成才决定精确历史身份核对与授权范围。
