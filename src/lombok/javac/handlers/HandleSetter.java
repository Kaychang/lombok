package lombok.javac.handlers;

import static lombok.javac.handlers.PKG.*;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.javac.Javac;
import lombok.javac.JavacAST;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacAST.Node;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

@ProviderFor(JavacAnnotationHandler.class)
public class HandleSetter implements JavacAnnotationHandler<Setter> {
	public void generateSetterForField(Node fieldNode, DiagnosticPosition pos) {
		AccessLevel level = Setter.DEFAULT_ACCESS_LEVEL;
		Node errorNode = fieldNode;
		
		for ( Node child : fieldNode.down() ) {
			if ( child.getKind() == Kind.ANNOTATION ) {
				if ( Javac.annotationTypeMatches(Setter.class, child) ) {
					level = Javac.createAnnotation(Setter.class, child).getInstance().value();
					errorNode = child;
					pos = child.get();
					break;
				}
			}
		}
		
		createSetterForField(level, fieldNode, errorNode, pos);
	}
	
	@Override public boolean handle(AnnotationValues<Setter> annotation, JCAnnotation ast, Node annotationNode) {
		Node fieldNode = annotationNode.up();
		AccessLevel level = annotation.getInstance().value();
		return createSetterForField(level, fieldNode, annotationNode, annotationNode.get());
	}
	
	private boolean createSetterForField(AccessLevel level, Node fieldNode, Node errorNode, DiagnosticPosition pos) {
		if ( fieldNode.getKind() != Kind.FIELD ) {
			fieldNode.addError("@Setter is only supported on a field.");
			return false;
		}
		
		JCVariableDecl fieldDecl = (JCVariableDecl)fieldNode.get();
		String methodName = toSetterName(fieldDecl);
		
		if ( methodExists(methodName, fieldNode) ) {
			errorNode.addWarning(
					String.format("Not generating %s(%s %s): A method with that name already exists",
							methodName, fieldDecl.vartype, fieldDecl.name));
			return false;
		}
		
		JCClassDecl javacClassTree = (JCClassDecl) fieldNode.up().get();
		
		long access = toJavacModifier(level) | (fieldDecl.mods.flags & Flags.STATIC);
		
		JCMethodDecl setterMethod = createSetter(access, fieldNode, fieldNode.getTreeMaker());
		javacClassTree.defs = javacClassTree.defs.append(setterMethod);
		return true;
	}
	
	private JCMethodDecl createSetter(long access, JavacAST.Node field, TreeMaker treeMaker) {
		JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
		
		JCFieldAccess thisX = treeMaker.Select(treeMaker.Ident(field.toName("this")), fieldDecl.name);
		JCAssign assign = treeMaker.Assign(thisX, treeMaker.Ident(fieldDecl.name));
		
		JCBlock methodBody = treeMaker.Block(0, List.<JCStatement>of(treeMaker.Exec(assign)));
		Name methodName = field.toName(toSetterName(fieldDecl));
		JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(0, List.<JCAnnotation>nil()), fieldDecl.name, fieldDecl.vartype, null);
		JCExpression methodType = treeMaker.Type(field.getSymbolTable().voidType);
		
		List<JCTypeParameter> methodGenericParams = List.nil();
		List<JCVariableDecl> parameters = List.of(param);
		List<JCExpression> throwsClauses = List.nil();
		JCExpression annotationMethodDefaultValue = null;
		
		return treeMaker.MethodDef(treeMaker.Modifiers(access, List.<JCAnnotation>nil()), methodName, methodType,
				methodGenericParams, parameters, throwsClauses, methodBody, annotationMethodDefaultValue);
	}
}
