# G56 — 正文侧栏正式项目与当前执行版本

Status: ready_for_closeout_for_Root_review。Root在本轮完整HTML核对期间授权本Agent唯一修shared/ControlledFileBasicInfoPanel.vue两处已确认缺口：项目链接文字仍取productName/productCode，当前受控版本标签实际取currentActiveVersionNo。Root主detail已G55修复，不重复修改；不新增latest字段或读接口。

Given正式projectName/projectCode与产品名/码不同，When实际formatDccProjectCodeLink及原侧栏project link渲染，Then只显示正式项目名/码并沿实际dccProjectCodeId导航，产品自身字段保持。Given历史未绑定，Then明确未绑定DCC项目代码，不借产品推断项目；绑定但缺正式名称/编码明确未记录。

Given新查看A/2受控待生效、currentActiveVersionNo=A/1，When侧栏执行版本字段显示，Then准确label“当前执行受控版本”和值A/1，不将当前执行号叫最新受控版或伪改为A/2。当前查看版本/待生效页标题保持其正式事实。

有效RED执行实际helper和原项目/产品/执行版本template AST编译Vue renderer，再最小GREEN及受影响有限回归/lint。范围仅1生产文件与必要测试，其他detail/upload/API/Root脚本/BE冻结。Root统一types/build/真实页面；本Agent不实际DB/API/browser/服务/Git/Maven。

实际RED4项1PASS/3FAIL/exit1（原产品编号字段已PASS；项目helper、实际link和执行标签旧行为失败）；GREEN与既有G55项目/路由/签名提示共4files15执行全部PASS/0fail/0skip/exit0、唯一源ESLint --max-warnings 0 exit0且日志空(session36480终态)。产品栏原文字及真实projectID导航保留，无bound字段时准确“项目名称及编码未记录”，unbound文字明确，不借产品。生产/测试冻结；Root全types/build与实际viewer验收另核。审查矩阵中其它入口/档案/进度缺口未改，不扩本批source。
