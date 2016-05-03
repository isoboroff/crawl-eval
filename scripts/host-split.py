#!/usr/bin/env python3.5
import json
from json import JSONDecodeError
import sys
from urllib.parse import urlsplit
import argparse
import os
import os.path

parser = argparse.ArgumentParser(description='Split a CDRv2 JSON file up by URL host')
parser.add_argument('-o', '--outputdir', help='output directory (default .)', default='.')
args = parser.parse_args()

if not os.path.exists(args.outputdir):
    os.makedirs(args.outputdir)

lineno = 0

for line in sys.stdin:
    try:
        o = json.loads(line)
    except JSONDecodeError as err:
        print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)
        
    lineno += 1
    if 'url' in o:
        host = "{0.netloc}".format(urlsplit(o['url']))
        with open(os.path.join(args.outputdir, "{0}.json".format(host)), 'a') as fp:
            print(json.dumps(o), file=fp)

    else:
        print('{0}: missing url field'.format(lineno), file=sys.stderr)

