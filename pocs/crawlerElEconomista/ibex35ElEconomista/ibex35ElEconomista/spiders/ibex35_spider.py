import scrapy


class HotelsSpider(scrapy.Spider):
    name = "hotels"

    start_urls = [
        "http://www.eleconomista.es/indice/IBEX-35/historico"
    ]

    def parse(self, response):
        for ibex_row_data in response.css('table[class*=tablehistoricos]').css("tbody").css("tr")
            cols = ibex_close_data.css("td").extract()
            if #Check is not an "information row"
            date =
            close_value =
            max_value =
            min_value =
            

        #next_page = response.css('[data-page-next]::attr(href)').extract_first()
        #if next_page is not None:
        #    next_page = response.urljoin(next_page)
        #    yield scrapy.Request(next_page, callback=self.parse)
