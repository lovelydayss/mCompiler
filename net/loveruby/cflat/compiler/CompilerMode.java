package net.loveruby.cflat.compiler;

import java.util.Map;
import java.util.HashMap;

// 枚举类型，命令行参数
enum CompilerMode {
    CheckSyntax("--check-syntax"), // 检查命令行语法正确性 ()
    DumpTokens("--dump-tokens"), // 查询符号表
    DumpAST("--dump-ast"),
    DumpStmt("--dump-stmt"),
    DumpExpr("--dump-expr"),
    DumpSemantic("--dump-semantic"),
    DumpReference("--dump-reference"),
    DumpIR("--dump-ir"),
    DumpAsm("--dump-asm"),
    PrintAsm("--print-asm"),
    Compile("-S"),
    Assemble("-c"),
    Link("--link");

    // 命令行字符串-枚举类型字符字典 modes 声明及构造
    static private Map<String, CompilerMode> modes;
    static {
        modes = new HashMap<String, CompilerMode>();
        modes.put("--check-syntax", CheckSyntax); // put 添加字典元素
        modes.put("--dump-tokens", DumpTokens);
        modes.put("--dump-ast", DumpAST);
        modes.put("--dump-stmt", DumpStmt);
        modes.put("--dump-expr", DumpExpr);
        modes.put("--dump-semantic", DumpSemantic);
        modes.put("--dump-reference", DumpReference);
        modes.put("--dump-ir", DumpIR);
        modes.put("--dump-asm", DumpAsm);
        modes.put("--print-asm", PrintAsm);
        modes.put("-S", Compile);
        modes.put("-c", Assemble);
    }

    // isModeOption() 判断命令行字符串是否合法
    static public boolean isModeOption(String opt) {
        return modes.containsKey(opt);
    }

    // fromOption() 根据命令行参数生成对应枚举字符
    static public CompilerMode fromOption(String opt) {
        CompilerMode m = modes.get(opt); // get 判字典查询是否成功
        if (m == null) {
            throw new Error("must not happen: unknown mode option: " + opt);
        }
        return m;
    }

    // ？
    private final String option;

    CompilerMode(String option) {
        this.option = option;
    }

    public String toOption() {
        return option;
    }

    boolean requires(CompilerMode m) {
        return ordinal() >= m.ordinal();
    }
}
