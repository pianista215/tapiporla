# Docker build
In order to build the docker, in the project folder (where build.sbt is place) execute:

sbt docker:publishLocal

That will create the docker image with the compilation. To run it:

docker run --net=host stockmicroservice

(We need net host to access ES, and Scrapy crawler)
