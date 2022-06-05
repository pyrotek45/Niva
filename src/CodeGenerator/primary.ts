import { Primary } from "../AST_Nodes/Statements/Expressions/Primary/Primary";

// If primary is not an expression
export function getAtomPrimary(primary: Primary): string {
	const atom = primary.atomReceiver;
	switch (atom.kindPrimary) {
		case 'Identifer':
		case 'IntLiteral':
		case 'StringLiteral':
		case 'BoolLiteral':
			return atom.value;
		default:
			const _never: never = atom;
			console.log("!!! atom = ", atom);
			
			throw new Error('SoundError');
	}
}