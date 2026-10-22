# 真实页面客户端导航空详情（待 Root 复核）

当前运行：runs/acceptance-2026-10-17T14-27-29-581Z，int_qms，8061/48061，Jar 1e8d1c791ed7f6342c29ec44a4d3d50523a8cb8cc172b505c702b6920167feb0。

Given 本例 C01 已由原主管真实签名驳回（签名802 VALID）；When 申请人由受控浏览精确文件行点击操作面板；Then 应加载该文件详情、驳回状态及检出/检入入口。实际60秒后仍为空标题和动作投影缺失提示。D01工作稿由浏览器行进入也复现空详情；当前尚未修改生产代码。

同一真实 URL 使用浏览器刷新后，页面自然 GET 返回 HTTP200，C01显示编号、已驳回、原主管签名802及检出/检入；D01刷新后能准备并实际提交申请。没有直接调用接口或解析响应JSON。C的刷新后可见快照包含原批准人DCC241016C10文控审核人。

本轮 session.json 操作 C-author-terminal-readback-and-D-draft / C-author-rejected-load-evidence / C-rejected-refresh-observation / D-initial-submit / D-refresh-draft-and-submit；截图 C-rejected-author-load.png 与 C-rejected-refresh-observation.png。主管C首次详情加载5秒断言也失败，但稍后正确加载，属短等待现象，不能与申请人60秒空详情直接归同因。

静态检查入口：IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue 的 reloadAll、loadData、isActiveControlledFileDetailRoute、isCurrentDetailLoad，以及末尾对route.fullPath/currentUserId的同步watch。失效读取保护存在，但客户端路由复用时的加载/上下文时序需 Root 安排定向复核；本记录不凭现象断定具体根因。最小修复候选文件为该详情组件及其对应读取上下文测试，未获得生产文件归属前本线程只留证。

矩阵分支继续经真实刷新后的正式界面办理；该导航缺陷另列，不能将其描述为已经修复。
