"""Manage ordinary persisted Codex app-server threads through the installed protocol.

No collaboration/subagent APIs, no raw storage writes, no desktop UI automation.
The first dispatch is read-only contract/BDD work until a shared code baseline exists.
"""
from __future__ import annotations

import argparse
import json
import queue
import shutil
import subprocess
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TASK = Path(__file__).resolve().parent
REGISTRY = TASK / "independent-thread-registry.json"
CONTRACTS = TASK / "thread-contract-results.json"


def save_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


class Client:
    def __init__(self) -> None:
        executable = shutil.which("codex")
        if not executable:
            raise RuntimeError("Installed codex CLI not found")
        self.process = subprocess.Popen(
            [executable, "app-server", "--stdio"],
            cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            text=True, encoding="utf-8", bufsize=1,
            creationflags=subprocess.CREATE_NO_WINDOW if hasattr(subprocess, "CREATE_NO_WINDOW") else 0,
        )
        self.responses: dict[int, dict] = {}
        self.notifications: queue.Queue[dict] = queue.Queue()
        self.messages: queue.Queue[dict] = queue.Queue()
        self.sequence = 0
        self.stderr_tail: list[str] = []
        threading.Thread(target=self._read, daemon=True).start()
        threading.Thread(target=self._stderr, daemon=True).start()
        self.rpc("initialize", {"clientInfo": {"name": "dcc_independent_thread_manager", "version": "1.0"}})
        self.send({"method": "initialized"})

    def _read(self) -> None:
        assert self.process.stdout
        for line in self.process.stdout:
            try:
                self.messages.put(json.loads(line))
            except json.JSONDecodeError:
                self.messages.put({"protocolError": "non-JSON output"})

    def _stderr(self) -> None:
        assert self.process.stderr
        for line in self.process.stderr:
            self.stderr_tail.append(line.strip())
            self.stderr_tail = self.stderr_tail[-8:]

    def send(self, value: dict) -> None:
        assert self.process.stdin
        self.process.stdin.write(json.dumps(value, ensure_ascii=False) + "\n")
        self.process.stdin.flush()

    def pump(self, timeout: float = 1.0) -> None:
        try:
            message = self.messages.get(timeout=timeout)
        except queue.Empty:
            if self.process.poll() is not None:
                raise RuntimeError(f"Codex app-server exited {self.process.returncode}")
            return
        if "protocolError" in message:
            raise RuntimeError(message["protocolError"])
        if "id" in message and "method" not in message:
            self.responses[message["id"]] = message
        elif "id" in message:
            # Read-only contract dispatch should not request approval or dynamic tools.
            # Never auto-approve an unexpected action.
            self.send({"id": message["id"], "error": {"code": -32601,
                       "message": "Manager did not authorize interactive approval or dynamic tool execution"}})
            self.notifications.put({"method": "manager/unexpected_request", "params": {"method": message.get("method")}})
        else:
            self.notifications.put(message)

    def rpc(self, method: str, params: dict, timeout: float = 45.0) -> dict:
        self.sequence += 1
        identifier = self.sequence
        self.send({"id": identifier, "method": method, "params": params})
        deadline = time.monotonic() + timeout
        while identifier not in self.responses:
            if time.monotonic() >= deadline:
                raise TimeoutError(f"No response to {method}")
            self.pump()
        response = self.responses.pop(identifier)
        if "error" in response:
            raise RuntimeError(f"{method} rejected: {response['error']}")
        return response["result"]

    def close(self) -> None:
        if self.process.poll() is not None:
            return
        if self.process.stdin:
            self.process.stdin.close()
        try:
            self.process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            self.process.terminate()
            self.process.wait(timeout=10)


