completed

# 正式服手动发布展厅

## 任务目标
- 在正式服更新后，通过真实后台路径执行一次“手动发布展厅”。
- 若发布失败，按根因修复，不引入 fallback、不吞异常、不绕过发布校验。

## 里程碑
1. 建立任务记录并读取正式服、发布、登录和 PowerShell 门禁。- 已完成
2. 使用正式服真实后台路径触发手动发布展厅。- 已完成
3. 根据发布错误定位并做最小修复。- 已完成
4. 复验手动发布成功并记录发布版本、releaseId、manifestHash 等证据。- 已完成
5. 收尾清理并提交任务记录。- 进行中

## 预期验证
- 正式服后台手动发布展厅成功。
- 后端日志无新的展厅发布失败异常。
- 发布状态、当前生效版本或 release 状态可读回。

## 经验门禁
- 正式服写入已由用户当前任务授权；操作范围仅限展厅手动发布及其必要根因修复。
- PowerShell 使用 UTF-8；不使用 &&；远程多行脚本用 UTF-8 base64。
- 发布链路不得用 mock、默认成功、静默跳过或自动降级掩盖失败。
- 不操作 /mnt/nas 根目录、挂载或 fstab，不清空共享盘。
- 真实 E2E 优先使用既有登录脚本/真实用户路径；接口只用于最终校验或前端无可自动化入口时的受控定位。

## 执行结果
- 官方登录前置通过：正式服 芋道源码/admin 进入 /showroom/company。
- 真实页面点击“手动发布展厅”成功，接口 /admin-api/showroom/release/publish 返回 code=0。
- releaseId：$(@{ok=True; httpStatus=200; response=; releaseId=20260705T034529Z-be276b74dfa8-a93b25a4d7bf; manifestHash=be164e56b41575837bc0c16d77bf593d7229e894501b43150f8e0cb39d85a05a; documentCount=196; assetCount=618; installBytes=747887446; pageHasSystemError=False; publishResponses=System.Object[]; events=System.Object[]; finalUrl=http://172.30.30.57:8081/showroom/company; screenshotPath=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260705-prod-manual-showroom-publish\prod-manual-publish-after.png; timestamp=2026-07-05T03:45:54.791Z}.releaseId)。
- manifestHash：$(@{ok=True; httpStatus=200; response=; releaseId=20260705T034529Z-be276b74dfa8-a93b25a4d7bf; manifestHash=be164e56b41575837bc0c16d77bf593d7229e894501b43150f8e0cb39d85a05a; documentCount=196; assetCount=618; installBytes=747887446; pageHasSystemError=False; publishResponses=System.Object[]; events=System.Object[]; finalUrl=http://172.30.30.57:8081/showroom/company; screenshotPath=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260705-prod-manual-showroom-publish\prod-manual-publish-after.png; timestamp=2026-07-05T03:45:54.791Z}.manifestHash)。
- documentCount：$(@{ok=True; httpStatus=200; response=; releaseId=20260705T034529Z-be276b74dfa8-a93b25a4d7bf; manifestHash=be164e56b41575837bc0c16d77bf593d7229e894501b43150f8e0cb39d85a05a; documentCount=196; assetCount=618; installBytes=747887446; pageHasSystemError=False; publishResponses=System.Object[]; events=System.Object[]; finalUrl=http://172.30.30.57:8081/showroom/company; screenshotPath=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260705-prod-manual-showroom-publish\prod-manual-publish-after.png; timestamp=2026-07-05T03:45:54.791Z}.documentCount)。
- assetCount：$(@{ok=True; httpStatus=200; response=; releaseId=20260705T034529Z-be276b74dfa8-a93b25a4d7bf; manifestHash=be164e56b41575837bc0c16d77bf593d7229e894501b43150f8e0cb39d85a05a; documentCount=196; assetCount=618; installBytes=747887446; pageHasSystemError=False; publishResponses=System.Object[]; events=System.Object[]; finalUrl=http://172.30.30.57:8081/showroom/company; screenshotPath=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260705-prod-manual-showroom-publish\prod-manual-publish-after.png; timestamp=2026-07-05T03:45:54.791Z}.assetCount)。
- installBytes：$(@{ok=True; httpStatus=200; response=; releaseId=20260705T034529Z-be276b74dfa8-a93b25a4d7bf; manifestHash=be164e56b41575837bc0c16d77bf593d7229e894501b43150f8e0cb39d85a05a; documentCount=196; assetCount=618; installBytes=747887446; pageHasSystemError=False; publishResponses=System.Object[]; events=System.Object[]; finalUrl=http://172.30.30.57:8081/showroom/company; screenshotPath=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260705-prod-manual-showroom-publish\prod-manual-publish-after.png; timestamp=2026-07-05T03:45:54.791Z}.installBytes)。
- pageHasSystemError：$(@{ok=True; httpStatus=200; response=; releaseId=20260705T034529Z-be276b74dfa8-a93b25a4d7bf; manifestHash=be164e56b41575837bc0c16d77bf593d7229e894501b43150f8e0cb39d85a05a; documentCount=196; assetCount=618; installBytes=747887446; pageHasSystemError=False; publishResponses=System.Object[]; events=System.Object[]; finalUrl=http://172.30.30.57:8081/showroom/company; screenshotPath=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260705-prod-manual-showroom-publish\prod-manual-publish-after.png; timestamp=2026-07-05T03:45:54.791Z}.pageHasSystemError)。

## 过程中修复的问题
- 本机 Playwright headless shell 启动报 ICU 错误，改用系统 Chrome 作为执行浏览器。
- 初始密码尝试失败，回到历史正式登录基线使用 dmin123 并通过官方登录前置。
- 临时脚本放在后端任务目录解析不到前端 Playwright 包，改为从前端 package.json 建立 createRequire。
- 页面确认框是 Element Plus 组件，不是浏览器原生 dialog，修正为点击页面内“确定/确认”按钮。
- 最终服务器核验首次脚本被 PowerShell 提前展开远端命令，已改用单引号脚本体和占位符替换后完成读回。

## 最终验证
- 服务器健康检查返回 UP。
- showroom_release 可读回本次 release。
- showroom_release_pointer 指向本次 release。
- 近 10 分钟后端日志未发现新的展厅发布失败错误。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，所有遇到的问题均按真实路径修正执行前置或自动化脚本。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep
- doc/tasks/20260705-prod-manual-showroom-publish/task.md
- doc/tasks/20260705-prod-manual-showroom-publish/execution-log.md

## 当前状态`n- 状态：已完成。`n- 文档校正：已修正首次 pointer 字段名误查造成的不准确表述。
- 最终结果：正式服手动发布展厅成功，当前 release 为 $(@{ok=True; httpStatus=200; response=; releaseId=20260705T034529Z-be276b74dfa8-a93b25a4d7bf; manifestHash=be164e56b41575837bc0c16d77bf593d7229e894501b43150f8e0cb39d85a05a; documentCount=196; assetCount=618; installBytes=747887446; pageHasSystemError=False; publishResponses=System.Object[]; events=System.Object[]; finalUrl=http://172.30.30.57:8081/showroom/company; screenshotPath=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260705-prod-manual-showroom-publish\prod-manual-publish-after.png; timestamp=2026-07-05T03:45:54.791Z}.releaseId)。