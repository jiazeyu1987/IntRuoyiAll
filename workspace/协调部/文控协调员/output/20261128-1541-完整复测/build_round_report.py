import collections, datetime, hashlib, json, pathlib, re, shutil, subprocess, zipfile
import xml.etree.ElementTree as ET
from PIL import Image, ImageOps
root = pathlib.Path(__file__).resolve().parent
workspace = root.parent.parent
old = workspace / 'output' / '20261128-0112-完整复测'
images = root / '07-08'
permission = root / '00-项目权限'
blocker = permission / '04-赵海辰受控浏览重置后无项目.png'
rows = json.loads((old / 'merged-cases.json').read_text(encoding='utf-8'))
environment = '2026年11月28日本轮DCC-20261128-1541；本地 http://localhost:8081/，测试1。'
access_path = '项目管理员核对IDI查看规则启用且无日期限制；赵海辰重新登录，项目代码清除筛选，受控浏览重置项目列表，16时10分项目无数据，16时14分受控浏览无项目。'
for row in rows:
    row['status'] = '存疑'
    if row['chain'] == '共同前置':
        original = row['actual']
        row['actual'] = '本轮沿用凌晨本人登录证据，未重复登录，不算本轮新实测通过。原观察：' + original
        matches = list((permission / '原始九人证据').glob(row['account'].split('（')[0] + '*'))
        row['screenshotPaths'] = [str(matches[0])] if matches else [str(blocker)]
        row['evidenceNote'] = '历史通过证据仅供参考，本轮未新登录。十人前置总体未齐。'
        if row['caseId'] == '00-09':
            row.update(status='未通过', actual='本轮项目管理员重新登录复核：赵海辰IDI查看授权存在并启用，本人项目代码无数据、受控浏览无项目。无明确错误码，根因未定。', reproduction=environment + access_path + '恢复需账号数据范围或授权逻辑维护人员处理后，由本人重登看到并打开IDI。', screenshotPaths=[str(permission / '01-当前IDI赵海辰正式VIEW.png'), str(blocker)], evidenceNote='本轮项目管理员原始截图已核读，非下游业务执行证据。未新增或修改规则。')
    elif row['chain'] in ['01','02','03','04','05','06']:
        steps = row['reproduction'].split('按以下路径复测：',1)[-1]
        row.update(actual='未执行。指南要求十人项目访问前置齐全，赵海辰本轮重登仍看不到IDI。未新增本项文件、申请轮次或签名，不沿用旧轮结果。', reproduction=environment + access_path + '责任维护人解决、项目管理员确认十人齐备后，使用独立模拟标识按以下路径复测：' + steps, screenshotPaths=[str(blocker)], evidenceNote='仅证明本轮前置阻塞，不是本项业务执行或结果证据。前六链71项均跳过未执行。')
    else:
        chain = row['chain']; number = int(row['caseId'].split('-')[1])
        name = 'QMS' if chain == '07' else '通用'
        mode = 'QMS' if chain == '07' else 'GENERAL'
        listing = images / (chain + '-' + name + '独立列表.png')
        search = images / (chain + '-' + name + '编号检索.png')
        body = images / ('07-QMS原失败样本本轮正文可读.png' if chain == '07' else '08-通用正文可读.png')
        denied = images / ('07-QMS下载无权.png' if chain == '07' else '08-重新登录后下载仍无权.png')
        code = 'DCC0-QMS-20261126-001' if chain == '07' else 'DCC0-GENERAL-20261127-G08'
        row['object'] = code + '，生效A/2。' + ('另核DCC-MAIN-20261126，A/2。' if chain == '07' else '文件2054545668044070381。')
        row['reproduction'] = environment + '张鑫悦正常登录核对租户姓名，文控中心、受控浏览，不选项目直接进入' + name + '，地址 http://localhost:8081/dcc/controlled-file/browser?browserMode=' + mode + '。'
        row['screenshotPaths'] = [str(listing)]
        row['evidenceNote'] = '本轮既有模拟样本只读复测，未新增修订或审批。'
        if number == 1:
            row['actual'] = '本轮已有生效A/2样本可读。本人管理员另核DCC0类别五条规则启用、编辑规则对应文控编制编辑测试、张鑫悦为其成员；目录授权页无已绑定目录，有效目录编辑资格尚未完全证明。'
            row['screenshotPaths'] = [str(images / 'DCC0类别五条启用规则.png'), str(images / '张鑫悦当前编辑角色成员.png'), str(images / '管理员目录授权空列表.png')]
            row['reproduction'] += '管理员登录，文控权限、类别权限、DCC0、维护权限；系统管理、角色管理、权限角色、文控、文控编制编辑测试、分配，按用户名zhangxinyue查询，绿色为已分配；再查看文控权限、目录授权。只读不保存。'
        elif number in [2,3,4]:
            row['status'] = '已通过'
            row['actual'] = {2:'不选项目直接出现两条既有模拟文件，均为生效A/2。仅覆盖可见样本，不代替非生效或未授权隔离。',3:'完整编号检索返回对应一条生效A/2，编号版本一致。',4:'只读正文文字、水印及受控章可见。QMS旧失败PDF本轮可读，不沿用旧空白结论。' if chain == '07' else '正文可读到模拟声明及Revision: 2，水印受控章可见。'}[number]
            row['screenshotPaths'] = [str({2:listing,3:search,4:body}[number])]
            row['reproduction'] += {2:'观察列表及版本状态。',3:'完整编号检索' + code + '，加载后核对唯一记录A/2。',4:'打开样本浏览，等待正文加载并核对文字内容及章印。'}[number]
        elif number == 5:
            row['actual'] = '列表下载原件并确认，报当前账号无权访问此受控文件，请核对文件权限或审批任务。' + ('通用先报账号未登录，正常退出重登仍无权；详情下载正文也未取得可核验文件。' if chain == '08' else '') + '目录编辑资格未完全证实，不能直接归因下载缺陷，也不记下载通过。'
            row['screenshotPaths'] = [str(denied)]
            row['reproduction'] += '检索' + code + '，点击下载原件、确认，观察权限原始错误。授权维护人员先核清目录与类型有效编辑资格，再由合格账号重试核对实际文件。'
        elif number == 6:
            row['actual'] = '未取得可核验下载文件，未落实获准扫描原件对照；扫描原章保真及本轮下载留痕未完成。'
            row['screenshotPaths'] = [str(denied),str(body)]
            row['reproduction'] += '样本责任人提供获准扫描原件、授权维护人员落实下载资格后，下载原件核对版本内容及原印章，再查正常下载记录中的账号时间。当前缺资格确认及样本。'
        elif number in [7,8,9,10,11]:
            row['actual'] = '未落实并现场证明符合本项条件的既有对照身份。张鑫悦是编辑角色，不能冒充纯浏览或仅单目录人员。列表、检索、正文、直接链接隔离均未执行。'
            row['screenshotPaths'] = [str(images / '张鑫悦当前编辑角色成员.png'),str(images / '管理员目录授权空列表.png')]
            row['reproduction'] += '授权维护人员提供并证明符合“' + row['title'] + '”的既有账号后，依次测试列表、完整编号检索、正文、直接链接四层隔离。当前缺合格身份，未改权限。'
            row['evidenceNote'] = '图片仅说明身份前置缺口，不是隔离执行证据。'
        elif number == 12:
            row['actual'] = '本轮样本是普通文件，未落实记录表单及必要对照身份，全员下载例外未执行。'
            row['reproduction'] += '样本责任人落实本目录已受控记录表单和准用身份后，以普通用户下载记录表单并核对真实文件；不以普通原件下载代替。当前缺样本及身份。'
        elif number == 13:
            row['account'] = 'yingtaigroupadmin（管理员）'
            row['actual'] = '管理员本轮文件上传页面可切换QMS和通用目标；未选文件、填编号或提交，未证明后台接收。扫描入口未完整验证。与指南仅项目口径存在页面选项差异，暂留存疑。'
            row['screenshotPaths'] = [str(images / ('管理员上传页可选QMS.png' if chain == '07' else '管理员上传页可选通用.png'))]
            row['reproduction'] = environment + '管理员登录，文控中心、文件上传，目标范围选择' + name + '，观察可选中、DCC项目框隐藏。不选文件不提交；需求责任人确认入口限制口径，后端接收及扫描限制仍未测试。'
        else:
            row['actual'] = '本轮只核正文阅读，未执行关键词查找。指南注明需求内部冲突，仍需需求责任人澄清；不沿用凌晨搜索现象作为本轮结果。'
            row['screenshotPaths'] = [str(body)]
            row['reproduction'] += '打开正文，需求口径澄清后以实际词语搜索核对命中高亮。本轮查找未执行。'
            row['evidenceNote'] = '阅读图片不是搜索执行证据。'
