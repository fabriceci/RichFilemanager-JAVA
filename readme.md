# Java connector for RFM plugin

_This is a (quick) conversion of the RFM PHP connector tested with Spring MVC. The code needs to be refactored and fully unit tested, but it can be already used._

Requirement:

- Servlet API >= 3.1+
- Java >= 7

## Demo project

[Example with Spring MVC](https://github.com/fabriceci/RichFilemanager-JAVA-demo-Spring)

## Installation

### Plugin configuration (JavaScript side)

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

### Java side

#### Add the repository as dependecy

The easiest way to do that is to use [JitPack.io](https://github.com/jitpack/jitpack.io)

Here is an example with Gradle (there a maven example available in the JitPack repository)

```
    allprojects {
        repositories {
            jcenter()
            maven { url "https://jitpack.io" }
        }
   }
   dependencies {
        compile 'com.github.fabriceci:RichFilemanager-JAVA:master-SNAPSHOT'
   }
```

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

## configuration

There are two ways to override the configuration. Please read the [filemanager.config.default.properties](https://github.com/fabriceci/RichFilemanager-JAVA/blob/master/src/main/resources/filemanager.config.default.properties) to have more information.

### Add a file property

Add to your resources folder the file: [filemanager.config.default.properties](https://github.com/fabriceci/RichFilemanager-JAVA/blob/master/src/main/resources/filemanager.config.default.properties) to override the default configuration

### Override during runtime

You can pass a property Map into the constructor:

```
@RequestMapping(value = "fm/api")
public void fm(ModelMap model, HttpServletRequest request, HttpServletResponse response) throws IOException, FileManagerException {
        Map<String,String> options = new HashMap<>();
        options.put("propertyName", "value");
        new LocalFileManager(options).handleRequest(request, response);
}
```

## Note

It's optional, but I **strongly recommend** to add the [twelvemonkeys library](https://github.com/haraldk/TwelveMonkeys) to avoid errors during thumbnail generations.

`compile('com.twelvemonkeys.imageio:imageio-jpeg:3.3.+');`
