#!/usr/bin/env python3
"""Summarize recorded evidence without converting software timings into panel latency."""
import argparse
import collections
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("capture", type=Path)
args = parser.parse_args()
reports = []
for metadata in sorted(args.capture.rglob("metadata.json")):
    event_file = metadata.with_name("events.jsonl")
    if not event_file.exists():
        continue
    info = json.loads(metadata.read_text())
    counts = collections.Counter()
    tools = collections.Counter()
    buttons = collections.Counter()
    keys = collections.Counter()
    states = collections.Counter()
    services = collections.Counter()
    failures = []
    observations = []
    restored = []
    for line in event_file.read_text().splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            failures.append("Incomplete JSONL record; capture may have occurred during writing")
            continue
        kind, data = event["type"], event["data"]
        counts[kind] += 1
        if kind == "motion":
            buttons[data["buttons"]] += 1
            for sample in data["samples"]:
                for pointer in sample["pointers"]:
                    tools[pointer["tool"]] += 1
        elif kind == "key": keys[data["code"]] += 1
        elif kind == "sessionState":
            states[data["state"]] += 1
            if data["state"] == "FAILED": failures.append(data["detail"])
        elif kind == "service": services[data["operation"]] += 1
        elif kind == "humanObservation": observations.append(data)
        elif kind == "activityCreated": restored.append(data["restoredStrokes"])
        elif kind.endswith("Failed"): failures.append(data)
    reports.append(dict(session=metadata.parent.name, uid=info["uid"], model=info["model"],
        available=info["available"], reason=info.get("failure"), events=counts, tools=tools,
        buttons=buttons, keycodes=keys, states=states, services=services, failures=failures,
        restored_strokes=restored, human_observations=observations))
print(json.dumps({"schemaVersion": 1, "note": "Motion events can appear at both Activity and View; these counts are records, not unique physical events. No visible-ink latency is inferred.", "sessions": reports}, indent=2))
