# G56 首次升版整改读取 BDD

Status: ready_for_closeout — 唯一生产Policy及专属测试已冻结，原业务RED→11 GREEN→67/6实际定向回归全0；Maven已释放Root。无FE/Workflow/历史回填/schema/实际DB/服务/Git/package操作，真实E2E另待Root。

- Given 当前tenant启用leader有真实活跃REVISION/MATRIX_REVIEW任务，task-local obligation准确对应当前file/tenant/部门义务，尚未指派的义务round为NULL；When 调用正式arrangements GET service；Then 返回实际已存安排（允许空），原义务/file/task/安排/签名均不写入，不需先签名形成资格循环。
- Given 错actor/round/file/tenant、非REVISION定义、task-local obligation不匹配、已结束任务或停用账号；When 首次安排读取；Then 仍明确拒绝，不能用File原上传key/admin/任意NULLround绕过。
- Given 合法首次NULLround读取；When 调用原assertCanArrange写资格；Then 仍拒绝未签名指派写；新read路径不能变成未签名安排写入口。
- Given 既有已绑定历史participant；When 读取其原round；Then 保留原严格历史participant协议，不要求旧round仍有活跃task；其他round不借该证据。

测试使用真实任务自有H2 Mapper及Flowable活跃任务、正式BpmTaskServiceImpl.validateTask与DefinitionServiceImpl读取方法，已授权名称发现/人员目录为显式隔离端口。有效RED先在旧Policy跑首次NULLround正向真实拒绝，再仅改Policy完成GREEN；最终覆盖现有签名安排事务/关系权限/通知边界相关类，不跑全套。真实UI由Root后续验收，不由H2或mock姓名端口替代。

RED：旧Policy正式listArrangements抛DCC_RELATION_ARRANGEMENT_FORBIDDEN，CLI1、1test/0fail/1error，属于要修的业务拒绝而非setup/compile异常。原XML及log已保留。GREEN：11/1全0，CLI0，14:19:04；REGRESSION：67/6全0，CLI0，14:21:54，六XML永久归档，包含既有签名、关系保存/晚失败回滚和正式名称/正文权限读边界，轮次重叠不相加。具体命令及rawbytes/SHA见verification-receipt和fingerprints。
