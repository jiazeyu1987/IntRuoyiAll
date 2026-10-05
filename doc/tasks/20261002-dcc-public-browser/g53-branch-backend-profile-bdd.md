# G53 — 正式分支启动脚本单Spring profile参数

Status: ready_for_closeout_for_Root_review。Root实际本机启动已复现硬编码local与任务ExtraArgs dcc-local-development合并导致Quartz RAM/JDBC配置混用、No setter for isClustered；旧单dcc开发profile可启动。有限授权仅scripts/runtime/start-branch-backend.ps1、专属脚本合同和必要记录；本Agent不运行Java/Maven/Git/DB/服务/浏览器/环境配置。

Given正式分支launcher默认调用，When未提供新SpringProfile，Then仍准确local、Build/ExtraArgs/Slot与各branch端口规则原样。GivenRoot指定SpringProfile=dcc-local-development，When构造真实javaArgs，Then只由该正式参数生成这一个active-profile项，端口仍由当前branch的Resolve-BranchRuntimeContext决定，不能由SpringProfile或目录猜48061。Root从任务ExtraArgs去重复profile声明。

Given空白/空字符串/逗号多profile等无效值，When真实PowerShell绑定新参数，Then准确拒绝，不把它变local或静默trim。新参数追加末尾保旧位置参数语义，默认local保原；不额外变更Java/Quartz规则或任意ExtraArgs业务。

RED→GREEN执行生产launcher实际AST中的参数绑定和参数构造，移除实际dot-source/Java/exit外部端点并用显式repo/currentbranch/context/listener/jar宿主，防真实Git/服务；不是手写javaArgs镜像。现有DCC启动安全静态合同改断言local默认+正式可选profile，并保原直Java数组、无旧加密注入门禁。Root唯一修改g53调用并启动/健康/真实页面，离线参数PASS不冒后台已启动。

实际结果：PowerShell RED指定profile仍构造local，exit1；修复后默认local/指定dcc-local-development、原Slot/ExtraArgs/branch context端口、其他四branch默认local及实际参数绑定无效输入全部PASS/exit0。原DCC安全静态脚本exit0。首GREEN曾因测试catch不存在的ParameterBindingValidationException而退出1（测试宿主错误非业务失败），一次改真实ParameterBindingException和FQID核验后最终通过，保该日志。输出“Starting”来自被执行生产构造体的Write-Host，真实Java调用已被精确移除且有guard，不能称服务启动。

唯一生产新增可选参数置于末尾保持旧位置参数顺序，校验单profile `[A-Za-z0-9][A-Za-z0-9_-]*`；真实端口继续branch-runtime-profile authority。Root调用应传`-SpringProfile 'dcc-local-development'`，并删除任务ExtraArgs内`--spring.profiles.active`，其他实际task props照其审查方案。source/test冻结，Root运行实际后台的结果独立记录。
