import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
spec=importlib.util.spec_from_file_location('package',Path(__file__).resolve().parents[1]/'runtime/backend-package.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class ProvenanceTest(unittest.TestCase):
 def test_mutated_added_deleted_source_and_replaced_jar_are_rejected(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp); (root/'pom.xml').write_text('<project/>'); source=root/'src/main/A.java';source.parent.mkdir(parents=True);source.write_text('original')
   jar=root/'app.jar';jar.write_bytes(b'package')
   receipt=jar.with_suffix('.provenance.json')
   with self.assertRaisesRegex(RuntimeError,'Missing'):m.verify(root,jar)
   receipt.write_text(json.dumps(dict(version=1,sourceSha256=m.fingerprint(root),jarSha256=m.digest(jar))))
   m.verify(root,jar)
   source.write_text('changed')
   with self.assertRaisesRegex(RuntimeError,'source differs'):m.verify(root,jar)
   source.write_text('original');extra=source.with_name('B.java');extra.write_text('added')
   with self.assertRaisesRegex(RuntimeError,'source differs'):m.verify(root,jar)
   extra.unlink();source.unlink()
   with self.assertRaises(RuntimeError):m.verify(root,jar)
   source.write_text('original');jar.write_bytes(b'other package')
   with self.assertRaisesRegex(RuntimeError,'package differs'):m.verify(root,jar)
if __name__=='__main__':unittest.main()
