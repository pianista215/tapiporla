import scrapy
from datetime import datetime as dt


class EURSpider(scrapy.Spider):
    name = "EUR"

    date = None

    currenciesToRetrieve = ["USD", "GBP", "JPY", "COP", "CLP", "ARS", "VEF", "MXN"]

    def __init__(self, date=None, *args, **kwargs):
        super(EURSpider, self).__init__(*args, **kwargs)
        self.start_urls = ['http://www.xe.com/currencytables/?from=EUR&date=%s' % date]
        self.date = date

    def date_formated(self):
        return dt.strptime(self.date, "%Y-%m-%d").strftime("%d-%m-%Y")

    def parse(self, response):
        identifier = None

        for currency_data in response.css("#historicalRateTbl").css("tbody").css("tr"):
            identifier =  currency_data.css("a::text").extract_first().strip()

            if identifier in self.currenciesToRetrieve: 
                for col_num, currency_col in enumerate(currency_data.css("td::text")):
                    if col_num == 1:
                        yield {
                            'date': self.date_formated(),
                            'currency': identifier,
                            'max_value': float(currency_col.extract().strip()),
                        }

