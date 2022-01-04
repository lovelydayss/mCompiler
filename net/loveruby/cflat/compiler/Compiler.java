package net.loveruby.cflat.compiler;

import net.loveruby.cflat.parser.Parser;
import net.loveruby.cflat.ast.AST;
import net.loveruby.cflat.ast.StmtNode;
import net.loveruby.cflat.ast.ExprNode;
import net.loveruby.cflat.type.TypeTable;
import net.loveruby.cflat.ir.IR;
//import net.loveruby.cflat.sysdep.CodeGenerator;
import net.loveruby.cflat.sysdep.AssemblyCode;
import net.loveruby.cflat.utils.ErrorHandler;
import net.loveruby.cflat.exception.*;
import java.util.*;
import java.io.*;

// 编译类 (Compiler)
public class Compiler {

    // #@@range/main{
    // 编译命令行中 版本名及版本号定义
    static final public String ProgramName = "cbc";
    static final public String Version = "1.0.0";

    // main(),构建 Compiler 对象，以命令行参数 args 为输入执行 commandMain()
    static public void main(String[] args) {
        new Compiler(ProgramName).commandMain(args);
    }

    // 错误类对象，统一封装于 net.loveruby.cflat.utils
    private final ErrorHandler errorHandler;

    // main()中 Compiler 对象差错处理
    public Compiler(String programName) {
        this.errorHandler = new ErrorHandler(programName);
    }
    // #@@}

    // 调用 parseOptions() 解析命令行参数，构建对应数组，执行
    // 调用 build() , 执行编译
    public void commandMain(String[] args) {

        // 调用 parseOptions() 解析命令行参数，构建对应数组
        Options opts = parseOptions(args);

        // "--check-syntax" 情况, 检验命令行参数正确性
        if (opts.mode() == CompilerMode.CheckSyntax) {
            System.exit(checkSyntax(opts) ? 0 : 1);
        }

        try {
            // 数组存储所有命令行参数
            List<SourceFile> srcs = opts.sourceFiles();

            // 调用 build() , 执行编译过程
            build(srcs, opts);

            // 编译系统正常结束
            System.exit(0);

        } catch (CompileException ex) {
            errorHandler.error(ex.getMessage());
            System.exit(1);
        }
    }

    // 调用 Options 类中 parse() 执行命令行解析
    // 异常输出错误信息及尝试 -help 信息
    private Options parseOptions(String[] args) {
        try {
            // 解析执行
            return Options.parse(args);

        } catch (OptionParseError err) {
            errorHandler.error(err.getMessage());
            errorHandler.error("Try \"cbc --help\" for usage");
            System.exit(1);
            return null; // never reach
        }
    }

    // ？ 检查命令行参数准确性
    private boolean checkSyntax(Options opts) {

        // 不合法标志预定义
        boolean failed = false;

        // 对各枚举逐个判别处理
        for (SourceFile src : opts.sourceFiles()) {
            if (isValidSyntax(src.path(), opts)) {
                System.out.println(src.path() + ": Syntax OK");
            } else {
                System.out.println(src.path() + ": Syntax Error");
                failed = true; // 一个参数错误即终止编译
            }
        }
        return !failed;
    }

    private boolean isValidSyntax(String path, Options opts) {
        try {
            parseFile(path, opts);
            return true;
        } catch (SyntaxException ex) {
            return false;
        } catch (FileException ex) {
            errorHandler.error(ex.getMessage());
            return false;
        }
    }

    // build() 具体组织编译操作
    // #@@range/build{
    public void build(List<SourceFile> srcs, Options opts)
            throws CompileException {
        for (SourceFile src : srcs) {

            // 调用 compile() 执行狭义编译操作
            if (src.isCflatSource()) {
                String destPath = opts.asmFileNameOf(src);
                compile(src.path(), destPath, opts);
                src.setCurrentName(destPath);
            }

            // 无汇编要求，结束
            if (!opts.isAssembleRequired())
                continue;

            // 调用 assemble() 执行汇编操作
            if (src.isAssemblySource()) {
                String destPath = opts.objFileNameOf(src);
                assemble(src.path(), destPath, opts);
                src.setCurrentName(destPath);
            }
        }

        // 无链接要求
        if (!opts.isLinkRequired())
            return;

        // 调用 link() 执行链接操作
        link(opts);
    }
    // #@@}

