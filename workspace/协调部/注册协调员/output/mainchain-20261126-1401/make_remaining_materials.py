from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import fitz

output = Path(__file__).parent
pdfmetrics.registerFont(TTFont('EmbeddedHei', 'C:/Windows/Fonts/simhei.ttf'))
for kind in ['CN', 'CE', 'FDA']:
    for purpose in ['certificate', 'attachment', 'renewal', 'change']:
        if kind == 'CE' and purpose in ['certificate', 'attachment']:
            continue
        target = output / f'SIM-REG-20261126-1401-{kind}-{purpose}-R1.pdf'
        lines = [
            '模拟测试资料 / SIMULATED TEST ONLY',
            '非真实注册证、非监管批准、不可用于临床或经营',
            f'测试标识：SIM-REG-20261126-1401-{kind}；材料：{purpose}',
            '公司及持证主体：上海七木医疗器械有限公司',
            '项目：IDI；DCC：0313170202；产品：按压式压力泵',
            f'证号：SIM-REG-20261126-1401-{kind}；分类：二类 / II',
            '型号：SIM-B' if purpose == 'change' else '型号：SIM-A',
            '批准日期：2026-11-25；首次获证日期：2026-11-25',
            '预期用途：仅验证系统材料流转，不用于临床。',
            '自产：是；委托生产：否；住所：模拟测试路1号',
        ]
        if kind != 'FDA':
            lines += ['有效期至：2027-01-20']
        if kind == 'CN':
            lines += ['生效日期：2026-11-26']
        if kind == 'CE':
            lines += ['英文名：Simulated Pressure Pump；公告机构：0123',
                      '产品代码：SIM-IDI；商品名：SIM-PUMP；Rev.0；签发2026-11-25']
        if kind == 'FDA':
            lines += ['K/P号：K261401；提交类型：510(k)；产品代码：SIM',
                      '批准信函或产品描述格式样本；无独立生效日和有效期。']
        if purpose == 'change':
            lines += ['变更批准日期：2026-11-26；SIM-A改为SIM-B；其他信息不变。']
        if purpose == 'renewal':
            lines += ['续证批准日期：2026-11-27；有效期至：2029-01-20',
                      '国内续证生效：2026-11-27；CE批准日期即生效含义。']
        document = canvas.Canvas(str(target))
        document.setFont('EmbeddedHei', 11)
        for index, line in enumerate(lines):
            document.drawString(32, 800 - index * 32, line)
        document.save()
        preview = fitz.open(target)
        assert 'SIMULATED TEST ONLY' in preview[0].get_text()
        preview[0].get_pixmap().save(str(target.with_suffix('.png')))
        print(target.name)
