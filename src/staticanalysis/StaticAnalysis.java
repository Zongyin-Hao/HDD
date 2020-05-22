package staticanalysis;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;
import soot.tagkit.LineNumberTag;
import soot.jimple.FieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.Stmt;
import soot.jimple.IfStmt;
import soot.jimple.GotoStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.AssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JAssignStmt;

import staticanalysis.petrinet.*; 
import staticanalysis.occurrentnet.*;
import staticanalysis.unfolding.*;

public class StaticAnalysis {
	private SootClass mainClass;
	private SootMethod mainMethod;
	private List<SootClass> testClasses; //待检测的类
	//初始化相关：
	private Map<String, SootClass> s_to_c; //由hash码获取SootClass
	private Map<String, SootField> s_to_f; //由hash码获取SootField
	private Map<String, SootMethod> s_to_m; //由hash码获取SootMethod
	private Map<JimpleLocal, SootClass> v_to_c; //通过栈空间变量关联局部变量与类
	private Map<JimpleLocal, SootField> v_to_f; //通过栈空间变量关联局部变量与成员变量
	private Map<Stmt, Integer> stmt_to_iid; //获取stmt在JimpleBody中的行数
	//运行相关：
	private List<Place> P;
	private List<Transition> T;
	private Place Ps;
	//以thread前缀+SootMethod的hash码唯一标识搜索过程中的函数
	//以thread前缀+SootMethod的hash码+iid唯一标识搜索过程中的语句
	private Set<String> used; //记录搜索过的函数
	private Map<String, Place> m_to_ps; //记录函数头
	private Map<String, Place> m_to_pt; //记录函数尾
	private Map<String, Place> stmt_to_p; //记录敏感语句对应的库所，高优先级
	private Map<String, Place> target_to_p; //辅助stmt_to_p,特殊处理跳转语句，低优先级
	
	private List<Condition> C;
	private List<Event> E;
	private Event Es;
	private static final int maxDataRaceNumber = 3; //最大检测数，避免动态检测的压力过大
	private List<List<String> > dataRacePath; //潜在数据竞争的线程轨迹
	
