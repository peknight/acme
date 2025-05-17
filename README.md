# ACME
[Automated Certificate Management Environment (ACME) Protocol](https://www.iana.org/assignments/acme/acme.xhtml)

想跑个https服务太难了，

我要起个acme.sh去定时申请更新证书，

还要起一个nginx加载证书然后反向代理自己的http服务（当然之前我菜，没研究明白怎么直接用证书起https服务）

我还得在证书快要过期的时候手动重启一下服务，要不然证书一过期服务就挂了（当然也可以写个cron脚本去重启）

我想在自己的一个应用里搞定这套流程，但是我还希望代码是函数式的。  

所以我照着RFC8555规范和acme4j库手撸了一套基于http4s的函数式ACME客户端库。  

太难了，为此我还手撸了一套函数式jose库，为了jose库手撸了一套函数式的加解密security库。  

为了处理一些codec问题也对我的codec库做了大量改进。  

整个ACME客户端协议我都成功实现完了，当然我自己用只用到Let's Encrypt作为证书提供商，Cloudflare作为完成DNS挑战的客户端。

对于其它类型的挑战我没有做实现，其它的提供商我也没有对接。  

基于这个ACME客户端我终于实现了自动获取证书然后启动https服务并且在证书临期时自动获取新证书重启ebrl的功能，美滋滋。