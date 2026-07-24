import argparse
import re
import sys

import pymysql


PROCESS_DEFINITION_KEY = "dcc-controlled-file-approval"
EXPECTED_STAGE_NAMES = {
    "DOC_CONTROL_REVIEW": "文控审核",
    "MATRIX_REVIEW": "审核会签",
    "MATRIX_APPROVAL": "批准",
    "DOC_CONTROL_APPROVAL": "文控批准",
}


def fetch_latest_model_source(connection):
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT m.ID_, m.KEY_, b.ID_, CONVERT(b.BYTES_ USING utf8mb4) AS xml_text
            FROM ACT_RE_MODEL m
            JOIN ACT_GE_BYTEARRAY b ON b.ID_ = m.EDITOR_SOURCE_VALUE_ID_
            WHERE m.KEY_ = %s
            ORDER BY m.CREATE_TIME_ DESC, m.VERSION_ DESC
            LIMIT 1
            """,
            (PROCESS_DEFINITION_KEY,),
        )
        return cursor.fetchone()


def fetch_latest_deployment_source(connection):
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT d.ID_, d.VERSION_, d.DEPLOYMENT_ID_, b.ID_, b.NAME_,
                   CONVERT(b.BYTES_ USING utf8mb4) AS xml_text
            FROM ACT_RE_PROCDEF d
            JOIN ACT_GE_BYTEARRAY b
              ON b.DEPLOYMENT_ID_ = d.DEPLOYMENT_ID_
             AND b.NAME_ = d.RESOURCE_NAME_
            WHERE d.KEY_ = %s
            ORDER BY d.VERSION_ DESC
            LIMIT 1
            """,
            (PROCESS_DEFINITION_KEY,),
        )
        return cursor.fetchone()


def fetch_active_tasks(connection):
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT t.ID_, t.NAME_, t.TASK_DEF_KEY_, d.ID_ AS proc_def_id, d.VERSION_ AS proc_def_version
            FROM ACT_RU_TASK t
            JOIN ACT_RE_PROCDEF d ON d.ID_ = t.PROC_DEF_ID_
            WHERE d.KEY_ = %s
            ORDER BY t.CREATE_TIME_ DESC
            """,
            (PROCESS_DEFINITION_KEY,),
        )
        return cursor.fetchall()


def fetch_historic_tasks(connection):
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT t.ID_, t.NAME_, t.TASK_DEF_KEY_, d.ID_ AS proc_def_id, d.VERSION_ AS proc_def_version
            FROM ACT_HI_TASKINST t
            JOIN ACT_RE_PROCDEF d ON d.ID_ = t.PROC_DEF_ID_
            WHERE d.KEY_ = %s
            ORDER BY t.START_TIME_ DESC
            """,
            (PROCESS_DEFINITION_KEY,),
        )
        return cursor.fetchall()


