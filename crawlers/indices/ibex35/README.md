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

#Docker support
In order to embed that using Docker (and getting the passing parameters functionality already implemented as a patch in ScrapyRT) use:
sudo docker run --rm -p 9080:9080 -v `pw^C:/scrapyrt/project pianista215/scrapyrt

(We need to improve the logs mounting, as it will be stored in the root project, so be aware of that)

Now you are ready to attend requests in the form:
curl -XPOST -d '{ "spider_name":"ibex35", "start_requests":true, "lookup_until_date": "05-11-2017" }' "http://localhost:9080/crawl.json"
