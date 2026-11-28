import hashlib
import json
import shutil
from datetime import datetime, timezone, timedelta
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parent
shots = root / 'screenshots'
source = Path(r'C:\Users\BJB110\.agent-browser\tmp\screenshots')
captures = {
 'category': ('1795800984385', '类别权限编辑浏览.png'),
 'role': ('1795803219945', '编辑角色QMS通用资格.png'),
 'member': ('1795803335950', '张鑫悦编辑角色成员.png'),
 'qms_list': ('1795804273537', '07-独立访问列表.png'),
 'qms_search': ('1795804434259', '07-编号搜索.png'),
 'qms_preview': ('1795805214862', '07-搜索命中正文空白.png'),
 'qms_scroll': ('1795805405498', '07-滚动仍无正文.png'),
 'qms_download': ('1795805905718', '07-下载确认及系统异常.png'),
 'general_list': ('1795806632322', '08-独立访问列表.png'),
 'general_search': ('1795806856429', '08-编号搜索.png'),
 'general_preview': ('1795807554397', '08-正文可读搜索修订2.png'),
 'general_detail': ('1795808048311', '08-详情系统异常.png'),
 'upload_qms': ('1795808602372', '07-上传可选QMS.png'),
 'upload_general': ('1795808785667', '08-上传可选通用.png'),
}
evidence = {}
times = {}
for key, (stamp, name) in captures.items():
    original = source / f'screenshot-{stamp}.png'
    destination = shots / name
    shutil.copy2(original, destination)
    evidence[key] = str(destination)
    times[key] = datetime.fromtimestamp(original.stat().st_mtime, timezone(timedelta(hours=8))).isoformat(timespec='seconds')

settings = {
 '07': ('QMS', 'QMS文件夹', 'DCC-MAIN-20261126', '2054545668044070362'),
 '08': ('GENERAL', '通用文件夹', 'DCC0-GENERAL-20261127-G08', '2054545668044070381'),
}
cases = []

def add(chain, number, title, expected, actual, status, keys, steps, note='本轮真实页面原图，不使用历史截图。', account='zhangxinyue（张鑫悦）'):
    mode, library, identifier, file_id = settings[chain]
    cases.append({
      'caseId': f'{chain}-{number:02d}', 'chain': chain, 'title': title,
      'account': account, 'object': f'{identifier}，A/2，文件{file_id}',
      'expected': expected, 'actual': actual, 'status': status,
      'reproduction': f'环境：http://localhost:8081；租户：测试1；账号：{account}；菜单：文控中心→受控浏览→{library}；入口：http://localhost:8081/dcc/controlled-file/browser?browserMode={mode}；对象：{identifier}，A/2；正文链接：http://localhost:8081/dcc/controlled-file/detail/{file_id}?viewer=1；输入动作：{steps}；实际：{actual}；预期：{expected}；证据时间：' + '、'.join(times[key] for key in keys),
      'screenshotPaths': [evidence[key] for key in keys], 'evidenceNote': note,
    })

