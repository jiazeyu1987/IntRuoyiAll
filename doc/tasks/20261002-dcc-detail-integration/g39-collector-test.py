"""Finite pure collection-mode boundary; no adapter/DB/backup is invoked."""
import importlib.util
import unittest
from pathlib import Path
from unittest.mock import Mock
import ast

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('g39_collector_tested',HERE/'g39-collector.py');c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)

class CollectionModeTest(unittest.TestCase):
    def test_only_explicit_existing_clone_can_collect_repeat_ready_first_state(self):
        c.validate_collection_state(c.CLONE,True,'REPEAT_ALLOWED')
        c.validate_collection_state(c.SOURCE,False,'FIRST_REQUIRED')
        c.validate_collection_state(c.CLONE,False,'FIRST_REQUIRED')
        for database,mode,state in [(c.SOURCE,True,'REPEAT_ALLOWED'),(c.CLONE,False,'REPEAT_ALLOWED'),(c.CLONE,True,'FIRST_REQUIRED'),(c.CLONE,1,'REPEAT_ALLOWED')]:
            with self.assertRaises(ValueError):c.validate_collection_state(database,mode,state)
    def test_wrong_source_resume_rejected_before_adapter_or_output_creation(self):
        adapter=Mock();target=c.PROTECTED/'g39-offline-never-created'
        with self.assertRaises(ValueError):c.collect_fresh_inputs(c.SOURCE,target,{},adapter,existing_first_resume=True)
        adapter.assert_not_called();self.assertFalse(target.exists())
    def test_new_collector_uses_versioned_driver_and_keeps_formal_backup_scopes(self):
        self.assertEqual('g39-driver.py',Path(c.g28.__file__).name)
        self.assertEqual('g39-schema.py',Path(c.g28.schema.__file__).name)
        self.assertIn('system_electronic_signature',c.TABLES)
        self.assertIn('--skip-extended-insert',c.dump_command(c.CLONE,'protected-original-rows','docker.exe'))
        self.assertEqual(7,len(c.TABLES))
    def test_collector_source_has_no_ddl_execute_resume_or_restore_call(self):
        tree=ast.parse((HERE/'g39-collector.py').read_text(encoding='utf-8'))
        forbidden={'execute','resume_reviewed_first','run_authorized_sql','restore_gzip'}
        self.assertFalse(any(isinstance(n,ast.Call) and isinstance(n.func,ast.Attribute) and n.func.attr in forbidden for n in ast.walk(tree)))

if __name__=='__main__':unittest.main()
