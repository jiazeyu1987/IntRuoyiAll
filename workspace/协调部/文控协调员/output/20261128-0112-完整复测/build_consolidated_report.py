import collections
import datetime
import hashlib
import json
import pathlib
import re
import subprocess
import zipfile
import xml.etree.ElementTree as ET

from PIL import Image, ImageOps


root = pathlib.Path(__file__).resolve().parent
workspace = root.parent.parent
permission_root = root / '00-项目权限'
blocker = permission_root / 'zhaohaichen-补权重登刷新后受控浏览无项目.png'
rows = []
environment = '本地 http://localhost:8081，测试1租户，IDI项目，按压式球囊扩充压力泵。'
permission_steps = '王艺茹 wangyiru 登录后进入基础数据、项目代码，定位IDI，打开权限，核对赵海辰 zhaohaichen 的查看授权已启用且无日期限制。退出并以赵海辰重新登录，核对姓名和测试1，进入项目代码，再到文控中心、受控浏览、项目文件与引用，刷新项目列表。2026年11月28日05时29分仍没有IDI。'
block_note = '本项未执行。图片仅证明赵海辰的项目访问前置未满足，不是本项业务动作或下游结果的执行证据。当前指南要求十人项目权限确认齐全后才开始前六条主链。'


def add_permission(case_number, name, account, required, actual, screenshot, status='已通过'):
    rows.append({
        'caseId': f'00-{case_number:02d}', 'chain': '共同前置',
        'title': name + '的项目访问资格', 'account': account + '（' + name + '）',
        'object': 'IDI项目，未创建本轮受控文件',
        'expected': required,
        'actual': actual,
        'status': status,
        'reproduction': environment + '确认人：项目代码管理员王艺茹。' + permission_steps if status != '已通过' else environment + '2026年11月28日，项目代码管理员王艺茹核对。以' + account + '本人登录，核对测试1及姓名，' + ('文控中心、文件上传，项目选择IDI，观察带出的项目资料，不选择文件、不提交。' if case_number in [1, 2, 10] else '基础数据、项目代码，定位IDI，打开项目详情及关联文档，只查看不提交。') + '实际结果：' + actual,
        'screenshotPaths': [str(permission_root / screenshot)],
        'evidenceNote': '仅确认项目访问范围，不代表上传、审批、签名或完整主链通过。' if status == '已通过' else '已保存的查看授权与本人实际项目可见性不一致。原因尚未查明，不能认定为某项具体程序或角色缺陷。'
    })


add_permission(1, '陈佩佩', 'chenpeipei', '编制人能够选择需要编辑的IDI项目。', '本人登录文件上传页可选IDI并带出项目资料，未提交。核验时间04时30分02秒。', 'chenpeipei-本轮IDI编辑范围.png')
add_permission(2, '袁宏伟', 'yuanhongwei', '并发编辑测试人员能够选择IDI项目。', '本人登录文件上传页可选IDI并带出项目资料，未提交。核验时间04时38分04秒。', 'yuanhongwei-本轮IDI编辑范围.png')
add_permission(3, '潘鑫', 'panxin', '主管能够查看IDI项目，不必统一改为编辑权限。', '原项目负责人身份保留；本人可见IDI并打开详情及关联文档。核验时间04时48分01秒。', 'panxin-本轮IDI项目详情.png')
for case_number, name, account, time in [
    (4, '尚磊', 'shanglei', '04时50分51秒'),
    (5, '徐建海', 'xujianhai', '04时54分20秒'),
    (6, '周红欣', 'zhouhongxin', '04时57分33秒'),
    (7, '危家豪', 'weijiahao', '05时00分40秒'),
    (8, '朱利江', 'zhulijiang', '05时05分54秒'),
]:
    add_permission(case_number, name, account, '参与人员能够查看IDI项目，不统一扩大为编辑权限。', '本人可见IDI并打开详情及关联文档；未变更其原有效权限。核验时间' + time + '。', account + '-本轮IDI项目可见.png')
