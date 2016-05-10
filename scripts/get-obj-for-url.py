#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys
from collections import Counter
from urllib.parse import urlsplit
import argparse

lineno = 0
ctypes = Counter()

parser = argparse.ArgumentParser(description='Dump the raw web page content given a URL from the JSON on stdin')
parser.add_argument('url', help='URL to look for in the url property')
args = parser.parse_args()

for line in sys.stdin:
    try:
        o = json.loads(line)
    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)
        
    lineno += 1
    if 'url' in o and o['url'] == args.url:
        print(json.dumps(o))
        break
        
