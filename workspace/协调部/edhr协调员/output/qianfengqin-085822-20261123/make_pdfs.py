from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
import fitz

folder = Path(__file__).parent
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
for category in ['成品检验报告', '成品检验记录']:
    filename = folder / f'881MO085822_K202510048_{category}_软件模拟.pdf'
    document = canvas.Canvas(str(filename), pagesize=A4)
    document.setTitle(f'881MO085822 {category} 软件模拟')
    document.setFont('STSong-Light', 19)
    document.drawString(42, 790, f'{category} - 软件模拟')
    document.setFillColorRGB(0.75, 0, 0)
    document.setFont('STSong-Light', 13)
    document.drawString(42, 756, '模拟测试附件、非真实检验报告／记录')
    document.drawString(42, 734, '不可作为真实质量依据')
    document.setFillColorRGB(0, 0, 0)
    document.setFont('STSong-Light', 12)
    lines = [f'资料类别：{category}', '环境：localhost:8081／测试1',
        '生产订单：881MO085822；活跃订单周期：1009200660',
        '批号：K202510048；批次数量：72件',
        '产品：数显球囊扩张压力泵 INT-IDE-243',
        '生产指令：MPS02068601',
        '批次执行：900000001359；PQC申请：350',
        '用途：本轮CASE001软件流程模拟，仅验证本类别上传保存。',
        '本附件不提供真实实测数据、合格结论、机构证明或签章。',
        '交接依据：PQC生产放行页面时间2026-11-23 06:38:20。',
        '交接披露：最新正式汇总16次提交、65项通过；旧检验详情',
        '存在不合格视图差异，不消除差异或替代责任岗位判断。',
        '损耗1件（检测用／报废）及生产数量、参数异常原值保留。',
        '不更改检验原值，不办理质量审批、上市放行或归档。',
        '编制身份：钱凤琴资料上传岗；日期：2026-11-23北京时间。',
        '签名／印章：无；未模拟任何真实人员签章。']
    for index, line in enumerate(lines):
        document.drawString(42, 690 - index * 27, line)
    document.save()
    pdf = fitz.open(filename)
    assert len(pdf) == 1 and '881MO085822' in pdf[0].get_text()
    pdf[0].get_pixmap(matrix=fitz.Matrix(1.3, 1.3)).save(filename.with_suffix('.png'))
    print(filename)
