# commonplugin
自定义 Gradle 插件，详情移步：https://juejin.im/post/5cff56d3f265da1ba431e46c
   
   
   
   >Gradle系列分2章<br>
[上篇Android Gradle Groovy自动化构建入门篇](https://juejin.im/post/5a5b366af265da3e58594f00)<br>
[下篇Android Gradle Groovy自动化构建进阶篇](https://juejin.im/post/5cff56d3f265da1ba431e46c)

[上篇，我们已经介绍了Gradle的基本语法](https://www.jianshu.com/p/20cdcb1bce1b)，接下来让我们一起学习下Gradle高级知识：构建脚本，自定义任务，构建生命周期，解决依赖冲突，多项目构建等高阶技巧。
## 接下来，我们先看下gradle的几个个简单应用
*  由于项目越来越大，组件化盛行，有时我们不得不多module统一版本管理。一般做法是：

>1. 在 project 根目录新建****.gradle 文件；
>2. 通过`apply from`引入该配置文件，然后使用 `rootProject.ext`引入相关属性.

```
//1.比如这里新建文件为config.gradle
ext {  
    versions = [  
            sdkMinVersion     : 14,  
            sdkTargetVersion  : 26,  
            ... 
    ]  
   
    depVersion = [  
            appCompatVersion : "26.+",  
            recyclerViewVersion : "26.0.0-alpha1"  
    ]  

    deps = [  
        suport : [  
                appcompat   : "com.android.support:appcompat-v7:${depVersion.appCompatVersion}",  
                recyclerview: "com.android.support:recyclerview-v7:${depVersion.recyclerViewVersion}"  
        ]  
    ]  
}  

// 2. 引入已声明好的属性
apply from: 'config/config.gradle' 
android {  
    def versions = rootProject.ext.versions  
    compileSdkVersion versions.sdkCompileVersion  
    buildToolsVersion versions.toolsBuildVersion  
     ...
  }  
  
dependencies {  
    def dependencies = rootProject.ext.deps  
    compile dependencies.suport.appcompat  
}

```

* 在比如我们经常看到的这个错误：

```
Error:Execution failed for task ':test:processDebugManifest'.> Manifest merger failed with multiple errors, see logs
```
我们一般的解决方案为：命令行输入`gradlew processDebugManifest --stacktrace`.

* 最后我们在看个简单的单机执行任务
![-w523](https://user-gold-cdn.xitu.io/2019/6/11/16b456b24efdbf6a?w=1046&h=1394&f=jpeg&s=96247)

* 由于开发过程中经常导入第三方jar包，一不小心就报jar包冲突这，这时我们会执行
`gradle app:dependencies ` 查看app重复依赖，然后在通过exclude剔除重复的jar。

```
compile ('com.android.support:design:22.2.1')
            {
                exclude group: 'com.android.support'
            }

```
其实当我们点击后Gradle会去寻找当前目录下的 build.gradle 的文件，这个文件是 Gradle 的脚本文件，它里面定义了工程和工程拥有的所有任务等信息。然后执行相关task。下面我们一起来一步步揭开它的神秘面纱吧。

##  Gradle 中的工程（ Project ）和任务（ Task ）
就像上面的截图一样，我们知道，每一个 Gradle 的项目都会包含一个或多个工程，每一个工程又由一个或多个任务组成，一个任务代表了一个工作的最小单元，它可以是一次类的编译、打一个 JAR 包、生成一份 Javadoc 或者是向仓库中提交一次版本发布。

### 任务的定义和使用

>在任务中，我么可以利用`dependsOn`定义依赖关系，`doFirst`、`doLast`对现有任务增强。
我们还是使用 IDEA 开发工具打开之前的项目工程，把之前 build.gradle 文件中所有的内容全部删除，编写输入如下代码

```
task hello {
    doLast {
        println 'Hello world!'
    }
}

task release() {
    doLast {
        println "I'm release task"
    }
}

// 添任务依赖关系
release.dependsOn hello

//对现有的任务增强
// 法方一，在doFirst动作中添加
hello.doFirst {
    println 'Hello doFirst'
}
// 法方二 在doLast动作中添加
hello.doLast {
    println 'Hello doLast'
}
```
打开命令行端终，执行命令:` gradle -q release`，输出结果如下：![-w558](https://user-gold-cdn.xitu.io/2019/6/11/16b456b24ec44231?w=1116&h=1266&f=jpeg&s=97531)
另外我们还可以为任务设置属性，主要通过 ext.myProperty 来初始化值，如下所示

```
task myTask {
    ext.myProperty = "myValue"
}

task printTaskProperties {
    doLast {
        println myTask.myProperty
    }
}
命令行执行 ➜  gradle -q printTaskProperties
myValue
```
当然我们可以对现有任务进行配置：禁用或者重写。
首先我们定义一个 myCopy 的任务，代码如下：
```
task myCopy(type: Copy) {
   from 'resources'
   into 'target'
   include('**/*.txt', '**/*.xml', '**/*.properties')
}
```
类似java有API文档，Gradle也有类似文档，Gradle 中很多其它常用的任务，小伙伴们可以[点击查看](https://docs.gradle.org/3.3/dsl/org.gradle.api.tasks.Copy.html)，
如果我们想重写 copy任务可通过overwrite属性为 true 来现如下所示：

```
task copy(type: Copy)

task copy(overwrite: true) {
    doLast {
        println('overwrite the copy.')
    }
}
命令行执行 
> gradle -q copy
overwrite the copy.
```
最后我们看下如何禁用某些任务。直接看代码吧

```
copy.enabled = false
```
## 实战篇
 接下来我们来自定义插件，有三种方式来编写
1. 在我们构建项目的 build.gradle 脚本中直接编写，这种方式的好处是插件会自动被编译加载到我们的 classpath 中，但是它有很明显的局限性，就是除了在包括它的脚本外别的地方无法复用。

2. 在我们构建项目的rootProjectDir/buildSrc/src/main/groovy 目录下编写，Gradle 会自动编译到当前项目的 classpath 中，该项目下所有编译脚本都可以使用，但是除了当前项目之外的都无法复用。

3. 以单独的工程方式编写，这个工程最终编译发布为一个 JAR 包，它可以在多个项目或不同的团队中共享使用。

接下来我们一步步把按照第三种方式写个Demo吧。
#### 首先使用IDEA新建gradle 工程选择groovy（跟上面一样就不细说了），然后按照下面截图新建src/main/groovy/你的包名，接着在resources 目录下建立 META-INF/gradle-plugins 文件夹，在其中新建 hello.properties 文件，敲黑板注意此处文件名，就是以后使用时要用的名字。此处是hello，所以我们得按照这样引入`apply plugin: 'hello'`，里面像这样输入插件的全路径名字：`implementation-class=org.gradle.HelloPlugin`
![-w1092](https://user-gold-cdn.xitu.io/2019/6/11/16b456b24f19963f?w=1240&h=990&f=jpeg&s=97235)

#### 接下来分 2 步编写代码：
>1. 继承自DefaultTask的，使用TaskAction进行标注，这样 Gradle 就会在任务执行的时候默认调用它 
>2. 然后通过实现Plugin接口来实现自定义插件类，实现apply(Project project) 方法。

按照步骤，首先我们新建MyTask.groovy文件，里面仅仅是简单声明了一个成员变量，然后打印。

```
class MyTask extends DefaultTask {
    String input = 'hello from MyTask'

    @TaskAction
    def greet() {
        println input
    }
}

```
然后我们新建Hello.groovy文件.我们向这个plugin添加了一个`hello任务`,我们知道gradle中可以配置参数比如：` defaultConfig {} ndk {}`等，其实gradle是使用 extension objects来现实给插件传参，具体实现看下面代码的注释：

```
class Hello implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // 向extension container保存para参数，并应用给HelloPluginExtension
        project.extensions.create("para", HelloPluginExtension)
        // 向project对象添加hello任务
        project.task('hello',type:MyTask) {
            input = 'Hello Plugin input!'
            doLast {
                println "${project.para.first}${project.para.last}"
            }
        }
    }
}
```

#### 接下来发布工程到本地仓库，供其他项目使用，在`build.gradle`中输入

```
//使用 maven-publish 插件先发布到本地
apply plugin: 'maven-publish'
publishing{
    publications {
        mavenJava(MavenPublication) {
            from components.java

            groupId 'org.gradle'
            artifactId 'customPlugin'
            version '1.0-SNAPSHOT'

        }
    }

    repositories{
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            url "../repo"
        }
    }
}
```
点击publish任务，看到成功发布工程到本地仓库../repo中了。
![-w1215](https://user-gold-cdn.xitu.io/2019/6/11/16b456b24f2c0586?w=1240&h=892&f=jpeg&s=125375)
最后用 IDEA 开发工具新创建一个 Gradle 工程来验证我们的插件。看到右侧gradle任务中多了我们添加的hello任务，点击查看成功输出了Hello world。
![-w1086](https://user-gold-cdn.xitu.io/2019/6/11/16b456b24f7e633f?w=1240&h=1061&f=jpeg&s=99674)

## 补充及常见问题
#### 文件树是有层级结构的文件集合，一个文件树它可以代表一个目录结构或一 ZIP 压缩包中的内容结构。使用`Project.fileTree(java.util.Map)`创建，可以使用过虑条件来包含或排除相关文件。

```
// 指定目录创建文件树对象
FileTree tree = fileTree(dir: 'src/main')
// 给文件树对象添加包含指定文件
tree.include '**/*.java'
//andoid中使用文件树
implementation fileTree(include: ['*.jar'], dir: 'libs')
```
#### 多module的编译配置
注意 `settings.gradle`引入的module才会参与编译`include ':app', ':plugin_common', ':plugin_gallery'`，可以在跟g`uild.gradle` 中统一设置公共行为；比如下图添加一个hello任务。
![-w1615](https://user-gold-cdn.xitu.io/2019/6/11/16b456b24f8b4ecf?w=1240&h=461&f=jpeg&s=97759)

一个工程的路径为：以冒号（: 它代表了根工程）开始，再加上工程的名称。例如“:common”。
一个任务的路径为：工程路径加上任务名称，例如“:common:hello”.
比如:仅仅执行 `gradle :plugin_common:hello`


