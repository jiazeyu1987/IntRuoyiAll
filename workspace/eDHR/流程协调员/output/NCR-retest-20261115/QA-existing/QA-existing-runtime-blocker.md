# 不合格评审定向复测：运行前置阻塞回执

## 本轮范围与既有动作核对
- 委托账号yingtaigroupadmin，目标localhost:8081／测试1，只读核验评审117／BHGSP-202611-00000008／原活跃1009200609／签名64314。
- 上轮被重启中断前仅读取岗位规则及原QA117回执；两次内置浏览器调用均被另一会话占用而未执行，无业务写入、无新文件。
- 续做先确认QA-existing目录尚无输出；第三次浏览器调用仍未执行。没有重复此前业务处置。
- 后收到明确新增授权：检查重启后本地主工作区，仅启动缺少的前端8081和后端48081，不重建部署、不修改配置／数据库。

## 启动前核查与实际动作
- 当前仓库D:/ProjectPackage/IntRuoyi/IntRuoyiAll，git branch为int_main。读取当前AGENTS、docs/local-runtime.md和标准前后端启动脚本。
- native netstat确认8081及48081均未监听；前端vite依赖存在。
- python scripts/runtime/backend-package.py verify --root IntRuoyiBackend退出0，返回Backend package source and artifact verified。未构建或修改产物。
- 使用现有scripts/runtime/start-branch-frontend.ps1及start-branch-backend.ps1 -NoDebug，隐藏后台启动各一次，未改端口及配置。
- 原始运行日志目录（不作为已脱敏附件）：D:/ProjectPackage/IntRuoyi/IntRuoyiAll/output/runtime/ncr-retest-20261115-1228。保留服务所需文件，不清理正在使用日志。

## 已就绪与尚未就绪分开
- 前端日志：Starting int_main frontend on 8081, proxying backend 48081；Vite ready；netstat显示8081 LISTENING，PID39384。
- http://127.0.0.1:8081/实测HTTP200。此项仅证明前端服务可用。
- 后端标准脚本已发起一次，但观察时backend.out.log和backend.err.log均为空，没有java或python子进程，未出现Started YudaoServerApplication。
- http://127.0.0.1:48081/actuator/health实测拒绝连接：由于目标计算机积极拒绝，无法连接。 (127.0.0.1:48081)。后端未就绪，不能称前后端已恢复。
- 具体阻塞：标准脚本在包核验前依赖Get-NetTCPConnection端口查询；独立限时复核Get-NetTCPConnection -LocalPort 48081 -State Listen，输出BEGIN后5秒超时，未输出END。
- 首次合并检查中的Windows端口查询也曾10秒超时；已改用native netstat仅作独立证据，未用其绕过标准启动脚本。
- 按docs/local-runtime.md的Windows管理查询阻断标准启动规则，不绕过该检查、不自行修复系统管理服务、不另起第二个后端实例。已发起的标准后端启动器可能仍待查询返回，需先核实其状态后再安排重试。

## 真实页面证据
- 内置浏览器实际加载http://localhost:8081；初次页面初始化失败：GET /admin-api/system/dict-data/simple-list、/system/auth/get-permission-info返回500，AxiosError，系统初始化失败。
- 后续真实页面显示登录；租户选项残留测试2、用户名yingtaigroupadmin、密码为空；并非已验证的测试1登录。没有填凭据或提交登录，也没有切租户操作。
- 登录页GET /admin-api/system/tenant/simple-list及/system/tenant/get-by-website返回500；页面错误Request failed with status code 500。
- QA-existing-01-runtime-login-500.png保存实际登录页500，不包含密码、令牌或签名凭据。

## 复测结果边界
- 本轮尚未进入正常不合格评审或签名治理页面，未能确认117／64314仍保留或已清理。不能写不适用，也不能据此判定两项技术修复失败。
- QA签名上下文、当前订单／周期／记录／签署人匹配、完整签名核验、治理详情路径及只读限制全部未覆盖，原因是后端未就绪和登录初始化500。
- 旧回执仅作为目标定位和防重复依据，未把旧关闭／已校验结果当作本轮复测通过。
- 本轮没有新建、重置、签名、重新处置、改权限、接口／数据库写入；不影响其他业务端口，不另派岗位。

## 本轮UI观察
- 登录初始化失败仅呈现英文Request failed with status code 500，未明确解释后端不可用、租户选项尚未验证；建议中文标明依赖服务未就绪及重试说明。
- 错误状态仍展示残留测试2选项；用户容易误认为目标租户已加载成功，建议对租户加载失败与已确认选项作清晰区分。
- 不合格评审业务UI及签名治理UI因未进入，不能提供本轮修复后设计结论。

## 下一步
- 先由具备系统维护职责者核查Windows标准端口管理查询为何挂起及本次后端启动器状态；不重复启动健康服务、不改数据库、不绕过标准核验。后端health200／UP及真实登录可用后再续做原117只读复测。
