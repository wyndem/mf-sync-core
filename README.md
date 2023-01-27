## 项目名称
> 兼容Mysql和SF的同步核心

## SF同步
相关API:
- 单查询

最高可查2千条数据
> /services/data/v55.0/query



- 多查询

多查询使用复合API，每次可查5条。如数据超过5条，会分批进行查询
> /services/data/v55.0/composite/

- 更新和新增

更新和新增是分开调用接口，而不是直接使用upsert接口。最高200条，超过则分批。具体文档如下：
>  https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_composite_sobjects_collections.htm

### 注意
更新和新增不允许使用别的外部id进行upsert，具体原因看上述文档。


## MYSQL同步
- 单查询 无限制
- 多查询 无限制
- 更新和新增 无限制


## 注意
多查询中，每个查询语句中最多只返回一条数据