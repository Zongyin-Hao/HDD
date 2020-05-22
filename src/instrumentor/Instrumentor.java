package instrumentor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import soot.util.Chain;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Body;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.Value;
import soot.ValueBox;
import soot.jimple.FieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;
import soot.tagkit.LineNumberTag;
import soot.jimple.StringConstant;

public class Instrumentor {
	
	private SootClass mainClass;
	private SootMethod mainMethod;
	private List<SootClass> testClasses;
	private SootClass callbackClass;
	private SootMethod startBefore;
	private SootMethod lockBefore;
	private SootMethod unlockBefore;
	private SootMethod writeBefore;
	private SootMethod readBefore;
	
	private Map<String, SootClass> s_to_c; //由hash码获取SootClass
	private Map<String, SootField> s_to_f; //由hash码获取SootField
	private Map<String, SootMethod> s_to_m; //由hash码获取SootMethod
	private Map<JimpleLocal, SootClass> v_to_c; //通过栈空间变量关联局部变量与类
	private Map<JimpleLocal, SootField> v_to_f; //通过栈空间变量关联局部变量与成员变量
	private Map<Stmt, Integer> stmt_to_iid; //获取stmt在JimpleBody中的行数
	
	public Instrumentor(SootClass mainClass, SootClass callbackClass, List<SootClass> testClasses) {
		this.mainClass = mainClass;
		this.mainMethod = this.mainClass.getMethodByName("main");
		this.callbackClass = callbackClass;
		this.testClasses = testClasses;
		startBefore = this.callbackClass.getMethodByName("startBefore");
		lockBefore = this.callbackClass.getMethodByName("lockBefore");
		unlockBefore = this.callbackClass.getMethodByName("unlockBefore");
		writeBefore = this.callbackClass.getMethodByName("writeBefore");
		readBefore = this.callbackClass.getMethodByName("readBefore");
		
		s_to_c = new HashMap<> ();
		s_to_f = new HashMap<> ();
		s_to_m = new HashMap<> ();
		v_to_c = new HashMap<> ();
		v_to_f = new HashMap<> ();
		stmt_to_iid = new HashMap<> ();
	}
	
	private String hashCodeC(SootClass myClass) {
		return myClass.getShortName(); //以所有类都在同一包下为前提
	}
	
	private String hashCodeF(SootField myField) {
		String s = hashCodeC(myField.getDeclaringClass())+":";
		s += myField.getName();
		return s;
	}
	
	private String hashCodeM(SootMethod myMethod) {		
		String s = hashCodeC(myMethod.getDeclaringClass())+":";
		//考虑函数重载
		s += myMethod.getReturnType()+" ";
		s += myMethod.getName()+" ";
		s += myMethod.getParameterTypes().toString();
		return s;
	}
	
	private String hashCodeStmt(SootMethod myMethod, Stmt stmt) {
		if (!stmt_to_iid.containsKey(stmt)) {
			System.err.println("[HDD-ActiveScheduling] Can not get the iid of <" + stmt + ">!");
			System.exit(1);
		}
		return hashCodeM(myMethod) + ":" + stmt_to_iid.get(stmt);
	}
	
	//判断是否为栈空间变量，在指向分析中起连接作用
	private boolean isStackVariable(Value v) {
		if (!(v instanceof JimpleLocal)) return false;
		JimpleLocal myLocal = (JimpleLocal)v;
		String name = myLocal.getName();
		if (name.length() > 0 && name.charAt(0) == '$') return true;
		return false;
	}
	
	//判断是否为共享变量，这里将public static视作共享变量
	//这个函数主要用来判断锁，读写共享变量另做判断
	private boolean isSharedVariable(Value v) {
		if (!(v instanceof JimpleLocal)) return false;
		JimpleLocal myLocal = (JimpleLocal)v;
		if (!v_to_f.containsKey(myLocal)) return false;
		SootField sf = v_to_f.get(myLocal);
		if (sf.isPublic() && sf.isStatic()) return true;
		return false;
	}
	
	private int getLineNumber(Stmt s) {
		Iterator ti = s.getTags().iterator();
		while (ti.hasNext()) {
			Object o = ti.next();
			if (o instanceof LineNumberTag) {
				return Integer.parseInt(o.toString());
			}
		}
		return 0;
	}
	
