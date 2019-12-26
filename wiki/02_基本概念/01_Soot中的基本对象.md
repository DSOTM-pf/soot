以下主要介绍一些重要的类，方便后续开发：
Body, Unit, Local, Value, UnitBox和 ValueBox。

## Body
Soot使用Body来存储一个方法的代码。Soot中共有四种不同类型的Body。
  - 即：**BafBody**,**JimpleBody**,**ShimpleBody**,**GrimpBody**
Body中的三条主要的链是:Units链、Locals链和Traps链。

考虑下面的Java方法
```
public static void main(String[] argv) throws Exception
{
    int x = 2, y = 6;

    System.out.println("Hi!");
    System.out.println(x * y + y);
    try
    {
        int z = y * x;
    }
    catch (Exception e)
    {
        throw e;
    }
}
```
将它转换为Jimple代码之后，我们有下面简短的Jimple代码：
```
public static void main(java.lang.String[]) throws java.lang.Exception
{
    java.lang.String[] r0;
    int i0, i1, i2, $i3, $i4;
    java.io.PrintStream $r1, $r2;
    java.lang.Exception $r3, r4;

    r0 := @parameter0;
    i0 = 2;
    i1 = 6;
    $r1 = java.lang.System.out;
    $r1.println(``Hi!'');
    $r2 = java.lang.System.out;
    $i3 = i0 * i1;
    $i4 = $i3 + i1;
    $r2.println($i4);

 label0:
    i2 = i1 * i0;

 label1:
    goto label3;

 label2:
    $r3 := @caughtexception;
    r4 = $r3;
    throw r4;

 label3:
    return;

    catch java.lang.Exception from label0 to label1 with label2;
}
```
### Local 变量
上面方法里的locals在该方法的最开始处
  ```
  java.lang.String[] r0;
  int i0, i1, i2, $i3, $i4;
  java.io.PrintStream $r1, $r2;
  java.lang.Exception $r3, r4;
  ```
Local(s)被收集在localChain中，可以通过body.getLocals()访问。每一种中间表示都有它自己对Local的实现。然后这些实现都得保证对于域每一个定义Local r0,能够调用r0.getName(),r0.getType(),r0.setName()和r0.setType()。
注意这些local变量必须是有类型的。

### Traps
为了支持Java的异常处理，Soot的Body定义了traps这一概念。其观点是在java字节码中，异常处理程序是由一个多元组(异常、开始点、结束点、处理程序)表示；在开始和结束单元之间（包括开始处，但不包括结束处），如果异常被抛出，则执行处理程序。
上述例子中的 trap:
```
  catch java.lang.Exception from label0 to label1 with label2;
```
### Units
Unit(s)链是Body中的实际代码。
Jimple用Stmt实现了Unit。Grimp则用Instagram来实现的。这表明每个中间表示(IR)有它自己的对语句(Statement)的概念（我理解的是每一个中间表示有自己的形式去表示Unit）。
例子：Jimple Stmt中的一个例子便是AssignStmt，这代表一条Jimple的分配语句。AssignStmt可能是下面这种形式：
```
x = y + z;
```
## Value
代码通常会对数据进行操作，为了表示数据，soot提供了Value接口。下面是一些不同类型的Value(s):
- ```Local```(本地变量)
- ```Constant```s（常量）
- Expressions (Expr)（表达式？）
- ParameterRefs, CaughtExceptionRefs and ThisRefs.

Expr接口，反过来，有一整套实现；其中有NewExpr和AddExpr。通常，一个Expr会对一个或多个Value(s)执行一些动作并返回另一个Value。
下面是使用Value(s)的一个例子：
```
x = y + 2;
(我理解的：
  x:Local
  2:Constant
  y + 2:Expressions)
```
这是一个AssignStmt，左操作数是x，右操作数是y+2，右操作数是一个AddExpr。
这个AddExpr反过来包含Value(s) y和2作为它的操作数；
前一个操作数是一个Local，后一个操作数是一个Constant。

在Jimple中，我们强制要求所有的Value(s)最多包含一个表达式(contain at most 1 expression)。Grimp则没有这个限制，这使得其产生的代码很容易阅读但很难分析。
## Boxes
Box是一个指针，提供对Soot对象的间接访问。
描述指针的话，用Ref可能更合适，但是Ref在Soot里面已经有其他的意思了。

