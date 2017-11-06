Retrieving data (historic data for Ibex35)
==========================================================================

# Requirements #

Python 3

Scrapy required:
pip3 install Scrapy

# Launch crawler #
cd ibex35

Launch without limit:
scrapy crawl ibex35 -o ../ibex35.json

Or if you want to limit with a date (not inclusive):
scrapy crawl ibex35 -a lookup_until_date="29-05-2017"
