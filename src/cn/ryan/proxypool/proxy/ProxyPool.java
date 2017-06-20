package cn.ryan.proxypool.proxy;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;

import cn.ryan.entity.EvaluateScoringEntity;
import cn.ryan.entity.ProxyEntity;
import cn.ryan.processor.CrawlerSite;
import cn.ryan.processor.PageProcessor;
import cn.ryan.processor.Processor;
import cn.ryan.proxypool.utils.JsonUtils;
import cn.ryan.proxypool.utils.StringUtils;

public class ProxyPool implements Processor {
	private CrawlerSite site = CrawlerSite.create()
			.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36")
			.ignoreContentType(true).timeOut(5000);

	private final static String BASE_URL = "#";

	private static ProxyPool instance = null;

	private Vector<ProxyEntity> originalIpPool = new Vector<>();// 原始代理池

	private ConcurrentHashMap<String, EvaluateScoringEntity> userdIpPool = new ConcurrentHashMap<>();// 计分代理池

	private ProxyEntity proxyEntity;

	private static int errorOutTime = 100;

	public enum RandomType {
		RANDOM, SHUFFLE;
	}

	private ProxyPool() {
		setCache();
	}

	/***
	 * 同步设置缓存
	 * 
	 */
	private void setCache() {
		if (getOriginalPoolSize() <= 0) {
			synchronized (this) {
				getProxyList(true);
			}
		}
	}

