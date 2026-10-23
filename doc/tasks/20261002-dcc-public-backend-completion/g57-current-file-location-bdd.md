# G57 当前文件正式项目位置投影

状态：ready_for_closeout，Root真实检出/检入入口复现metadata缺folder；唯一BE owner授权Query+RespVO及必要当前H2/原G55定向测试。没有DB/SQL/浏览器/服务/Git写入。

- Given已授权当前File真实自有placement与同tenant/project/folder/storage身份一致，When正式详情投影，Then hasProjectStorageMapping为true、projectFolderId/Name来自该placement/folder；不从Master或目录名称推断。
- Given历史File无placement，且其directory无正式derived storage mapping，Then false、folderId/Name为空，即使DCC项目已绑定也不转为project location。
- Given当前directory已有正式findLeaf但File没有placement，或者placement/Folder/Mapping外tenant、错file/project/category/storage、已删/停用folder，Then明确数据错误，不把损坏当legacy false、无读时创建/修复。
- Given仅name metadata/列表，Then不借位置能力授予checkout或正文权限；本修只详情includeRouteSnapshots。现G55项目/产品身份、R02关系/current控源与G57培训资格保持。

新的required primitive hasProjectStorageMapping表示正式当前File自有存储位置事实，不表示项目存在或用户已获权限。前端Root helper只按服务端bool分支，保合法无logical placement历史physical入口。有效RED通过实际H2 File/project/folder/placement的正式getControlledFile projection复现folder丢失，再GREEN和坏事实/正常历史必要回归；缺测试端口依赖不当业务RED。


## 最终验证与边界

最终当前源码reactor九组284项全部0fail/error/skip，CLI0，2026-10-05 18:15:24，原九XML已逐字节归档g57-final-junit，两有效RED原XML分别保留。源码/target/Maven已冻结并交Root，详情数据来源、角色与原正文权限分离、真实隔离HMAC双签名/Flowable及官方通知同事务回滚通过。账户/密码/签名图片目录/Gxp及模板目录端口为明确隔离夹具，不冒充不同实际用户E2E。

现共用业务资格不增加bpm:task:query：消息直达管理详情仅依赖正式dcc query/approve、doc_control/category/hard资格；中心/工作台队列沿原controller bpm:task:query。测试独立文控88未配置该中心读权限，业务资格与正式通知仍合法，不自动grant或新增API。前端与真实运行包/页面验收由Root承担。
