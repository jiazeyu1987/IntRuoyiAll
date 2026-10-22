# 线程C：版本、检出与名称编号占用

任务id：20260930-dcc-c-version。Owner：C。优先级P1。G0共同基线、G1共享契约通过后才能实施；此前可设计纯版本/名称策略。

## 目标与边界

1. 检出互斥、检出人展示、本人检入/撤销和失败事务保护。
2. 检入只保存小版本A/1-1、A/1-2…；选定小版本再发起局部/换版，冻结选中正文。局部A/1→A/2、A/9→B/1；换版A/3→B/1。
3. 保存实际变更类型、来源受控版和选中小版本；所有版本详情和历史据此显示，不能只针对B/1或从字母推断。
4. 同名按完整源文件名含后缀、大小写精确一致跨项目判定；模板显示名和生成PDF名称不能代替。数据库唯一索引和服务比较一致。
5. 作废保留期内原名称及编号保持占用，原文件链自身版本可沿用；到期释放须按确认规则，不设置假默认保留天数。
6. A拥有受控/生效/作废事务；C提供版本分配、名称编号占用/保留策略服务及必要查询，不改Finalization或Obsolete大文件。

允许：ownership中C服务、QueryService、NameClaim及对应测试、独立Revision组件；公共DO/VO/主页面由主管理接入。QueryService若D需要新投影，由C按Review合同接入，不由两线程同时修改。

## 当前依据

- VersionPolicy使用斜杠段；默认A/1→A/2是小迭代，nextMinor不做9进位；不解析横杠小版本。
- QueryService.resolveCheckinVersion在检入时选MINOR/MAJOR，copyForCheckin仅保留NEW/REVISION及备注，尚无正式局部/换版区别。
- NameClaimService.normalize trim后转小写，claim用模板业务fileName；不符合源完整文件名精确比较。
- 独立作废服务目前立即release名称；编号按项目/分类身份解析，无完整保留期合同。保留期不是只删除release调用就完成。

## 里程碑

- C1：版本语法/变更意图、名称唯一键与占用服务合同G1Review。
- C2：纯策略及分配RED/GREEN，明确旧数据解析和历史展示边界。
- C3：检出检入、选中小版送审、占用并发/期限保护及查询投影回归。
- C4：交Revision组件、字段/接口/迁移差异及A调用说明供主管理Review。

## 必须记录的BDD

| 编号 | Given | When | Then |
|---|---|---|---|
| C-01 | 正式版A/1 | 连续修改检入 | 生成A/1-1、A/1-2，无正式替换或旧版作废 |
| C-02 | A/1及A/9下选中小版 | 局部变更 | 目标A/2和B/1，无A/10 |
| C-03 | A/3下选中小版 | 换版变更 | 目标B/1并保存REPLACEMENT事实 |
| C-04 | 两条路径均得到B/1及普通A/2 | 查看各版历史/详情 | 显示各自实际局部/换版类型，不按编号猜测 |
| C-05 | 甲已检出 | 乙检出或检入、甲重复请求 | 乙拒绝、甲幂等，UI有检出人事实 |
| C-06 | 检入源件未变化或上传失败 | 提交检入 | 不生成伪版本，锁与正文一致，错误可见 |
| C-07 | 较早小版被选中并送审 | 审批期间另改正文 | 送审内容保持冻结；规则未允许的修改拒绝 |
| C-08 | 已有SOP.pdf | 他项目上传SOP.pdf/sop.pdf/SOP.PDF/SOP.docx | 只有完全相同为同名；真实唯一约束一致 |
| C-09 | 作废文件尚在保留期 | 新文件链用原编号/名称 | 拒绝；原文件链后续版可沿用 |
| C-10 | 保留期满但同链当前版仍占用 | 执行释放 | 不能释放仍在用身份，不删除历史 |
| C-11 | 两请求并发创建相同名称或分配版本 | 提交 | 至多一个合法新身份/号，另一个明确冲突，不重复 |
| C-12 | 非法版本、错租户、无正式编辑资格 | 请求 | 明确拒绝，不兼容猜测或fallback版本 |

REPLACEMENT等仅是语义示例，正式枚举由G1统一；Z后规则、初始版范围、旧历史处理未明确时先设计，不改变历史行或引入猜测。

## 验证计划

IntRuoyiBackend现有测试：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileVersionPolicyTest,DccNewFileInitialVersionTest,DccControlledFileVersionNumberAllocationTest,DccCheckoutCheckinLifecycleTest,DccCheckoutFailureRollbackTest,DccControlledFileCheckoutContractTest,DccControlledFileNameClaimServiceTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

新增局部/换版、精确名称/期限/并发测试必须加入；已有测试存在不代表按新规则通过。名称索引DDL需静态迁移验证及授权后的实际MySQL大小写/索引测试，不能用H2默认排序推断MySQL行为。

IntRuoyiFronted：新增独立Revision组件与版本历史投影测试；已有browser/checkin-main-flow.spec.cjs和scripts/dcc-checkin-upload-state.test.mjs、dcc-checkin-cleanup.test.mjs作为回归候选，先核对入口约定与旧策略断言。不运行真实检出检入E2E，除非当轮明确授权。

## 交付与Review

输出版本语法与分配结果表、实际变更类型持久化合同、当前Query读写影响、源名称身份和索引方案、期限占用服务、A调用说明、原有历史处理边界、真实RED/GREEN报告。

主管理重点Review“检入小版本不等于正式升版”“同名不是不区分大小写”“保留期不等于永不释放或立即释放”、选小版身份冻结、事务失败后的锁和源文件清理。

## 可复制给线程C的指令

你负责线程C。阅读AGENTS.md、对应规则及docs/dcc-parallel-delivery任务包；未锁基线与G1前只设计。独立worktree内先BDD→RED→GREEN，只改C归属文件。不要改A的最终化/作废事务；提供正式服务让A调用。禁止硬编码未确认的保留天数或按版本号猜变更类型。公共DO/VO/页面由主管理统一接入。未授权时不提交推送、写库、运行服务或真实E2E；交最小改动及验证证据供Review。
