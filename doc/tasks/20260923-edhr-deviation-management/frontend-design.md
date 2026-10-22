# 偏差管理前端设计

## Purpose and Scope

实现偏差管理、表单处理和批记录追溯；交互合同详见 frontend-interaction.md。后端权威校验权限、签名、内容版本与批次阻断。

## Evidence Reviewed

- IntRuoyiFronted/src/components/UnifiedListTemplate/index.vue。
- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue。
- IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue。
- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue。
- IntRuoyiFronted/src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue。
- IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue。
- IntRuoyiFronted/src/views/mes/pro/production-release/PqcProductionReleasePage.vue。

## Pages and Routes

新增偏差管理列表/详情路由及菜单，列表Tab为全部/未处理/已处理。详情复用一份发起展示、一份可修订处理记录及签名审计区。

正式活跃表单详情通过共享 ActiveOrderSubmissionDetailPanel 增加主偏差Tab。显式宿主开关（建议 showDeviationTab）和 readOnly 决定位置/交互，不以 recordScope 自动允许写操作。历史 BatchExecutionTraceDrawer 和批次详情自己的 traceRecordDrawer 各增加只读偏差Tab；逐工序内嵌表单不重复插入主Tab。

正常 activeOrderId-only 详情由后端正式来源解析返回 batchExecutionId，再透传给偏差区；不得用前端猜测或仅显示缺ID错误作为实现结果。缺失/多义来源仍明确失败。普通订单/PQC/NCR共用组件不因这次扩展自动增加偏差菜单；既有独立不合格发起入口保留。

## Components

- DeviationListPage：标准列表和三Tab，后端分页，关闭方式和NCR状态分列。
- DeviationDetailPage：发起只读、唯一处理记录、签署/审计；没有第二份处理单。
- BatchDeviationPane：正式 batchExecutionId + readOnly，复用列表/详情展示。
- CriticalDeviationSelector：固定当前批记录的标准列表，可多选开放关键偏差。
- 既有签名对话框与受控附件预览：新增正式动作，不手写姓名/时间冒充签名。

组件名为建议；API字段、权限和行为以任务合同为准。

## State and Data Flow

1. 查询端返回ID字符串、业务编号、status、closeReason、contentVersion、有效签名、NCR ID/状态/处置、allowedActions和blockers。不同状态分别展示，不从偏差CLOSED推断批次可推进。
2. 切Tab保留显式查询条件、页码回1；重置清条件但保留Tab。保存/签署成功后回读服务端；旧请求不能覆盖新上下文。
3. 处理首次保存可建唯一记录，之后基于expectedVersion更新；内容改动要求原因，回读失效签名。签名新增不能改变内容版本。
4. 转NCR按钮允许关键偏差在处理尚未完成时直接发起；只能提交同批选中项，创建失败不关闭任何偏差，重放用同幂等键。
5. 未上市放行但未推送PQC的合法批记录仍可发起；审批资格不依赖PQC申请ID。
6. 关闭/上市放行后资料只读；关联评审处置结果仍随正式NCR读取，不能修改历史偏差签名内容。

## Error States

无query权限、缺/歧义身份、请求失败和正常空集合四种情况分别展示。正常空集合文案“没有偏差”。版本冲突保留未提交输入并提示重新加载；不得自动覆盖。认证失败清空口令，其他输入保留。终态、冻结或未满足签名条件显示可读原因及相关编号。

## Accessibility and Responsive Behavior

沿用Element Plus表单校验、焦点和键盘操作。窄屏表格可滚动，长文本/附件名换行；状态文字不只靠颜色。避免复制纸表合并单元格造成页面不可操作。

## Open Questions

首版工程取舍见PRD；没有以已确认规则为理由再次等待确认的步骤。候选归档导出不进入前端本次验收。

## Design Blockers

实现前核对各宿主最新代码及正式来源回传，特别是未推送PQC和只有activeOrderId入口。只读宿主不能因FORMAL_BATCH_SOURCE_DETAIL相同而开放写按钮。重叠未提交改动需协调归属，任务外改动保持原样。
