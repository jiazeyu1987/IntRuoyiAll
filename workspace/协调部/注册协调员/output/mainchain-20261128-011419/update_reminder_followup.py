import json
import subprocess
from datetime import datetime
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parent
manifest_path = root / 'report-manifest.json'
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
report = Path(manifest['report'])
cases = manifest['rows'] if 'rows' in manifest else manifest['cases']
row = next(item for item in cases['存疑'] if item[0] == 'U11')
row[2] = '09时后未查到送达，仍待确认'
row[3] = '2026年11月28日09时42分至09时52分续核：国内正式证990824397仍为第一版，有效期至2027年1月20日，显示到期前2个月。提醒启用、每日09时；两个月节点接收人为徐翠、管理员、杨琨。徐翠收件箱最新消息为今日07时47分55秒，未见本轮到期提醒；徐晨最新为06时56分11秒，亦未见，但他不在该节点名单中，不能据此判失败。详情中的06时39分30秒记录是变更确认通知，不是到期提醒。'
row[5] = '打开 http://localhost:8081，选择测试1。徐翠 xucui 登录，个人中心、我的站内信，保持无筛选、第一页，核对最新消息时间与本轮证号SIM-REG-20261128011419-CN；再进入注册证管理、注册证配置、通知设置，核对国内到期前2个月的徐翠、管理员、杨琨，取消关闭不保存。徐晨 xuchen登录同样查看站内信作补充。正式证详情为 http://localhost:8081/mdm/registration-certificate/detail/990824397。不得把变更生效通知当成到期提醒。'
row[6] = '目前只能确认所查收件箱无本轮到期提醒，不能确认09时任务是否执行、是否发给其他接收人或是否存在发送异常。尚无正常任务执行及发送记录页面证据，需由任务维护人员提供对应记录后才能判定；未手工触发任务、改时间、改配置或重提申请。保留存疑，不宣称通过或已证实程序漏发。'
row[7] = '63-reminder-followup-combined.jpg'
font = ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc', 23)
panels = [
    ('原报告截图保留：提醒已启用，每日北京时间09时', '58-reminder-config.png'),
    ('本次续核：国内到期前两个月的实际接收名单', '62-expiry-recipient-config.png'),
    ('本次续核：名单内徐翠的收件箱，最新07时47分55秒', '61-xucui-reminder-inbox.png'),
    ('补充核验：徐晨不在该节点名单中，不作为漏发判断依据', '60-xuchen-reminder-inbox.png'),
]
canvas = Image.new('RGB', (2560, 1536), 'white')
draw = ImageDraw.Draw(canvas)
for index, (title, filename) in enumerate(panels):
    left = (index % 2) * 1280
    top = (index // 2) * 768
    draw.text((left + 12, top + 8), title, font=font, fill='#19374d')
    image = Image.open(root / filename).convert('RGB')
    image.thumbnail((1280, 720))
    canvas.paste(image, (left, top + 48))
canvas.save(root / row[7], quality=86)
commands = []
for column, value in zip('ABCDEFG', row[:7]):
    commands.append({'command':'set','path':f'/存疑/{column}13','props':{'value':value,'type':'string'}})
commands.append({'command':'set','path':'/存疑/picture[10]','props':{'src':str(root / row[7]),'alt':'原配置截图及本次接收名单、徐翠与徐晨收件箱；只证明实际查到的状态，不证明正常任务已执行。'}})
manifest['date'] = datetime.now().isoformat(timespec='seconds')
manifest['status'] = '尚未全部通过；到期提醒09时后仍无可核实送达证据，未来新版待11月29日自然生效'
manifest['reminder_followup'] = {'checked_at':manifest['date'],'result':'存疑','evidence':[name for _, name in panels] + [row[7]],'normal_task_execution':'页面证据不足，未手动触发','recipient_checked':'徐翠','other_configured_recipients_not_checked':['管理员','杨琨']}
commands_path = root / 'reminder-followup-commands.json'
commands_path.write_text(json.dumps(commands, ensure_ascii=False, indent=2), encoding='utf-8')
result = subprocess.run(['officecli','batch',str(report),'--input',str(commands_path),'--stop-on-error','--json'], capture_output=True, text=True, encoding='utf-8')
(root / 'reminder-followup-officecli-result.json').write_text(result.stdout + result.stderr, encoding='utf-8')
if result.returncode:
    raise RuntimeError(result.stdout + result.stderr)
subprocess.run(['officecli','close',str(report)], check=True)
manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'report':str(report),'counts':manifest['counts'],'reminder_result':row[2]}, ensure_ascii=False))
