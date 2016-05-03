#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys
from collections import Counter
from urllib.parse import urlsplit

lineno = 0
ctypes = Counter()

for line in sys.stdin:
    try:
        o = json.loads(line)
    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)
        
    lineno += 1
    if 'url' in o:
        host = "{0.scheme}://{0.netloc}/".format(urlsplit(o['url']))
        ctypes[host] += 1
    else:
        print('{0}: missing url field'.format(lineno), file=sys.stderr)

for type in ctypes:
    print(type, ctypes[type])
