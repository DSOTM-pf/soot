import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.options.Options;


public class AndroidInstrument {

	public static void main(String[] args) {

		//prefer Android APK files// -src-prec apk
    /**设置处理的文件类型，Options.src_prec_apk表示处理的是Android apk*/
		Options.v().set_src_prec(Options.src_prec_apk);

		//output as APK, too//-f J
    ////设置输出的文件类型:output_format_dex表示输出的文件的apk/dex，将会输出在工程目录下的sootOutput目录
		Options.v().set_output_format(Options.output_format_dex);

    //设置Soot的soot-class路径
    Options.v().set_soot_classpath("G:\\Soot\\sdk\\platforms\\android-17\\android.jar;C:\\Program Files\\Java\\jre7\\lib\\rt.jar;C:\\Program Files\\Java\\jre7\\lib\\jce.jar;G:\\李政桥\\工具\\android相关工具\\soot-trunk.jar");


        // resolve the PrintStream and System soot-classes

    	//加载java.io.PrintStream和java.lang.System
    	//java.lang.System中存在字段static PrintStream out
    	//java.io.PrintStream中存在方法void println(java.lang.String)
    	//插入的代码是：System.out.println("HELLO");
		Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);

        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

			@Override
			protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
        //Body带代表一个method body；
        //Body中存在三个基本元素：locals、statements、exceptions，即：getLocals();getUnits();getTraps()
      	final PatchingChain<Unit> units = b.getUnits();

				//important to use snapshotIterator here
				for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = iter.next();
					u.apply(new AbstractStmtSwitch() {
            //如果u是一个调用语句的时候，类似：staticinvoke <com.example.source_a.MainActivity: int add4(int,int)>($i0, $i1);
						public void caseInvokeStmt(InvokeStmt stmt) {
							InvokeExpr invokeExpr = stmt.getInvokeExpr();
              //该调用语句中，如果被调用的方法是onDraw时
							if(invokeExpr.getMethod().getName().equals("onDraw")) {

								Local tmpRef = addTmpRef(b);
								Local tmpString = addTmpString(b);
                //插入：tmpRef = <java.lang.System: java.io.PrintStream out>;类似于类的实例化
							 //newAssignStmt()为赋值语句
								  // insert "tmpRef = java.lang.System.out;"
						        units.insertBefore(Jimple.v().newAssignStmt(
						                      tmpRef, Jimple.v().newStaticFieldRef(
						                      Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

						        // insert "tmpLong = 'HELLO';"
                    //插入：tmpLong = "HELLO"
						        units.insertBefore(Jimple.v().newAssignStmt(tmpString,
						                      StringConstant.v("HELLO")), u);

						        // insert "tmpRef.println(tmpString);"
						        SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
                    //newVirtualInvokeExpr生成调用语句 第1个参数为类的实例化，第2个参数为类中的方法的引用，第3个为方法的参数
                    units.insertBefore(Jimple.v().newInvokeStmt(
						                      Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), tmpString)), u);

						        //check that we did not mess up the Jimple
						        b.validate();
							}
						}
            //同样可以处理其他语句,如下面的赋值语句
					public void caseAssignStmt(AssignStmt stmt){}
					//这些都是class AbstraceStmtSwitch中可以查看
					});
				}
			}


		}));

		soot.Main.main(args);
	}

    private static Local addTmpRef(Body body)
    {
      //生成Local tmpRef,生成的Jimple代码为：java.io.PrintStrem tmpRef;
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        //加到Locals链中
        body.getLocals().add(tmpRef);
        return tmpRef;
    }

    private static Local addTmpString(Body body)
    {
      //生成Local tmpString，生成的Jimple代码为：java.lang.String tmpString;
        Local tmpString = Jimple.v().newLocal("tmpString", RefType.v("java.lang.String"));
        body.getLocals().add(tmpString);
        return tmpString;
    }
}
