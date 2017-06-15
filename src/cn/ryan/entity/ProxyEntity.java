package cn.ryan.entity;

import java.net.InetSocketAddress;
import java.net.Proxy.Type;

import org.jsoup.helper.Validate;

public class ProxyEntity {
	private String ip;// ip
	private Integer port;// 端口
	private boolean isCheck;// 是否验证
	private boolean isValid;// 验证是否有效

	public java.net.Proxy getProxy() {
		return new java.net.Proxy(Type.HTTP, new InetSocketAddress(ip, port));
	}

	public ProxyEntity(java.net.Proxy p) {
		Validate.notNull(p);
		String address = p.address().toString();
		String[] arr = address.split("/")[1].split(":");
		this.ip = arr[0];
		this.port = Integer.parseInt(arr[1]);
	}

	public ProxyEntity(String ip, Integer port) {
		super();
		this.ip = ip;
		this.port = port;
	}

	public String toString() {
		return ip + ":" + port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public boolean isCheck() {
		return isCheck;
	}

	public void setCheck(boolean isCheck) {
		this.isCheck = isCheck;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isCheck = true;
		this.isValid = isValid;
	}

}