	private void initialize() {
		//1.s_to_c,s_to_f,s_to_m
		for (SootClass curClass : testClasses) {
			s_to_c.put(hashCodeC(curClass), curClass);
			
			Iterator fieldIterator = curClass.getFields().iterator();
			while (fieldIterator.hasNext()) {
				SootField curField = (SootField)fieldIterator.next();
				s_to_f.put(hashCodeF(curField), curField);
			}
			
			Iterator methodIterator = curClass.getMethods().iterator();
			while (methodIterator.hasNext()) {
				SootMethod curMethod = (SootMethod)methodIterator.next();
				s_to_m.put(hashCodeM(curMethod), curMethod);
			}
		}
		//2.v_to_c,v_to_f
		for (SootClass curClass : testClasses) {
//			System.out.println("==================================================");
//			System.out.println("[HDD-Static Analysis] initialize class " + curClass);
			Iterator methodIterator = curClass.getMethods().iterator();
			while (methodIterator.hasNext()) {
				SootMethod curMethod = (SootMethod)methodIterator.next();
//				System.out.println("=========================");
//				System.out.println("[HDD-Static Analysis] initialize method " + curMethod);
				JimpleBody jb = (JimpleBody)curMethod.getActiveBody();
				Iterator ui = jb.getUnits().iterator();
				while (ui.hasNext()) {
					Stmt stmt = (Stmt)ui.next();
//					System.out.println(stmt);
					if (stmt instanceof JAssignStmt) {
						JAssignStmt jstmt = (JAssignStmt)stmt;
						Value left = jstmt.getLeftOp();
						Value right = jstmt.getRightOp();
					
						if (isStackVariable(left) && right instanceof FieldRef) {
							SootField field = ((FieldRef)right).getField();
							if (s_to_f.containsKey(hashCodeF(field))) {				
								v_to_f.put((JimpleLocal)left, s_to_f.get(hashCodeF(field)));
							}
						}
						else if (isStackVariable(left) && right instanceof JNewExpr) {
							JNewExpr newExpr = (JNewExpr)right;
							if (s_to_c.containsKey(newExpr.getType().toString())) {
								v_to_c.put((JimpleLocal)left, s_to_c.get(newExpr.getType().toString()));
							}
						}
						else if (left instanceof JimpleLocal && !isStackVariable(left) && isStackVariable(right)) {
							if (v_to_c.containsKey(right)) {
								v_to_c.put((JimpleLocal)left, v_to_c.get(right));
							}
							if (v_to_f.containsKey(right)) {
								v_to_f.put((JimpleLocal)left, v_to_f.get(right));
							}
						}
					}
				}
			}
		}
		//3.stmt_to_iid
		for (SootClass curClass : testClasses) {
			Iterator methodIterator = curClass.getMethods().iterator();
			while (methodIterator.hasNext()) {
				SootMethod curMethod = (SootMethod)methodIterator.next();
				JimpleBody jb = (JimpleBody)curMethod.getActiveBody();
				Iterator ui = jb.getUnits().iterator();
				int id = 1;
				while (ui.hasNext()) {
					Stmt stmt = (Stmt)ui.next();
					stmt_to_iid.put(stmt, id);
					id++;
				}
			}
		}
	}
	
	private void visitInvokeStmt(SootMethod curMethod, Chain units, InvokeStmt stmt) {
		//thread start
		JInvokeStmt jstmt = (JInvokeStmt)stmt;
		if (!jstmt.containsInvokeExpr()) return;
		SootMethod nextMethod = jstmt.getInvokeExpr().getMethod();
		if (nextMethod.getSignature().equals("<java.lang.Thread: void start()>")) {
			//线程创建
			List<ValueBox> useBoxes = jstmt.getUseBoxes();
			if (useBoxes.size() > 0 && useBoxes.get(0) instanceof JimpleLocalBox) {
				JimpleLocal jLocal = (JimpleLocal)useBoxes.get(0).getValue(); //获取调用者
				if (v_to_c.containsKey(jLocal)) {
					SootClass sc = v_to_c.get(jLocal);
					//确认调用者所属的类是线程类
					if (!sc.getSuperclass().getName().equals("java.lang.Thread")) return;
					InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(startBefore.makeRef(), StringConstant.v(hashCodeC(curMethod.getDeclaringClass())+":"+getLineNumber(stmt)),
							StringConstant.v(hashCodeStmt(curMethod, stmt)), jLocal);
					Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
					units.insertBefore(incStmt, stmt);
				}
			}
		}
	}
	
