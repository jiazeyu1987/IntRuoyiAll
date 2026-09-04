# ADR-0001：在现有 DCC 主档上引入 Revision / Iteration 版本模型

## Status

Accepted。用户已批准 Revision/Iteration 业务规则，并授权剩余第一阶段口径按推荐方案收口。当前接受范围仅为测试环境；生产部署需要独立评审。

## Context

当前 DCC 使用 `dcc_controlled_file_master` 表示版本链，使用 `dcc_controlled_file` 表示一个业务版本，但逻辑文件身份仍由类别、目录、文件名和全局文件编码规则混合决定。`version_no` 是可输入字符串，解析器只理解数字段版本；检出/检入只在当前文件行写入或清除检出人，不会形成小版本。

用户已经批准采用 Windchill 式版本规则：逻辑文件以项目、分类叶子、文件编码唯一；字母代表大版本，数字代表小版本；检入产生小版本；从当前大版本的指定历史小版本创建下一大版本；只能提交最新工作小版本；审批和文控发布后才成为正式版本。

现有系统已经具备路线快照、电子签名、文件哈希、发布原子切换和组织级 `controlled_content_version_ref` 单正式版本约束，设计必须复用这些能力，不能另建平行生命周期注册表。

## Decision Drivers

- 逻辑文件身份必须稳定且由数据库阻止并发重复。
- 工作小版本必须完整保留，但不能全部占用平台开放审批候选。
- 检出是逻辑文件级独占编辑权，不能附着在任意历史版本行上。
- 现有审批、签名、分发、培训、受控浏览和发布关联不能大规模断裂。
- 迁移必须失败关闭，不能使用双写、旧格式 fallback 或自动猜测历史映射。
- 新模型需要支持发布失败时旧正式版本继续有效。

## Options

### Option 1：继续使用单一 `version_no` 字符串

只把展示格式从 `V1.0` 改成 `A/1`，继续由业务代码解析和比较。

优点是改动最小；缺点是无法可靠区分大版本、小版本、修订来源和直接前驱，也无法用数据库约束并发版本分配。

### Option 2：只给 `dcc_controlled_file` 增加字母和数字字段

不建立 Revision 聚合，只通过 Master 和 Iteration 行推导当前开放大版本、最新小版本和送审版本。

优点是少一张表；缺点是每个关键动作都需要扫描版本行推导大版本状态，难以原子约束单开放大版本和非最新来源选择。

### Option 3：保留 Master 和 Iteration，新增 Revision、Checkout 和版本事件

Master 表示稳定逻辑文件，新增 Revision 表表示大版本，现有 `dcc_controlled_file` 收敛为小版本；检出使用独立记录；平台生命周期只登记送审和正式小版本。

优点是语义清楚、能复用现有外键式关联，并可用唯一索引和事务锁表达核心不变量；缺点是需要高风险数据迁移和集中修改查询服务。

### Option 4：新建一套平行 DCC 版本表并长期双写

旧表继续服务当前功能，新表承载 Windchill 版本，逐步切流。

优点是短期切换看似容易；缺点是形成两个事实源，审批、签名和发布记录可能分别指向不同版本，违背无 fallback 和单一事实源要求。

## Decision

选择 Option 3。

具体决策如下：

1. `dcc_controlled_file_master` 继续作为逻辑文件，身份改为项目、分类叶子、规范化文件编码。
2. 新增 `dcc_controlled_file_revision`，保存大版本序列、展示代码、明确来源、最新小版本、送审小版本和发布小版本。
3. `dcc_controlled_file` 继续使用现有主键并成为不可变 Iteration，现有审批、签名、关联、分发和发布表继续绑定该 ID。
4. 新增 `dcc_controlled_file_checkout`，从 Iteration 行移除当前检出字段。
5. 新增不可变 `dcc_controlled_file_version_event`，承担工作版本动作审计和幂等回执。
6. 只有送审小版本才创建平台开放候选；纯工作小版本不创建 `controlled_content_version_ref`。
7. 发布继续通过平台生命周期服务原子切换旧 ACTIVE 与新 ACTIVE。
8. 采用维护窗口一次切换，不进行长期双写或旧版本格式兼容读取。
9. 用户选定的内容来源保存在 Revision；平台候选 source ref 仍指向提交时当前 ACTIVE 正式版本，两条来源链分别审计。
10. 迁移前签名中的旧版本标签和证据哈希保持不变；Iteration 单独保存 `legacy_version_no` 和迁移后版本身份。
11. 正式 Iteration 继续使用现有 DCC 的 `ACTIVE/SUPERSEDED` 状态，并保留培训中、待手工下发和发布失败状态；Revision/Iteration 拆分不重新命名成熟业务状态。
12. Revision code 使用确定性 Excel 式字母进位：1=A、26=Z、27=AA、28=AB、52=AZ、53=BA，不依赖运行配置。
13. 新增 `dcc_project_access_rule` 作为项目访问权威来源，访问级别为 OWNER、EDIT、VIEW。
14. 检入允许 CONTENT、METADATA 或 BOTH；metadata-only 必须有允许字段差异并复制独立源文件记录，空操作拒绝。
15. 只有项目 OWNER 可以从当前大版本选择历史小版本创建下一 Revision；选择非当前正式来源必须填写理由，不增加独立预审批。
16. 旧测试链只有在身份、版本、状态、正式指针和证据全部确定时，才按每个旧业务版本一个 Revision `/1` 自动映射；其他数据失败关闭。

