Retrieving data from ElEconomista website (historic data for Ibex35)
==========================================================================

# Requirements #

Python 3

Scrapy required:
pip install Scrapy

# Launch crawler #
cd ibex35ElEconomista
scrapy crawl ibex35 -o ../ibex35.json