	public StaticAnalysis(SootClass mainClass, List<SootClass> testClasses) {
		this.mainClass = mainClass;
		this.testClasses = testClasses;
		mainMethod = this.mainClass.getMethodByName("main");
		
		s_to_c = new HashMap<> ();
		s_to_f = new HashMap<> ();
		s_to_m = new HashMap<> ();
		v_to_c = new HashMap<> ();
		v_to_f = new HashMap<> ();
		stmt_to_iid = new HashMap<> ();
		
		P = new ArrayList<> ();
		T = new ArrayList<> ();
		used = new HashSet<> ();
		m_to_ps = new HashMap<> ();
		m_to_pt = new HashMap<> ();
		stmt_to_p = new HashMap<> ();
		target_to_p = new HashMap<> ();
		
		dataRacePath = new ArrayList<List<String> > ();
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
	
	private String hashCodeTM(String thread, SootMethod myMethod) {
		return thread + ":" + hashCodeM(myMethod);
	}
	
	private String hashCodeTStmt(String thread, SootMethod myMethod, Stmt stmt) {
		if (!stmt_to_iid.containsKey(stmt)) {
			System.err.println("[HDD-StaticAnalysis] Can not get the iid of <" + stmt + ">!");
			System.exit(1);
		}
		return hashCodeTM(thread, myMethod) + ":" + stmt_to_iid.get(stmt);
	}
	
	private Place newPlace() {
		Place p = new Place(P.size());
		P.add(p);
		return p;
	}
	
	private Transition newTransition(String signature, String message) {
		Transition t = new Transition(T.size(), signature, message);
		T.add(t);
		return t;
	}
	
	private Transition newTransitionStart(String signature, String message, String thread, String threadType) {
		Transition t = new TransitionStart(T.size(), signature, message, thread, threadType);
		T.add(t);
		return t;
	}
	
	private Transition newTransitionSyn(String signature, String message, String lock, boolean isAcquire) {
		Transition t = new TransitionSyn(T.size(), signature, message, lock, isAcquire);
		T.add(t);
		return t;
	}
	
	private Transition newTransitionRW(String signature, String message, String variable, boolean isWrite) {
		Transition t = new TransitionRW(T.size(), signature, message, variable, isWrite);
		T.add(t);
		return t;
	}
	
	private void addEdge(Place p, Transition t) {
		p.addPostSet(t);
		t.addPreSet(p);
	}
	
	private void addEdge(Transition t, Place p) {
		t.addPostSet(p);
		p.addPreSet(t);
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
	
	private String white(int n) {
		String s = "";
		for (int i = 1; i <= n; i++) {
			s += "--";
		}
		return s;
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
	
	private void visitIfStmt_Init(String thread, SootMethod curMethod, IfStmt stmt) {
		stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
	}
	
	private void visitGotoStmt_Init(String thread, SootMethod curMethod, GotoStmt stmt) {
		stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
	}
	
	private void visitEnterMonitorStmt_Init(String thread, SootMethod curMethod, EnterMonitorStmt stmt) {
		JEnterMonitorStmt jstmt = (JEnterMonitorStmt)stmt;
		Value v = jstmt.getOp();
		if (isSharedVariable(v)) {			
			stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
		}
	}
	
	private void visitExitMonitorStmt_Init(String thread, SootMethod curMethod, ExitMonitorStmt stmt) {
		JExitMonitorStmt jstmt = (JExitMonitorStmt)stmt;
		Value v = jstmt.getOp();
		if (isSharedVariable(v)) {			
			stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
		}
	}
	
	private void visitReturnStmt_Init(String thread, SootMethod curMethod, ReturnStmt stmt) {
		stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
	}
	
	private void visitReturnVoidStmt_Init(String thread, SootMethod curMethod, ReturnVoidStmt stmt) {
		stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
	}
	
	private void visitInvokeStmt_Init(String thread, SootMethod curMethod, InvokeStmt stmt) {
		JInvokeStmt jstmt = (JInvokeStmt)stmt;
		if (!jstmt.containsInvokeExpr()) return;
		SootMethod nextMethod = jstmt.getInvokeExpr().getMethod();
		if (nextMethod.getSignature().equals("<java.lang.Thread: void start()>")) {
			//线程创建
			List<ValueBox> useBoxes = jstmt.getUseBoxes();
			if (useBoxes.size() > 0 && useBoxes.get(0) instanceof JimpleLocalBox) {
				JimpleLocal jLocal = (JimpleLocal)useBoxes.get(0).getValue(); // 获取调用者
				if (v_to_c.containsKey(jLocal)) {
					SootClass sc = v_to_c.get(jLocal);
					//确认调用者所属的类是线程类
					if (!sc.getSuperclass().getName().equals("java.lang.Thread")) return;
					stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
				}
			}
		}
		else {
			//一般调用
			if (s_to_m.containsKey(hashCodeM(nextMethod))) {
				nextMethod = s_to_m.get(hashCodeM(nextMethod));
				stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
			}
		}
	}
	
	private void visitAssignStmt_Init(String thread, SootMethod curMethod, AssignStmt stmt) {
		JAssignStmt jstmt = (JAssignStmt)stmt;
		Value left = jstmt.getLeftOp();
		Value right = jstmt.getRightOp();
		
		boolean write = false;
		boolean read = false;
		if (left instanceof FieldRef) {
			// write
			SootField field = ((FieldRef)left).getField();
			if (s_to_f.containsKey(hashCodeF(field))) {				
				field = s_to_f.get(hashCodeF(field));
				if (field.isPublic() && field.isStatic()) {
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
					read = true;
				}
			}
		}
		if (write) {
			stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
		}
		else if (read) {
			stmt_to_p.put(hashCodeTStmt(thread, curMethod, stmt), newPlace());
		}
	}
	
	private void visitTarget_Init(String thread, SootMethod curMethod, Stmt target) {
		if (stmt_to_p.containsKey(hashCodeTStmt(thread, curMethod, target))) return;
		if (target_to_p.containsKey(hashCodeTStmt(thread, curMethod, target))) return;
		target_to_p.put(hashCodeTStmt(thread, curMethod, target), newPlace());
	}
	
	private Transition visitIfStmt(Place p, String thread, SootMethod curMethod, IfStmt stmt) {
		Transition t1 = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt+":true");
		Transition t2 = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt+":false");
		addEdge(p, t1);
		addEdge(p, t2);
		JIfStmt jstmt = (JIfStmt)stmt;
		Stmt target = jstmt.getTarget();
		if (stmt_to_p.containsKey(hashCodeTStmt(thread, curMethod, target))) {
			Place targetP = stmt_to_p.get(hashCodeTStmt(thread, curMethod, target));
			addEdge(t1, targetP);
		}
		else {
			if (target_to_p.containsKey(hashCodeTStmt(thread, curMethod, target))) {
				Place targetP = target_to_p.get(hashCodeTStmt(thread, curMethod, target));
				addEdge(t1, targetP);
			}
			else {
				System.err.println("[HDD-StaticAnalysis] can not get the target of <"+stmt+">!");
				System.exit(1);
			}
		}
		return t2;
	}
	
	private Transition visitGotoStmt(Place p, String thread, SootMethod curMethod, GotoStmt stmt) {
		Transition t = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt);
		addEdge(p, t);
		JGotoStmt jstmt = (JGotoStmt)stmt;
		Stmt target = (Stmt)jstmt.getTarget();
		if (stmt_to_p.containsKey(hashCodeTStmt(thread, curMethod, target))) {
			Place targetP = stmt_to_p.get(hashCodeTStmt(thread, curMethod, target));
			addEdge(t, targetP);
		}
		else {
			if (target_to_p.containsKey(hashCodeTStmt(thread, curMethod, target))) {
				Place targetP = target_to_p.get(hashCodeTStmt(thread, curMethod, target));
				addEdge(t, targetP);
			}
			else {
				System.err.println("[HDD-StaticAnalysis] can not get the target of <"+stmt+">!");
				System.exit(1);
			}
		}
		return t;
	}
	
