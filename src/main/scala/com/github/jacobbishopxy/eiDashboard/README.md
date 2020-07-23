# eiDashboard REST

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

3. store

    - GET: `http://localhost:2020/ei/dashboard/industry-store?collection=layout-dev&identity=%231&category=text`
    
    - POST: `http://localhost:2020/ei/dashboard/industry-store?collection=layout-dev`
    ```
    {
        "anchor": {
            "identity": "#1",
            "category": "text",
            "symbol": "000001",
            "date": "20200721"
        },
        "content": {
            "data": "dev data321",
            "config": "dev config"
        }
    }
    ```

4. layout

    - GET: `http://localhost:2020/ei/dashboard/template-layout?collection=layout-dev2&template=dev&panel=p`
    
    - POST: `http://localhost:2020/ei/dashboard/template-layout?collection=layout-dev2`
    ```
    {
        "templatePanel": {
            "template": "dev",
            "panel": "p"
        },
        "layouts": [
            {
                "anchor": {
                    "identity": "num1",
                    "category": "text",
                    "symbol": "000001",
                    "date": "20200101"
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