for chain, (mode, library, identifier, file_id) in settings.items():
    prefix = 'qms' if chain == '07' else 'general'
    add(chain, 1, '实际账号资格及既有样本',
        '现场确认账号本范围及文件类型的浏览编辑资格，既有样本当前已生效。',
        '管理员前端核实张鑫悦为编制编辑测试角色成员；实际勾选QMS和通用浏览编辑及文件独立编辑资格；DCC0流程验证类别中该角色为编辑。张鑫悦登录后查到样本A/2受控已生效。没有修改授权。',
        '已通过', ['member', 'role', 'category', f'{prefix}_list'],
        '管理员→系统管理→角色管理→权限角色→文控→文控编制编辑测试→查看成员及菜单权限，仅展开；文控权限→类别权限→DCC0流程验证文件→维护权限，只读；张鑫悦登录核对测试1及姓名后进本范围。',
        account='yingtaigroupadmin（管理员）及zhangxinyue（张鑫悦）')
    add(chain, 2, '不选项目直接访问当前受控列表',
        '不先选项目即可独立访问，当前列表显示生效受控版本。',
        f'张鑫悦从受控浏览直接切换{library}，无需选择项目；列表两条均A/2受控已生效，包含指定样本。',
        '已通过', [f'{prefix}_list'],
        f'默认项目页不选项目，直接点{library}，保持当前目录和当前生效受控版本，查看列表。',
        '仅证明本轮返回的列表均为当前生效版，未宣称全部草稿待生效作废数据的反向隔离均通过。')
    add(chain, 3, '完整文件编号检索当前版本',
        '按完整编号返回指定文件的当前受控版本。',
        f'输入{identifier}查询，返回一条正确样本A/2受控已生效，没有混入另一范围文件。',
        '已通过', [f'{prefix}_search'],
        f'文件名称、编号或项目名称输入{identifier}，点击查询，结果稳定后核对编号来源版本状态。')
    add(chain, 4, '只读正文实际可读',
        '浏览可读到本样本A/2正文，并有当前用户受控水印。',
        '受控章和张鑫悦水印可见，但正文区空白；缩到50%并在正文区向下滚动仍无正文。搜索能命中revision 2。同次下载原件本地渲染中文正文清楚，不能把搜索命中当作预览可读。' if chain == '07' else '正文清楚，可读测试用途、文件编号和Revision: 2；受控章及张鑫悦水印可见。',
        '未通过' if chain == '07' else '已通过',
        ['qms_preview', 'qms_scroll'] if chain == '07' else ['general_preview'],
        f'编号{identifier}查询→浏览，打开文件{file_id}的viewer=1页面；点击缩小两次到50%，查看正文；QMS再在正文显示区向下滚动。',
        '下载文件渲染图另保留，属于下载内容核对，不冒充浏览器原图。')
    pdf_name = '07-QMS-A2.pdf' if chain == '07' else '08-通用-A2.pdf'
    pdf = root / 'downloads' / pdf_name
    digest = hashlib.sha256(pdf.read_bytes()).hexdigest()
    add(chain, 5, '编辑账号下载及版本内容核对',
        '实际编辑账号经前端确认下载取得保存文件，内容编号和修订2正确。',
        f'张鑫悦从详情更多→下载正文→确认下载，取得一页PDF，编号正确、修订2，与页面A/2一致。文件{pdf_name}，大小{pdf.stat().st_size}字节；完整性指纹{digest}。未虚构可编辑源文件。',
        '已通过', ['qms_download'] if chain == '07' else ['general_detail', 'general_preview'],
        '列表浏览→查看详情→更多→下载正文→确认下载，在下载目录核实新文件时间，解析和渲染本轮PDF核对正文。',
        '下载文件及渲染图随包保留。通用列表首次确认未观察到落地文件，随后详情页下载后核实新文件，不把首次点击当作成功。没有据此宣称后台留痕已核验。')
    cases[-1]['screenshotPaths'].append(str(shots / ('07-下载原件渲染.png' if chain == '07' else '08-下载原件渲染.png')))
    add(chain, 6, '扫描原印章保真及本轮下载留痕',
        '扫描件保留原印章，下载留痕可核对本轮用户版本时间和结果。',
        '下载普通样本看到系统受控章，但无可独立对比的扫描原件，不能替代原扫描印章保真验证。详情文件与申请资料显示“系统异常”；未取得本轮下载留痕的前端记录，不能以确认文案代替持久化记录。',
        '存疑', ['qms_download'] if chain == '07' else ['general_detail'],
        f'打开{identifier} A/2详情检查文件归属及文件与申请资料，出现系统异常；下载已执行，但未获得该次用户版本时间结果记录。需要扫描原件和可读取下载留痕后续验。',
        '截图是前置异常及下载上下文，不是扫描印章或下载留痕成功证据。未用接口或数据库。')
    add(chain, 7, '纯浏览可读但不能下载普通原件',
        '确认只有浏览没有编辑的账号可读而不能下载普通原件。',
        '可靠纯浏览账号尚未落实。现场核实的张鑫悦同时有QMS及通用编辑，不能冒充纯浏览对照。依赖该身份的可读和拒绝下载动作均未执行。',
        '存疑', ['member', 'role'],
        '管理员只读查看编制编辑测试角色成员与受控浏览子权限，张鑫悦是成员且两范围编辑均勾选。需已确认纯浏览账号及目标类别浏览资格；未猜账号或扩权。',
        '前置阻塞图，不是纯浏览下载被拒的下游执行证据。')
    for number, kind in ((8, '仅项目'), (9, '仅QMS'), (10, '仅通用'), (11, '同目录其他未授权类型')):
        add(chain, number, f'{kind}对照四层隔离',
            f'证明账号确属{kind}后，验证列表、检索、正文和直接链接；范围类型权限不得继承。',
            f'没有落实已现场确认的{kind}对照账号。张鑫悦有项目入口及QMS通用编辑浏览，不能作此对照。该身份下列表、编号检索、正文、直接链接四项均因前置不足未执行，不能记通过或产品缺陷。',
            '存疑', ['member', 'role'],
            f'管理员先只读核对{kind}账号全部有效权限及目标类型；本轮仅核实张鑫悦多范围资格。前置具备后该账号登录测试1→受控浏览→{library}→列表→编号{identifier}查询→正文→直接访问文件{file_id}的viewer=1链接；后四步本轮未执行。',
            '前置阻塞图，非隔离执行证据。需业务方落实符合条件的已有账号，没有创建账号或修改角色。')
    add(chain, 12, '记录表单全员下载例外',
        '记录表单允许全员下载例外与普通文件浏览禁下分别验证。',
        '获准样本均是DCC0流程验证普通文件，没有落实获准记录表单及必要对照身份。不能用普通文件编辑账号下载冒充记录表单例外。',
        '存疑', [f'{prefix}_search', 'role'],
        f'查到{identifier} A/2普通样本及张鑫悦多范围编辑资格；需获准记录表单与适用账号后，从实际前端入口实测例外下载。',
        '引用前置图，不是记录表单执行证据。')
    add(chain, 13, '普通及扫描上传仅项目范围',
        '普通和扫描件上传仅接受项目，不接收QMS及通用；不要求新增这些范围上传流程。',
        f'文件上传页仍有项目、QMS文件、通用文件目标，实际能切换{library}对应目标。没有选文件、填编号或提交，未证明后端是否拒绝非项目；扫描件入口也未完整核实，不能声称已接收。',
        '存疑', ['upload_qms'] if chain == '07' else ['upload_general'],
        f'张鑫悦→文控中心→文件上传→目标范围选{library}对应目标，观察可切换且项目字段消失、文件夹字段保留；不选文件、不创建。需明确普通和扫描件入口对应关系及获准拒绝测试方式后续验。',
        '只是前端目标选项现象，不证明后台接受。未因缺上传流程判失败，未上传、修订或审批。')
    add(chain, 14, '正文搜索现象及需求待澄清',
        '采集正文搜索现象，按指南保留需求内部冲突，不自行定最终口径。',
        '浏览页输入revision，搜索命中一处修订2，但QMS画面仍无正文。需求冲突未澄清，不把搜索命中当最终通过。' if chain == '07' else '输入revision搜索命中一处Revision: 2，正文高亮可见。需求内部冲突仍未澄清，不把当前现象当最终口径通过。',
        '存疑', [f'{prefix}_preview'],
        f'受控浏览→{library}→编号{identifier}查询→浏览→PDF正文关键字输入revision→搜索，读取一处匹配修订2；功能开放范围需CR-052口径澄清。',
        '搜索已实际执行，存疑因需求冲突而非未执行；QMS视觉问题独立记未通过。')

