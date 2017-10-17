import scrapy


class HotelsSpider(scrapy.Spider):
    name = "hotels"

    start_urls = [
        "http://www.eleconomista.es/indice/IBEX-35/historico"
    ]

    def parse(self, response):
        for ibex_row_data in response.css('table[class*=tablehistoricos]').css("tbody").css("tr")
            cols = ibex_close_data.css("td").extract()
            date =
            close_value =
            max_value =
            min_value =
            name = hotel.css(".sr-hotel__name::text").extract_first().strip()
            stars = hotel.css("::attr(data-class)").extract_first().strip()
            price = hotel.css('strong').css("b::text").extract_first()
            image_url = hotel.css('img::attr(src)').extract_first().strip()
            if price is not None:
                #Avoid € storage
                price_value = price.strip()[2:]
                yield {
                    'city': city,
                    'name': name,
                    'stars': int(stars),
                    'price': float(price_value),
                    'image_url': image_url
                }

        #next_page = response.css('[data-page-next]::attr(href)').extract_first()
        #if next_page is not None:
        #    next_page = response.urljoin(next_page)
        #    yield scrapy.Request(next_page, callback=self.parse)
