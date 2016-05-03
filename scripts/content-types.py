#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys
from collections import Counter

lineno = 0
ctypes = Counter()

for line in sys.stdin:
    try:
        o = json.loads(line)
    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err))
        
    lineno += 1
    if 'content_type' in o:
        ctypes[o['content_type']] += 1
    else:
        print('{0}: missing content-type field'.format(lineno))

for type in ctypes:
    print(type, ctypes[type])
