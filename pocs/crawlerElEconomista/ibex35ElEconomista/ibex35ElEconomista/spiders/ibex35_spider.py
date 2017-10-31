import scrapy
from datetime import datetime as dt


class Ibex35Spider(scrapy.Spider):
    name = "ibex35"

    start_urls = [
        "http://www.eleconomista.es/indice/IBEX-35/historico"
    ]

    def parseDatetime(self,string): 
        return dt.strptime(string, "%d/%m/%y")

    def parseFloat(self, string):
        return float(string.replace(".","").replace(",","."))

    def is_date(self,string):
        try:
            self.parseDatetime(string)
            return True
        except ValueError:
            return False


    def parse(self, response):
        validIbexRow = False
        ibex_date = None
        close_value = None
        max_value = None
        min_value = None
        for ibex_row_data in response.css('table[class*=tablehistoricos]').css("tbody").css("tr"):
            for col_num, ibex_col_data in enumerate(ibex_row_data.css("td::text")):
                col_value = ibex_col_data.extract().strip()
                if col_num == 0:
                    validIbexRow = True
                    ibex_date = None
                    close_value = None
                    max_value = None
                    min_value = None
                    if self.is_date(col_value):
                        parsed_date = self.parseDatetime(col_value)
                        ibex_date = parsed_date.strftime("%d-%m-%Y")
                    else:
                        validIbexRow = False
                        
                elif validIbexRow:
                    if col_num == 1:
                        close_value = self.parseFloat(col_value)
                    elif col_num == 4:
                        max_value = self.parseFloat(col_value)
                    elif col_num == 5:
                        min_value = self.parseFloat(col_value)

            if validIbexRow:
                yield {
                    'date': ibex_date,
                    'close_value': close_value,
                    'max_value': max_value,
                    'min_value': min_value
                }

            

        #next_page = response.css('[data-page-next]::attr(href)').extract_first()
        #if next_page is not None:
        #    next_page = response.urljoin(next_page)
        #    yield scrapy.Request(next_page, callback=self.parse)