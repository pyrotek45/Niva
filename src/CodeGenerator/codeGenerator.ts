import { StatementList } from '../AST_Nodes/AstNode';
import { Mutability, ReturnStatement } from '../AST_Nodes/Statements/Statement';
import { generateAssigment } from './assigment';
import { generateMethodDeclaration } from './methodDeclaration';
import { processExpression } from './expression/expression';
import { generateSwitchExpression } from './expression/switchExpression';
import { generateTypeDeclaration } from './typeDeclaration';


// То что может быть на первом уровне вложенности
export function generateNimFromAst(x: StatementList, identation = 0, discardable = false): string {
	let lines: string[] = [];

	if (discardable) {
		lines.push('{. push discardable .}');
	}

	for (const s of x.statements) {
		switch (s.kindStatement) {
			case 'MessageCallExpression':
			case "BracketExpression":
				
				const expressionCode = processExpression(s, identation)
				lines.push(expressionCode)
			break;

			case 'Assignment':
				// codeGenerateExpression(s.value, lines)
				const assignment = s;
				if (assignment.mutability === Mutability.IMUTABLE) {
					
					lines.push(generateAssigment(assignment.assignmentTarget, assignment.to, identation, s.type));
				}
				break;

			case 'ReturnStatement':
			  const retrunCode: string =	generateReturn(s, identation)
				lines.push(retrunCode)
				break;
			case 'TypeDeclaration':
				const typeDeclarationAst = s
				const typeDeclarationCode: string = generateTypeDeclaration(typeDeclarationAst, identation)
				lines.push(typeDeclarationCode)

				break;
			case 'MethodDeclaration':
				const methodDeclarationAst = s
				const methodDeclarationCode = generateMethodDeclaration(methodDeclarationAst, identation);
				lines.push(methodDeclarationCode)

			break;

			case 'SwitchExpression':
				const switchCode = generateSwitchExpression(s, identation);
				lines.push(switchCode)
				break;
			default:
				const _never: never = s;
				console.log("!!! s = ", s);
				
				throw new Error('SoundError');
		}
	}

	return lines.join('\n');
}
function generateReturn(s: ReturnStatement, identation: number): string {
	const ident = " ".repeat(identation) 
	if (s.value.kindStatement === "SwitchExpression"){
		const switchCode = generateSwitchExpression(s.value, 0)
		return `${ident}return ${switchCode}` 
	}
	const exprCode = processExpression(s.value, 0)
	return `${ident}return ${exprCode}` 
}