    // 狭义编译
    public void compile(String srcPath, String destPath,
            Options opts) throws CompileException {
        AST ast = parseFile(srcPath, opts);
        if (dumpAST(ast, opts.mode()))
            return;
        TypeTable types = opts.typeTable();
        AST sem = semanticAnalyze(ast, types, opts);
        if (dumpSemant(sem, opts.mode()))
            return;
        IR ir = new IRGenerator(types, errorHandler).generate(sem);
        if (dumpIR(ir, opts.mode()))
            return;
        AssemblyCode asm = generateAssembly(ir, opts);
        if (dumpAsm(asm, opts.mode()))
            return;
        if (printAsm(asm, opts.mode()))
            return;
        writeFile(destPath, asm.toSource());
    }

    public AST parseFile(String path, Options opts)
            throws SyntaxException, FileException {
        return Parser.parseFile(new File(path),
                opts.loader(), errorHandler, opts.doesDebugParser());
    }

    public AST semanticAnalyze(AST ast, TypeTable types,
            Options opts) throws SemanticException {
        new LocalResolver(errorHandler).resolve(ast);
        new TypeResolver(types, errorHandler).resolve(ast);
        types.semanticCheck(errorHandler);
        if (opts.mode() == CompilerMode.DumpReference) {
            ast.dump();
            return ast;
        }
        new DereferenceChecker(types, errorHandler).check(ast);
        new TypeChecker(types, errorHandler).check(ast);
        return ast;
    }

    public AssemblyCode generateAssembly(IR ir, Options opts) {
        return opts.codeGenerator(errorHandler).generate(ir);
    }

    // 汇编
    // #@@range/assemble{
    public void assemble(String srcPath, String destPath,
            Options opts) throws IPCException {
        opts.assembler(errorHandler)
                .assemble(srcPath, destPath, opts.asOptions());
    }
    // #@@}

    // 链接
    // #@@range/link{
    public void link(Options opts) throws IPCException {
        if (!opts.isGeneratingSharedLibrary()) {
            generateExecutable(opts);
        } else {
            generateSharedLibrary(opts);
        }
    }
    // #@@}

    // #@@range/generateExecutable{
    public void generateExecutable(Options opts) throws IPCException {
        opts.linker(errorHandler).generateExecutable(
                opts.ldArgs(), opts.exeFileName(), opts.ldOptions());
    }
    // #@@}

    // #@@range/generateSharedLibrary{
    public void generateSharedLibrary(Options opts) throws IPCException {
        opts.linker(errorHandler).generateSharedLibrary(
                opts.ldArgs(), opts.soFileName(), opts.ldOptions());
    }
    // #@@}

    // 写回
    private void writeFile(String path, String str) throws FileException {
        if (path.equals("-")) {
            System.out.print(str);
            return;
        }
        try {
            BufferedWriter f = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(path)));
            try {
                f.write(str);
            } finally {
                f.close();
            }
        } catch (FileNotFoundException ex) {
            errorHandler.error("file not found: " + path);
            throw new FileException("file error");
        } catch (IOException ex) {
            errorHandler.error("IO error" + ex.getMessage());
            throw new FileException("file error");
        }
    }

    private boolean dumpAST(AST ast, CompilerMode mode) {
        switch (mode) {
            case DumpTokens:
                ast.dumpTokens(System.out);
                return true;
            case DumpAST:
                ast.dump();
                return true;
            case DumpStmt:
                findStmt(ast).dump();
                return true;
            case DumpExpr:
                findExpr(ast).dump();
                return true;
            default:
                return false;
        }
    }

    private StmtNode findStmt(AST ast) {
        StmtNode stmt = ast.getSingleMainStmt();
        if (stmt == null) {
            errorExit("source file does not contains main()");
        }
        return stmt;
    }

    private ExprNode findExpr(AST ast) {
        ExprNode expr = ast.getSingleMainExpr();
        if (expr == null) {
            errorExit("source file does not contains single expression");
        }
        return expr;
    }

    private boolean dumpSemant(AST ast, CompilerMode mode) {
        switch (mode) {
            case DumpReference:
                return true;
            case DumpSemantic:
                ast.dump();
                return true;
            default:
                return false;
        }
    }

    private boolean dumpIR(IR ir, CompilerMode mode) {
        if (mode == CompilerMode.DumpIR) {
            ir.dump();
            return true;
        } else {
            return false;
        }
    }

    private boolean dumpAsm(AssemblyCode asm, CompilerMode mode) {
        if (mode == CompilerMode.DumpAsm) {
            asm.dump(System.out);
            return true;
        } else {
            return false;
        }
    }

    private boolean printAsm(AssemblyCode asm, CompilerMode mode) {
        if (mode == CompilerMode.PrintAsm) {
            System.out.print(asm.toSource());
            return true;
        } else {
            return false;
        }
    }

    private void errorExit(String msg) {
        errorHandler.error(msg);
        System.exit(1);
    }
}
