# Bug Regression Evidence

## Bug Summary

DCC 顶部页签“受控浏览”在切到“文件上传”等其他页签后再切回，会重新执行首屏加载。用户期望红框内“文件上传”和“受控浏览”两个页签之间切换时不重复加载。

## Expected Behavior

“文件上传”和“受控浏览”都是正式菜单页签，打开后应进入 `keep-alive` 缓存；切回已打开页签不应重新挂载页面或重复首屏请求。

## Reproduction Command Or Path

- Static reproduction: `pnpm e2e:dcc:upload-browser-tab-cache:static`
- User path: 打开 `/dcc/controlled-file/browser`，切换到 `/dcc/controlled-file/upload`，再点击“受控浏览”顶部页签。

## Root Cause

待 RED 合同和代码修复后补充。

## Regression Test

待新增或更新静态合同。

## RED

待记录。

## GREEN

待记录。

## Risk And Regression Scope

风险集中在动态菜单路由元数据、TagsView 缓存和 DCC 两个正式菜单页签；不改变 DCC 受控浏览权限、数据查询、上传提交或审批链路。

## Blockers And Follow-Up

任务开始前已有大量无关脏改动和本地 ahead 状态，可能阻塞最终提交/推送。