	private void visitEnterMonitorStmt(SootMethod curMethod, Chain units, EnterMonitorStmt stmt) {
		//lock
		JEnterMonitorStmt jstmt = (JEnterMonitorStmt)stmt;
		Value v = jstmt.getOp();
		if (isSharedVariable(v)) {
			InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(lockBefore.makeRef(), StringConstant.v(hashCodeC(curMethod.getDeclaringClass())+":"+getLineNumber(stmt)),
					StringConstant.v(hashCodeStmt(curMethod, stmt)), StringConstant.v(hashCodeF(v_to_f.get((JimpleLocal)v))));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			units.insertBefore(incStmt, stmt);
		}
	}
	
	private void visitExitMonitorStmt(SootMethod curMethod, Chain units, ExitMonitorStmt stmt) {
		//unlock
		JExitMonitorStmt jstmt = (JExitMonitorStmt)stmt;
		Value v = jstmt.getOp();
		if (isSharedVariable(v)) {
			InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(unlockBefore.makeRef(), StringConstant.v(hashCodeC(curMethod.getDeclaringClass())+":"+getLineNumber(stmt)),
					StringConstant.v(hashCodeStmt(curMethod, stmt)), StringConstant.v(hashCodeF(v_to_f.get((JimpleLocal)v))));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			units.insertBefore(incStmt, stmt);
		}
	}
	
	private void visitAssignStmt(SootMethod curMethod, Chain units, AssignStmt stmt) {
		//r/w shared variable
		JAssignStmt jstmt = (JAssignStmt)stmt;
		Value left = jstmt.getLeftOp();
		Value right = jstmt.getRightOp();
		SootField fLeft = null;
		SootField fRight = null;
		boolean write = false;
		boolean read = false;
		if (left instanceof FieldRef) {
			// write
			SootField field = ((FieldRef)left).getField();
			if (s_to_f.containsKey(hashCodeF(field))) {				
				field = s_to_f.get(hashCodeF(field));
				if (field.isPublic() && field.isStatic()) {
					fLeft = field;
					write = true;
				}
			}
		}
		if (right instanceof FieldRef) {
			//read
			SootField field = ((FieldRef)right).getField();
			if (s_to_f.containsKey(hashCodeF(field))) {				
				field = s_to_f.get(hashCodeF(field));
				if (field.isPublic() && field.isStatic()) {
					fRight = field;
					read = true;
				}
			}
		}
		if (write) {
			InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(writeBefore.makeRef(), StringConstant.v(hashCodeC(curMethod.getDeclaringClass())+":"+getLineNumber(stmt)),
					StringConstant.v(hashCodeStmt(curMethod, stmt)), StringConstant.v(hashCodeF(fLeft)));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			units.insertBefore(incStmt, stmt);
		}
		else if (read) {
			InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(readBefore.makeRef(), StringConstant.v(hashCodeC(curMethod.getDeclaringClass())+":"+getLineNumber(stmt)),
					StringConstant.v(hashCodeStmt(curMethod, stmt)), StringConstant.v(hashCodeF(fRight)));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			units.insertBefore(incStmt, stmt);
		}
	}
	
	private void insertCallBackFunctions() {
		for (SootClass curClass : testClasses) {
			Iterator<SootMethod> methodIterator = curClass.getMethods().iterator();
			while (methodIterator.hasNext()) {
				SootMethod curMethod = methodIterator.next();
//				if (curMethod.getName().equals("<clinit>")) continue; //不分析clinit
				Body body = curMethod.getActiveBody();
				Chain units = body.getUnits();
				Iterator stmtIt = units.snapshotIterator();//注意这里需要在副本上进行遍历
				while (stmtIt.hasNext()) {
					Stmt stmt = (Stmt)stmtIt.next();
					if (stmt instanceof InvokeStmt) {
						visitInvokeStmt(curMethod, units, (InvokeStmt)stmt);
					}
					else if (stmt instanceof EnterMonitorStmt) {
						visitEnterMonitorStmt(curMethod, units, (EnterMonitorStmt)stmt);
					}
					else if (stmt instanceof ExitMonitorStmt) {
						visitExitMonitorStmt(curMethod, units, (ExitMonitorStmt)stmt);
					}
					else if (stmt instanceof AssignStmt) {
						visitAssignStmt(curMethod, units, (AssignStmt)stmt);
					}
				}
			}
		}
	}
	
	public void start() {
		System.out.println("[HDD-Instrumentor] Initialize");
		initialize();
		System.out.println("[HDD-Instrumentor] Insert call back functions");
		insertCallBackFunctions();
	}
}
