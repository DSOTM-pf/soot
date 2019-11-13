在Soot中，增加了对使用Soot读写Dalvik字节码的支持。 该支持包括两个主要模块。 一个叫做[Dexpler](http://dl.acm.org/citation.cfm?doid=2259051.2259056)http://dl.acm.org/citation.cfm?doid=2259051.2259056，主要由[Alexandre Bartel](http://www.abartel.net/)周围的团队开发，并由Ben Bellamy，Eric Bodden以及Frank Hartmann和Michael Markert进行了一些增强。 Dexpler将Dalvik字节码转换为Jimple的三地址代码。

## 如何使用
首先获取最新版本的Soot，例如[每夜版](https://soot-build.cs.uni-paderborn.de/public/origin/develop/soot/soot-develop/build/)。 还可以在[https://github.com/Sable/android-platforms](https://github.com/Sable/android-platforms)上检出目录。 此目录包含Soot用来解析、分析或检测的应用程序类型所需的Android标准库的不同版本。
接下来，我们使用主要方法实现驱动程序类，并在其中粘贴以下代码：
```
//prefer Android APK files// -src-prec apk
Options.v().set_src_prec(Options.src_prec_apk);

//output as APK, too//-f J
Options.v().set_output_format(Options.output_format_dex);

// resolve the PrintStream and System soot-classes
Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
```
第一个操作指示Soot加载Android APK文件。
第二个操作指示Soot生成Dex / APK文件作为输出。 （从理论上讲，您也可以将Java转换为Dex或将Dex转换为Java，依此类推。）
最后两个操作告诉Soot加载我们的检测将需要的两个类，但被检测的APK可能不需要这些类。

接下来，我们向Soot添加一个Transform：
```
PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

	@Override
	protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
		final PatchingChain units = b.getUnits();		
		//important to use snapshotIterator here
		for(Iterator iter = units.snapshotIterator(); iter.hasNext();) {
			final Unit u = iter.next();
			u.apply(new AbstractStmtSwitch() {

				public void caseInvokeStmt(InvokeStmt stmt) {
					//code here
				}

			});
		}
	}
}));
```
这将遍历APK中所有Bodies的所有Units，并且在每个InvokeStmt上都将调用标记为“code here”的代码。

插入以下内容：
```
InvokeExpr invokeExpr = stmt.getInvokeExpr();
if(invokeExpr.getMethod().getName().equals("onDraw")) {

	Local tmpRef = addTmpRef(b);
	Local tmpString = addTmpString(b);

	  // insert "tmpRef = java.lang.System.out;"
    units.insertBefore(Jimple.v().newAssignStmt(
              tmpRef, Jimple.v().newStaticFieldRef(
              Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

    // insert "tmpLong = 'HELLO';"
    units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                  StringConstant.v("HELLO")), u);

    // insert "tmpRef.println(tmpString);"
    SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void     println(java.lang.String)");                    
    units.insertBefore(Jimple.v().newInvokeStmt(
                  Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), tmpString)), u);

    //check that we did not mess up the Jimple
    b.validate();
}
```
这导致Soot在方法调用之前但仅在此调用的目标是onDraw方法的情况下才插入```System.out.println（“ HELLO”）```。

最后最重要的一点是，不要忘记实际调用Soot的主要方法：
```
soot.Main.main(args)
```
现在需要做的就是使用以下参数运行驱动程序类：
```
-android-jars path/to/android-platforms -process-dir your.apk
```
这里的```path/to/android-platforms```是您先前下载的平台JAR文件的路径，而```your.apk```是您要进行检测的APK的路径。 选项```-process-dir```指示Soot处理此APK中的所有类。 输出结果，将在目录```./sootOutput```中找到一个具有相同名称的新APK。

代码见[AndroidInstrument.java]()
