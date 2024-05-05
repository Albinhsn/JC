package se.liu.albhe576.project.frontend;

import java.util.List;

/**
 * Token record for an if block, primarily used to help define if and "else if" blocks, just contains the condition and body of a if block
 * @see IfStatement
 * @param condition
 * @param body
 */
public record IfBlock(Expression condition, List<Statement> body) { }