	/***
	 * 打开原始代理连接，并执行请求，默认延迟0秒
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	public Response openOriginaProxyConnection(Connection conn) throws IOException {
		return openOriginaProxyConnection(conn, 0);
	}

	/***
	 * 打开原始代理连接，并执行请求
	 * 
	 * @param conn
	 * @param sleepTime
	 *            延迟时间
	 * @return
	 * @throws IOException
	 */
	public Response openOriginaProxyConnection(Connection conn, long sleepTime) throws IOException {
		Validate.notNull(conn);

		Response res = null;

		ProxyEntity pe = getProxyEntity();
		try {
			res = conn.proxy(pe.getProxy()).execute();
			pe.setValid(true);
		} catch (SocketTimeoutException | SocketException | EOFException | HttpStatusException e) {
			pe.setValid(false);
			removeProxy();
		} finally {
			proxyScoring(pe);// 代理评分
		}
		try {
			TimeUnit.MILLISECONDS.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return res;
	}

	/***
	 * 打开使用过的代理连接，并执行请求，默认延迟0秒
	 * 
	 * @param conn
	 * @param score
	 *            分数
	 * @param randomType
	 *            随机模式 随机模式{@link RandomType}：SHUFFLE ：打乱随机 RANDOM：随机数随机
	 * @return
	 * @throws IOException
	 */
	public Response openUsedProxyConnection(Connection conn, int score, RandomType randomType) throws IOException {
		return openUsedProxyConnection(conn, 0, score, randomType);
	}

	/***
	 * 打开使用过的代理连接，并执行请求
	 * 
	 * @param conn
	 * @param sleepTime
	 *            延迟时间
	 * @param score
	 *            分数
	 * @param randomType
	 *            随机模式{@link RandomType}：SHUFFLE ：打乱随机 RANDOM：随机数随机
	 * @return
	 * @throws IOException
	 */
	public Response openUsedProxyConnection(Connection conn, long sleepTime, int score, RandomType randomType) throws IOException {
		Validate.notNull(conn);

		Response res = null;

		ProxyEntity pe = getRandomUsedProxyEntity(score, randomType);
		try {
			if (pe == null) {
				res = conn.execute();
			} else {
				res = conn.proxy(pe.getProxy()).execute();
			}
			pe.setValid(true);
		} catch (SocketTimeoutException | SocketException | EOFException | HttpStatusException e) {
			pe.setValid(false);
			removeProxy();
		} finally {
			proxyScoring(pe);// 代理评分
		}
		try {
			TimeUnit.MILLISECONDS.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return res;
	}

	/***
	 * 打开使用过的且评分为10分以上代理连接，并执行请求，默认延迟0秒
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	public Response openUsedProxyConnection(Connection conn) throws IOException {
		return openUsedProxyConnection(conn, 0, 10, RandomType.SHUFFLE);
	}

	/***
	 * 打开代理连接并执行请求,如原始代理不可用，则使用满足条件的稳定代理
	 * 
	 * @param conn
	 * @param seepTime
	 *            延迟时间
	 * @param score
	 *            分数
	 * @param randomType
	 *            随机模式{@link RandomType}：SHUFFLE ：打乱随机 RANDOM：随机数随机
	 * @return
	 * @throws IOException
	 */
	public Response openProxyConnection(Connection conn, long seepTime, int score, RandomType randomType) throws IOException {
		Response res = openOriginaProxyConnection(conn, seepTime);
		return res == null ? openUsedProxyConnection(conn, seepTime, score, randomType) : res;
	}

	/***
	 * 打开代理连接并执行请求，如原始代理不可用，则使用评分>=10的稳定代理，默认延迟0秒
	 * 
	 * @param conn
	 * @param randomType
	 *            随机模式{@link RandomType}：SHUFFLE ：打乱随机 RANDOM：随机数随机
	 * @return
	 * @throws IOException
	 */
	public Response openProxyConnection(Connection conn, RandomType randomType) throws IOException {
		return openProxyConnection(conn, 0, 10, randomType);
	}

	/***
	 * 打开代理连接并执行请求，如原始代理不可用，则使用稳定代理，默认延迟0秒
	 * 
	 * @param conn
	 * @param score
	 * @param randomType
	 *            随机模式{@link RandomType}：SHUFFLE ：打乱随机 RANDOM：随机数随机
	 * @return
	 * @throws IOException
	 */
	public Response openProxyConnection(Connection conn, int score, RandomType randomType) throws IOException {
		return openProxyConnection(conn, 0, score, randomType);
	}

	/**
	 * 打开代理连接并执行请求，如原始代理不可用，则使用满足分数条件的稳定代理，默认延迟0秒，使用打乱模式
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	public Response openProxyConnection(Connection conn, int score) throws IOException {
		return openProxyConnection(conn, 0, score, RandomType.SHUFFLE);
	}

	/***
	 * 代理评分
	 * 
	 * @param entity
	 */
	public void proxyScoring(ProxyEntity entity) {
		EvaluateScoringEntity e = null;
		String ipAndPort = entity.toString();
		e = userdIpPool.get(ipAndPort);

		if (StringUtils.isNullOrEmpty(e)) {// 判断代理是否存在
			e = new EvaluateScoringEntity();
			e.setCreateTime(new Date());
			e.setProxyEntity(entity);
		} else {
			e.setUpdatedTime(new Date());
		}
		e.setTimes(e.getTimes() + 1);
		e.setScore(entity.isValid() ? e.getScore() + 1 : e.getScore() - 1);
		userdIpPool.put(ipAndPort, e);
	}

	/***
	 * 获取当前代理
	 * 
	 * @return
	 */
	public java.net.Proxy getCurrentProxy() {
		return proxyEntity.getProxy();
	}

	/***
	 * 从使用过的代理池随机获取一条满足条件的代理，如使用过的代理池内没有代理，则从原始代理池获取
	 * 
	 * @param score
	 * @param randomType
	 *            随机模式{@link RandomType}：SHUFFLE ：打乱随机 RANDOM：随机数随机
	 * @return
	 */
	public java.net.Proxy getRandomUsedProxy(int score, RandomType randomType) {
		return getRandomUsedProxyEntity(score, randomType).getProxy();
	}

	/***
	 * 随机获取使用过的IP实体类,如使用过的代理池不满足条件则从原始代理池中抽取
	 * 
	 * @param score
	 *            分数
	 * @param randomType
	 *            随机模式{@link RandomType}：SHUFFLE ：打乱随机 RANDOM：随机数随机
	 * @return
	 *
	 */
	public ProxyEntity getRandomUsedProxyEntity(int score, RandomType randomType) {
		if (randomType == RandomType.SHUFFLE) {
			EvaluateScoringEntity e = getRandomUsedIpPool(score);// 从满足条件的代理集合中用打乱集合的形式抽取一条代理
			if (e != null) {
				return e.getProxyEntity();
			}
		} else if (randomType == RandomType.RANDOM) {
			List<EvaluateScoringEntity> list = getUsedIpPoolList(score);
			if (list.size() > 0) {
				return list.get(ThreadLocalRandom.current().nextInt(list.size())).getProxyEntity();// 从满足条件的代理集合中用随机数随机抽取一条代理
			}
		}
		return getProxyEntity();
	}

	/**
	 * 抽取满足条件的使用过的代理集合
	 * 
	 * @param score
	 *            分数
	 * @return
	 */
	public List<EvaluateScoringEntity> getUsedIpPoolList(int score) {
		List<EvaluateScoringEntity> list = new ArrayList<>();
		for (Entry<String, EvaluateScoringEntity> e : userdIpPool.entrySet()) {
			if (e.getValue().getScore() >= score) {
				list.add(e.getValue());
			}
		}
		return list;
	}

	/**
	 * 打乱使用过的代理池并随机返回一个满足条件的{@link EvaluateScoringEntity}实体
	 * 
	 * @param score
	 *            分数
	 * @return
	 */
	public EvaluateScoringEntity getRandomUsedIpPool(int score) {
		List<EvaluateScoringEntity> list = new ArrayList<>();
		list.addAll(userdIpPool.values());
		Collections.shuffle(list);
		for (EvaluateScoringEntity e : list) {
			if (e.getScore() >= score) {
				return e;
			}
		}
		return null;
	}

	/**
	 * 抽取所有使用过的代理集合
	 * 
	 * @return
	 */
	public List<EvaluateScoringEntity> getUsedIpPoolList() {
		return getUsedIpPoolList(Integer.MIN_VALUE);
	}

	/***
	 * 获取实例
	 * 
	 * @return
	 */
	public static ProxyPool getInstance() {// 双重锁，保证高并发下的可靠性
		if (instance == null) {
			synchronized (ProxyPool.class) {
				if (instance == null) {
					instance = new ProxyPool();
				}
			}
		}
		return instance;
	}

	/***
	 * 获取原始代理池剩余ip数量
	 * 
	 * @return
	 */
	public int getOriginalPoolSize() {
		return originalIpPool.size();
	}

	/***
	 * 获取使用过代理池剩余ip数量
	 * 
	 * @return
	 */
	public int getUsedPoolSize() {
		return userdIpPool.size();
	}

	/**
	 * 移除当前代理
	 */
	public void removeProxy() {
		if (proxyEntity == null) {
			return;
		}
		originalIpPool.removeElement(proxyEntity);
	}

	/**
	 * 从原始代理池获取代理
	 * 
	 * @param type
	 *            0 Not checked，1= ping，2 = http，3= ping and http
	 * @return
	 */
	public java.net.Proxy getProxy(int type) {
		return getValidProxy(type);
	}

	/**
	 * 从原始代理池获取代理 默认不验证有效性
	 * 
	 * @return java.net.Proxy
	 */
	public java.net.Proxy getProxy() {
		return getValidProxy(0);
	}

	/***
	 * 从原始代理池获取代理实体 默认不验证有效性
	 * 
	 * @return ProxyEntity
	 */
	public ProxyEntity getProxyEntity() {
		return new ProxyEntity(getProxy());
	}

	/**
	 * 随机从原始代理池获取一条代理
	 * 
	 * @return
	 */
	private void getRandomProxy() {
		setCache();
		int r = ThreadLocalRandom.current().nextInt(getOriginalPoolSize());
		proxyEntity = originalIpPool.get(r);
	}

	/***
	 * 获取有效代理
	 * 
	 * @param type
	 *            0= Not checked，1= Ping，2 = Http，3= Ping and Http
	 * @return
	 */
	private java.net.Proxy getValidProxy(int type) {
		while (true) {
			getRandomProxy();
			if (test(type)) {
				break;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return proxyEntity.getProxy();
	}

	/**
	 * 测试ip可用性
	 * 
	 * @param type
	 *            0= Not checked，1= Ping，2 = Http，3= Ping and Http
	 */
	private boolean test(int type) {
		boolean isSuccess = false;
		if (type == 3) {
			isSuccess = ping() ? http() : false;
		} else if (type == 2) {
			isSuccess = http();
		} else if (type == 1) {
			isSuccess = ping();
		} else if (type == 0) {
			isSuccess = true;
		}
		return isSuccess;
	}

	public static void main(String[] args) throws InterruptedException {
		ExecutorService exe = Executors.newFixedThreadPool(20);
		while (true) {
			exe.execute(new Runnable() {
				@Override
				public void run() {
					System.out.println(ProxyPool.getInstance().getProxy(2));
					ProxyPool.getInstance().removeProxy();
					System.out.println(ProxyPool.getInstance().getOriginalPoolSize());
				}
			});
			TimeUnit.MILLISECONDS.sleep(10);
		}
	}

	/***
	 * 请求Api获取代理ip
	 * 
	 * @return
	 */
	private Vector<ProxyEntity> getProxyList() {
		Map<String, Object> map = new HashMap<>();
		Vector<ProxyEntity> ipList = new Vector<>();
		try {
			Document doc = PageProcessor.create(this).url(BASE_URL).get();
			map = JsonUtils.getInstance().parse(Map.class, doc.text().toLowerCase());// 将返回的JSON数据转换成对象
			if (map != null && Integer.parseInt(map.get("code").toString()) == 0) {
				Map<String, List<String>> data = (Map<String, List<String>>) map.get("data");
				List<String> list = data.get("proxy_list");// 抽取返回的所有代理
				for (String s : list) {
					boolean f = s.matches("(.*?):(.*)");
					if (f) {// 判断ip合法性
						String[] arr = s.split(":");
						ipList.addElement(new ProxyEntity(arr[0], Integer.parseInt(arr[1])));
					}
				}
			}
		} catch (IOException e) {
			errorOutTime = errorOutTime >= 5000 ? 100 : errorOutTime + 100;
		}
		return ipList;
	}

	/***
	 * API请求
	 * 
	 * @param ignoreExceptions
	 *            是否忽略异常，如为True则请求到非异常为止（不推荐，除非业务需要，或加错误计数器用以停止），反之则只请求一次
	 * @return
	 * @throws InterruptedException
	 */
	private Vector<ProxyEntity> getProxyList(boolean ignoreExceptions) {
		while (true) {
			originalIpPool = getProxyList();
			if (!ignoreExceptions) {
				break;
			}
			if (originalIpPool != null && getOriginalPoolSize() > 0) {
				break;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(errorOutTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return originalIpPool;
	}

	/***
	 * Ping 轻验证
	 * 
	 * @param proxy
	 * @return
	 */
	private boolean ping() {
		boolean state = false;
		try {
			InetAddress address = InetAddress.getByName(proxyEntity.getIp());
			state = address.isReachable(3000);
		} catch (IOException e) {
		} finally {
			if (!state) {
				removeProxy();
			}
		}
		return state;
	}

	/***
	 * Http 验证
	 * 
	 * @return
	 */
	private boolean http() {
		boolean state = false;
		try {
			Response res = PageProcessor.create(this).url("http://1212.ip138.com/ic.asp").proxy(proxyEntity.getProxy()).execute();
			if (res.statusCode() == 200) {
				state = true;
			}
		} catch (IOException e) {
		} finally {
			if (!state) {
				removeProxy();
			}
		}
		return state;
	}

	@Override
	public CrawlerSite getSite() {
		return site;
	}

}
