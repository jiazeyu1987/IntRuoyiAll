package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordCellRuleSupportTest {

    @Test
    void buildSuggestions_prefersColumnDateHeaderForDenseProcessGrid() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"工序名称"},
                        "1":{"text":"操作人员"},
                        "2":{"text":"装配日期"},
                        "3":{"text":"工序名称"},
                        "4":{"text":"操作人员"},
                        "5":{"text":"装配日期"}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"粗洗"},
                        "1":{"text":"","fillForm":{"field":"ebr_r1_c1","component":"Input","componentFlag":"input-text"}},
                        "2":{"text":"","fillForm":{"field":"ebr_r1_c2","component":"Input","componentFlag":"input-text"}},
                        "3":{"text":"硅化II"},
                        "4":{"text":"","fillForm":{"field":"ebr_r1_c4","component":"Input","componentFlag":"input-text"}},
                        "5":{"text":"","fillForm":{"field":"ebr_r1_c5","component":"Input","componentFlag":"input-text"}}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO leftDate = findRule(suggestions, 1, 2);
        BatchRecordReportCellRuleVO rightDate = findRule(suggestions, 1, 5);
        assertEquals("装配日期", leftDate.getLabel());
        assertEquals("DATE", leftDate.getValueType());
        assertEquals("装配日期", rightDate.getLabel());
        assertEquals("DATE", rightDate.getValueType());
    }

    @Test
    void toRuleJson_keepsAttachmentRuleForRequiredEvidence() {
        BatchRecordReportCellRuleVO rule = new BatchRecordReportCellRuleVO()
                .setRowIndex(1)
                .setColumnIndex(2)
                .setValueType("STRING")
                .setComponentFlag("upload-file")
                .setRequired(false)
                .setLabel("灭菌报告")
                .setConstraints(Map.of())
                .setAttachmentRule(Map.of(
                        "required", true,
                        "minCount", 1,
                        "attachmentType", "FILE",
                        "groupKey", "sterilization-report"))
                .setReviewed(true);

        JSONObject json = MesProBatchRecordCellRuleSupport.toRuleJson(rule);

        JSONObject attachmentRule = json.getJSONObject("attachmentRule");
        assertNotNull(attachmentRule);
        assertEquals(true, attachmentRule.getBoolean("required"));
        assertEquals(1, attachmentRule.getInteger("minCount"));
        assertEquals("FILE", attachmentRule.getString("attachmentType"));
        assertEquals("sterilization-report", attachmentRule.getString("groupKey"));
    }

    @Test
    void toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule() {
        BatchRecordReportCellRuleVO rule = new BatchRecordReportCellRuleVO()
                .setRowIndex(1)
                .setColumnIndex(2)
                .setValueType("NUMBER")
                .setComponentFlag("input-number")
                .setRequired(true)
                .setLabel("重量")
                .setConstraints(Map.of("min", 0, "max", 100))
                .setSource("AUTO")
                .setConfidence(0.84)
                .setReviewed(true);

        JSONObject json = MesProBatchRecordCellRuleSupport.toRuleJson(rule);

        assertEquals("MANUAL", json.getString("source"));
        assertEquals(true, json.getBoolean("reviewed"));
        assertTrue(MesProBatchRecordCellRuleSupport.isReviewedRule(json));
    }

    @Test
    void manualRulePlaceholder_roundTripsToRuleJsonAndFillForm() {
        BatchRecordReportCellRuleVO rule = new BatchRecordReportCellRuleVO()
                .setRowIndex(2)
                .setColumnIndex(4)
                .setValueType("STRING")
                .setComponentFlag("input-text")
                .setRequired(true)
                .setLabel("生产批号")
                .setPlaceholder("请输入生产批号")
                .setConstraints(Map.of())
                .setSource("MANUAL")
                .setConfidence(1.0)
                .setReviewed(true);

        JSONObject json = MesProBatchRecordCellRuleSupport.toRuleJson(rule);
        BatchRecordReportCellRuleVO roundTripped = MesProBatchRecordCellRuleSupport.toRuleVO(2, 4, json);
        JSONObject cell = JSON.parseObject("""
                {
                  "text":"",
                  "fillForm":{"field":"ebr_r2_c4","component":"Input","componentFlag":"input-text"}
                }
                """);

        MesProBatchRecordCellRuleSupport.ensureManualFillForm(rule, cell, "REPORT-PROMPT");

        assertEquals("请输入生产批号", json.getString("placeholder"));
        assertEquals("请输入生产批号", roundTripped.getPlaceholder());
        assertEquals("请输入生产批号", cell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("placeholder"));
    }

    @Test
    void manualRuleHelpText_roundTripsToRuleJsonAndFillForm() {
        BatchRecordReportCellRuleVO rule = new BatchRecordReportCellRuleVO()
                .setRowIndex(2)
                .setColumnIndex(4)
                .setValueType("STRING")
                .setComponentFlag("input-text")
                .setRequired(true)
                .setLabel("生产批号")
                .setPlaceholder("请输入生产批号")
                .setHelpText("填写当前批次对应的生产批号，需与工单批号保持一致")
                .setConstraints(Map.of())
                .setSource("MANUAL")
                .setConfidence(1.0)
                .setReviewed(true);

        JSONObject json = MesProBatchRecordCellRuleSupport.toRuleJson(rule);
        BatchRecordReportCellRuleVO roundTripped = MesProBatchRecordCellRuleSupport.toRuleVO(2, 4, json);
        JSONObject cell = JSON.parseObject("""
                {
                  "text":"",
                  "fillForm":{"field":"ebr_r2_c4","component":"Input","componentFlag":"input-text"}
                }
                """);

        MesProBatchRecordCellRuleSupport.ensureManualFillForm(rule, cell, "REPORT-HELP");

        assertEquals("填写当前批次对应的生产批号，需与工单批号保持一致", json.getString("helpText"));
        assertEquals("填写当前批次对应的生产批号，需与工单批号保持一致", roundTripped.getHelpText());
        assertEquals("填写当前批次对应的生产批号，需与工单批号保持一致", cell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("helpText"));
    }

    @Test
    void buildSuggestions_preservesExistingFillFormPlaceholder() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{"cells":{"0":{"text":"生产批号"}}},
                    "1":{"cells":{
                      "0":{
                        "text":"",
                        "fillForm":{
                          "field":"ebr_r1_c0",
                          "component":"Input",
                          "componentFlag":"input-text",
                          "placeholder":"请输入生产批号"
                        }
                      }
                    }}
                  }
                }
                """);

        BatchRecordReportCellRuleVO rule = findRule(MesProBatchRecordCellRuleSupport.buildSuggestions(root), 1, 0);

        assertEquals("请输入生产批号", rule.getPlaceholder());
    }

    @Test
    void buildSuggestions_generatesHelpTextForAutoFieldRecognitionWithoutReviewing() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{"cells":{"0":{"text":"生产批号"}}},
                    "1":{"cells":{
                      "0":{
                        "text":"",
                        "fillForm":{
                          "field":"ebr_r1_c0",
                          "component":"Input",
                          "componentFlag":"input-text",
                          "placeholder":"请输入生产批号"
                        }
                      }
                    }}
                  }
                }
                """);

        BatchRecordReportCellRuleVO rule = findRule(MesProBatchRecordCellRuleSupport.buildSuggestions(root), 1, 0);

        assertEquals("生产批号", rule.getLabel());
        assertNotNull(rule.getHelpText());
        assertTrue(rule.getHelpText().contains("生产批号"));
        assertEquals("AUTO", rule.getSource());
        assertEquals(false, rule.getReviewed());
    }

    @Test
    void buildAutoCheckboxRule_keepsSharedAutoCheckboxDefaults() {
        BatchRecordReportCellRuleVO rule = MesProBatchRecordCellRuleSupport
                .buildAutoCheckboxRule(2, 3, "□符合要求");

        assertEquals(2, rule.getRowIndex());
        assertEquals(3, rule.getColumnIndex());
        assertEquals("BOOLEAN", rule.getValueType());
        assertEquals("checkbox", rule.getComponentFlag());
        assertEquals("符合要求", rule.getLabel());
        assertEquals("AUTO", rule.getSource());
        assertEquals(0.92, rule.getConfidence());
        assertEquals(false, rule.getReviewed());
    }

    @Test
    void defaultFillValue_returnsBooleanFalseOnlyForBooleanRules() {
        assertEquals(Boolean.FALSE, MesProBatchRecordCellRuleSupport.defaultFillValue("BOOLEAN"));
        assertEquals("", MesProBatchRecordCellRuleSupport.defaultFillValue("STRING"));
        assertEquals("", MesProBatchRecordCellRuleSupport.defaultFillValue("DATE"));
    }

    @Test
    void buildSuggestions_includesStructuredHeaderBlankCellsWithEntryCues() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"设备编码"},
                        "1":{"text":"设备名称"},
                        "2":{"text":"设备型号"},
                        "3":{"text":"设备编号"},
                        "4":{"text":"是否在计量效期内"},
                        "5":{"text":"","fillForm":{"field":"ebr_r0_c5","component":"Input","componentFlag":"input-text"}},
                        "6":{"text":"操作人"},
                        "7":{"text":"","fillForm":{"field":"ebr_r0_c7","component":"Input","componentFlag":"input-text"}},
                        "8":{"text":"复核人"},
                        "9":{"text":"","fillForm":{"field":"ebr_r0_c9","component":"Input","componentFlag":"input-text"}}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        assertTrue(suggestions.stream().anyMatch(item -> item.getRowIndex() == 0 && item.getColumnIndex() == 5),
                "entry-cue blank should appear in suggestions");
        assertEquals("BOOLEAN", findRule(suggestions, 0, 5).getValueType());
        assertEquals("操作人", findRule(suggestions, 0, 7).getLabel());
        assertEquals("复核人", findRule(suggestions, 0, 9).getLabel());
    }

    @Test
    void buildSuggestions_includesStaticCheckboxTextCellsAsBooleanCandidates() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"操作日期"},
                        "1":{"text":"物料编码"},
                        "2":{"text":"物料名称"},
                        "3":{"text":"批号"}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":""},
                        "1":{"text":"/"},
                        "2":{"text":"□30atm压力表"},
                        "3":{"text":""}
                      }
                    },
                    "2":{
                      "cells":{
                        "0":{"text":""},
                        "1":{"text":"/"},
                        "2":{"text":"□40atm压力表"},
                        "3":{"text":""}
                      }
                    },
                    "3":{
                      "cells":{
                        "0":{"text":"检查结果"},
                        "1":{"text":"□是 □否"}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO pressure30 = findRule(suggestions, 1, 2);
        assertEquals("BOOLEAN", pressure30.getValueType());
        assertEquals("checkbox", pressure30.getComponentFlag());
        assertEquals("30atm压力表", pressure30.getLabel());
        assertEquals(3, MesProBatchRecordCellRuleSupport.countUnreviewedFillableCells(root));

        JSONObject checkboxCell = MesProBatchRecordCellRuleSupport.requireCell(root, 1, 2);
        MesProBatchRecordCellRuleSupport.ensureManualFillForm(pressure30, checkboxCell, "REPORT-CHECK");
        JSONObject fillForm = checkboxCell.getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY);
        assertEquals("checkbox", fillForm.getString("componentFlag"));
        assertEquals(Boolean.FALSE, fillForm.get("value"));
        assertEquals(Boolean.FALSE, fillForm.get("defaultValue"));
    }

    @Test
    void buildSuggestions_includesMultilineStaticCheckboxTextCellsAsBooleanCandidates() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"检查要求"},
                        "1":{"text":"结果"},
                        "2":{"text":"操作人/日期"}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"工作场所无上批遗留的产品、文件或与本批产品生产无关的物料。"},
                        "1":{"text":"□符合要求\\n□不符合要求"},
                        "2":{"text":""}
                      }
                    },
                    "2":{
                      "cells":{
                        "0":{"text":"损耗描述：\\n不合格日期\\n处置方式\\n\\n□报废  □其他：________________\\n□报废  □其他：________________"},
                        "1":{"text":"封口热合机：□A05199"},
                        "2":{"text":"☑关键/特殊工序"}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO resultRule = findRule(suggestions, 1, 1);
        assertEquals("STRING", resultRule.getValueType());
        assertEquals("radio-group", resultRule.getComponentFlag());
        assertEquals("检测结果", resultRule.getLabel());
        assertEquals("single", resultRule.getConstraints().get("selectionMode"));
        assertEquals(List.of(
                Map.of("label", "符合要求", "value", "符合要求"),
                Map.of("label", "不符合要求", "value", "不符合要求")),
                resultRule.getConstraints().get("options"));

        int appliedCount = MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-CHECK");
        assertEquals(1, appliedCount);
        JSONObject resultCell = MesProBatchRecordCellRuleSupport.requireCell(root, 1, 1);
        assertEquals("radio-group", resultCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("componentFlag"));
        assertEquals("", resultCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .get("defaultValue"));
        assertEquals("符合要求", resultCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getJSONArray("options")
                .getJSONObject(0)
                .getString("label"));
        assertEquals(false, resultCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getBoolean("reviewed"));

        assertTrue(!MesProBatchRecordCellRuleSupport.requireCell(root, 2, 0)
                .containsKey(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY));
        assertTrue(!MesProBatchRecordCellRuleSupport.requireCell(root, 2, 1)
                .containsKey(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY));
        assertTrue(!MesProBatchRecordCellRuleSupport.requireCell(root, 2, 2)
                .containsKey(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY));
    }

    @Test
    void buildSuggestions_doesNotPromoteSignatureDateColumnCheckboxFragments() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"检查要求","merge":[0,1]},
                        "2":{"text":"结果"},
                        "3":{"text":"操作人/日期"},
                        "4":{"text":"复核人/日期"}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"工作场所检查","merge":[0,1]},
                        "2":{"text":"□符合要求"},
                        "3":{"text":"□不符合要求"},
                        "4":{"text":""}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO resultRule = findRule(suggestions, 1, 2);
        BatchRecordReportCellRuleVO operatorDateRule = findRule(suggestions, 1, 3);
        assertEquals("BOOLEAN", resultRule.getValueType());
        assertEquals("checkbox", resultRule.getComponentFlag());
        assertEquals("STRING", operatorDateRule.getValueType());
        assertEquals("input-text", operatorDateRule.getComponentFlag());
        assertEquals("操作人/日期", operatorDateRule.getLabel());

        MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-SIGNATURE-DATE");
        JSONObject resultCell = MesProBatchRecordCellRuleSupport.requireCell(root, 1, 2);
        JSONObject operatorDateCell = MesProBatchRecordCellRuleSupport.requireCell(root, 1, 3);
        assertEquals("checkbox", resultCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("componentFlag"));
        assertEquals("input-text", operatorDateCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("componentFlag"));
        assertFalse("checkbox".equals(operatorDateCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getString("componentFlag")));
    }

    @Test
    void buildSuggestions_doesNotPromoteMisalignedSignatureDateTailCheckboxFragments() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"检查要求","merge":[0,1]},
                        "2":{"text":"结果"},
                        "4":{"text":"操作人/日期"},
                        "6":{"text":"复核人/日期"}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"工作场所检查","merge":[0,1]},
                        "2":{"text":"□符合要求"},
                        "5":{"text":"□不符合要求"},
                        "6":{"text":""}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO resultRule = findRule(suggestions, 1, 2);
        BatchRecordReportCellRuleVO shiftedSignatureDateRule = findRule(suggestions, 1, 5);
        assertEquals("BOOLEAN", resultRule.getValueType());
        assertEquals("checkbox", resultRule.getComponentFlag());
        assertEquals("STRING", shiftedSignatureDateRule.getValueType());
        assertEquals("input-text", shiftedSignatureDateRule.getComponentFlag());
        assertEquals("操作人/日期", shiftedSignatureDateRule.getLabel());

        MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-SIGNATURE-DATE-OFFSET");
        JSONObject shiftedSignatureDateCell = MesProBatchRecordCellRuleSupport.requireCell(root, 1, 5);
        assertEquals("input-text", shiftedSignatureDateCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("componentFlag"));
        assertFalse("checkbox".equals(shiftedSignatureDateCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getString("componentFlag")));
    }

    @Test
    void buildSuggestions_includesAlphanumericCodeCheckboxGroups() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"光固机设备编码"},
                        "1":{"text":"是否在计量效期内"}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"□A05075\\n(A05059"},
                        "1":{"text":"□是 □否"}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO equipmentCodeRule = findRule(suggestions, 1, 0);
        assertEquals("BOOLEAN", equipmentCodeRule.getValueType());
        assertEquals("checkbox", equipmentCodeRule.getComponentFlag());
        assertEquals("A05075 (A05059", equipmentCodeRule.getLabel());

        int appliedCount = MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-LIGHT-CURE");
        assertEquals(2, appliedCount);
        JSONObject equipmentCodeCell = MesProBatchRecordCellRuleSupport.requireCell(root, 1, 0);
        assertEquals("checkbox", equipmentCodeCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("componentFlag"));
        assertEquals(Boolean.FALSE, equipmentCodeCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .get("defaultValue"));
    }

    @Test
    void applyAutomaticSuggestions_promotesStaticCheckboxTextAsUnreviewedAutoRules() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"物料名称"},
                        "1":{"text":"□30atm压力表"},
                        "2":{"text":"封口热合机：□A05199"},
                        "3":{"text":"☑关键/特殊工序"}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"结果"},
                        "1":{"text":"□是 □否"}
                      }
                    }
                  }
                }
                """);

        int appliedCount = MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-CHECK");
        assertEquals(2, appliedCount);

        JSONObject pressureCell = MesProBatchRecordCellRuleSupport.requireCell(root, 0, 1);
        JSONObject pressureFillForm = pressureCell.getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY);
        JSONObject pressureRule = pressureCell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertNotNull(pressureFillForm);
        assertNotNull(pressureRule);
        assertEquals("checkbox", pressureFillForm.getString("componentFlag"));
        assertEquals(Boolean.FALSE, pressureFillForm.get("value"));
        assertEquals("30atm压力表", pressureFillForm.getString("labelText"));
        assertEquals("BOOLEAN", pressureRule.getString("valueType"));
        assertEquals("checkbox", pressureRule.getString("componentFlag"));
        assertEquals(false, pressureRule.getBoolean("reviewed"));

        JSONObject yesNoCell = MesProBatchRecordCellRuleSupport.requireCell(root, 1, 1);
        JSONObject yesNoFillForm = yesNoCell.getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY);
        JSONObject yesNoRule = yesNoCell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertEquals("radio-group", yesNoFillForm.getString("componentFlag"));
        assertEquals("", yesNoFillForm.get("defaultValue"));
        assertEquals("结果", yesNoRule.getString("label"));
        assertEquals("single", yesNoRule.getJSONObject("constraints").getString("selectionMode"));
        assertEquals("是", yesNoRule.getJSONObject("constraints")
                .getJSONArray("options").getJSONObject(0).getString("label"));
        assertEquals("否", yesNoRule.getJSONObject("constraints")
                .getJSONArray("options").getJSONObject(1).getString("label"));
        assertEquals(false, yesNoRule.getBoolean("reviewed"));

        assertTrue(!MesProBatchRecordCellRuleSupport.requireCell(root, 0, 2)
                .containsKey(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY));
        assertTrue(!MesProBatchRecordCellRuleSupport.requireCell(root, 0, 3)
                .containsKey(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY));
    }

    @Test
    void applyAutomaticSuggestions_rewritesLegacyAutoReviewedRulesAsUnreviewedSuggestions() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"处置方式"},
                        "1":{"text":"□报废",
                             "edhrCellRule":{"rowIndex":0,"columnIndex":1,"valueType":"BOOLEAN","componentFlag":"checkbox","required":false,"label":"报废","constraints":{},"source":"AUTO","confidence":0.92,"reviewed":true}},
                        "2":{"text":"","fillForm":{"field":"ebr_r0_c2","component":"Input","componentFlag":"input-text"},
                             "edhrCellRule":{"rowIndex":0,"columnIndex":2,"valueType":"STRING","componentFlag":"input-text","required":true,"label":"人工规则","constraints":{"maxLength":20},"source":"MANUAL","confidence":1.0,"reviewed":true}}
                      }
                    }
                  }
                }
                """);

        int appliedCount = MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-CHECK");

        assertEquals(1, appliedCount);
        JSONObject legacyAutoRule = MesProBatchRecordCellRuleSupport.requireCell(root, 0, 1)
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertEquals("AUTO", legacyAutoRule.getString("source"));
        assertEquals(false, legacyAutoRule.getBoolean("reviewed"));
        assertEquals("BOOLEAN", legacyAutoRule.getString("valueType"));
        assertEquals("checkbox", legacyAutoRule.getString("componentFlag"));

        JSONObject manualRule = MesProBatchRecordCellRuleSupport.requireCell(root, 0, 2)
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertEquals("MANUAL", manualRule.getString("source"));
        assertEquals(true, manualRule.getBoolean("reviewed"));
        assertEquals(20, manualRule.getJSONObject("constraints").getInteger("maxLength"));
    }

    @Test
    void applyAutomaticSuggestions_marksAutoSignatureRulesAsUnreviewedSuggestions() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"批准人/日期：",
                             "fillForm":{"field":"ebr_r0_c0","component":"Input","componentFlag":"signature"},
                             "edhrSignature":{"enabled":true,"actionType":"APPROVE","label":"批准人/日期：","displayFormat":"ACTOR_SIGNED_AT"}}
                      }
                    }
                  }
                }
                """);

        int appliedCount = MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-SIGNATURE");

        assertEquals(1, appliedCount);
        JSONObject signatureRule = MesProBatchRecordCellRuleSupport.requireCell(root, 0, 0)
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertEquals("AUTO", signatureRule.getString("source"));
        assertEquals("SIGNATURE", signatureRule.getString("valueType"));
        assertEquals("signature", signatureRule.getString("componentFlag"));
        assertEquals(false, signatureRule.getBoolean("reviewed"));
    }

    @Test
    void normalizeAutomaticRulesAsUnreviewed_preservesManualReviewedRules() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"□报废",
                             "edhrCellRule":{"rowIndex":0,"columnIndex":0,"valueType":"BOOLEAN","componentFlag":"checkbox","required":false,"label":"报废","constraints":{},"source":"AUTO","confidence":0.92,"reviewed":true}},
                        "1":{"text":"",
                             "edhrCellRule":{"rowIndex":0,"columnIndex":1,"valueType":"STRING","componentFlag":"input-text","required":true,"label":"人工规则","constraints":{"maxLength":20},"source":"MANUAL","confidence":1.0,"reviewed":true}}
                      }
                    }
                  }
                }
                """);

        int normalizedCount = MesProBatchRecordCellRuleSupport.normalizeAutomaticRulesAsUnreviewed(root);

        assertEquals(1, normalizedCount);
        assertEquals(false, MesProBatchRecordCellRuleSupport.requireCell(root, 0, 0)
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getBoolean("reviewed"));
        assertEquals(true, MesProBatchRecordCellRuleSupport.requireCell(root, 0, 1)
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getBoolean("reviewed"));
    }

    @Test
    void buildSuggestions_infersCommonFormatAndRangeConstraintsAcrossBatchAndExtraForms() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"生产数量（pcs）"},
                        "1":{"text":"","fillForm":{"field":"ebr_r0_c1","component":"Input","componentFlag":"input-text"}},
                        "2":{"text":"操作日期"},
                        "3":{"text":"","fillForm":{"field":"ebr_r0_c3","component":"Input","componentFlag":"input-text"}}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"温度(℃)"},
                        "1":{"text":"","fillForm":{"field":"ebr_r1_c1","component":"Input","componentFlag":"input-text"}},
                        "2":{"text":"压力(MPa)"},
                        "3":{"text":"","fillForm":{"field":"ebr_r1_c3","component":"Input","componentFlag":"input-text"}}
                      }
                    },
                    "2":{
                      "cells":{
                        "0":{"text":"损耗数量"},
                        "1":{"text":"","fillForm":{"field":"ebr_r2_c1","component":"Input","componentFlag":"input-text"}},
                        "2":{"text":"处置描述"},
                        "3":{"text":"","fillForm":{"field":"ebr_r2_c3","component":"Input","componentFlag":"input-textarea"}}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO productionQuantity = findRule(suggestions, 0, 1);
        assertEquals("NUMBER", productionQuantity.getValueType());
        assertEquals("pcs", productionQuantity.getUnit());
        assertEquals(0, productionQuantity.getConstraints().get("min"));
        assertEquals(0, productionQuantity.getConstraints().get("scale"));

        BatchRecordReportCellRuleVO operationDate = findRule(suggestions, 0, 3);
        assertEquals("DATE", operationDate.getValueType());
        assertEquals("yyyy-MM-dd", operationDate.getConstraints().get("format"));

        BatchRecordReportCellRuleVO temperature = findRule(suggestions, 1, 1);
        assertEquals("NUMBER", temperature.getValueType());
        assertEquals("℃", temperature.getUnit());
        assertEquals(-50, temperature.getConstraints().get("min"));
        assertEquals(200, temperature.getConstraints().get("max"));
        assertEquals(1, temperature.getConstraints().get("scale"));

        BatchRecordReportCellRuleVO pressure = findRule(suggestions, 1, 3);
        assertEquals("NUMBER", pressure.getValueType());
        assertEquals("MPa", pressure.getUnit());
        assertEquals(0, pressure.getConstraints().get("min"));
        assertEquals(100, pressure.getConstraints().get("max"));
        assertEquals(3, pressure.getConstraints().get("scale"));

        BatchRecordReportCellRuleVO lossQuantity = findRule(suggestions, 2, 1);
        assertEquals("NUMBER", lossQuantity.getValueType());
        assertEquals(0, lossQuantity.getConstraints().get("min"));
        assertEquals(0, lossQuantity.getConstraints().get("scale"));

        BatchRecordReportCellRuleVO disposalDescription = findRule(suggestions, 2, 3);
        assertEquals("STRING", disposalDescription.getValueType());
        assertEquals("input-textarea", disposalDescription.getComponentFlag());
        assertEquals(1000, disposalDescription.getConstraints().get("maxLength"));
    }

    @Test
    void buildSuggestions_infersProcessInspectionAndParameterRecordFormats() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"检验日期"},
                        "1":{"text":"","fillForm":{"field":"ebr_r0_c1","component":"Input","componentFlag":"input-text"}},
                        "2":{"text":"检测数量"},
                        "3":{"text":"","fillForm":{"field":"ebr_r0_c3","component":"Input","componentFlag":"input-text"}}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"检测结果"},
                        "1":{"text":"□符合要求  □不符合要求____"},
                        "2":{"text":"不合格评审报告编号"},
                        "3":{"text":"","fillForm":{"field":"ebr_r1_c3","component":"Input","componentFlag":"input-text"}}
                      }
                    },
                    "2":{
                      "cells":{
                        "0":{"text":"运行时长"},
                        "1":{"text":"","fillForm":{"field":"ebr_r2_c1","component":"Input","componentFlag":"input-text"}},
                        "2":{"text":"设定温度(℃)"},
                        "3":{"text":"","fillForm":{"field":"ebr_r2_c3","component":"Input","componentFlag":"input-text"}}
                      }
                    },
                    "3":{
                      "cells":{
                        "0":{"text":"自检合格数量"},
                        "1":{"text":"","fillForm":{"field":"ebr_r3_c1","component":"Input","componentFlag":"input-text"}}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO inspectionDate = findRule(suggestions, 0, 1);
        assertEquals("DATE", inspectionDate.getValueType());
        assertEquals("yyyy-MM-dd", inspectionDate.getConstraints().get("format"));

        BatchRecordReportCellRuleVO inspectionQuantity = findRule(suggestions, 0, 3);
        assertEquals("NUMBER", inspectionQuantity.getValueType());
        assertEquals(0, inspectionQuantity.getConstraints().get("min"));
        assertEquals(0, inspectionQuantity.getConstraints().get("scale"));

        BatchRecordReportCellRuleVO inspectionResult = findRule(suggestions, 1, 1);
        assertEquals("STRING", inspectionResult.getValueType());
        assertEquals("radio-group", inspectionResult.getComponentFlag());
        assertEquals("检测结果", inspectionResult.getLabel());

        BatchRecordReportCellRuleVO reviewReportCode = findRule(suggestions, 1, 3);
        assertEquals("STRING", reviewReportCode.getValueType());
        assertEquals(128, reviewReportCode.getConstraints().get("maxLength"));

        BatchRecordReportCellRuleVO duration = findRule(suggestions, 2, 1);
        assertEquals("NUMBER", duration.getValueType());
        assertEquals(0, duration.getConstraints().get("min"));
        assertEquals(2, duration.getConstraints().get("scale"));
        assertEquals(8, duration.getConstraints().get("precision"));

        BatchRecordReportCellRuleVO configuredTemperature = findRule(suggestions, 2, 3);
        assertEquals("NUMBER", configuredTemperature.getValueType());
        assertEquals(-50, configuredTemperature.getConstraints().get("min"));
        assertEquals(200, configuredTemperature.getConstraints().get("max"));

        BatchRecordReportCellRuleVO qualifiedQuantity = findRule(suggestions, 3, 1);
        assertEquals("NUMBER", qualifiedQuantity.getValueType());
        assertEquals(0, qualifiedQuantity.getConstraints().get("scale"));
    }

    @Test
    void buildSuggestions_distinguishesTimePointFieldsFromDurationFields() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"操作时间"},
                        "1":{"text":"","fillForm":{"field":"ebr_r0_c1","component":"Input","componentFlag":"input-text"}}
                      }
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"运行时长"},
                        "1":{"text":"","fillForm":{"field":"ebr_r1_c1","component":"Input","componentFlag":"input-text"}}
                      }
                    },
                    "2":{
                      "cells":{
                        "0":{"text":"保压时间(min)"},
                        "1":{"text":"","fillForm":{"field":"ebr_r2_c1","component":"Input","componentFlag":"input-text"}}
                      }
                    }
                  }
                }
                """);

        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);

        BatchRecordReportCellRuleVO operationTime = findRule(suggestions, 0, 1);
        assertEquals("DATETIME", operationTime.getValueType());
        assertEquals("yyyy-MM-dd HH:mm:ss", operationTime.getConstraints().get("format"));

        BatchRecordReportCellRuleVO duration = findRule(suggestions, 1, 1);
        assertEquals("NUMBER", duration.getValueType());
        assertEquals(0, duration.getConstraints().get("min"));
        assertEquals(2, duration.getConstraints().get("scale"));

        BatchRecordReportCellRuleVO pressureHoldDuration = findRule(suggestions, 2, 1);
        assertEquals("NUMBER", pressureHoldDuration.getValueType());
        assertEquals("min", pressureHoldDuration.getUnit());
        assertEquals(2, pressureHoldDuration.getConstraints().get("scale"));
    }

    @Test
    void applyAutomaticSuggestions_setsRulesAfterWordImportWithoutMarkingUserReviewed() {
        JSONObject root = JSON.parseObject("""
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"生产数量（pcs）"},
                        "1":{"text":"","fillForm":{"field":"ebr_r0_c1","component":"Input","componentFlag":"input-text"}},
                        "2":{"text":"操作日期"},
                        "3":{"text":"","fillForm":{"field":"ebr_r0_c3","component":"Input","componentFlag":"input-text"}},
                        "4":{"text":"已人工确认"},
                        "5":{"text":"","fillForm":{"field":"ebr_r0_c5","component":"Input","componentFlag":"input-text"},
                             "edhrCellRule":{"rowIndex":0,"columnIndex":5,"valueType":"STRING","componentFlag":"input-text","required":true,"label":"人工规则","constraints":{"maxLength":20},"source":"MANUAL","confidence":1.0,"reviewed":true}}
                      }
                    }
                  }
                }
                """);

        int appliedCount = MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions(root, "REPORT-AUTO");

        assertEquals(2, appliedCount);
        JSONObject quantityCell = MesProBatchRecordCellRuleSupport.requireCell(root, 0, 1);
        JSONObject quantityRule = quantityCell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertEquals("NUMBER", quantityRule.getString("valueType"));
        assertEquals("input-number", quantityRule.getString("componentFlag"));
        assertEquals(false, quantityRule.getBoolean("reviewed"));
        assertEquals(0, quantityRule.getJSONObject("constraints").getInteger("min"));
        assertEquals("input-number", quantityCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.FILL_FORM_KEY)
                .getString("componentFlag"));

        JSONObject dateCell = MesProBatchRecordCellRuleSupport.requireCell(root, 0, 3);
        JSONObject dateRule = dateCell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertEquals("DATE", dateRule.getString("valueType"));
        assertEquals("yyyy-MM-dd", dateRule.getJSONObject("constraints").getString("format"));

        JSONObject reviewedRule = MesProBatchRecordCellRuleSupport.requireCell(root, 0, 5)
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertEquals("人工规则", reviewedRule.getString("label"));
        assertEquals(20, reviewedRule.getJSONObject("constraints").getInteger("maxLength"));
    }

    private BatchRecordReportCellRuleVO findRule(List<BatchRecordReportCellRuleVO> suggestions,
                                                 int rowIndex,
                                                 int columnIndex) {
        BatchRecordReportCellRuleVO rule = suggestions.stream()
                .filter(item -> item.getRowIndex() == rowIndex && item.getColumnIndex() == columnIndex)
                .findFirst()
                .orElse(null);
        assertNotNull(rule, "missing suggestion for row " + rowIndex + " column " + columnIndex);
        return rule;
    }
}
