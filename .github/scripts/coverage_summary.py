"""Write line coverage from a Kover XML report to the GitHub job summary.

Coverage is informational: this never fails the build.
"""

import os
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path


def lines(element):
    counter = element.find("counter[@type='LINE']")
    if counter is None:
        return 0, 0
    return int(counter.get("covered")), int(counter.get("missed"))


def percent(covered, missed):
    total = covered + missed
    return f"{100 * covered / total:.0f} %" if total else "–"


report = ElementTree.parse(sys.argv[1]).getroot()
rows = []
for package in report.iter("package"):
    for source in package.iter("sourcefile"):
        covered, missed = lines(source)
        if covered + missed:
            rows.append((f"{package.get('name')}/{source.get('name')}", covered, missed))
rows.sort(key=lambda row: (-row[2], row[0]))

covered, missed = lines(report)
summary = [
    f"## Line coverage: {percent(covered, missed)}",
    "",
    f"{covered} of {covered + missed} lines covered by unit tests.",
    "",
    "| File | Coverage | Missed lines |",
    "| --- | ---: | ---: |",
    *(f"| `{name}` | {percent(c, m)} | {m} |" for name, c, m in rows),
]
text = "\n".join(summary) + "\n"
print(text)
if "GITHUB_STEP_SUMMARY" in os.environ:
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as file:
        file.write(text)
