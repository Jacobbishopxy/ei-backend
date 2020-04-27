# eiGridLayout REST

1. grid-layout-all

    - GET: `http://localhost:2020/ei/utils/grid-layout-all`

2. grid-layout

    - GET: `http://localhost:2020/ei/utils/grid-layout?panel=test`
    
    - POST: `http://localhost:2020/ei/utils/grid-layout`
    ```
    {
        "panel": "test",
        "layouts": [
            {
                "coordinate": {
                    "i": "2020-03-19T13:38:19+08:00",
                    "x": 0,
                    "y": 0,
                    "h": 4,
                    "w": 5
                },
                "content": {
                    "title": "t1",
                    "contentType": "embedLink",
                    "hyperLink": ""
                }
            },
            {
                "coordinate": {
                    "i": "2020-03-20T10:37:50+08:00",
                    "x": 6,
                    "y": 0,
                    "h": 4,
                    "w": 6
                },
                "content": {
                    "title": "t2",
                    "contentType": "embedLink",
                    "hyperLink": ""
                }
            }
        ]
    }
    ```