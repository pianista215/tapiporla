import scrapy
from datetime import datetime as dt
import logging


class Ibex35Spider(scrapy.Spider):
    name = "ibex35"

    lookup_until_date = None

    parameters = None

    nextPage = 0

    start_urls = [
        "http://www.infomercados.com/cotizaciones/historico/ibex-35-i%20ib/"
    ]

    __allowed = ("parameters")

    def __init__(self, lookup_until_date=None, *args, **kwargs):
        super(Ibex35Spider, self).__init__(*args, **kwargs)
        if kwargs:
            for k, v in kwargs.iteritems():
                assert( k in self.__class__.__allowed )
                setattr(self, k, v)

        if lookup_until_date is not None:
            self.lookup_until_date = dt.strptime(lookup_until_date, "%d-%m-%Y")

        if 'lookup_until_date' in self.parameters:
            self.lookup_until_date = dt.strptime(self.parameters['lookup_until_date'], "%d-%m-%Y")

    def parseDatetimeInfoMercado(self,string): 
        return dt.strptime(string, "%d/%m/%Y")

    def strFloat(self, string):
        return string.replace(".","").replace(",",".")

    def is_date(self,string):
        try:
            self.parseDatetimeInfoMercado(string)
            return True
        except ValueError:
            return False


    def parse(self, response):
        self.logger.info(self.parameters)

        self.nextPage = self.nextPage + 1
        emptyPage = True
        validIbexRow = False
        ibex_date = None
        opening_value = None
        close_value = None
        max_value = None
        min_value = None
        volume = None
        found_limit = False #When the limit date is found, don't process more
        for ibex_row_data in response.css('#cot_historico1').css("tbody").css("tr"):
            for col_num, ibex_col_data in enumerate(ibex_row_data.css("td::text")):
                col_value = ibex_col_data.extract().strip()
                if col_num == 0:
                    validIbexRow = True
                    ibex_date = None
                    opening_value = None
                    close_value = None
                    max_value = None
                    min_value = None
                    parsed_date = None
                    volume = None
                    if self.is_date(col_value):
                        parsed_date = self.parseDatetimeInfoMercado(col_value)
                        ibex_date = parsed_date.strftime("%d-%m-%Y")
                        if self.lookup_until_date is not None:
                            found_limit = parsed_date <= self.lookup_until_date
                    else:
                        validIbexRow = False
                        
                elif validIbexRow:
                    if col_num == 1:
                        opening_value = self.strFloat(col_value)
                    elif col_num == 2: 
                        max_value = self.strFloat(col_value)
                    elif col_num == 3:
                        min_value = self.strFloat(col_value)
                    elif col_num == 4:
                        close_value = self.strFloat(col_value)
                    elif col_num == 5:
                        volume = self.strFloat(col_value)

            if validIbexRow and not found_limit:
                emptyPage = False
                yield {
                    'date': ibex_date,
                    'opening_value': opening_value,
                    'close_value': close_value,
                    'max_value': max_value,
                    'min_value': min_value,
                    'volume': volume
                }

        next_page_data = {'N': "%d" % self.nextPage}

        if not emptyPage:
            yield scrapy.FormRequest(url=self.start_urls[0], method="POST", formdata={'N': "%d" % self.nextPage}, callback = self.parse)
