# -*- coding: utf-8 -*-

# Define here the models for your scraped items
#
# See documentation in:
# http://doc.scrapy.org/en/latest/topics/items.html

import scrapy
import time
import json
from selenium import webdriver
from infoq_article.items import InfoqArticleItem
from selenium.webdriver.common.action_chains import ActionChains
import sys
reload(sys)
sys.setdefaultencoding('utf-8')

def cleanse(alist):
    return alist[0].strip().encode('utf-8').replace('"', '“').replace('\n', '').replace('\t', '    ').replace('\\', '“') if alist else u''

def getMonth(monthInChinese):
    if (monthInChinese == '一月'):
        return '01'
    if (monthInChinese == '二月'):
        return '02'
    if (monthInChinese == '三月'):
        return '03'
    if (monthInChinese == '四月'):
        return '04'
    if (monthInChinese == '五月'):
        return '05'
    if (monthInChinese == '六月'):
        return '06'
    if (monthInChinese == '七月'):
        return '07'
    if (monthInChinese == '八月'):
        return '08'
    if (monthInChinese == '九月'):
        return '09'
    if (monthInChinese == '十月'):
        return '10'
    if (monthInChinese == '十一月'):
        return '11'
    if (monthInChinese == '十二月'):
        return '12'
    return '00'
    
class InfoqSpider(scrapy.Spider):
    name = 'infoq_article'
    allowed_domains = ["www.infoq.com"]
    start_urls = ["http://www.infoq.com/cn/articles"]
    out = []

    def __init__(self):
        scrapy.Spider.__init__(self)

        profile = webdriver.FirefoxProfile()
        profile.set_preference("general.useragent.override","Mozilla/5.0 (Linux; Android 4.4; Nexus 5 Build/BuildID) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36")
        self.driver = webdriver.Firefox(profile)

    def __del__(self):
        self.driver.close()

    def getout(self):
        if len(self.out) == 0:
            inputfile = open('out.json','r')
            lines = inputfile.readlines()
            inputfile.close()
            for line in lines:
                self.out.append(json.loads(line))
        return self.out

    def downloaded(self, link):
        out = self.getout()
        for js in out:
            if js['link'] == link:
                return True
        return False

    def parse(self, response):

        self.driver.get('http://www.infoq.com/cn/articles')
        time.sleep(2)
        
        while True:

            hxs = scrapy.Selector(text = self.driver.page_source)
            print (hxs)
            for info in hxs.xpath('/html/body/div[1]/ul[2]/li'):
                print 'info'
                item = InfoqArticleItem()
                link = cleanse(info.xpath('a[1]/@href').extract())
                link = 'http://www.infoq.com{0}'.format(link)
                print (link)
                if self.downloaded(link): return
                print (self.downloaded(link))
                item['link'] = link
                item['title'] = cleanse(info.xpath('a[1]/text()').extract())
                item['description'] = cleanse(info.xpath('a[2]/text()').extract())
                item['piclink'] = cleanse(info.xpath('a[2]/img/@src').extract())
                item['courselink'] = u''
                item['source'] = u'InfoQ'
                item['school'] = u'InfoQ'
                item['instructor'] = cleanse(info.xpath('span/a/text()').extract())
                item['language'] = u'中文'
                item['tags'] = u'InfoQ'
                p_year = cleanse(info.xpath('ul/li[1]/text()').extract())
                p_month = cleanse(info.xpath('ul/li[2]/text()').extract())
                p_day = cleanse(info.xpath('ul/li[3]/text()').extract())
                item['posted'] = u'{0}-{1}-{2}'.format(p_year, getMonth(p_month), p_day)
                print u'{0}-{1}-{2}'.format(p_year, getMonth(p_month), p_day)
                item['crawled'] = time.strftime('%Y-%m-%d %H:%M')
                yield item

            next = self.driver.find_element_by_xpath('/html/body/div[1]/ul[1]/li[2]/a')
            aclass = next.get_attribute('class')
            if ('btn_inactive' in aclass):
                break

            try:
                #next.click()
                ActionChains(self.driver).move_to_element(next).click().perform()
                time.sleep(5)

            except Exception as err:
                print(err)
                break






