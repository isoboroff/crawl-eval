#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys

lineno = 0
for line in sys.stdin:
    try:
        o = json.loads(line)
    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err))
        
    lineno += 1
    if 'url' not in o:
        print('{0}: missing "url" field'.format(lineno))
    if 'raw_content' not in o:
        print('{0}: missing "raw_content" field'.format(lineno))
    if 'timestamp' not in o:
        print('{0}: missing "timestamp" field'.format(lineno))
    if 'team' not in o:
        print('{0}: missing "team" field'.format(lineno))


print("Finished checking, read {0} records.".format(lineno))
