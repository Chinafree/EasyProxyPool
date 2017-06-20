# EasyProxyPool
## 自动预热高性能代理池  
核心逻辑在cn.ryan.proxy.ProxyPool.java  
根据自己的业务重构该类，理论上可满足绝大部分代理需求。
# 核心
1：originalIpPool（原始代理池）
<br>
2：userdIpPool（使用过的代理池，附带IP质量评分）  
默认使用的RyanCrawler执行请求，可重写openProxyConnection支持更多组件。
## 注：
1.获取代理API接口需根据API结构做定制化重写getProxyList。
<br>
2.因代理池使用的是Jvm的内存，故建议每次API获取的代理数不要太大，否则会对代理池性能造成一定影响。
<br>
3.本源码只提供了核心（项目可正常运行），部分持久化/缓存等等功能需自己手动实现。  
