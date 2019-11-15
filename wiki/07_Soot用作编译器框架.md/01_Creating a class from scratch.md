首先，需要创建一个类，在类中创建方法。下列的步骤对于创建一个类文件来说是必须的。
## 加载```java.lang.Object```和库类
*Load``` java.lang.Object```, the root of the Java class hierarchy.*

在扩展Soot框架的时候，此步骤不是必要的。在这种情况下，当用户代码被调用的时候加载类文件已经完成了。
```
Scene.v().loadClassAndSupport("java.lang.Object");
```
这行代码使Soot加载java.lang.Object类并创建相应的```SootClass```对象以及其字段的```SootMethods```和```SootFields```。 当然，```java.lang.Object```引用了其他对象。 对```loadClassAndSupport```的调用将加载指定类的可传递闭包，以便加载```java.lang.Object```所需的所有类型都将被加载。

此过程称为**解析**（resolution）。

由于我们的HelloWorld程序将使用标准库中的类，因此我们还必须添加下面这一行代码：
```
Scene.v().loadClassAndSupport("java.lang.System");
```
上面代码引用```Scene.v（）```。 ```Scene```是程序中所有```SootClasses```的容器，并提供各种实用的方法。 对于一个单例场景对象(singleton Scene object)，可通过调用```Scene.v（）```进行访问。

Implementation note：Soot从类文件或```.jimple```输入文件中加载这些类。 使用前者时，Soot将加载每个类文件的常量池中引用的所有类名称。 从```.jimple```加载将使Soot仅加载所需的类型。
## 创建一个新的```SootClass```对象
*创建`HelloWorld`SootClass,并且设置它的 super class 为"java.lang.Object"*
```
sClass = new SootClass("HelloWorld", Modifier.PUBLIC);
```
上面代码创建了一个名为```HelloWorld```的public的```SootClass```对象

```
sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
```
这会将新创建的类的superclass设置为java.lang.Object的```SootClass```对象。 注意在```Scene```上使用方法```getSootClass```。

```
Scene.v().addClass(sClass);
```
将新创建的```HelloWorld```类添加到```Scene```中，所有的类一旦他们被创建都应该属于```Scene```
## 向```SootClass```中添加方法
给 HelloWorld 创建一个 ```main()```方法。
现在有```SootClass```,下一步把方法添加进去。


```
method = new SootMethod("main",                 
    Arrays.asList(new Type[] {ArrayType.v(RefType.v("java.lang.String"), 1)}),
    VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
```
创建一个新的public static 方法main，声明它接受java.lang.String对象的数组，并返回void。

SootMethod的构造函数需要一个list，因此我们调用Java方法```Arrays.asList```从单元素数组创建一个list，该数组由我们使用new Type [] ...动态生成。 在列表中，我们放置了一个数组类型，它对应于```java.lang.String```对象的一维ArrayType。 对```RefType```的调用将获取与```java.lang.String```类相对应的类型。

**Type** 每个```SootClass```代表一个Java对象。 我们可以实例化该类，为对象指定类型。
类型和类这两个概念紧密相关，但又截然不同。
要通过名称获取```java.lang.String```类的类型，我们调用```RefType.v（“ java.lang.String”）```。
给定一个SootClass对象```sc```，我们也可以调用```sc.getType（）```来获取相应的类型。
```
sClass.addMethod(method);
```
将方法添加到类中
## 向方法中添加代码