def fetch_historic_activities(connection):
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT a.ID_, a.ACT_NAME_, a.ACT_ID_, d.ID_ AS proc_def_id, d.VERSION_ AS proc_def_version
            FROM ACT_HI_ACTINST a
            JOIN ACT_RE_PROCDEF d ON d.ID_ = a.PROC_DEF_ID_
            WHERE d.KEY_ = %s
              AND a.ACT_TYPE_ = 'userTask'
            ORDER BY a.START_TIME_ DESC
            """,
            (PROCESS_DEFINITION_KEY,),
        )
        return cursor.fetchall()


def validate_xml(label, xml_text):
    errors = []
    if not xml_text:
        errors.append(f"{label}: missing BPMN XML bytes")
        return errors
    for task_def_key, expected_name in EXPECTED_STAGE_NAMES.items():
        expected_fragment = f'<userTask id="{task_def_key}" name="{expected_name}">'
        if expected_fragment not in xml_text:
            errors.append(f"{label}: missing expected fragment {expected_fragment}")
        placeholder_pattern = (
            rf'<userTask id="{re.escape(task_def_key)}" name="[^"]*\?[^"]*">'
        )
        if re.search(placeholder_pattern, xml_text):
            errors.append(
                f"{label}: task {task_def_key} still contains question-mark placeholders"
            )
    return errors


def validate_active_tasks(tasks):
    errors = []
    bad_tasks = []
    for task_id, task_name, task_def_key, proc_def_id, proc_def_version in tasks:
        if not task_name:
            bad_tasks.append(
                (task_id, task_def_key, proc_def_id, proc_def_version, "<empty>")
            )
            continue
        if "?" in task_name:
            bad_tasks.append(
                (task_id, task_def_key, proc_def_id, proc_def_version, task_name)
            )
    if bad_tasks:
        errors.append("active DCC tasks still contain garbled names:")
        errors.extend(
            [
                f"  task={task_id} key={task_def_key} procDef={proc_def_id}@v{proc_def_version} name={task_name}"
                for task_id, task_def_key, proc_def_id, proc_def_version, task_name in bad_tasks
            ]
        )
    return errors


def validate_historic_records(label, rows):
    errors = []
    bad_rows = []
    for record_id, record_name, activity_key, proc_def_id, proc_def_version in rows:
        if not record_name:
            bad_rows.append(
                (record_id, activity_key, proc_def_id, proc_def_version, "<empty>")
            )
            continue
        if "?" in record_name:
            bad_rows.append(
                (record_id, activity_key, proc_def_id, proc_def_version, record_name)
            )
    if bad_rows:
        errors.append(f"{label} still contain garbled names:")
        errors.extend(
            [
                f"  id={record_id} key={activity_key} procDef={proc_def_id}@v{proc_def_version} name={record_name}"
                for record_id, activity_key, proc_def_id, proc_def_version, record_name in bad_rows
            ]
        )
    return errors


def main():
    parser = argparse.ArgumentParser(
        description="Fail when DCC approval-task names or BPMN XML bytes still contain garbled placeholders."
    )
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=23306)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--database", default="ruoyi-vue-pro")
    args = parser.parse_args()

    connection = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
    )
    try:
        model_source = fetch_latest_model_source(connection)
        deployment_source = fetch_latest_deployment_source(connection)
        active_tasks = fetch_active_tasks(connection)
        historic_tasks = fetch_historic_tasks(connection)
        historic_activities = fetch_historic_activities(connection)
    finally:
        connection.close()

    errors = []
    if not model_source:
        errors.append(f"missing Flowable model source for key {PROCESS_DEFINITION_KEY}")
    else:
        errors.extend(validate_xml("model editor source", model_source[3]))
    if not deployment_source:
        errors.append(f"missing latest deployment source for key {PROCESS_DEFINITION_KEY}")
    else:
        errors.extend(validate_xml("latest deployed BPMN resource", deployment_source[5]))
    errors.extend(validate_active_tasks(active_tasks))
    errors.extend(validate_historic_records("historic DCC tasks", historic_tasks))
    errors.extend(validate_historic_records("historic DCC user-task activities", historic_activities))

    if errors:
        print("FAIL: DCC approval-task names are still garbled")
        for error in errors:
            print(error)
        sys.exit(1)

    print("PASS: DCC approval-task names and BPMN XML bytes are readable")
    if model_source:
        print(f"  model source id: {model_source[0]} / bytearray: {model_source[2]}")
    if deployment_source:
        print(
            f"  latest deployment: {deployment_source[0]} / version: {deployment_source[1]} / bytearray: {deployment_source[3]}"
        )
    if active_tasks:
        print(f"  active DCC tasks checked: {len(active_tasks)}")
    else:
        print("  active DCC tasks checked: 0")
    print(f"  historic DCC tasks checked: {len(historic_tasks)}")
    print(f"  historic DCC user-task activities checked: {len(historic_activities)}")


if __name__ == "__main__":
    main()