add_permission(9, '赵海辰', 'zhaohaichen', '文控人员的IDI项目查看授权保存后，本人重新登录应能看到项目。', '已新增本人IDI查看授权，05时21分31秒重新打开确认启用且无日期限制；本人重新登录后，项目代码仍无数据，受控浏览刷新后仍无项目。05时29分04秒核验未通过。', 'zhaohaichen-补权重登刷新后受控浏览无项目.png', '未通过')
rows[-1]['screenshotPaths'].insert(0, str(permission_root / 'zhaohaichen-VIEW保存后重开.png'))
add_permission(10, '张鑫悦', 'zhangxinyue', '现行下载方案下能够进入IDI编辑项目范围；具体文件下载另验。', '本人登录文件上传页可选IDI并带出项目资料，未提交。核验时间04时42分09秒。', 'zhangxinyue-本轮IDI编辑范围.png')


def blocked(chain, title, account, steps, expected, branch=''):
    sequence = sum(item['chain'] == chain for item in rows) + 1
    rows.append({
        'caseId': f'{chain}-{sequence:02d}', 'chain': chain, 'title': title,
        'account': account, 'object': 'IDI本轮' + (branch or chain) + '测试对象尚未建立，无文件编号、版本或审批轮次',
        'expected': expected,
        'actual': '因十人项目权限前置尚未满足，本项按指南跳过，未执行。赵海辰的查看授权已保存，但本人仍看不到IDI；本轮没有为本项新建或提交业务，不沿用旧轮次结论。',
        'status': '存疑',
        'reproduction': environment + permission_steps + '恢复条件：维护人员定位并解决保存授权与实际不可见的差异；项目代码管理员确认十人齐备。之后使用独立本轮软件测试材料，按以下路径复测：' + steps + '预期：' + expected,
        'screenshotPaths': [str(blocker)], 'evidenceNote': block_note,
    })


normal = '潘鑫主管审核；尚磊指派本人并另行实际会签，徐建海指派本人并另行实际会签，周红欣指派危家豪、危家豪实际会签；三部门全部完成后朱利江批准，赵海辰文控审核，共九次岗位签名。'
chain01 = [
    ('初版资料和两类名单分别核对', '陈佩佩 chenpeipei', '文控中心、文件上传，选择IDI目录类型，准备本轮初版正文及两个不同附件，检查属性、审批矩阵、会签名单及下发名单。', '项目和文件类型正确，会签及下发名单独立确认，不从旧名单推定。'),
    ('取消确认不产生申请', '陈佩佩 chenpeipei', '填完独立初版申请，选择文控审核后立即生效、不培训，在最终确认框选择取消，再查询本轮编号。', '取消不产生文件申请或待办。'),
    ('初版九次独立岗位签名', '各指定审批账号', '重新核对初版确认框并提交。' + normal, '每个岗位本人签名，指派不等于实际会签；批准人只批准，不另选负责人。'),
    ('初版受控盖章和各时间', '赵海辰 zhaohaichen', '完成初版文控审核后查看受控执行结果、实际生效时间及受控时间，再查日常列表和下发结果。', '实际生效与受控执行分别记录，受控并生效后盖章、下发。'),
    ('初版正文附件查看与下载', '张鑫悦 zhangxinyue', '在初版仍是当前有效版时搜索其编号，查看正文及附件，下载打开正文和每个附件，查看下载记录。', '每份正文附件对应初版，下载内容正确且有记录。'),
    ('本人检出和他人操作限制', '陈佩佩 chenpeipei、袁宏伟 yuanhongwei', '陈佩佩检出初版，查看锁定人及时间；袁宏伟尝试正常页面中的检出、检入和撤销他人检出。', '锁归陈佩佩；袁宏伟不能操作该锁，原有效版仍可授权阅读。'),
    ('本人撤销检出不改正文', '陈佩佩 chenpeipei', '陈佩佩在检入前撤销本人检出，记录时间及原因，再查看当前正文和版本历史。', '释放锁，不生成版本，不改变受控正文。'),
    ('再次检出修改形成工作草稿', '陈佩佩 chenpeipei', '再次检出，修改同名正文、上传两个对应附件和说明，全部保存后检入，检查状态和历史。', '只形成草稿，不显示内部小版本号，不替换当前版；全部保存成功才放锁，旧附件保留。'),
    ('维护入口下载与普通账号隔离', '陈佩佩 chenpeipei、张鑫悦 zhangxinyue', '本人检出后从维护入口下载未作废版本及附件，包括草稿和待生效版；普通账号从日常入口检查访问边界。', '检出维护资格不授予普通账号，作废件也不因此开放。'),
    ('上传保存失败不误报成功', '陈佩佩 chenpeipei', '仅遇真实且安全可观察的正文或附件保存失败时核对提示和锁状态，不人为破坏有效文件。', '失败不报成功、不提前放锁；没有安全样本则记未覆盖。'),
    ('局部变更完整重审并等待培训', '陈佩佩 chenpeipei、各指定审批账号', '从草稿发起局部变更，选择立即生效及培训。' + normal + '检查文控后、培训前的状态。', '文控签名记录实际生效时间，但培训未完成不得受控或切换旧版。'),
    ('编制人保存培训记录后切版', '陈佩佩 chenpeipei', '上传并保存标明本轮申请及版本的模拟培训记录，核对培训结果、受控盖章、切版和下发。', '培训只由编制人上传保存，不增加培训审核；培训完成后才受控切版，不等签收。'),
    ('局部版逐份下载及旧版归档', '张鑫悦 zhangxinyue、赵海辰 zhaohaichen', '在局部版仍为当前有效版时搜索、阅读、下载正文和所有附件并核对记录；赵海辰从归档检查旧版。', '局部版独立验收，不用最终换版替代；旧正文附件和审批历史保留。'),
    ('换版完整审批与三版历史', '陈佩佩 chenpeipei、各指定审批账号', '再次检出修改检入，从同一对象发起无需培训的换版。' + normal + '检查应有的初版、局部版、换版关系。', '版本按规则更新，新旧替代正确，同一编号保留三轮历史。'),
    ('换版正文附件下载与旧版保留', '张鑫悦 zhangxinyue、赵海辰 zhaohaichen', '换版当前有效期间逐份阅读下载正文附件及记录，文控核查两个旧版本归档、替代指向和历史。', '最终版内容正确，不覆盖旧附件、意见或签名。'),
]
for item in chain01:
    blocked('01', *item)

