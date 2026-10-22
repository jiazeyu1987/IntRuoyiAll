# 运行包更新验证

运行交付 PASS，工程收尾受当轮禁止 Git/删除证据约束。

- 实际分支 int_qms；完整 `mvn -q -pl yudao-server -am -DskipTests -Dfile.encoding=UTF-8 package` 退出0，保留 testCompile；未重跑定向测试，独立核验 E2E24 GREEN XML 15/15 和冻结4源码、2class哈希。
- 不可变 Jar：output/runtime/int_qms/backend-e2e24-spreadsheet-20261020-030326.jar；SHA256 `ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab`。
- runtime-copy.json 逐项核验76class；既有74个class字节/哈希保持不变，新增两个转换class与E2E24冻结编译产物一致，全部与完整Jar内嵌模块一致。
- 原有配置/签名密钥仅内存复用；exact registry保留2条映射，SHA256 `eca016d3ab2ed4beb6f66e32d549d35ed47aef2513c27edf237064d9b36bde96`；schema自动更新关闭，无新凭据。
- 预检通过后仅停止30476/48061，启动27908/48061。初次40秒health轮询为NOT_READY，随后启动完成health UP，非业务失败。前端8061 HTTP200；8081/48081/8101/48101与前端监听快照一致。
- 真实 Playwright 登录芋道源码 / DCC241016C10Requester，页面身份DCC241016C10申请人，最终页面/user/profile。最新login-proof.json PASS、browserClosed=true；截图login-home-ready.png已人工视觉核对。首次截图登录已成功但仍有加载遮罩，原图和proof保留；一次补拍因多遮罩locator严格匹配失败，失败proof保留，改为等待全部遮罩消失后补拍PASS。均未执行业务动作。
- runtime-proof.json：READY_HEALTH_AND_REAL_UI_LOGIN_VERIFIED；businessCasesExecuted=false。验证Chrome已退出、Maven结束；用户Edge、历史stdin会话均未操作。
- 未复跑E2E10、未验证表格转换业务、未改写4270/4271原失败发布产物；转换业务交回E2E24。无Git修改、提交/推送、数据库/业务API写入、远程操作或子Agent。

只读清理preview见cleanup-preview.txt；禁止cleanup apply/删除证据，未标记completed。
