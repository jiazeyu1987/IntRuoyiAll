# 受控导入正式关联集合缺失：只读审查

审查范围：受控导入入口→导入身份/权限→正式source ownership/master→关系集合→后续升版审批；不修改本链生产代码，不修补业务库数据。

## 结论

新受控导入应在原创建事务内建立有来源依据的空current关系集合，不能借普通BPM受控事件伪造审批轮次。修复挂在`DccControlledFileHistoricalImportService.create`中：正式file、source ownership、master latest/currentActive保存及平台投影之后，原创建事务返回前；初始化与平台投影同属placement事务，失败整体回滚。幂等重放应沿既有creation key提前返回，不重复初始化或审计。初始化失败必须连同file/master/placement/ownership及文件清理策略一起回滚。

## 为什么空集合有依据

`DccControlledFileHistoricalImportReqVO`只有目标项目、文件夹、类别、类型、编号、版本、幂等键、正文及附件，不接受关联文件选择。该入口明确为已受控来源登记，不产生上传BPM、审批签名或会签补充快照。只有真实完整CONTROLLED_IMPORT来源、准确tenant/master/file/project/type/category身份、正式source ownership、master latest/currentActive精确指向新文件，并且原固定关联、current关系均为空、无既有set时，才能建立明确的空集。

原固定关联若非空必须拒绝，不能把“本入口不提供选择”推成“任何历史数据都可以清空”。在新建事务内应防止异常fixture/来源伪造混入；既有数据修复还须排除原生round link、签名补充和已存在变更命令等证据，不能覆盖有效关系或让旧版本倒退覆盖master的最新集合。

## 不能直接复用recordControlled

`DccRelationRemediationService.recordControlled`要求非空真实applicationRound，调用`DccLatestControlledFileResolverImpl.assertControlledEvent`核对file.processInstanceId、controlledTime和`dcc_workflow_lifecycle_event`中准确CONTROLLED事件；普通入口由`DccRelationControlledEventConsumer`消费生命周期事件。受控导入没有BPM。合成IMPORT round、签名、生命周期事件来满足该契约会伪造事实，应保留原校验。

专门初始化入口可复用`DccRelationStore`既有tenant限定master锁、JdbcTemplate和GxP审计，不复制一套角色权限。权限仍由导入外层`requireImporter`（doc_control/import/edit）及`assertEditableProjectCategory`承担；内部初始化只供正式导入服务调用，不增控制器、维护菜单或读取时自动补建。

## 既有数据的正式迁移

需交付采用现有release-migration元数据的迁移产物，不在本轮E2E直接写库。迁移先列出资格与阻断清单；只对完整导入provenance、source ownership、同租户正式身份、仍为当前latest/ACTIVE、无关联证据且无set的记录执行，锁定master后复检，创建set并保留迁移批次/来源审计。所有非空、版本不一致或证据不完整记录应明确阻断并列出，不能默认空或回退指针。旧4587/4588失败证据保留；修复后用真实UI重新导入新任务数据完成整链，不修旧fixture来冒充通过。

## 建议回归

- 真实H2导入成功后set精确绑定tenant/master/file且无伪造BPM、签名、受控事件；重复幂等请求不重插、不重复审计。
- 关系初始化失败时创建全事务回滚，不能出现已受控文件但无set的半成品。
- tenant/master/ownership不匹配、非完整导入来源、原固定关联或current关系非空、已有set均拒绝。
- 用真实导入后的新文件走已有检出/检入→正式关联读取/审批预览，验证下游真正不再报CURRENT_SET_NOT_INITIALIZED；现有普通BPM受控事件契约保持原用例。

本报告是代码审查及实现建议，不作为审批/签名/发布/作废E2E通过证据。
