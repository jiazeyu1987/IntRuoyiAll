# Execution Log：电子批记录表单预览缓存

BDD: 重复切换同一报表复用预览缓存 -> Given 用户已加载某个电子批记录报表预览 / When 切换到其他报表后再次点回同一报表且该报表未更新 / Then 前端复用已缓存的预览地址，不再重新请求预览路径。

BDD: 报表变更后预览缓存失效 -> Given 某个电子批记录报表已经有预览缓存 / When 用户执行会修改该报表模板数据的操作并再次查看预览 / Then 前端清理该报表缓存并重新请求最新预览路径。

RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前页面未声明预览缓存容器，也未断言缓存复用与失效逻辑。

GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-cache\frontend-feature-evidence.md` -> PASS
