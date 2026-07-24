# Execution Log

BDD: 上线已验证发布包使用发布包选择器 -> Given 运维人员打开“上线已验证发布包”弹窗 / When 页面渲染发布包字段 / Then 字段必须使用与“部署发布包到测试服”相同的 NAS 发布包下拉选择器，而不是普通输入框。

BDD: 上线已验证发布包默认原因明确 -> Given 运维人员打开“上线已验证发布包”弹窗 / When 弹窗初始化 / Then 原因字段默认值必须为“默认发布”。

BDD: 发布包列表显示测试服使用状态 -> Given 页面已经加载当前测试服运行状态和历史成功的测试服部署记录 / When 运维人员展开发布包选择器 / Then 当前测试服正在使用的发布包应显示“当前测试服”，历史曾成功部署到测试服的发布包应显示“曾部署测试服”。

RED: `node tests\e2e\runtime-control-release-package-static.spec.js` -> FAIL，正式上线动作尚未被静态合同要求使用发布包下拉选择器、默认原因和测试服使用状态标识。

GREEN: `node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。

GREEN: `node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。

GREEN: Playwright 真实页面验证 `http://127.0.0.1:8081/infra/monitors/runtime-control` -> PASS，“上线已验证发布包”弹窗使用发布包选择器且默认原因为“默认发布”。
