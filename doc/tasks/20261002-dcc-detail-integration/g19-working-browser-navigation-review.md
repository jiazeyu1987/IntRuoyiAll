# G19 详情→存储操作导航独立只读 Review

2026-10-03；整合树codex/20261001-dcc-integration。只读shared/working-browser-navigation.ts、detail/index.vue导航handler/按钮及browser/index.vue初始化/查询/选择/路由；未改任何生产或Root测试。仅运行Root新测试及stdin中的实际AST提取实验，无服务/数据库/Maven/types/build/Git/E2E。

## 已实际通过且有边界的结论

`node --test tests/unit/dcc-working-browser-navigation.test.cjs` 9/9 PASS。helper保持exact file/Master字符串，目录必须正式可被既有numeric存储树表示，unsafe number/超Long/空编号/缺Master或目录拒绝；detail普通按钮和returned-applicant handler都使用真实file projection造storage路由，所选较早WORKING先于ACTIVE默认；匹配必须option ID+row Master同时一致，missing option不被helper伪造。query同步保留workingFileId/workingMasterId，keep-alive project→storage query watcher存在。

actual getList提取实验另外确认：授权列表没有requested target时抛明确“未找到所选文件”并清list/total，不静默显示其它ACTIVE作为目标成功。

这些只是源码/转译handler证据。没有真实项目列表→详情→既有存储页的Playwright证据；菜单、列表服务器分页/权限、正文/检入上传及运行Jar仍由Root验收。

## 实际复现的缺口

### G19-NAV-01 [P2] 详情换文件等待期间仍可导航旧fileDetail

源码：detail/index.vue `openWorkingFileInBrowser`只捕获`fileDetail.value`并build route，没有核对`String(file.id) === controlledFileId.value`；普通按钮条件也是fileDetail存在。reloadAll/loadData在开始加载新route时不清旧fileDetail，旧值直到新detail成功才替换；若新读取拒绝，旧file仍保留。

只读真实AST实验：环境route.params.id='31'、controlledFileId='31'，旧fileDetail.id='21'/Master20/目录7仍在；实际openWorkingFileInBrowser执行，router收到workingFileId21，无错误。断言通过输出REPRODUCED。不是实际服务器写错对象，但新route页能把旧对象作为“当前操作”送到存储页。

最小修复：导航按钮/handler都校验当前路由file与正式detail projection相同，并与本次detailLoad成功context对应；等待/失败时禁导航且报明确未读取当前文件。将此真实handler实验升级为Root测试：route B + stale file A必须0push，不能从缓存造B的Master/目录。

### G19-NAV-02 [P2] 存储getList晚响应守卫遗漏所选file/Master/fullPath

源码：browser/index.vue getList的contextKey只包含`route.path/getBrowserCacheContext()/buildBrowserRequestParams()`；HTTP参数含目录/编号/page/status，但workingFileId/workingMasterId只属于导航，不在该key内。response到达后`resolveInitialSelectedVersionId`读取**当前**route.query；mark loaded使用旧requestRouteStateKey。

只读真实AST组合实验：实际getList+实际resolveInitialSelectedVersionId+真实resolveWorkingBrowserSelection helper；同目录7/keywordF/page参数不变，开始workingFile21/Master20查询；挂起时route.fullPath/query切workingFile22/Master20；旧响应含同chain ACTIVE41/WORKING21/WORKING22。getList将旧结果接受并selectedVersionId22，输出REPRODUCED。requestSequence只有下一次getList发起才改变；route watcher需等待restore初始路由/目录，故存在窗口，不能假定新查询必先发出。

最小修复：requestContext加入route.fullPath、browserMode及exact working identity/query，route变化同步失效list request；响应解析使用本次request捕获的working query并在写list/markLoaded前再核对。取消/离开storage/unmount同样失效。Root长期测试应覆盖相同HTTP参数但所选file或Master变化，旧响应必须被丢弃不写selected22，不是简单匹配当前options就成功。

## 源码推导待核对项（未称实际缺陷）

- resolveInitialSelectedVersionId对无目标的其它chain仍给ACTIVE默认，但getList末尾整体target-missing gate清列表。目标存在时其它行显示ACTIVE属正常；不能仅凭helper返回undefined误报fallback成功。
- `initializeStorageBrowser`只用成功后的storageBrowserInitialized标记，keep-alive首次storage及route fullPath watch可能同时恢复；此Review未复现双写，但是否重复请求/旧目录覆盖需Root根据导航实际组件时序测试。不修改其实现。
- helper安全拒绝大目录而不猜目录ID是明确边界；正式后端目录目前numeric树，超safe目录需单独系统类型迁移，不能本导航把其Number截断。
- getVersionOptions会滤掉unsafe numeric历史option，最终missing gate明确报错；没有看到前端通过Number截断工作文件身份取得其它option的证据。

## 下一步与验证边界

优先修NAV-01/02两个公开handler身份窗口；其余Root新9项成果保留，不重写helper/版本选择业务。修改者须先新增实际AST/SFC行为RED，重点route swap/new file读失败与same-params selected target晚响应。Root负责统一FE类型/build及真实页面，本Reviewer未运行其进程或改生产。
