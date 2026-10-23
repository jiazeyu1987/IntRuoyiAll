# C23 历史地址直接访问拒绝补证

范围：只补未验收直达历史地址的负向入口；六个原业务PASS及签名/培训/附件数据冻结，不重跑。不修改共享Query或其他产品文件。

Given 原A/1历史文件4233、B/1当前文件4247与现行三类账号不变；真实有权账号从正式历史页面生成A地址，NoAccess本人使用新独立context真实登录且无内容查看/下载资格。

When 在NoAccess浏览器打开上一步实际页面产生的完整历史地址，等待页面正式详情请求结束及稳定可见状态。

Then 页面明确拒绝，等待后拒绝仍可见；无A附件/培训事实、正文canvas/text或下载入口、无download事件；记录真实请求路径/状态作为辅助而不解析响应JSON。若有权正文泄露，保存产品RED并先交Root协调Query；若只是加载/暂态toast或网络故障，只记补证未完成。

原RED：六PASS结果negativeBoundary明确直达历史地址未取得稳定拒绝，不计该入口PASS。此次应分离“瞬时toast拒绝”和“持久拒绝”：先按真实DOM记录，不能扩大成功口径。

本轮真实RED：完整历史表的原生入口先生成viewer，再由查看详情生成只读地址；原路由beforeEnter不承认from=version-history的traceability读页，完整重新打开被送回browser，精确A详情请求0。原生viewer地址则实际请求A并显示cannot access，但toast消失后空页；记录10秒采样、0正文/附件/下载。两项均前端导航/拒绝展示缺陷，未发现内容授权泄露，不改Query。

最小方案：remaining.ts只补既有历史只读导航态准入，不赋予任何业务权限；index.vue复用已有accessExplanationError保留加载失败，在未取得fileDetail时显示不可关闭alert，成功或新读沿原reset清除。focused测试先真实RED（9项中3失败），修正后连同路由/迟到响应/正文详情分离邻近测试34项PASS。后端包ba0a8ead保持，真实页面补证仅验前端修复及原有拒绝，不声称13/22/24后端修复通过。

运行限制：本轮dispatch的ba0a8ead旧workflow-v6仅用于原有历史权限负例取证，不声称包含13/22/24的新修复；不Maven/类型检查/构建/服务/DB/Git/全租户job。独立时间戳trace，登录无trace，复用本例DPAPI凭据。
