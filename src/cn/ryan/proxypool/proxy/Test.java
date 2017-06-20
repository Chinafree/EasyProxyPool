package cn.ryan.proxypool.proxy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import cn.ryan.entity.EvaluateScoringEntity;
import cn.ryan.processor.CrawlerSite;
import cn.ryan.processor.PageProcessor;
import cn.ryan.processor.Processor;
import cn.ryan.proxypool.proxy.ProxyPool.RandomType;

public class Test implements Processor {
	private CrawlerSite site = CrawlerSite.create()
			.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/527.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36")
			.ignoreContentType(true).timeOut(5000);
	static int count = 0;

	public static void main(String[] args) throws IOException {
		ExecutorService exe = Executors.newFixedThreadPool(20);

		for (int i = 0; i < 1; i++) {
			exe.execute(new Runnable() {
				@Override
				public void run() {
					Document doc;
					try {
						Response res = ProxyPool.getInstance().openProxyConnection(PageProcessor.create(new Test()).url("https://www.baidu.com/s?wd=Java"), 5);
						if (res == null) {
							return;
						}
						doc = res.parse();
						if (doc.title().contains("跨境云")) {
							count++;
						}
					} catch (IOException e) {
						System.err.println(e.getMessage());
					}
				}
			});
		}
		exe.shutdown();
		while (true) {
			if (exe.isTerminated()) {
				System.out.println(count);
				List<EvaluateScoringEntity> list = ProxyPool.getInstance().getUsedIpPoolList();
				System.out.println("IP：\t\t\t\t\t Score：Times：");
				for (EvaluateScoringEntity e : list) {
					System.out.println(e.getProxyEntity().getProxy() + "\t" + e.getScore() + "\t" + e.getTimes());
				}
				break;
			}
		}

	}

	@Override
	public CrawlerSite getSite() {
		// TODO Auto-generated method stub
		return site;
	}

}
