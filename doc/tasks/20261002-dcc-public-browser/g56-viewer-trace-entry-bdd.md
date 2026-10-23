# G56 F03 — 正文预览准确只读档案入口

Status: ready_for_closeout_for_Root_review。Root授权当前detail/index.vue的viewer toolbar给准确当前所选文件正式档案/版本历史trace入口。复用现buildControlledFileTraceabilityPath，非management编辑，不更改正文权限/角色或复制档案API。

Given正式所选历史/待生效ID且当前已成功读取同ID、同route，When正文预览点击档案/版本历史，Then现trace route保原LongID与returnTo，不混latestID、不management=1；该页原申请冻结/历史权限独立控制。Given未加载、ID变化或旧响应，Then不导航旧对象。

有效RED执行actualviewer入口handler和原toolbar新button Vue AST渲染，再GREEN及相邻detail/route文案回归。Root真实页面/完整types/build独立，Agent不环境/API/DB/browser/Git。

结果：实际专属2项RED全FAIL；初GREEN一项预期误写mode=browser而现正式helper是traceability=1，纠正测试合同不改helper后通过，保初日志。当前按钮准确所选LongfileID，同file/route已读取才导航，traceScope=trace/from=viewer/returnTo不加management或viewer；现只读详情/历史权限独立。最终本批90执行PASS、7prod lint0warning/exit0；真实旧版/待生效档案页由Root验，不把链接存在当全档案页面E2E。
