# 注册证精确清单保留证据删除

1. 本次授权只包含正式租户163的161张、测试租户169的165张证书。原始批准清单SHA256为`5942519b80ca65951f64d2e027906226486321adf044174b11c7639962739157`。租户删除不级联业务数据；不能登录已删除租户，也不恢复租户、不匹配产品、不删除剩余196张证书。

2. 删除契约为主档`deleted:0→1`、`row_version:+1`及处置人/时间变更；版本、快照、绑定、附件、旧审计和迁移账本不变。新审计为`CERTIFICATE_DISPOSAL_SUCCEEDED`，执行主体`SYSTEM_MAINTENANCE`/actor 0，记录清单SHA、原行版本、待生效版本和人类授权，明确`businessApprovalCreated=false`。这是运维处置，不创建审批或冒充业务审批。

3. 两服各4张证书有待生效版本302/308/319/325。保留版本状态和主档指针，不激活、不作废、不补审批。逻辑删除后的主档不再被激活/提醒查询选中。清单内未发现待审批变更、待上传流程或关联产品/注册办理任务引用；工具再次发现任何这类记录即阻断，不自动取消。

4. 根因和修复：`TenantServiceImpl.deleteTenant/deleteTenantList`只删除租户主档；租户安全过滤拒绝已删除租户。注册证控制器DELETE经权限、幂等、公司范围、行版本及待办检查后，只更新证书主档。旧MySQL触发器禁止正式主档删除标记，导致页面契约无法落库。现在删除审计在同一事务先写入，触发器要求其身份、版本和快照匹配；失败时一起回滚。

5. 新迁移`20261127_dcc_registration_certificate_retained_delete`只直接替代`20261127_dcc_registration_certificate_multi_products`。后者原封不动地替代`20261125_dcc_registration_certificate_products`和`20261127_dcc_registration_certificate_products_admission`。发布计划将三条旧迁移解析到最终新迁移，全部`SKIP_IMMUTABLE`；新迁移为`APPLY`。旧SQL和历史checksum没有重写。

6. 新迁移保留多证产品关系规则，不按租户删除状态豁免任何有效证书；只对主档未删除证书执行快照结构转换。已删除证书的原始快照与历史产品绑定保留。配套只读预检接受已核验旧/新主档触发器，其余未知状态阻断；持续数据库契约在每次发布后核验当前删除、快照和审计保护，不能以历史APPLIED替代。

7. 应用仓只修改Java、前向SQL/登记、当前数据库契约及本目录工具/验证。维护仓planner和远端`migration_plan`的连续替代、依赖解析由发布主会话单独修复和安装；本任务没有修改或安装维护仓。新包必须先通过该运行时对固定完整计划的校验。新提交需按一个新的北京时间发布tag重新构建、测试、正式发布，不能把41811b旧包验收作为新包验收。

## 固定输入

- 私有目录：`C:/SpugHandoff/release/private/test-prod-20261126/certificate-deletion/retained-delete-v2`。
- manifest SHA256：`6b48ce2e95b7e4c5d1170055925615bbfdc9025868c6cbc4fe9101d1a3b69f73`。
- 本机传输入口：同一父目录`run-retained-delete-v2.py`，SHA256 `e5d417eae1e95d8c2c279372ff000ba17015734ff50791f59986049b5373a224`。
- 两服已安装只读输入文件至`/root/intruoyi-certificate-retained-delete-20261127-v2`，权限0700/0600。尚未执行apply或停写。早期v1因远端Python3.6参数不支持而在只读阶段终止，禁止使用v1。
- 完整只读证据包含所有注册证表、所有范围外主档、附件元数据、infra_file、旧审计、租户、产品来源、下游引用和迁移账本。两服注册证专用文件表当前均为空；不会把“无专用附件行”解释为允许删除其他原始附件。工具没有文件删除操作，全部infra_file行保持不变。
- 每服`*-apply.private.sql`是可审查固定事务输入，SHA分别为正式`67fa5011a367f986614f4583dc581bcbfefe7324b2910675faba37450a5280b7`、测试`d8d403b6576907210705395fdb9b0b576800e8eac7605e60971a669c95f48a32`。不要直接运行该文件，入口还校验停止状态、触发器和持久化SQL会话身份。

