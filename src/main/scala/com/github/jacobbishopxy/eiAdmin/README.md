# eiAdmin REST

1. show-collections
    
    - GET: `http://localhost:2020/ei/admin/show-collections`

1. does-collection-exist

    - GET: `http://localhost:2020/ei/admin/does-collection-exist?collection=demo`

1. show-collection

    - GET: `http://localhost:2020/ei/admin/show-collection?collection=demo`

1. create-collection

    - POST: `http://localhost:2020/ei/admin/create-collection`
    ```
    {
        "collectionName": "demo",
        "fields": [
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

1. drop-collection

    - GET: `http://localhost:2020/ei/admin/drop-collection?collection=demo`

1. modify-validator

    - POST: `http://localhost:2020/ei/admin/modify-validator?collection=demo`

    - ps: add and remove can be place in the same actions list

    - add a field:
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
    
    - remove a field:
    ```
    {
        "actions": [
            {
                "fieldName": "preClose"
            }
        ]
    }
    ```

1. modify-collection

    - POST: `http://localhost:2020/ei/admin/modify-collection?collection=demo`

    - remove `close` and add `preClose`
    ```
    {
        "collectionName": "demo",
        "fields": [
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
                "fieldName": "preClose",
                "nameAlias": "前收",
                "fieldType": 1,
                "description": "带小数点"
            }
        ]
    }
    ```

1. show-index

    - GET: `http://localhost:2020/ei/admin/show-index?collection=demo`

1. insert-data

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

1. query-data

    - POST: `http://localhost:2020/ei/admin/query-data?collection=demo`
    ```
    {
        "limit": 10,
        "filter": {
            "$and": [
                {
                    "date": {
                        "$gt": 20190101,
                        "$lte": 20200501
                    }
                },
                {
                    "symbol": "000001.SZ"
                }
            ]
        }
    }
    ```

1. delete-data

    - POST: `http://localhost:2020/ei/admin/delete-data?collection=demo`
    ```
    "filter": {
        "$and": [
            {
                "date": {
                    "$gt": 20190101,
                    "$lte": 20200501
                }
            },
            {
                "symbol": "000001.SZ"
            }
        ]
    }
    ```