chain02 = [
    ('由有效文件形成延迟生效草稿', '陈佩佩 chenpeipei', '建立本轮独立有效IDI基线，再检出修改正文附件、检入，从草稿发起延迟生效的正式升版。', '选择延迟方式时不提前要求具体时间，旧版继续日常使用。'),
    ('完整审批及培训后给编制人确认', '各指定审批账号、陈佩佩 chenpeipei', normal + '完成所需模拟培训，再查看陈佩佩的延迟生效确认任务。', '审批及必要培训未完成不得提前确认受控。'),
    ('缺时间过去时间与未签名校验', '陈佩佩 chenpeipei', '在正常确认页分别留空时间、输入过去时间、未签名尝试继续，观察页面及业务状态。', '均不能继续，且不结束或改变本轮审批。'),
    ('确认说明及已审内容不可修改', '陈佩佩 chenpeipei', '查看确认页时间确定说明，检查已审正文附件及审批信息，再选未来时间本人签名确认。', '说明时间应按上下游衔接确定；不能修改已审核内容。'),
    ('计划确认受控执行时间分别记录', '陈佩佩 chenpeipei', '确认后读回计划时间、签名时间、受控时间、执行结果及实际生效记录。', '字段分别保存，受控时间取确认签名时间，执行结果不以无报错代替。'),
    ('到期前不下发不切版', '张鑫悦 zhangxinyue、赵海辰 zhaohaichen', '在所选时间之前查看盖章、状态、日常列表和分发签收任务。', '成功受控但未到期仍待生效，不下发、不生成签收、不替换旧版。'),
    ('到期自动启用和旧版作废', '张鑫悦 zhangxinyue、赵海辰 zhaohaichen', '记录实际观察时间，在到期后正常刷新查看自动切版、下发及旧版归档，不手工运行调度。', '到期自动启用、旧版作废，记录实际延迟，不自设时间容差。'),
    ('到期新版正文附件和替代历史', '张鑫悦 zhangxinyue、赵海辰 zhaohaichen', '到期后张鑫悦搜索阅读下载新版正文附件和记录，赵海辰核对旧版归档及新版指向。', '当前版、下载内容与历史替代一致。'),
    ('受控执行失败不切版', '相关岗位账号', '仅在真实且安全的受控失败样本中检查执行结果、下发及原有效版，不制造破坏性失败。', '失败不得标成功、下发或替换旧版；缺安全样本记未覆盖。'),
]
for item in chain02:
    blocked('02', *item)

