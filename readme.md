# Java connector for RFM plugin

*This is a (quick) conversion of the RFM PHP connector tested with Spring MVC.
 The code needs testing and a big refactoring, but it can be already used.*

Requirement:

* Servlet 3.1+ API.
* Java 7+

## Installation

### JS part

#### Set the connectorUrl (filemanager.config.js)

```
 "api": {
        ...
        "connectorUrl": "/your/path/to/api",
        ...
    }
```

#### Set the previewUrl to false (filemanager.config.js)

We will use the connector to show images

```
"viewer": {
  ...
  "absolutePath": false,
   ...
}
```

#### Set the base Url in the initialisation of the connector

The path where your RFM JS plugin is located

```
$('.fm-container').richFilemanager({
   baseUrl: "/your/path/to/fm/folder",
});
```

### Java part

#### (Spring MVC) Create a controller to handle the manager

```
@Controller
@RequestMapping(value = "/admin/fileManager")
public class AdminFileManagerController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String index(ModelMap model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //content of the index.html in the (JS) plugin's folder
        return "admin/rfm/home";
    }

    @RequestMapping(value = "/api")
    public void fm(ModelMap model, HttpServletRequest request, HttpServletResponse response) throws IOException, FileManagerException {
        new LocalFileManager().handleRequest(request, response);
    }
}
```

#### configuration

There is two way to override the configuration. Please read the filemanager.config.default.properties to have more information.

##### Add a file property

Add to your resources folder the file: filemanager.config.properties to override the default configuration

##### Override during runtime.

You can pass a property Map into the constructor:

```
@RequestMapping(value = "fm/api")
public void fm(ModelMap model, HttpServletRequest request, HttpServletResponse response) throws IOException, FileManagerException {
        Map<String,String> options = new HashMap<>();
        options.put("propertyName", "value");
        new LocalFileManager(options).handleRequest(request, response);
}
```

#### Note

It's optional, but I **strongly recommend** to add the [twelvemonkeys library](https://github.com/haraldk/TwelveMonkeys) to avoid errors during thumbnail generations.

`compile('com.twelvemonkeys.imageio:imageio-jpeg:3.3.2');`
