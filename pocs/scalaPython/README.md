# Scala - Python integration

## Introduction
We should to see a way to integrate the python layers into our Scala services...

There are some possibilities like Jython, Jep...

After looking in some other references (paper https://es.slideshare.net/JerryChou4/how-to-integrate-python-into-a-scala-stack-to-build-realtime-predictive-models-v2-nomanuscript), I think we should take the microservices approach.

With this approach we will we independent in our codes, without using some kinds of "weird" libraries, but the overhead is that we should implement microservices in python to launch the processes of Scrapy.

The good point is that this way is more scalable than the others, and I think more stable (It admits generations of dockers with the python crawlers, and you haven't to worry about the selfconfiguration)...

## UPDATEEEEE 
After some reviews, there are one module that provides this funcionality:
http://scrapyrt.readthedocs.io/en/latest/api.html

With that you can invoke through REST from Akka to the "Crawler" and return the JSON formats. 

I've thought in a pipeline similar to that:

Akka Actor -> Give me all values from 10-10-2017 -> ScrapyRT -> Return JSON with new Values -> Akka Actor -> Save on DB, CSV...


## Python references for microservices
- http://flask.pocoo.org/
- https://github.com/nameko/nameko

## Mixing microservices and Scrapy
https://stackoverflow.com/questions/36384286/how-to-integrate-flask-scrapy

