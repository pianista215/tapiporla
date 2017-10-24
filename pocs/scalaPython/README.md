# Scala - Python integration

## Introduction
We should to see a way to integrate the python layers into our Scala services...

There are some possibilities like Jython, Jep...

After looking in some other references (paper https://es.slideshare.net/JerryChou4/how-to-integrate-python-into-a-scala-stack-to-build-realtime-predictive-models-v2-nomanuscript), I think we should take the microservices approach.

With this approach we will we independent in our codes, without using some kinds of "weird" libraries, but the overhead is that we should implement microservices in python to launch the processes of Scrapy.

The good point is that this way is more scalable than the others, and I think more stable (It admits generations of dockers with the python crawlers, and you haven't to worry about the selfconfiguration)...

Please read the doc, and tell me things :P

## Python references for microservices
- http://flask.pocoo.org/
- https://github.com/nameko/nameko
