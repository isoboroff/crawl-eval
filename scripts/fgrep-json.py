#!/usr/bin/env python3.5

import json
from json import JSONDecodeError
import sys
import argparse
import gzip
import re

parser = argparse.ArgumentParser(description='Match multiple patterns against a JSON field')
parser.add_argument('--field', help='Field to grep', default='url')
parser.add_argument('file', help='File to look in')
parser.add_argument('patterns', nargs=argparse.REMAINDER, help='Patterns to match')
args = parser.parse_args()

lineno = 0

with gzip.open(args.file, 'rt') as fp:
    for line in fp:
        try:
            o = json.loads(line)
        
            lineno += 1
            success = False
            if args.field in o:
                fval = o[args.field]
                for p in args.patterns:
                    if p in fval:
                        success = True
                        break
            if success:
                print(json.dumps(o))
        except JSONDecodeError as err:
            print('{0}: JSON parse error: {1}'.format(lineno, err), file=sys.stderr)
        except KeyError as err:
            print('{0}: Missing field: {1}'.format(lineno, err), file=sys.stderr)

