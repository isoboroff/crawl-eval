#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys
from urllib.parse import urlsplit
import argparse
import re

parser = argparse.ArgumentParser(description='Grep a CDRv2 JSON file up by URL host')
parser.add_argument('host', help='Host to look for', default='.*')
args = parser.parse_args()

lineno = 0

hostre = re.compile(args.host)

for line in sys.stdin:
    try:
        o = json.loads(line)
    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)
        
    lineno += 1
    if 'url' in o:
        host = "{0.netloc}".format(urlsplit(o['url']))
        if re.search(hostre, host):
            print(json.dumps(o))

    else:
        print('{0}: missing url field'.format(lineno), file=sys.stderr)

