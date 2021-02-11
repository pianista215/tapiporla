#!/usr/bin/python

import sys
import csv
import json
from datetime import datetime as dt

if len(sys.argv) != 3:
	print("Usage: python csv_converter.py <json_file> <output_csv>")
	sys.exit(-1)

input_file = sys.argv[1]
output_file = sys.argv[2]

with open(input_file) as json_file, open(output_file, 'w') as csv_file:  
	json_values = json.load(json_file)
	f = csv.writer(csv_file)

	# Write CSV Header, If you dont need that, remove this line
	f.writerow(["day","month","year","opening_value","close_value","min_value","max_value","volume"])

	for x in json_values["items"]:
		date = dt.strptime(x["date"], "%d-%m-%Y")
		f.writerow([
    			date.day,
    			date.month,
    			date.year,
                x["opening_value"],
                x["close_value"],
                x["min_value"],
                x["max_value"],
                x["volume"]
                ])