## 执行顺序与命令

使用本机Python：`C:/Users/D01021/Documents/spug-handoff-work-20261030/runtime/Python312-3.12.14/python.exe`。

```powershell
$pythonRuntime = 'C:/Users/D01021/Documents/spug-handoff-work-20261030/runtime/Python312-3.12.14/python.exe'
$deletionRunner = 'C:/SpugHandoff/release/private/test-prod-20261126/certificate-deletion/run-retained-delete-v2.py'
& $pythonRuntime $deletionRunner plan --environment test
```

1. 发布主会话确认当前服务/包和固定容器ID，停止该环境写入：先前端后后端，并保持外部维护入口关闭。测试为`intruoyi-july-unified-58-frontend`、`brlab-prod57-nas-test58-20261020-r3-backend`；正式为`intruoyi-frontend`、`intruoyi-backend`。工具不会自动停止或启动服务。

2. 再运行同一`plan`，必须为`ORIGINAL`。所有基线行、范围外主档及证据须完全一致，已删除租户仍为deleted=1，逐主档行版本匹配；任何漂移阻断。`apply`还要求精确容器已停止、无其他活动SQL/事务，数据库容器ID一致。

```powershell
& $pythonRuntime $deletionRunner apply --environment test
& $pythonRuntime $deletionRunner verify --environment test
```

3. 执行器先在停写状态安装同新迁移完全一致的主档触发器，不写迁移账本；正式发布后由前向迁移重复安装并正式登记。事务内获取MySQL命名锁、SERIALIZABLE隔离与主档行锁，断言完整前证据、逐证写处置审计并更新主档，断言完整后证据后才COMMIT。前后证据、SQL会话和输出保存在远端`execution-test/attempt-*`。

4. `verify`必须为`COMPLETED`且原SQL连接消失。每个新增处置审计逐字段核验；清单外主档和原证据完全不变。之后重跑新迁移只读预检，构建并完成新包测试/验收，再按同一tag部署正式服。正式动作把参数改为`--environment prod`，不得改清单或基线。正式删除后的新迁移预检应PASS；删除前正式仍应因真实身份问题BLOCKED。

5. 恢复写入由发布主会话负责：保持维护入口，后端先启动并完成健康/新包契约验收，再启动前端，最后开放入口。新包完整验收未完成不能开入口。工具重复`apply`若精确状态已完成，只核验并返回`COMPLETED`，不重放。

## 失败续办

1. apply/resume任何错误、SSH断开或未知结果都保持停写。不得把客户端退出当作MySQL回滚，不能直接再apply。工具在发送事务写入之前，将真实connection ID、库身份、SQL SHA fsync至`session.json`。

2. 先`verify`。若连接还存在、数据库还有活动事务、证据/范围外行变化、部分删除、审计不配套或会话记录缺失，阻断并保留全部现场；由维护主会话定位，不自动kill、不猜测、不重建基线。缺失session意味着不能自动证明是否曾发SQL，须人工取证。

3. 若原SQL连接确已消失、无活动事务、全量证据仍精确`ORIGINAL`，可用`resume --environment test/prod`新建一次审计化attempt；若为精确`COMPLETED`，仅核验不执行。DDL触发器安装可能已生效而业务事务回滚，工具接受已核验的新触发器；其余保护必须仍与基线一致。

## 验证结果及边界

1. Maven注册证命令45项全部通过，包括保留证据、幂等、越租户/行版本冲突和待续证阻断。隔离MySQL8.0.39、network=none、无发布端口，使用两服完整只读证据副本，验证精确161/165删除、事务末尾失败完整回滚、缺审计/错误版本/同时改指针拒绝、正常页面审计通过、删除后不可恢复/物理删除、前向迁移与重复执行、历史快照不变、实际CLI会话fsync及重复不重放。

2. 两服真实只读plan均为`ORIGINAL`，没有远端证书写入；远端只安装私有工具文件。验证报告见`verification.json`。新发布包尚未构建或验收，不能将这些开发验证标为正式发布PASS。
