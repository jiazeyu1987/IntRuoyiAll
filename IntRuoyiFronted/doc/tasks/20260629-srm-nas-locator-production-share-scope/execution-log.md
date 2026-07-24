BDD: 页面状态区展示双共享范围 -> Given 页面读取到最新 NAS定位 状态 / When 渲染状态卡片和提示条 / Then 用户必须同时看到 质量体系文件 与 生产部 两个共享范围。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> FAIL，旧页面文案未包含 `生产部`。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> PASS
GREEN: `nas-locator-real-flow.e2e.js` 等待条件修正 -> PASS，真实脚本不再把旧 `SUCCESS` 误判成新刷新完成。