在Soot中有两类Box(es)，分别为ValueBox和UnitBox。一个UnitBox包含Unit(s)，一个ValueBox包含Value(s)，在C++中，分别表示为（Unit*）和（Value*）。
### UnitBox
某些Unit(s)可能需要包含对其它Unit(s)的引用。
比如，一个GotoStmt需要知道它的目标是什么。因此，Soot提供了UnitBox，UnitBox是一个包含了一个Unit的Box。
例如下面的Jimple代码：
```
x = 5;
```
```
goto l2;
```
```
y = 3 ;
```
```
l2: z = 9;
```
每个Unit都必须提供getUnitBoxes()方法，对于大多数UnitBox(es)而言，这个方法会返回一个空列表，对于GotoStmt而言，getUnitBoxes()方法将会返回包含一个元素的列表。包含一个指向l2的Box。

注意到对于SwitchStmt而言，通常getUnitBoxes()方法将会返回包含许多boxes的列表。
Box的概念对于修改代码而言最为有用。比如我们有下面一条语句s：
```
s: goto l2;
```
在l2处有一个Stmt
```
l2: goto l3;
```
很明显，不管s的实际类型是什么，s可以指向l3而不指向l2。因此对于各种类型的Unit(s)，我们可以统一做下面的事情：
```
public void readjustJumps(Unit s, Unit oldU, Unit newU)
{
    Iterator ubIt = s.getUnitBoxes.iterator();
    while (ubIt.hasNext())
    {
        StmtBox tb = (StmtBox)ubIt.next();
        Stmt targ = (Stmt)tb.getUnit();

        if (targ == oldU)
            tb.setUnit(newU);
    }
}
```
Unit自身使用了一些与上面类似的代码，这样便能够创建PatchingChain，PatchingChain是对Chain的一种实现，它能够调整那些指向将要从Chain中删除的Unit(s)的指针。
### ValueBox
Value的指针。
对于一个Unit，可以获得ValueBox的列表，包括在这个Unit中已使用和被定义的值(values)

我们可以利用这些boxes来进行常数合并(constant folding)：对于一个AssignStmt，其计算一个AddExpr，将两个常数值相加，我们则可以静态的将它们相加然后把结果放置到UseBox。
下面是一个合并 AddExpr 的例子：
```

public void foldAdds(Unit u)
{
    Iterator ubIt = u.getUseBoxes().iterator();
    while (ubIt.hasNext())
    {
        ValueBox vb = (ValueBox) ubIt.next();
        Value v = vb.getValue();
        if (v instanceof AddExpr)
        {
            AddExpr ae = (AddExpr) v;
            Value lo = ae.getOp1(), ro = ae.getOp2();
            if (lo instanceof IntConstant && ro instanceof IntConstant)
            {
                IntConstant l = (IntConstant) lo,
                      r = (IntConstant) ro;
                int sum = l.value + r.value;
                vb.setValue(IntConstant.v(sum));
            }
        }
    }

```
注意到不管Unit 的类型是什么，这都适用。
## Unit revisited
下面讨论任何Unit都必须提供的几种不同的方法
```
public List getUseBoxes();
public List getDefBoxed();
public List getUseAndDefBoxes();
```
上面三个方法返回在该Unit被使用、被定义或二者兼有的ValueBox(s)的一个List(s)。

对于getUseBoxes(),返回所有被使用的值(values)，包括表达式以及这些表达式的组成部分。
```
public List getUnitBoxes(); //此方法返回由该方法指向的unit的UnitBox(es)的一个List
它指向别人
```
```
public List getBoxesPointingToThis(); //这个方法返回UnitBox(es)的列表，这些UnitBox都指向此Unit(即调用这个方法的Unit)。
别人指向它
```
```
public boolean fallsThrough();
public boolean branches();
这两个方法处理该Unit之后的执行流，前面一个方法如果可以接着执行接下来的Unit，就会返回真。
后面一个方法如果执行流可能流入其他的Unit，就会返回真。这里的流入其他Unit指的不是紧跟当前Unit之后的Unit
```
```
这个方法使用getBoxesPointingToThis()来改变所有跳转至该Unit的跳转语句，将它们指向newLocation。

```