	private Transition visitEnterMonitorStmt(Place p, String thread, SootMethod curMethod, EnterMonitorStmt stmt) {
		JEnterMonitorStmt jstmt = (JEnterMonitorStmt)stmt;
		Value v = jstmt.getOp();
		if (!isSharedVariable(v)) {
			System.err.println("[HDD-StaticAnalysis] can not load lock " + jstmt.getOp() + "!");
			System.exit(1);
		}
		SootField lock = v_to_f.get((JimpleLocal)v);
		Transition t = newTransitionSyn(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt,
				hashCodeF(lock), true);
		addEdge(p, t);
		return t;
	}
	
	private Transition visitExitMonitorStmt(Place p, String thread, SootMethod curMethod, ExitMonitorStmt stmt) {
		JExitMonitorStmt jstmt = (JExitMonitorStmt)stmt;
		Value v = jstmt.getOp();
		if (!isSharedVariable(v)) {
			System.err.println("[HDD-StaticAnalysis] can not load lock " + jstmt.getOp() + "!");
			System.exit(1);
		}
		SootField lock = v_to_f.get((JimpleLocal)v);
		Transition t = newTransitionSyn(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt,
				hashCodeF(lock), false);
		addEdge(p, t);
		return t;
	}
	
	private Transition visitReturnStmt(Place p, Place pt, String thread, SootMethod curMethod, ReturnStmt stmt) {
		Transition t = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt);
		addEdge(p, t);
		addEdge(t, pt);
		return t;
	}
	
	private Transition visitReturnVoidStmt(Place p, Place pt, String thread, SootMethod curMethod, ReturnVoidStmt stmt) {
		Transition t = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt);
		addEdge(p, t);
		addEdge(t, pt);
		return t;
	}
	
	private Transition visitInvokeStmt(int depth, Place p, String thread, SootMethod curMethod, InvokeStmt stmt) {
		JInvokeStmt jstmt = (JInvokeStmt)stmt;
		if (!jstmt.containsInvokeExpr()) {
			System.err.println("[HDD-StaticAnalysis] can not load invokeExpr in <"+stmt+">!");
			System.exit(1);
		}
		SootMethod nextMethod = jstmt.getInvokeExpr().getMethod();
		if (nextMethod.getSignature().equals("<java.lang.Thread: void start()>")) {
			//线程创建
			List<ValueBox> useBoxes = jstmt.getUseBoxes();
			if (useBoxes.size() > 0 && useBoxes.get(0) instanceof JimpleLocalBox) {
				JimpleLocal jLocal = (JimpleLocal)useBoxes.get(0).getValue(); // 获取调用者
				if (v_to_c.containsKey(jLocal)) {
					SootClass sc = v_to_c.get(jLocal);
					//确认调用者所属的类是线程类
					if (sc.getSuperclass().getName().equals("java.lang.Thread")) {
						nextMethod = sc.getMethodByName("run");
						String newThread = thread+"."+stmt_to_iid.get(stmt);
						Transition t = newTransitionStart(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt, 
								newThread, hashCodeC(sc));
						addEdge(p, t);
						constructModel(newThread, nextMethod, depth+1);
						addEdge(t, m_to_ps.get(hashCodeTM(newThread, nextMethod)));
						return t;
					}
				}
			}
		}
		else {
			//一般调用
			if (s_to_m.containsKey(hashCodeM(nextMethod))) {
				nextMethod = s_to_m.get(hashCodeM(nextMethod));
				Transition t1 = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt);
				addEdge(p, t1);
				constructModel(thread, nextMethod, depth+1);
				addEdge(t1, m_to_ps.get(hashCodeTM(thread, nextMethod)));
				Transition t2 = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt+" special end");
				addEdge(m_to_pt.get(hashCodeTM(thread, nextMethod)), t2);
				Place p1 = newPlace();
				addEdge(t1, p1);
				addEdge(p1, t2);
				return t2;
			}
		}
		return null;
	}
	
	private Transition visitAssignStmt(Place p, String thread, SootMethod curMethod, AssignStmt stmt) {
		JAssignStmt jstmt = (JAssignStmt)stmt;
		Value left = jstmt.getLeftOp();
		Value right = jstmt.getRightOp();
		
		Transition t = null;
		SootField lField = null;
		SootField rField = null;
		if (left instanceof FieldRef) {
			// write
			SootField field = ((FieldRef)left).getField();
			if (s_to_f.containsKey(hashCodeF(field))) {				
				field = s_to_f.get(hashCodeF(field));
				if (field.isPublic() && field.isStatic()) {
					lField = field;
				}
			}
		}
		if (right instanceof FieldRef) {
			//read
			SootField field = ((FieldRef)right).getField();
			if (s_to_f.containsKey(hashCodeF(field))) {				
				field = s_to_f.get(hashCodeF(field));
				if (field.isPublic() && field.isStatic()) {
					rField = field;
				}
			}
		}
		if (lField != null) {
			t = newTransitionRW(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt,
					hashCodeF(lField), true);
		}
		else if (rField != null) {
			t = newTransitionRW(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt,
					hashCodeF(rField), false);
		}
		if (t == null) {
			System.err.println("[HDD-StaticAnalysis] can not load r/w in <"+stmt+">!");
			System.exit(1);
		}
		addEdge(p, t);
		return t;
	}
	
	private Transition visitLink(Place p, String thread, SootMethod curMethod, Stmt stmt) {
		Transition t = newTransition(hashCodeTStmt(thread, curMethod, stmt), "["+thread+"]:"+stmt);
		addEdge(p, t);
		return t;
	}
	
	private void constructModel(String thread, SootMethod curMethod, int depth) {
//		System.out.println(white(depth)+"enter "+hashCodeTM(thread, curMethod));
		if (used.contains(hashCodeTM(thread, curMethod))) {
//			System.out.println(white(depth)+"visited!!!!!");
			return;
		}
		used.add(hashCodeTM(thread, curMethod));
		Place ps = newPlace();
		Place pt = newPlace();
		m_to_ps.put(hashCodeTM(thread, curMethod), ps);
		m_to_pt.put(hashCodeTM(thread, curMethod), pt);
		
		//记录敏感语句,stmt_to_p,target_to_p
		//1.顺序遍历
		JimpleBody jb = (JimpleBody)curMethod.getActiveBody();
		Iterator ui = jb.getUnits().iterator();
		while (ui.hasNext()) {
			Stmt stmt = (Stmt)ui.next();
			if ((stmt instanceof IfStmt)) {
				visitIfStmt_Init(thread, curMethod, (IfStmt)stmt);
			}
			else if (stmt instanceof GotoStmt) {
				visitGotoStmt_Init(thread, curMethod, (GotoStmt)stmt);
			}
			else if (stmt instanceof EnterMonitorStmt) {
				visitEnterMonitorStmt_Init(thread, curMethod, (EnterMonitorStmt)stmt);
			}
			else if (stmt instanceof ExitMonitorStmt) {
				visitExitMonitorStmt_Init(thread, curMethod, (ExitMonitorStmt)stmt);
			}
			else if (stmt instanceof ReturnStmt) {
				visitReturnStmt_Init(thread, curMethod, (ReturnStmt)stmt);
			}
			else if (stmt instanceof ReturnVoidStmt) {
				visitReturnVoidStmt_Init(thread, curMethod, (ReturnVoidStmt)stmt);
			}
			else if (stmt instanceof InvokeStmt) {
				visitInvokeStmt_Init(thread, curMethod, (InvokeStmt)stmt);
			}
			else if (stmt instanceof AssignStmt) {
				visitAssignStmt_Init(thread, curMethod, (AssignStmt)stmt);
			}
		}
		//2.特殊处理跳转
		ui = jb.getUnits().iterator();
		while (ui.hasNext()) {
			Stmt stmt = (Stmt)ui.next();
			if ((stmt instanceof IfStmt)) {
				JIfStmt jstmt = (JIfStmt)stmt;
				Stmt target = (Stmt)jstmt.getTarget();
				visitTarget_Init(thread, curMethod, target);
			}
			else if (stmt instanceof GotoStmt) {
				JGotoStmt jstmt = (JGotoStmt)stmt;
				Stmt target = (Stmt)jstmt.getTarget();
				visitTarget_Init(thread, curMethod, target);
			}
		}
		
		//构建1-safe Petri net
		Transition ts = newTransition(hashCodeTM(thread, curMethod),"["+thread+"]:enter "+curMethod);
		addEdge(ps, ts);
		Transition pre = ts;
		ui = jb.getUnits().iterator();
		while (ui.hasNext()) {
			Stmt stmt = (Stmt)ui.next();
			if (stmt_to_p.containsKey(hashCodeTStmt(thread, curMethod, stmt))) {
//				System.out.println(white(depth)+"["+thread+"]:"+stmt);
				Place p = stmt_to_p.get(hashCodeTStmt(thread, curMethod, stmt));
				if (pre != null) addEdge(pre, p);
				pre = null;
				Transition t = null;
				if ((stmt instanceof IfStmt)) {
					t = visitIfStmt(p, thread, curMethod, (IfStmt)stmt);
				}
				else if (stmt instanceof GotoStmt) {
					t = visitGotoStmt(p, thread, curMethod, (GotoStmt)stmt);
				}
				else if (stmt instanceof EnterMonitorStmt) {
					t = visitEnterMonitorStmt(p, thread, curMethod, (EnterMonitorStmt)stmt);
				}
				else if (stmt instanceof ExitMonitorStmt) {
					t = visitExitMonitorStmt(p, thread, curMethod, (ExitMonitorStmt)stmt);
				}
				else if (stmt instanceof ReturnStmt) {
					t = visitReturnStmt(p, pt, thread, curMethod, (ReturnStmt)stmt);
				}
				else if (stmt instanceof ReturnVoidStmt) {
					t = visitReturnVoidStmt(p, pt, thread, curMethod, (ReturnVoidStmt)stmt);
				}
				else if (stmt instanceof InvokeStmt) {
					t = visitInvokeStmt(depth, p, thread, curMethod, (InvokeStmt)stmt);
				}
				else if (stmt instanceof AssignStmt) {
					t = visitAssignStmt(p, thread, curMethod, (AssignStmt)stmt);
				}
				if (t == null) {
					System.err.println("[HDD-StaticAnalysis] can not parse the stmt <"+stmt+">!");
					System.exit(1);
				}
				if (stmt.fallsThrough()) pre = t;
			}
			else {
				if (target_to_p.containsKey(hashCodeTStmt(thread, curMethod, stmt))) {
//					System.out.println(white(depth)+"["+thread+"]:"+stmt);
					Place p = target_to_p.get(hashCodeTStmt(thread, curMethod, stmt));
					if (pre != null) addEdge(pre, p);
					pre = null;
					Transition t = visitLink(p, thread, curMethod, stmt);
					if (stmt.fallsThrough()) pre = t;
				}
			}
		}
		
	}

	private void debugModel(Set<Transition> vis, Place ps) {
		for (Transition t : ps.getPostSet()) {
			System.out.println(t.getMessage());
			if (!vis.contains(t)) {
				vis.add(t);
				for (Place p : t.getPostSet()) {
					debugModel(vis, p);
				}
			}
			else {
				System.out.println("visited!!!!!!");
			}
		}
	}
	
	private void constructLockSet() {
		Map<Event, Integer> counter = new HashMap<> ();
		for (Event e : E) counter.put(e, 0);
		counter.put(Es, 1);
		Queue<Event> queue = new LinkedList<Event> ();
		queue.offer(Es);
		while (!queue.isEmpty()) {
			Event e = queue.poll();
			Transition t = e.getMap();
			if (t instanceof TransitionSyn) {
				TransitionSyn tSyn = (TransitionSyn)t;
				if (tSyn.isAcquire()) {
					e.addLockSet(tSyn.getLock());
				}
				else {
					e.removeLockSet(tSyn.getLock());
				}
			}
			for (Condition c : e.getPostSet()) {
				for (Event e_next : c.getPostSet()) {
					for (String lock : e.getLockSet()) {
						e_next.addLockSet(lock);
					}
					counter.put(e_next, counter.get(e_next)+1);
					if (counter.get(e_next) == e_next.getPreSet().size()) {
						queue.offer(e_next);
					}
				}
			}
		}
	}
	
	private void constructDataRacePath(Event e1, Event e2) {
		Set<Event> vis = new HashSet<> ();
		vis.add(e1);
		vis.add(e2);
		Queue<Event> queue = new LinkedList<Event> ();
		queue.offer(e1);
		queue.offer(e2);
		while (!queue.isEmpty()) {
			Event e = queue.poll();
			for (Condition c : e.getPreSet()) {
				Event e_next = c.getPreSet();
				if (e_next == null) continue;
				if (!vis.contains(e_next)) {
					vis.add(e_next);
					queue.add(e_next);
				}
			}
		}
		
		List<String> path = new ArrayList<> ();
		boolean visited1 = false;
		boolean visited2 = false;
		Map<Event, Integer> counter = new HashMap<> ();
		for (Event e : E) counter.put(e, 0);
		if (!vis.contains(Es)) {
			System.err.println("[HDD-StaticAnalysis] can not construct path for this data race!");
			System.exit(1);
		}
		counter.put(Es, 1);
		//topo序+分析锁的申请
		while (!queue.isEmpty()) queue.poll();
		queue.offer(Es);
		Set<String> lock = new HashSet<> (); //记录锁的占用情况
		Queue<Event> blocked = new LinkedList<> (); //暂存阻塞事件
		while (!queue.isEmpty()) {
			Event e = queue.poll();
			Transition t = e.getMap();
			if (e == e1) visited1 = true;
			if (e == e2) visited2 = true;
			if (t instanceof TransitionStart || t instanceof TransitionSyn || t instanceof TransitionRW) {
				path.add(t.getSignature());
//				System.out.println("[path]" + t.getMessage());
			}
			if (t instanceof TransitionSyn) {
				TransitionSyn tSyn = (TransitionSyn)t;
				if (!tSyn.isAcquire()) {
					lock.remove(tSyn.getLock());
				}
			}
			List<Event> temp = new ArrayList<> ();
			while (!blocked.isEmpty()) temp.add(blocked.poll());
			for (Event e_next : temp) {
				TransitionSyn t_nextSyn = (TransitionSyn)e_next.getMap(); //must be acquire
				if (lock.contains(t_nextSyn.getLock())) {
					blocked.offer(e_next);
				}
				else {
					lock.add(t_nextSyn.getLock());
					queue.offer(e_next);
				}
			}
			for (Condition c : e.getPostSet()) {
				for (Event e_next : c.getPostSet()) {
					if (!vis.contains(e_next)) continue;
					counter.put(e_next, counter.get(e_next)+1);
					if (counter.get(e_next) == e_next.getPreSet().size()) {
						Transition t_next = e_next.getMap();
						if (t_next instanceof TransitionSyn) {
							TransitionSyn t_nextSyn = (TransitionSyn)t_next;
							if (t_nextSyn.isAcquire()) {
								if (lock.contains(t_nextSyn.getLock())) {
									blocked.offer(e_next);
								}
								else {
									lock.add(t_nextSyn.getLock());
									queue.offer(e_next);
								}
							}
							else {
								queue.offer(e_next);
							}
						}
						else {							
							queue.offer(e_next);
						}
					}
				}
			}
		}
		if (visited1 && visited2) dataRacePath.add(path);
		else {
			System.out.println("[HDD-StaticAnalysis] Warning: Low path accuracy!");
			dataRacePath.add(path);
		}
	}
	
	private void analysis() {
		List<Event> ERW =new ArrayList<Event> ();
		for (Event e : E) {
			if (e.getMap() instanceof TransitionRW) {
				ERW.add(e);
			}
		}
		int dataRaceNumber = 0;
		for (int i = 0; i < ERW.size(); i++) {
			for (int j = i + 1; j < ERW.size(); j++) {
				Event e1 = ERW.get(i);
				TransitionRW t1 = (TransitionRW)e1.getMap();
				Event e2 = ERW.get(j);
				TransitionRW t2 = (TransitionRW)e2.getMap();
				if (!t1.getVariable().equals(t2.getVariable())) continue;//同一变量
				if (!t1.isWrite() && !t2.isWrite()) continue;//至少一个是写
				if (!e1.isConcurrentWith(e2)) continue;//在展开中并发
				if (e1.hasSameLockWith(e2)) continue;//未加同一把锁
				System.out.println("========================================");
				System.out.println(t1.getSignature() + "-----" + t1.getMessage());
				System.out.println(t2.getSignature() + "-----" + t2.getMessage());
				constructDataRacePath(e1, e2);
				dataRaceNumber++;
				if (dataRaceNumber >= maxDataRaceNumber) return;
			}
		}
	}
	
	public void start() {
		System.out.println("[HDD-StaticAnalysis] Initialize");
		initialize();
		System.out.println("[HDD-StaticAnalysis] Construct 1-safe Petri net");
		constructModel("0", mainMethod, 0);
		Ps = m_to_ps.get(hashCodeTM("0", mainMethod));
		//debug model
//		System.out.println("====================debug model====================");
//		Set<Transition> vis = new HashSet<> ();
//		debugModel(vis, Ps);
		System.out.println("|P|="+P.size());
		System.out.println("|T|="+T.size());
		System.out.println("[HDD-StaticAnalysis] Unfolding");
		Unfolder unfolder = new Unfolder(Ps);
		unfolder.start(10000);
		C = unfolder.getC();
		E = unfolder.getE();
		Es = unfolder.getEs();
		System.out.println("|C|="+C.size());
		System.out.println("|E|="+E.size());
		System.out.println("cutoffNumber="+unfolder.getCutoffNumber());
		System.out.println("[HDD-StaticAnalysis] Detect potential data race");
		constructLockSet();
		analysis();
	}
	
	public List<List<String> > getDataRacePath() {
		return dataRacePath;
	}
}
