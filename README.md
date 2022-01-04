# My Compiler
</br>

    C 语言编译器项目，目标实现类似 GCC 的广义编译器

</br>

## 项目结构
</br>

    编译器 ———— 预处理器
         | ———— 编译器
         | ———— 汇编器
         | ———— 链接器
        

    输入为简化版 / 标准 C 语言文件
    最终输出满足 ELF 标准的 Linux 可执行文件
    支持各命令行参数处理

## 项目文件
</br>

    cbc-1.0.tar.gz 源码压缩包
    /net 编译器源码，待注释

## 项目规划
</br>

    1. 阅读项目源码并完成注释,项目采用Java语言，扫描及解析器采用JavaCC 生成
    2. 阅读 gcc 源码, 参考 gcc 完善编译器剩余部分，添加预编译器及部分简化 C 语言指令
    3. 尝试采用 C/C++ 重写代码
    4. 考虑优化

## 来源
</br>

    源码来自《自制编译器（How to Develop a Compiler）》
    参考龙书《编译原理（Compilers Principles,Techniques and Tools）》


## 更新记录
</br>

**2021-1-4** &ensp; 项目仓库创建 &ensp; [git-commit](https://github.com/lovely-days/MyCompiler)


