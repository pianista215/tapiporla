Retrieving data from Xe.com (historic data for EUR currency)
==========================================================================

# Requirements #

Python 3

Scrapy required:
pip install Scrapy

# Launch crawler #
cd EURXeCom 
scrapy crawl EUR -o ../EUR.json -a date=2017-10-12