for branch, name, account in [('03A', '主管', '潘鑫 panxin'), ('03C', '实际会签', '危家豪 weijiahao'), ('03D', '批准', '朱利江 zhulijiang'), ('03E', '文控', '赵海辰 zhaohaichen')]:
    for title, actor, steps, expected in [
        (name + '驳回的原因必填', account, '以独立本轮' + branch + '样本进入文件上传并推进到' + name + '节点，在正常页面留空驳回原因尝试继续。', '提示原因必填，本轮仍继续存在，不提前终止。'),
        (name + '签名驳回和旧待办取消', account, '填入本轮软件测试原因，由' + account + '本人签名驳回，查询待办、通知与历史。', '本轮结束，未完成待办取消，通知编制人，正文和旧意见签名保留。'),
        (name + '驳回后的原资料保留', '陈佩佩 chenpeipei', '从被驳回的' + branch + '申请进入回填写，核对类型、对象编号版本、填写值、原审批矩阵，按意见补正文附件。', '不误变新上传，不丢失资料；主动改类型才重新匹配矩阵。'),
        (name + '补正后从主管完整重审', '陈佩佩 chenpeipei、各指定审批账号', '原对象二次确认创建新轮次。' + normal + '完成本轮适用培训和生效条件。', '新轮从主管重新开始，不沿用旧签名、结论或培训；原有效版在切版前不变。'),
        (name + '重审终点下载和历史核对', '张鑫悦 zhangxinyue、赵海辰 zhaohaichen', '新轮受控生效后搜索阅读下载本轮正文附件和记录，按对象版本轮次查询驳回前后全部历史。', '正文附件正确，历史不被新轮覆盖；各页面审批节点、意见、签名对应一致。'),
    ]:
        blocked('03', title, actor, steps, expected, branch)

chain04 = [
    ('文控前本人合法撤回', '陈佩佩 chenpeipei', '以本轮独立上传或升版样本，在尚未进入文控时填写原因确认撤回，再查询任务与历史。', '终止本轮并取消待办，保留撤回人原因时间和原受控版。'),
    ('原对象修改后独立新轮次', '陈佩佩 chenpeipei、各指定审批账号', '修改原申请并二次确认重提，核对对象类型版本关系，再从主管按共同流程重审。', '原对象保留，新轮独立，不沿用旧签名或结论。'),
    ('已进文控未签名也不能撤回', '陈佩佩 chenpeipei', '将新轮推进到待文控，赵海辰尚未签名，检查陈佩佩正常页面的撤回入口或拒绝。', '进入文控即阻止撤回，不能以文控尚未签名作为允许条件。'),
    ('他人和结束轮次撤回限制', '非申请人账号、陈佩佩 chenpeipei', '分别对本轮待处理对象和已结束轮次检查正常页面的撤回边界；无入口不找隐藏按钮或接口。', '其他人和已结束申请不能撤回；仅无按钮不等于服务端拒绝已经实测。'),
    ('重提后受控生效并正确下载', '各指定审批账号、张鑫悦 zhangxinyue', normal + '完成适用培训和生效条件，搜索阅读下载新版正文附件与记录。', '新轮完整受控盖章并生效，下载内容与该版一致。'),
    ('撤回旧原因与轮次历史保留', '陈佩佩 chenpeipei、赵海辰 zhaohaichen', '新轮完成后重新打开旧轮次，核对原撤回原因、签名及正文附件。', '旧轮完整保留，不被新轮覆盖。'),
    ('作废申请允许阶段撤回', '陈佩佩 chenpeipei', '用独立有效样本发起作废，在主管、指派、实际会签、批准前的正常允许阶段验证本人撤回，重提仍为原对象作废。', '未批准作废前可合法撤回，原受控版保持有效。'),
    ('作废批准完成后不得撤回', '陈佩佩 chenpeipei', '作废流程批准完成后检查正常页面撤回操作及状态。', '已经完成作废不能撤回或重新恢复原有效版。'),
]
for item in chain04:
    blocked('04', *item)

