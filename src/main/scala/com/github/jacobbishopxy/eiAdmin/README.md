# eiAdmin REST

1. show-collections

2. create-collection

    - post JSON:
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

4. show-validator

5. modify-validator

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

- post JSON:
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

- post JSON:
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

- post JSON:
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
