import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Syntax-only JDK Tree parsing: no analyze/generate and no application dependency compilation. */
public class V02AssertionAst {
    static String encode(String text){return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
    public static void main(String[] files) throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null)throw new IllegalStateException("JDK compiler required for actual AST parsing");
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try(var manager=compiler.getStandardFileManager(diagnostics,null,StandardCharsets.UTF_8)) {
            var task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none"),null,
                    manager.getJavaFileObjectsFromStrings(Arrays.asList(files)));
            for(var unit:task.parse()) {
                String path=Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize().toString();
                System.out.println("FILE\t"+encode(path));
                new TreePathScanner<Void,Void>() {
                    String method="<class>";
                    public Void visitMethod(MethodTree node,Void unused){String previous=method;method=node.getName().toString();super.visitMethod(node,unused);method=previous;return null;}
                    void emit(String kind,Tree node){System.out.println("AST\t"+encode(path)+"\t"+encode(method)+"\t"+kind+"\t"+encode(node.toString()));}
                    public Void visitMethodInvocation(MethodInvocationTree node,Void unused) {
                        String select=node.getMethodSelect().toString();String name=select.substring(select.lastIndexOf('.')+1);
                        if(name.startsWith("assert")||name.startsWith("verify")||name.equals("fail")||name.equals("andExpect"))emit("assertion-call",node);
                        return super.visitMethodInvocation(node,unused);
                    }
                    public Void visitAssert(AssertTree node,Void unused){emit("java-assert",node);return super.visitAssert(node,unused);}
                }.scan(unit,null);
            }
        }
        for(var diagnostic:diagnostics.getDiagnostics())if(diagnostic.getKind()==Diagnostic.Kind.ERROR)
            throw new IllegalStateException("Java syntax error: "+diagnostic);
    }
}
