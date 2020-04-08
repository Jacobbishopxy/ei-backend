# eiAdmin REST

1. show-collections
    
    - GET: `http://localhost:2020/ei/admin/show-collections`

2. create-collection

    - POST: `http://localhost:2020/ei/admin/create-collection`
    ```
    {
        "collectionName": "demo",
        "cols": [
            {
                "fieldName": "symbol",
                "nameAlias": "代码",
                "fieldType": 2,
                "indexOption": {
                    "ascending": false
                },
                "description": "股票代码，例：000001.SZ"
            },
            {
                "fieldName": "date",
                "nameAlias": "日期",
                "fieldType": 16,
                "indexOption": {
                    "ascending": true
                },
                "description": "例：20200101"
            },
            {
                "fieldName": "close",
                "nameAlias": "收盘价",
                "fieldType": 1,
                "description": "带小数点，例：10.2"
            }
        ]
    }
    ```

3. show-index

    - GET: `http://localhost:2020/ei/admin/show-index?collection=demo`

4. show-validator

    - GET: `http://localhost:2020/ei/admin/show-validator?collection=demo`

5. modify-validator

    - POST: `http://localhost:2020/ei/admin/modify-validator?collection=demo`

    - ps: add and remove can be place in the same actions list

    - add field:
    ```
    {
        "actions": [
            {
                "fieldName": "preClose",
                "nameAlias": "前收",
                "fieldType": 1,
                "description": "带小数点"
            }
        ]
    }
    ```
    
    - remove field:
    ```
    {
        "actions": [
            {
                "fieldName": "preClose"
            }
        ]
    }
    ```

6. insert-data

    - POST: `http://localhost:2020/ei/admin/insert-data?collection=demo`
    ```
    [
        {
            "symbol": "000002.SZ",
            "date": 20200101,
            "close": 7.0
        },
        {
            "symbol": "000002.SZ",
            "date": 20200102,
            "close": 7.1
        }
    ]
    ```

7. query-data

    - POST: `http://localhost:2020/ei/admin/query-data?collection=demo`
    ```
    {
        "limit": 10,
        "filter": {
            "and": {
                "date": {
                    "gt": 20190101,
                    "lte": 20200501
                },
                "symbol": {
                    "eq": "000002.SZ"
                }
            }
        }
    }
    ```

8. delete-data

    - POST: `http://localhost:2020/ei/admin/delete-data?collection=demo`
    ```
    {
        "filter": {
            "and": {
                "symbol": {
                    "eq": "000002.SZ"
                },
                "date": {
                    "eq": 20200102
                }
            }
        }
    }
    ```
