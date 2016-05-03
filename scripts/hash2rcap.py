#!/usr/bin/env python3.5
import sys
from collections import defaultdict

teams = dict()
pages = defaultdict(dict)

for line in sys.stdin:
    (fp, team, url) = line.split(maxsplit=2)
    pages[fp][team] = 1
    teams[team] = 1

team_names = [t for t in teams]
print("hash", " ".join(team_names))
for fp in pages:
    print(fp, "", end="")
    for t in team_names:
        if t in pages[fp]:
            print("1 ", end="")
        else:
            print("0 ", end="")
    print()
