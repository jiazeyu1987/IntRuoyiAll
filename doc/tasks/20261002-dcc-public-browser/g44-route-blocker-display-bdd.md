# G44 上传审批前置人员提示

Status: ready_for_closeout_for_Root_review。限定归属：upload/index.vue 内 `dcc-upload-route-readiness` 的 blocker li 展示及专属离线渲染测试。本Agent不改API/业务权限/审批守卫/角色，不运行完整types/build、Maven、浏览器、DB或服务；Root负责运行页面和最终Review。共享task仍in_progress，本批独立交付不是完整目标完成。

实际触发：Root真实上传页routepreflight返回两个不同阶段/账号的blocker，message均为“审批人未配置有效签名图片”。正式VO同时提供stageNo/stageName/stageCode/userId/userName/reasonCode/message，但li只显示message，用户不能定位要处理的正式人员。

- Given 正式readiness含相同错误message的两条不同stage/user记录，When 上传页渲染，Then 两条各自显示服务返回的阶段名称/编号、人员名称和精确账号ID及原message，不合并或猜人。
- Given 人员/阶段信息部分缺失，When 渲染，Then 展示已知正式字段，并对缺失姓名/账号/阶段明确显示未记录，不用admin、阶段默认名或当前用户补齐。
- Given readiness返回全局error或ready=true，When 渲染，Then 原error优先和已就绪分支保持，不显示旧blockers或改变submit守卫。
- Given 返回内容含HTML字符或Long大ID字符串，When渲染，Then Vue文本转义且ID原样，不Number转换或HTML注入。

验证：专属Node test从实际完整SFC提取该真实template AST子树并用Vue compiler/renderer执行。Element Alert为离线宿主，readiness输入为正式VO结构fixture，不访问API，不mock业务成功；RED应因旧li缺stage/person/ID失败，GREEN须真实渲染显示身份，回归只跑受影响上传定向测试与owned lint。旧G20 seal保留，最终该singleasset新指纹另列。

实际结果：RED4项1PASS/3FAIL（stage/userID展示及缺值/转义新行为缺失），GREEN4PASS；受影响upload app/dept/render三文件27PASS，owned eslint最终exit0。先pnpm exec lint因当前运行时自动dependency检查要求TTY purge确认退出1，未进行获准安装/清理；复用已有eslint bin直接node检查通过，工具失败不记产品RED。原文件混合CRLF/LF，apply_patch局部改LF；仅还原本hunk三原有行及两新增行的CRLF后，反向替换3span为旧message重建原整个文件SHA ac6d4acceced7cbfd070302a89a6cf7cf6cc9d5242e64b6125963a0dc60c7169，证明无其它源码改变。初反向校验两次失败为换行定位工具问题，不记业务RED。
