#
# To build:
# > sudo docker build -t pianista215/ibex35_eleconomista:0.1.0 .
#
# to start as daemon with port 9080 of api exposed as 9080 on host mounting logs in /tmp/logs
#
# > sudo docker run -p 9080:9080 -v /tmp/logs/:/scrapyrt/project/logs pianista215/ibex35_eleconomista:0.1.0
#

FROM pianista215/scrapyrt:0.10-parameter-patched

ADD crawler /scrapyrt/project

RUN mkdir -p /scrapyrt/project/logs

WORKDIR /scrapyrt/project

ENTRYPOINT ["scrapyrt", "-i 0.0.0.0"]

EXPOSE 9080
