Retrieving data (historic data from InfoMercados)
==========================================================================

# Requirements #

Python 3

Scrapy required:
pip3 install Scrapy


# Docker support
In order to embed that using Docker (and getting the passing parameters functionality already implemented as a patch in ScrapyRT) use:
sudo docker run --rm -p 9080:9080 -v /tmp/logs/:/scrapyrt/project/logs pianista215/infomercados:0.1.0

Now you are ready to attend requests in the form:
curl -XPOST -d '{ "spider_name":"infomercados", "start_requests":true, "parameters":{ "lookup_until_date": "05-11-2017", "crawler_path": "ibex-35-i ib" } }' "http://localhost:9080/crawl.json"

Note: You need at least to provide a crawler_path in order to retrieve data (for example "ibex-35-i ib", or "bbva-bbva")
