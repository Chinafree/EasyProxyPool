package cn.ryan.proxypool.utils;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * json操作类
 * 
 * @author RuiStatham
 */
public class JsonUtils {
	private static JsonUtils instance;
	private ObjectMapper mapper;

	private JsonUtils() {
		mapper = new ObjectMapper();
	}

	/***
	 * 获取实例
	 * 
	 * @return
	 */
	public static JsonUtils getInstance() {// Jackson在单例模式下可拥有高性能
		if (instance == null) {
			instance = new JsonUtils();
		}
		return instance;
	}

	/**
	 * json String 转换成对象
	 * 
	 * @param type
	 * @param str
	 * @return 成功转换返回对象，失败返回null
	 */
	public <T> T parse(Class<T> type, String str) {
		if (str == null) {
			return null;
		}
		try {
			JsonParser jp = mapper.getJsonFactory().createJsonParser(str);
			jp.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
			T t = mapper.readValue(jp, type);
			return t;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 对象转换成json String
	 * 
	 * @param t
	 * @return
	 */
	public String format(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}