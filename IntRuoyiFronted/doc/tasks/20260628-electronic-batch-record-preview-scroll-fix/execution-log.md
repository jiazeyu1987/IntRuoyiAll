# Execution Log: 电子批记录表单预览滚动归位修复

BDD: 预览滚轮优先作用于报表区域 -> Given 用户已打开电子批记录表单预览 / When 鼠标悬停在报表区域内滚动滚轮 / Then 页面优先滚动当前报表预览容器，而不是最外层页面。
BDD: 预览滚动条可直接操作 -> Given 报表内容高度超过预览容器 / When 用户在右侧预览滚动条上拖动滚块 / Then 当前报表预览区域随之滚动，外层页面不抢占滚动。
RED: `node tests/e2e/batch-record-preview-toolbar.spec.js` -> FAIL，当前预览契约尚未要求 `iframe-shell` 承担滚动与滚动链路阻断。
GREEN: `node tests/e2e/batch-record-preview-toolbar.spec.js` -> PASS，预览 iframe 壳层已承担滚动并阻断滚动链路。
GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，右侧预览容器契约已同步更新。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-scroll-fix\bug-regression-evidence.md` -> PASS。
