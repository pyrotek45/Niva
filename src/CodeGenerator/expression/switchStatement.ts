import { SwitchStatement } from '../../AST_Nodes/Statements/Expressions/Expressions';
import { generateBranchExpressions2, generateElseBranchExpression, generateSwitchExpression } from './switchExpression';

export function generateSwitchStatement(s: SwitchStatement, identation: number) {
	const { receiver, switchExpression } = s;
  const {elseBranch} = switchExpression
	if (!elseBranch) {
		throw new Error('Switch statement must have else branch');
	}

	if (receiver.kindStatement === 'BlockConstructor' || receiver.kindStatement === 'BracketExpression') {
		throw new Error('BlockConstructor or BracketExpression not supported as switch statement case expression yet');
	}

	if (
		receiver.atomReceiver.kindPrimary === 'BoolLiteral' ||
		receiver.atomReceiver.kindPrimary === 'IntLiteral' ||
		receiver.atomReceiver.kindPrimary === 'StringLiteral'
	) {
		throw new Error('Receiver of switch statement must be identifier, not literal');
	}

  if (receiver.atomReceiver.kindPrimary !== "Identifer") {
		throw new Error('collection not yet implemented as Receiver of switch');
    
  }

	const ident = ' '.repeat(identation);

	const identifier = receiver.atomReceiver.value;

	const branchesCode: string[] = [];

  for (const branch of switchExpression.branches) {
    branchesCode.push(generateBranchExpressions2('of', branch, identation)) 
  }
  branchesCode.push(generateElseBranchExpression(elseBranch, ident, identation)) 


	return `case ${identifier}:\n${branchesCode.join("\n")}`;
}


/* 
x = 5.

x 
| 5 => "x = 5" echo
| 7 => "x = 7" echo
|=> "not 5, not 7" echo

/////////////////

var x = 5

case x:
of 5:
  echo "5"
of 7: 
  echo "6"
else:
  echo "not 5 not 6"

*/