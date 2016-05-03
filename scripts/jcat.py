#!/usr/bin/env python
import json
import sys

for line in sys.stdin:
    print json.dumps(json.loads(line), sort_keys=True, indent=4)

