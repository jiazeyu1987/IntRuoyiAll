from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

output = Path(__file__).parent
pdfmetrics.registerFont(TTFont('Hei', 'C:/Windows/Fonts/simhei.ttf'))
for kind in ['CN', 'CE', 'FDA']:
    for purpose in ['certificate', 'attachment', 'renewal', 'change', 'future']:
        lines = ['模拟资料，仅系统测试，非真实注册证或监管批准', '测试批次 SIM-REG-20261127-1446', '公司及持证主体 上海七木医疗器械有限公司', '项目 IDI，DCC 0313170202，产品 按压式压力泵', f'证号 SIM-REG-20261127-1446-{kind}', f'材料用途 {purpose}', '产品分类 二类，原型号 SIM-A', '首次获证日期 2026-11-26，批准日期 2026-11-27']
        if kind != 'FDA':
            lines += ['委托生产 否，自行生产 是', '生效日期 2026-11-27' if purpose != 'future' else '生效日期 2026-11-28', '有效期至 2027-01-20' if purpose in ['certificate', 'attachment'] else '有效期至 2029-01-20']
        if kind == 'CE':
            lines += ['CE批准日期即生效含义，不另填独立生效日', '制造商 上海七木医疗器械有限公司，公告机构 0123']
        if kind == 'FDA':
            lines += ['K/P号 K271446，提交类型 Traditional 510(k)，产品代码 SIM', '客户持证 否，批准信函 有效，工厂登记 Active', '无有效期及独立生效日期，预期用途 仅作系统流转测试']
        if purpose == 'change':
            lines += ['本次仅将产品名称改为 按压式压力泵模拟变更，其余字段不变']
        document = canvas.Canvas(str(output / f'{kind}-{purpose}.pdf'))
        document.setFont('Hei', 11)
        for index, text in enumerate(lines):
            document.drawString(30, 800-index*30, text)
        document.save()
(output / 'invalid-format.pdf').write_text('明确标记的格式错误反向测试资料，不是有效PDF', encoding='utf-8')
print('模拟资料已生成')
