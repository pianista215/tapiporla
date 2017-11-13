import scrapy
from datetime import datetime as dt
import logging


class Ibex35Spider(scrapy.Spider):
    name = "ibex35"

    lookup_until_date = None

    start_urls = [
        "http://www.eleconomista.es/indice/IBEX-35/historico"
    ]

    __allowed = ("lookup_until_date")

    def __init__(self, lookup_until_date=None, *args, **kwargs):
        super(Ibex35Spider, self).__init__(*args, **kwargs)
        for k, v in kwargs.iteritems():
            assert( k in self.__class__.__allowed )
            setattr(self, k, v)

        if lookup_until_date is not None:
            self.lookup_until_date = dt.strptime(lookup_until_date, "%d-%m-%Y")

    def parseDatetimeEleconomista(self,string): 
        return dt.strptime(string, "%d/%m/%y")

    def parseFloat(self, string):
        return float(string.replace(".","").replace(",","."))

    def is_date(self,string):
        try:
            self.parseDatetimeEleconomista(string)
            return True
        except ValueError:
            return False


    def parse(self, response):
        validIbexRow = False
        ibex_date = None
        close_value = None
        max_value = None
        min_value = None
        found_limit = False #When the limit date is found, don't process more
        for ibex_row_data in response.css('table[class*=tablehistoricos]').css("tbody").css("tr"):
            for col_num, ibex_col_data in enumerate(ibex_row_data.css("td::text")):
                col_value = ibex_col_data.extract().strip()
                if col_num == 0:
                    validIbexRow = True
                    ibex_date = None
                    close_value = None
                    max_value = None
                    min_value = None
                    parsed_date = None
                    if self.is_date(col_value):
                        parsed_date = self.parseDatetimeEleconomista(col_value)
                        ibex_date = parsed_date.strftime("%d-%m-%Y")
                        if self.lookup_until_date is not None:
                            found_limit = parsed_date <= self.lookup_until_date
                    else:
                        validIbexRow = False
                        
                elif validIbexRow:
                    if col_num == 1:
                        close_value = self.parseFloat(col_value)
                    elif col_num == 4:
                        max_value = self.parseFloat(col_value)
                    elif col_num == 5:
                        min_value = self.parseFloat(col_value)

            if validIbexRow and not found_limit:
                yield {
                    'date': ibex_date,
                    'close_value': close_value,
                    'max_value': max_value,
                    'min_value': min_value
                }

        next_page = response.css('.enlaces').css('a.posterior::attr(href)').extract_first()
        if next_page is not None and not found_limit:
            next_page = response.urljoin(next_page)
            yield scrapy.Request(next_page, callback=self.parse)
