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