## Consequences

正面影响：

- 逻辑文件、大版本、小版本和检出职责可以分别约束。
- 现有 `controlled_file_id` 关联基本保留，审批和签名不需要改绑到全新对象类型。
- 任意历史小版本作为下一大版本来源可以永久追溯。
- 普通用户正式版本与编制人员工作版本可以同时存在而不混淆。
- 数据库可以直接阻止重复身份、重复小版本、多个开放大版本和多个检出锁。
- Z、AA、AZ、BA 等边界版本不依赖人工配置，所有节点使用同一确定性算法。
- 项目权限有独立事实源，菜单和项目负责人文本不再承担业务授权。
- 元数据-only 修改既能形成历史，又不会伪造内容变化。

负面影响和成本：

- 主档身份、版本字符串、检出字段、浏览聚合和提交审批入口需要集中迁移。
- 当前按 `version_no` 字符串比较和按文件编号全局查找的代码必须移除。
- `controlled_content_version_ref` 需要按新的 submitted/released Iteration 重新对账。
- 迁移后不能直接回退到旧代码处理新 Revision/Iteration 数据。
- 需要新增项目访问规则配置页面和测试数据，旧项目负责人文本不会自动获得 OWNER。
- metadata-only 检入需要复制源文件记录，增加少量存储和哈希核验成本。

## Rejected Options

- 拒绝 Option 1，因为只改显示格式不能实现 Windchill 版本语义和并发约束。
- 拒绝 Option 2，因为缺少 Revision 聚合会让大版本状态和来源继续依赖运行时推断。
- 拒绝 Option 4，因为长期双写会产生两个事实源和不可证明的一致性。

## Migration Path

1. 完成测试库只读盘点，生成确定性 AUTO_MAP 候选和人工 blocker。
2. 部署仅增加表和列的 schema 迁移，不启用新写路径。
3. 在维护窗口冻结 DCC 受控文件写操作。
4. 按已确认映射回填 Project Access、Master、Revision、Iteration、Checkout 和平台生命周期引用。
5. 建立新唯一索引并移除旧身份索引、旧检出字段。
6. 部署只支持新模型的后端和前端。
7. 执行 postflight、定向回归和经授权的真实前端 E2E 后恢复写入口。

## Rollback and Revisit Conditions

- 新代码首次业务写入前，可以回退应用版本并保留未启用的新表。
- 新模型发生测试写入后，不允许直接回退到旧代码；只能向前修复，或从切换前测试数据库和任务文件快照恢复。
- 只读预检出现无法解释的多主档身份、多个正式版本、源文件缺失或平台生命周期漂移时，必须停止迁移并重新评审映射。
- 若后续证明确需同时开放多个大版本分支，应重新审议单开放 Revision 约束，不能直接删除唯一索引。

## Verification

- 数据库合同测试覆盖五个唯一性不变量和事务锁顺序。
- 服务测试覆盖 A/1 创建、A/2 检入、撤销不增版、A/x 到 B/1、非最新来源理由、最新小版本送审、驳回后新迭代和发布原子切换。
- 版本序列测试覆盖 Z -> AA、AA -> AB、AZ -> BA 以及双向解析。
- 权限测试覆盖 OWNER、EDIT、VIEW 与全局/类别权限交集。
- 检入测试覆盖 CONTENT、METADATA、BOTH 和无变化拒绝。
- 迁移测试覆盖确定性 AUTO_MAP 与整条 Master 失败关闭。
- 迁移 preflight/postflight 对同一快照执行，所有阻塞计数必须为零。
- 平台生命周期对账必须证明每个 Master 最多一个 ACTIVE、最多一个送审候选，并与 DCC 指针一致。
- 前端真实验收必须通过页面完成检出、检入、修订、送审和版本历史查看；API/数据库只做只读核验。

## References and Owners

- 产品合同：`docs/product/dcc-windchill-version-phase1-prd.md`。
- 数据设计：`docs/system/dcc-windchill-version-phase1-data-model.md`。
- 决策批准来源：当前会话中的用户业务口径确认。
- 接受范围：第一阶段测试环境设计。
- 生产技术评审责任：DCC 后端、前端、数据库和文控业务负责人；当前不进入生产。
