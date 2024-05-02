package se.liu.albhe576.project;

import java.util.List;
public record IfBlock(Expr condition, List<Stmt> body) { }
