#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys

lineno = 0

for line in sys.stdin:
    try:
        o = json.loads(line)
        
        o['lineno'] = lineno
        
        print(json.dumps(o))
    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)
    except KeyError as err:
        print('{0}: Missing field: {1}'.format(lineno, err), file=sys.stderr)

    lineno += 1