chain05 = [
    ('独立有效样本与关联影响', '陈佩佩 chenpeipei', '建立本轮独立有效测试文件，核对正文附件版本，查看真实关联引用和受影响项目，填写作废原因并二次确认。', '对象版本正确；缺真实引用仅影响该子场景，不用空列表声称影响分析已测。'),
    ('第一轮撤回不损害原有效版', '陈佩佩 chenpeipei', '本轮独立作废申请在允许阶段由陈佩佩撤回，读回当前版和历史。', '原版仍有效，撤回轮次保留。'),
    ('重提主管驳回及原因校验', '潘鑫 panxin、陈佩佩 chenpeipei', '从原对象重提作废，在主管节点先空原因，再填测试原因本人签名驳回。', '空原因不通过，正式驳回取消待办但不作废原有效版。'),
    ('最后一轮独立八次签名作废', '各指定审批账号', '原对象再次重提，主管、三部门指派、三人实际会签、批准共八次签名，最后朱利江批准。', '批准前原版有效，批准后直接作废；不增加文控培训盖章或下发节点。'),
    ('作废后有效入口移除', '张鑫悦 zhangxinyue', '批准作废后从日常目录搜索编号和名称，再核对作废归档。', '不再显示为有效版，原关系版本保留并提示作废，不自动改动关联版本。'),
    ('文控归档查询及正文附件下载', '赵海辰 zhaohaichen', '在作废文件夹按项目名称编号筛选并分页，核对版本归属作废时间原因，打开并下载正文附件。', '合格文控可查下载，原印章及内容保持；无新版的主动作废不要求替代指向。'),
    ('全部作废轮次历史和下载记录', '赵海辰 zhaohaichen', '逐轮核对撤回、驳回、最终批准中的正文附件、意见签名及下载记录。', '三个过程都可追溯，历史不丢失；不能只看当前状态。'),
    ('普通人员不可越权查看作废件', '张鑫悦 zhangxinyue及非文控账号', '从普通浏览、历史、有效版维护入口和直接链接检查已作废正文及下载限制。', '非文控不能浏览下载作废件，不因编辑或检出资格绕过。'),
    ('作废件不可编辑删除恢复', '陈佩佩 chenpeipei、相关账号', '在正常页面检查作废文件的检出、修改、删除和恢复入口及状态。', '不得检出修改删除或恢复作废件，不走隐藏接口强行测试。'),
    ('实际会签驳回作废分支', '危家豪 weijiahao、陈佩佩 chenpeipei', '独立作废分支推进到危家豪实际会签，签名驳回并核对原有效版，再从原对象补正完整八签。', '驳回不损害有效版，重提仍为作废；历史完整，最终按批准结果作废。'),
    ('批准驳回作废分支', '朱利江 zhulijiang、陈佩佩 chenpeipei', '独立作废分支推进到批准，签名驳回并核对原有效版，再从原对象补正完整八签。', '批准驳回不使原版作废，不增加文控驳回步骤。'),
]
for item in chain05:
    blocked('05', *item)

