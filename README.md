# Blockchain
java实现简单的区块链

本程序未使用任何框架，仿照springboot框架制作了定制URL匹配。

使用serversocket对http请求进行了简单处理。

使用@RestController进行action类的注入，使用@RequestMapping(value="此处放置URL",produces="此处放置content-type")进行URL定制。

提供了request参数自动转化为方法的参数，且提供了JSON文本直接转化为bean的方法。

提供JSON转复杂类型的bean的方法，例如json转化为Map<String,List<Bean>>类型。

使用基础的POW（工作量证明）和共识算法制作的区块链。

通过访问http://localhost:5000/mine进行挖矿操作。

访问http://localhost:5000/transactions/new，可上传如下的交易信息，并添加到块中

{"amount": 1,"recipient": "a5ddb3452525492cbec71f3fba97ff69","sender": "0"}

访问http://localhost:5000/chain可以获取所有区块链信息。

访问http://localhost:5000/nodes/register进行节点注册。

访问http://localhost:5000/nodes/resolve进行节点间区块链同步。
