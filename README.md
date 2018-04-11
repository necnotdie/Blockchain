# Blockchain
java实现简单的区块链
本程序未使用任何框架，仿照springboot制作了定制URL匹配，时间仓促实现了基本功能。
提供JSON转复杂类型的bean的方法。
使用基础的POW（工作量证明）和共识算法制作的区块链。
通过访问http://localhost:5000/mine进行挖矿操作。
访问http://localhost:5000/chain，可上传如下的交易信息，并添加到块中
{"amount": 1,"recipient": "a5ddb3452525492cbec71f3fba97ff69","sender": "0"}