for row in rows:
    for source in row['screenshotPaths']:
        if not pathlib.Path(source).is_file():
            raise FileNotFoundError(source)
def readable(value):
    return str(value).replace('→','、').replace('CR-','需求条目')
def run(*arguments):
    result = subprocess.run(['officecli',*map(str,arguments)],capture_output=True,encoding='utf-8',errors='replace')
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    return result.stdout
builder = (old / 'build_consolidated_report.py').read_text(encoding='utf-8')
tail = builder[builder.index("report_dir = workspace / 'report'"):]
tail = re.sub("    subtitle = .*","    subtitle = '本轮DCC-20261128-1541，本地测试1。六项子用例通过不等于八链通过。前六链71项未执行；九人沿用旧证据，下载隔离等仍受阻。'",tail)
exec(compile(tail,str(root / 'officecli_layout'),'exec'))
copy_dir = root / 'report'
copy_dir.mkdir(exist_ok=True)
shutil.copy2(target,copy_dir / target.name)
(root / '本轮汇总.md').write_text('本轮未达到八链终点。109项中通过6项、未通过1项、存疑102项；前六链71项因赵海辰项目不可见跳过。九人旧证据沿用不算新实测。QMS通用现有A/2列表、编号搜索、阅读通过；QMS旧失败PDF本轮可读。下载仍报当前账号无权访问此受控文件，请核对文件权限或审批任务，通用重登无改善。本人管理员核实编辑角色成员和类别规则，但目录资格未完全证实；未修改配置。纯浏览及隔离账号、记录表单和扫描样本未落实，搜索需求待澄清。恢复须账号数据范围或授权逻辑责任人处理赵海辰不可见，再由本人重登打开IDI；授权维护人落实合格目录编辑和反向身份，样本责任人落实记录表单扫描件，需求责任人澄清查找及上传限制。未新建文件、轮次或签名，未完成审批生效作废签收，未改代码或接口数据库写入。Excel：' + str(target),encoding='utf-8')
