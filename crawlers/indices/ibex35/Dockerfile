#
# To build:
# > sudo docker build -t pianista215/ibex35:0.1.0 .
#
# to start as daemon with port 9080 of api exposed as 9080 on host
#
# > sudo docker run -p 9080:9080 -tid pianista215/ibex35:0.1.0
#

FROM pianista215/scrapyrt:0.10-parameter-patched

ADD crawler /scrapyrt/project

WORKDIR /scrapyrt/project

ENTRYPOINT ["scrapyrt", "-i 0.0.0.0"]

EXPOSE 9080