(root / 'cases.json').write_text(json.dumps(cases, ensure_ascii=False, indent=2), encoding='utf-8')
counts = {status: sum(case['status'] == status for case in cases) for status in ('已通过', '未通过', '存疑')}
lines = ['# 本地07、08本轮复测报告', '',
 '环境仅本地 http://localhost:8081，测试1。两链不能宣称全部通过。',
 f"共{len(cases)}项：已通过{counts['已通过']}，未通过{counts['未通过']}，存疑{counts['存疑']}。",
 '本轮日期为2026年11月28日北京时间，逐项复现保留截图实际发生时间。', '',
 '## 核实结果',
 '两份指定A/2仍受控已生效。现场核实张鑫悦实际角色、QMS通用浏览编辑及类别编辑资格。独立访问、编号检索和前端下载已执行。通用正文可读；QMS正文预览空白，只见水印和受控章，搜索命中不能代替阅读；同次下载原件本地渲染正常。', '',
 '## 缺失条件及影响',
 '没有可靠纯浏览、仅项目、仅QMS、仅通用及未授权类型对照账号，相应四层隔离和普通原件禁下未执行。没有获准记录表单或可对照扫描原件，例外下载和原印章保真未验证。下载留痕未得到前端记录，详情申请资料显示系统异常。上述前置需业务方或本轮协调员落实；本分片不改权限补造身份。',
 '上传页仍能切换QMS及通用，但未上传提交，不推断后台是否接收；普通及扫描上传限制存疑。正文搜索按指南等待CR-052需求口径澄清。独立可测部分均已执行。',
 '当前列表只证明本轮返回均为当前生效文件，未扩大为全部草稿待生效作废隔离通过；未另造样本。作废归档另属05链，本分片未作废文件。', '',
 '## 证据与边界',
 'screenshots有14张本轮前端原图、2张真实下载内容渲染图；downloads有两份本轮实际下载PDF。阻塞项引用前置图，逐项明确不是下游执行证据。',
 '初查浏览器仅一个本地页面，提示原闲置页面已回收，没有抢占其他活跃页面。全程自有内置浏览器，未改源码配置权限服务目录或业务内容，仅产生授权浏览下载正常留痕。管理员只读弹窗均未保存。',
 '报告与cases.json供主协调员统一制作三工作表截图Excel，本分片未另造Excel。', '',
 '## 逐项结果']
for case in cases:
    lines.extend([f"### {case['caseId']} {case['title']}：{case['status']}", case['actual'], '复现：' + case['reproduction'], '证据说明：' + case['evidenceNote'], ''])
(root / '报告.md').write_text('\n'.join(lines), encoding='utf-8')
manifest = [{'path': str(path), 'bytes': path.stat().st_size, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()} for path in sorted(root.rglob('*')) if path.is_file() and path.suffix != '.zip' and path.name != '证据清单.json']
(root / '证据清单.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
archive = root / '07-08-本轮报告清单及原图.zip'
with ZipFile(archive, 'w', ZIP_DEFLATED) as package:
    for path in sorted(root.rglob('*')):
        if path.is_file() and path != archive:
            package.write(path, path.relative_to(root))
print(json.dumps({'cases': len(cases), 'counts': counts, 'archive': str(archive), 'screenshots': len(list(shots.glob('*.png')))}, ensure_ascii=False))