chain06 = [
    ('下发三部门单独确认', '陈佩佩 chenpeipei', '共用本轮01对象，在申请中独立选择生产制造中心、质量体系中心、新品开发部为下发部门。', '不从会签名单或历史七人名单推定下发正确。'),
    ('受控生效后只发合法负责人', '赵海辰 zhaohaichen', '文件同时受控盖章且生效后查看发送结果、任务接收人；并与待生效时状态比较。', '派给尚磊徐建海周红欣，不自动发部门全员；待生效不建签收。'),
    ('尚磊和徐建海本人签收', '尚磊 shanglei、徐建海 xujianhai', '分别登录本人签收任务，查看本版内容后本人签名，再读回签收时间和状态。', '实际本人签收可追溯，阅读不算签收，也不是质量批准。'),
    ('签收任务重新指定危家豪', '周红欣 zhouhongxin', '在本轮签收任务内单独指定危家豪并保存，核对人时间版本及待签状态。', '会签时的指派不能替代本次签收指定；指定后尚未签名仍待签。'),
    ('危家豪实际签收完成', '危家豪 weijiahao', '登录危家豪查看获派签收任务，核对版本后本人签名，并重新查询。', '保存实际签名人及时间后才完成，不能由周红欣代签。'),
    ('签收指定不扩正文权限', '合法指定人及无正文权限对照账号', '只对已核实权限的合适对照账号检查任务通知与直接正文链接，不增加授权。', '没有正文资格的人不能因指定或通知获得阅读权。'),
    ('未签收不阻止合法切版', '张鑫悦 zhangxinyue、赵海辰 zhaohaichen', '存在合法未签收任务时核对当前新版和普通浏览，不代签剩余人员。', '新版受控且生效即可切版，不要求所有人或七人全签。'),
    ('新版独立签收和旧版历史', '各合法签收账号、赵海辰 zhaohaichen', '在01授权换版完成后检查新版独立下发任务、指定关系和签名，再查询旧版历史。', '旧版指定和签名保留但不能充当新版完成，未签名仍待签；多派名单外人员按实际记录失败。'),
]
for item in chain06:
    blocked('06', *item)

rows.extend(json.loads((root / '07-08' / 'cases.json').read_text(encoding='utf-8-sig')))
for row in rows:
    if not row['screenshotPaths']:
        raise ValueError('没有图片：' + row['caseId'])
    for source in row['screenshotPaths']:
        if not pathlib.Path(source).is_file():
            raise FileNotFoundError(source)


def readable(value):
    text = str(value).replace('→', '，').replace('／', '或').replace('JSON', '结构化原始记录')
    text = re.sub(r'完整性指纹[0-9a-f]{64}。?', '下载文件的完整性核对值保存在原始证据中。', text)
    return text


def run(*arguments):
    result = subprocess.run(['officecli', *map(str, arguments)], capture_output=True, encoding='utf-8', errors='replace')
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    return result.stdout


report_dir = workspace / 'report'
report_dir.mkdir(exist_ok=True)
target = report_dir / (datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).strftime('%Y%m%d%H%M%S') + '.xlsx')
run('create', target)
commands = [{'command': 'set', 'path': '/Sheet1', 'props': {'name': '已通过'}}]
for name in ['未通过', '存疑']:
    commands.append({'command': 'add', 'parent': '/', 'type': 'sheet', 'props': {'name': name}})
