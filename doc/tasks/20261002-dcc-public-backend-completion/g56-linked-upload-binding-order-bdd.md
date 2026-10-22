# G56 R03 新申请所选关联绑定时点

状态：ready_for_closeout。Root真实培训NEW上传选已有受控关联，在POST进入PENDING后绑定被DCC_RELATION_APPROVAL_SNAPSHOT_FROZEN拒；空关联曾跳过此边界。本owner唯一Java/Maven，仅改Workflow候选状态阶段与必要真实public/H2组合；Related冻结守卫不动，源/票据/签名/历史/幂等资格不降，FE/SQL真实库/服务/Git由Root管理。最终六组257执行全部0fail/error/skip，CLI0、2026-10-05 15:42:21，源码冻结，Maven已交Root。

- Given正常public NEW及准确受控关联，Whencontroller→同事务正式mapping/Workflow/真实Related绑定，Then新File无BPM未受控WORKING时冻结所选关联，然后进入pending并建BPM/属性/placement；真实完整关联快照保存一次。
- Givenlate ticket/placement/BPM异常，Then新File/Master/nameclaim/属性/位置/mapping/关联同数据库事务回滚，旧受控目标快照不变；不能吞冻结守卫当成功。
- Given相同submitkey/payload已提交，When重放，Then返回原File，不再新增关联或BPM；载荷漂移拒绝。
- Given创建WORKING或从选定工作正文升版，Then原create/checkin/candidate继承仍在其真实WORKING窗口做一次，新送审不二次绑定/删除旧草稿关系；审批历史目标仍不能写。
- GivenwithoutApproval合法调用，Then绑定只在新未BPM候选窗口，随后保持原READY_TO_PUBLISH/FINALIZING合同，不引新legacyfallback或并行状态机。

EffectiveRED必须公共真实controller/Workflow入口+实际H2Mapper与真实Related冻结服务复现，metadata/源ticket/账号/路线/BPM跨端口fixture明确，不以模拟返回成功代业务判断。相关旧freeze/idempotency/revision测试限受影响范围。本owner不执行实际DB/浏览器或修改历史。

## 实施与有效验证

只有Workflow生产源变更：正常提交与原withoutApproval合法调用显式插入WORKING新候选，真实所选关联、票据和附件绑定成功后转换为原pending或READY_TO_PUBLISH/FINALIZING状态，随后沿原流程创建BPM/属性快照。Related审批快照冻结守卫完全不改。原WORKING创建和RevisionService的工作候选继承不改；submitWorkingIteration没有新增关联绑定。所有更新保持原事务。

有效RED是旧Workflow经真实公共controller、正式mapping/placement、实际H2和Related冻结服务抛DCC_RELATION_APPROVAL_SNAPSHOT_FROZEN，1test/0failure/1error；原XML `g56-linked-upload-order-effective-red.xml` 已保留。前两轮导包编译、第三轮测试目录Mapper未与真实派生叶子一致是夹具错误，不算业务RED。

新增public组合五项实际通过：真实关联快照先于pending与真实隔离Flowable BPM创建；同submitkey返回原File且关联/票据/BPM各一次；真实票据markBound后异常回滚；真实BPM创建后placement审计端口异常回滚；原合法withoutApproval同租户受控source20/Master10升A/2路径绑定后保留FINALIZING。后一个只证明既有正式source调用，不冒充legacy NEW的默认租户插入或真实激活成功。

首组合的withoutApproval legacy NEW夹具没有生产租户插入拦截器，触发Master租户不符；只将测试改为现有正式同租户受控来源，不改生产租户守卫。首六组257的唯一failure为撤回重提原times(2)交互合同；最终测试严格检查三次更新的精确身份与内容：新901状态pending、901仅新BPM、旧900仅后继901，旧WITHDRAWN状态/proc-old不变。不是放宽次数或删除历史验证。

最终当前源码Maven reactor六组257全绿：public5、Workflow170、真实关联持久化/冻结继承15、生命周期事务14、日期工作台3、同目标返工50。日期/返工覆盖合理相邻AC25/26/27，不累加前轮重复执行。六个原始JUnit XML逐字节归档 `g56-linked-upload-order-junit/`；完整命令、rawlog SHA、XML/source/14编译类SHA与此前R02三资产0漂移证据在fingerprints/verification-receipt中。

## 验证界限与交接

H2与Flowable为实际隔离开发实例，同DataSource/TransactionManager，包含真实数据库回滚与实例回滚。人员、访问、物理文件客户端、路线和审计/激活跨端口显式夹具；未执行真实用户电子签名、NAS、页面或生产库。原Related十种冻结目标拒写与继承原快照测试仍通过。Root负责唯一同源打包、真实选关联+培训页面流程、后续检入/升版/作废验收与最终提交推送；本条不声称整个HTML已完成。
