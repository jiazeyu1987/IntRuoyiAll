# G27 最新决定与代码 Review

状态：in_progress。用户再次答复“尚未批准”；3个缺失对象恢复不执行，审计规则也未获得实际质量批准。此前19项数据库升级已按真实授权完成，历史原列/原行摘要不变；不将新答复解释为撤销已完成的升级。

## 当前独立修复

唯一Java/SQL/Maven负责人继续历史名称占用核心和内部登记服务；独立审查负责人审查其实现及准备真实前端验收动作。主管理不与负责人并写生产文件、不并行运行Maven，不启动新服务或执行未获准写入。

已核对的核心组合日志：8类177项通过。进一步扩大受影响回归的556项出现20个旧测试交互断言失败，不能称扩大回归已通过；旧测试须准确区分只读源身份投影与占用写入，权限前置错误须修实际代码。

## 主管理新增Review要求

1. **权限顺序**：实际createRevision在hard assignment guard前读取已核验原名，未获授权用户可能先得到源证据缺失错误。要求将投影放到正式权限校验之后，负向测试仍准确ACCESS_DENIED及零身份读取/占用/正文复制，不能仅删除旧断言。
2. **保留期限审计**：旧NameClaim保持原值，真实20年保留期限改存sidecar后，现有状态审计只读旧claim导致retainUntil缺失。要求审计投影读取真实同Master已验证占用事实、明确混合旧/现代占用，审计失败与保留/作废在同一事务回滚。
3. **完整登记**：只传rawSHA不能证明实际facts、39项源bytes回执和用户历史口径。要求读取受保护证据的同一有界byte buffer，以strict JSON解析并核完整主键集合、真实结果/时间、记录原预像和当前存储定位。35MATCH/4缺失必须拒绝，不能生成VERIFIED。
4. **幂等/并发**：Master/claim锁后以current locking reads重新验证完整版本集合；重放对每行实际不可变字段重新比较，不只读取保存在列里的proof hash。GeneratedKeyHolder按明确id列读取，配置与正式审计同物理事务提交。
5. **正式审计覆盖**：真实新登记动作单独归属于legacy-name-occupancy.activate；原25动作没有其语义。覆盖parser只识别literal annotation，须用实际annotation及独立候选报告证明覆盖。旧25候选和质量依据保持历史封存，不冒称未登记新方法已有coverage或已获质量批准。

内部登记服务不新增REST、默认认证、job或业务审批节点。尚未实际登记。源代码和离线/H2测试仅是工程证据，后续运行库新增表、完整原件核验、真实质量批准与公共前端验收仍分别据实际证据核对。

## 保留的运行边界

- 实际39原件仍35MATCH、4NoSuchKey；4条引用对应3唯一对象，全部本地精确SHA候选已找齐，恢复被保留为未批准。
- 3对象恢复工具77项、bucket只读probe34项与Root授权边界3项离线验证完成；实际PUT为零。
- 20年保留期内原名/编号占用的核心已测试。保留历史Master唯一性后，20年到期公开NEW如何重新使用原编号仍有明确设计边界，不用人为创建第二Master的服务测试冒称公共流程完成。
- 真实E2E、最终源码构建、清理及本地int_qms合入尚未完成；整体目标保持进行中。

## G27 late validation updates

- Query240、Revision63、legacy occupancy39三类增量已通过，包含sidecar期限审计的有效RED→GREEN。
- 登记测试夹具先后暴露登录request、源ownership清理、ISO时间和六字段含null协议问题，失败保留而非业务PASS；修正fixture遵守严格输入，登记20项最新隔离测试已通过。最终缺版本/首次竞争及组合仍等待同源冻结证据。
- 原正式策略对新增登记operation的真实coverage门禁RED；独立候选34操作/12注解映射GREEN。正式策略和原25配置未改，候选没有获得质量批准。
- 新sidecar时间改DATETIME(6)，verifiedAt从真实UTC读取回执换算Asia/Shanghai并限定微秒；实际DDL首跑/重跑未获授权或执行。
- Root只读当前运行库MySQL8.0.40确认16KiB页、dynamic行格式、新3表缺席、新目标ledger0；这是前置事实，不是新迁移PASS。

## 最终候选规则和选择器增量

- Core17类579项已真实通过，含登记22、完整证据与正式Gxp kernel事务；原冻结源码37资产除下一批Query/测试指定变化外无意外漂移。
- 统一selector已按正式已验证evidence原名查询，candidate/count/page同一EXISTS条件，硬范围和名称权限先于投影，extension黑名单使用已核验原名的只读副本；增量5类324项通过，与579有重叠，不累加成903。
- 实际只读25个legacy Master最新受控指针全部NULL；六个缺正式元数据源版本controlledTime也NULL且非受控。不会将未受控旧记录猜成可选受控文件或回填最新指针。
- Root接受独立26配置候选（34总operation、12实际literalannotation），原33各字段完全不变，只追加历史名称登记operation。正式未批准策略文件SHA变661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894，正式路径实际coverageGate PASS，报告SHA3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d。
- 原G22的25项候选/coverage/影响资料保留，原未批准策略另保存g27-superseded-unapproved-25-policy.yaml；不复用其旧hash批准。用户25项条件写许可不扩大成26项，实际质量批准仍为尚未批准，运行库配置零写。

## 主应用打包的实际证据

唯一Maven负责人离线执行`mvn -o -pl yudao-server -am package -DskipTests`成功，21:29:00 exit0；skipTests仅此打包阶段使用，先前相关测试579/324已独立通过。新Jar507297396bytes/SHA81b56aa3e1c518ae8f989498a8624826a1ad946d8e2640e652e4cb3a32f30bc5。Root独立重算Jar/日志/41源码清单hash并实际核5个所需新class已装入DCC模块。原G20Jar单独保存，SDK旧依赖指纹来源未改。`g27-main-package-root-review.json`封存实际结论。未启动服务、未执行新DDL/配置/对象写入，未真实E2E或合入。
