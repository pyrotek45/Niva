import { NonterminalNode, IterationNode } from "ohm-js";
import { StatementList } from "../../AST_Nodes/AstNode";
import { SwitchStatement, SwitchExpression } from "../../AST_Nodes/Statements/Expressions/Expressions";
import { Receiver } from "../../AST_Nodes/Statements/Expressions/Receiver/Receiver";

export function statements(
  _s1: NonterminalNode,
  statement: NonterminalNode,
  _statementSeparator: IterationNode,
  otherStatements: IterationNode,
  _statementSeparator2: IterationNode,
): StatementList {
  // echo('statement');
  const firstStatementAst = statement.toAst();
  // echo({ firstStatementAst });

  const ast: StatementList = {
    kind: 'StatementList',
    statements: [firstStatementAst]
  };

  for (const otherStatemenrs of otherStatements.children) {
    const otherStatementAst = otherStatemenrs.toAst();
    ast.statements.push(otherStatementAst);
  }

  return ast;
}

export function statement(s: NonterminalNode) {
  return s.toAst();
}


export function switchStatement(receiverNode: NonterminalNode, switchExpressionNode: NonterminalNode): SwitchStatement {
  const switchExpression: SwitchExpression = switchExpressionNode.toAst()
  const receiver: Receiver = receiverNode.toAst()
  const result: SwitchStatement = {
    kindStatement: "SwitchStatement",
    receiver,
    switchExpression
  }

  return result
}