image_dir = root / 'report-images'
image_dir.mkdir(exist_ok=True)
image_cache = {}
headers = ['用例编号', '主链', '检查内容', '操作账号', '对象和版本', '应该发生什么', '实际发生什么', '完整测试路径与复现步骤', '证据说明及未覆盖范围', '本轮截图']
counts = collections.Counter(row['status'] for row in rows)
for sheet, color in [('已通过', '166534'), ('未通过', '991B1B'), ('存疑', '92400E')]:
    subset = [row for row in rows if row['status'] == sheet]
    commands.append({'command': 'set', 'path': '/' + sheet, 'props': {'freeze': 'D4', 'autoFilter': 'A3:J' + str(len(subset) + 3), 'tabColor': color}})
    subtitle = '2026年11月28日，北京时间。本地测试1。前六链因十人项目访问前置未齐备而跳过，不是通过，也不是已证明下游故障。第七、八链独立实测。'
    for number, text in [(1, '文控八主链本地复测结果：' + sheet), (2, subtitle)]:
        commands.append({'command': 'set', 'path': f'/{sheet}/A{number}:J{number}', 'props': {'merge': 'true'}})
        commands.append({'command': 'set', 'path': f'/{sheet}/A{number}', 'props': {'value': text, 'font.name': 'Microsoft YaHei', 'font.size': '12', 'font.bold': 'true', 'alignment.wrapText': 'true'}})
    commands.append({'command': 'set', 'path': '/' + sheet + '/row[2]', 'props': {'height': '36'}})
    for column, width, title in zip('ABCDEFGHIJ', [15, 12, 33, 26, 39, 48, 70, 88, 55, 100], headers):
        commands.append({'command': 'set', 'path': f'/{sheet}/col[{column}]', 'props': {'width': str(width)}})
        commands.append({'command': 'set', 'path': f'/{sheet}/{column}3', 'props': {'value': title, 'fill': color, 'font.color': 'FFFFFF', 'font.bold': 'true', 'font.name': 'Microsoft YaHei', 'alignment.wrapText': 'true'}})
    for number, row in enumerate(subset, 4):
        values = [row['caseId'], row['chain'], row['title'], row['account'], row['object'], row['expected'], row['actual'], row['reproduction'], row.get('evidenceNote', '')]
        for column, value in zip('ABCDEFGHI', values):
            commands.append({'command': 'set', 'path': f'/{sheet}/{column}{number}', 'props': {'value': readable(value), 'font.name': 'Microsoft YaHei', 'font.size': '11', 'alignment.wrapText': 'true', 'alignment.vertical': 'top', 'fill': 'F3F6FA' if number % 2 == 0 else 'FFFFFF'}})
        commands.append({'command': 'set', 'path': f'/{sheet}/row[{number}]', 'props': {'height': '390'}})
        source_key = tuple(row['screenshotPaths'])
        if source_key not in image_cache:
            sources = [Image.open(path).convert('RGB') for path in source_key]
            cell_width = 1280 if len(sources) <= 2 else 960
            cell_height = 760 if len(sources) <= 2 else 600
            grid_columns = 1 if len(sources) <= 2 else 2
            grid_rows = (len(sources) + grid_columns - 1) // grid_columns
            canvas = Image.new('RGB', (cell_width * grid_columns, cell_height * grid_rows), 'white')
            for index, source in enumerate(sources):
                contained = ImageOps.contain(source, (cell_width, cell_height))
                left = (index % grid_columns) * cell_width + (cell_width - contained.width) // 2
                top = (index // grid_columns) * cell_height + (cell_height - contained.height) // 2
                canvas.paste(contained, (left, top))
            destination = image_dir / (str(len(image_cache) + 1) + '.jpg')
            canvas.save(destination, quality=86)
            image_cache[source_key] = destination
        commands.append({'command': 'add', 'parent': '/' + sheet, 'type': 'picture', 'props': {'src': str(image_cache[source_key]), 'anchor': f'J{number}:K{number + 1}', 'alt': row['caseId'] + '本轮截图；覆盖范围以证据说明为准'}})
batch_path = root / 'consolidated-report-batch.json'
batch_path.write_text(json.dumps(commands, ensure_ascii=False), encoding='utf-8')
(root / 'consolidated-report-build.log').write_text(run('batch', target, '--input', batch_path, '--stop-on-error'), encoding='utf-8')
run('close', target)
validation = run('validate', target)
(root / 'consolidated-report-schema-check.txt').write_text(validation, encoding='utf-8')
(root / 'consolidated-report-preview.html').write_text(run('view', target, 'html'), encoding='utf-8')
with zipfile.ZipFile(target) as archive:
    workbook = ET.fromstring(archive.read('xl/workbook.xml'))
    namespaces = {'sheet': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
    sheets = [sheet.attrib['name'] for sheet in workbook.findall('sheet:sheets/sheet:sheet', namespaces)]
    picture_count = sum(archive.read(name).count(b'<xdr:pic>') for name in archive.namelist() if name.startswith('xl/drawings/drawing') and name.endswith('.xml'))
    if sheets != ['已通过', '未通过', '存疑'] or picture_count != len(rows):
        raise ValueError('工作表或截图数量不匹配')
summary = {'report': str(target), 'counts': dict(counts), 'caseCount': len(rows), 'images': picture_count, 'sheets': sheets, 'businessResults': dict(collections.Counter(row['status'] for row in rows if row['chain'] in ['07', '08'])), 'blockedFirstSix': sum(row['chain'] in ['01', '02', '03', '04', '05', '06'] for row in rows), 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()}
(root / 'merged-cases.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding='utf-8')
(root / 'consolidated-report-validation.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False))
print(validation)
