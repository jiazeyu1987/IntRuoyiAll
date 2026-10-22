# 本轮追加身份边界：Root review 交接

## 实际复现与修复

READ-CONT-05 不是静态“越权”结论：新增实际生产 SFC 测试，隔离 HTTP/缓存/DOM 端口，在 metadata、bytes、summary 或 start 在途时修改缓存，或 active owner 改缓存后 tick。首次5条Node测试全部实际FAIL：

- actor变更后旧metadata仍继续读取body（1 vs 0）。
- actor变更后旧bytes仍发布文本（real text vs 空）。
- actor变更后仍发送旧owner heartbeat（1 vs 0）。
- actor变更后旧summary99秒仍被接受（对象 vs null）。
- visitTenant变更后Office仍emit opened（1 vs 0）。

前三项RED循环在首个actor场景即失败，不能说当时所有base/visit变体都独立跑出了RED。GREEN后同三项覆盖actor/baseTenant/visitTenant全部变体；另覆盖visit不变但base改变、token移除、storage通知、focus不自动复活、显式reload重新捕获身份、迟到start不使用新身份发送旧stop。

修复仅位于view/index.vue：正规远程source开始前捕获cache USER.user.id、baseTenantId、原始visitTenantId；同时要求当前access token存在，但不保存/打印token。isCurrentRenderVersion在所有异步发布边界比较实时缓存，变化后锁存失效、清掉正文/媒体URL/PDF/Office URL/水印/登记与阅读统计，显示明确错误；Office opened在emit前也校验。owner持有原身份，start/heartbeat/stop/summary发送前比对；身份变化不能把旧capability放到新actor/tenant请求里，关闭未确认如实显示/脱敏日志，不模拟停止成功。只有显式reload才能捕获新身份重新走服务器授权。

没有修改Query/token/Java/DO/Mapper/迁移、OnlyOffice子组件、其它owner文件或共享宿主。之前heartbeat endedAt与dispose错误修复仍保留。

## 当前实际验证

IntRuoyiFronted：

```powershell
pnpm exec node --test --test-name-pattern='changed cached|active owner invalidates|old summary after cached|Office opened after cached' tests/unit/dcc-preview-reading-viewer.test.cjs
pnpm exec node --test tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-api.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs
pnpm exec node ../doc/tasks/20261015-dcc-preview-reading-duration/check-viewer-reading-types.cjs
```

新增5条RED→5条GREEN；当前专属最终41/41PASS，0FAIL/skip。当前viewer ESLint0error/0warning；定向实际identity/reading AST + API/helper/context/source strict0diagnostics。类型脚本补了新身份块与外部缓存/认证端口声明，不是完整types/build，不冒充真实浏览器/缓存持久化/服务器授权实验。Root上一轮200/200与types/build仍是历史，未重跑。

## 交 Root 统一的旧宿主适配清单（未写这些文件）

新增生产进口：

- @/utils/auth：getAccessToken、getTenantId、getVisitTenantId。
- @/hooks/web/useCache：useCache().wsCache.get(CACHE_KEY.USER)，真实cache形状为 { user: { id } }。

需要Root统一维护的宿主：

- tests/unit/dcc-g09-viewer-tools.test.cjs：完整SFC require分派识别上述两个模块；非计时local blob路径不会读取身份，但模块初始化会建立cache句柄。模板显式导出列表若需要，加入readingCloseError以免旧宿主变量警告。
- tests/unit/dcc-full-format-viewer-contract.test.cjs：完整SFC require分派及正规远程样本的缓存actor/base/visit要存在，不通过移除身份校验来迎合fixture。
- tests/unit/dcc-g07-preview-registration.test.cjs：它只截取登记函数；loadPreview现引用previewIdentityInvalidated、previewIdentitySnapshot、readPreviewIdentity。保持其登记范围的显式宿主字段/端口，真实身份失效覆盖由本轮完整SFC测试承担。

这些是源码依赖适配清单，未自行运行它们以伪称当前跨owner回归PASS，也未修改其源。由Root合并适配后判断必要完整types/build验证。

## 边界

客户端失效检查发生在异步响应、ready、reading请求/tick、focus/pageshow及storage通知。浏览器不会为同窗口localStorage赋值自动派发storage事件；未声称单次裸缓存赋值能同步让所有页面重绘。若要全局同窗口同步身份通知，需要auth/cache owner定义通道，本线程没有越界改auth/store。

已经在旧身份下发出的网络动作无法由前端撤销；本修复丢弃旧响应和后续动作，不声称服务器因此零执行或已撤回持久化。身份变化后不会用新身份重试旧stop，旧会话末尾按服务器最后确认心跳/后续合法请求治理，不能伪造已关闭。

真实MySQL/BPM/Office/浏览器focus/iframe/缓存事件/退出网络送达仍未E2E；服务器权限最终仍由既有Guard/token重验。所有生产再次冻结交Root，指纹记录于round3-evidence.json。本轮不运行Maven/sharedtarget/fulltypes/build，不修改依赖，不做服务/DB/remote/Git/子Agent。
