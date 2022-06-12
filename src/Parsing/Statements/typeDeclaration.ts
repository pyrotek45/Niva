import { TerminalNode, NonterminalNode, IterationNode } from "ohm-js";
import { TypeDeclaration, TypedProperty } from "../../AST_Nodes/Statements/TypeDeclaration/TypeDeclaration";
import { ErrorInfo } from "../../Errors/Error";
import { state, TypeField, typesBD } from "../../niva";

export function typeDeclaration(_type: TerminalNode, _s: NonterminalNode, untypedIdentifier: NonterminalNode, _s2: NonterminalNode, typedProperties: NonterminalNode): TypeDeclaration {
  const typedPropertiesAst: TypedProperty[] = typedProperties.toAst();
  const typeName = untypedIdentifier.sourceString
  const result: TypeDeclaration = {
    kindStatement: 'TypeDeclaration',
    typeName,
    typedProperties: typedPropertiesAst,
    ref: false // TODO
  };


  // Add to BD
  if (typesBD.alreadyHasType(typeName)){
    state.errors.push({
      errorKind: "TypeAlreadyDefined",
      lineAndColMessage: _type.source.getLineAndColumnMessage(),
      typeName
    })

  } else {
    const fields = new Map<string, TypeField>()
    typedPropertiesAst.forEach(x => {
      fields.set(x.identifier, {type: x.type ?? "auto"})
    })
  
    typesBD.addNewType(typeName, fields)
  }

  return result;
}

export function typedProperties(typedProperty: NonterminalNode, _spaces: IterationNode, other_typedProperty: IterationNode): TypedProperty[] {
  const result: TypedProperty[] = [typedProperty.toAst()];
  other_typedProperty.children.forEach((x) => result.push(x.toAst()));
  return result;
}

export function typedProperty(untypedIdentifier: NonterminalNode, _colon: TerminalNode, _s: NonterminalNode, untypedIdentifierOrNestedType: NonterminalNode): TypedProperty {
  const typeNameSourceString = untypedIdentifierOrNestedType.sourceString;
  if (typeNameSourceString.startsWith('type')) {
    throw new Error('Nested types not supported yet');
  }

  const result: TypedProperty = {
    identifier: untypedIdentifier.sourceString,
    type: typeNameSourceString
  };

  return result;
}