def dispatch_contracts(client: Client, registry: dict, summarize: bool = False) -> None:
    for entry in registry["threads"]:
        prompt = (
            f"你是主管理正式分配的独立Codex线程{entry['module']}，不是子Agent；禁止使用子Agent或collaboration工具。"
            f"本线程名：{entry['name']}。任务书：{entry['task_book']}。"
            "先读取AGENTS.md、docs/dcc-parallel-delivery/README.md、shared-contract.md、ownership.md、"
            "verification-plan.md，以及你自己的任务书和docs/product/dcc-final-requirements.html。"
            "主管理会统一Review。当前共同基线未锁定，现有分支int_qms有大量未提交改动；"
            "本轮只执行G1设计工作，严禁修改文件、提交推送、创建worktree、数据库操作、测试或启动服务。"
            "读取相关代码后提出你模块的具体接口/字段/组件、Given/When/Then、跨模块依赖和可独立开发的最小批次，"
            "指出任务书遗漏或冲突，不猜测未定业务参数。不得实际执行开发。"
            "最终输出包含：模块职责与代码边界；具体G1接口方案；需要主管理决策的共享字段；"
            "首批BDD/测试计划；可以先开始与必须等待的部分。此输出由主管理收集审查并正式派发下一轮。"
        )
        if summarize:
            prompt = (
                f"主管理给独立线程{entry['module']}的G1阶段收口指令：你前面已经读取了代码和任务书，"
                "请直接依据当前会话中的阅读结果输出设计方案，不再调用任何工具、不继续扫描代码。"
                "最多1500个中文字，包含你负责模块的接口/字段/组件、共享字段决策、首批3到5个BDD及"
                "可以独立开始和必须等待的部分。明确哪些只是设计、哪些待确认，不写实现已完成。"
                "本轮仍禁止改文件、测试、Git、数据库、服务或子Agent。主管理收集你的最终答复Review。"
            )
        result = client.rpc("turn/start", {"threadId": entry["id"],
                            "input": [{"type": "text", "text": prompt}], "approvalPolicy": "never"})
        entry["turn_id"] = result["turn"]["id"]
        entry["dispatch_status"] = result["turn"].get("status", "started")
        save_json(REGISTRY, registry)
        print(json.dumps({"event": "contract_task_dispatched", "module": entry["module"],
                          "thread_id": entry["id"], "turn_id": entry["turn_id"]}, ensure_ascii=False), flush=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["create", "contracts"], required=True)
    parser.add_argument("--summarize", action="store_true")
    parser.add_argument("--timeout-seconds", type=int, default=1200)
    args = parser.parse_args()
    client = Client()
    try:
        if args.mode == "create":
            if REGISTRY.exists():
                raise RuntimeError("Registry already exists; refuse to create duplicate threads")
            registry = {"manager_thread": "01a0eb90-0723-72b3-a6ca-7a22dab040ab",
                        "creation_method": "ordinary thread/start through installed codex app-server stdio",
                        "stage": "G0 baseline pending; G1 read-only design", "threads": []}
            save_json(REGISTRY, registry)
            jobs = [("A", "DCC-A 审批与生命周期", "task-a-workflow.md"),
                    ("B", "DCC-B 项目属性与基础配置", "task-b-project.md"),
                    ("C", "DCC-C 版本与名称编号", "task-c-version.md"),
                    ("D", "DCC-D 关联与引用", "task-d-relations.md")]
            for module, name, book in jobs:
                result = client.rpc("thread/start", {"cwd": str(ROOT), "ephemeral": False,
                                    "sandbox": "read-only", "approvalPolicy": "never"})
                thread = result["thread"]
                entry = {"module": module, "id": thread["id"], "name": name,
                         "task_book": f"docs/dcc-parallel-delivery/{book}",
                         "source": thread.get("source"), "cwd": thread.get("cwd"),
                         "dispatch_status": "created"}
                registry["threads"].append(entry)
                save_json(REGISTRY, registry)
                client.rpc("thread/name/set", {"threadId": entry["id"], "name": name})
                confirmation = client.rpc("thread/read", {"threadId": entry["id"], "includeTurns": False})
                assert confirmation["thread"]["id"] == entry["id"]
                print(json.dumps({"event": "independent_thread_created", **entry}, ensure_ascii=False), flush=True)
            dispatch_contracts(client, registry, args.summarize)
        else:
            registry = json.loads(REGISTRY.read_text(encoding="utf-8"))
            for entry in registry["threads"]:
                client.rpc("thread/resume", {"threadId": entry["id"], "approvalPolicy": "never", "sandbox": "read-only"})
            dispatch_contracts(client, registry, args.summarize)

        completed: set[str] = set()
        outputs: dict[str, list[str]] = {e["id"]: [] for e in registry["threads"]}
        results = {"stage": "G1 read-only contract proposal", "threads": []}
        last_status = time.monotonic()
        deadline = time.monotonic() + args.timeout_seconds
        while len(completed) < len(registry["threads"]):
            client.pump(timeout=1)
            while not client.notifications.empty():
                notification = client.notifications.get()
                method = notification.get("method", "")
                params = notification.get("params", {})
                thread_id = params.get("threadId")
                if method == "item/completed" and thread_id in outputs:
                    item = params.get("item", {})
                    if item.get("type") == "agentMessage" and item.get("text"):
                        outputs[thread_id].append(item["text"])
                if method == "turn/completed" and thread_id in outputs:
                    completed.add(thread_id)
                    entry = next(e for e in registry["threads"] if e["id"] == thread_id)
                    entry["dispatch_status"] = params.get("turn", {}).get("status", "completed")
                    results["threads"].append({"module": entry["module"], "thread_id": thread_id,
                                              "status": entry["dispatch_status"], "messages": outputs[thread_id]})
                    save_json(REGISTRY, registry)
                    save_json(CONTRACTS, results)
                    print(json.dumps({"event": "thread_contract_returned", "module": entry["module"],
                                      "thread_id": thread_id, "status": entry["dispatch_status"]}, ensure_ascii=False), flush=True)
                if method == "manager/unexpected_request":
                    print(json.dumps({"event": "unexpected_request_denied", "details": params}), flush=True)
                if method == "error":
                    error = params.get("error", {})
                    message = str(error.get("message", ""))[:500]
                    print(json.dumps({"event": "thread_execution_error", "thread_id": thread_id,
                                      "message": message, "willRetry": params.get("willRetry")}, ensure_ascii=False), flush=True)
                if method == "item/started" and thread_id in outputs:
                    item_type = params.get("item", {}).get("type", "")
                    if item_type != "userMessage":
                        print(json.dumps({"event": "thread_work_started", "thread_id": thread_id,
                                          "item_type": item_type}), flush=True)
            if time.monotonic() - last_status > 40:
                print(json.dumps({"event": "waiting_for_independent_threads", "completed": len(completed),
                                  "total": len(registry["threads"])}), flush=True)
                last_status = time.monotonic()
            if time.monotonic() >= deadline:
                for entry in registry["threads"]:
                    if entry["id"] not in completed:
                        client.rpc("turn/interrupt", {"threadId": entry["id"], "turnId": entry["turn_id"]})
                        entry["dispatch_status"] = "blocked_no_completed_output"
                save_json(REGISTRY, registry)
                save_json(CONTRACTS, results)
                raise TimeoutError(f"Independent thread contract execution did not complete within {args.timeout_seconds} seconds; ordinary threads are preserved")
        print(json.dumps({"result": "PASS", "ordinary_threads": len(registry["threads"]),
                          "contracts_returned": len(completed), "subagents_used": False}), flush=True)
    finally:
        client.close()


if __name__ == "__main__":
    raise SystemExit("Disabled: user requires Codex desktop-native threads; do not launch CLI app-server or queue as a substitute.")
