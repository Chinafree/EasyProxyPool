package cn.ryan.entity;

import java.util.Date;

public class EvaluateScoringEntity {
	private int score;// 得分
	private double speed;// 速度
	private int times;// 评分次数
	private Date createTime;// 创建时间
	private Date updatedTime;// 最后更新时间
	private ProxyEntity proxyEntity;// 代理实体

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}

	public ProxyEntity getProxyEntity() {
		return proxyEntity;
	}

	public void setProxyEntity(ProxyEntity proxyEntity) {
		this.proxyEntity = proxyEntity;
	}

	public int getTimes() {
		return times;
	}

	public void setTimes(int times) {
		this.times = times;
	}

}
