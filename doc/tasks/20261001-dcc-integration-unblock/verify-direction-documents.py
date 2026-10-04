"""Validate this direction review and the synchronized confirmed business wording."""
from pathlib import Path
from html.parser import HTMLParser
import json
import re

ROOT = Path('C:/IntRuoyiAll-int_main')
html = (ROOT / 'docs/product/dcc-final-requirements.html').read_text(encoding='utf-8')
parser = HTMLParser(convert_charrefs=True)
parser.feed(html)
parser.close()
ids = re.findall(r'id="([^"]+)"', html)
links = re.findall(r'href="#([^"]+)"', html)
assert len(ids) == len(set(ids))
assert set(links) <= set(ids)
assert all('flow-' + str(i).zfill(2) in ids for i in range(1, 13))
assert '文控上传线下培训文件即可完成' in html
assert '作废文件保存20年' in html
assert all(old not in html for old in (
    '名单内人员全部完成后才通过', '保留期长度和提醒提前量', '保留期限长度、起算规则'))
review = (ROOT / 'docs/dcc-parallel-delivery/development-direction-review.md').read_text(encoding='utf-8')
assert all(key in review for key in ('A 流程', 'B 项目', 'C 版本', 'D 关联', '782', 'CC-2', 'E2E'))
names = ('development-direction-review.md', 'shared-contract.md', 'implementation-contract.md', 'review-report.md')
for module in ('a', 'b', 'c', 'd'):
    worker = Path('C:/IntRuoyi/20260930-dcc-' + module)
    for name in names:
        assert (worker / 'docs/dcc-parallel-delivery' / name).read_bytes() == (ROOT / 'docs/dcc-parallel-delivery' / name).read_bytes()
    assert (worker / 'docs/product/dcc-final-requirements.html').read_bytes() == (ROOT / 'docs/product/dcc-final-requirements.html').read_bytes()
print(json.dumps({'PASS': True, 'flow_count': 12, 'html_ids': len(ids),
                  'all_fragment_links_resolved': True, 'workers_confirmed_documents_synced': 4}, ensure_ascii=False))
