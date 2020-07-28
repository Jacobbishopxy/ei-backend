# eiDashboard REST

## Routes

1. grid-layouts

    - GET: `http://localhost:2020/ei/dashboard/grid-layouts?db=market&collection=layout`

2. grid-layout

    - GET: `http://localhost:2020/ei/dashboard/grid-layout?db=market&collection=layout&template=600036&panel=intro`
    
    - POST: `http://localhost:2020/ei/dashboard/grid-layout?db=market&collection=layout&template=600036&panel=intro`
    ```
    {
        "layouts": [
            {
                "content": {
                    "contentConfig": "600036.SH 招商银行",
                    "contentData": "http://localhost:4013/bank",
                    "contentType": "proFile",
                    "title": "test"
                },
                "coordinate": {
                    "h": 7,
                    "i": "2020-07-15T09:30:53+08:00",
                    "w": 24,
                    "x": 0,
                    "y": 0
                }
            }
        ],
        "panel": "intro",
        "template": "600036"
    }
    ```

3. industry-store-fetch

    - POST: `http://localhost:2020/ei/dashboard/industry-store-fetch?collection=dev`
    ```
    {
        "anchorKey": {
            "identity": "num1",
            "category": "text"
        },
        "anchorConfig": {
            "symbol": "000001",
            "date": "20200721"
        }
    }
    ```

4. industry-stores-fetch

    - POST: `http://localhost:2020/ei/dashboard/industry-stores-fetch?collection=dev`
    ```
    [
        {
            "anchorKey": {
                "identity": "num1",
                "category": "text"
            },
            "anchorConfig": {
                "symbol": "000001",
                "date": "20200721"
            }
        },
        {
            "anchorKey": {
                "identity": "num2",
                "category": "text"
            },
            "anchorConfig": {
                "symbol": "000001",
                "date": "20200721"
            }
        }
    ]
    ```

5. industry-store-modify

    - POST: `http://localhost:2020/ei/dashboard/industry-store-modify?collection=dev`
    ```
    {
        "anchorKey": {
            "identity": "num1",
            "category": "text"
        },
        "anchorConfig": {
            "symbol": "000001",
            "date": "20200101"
        },
        "content": {
            "data": "dev data321",
            "config": "dev config"
        }
    }
    ```

5. industry-stores-modify

    - POST: `http://localhost:2020/ei/dashboard/industry-stores-modify?collection=dev`
    ```
    {
        "anchorKey": {
            "identity": "num1",
            "category": "text"
        },
        "anchorConfig": {
            "symbol": "000001",
            "date": "20200101"
        },
        "content": {
            "data": "dev data321",
            "config": "dev config"
        }
    }
    ```

6. industry-store-remove

    - POST: `http://localhost:2020/ei/dashboard/industry-store-remove?collection=dev`
    ```
    {
        "anchorKey": {
            "identity": "num1",
            "category": "text"
        },
        "anchorConfig": {
            "symbol": "000001",
            "date": "20200101"
        }
    }
    ```

7. industry-stores-remove

    - POST: `http://localhost:2020/ei/dashboard/industry-stores-remove?collection=dev`
    ```
    [
        {
            "anchorKey": {
                "identity": "num1",
                "category": "text"
            },
            "anchorConfig": {
                "symbol": "000001",
                "date": "20200101"
            }
        },
        {
            "anchorKey": {
                "identity": "num2",
                "category": "text"
            },
            "anchorConfig": {
                "symbol": "000001",
                "date": "20200101"
            }
        }
    ]
    ```

8. template-layout

    - GET: `http://localhost:2020/ei/dashboard/template-layout?collection=dev&template=dev&panel=p`
    
    - POST: `http://localhost:2020/ei/dashboard/template-layout?collection=dev`
    ```
    {
        "templatePanel": {
            "template": "dev",
            "panel": "p"
        },
        "layouts": [
            {
                "anchorKey": {
                    "identity": "num1",
                    "category": "text"
                },
                "coordinate": {
                    "x": 1,
                    "y": 2,
                    "h": 3,
                    "w": 4
                }
            }
        ]
    }
    ```

9. template-layout-remove

    - POST: `http://localhost:2020/ei/dashboard/template-layout-remove?collection=dev`
    ```
    {
        "template": "dev",
        "panel": "p"
    }
    ```

10. template-layout-industry-store-modify

    - POST: `http://localhost:2020/ei/dashboard/template-layout-industry-store-modify?collection=dev`
    ```
    {
        "templatePanel": {
            "template": "dev",
            "panel": "p"
        },
        "layouts": [
            {
                "anchorKey": {
                    "identity": "num1",
                    "category": "text"
                },
                "coordinate": {
                    "x": 1,
                    "y": 2,
                    "h": 3,
                    "w": 4
                }
            }
        ],
        "stores": [
            {
                "anchorKey": {
                    "identity": "num1",
                    "category": "text"
                },
                "anchorConfig": {
                    "symbol": "000001",
                    "date": "20200101"
                },
                "content": {
                    "data": "dev data321",
                    "config": "dev config"
                }
            }
        ]
    }
    ```
