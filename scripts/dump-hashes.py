#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys

lineno = 0

for line in sys.stdin:
    try:
        o = json.loads(line)
        
        lineno += 1
        print('{0} {1} {2}'.format(o['hash'], o['team'], o['url']))

    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)
    except KeyError as err:
        print('{0}: Missing field: {1}'.format(lineno, err), file=sys.stderr)

