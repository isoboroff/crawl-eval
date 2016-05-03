#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys
from collections import Counter

lineno = 0
ctypes = Counter()

try:
    olist = json.load(sys.stdin)
except JSONDecodeError as err:
    print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)

print("Finished loading", file=sys.stderr)
for o in olist:
    print(json.dumps(o))
    lineno += 1
    if lineno % 100 == 0:
        print(".", file=sys.stderr, end="", flush=True)
        
print("Parsing complete, {0} lines".format(lineno - 1), file=sys.stderr)
