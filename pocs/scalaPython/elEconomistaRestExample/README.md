Using ScrapyRT to reuse the ElEconomista crawler
==========================================================================

# Requirements #

Python 3

Scrapy required:
pip3 install Scrapy

Scrapy rt required:
pip3 install scrapyrt


# Launch scrapyrt to listend #
cd ibex35ElEconomista
scrapyrt -p 9081

And now open your browser with: 
http://localhost:9081/crawl.json?start_requests=false&spider_name=ibex35
