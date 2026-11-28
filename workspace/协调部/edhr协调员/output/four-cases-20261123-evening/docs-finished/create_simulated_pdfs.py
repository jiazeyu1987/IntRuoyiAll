from pathlib import Path
import pymupdf
from pypdf import PdfReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas

folder = Path(__file__).resolve().parent
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
for category in ['成品检验报告', '成品检验记录']:
    target = folder / f'881MO082554_K202507136_{category}_软件模拟.pdf'
    document = canvas.Canvas(str(target), pagesize=(595, 842))
    document.setTitle(category + '（软件模拟）')
    document.setFont('STSong-Light', 21)
    document.drawString(45, 785, category + '（软件模拟）')
    document.setFillColorRGB(0.8, 0, 0)
    document.setFont('STSong-Light', 13)
    document.drawString(45, 745, '模拟测试附件、非真实检验报告／记录、不可作为真实质量依据')
    document.setFillColorRGB(0, 0, 0)
    document.setFont('STSong-Light', 12)
    lines = [
        '生产订单：881MO082554    批次：K202507136',
        '产品：球囊扩张压力泵    型号：KDL-IN30    计划数量：18019件',
        '活跃周期：1009200665（交接标识）',
        '批次执行：900000001360（交接标识）',
        '资料类别：' + category,
        '用途：CASE001软件模拟附件上传、保存及刷新查回验证。',
        '本文件不包含真实实测数值、合格结论、机构证明、签名或印章。',
        '不替代系统内正式检验记录，不作任何真实质量决定。',
        '前端已核对：测试1、钱凤琴；本单已生产放行、待上市放行。',
        '页面生产放行签名：朱利江，2026-11-23 22:36:00。',
        '本附件不修改既有生产数量、参数异常或损耗。',
        '本附件不代表生产放行、上市放行或真实质量批准。',
        '编制日期：2026-11-23。仅供本轮软件模拟测试使用。',
    ]
    for index, text in enumerate(lines):
        document.drawString(45, 690 - index * 35, text)
    document.showPage()
    document.save()
    reader = PdfReader(str(target))
    assert len(reader.pages) == 1
    text = reader.pages[0].extract_text()
    assert '881MO082554' in text and 'K202507136' in text and '不可作为真实质量依据' in text
    rendered = pymupdf.open(target)
    rendered[0].get_pixmap(matrix=pymupdf.Matrix(1.2, 1.2)).save(str(target.with_suffix('.png')))
    print(target.name, target.stat().st_size, 'bytes, validated')
