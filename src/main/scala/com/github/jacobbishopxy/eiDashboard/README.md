# eiDashboard REST

## Routes

1. industry-store-fetch

    POST: `http://localhost:2020/ei/dashboard/industry-store-fetch?collection=dev`
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
    
    result:
    ```
    {
        "anchorConfig": {
            "date": "20200101",
            "symbol": "000001"
        },
        "anchorKey": {
            "category": "text",
            "identity": "num1"
        },
        "content": {
            "config": "dev config",
            "data": "dev data321"
        }
    }
    ```


1. industry-stores-fetch

    POST: `http://localhost:2020/ei/dashboard/industry-stores-fetch?collection=dev`
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

    result:
    ```
    [
       {
           "anchorConfig": {
               "date": "20200101",
               "symbol": "000001"
           },
           "anchorKey": {
               "category": "text",
               "identity": "num1"
           },
           "content": {
               "config": "dev config",
               "data": "dev data"
           }
       },
       {
           "anchorConfig": {
               "date": "20200101",
               "symbol": "000001"
           },
           "anchorKey": {
               "category": "text",
               "identity": "num2"
           },
           "content": {
               "config": "dev c1",
               "data": "dev "
           }
       }
    ]
    ```
   

1. industry-store-modify

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
    
    result:
    ```
    {
       "matchedCount": 0,
       "modifiedCount": 0,
       "upsertedId": "BsonObjectId{value=5f28c455e519071017732e65}"
    }
    ```


1. industry-stores-modify

    - POST: `http://localhost:2020/ei/dashboard/industry-stores-modify?collection=dev`
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
            },
            "content": {
                "data": "dev data",
                "config": "dev config"
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
            },
            "content": {
                "data": "dev ",
                "config": "dev c1"
            }
        }
    ]
    ```
   
    result: 
    ```
    {
       "insertedCount": 0,
       "matchedCount": 1,
       "modifiedCount": 1,
       "removedCount": 0,
       "upserts": [
           {
               "id": "BsonObjectId{value=5f28c54ce519071017732fdc}",
               "index": 1
           }
       ]
    }
    ```

   
1. industry-store-remove

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
   
    result:
    ```
    {
       "deletedCount": 1
    }
    ```


1. industry-stores-remove

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
   
    result:
    ```
    {
       "deletedCount": 1
    }
    ```

1. template-layout-fetch

    - POST: `http://localhost:2020/ei/dashboard/template-layout-fetch?collection=dev`
    ```
    {
        "template": "dev",
        "panel": "p"
    }
    ```
    
    result:
    ```
    {
        "layouts": [
            {
                "anchorKey": {
                    "category": "text",
                    "identity": "num1"
                },
                "coordinate": {
                    "h": 3,
                    "w": 4,
                    "x": 1,
                    "y": 2
                }
            }
        ],
        "templatePanel": {
            "panel": "p",
            "template": "dev"
        }
    }
    ```

1. template-layout-modify
       
    - POST: `http://localhost:2020/ei/dashboard/template-layout-modify?collection=dev`
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

    result:
    ```
    {
        "matchedCount": 0,
        "modifiedCount": 0,
        "upsertedId": "BsonObjectId{value=5f28c65be519071017733164}"
    }
    ```

1. template-layout-remove

    - POST: `http://localhost:2020/ei/dashboard/template-layout-remove?collection=dev`
    ```
    {
        "template": "dev",
        "panel": "p"
    }
    ```
    
    result:
    ```
    [
        {
            "deletedCount": 0
        },
        {
            "deletedCount": 1
        }
    ]
    ```

1. template-layout-industry-store-modify

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
    
    result:
    ```
    [
        {
            "matchedCount": 0,
            "modifiedCount": 0,
            "upsertedId": "BsonObjectId{value=5f28c713e51907101773326a}"
        },
        {
            "insertedCount": 0,
            "matchedCount": 0,
            "modifiedCount": 0,
            "removedCount": 0,
            "upserts": [
                {
                    "id": "BsonObjectId{value=5f28c713e51907101773326c}",
                    "index": 0
                }
            ]
        }
    ]
    ```

