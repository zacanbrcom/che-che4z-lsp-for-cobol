/*
 * Copyright (c) 2020 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */

package com.ca.lsp.core.cobol.visitor;

import com.broadcom.lsp.domain.common.model.Position;
import com.ca.lsp.core.cobol.model.DocumentHierarchyLevel;
import com.ca.lsp.core.cobol.model.ExtendedDocument;
import com.ca.lsp.core.cobol.model.SyntaxError;
import com.ca.lsp.core.cobol.model.Variable;
import com.ca.lsp.core.cobol.parser.CobolParserBaseVisitor;
import com.ca.lsp.core.cobol.semantics.SemanticContext;
import com.ca.lsp.core.cobol.semantics.SubContext;
import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

import static com.ca.lsp.core.cobol.parser.CobolParser.*;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * This extension of {@link CobolParserBaseVisitor} applies the semantic analysis based on the
 * abstract syntax tree built by {@link com.ca.lsp.core.cobol.parser.CobolParser}. It requires a
 * semantic context with defined elements to add the usages or throw a warning on an invalid
 * definition. If there is a misspelled keyword, the visitor finds it and throws a warning.
 */
public class CobolVisitor extends CobolParserBaseVisitor<Class> {
  private static final int WARNING_LEVEL = 2;
  private static final int INFO_LEVEL = 3;

  @Getter private SemanticContext semanticContext = new SemanticContext();
  @Getter private List<SyntaxError> errors = new ArrayList<>();

  private Deque<DocumentHierarchyLevel> documentHierarchyStack = new ArrayDeque<>();

  private String documentUri;
  private ExtendedDocument extendedDocument;

  public CobolVisitor(String documentUri, ExtendedDocument extendedDocument) {
    this.documentUri = documentUri;
    this.extendedDocument = extendedDocument;
    semanticContext.getCopybooks().merge(extendedDocument.getUsedCopybooks());
    documentHierarchyStack.push(
        new DocumentHierarchyLevel(
            documentUri, new ArrayList<>(extendedDocument.getTokenMapping().get(documentUri))));
  }

  @Override
  public Class visitDataDescriptionEntryCpy(DataDescriptionEntryCpyContext ctx) {
    String cpyName =
        ofNullable(ctx.IDENTIFIER())
            .map(ParseTree::getText)
            .orElse(ctx.getChildCount() > 1 ? ctx.getChild(1).getText() : "");
    documentHierarchyStack.push(
        new DocumentHierarchyLevel(
            cpyName,
            new ArrayList<>(
                ofNullable(extendedDocument.getTokenMapping().get(cpyName)).orElse(emptyList()))));
    return visitChildren(ctx);
  }

  @Override
  public Class visitEnterCpy(EnterCpyContext ctx) {
    String cpyName =
        ofNullable(ctx.IDENTIFIER())
            .map(ParseTree::getText)
            .orElse(ctx.getChildCount() > 1 ? ctx.getChild(1).getText() : "");
    documentHierarchyStack.push(
        new DocumentHierarchyLevel(
            cpyName,
            new ArrayList<>(
                ofNullable(extendedDocument.getTokenMapping().get(cpyName)).orElse(emptyList()))));
    return visitChildren(ctx);
  }

  @Override
  public Class visitDataDescriptionExitCpy(DataDescriptionExitCpyContext ctx) {
    moveToPreviousLevel();
    return visitChildren(ctx);
  }

  @Override
  public Class visitExitCpy(ExitCpyContext ctx) {
    moveToPreviousLevel();
    return visitChildren(ctx);
  }

  private void moveToPreviousLevel() {
    documentHierarchyStack.pop();
  }

  private Position retrievePosition(ParserRuleContext ctx) {
    DocumentHierarchyLevel currentDocument = documentHierarchyStack.peek();
    if (currentDocument == null) {
      return null;
    }
    Token start = ctx.getStart();
    List<Position> positions = currentDocument.getPositions();
    Position position =
        positions.stream()
            .filter(it -> it.getToken().equals(start.getText()))
            .findFirst()
            .orElse(null);

    int index = positions.indexOf(position);
    if (index == -1) {

      List<Position> initialPositions =
          extendedDocument.getTokenMapping().get(currentDocument.getName());
      int lenOfTail = initialPositions.size() - positions.size();
      List<Position> subList = initialPositions.subList(0, lenOfTail);
      Collections.reverse(subList);

      position =
          subList.stream()
              .filter(it -> it.getToken().equals(start.getText()))
              .findFirst()
              .orElse(null);

    } else {

      currentDocument.setPositions(positions.subList(index + 1, positions.size()));
    }
    return position;
  }

  @Override
  public Class visitProcedureSection(ProcedureSectionContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitStatement(StatementContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitIfThen(IfThenContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitIfElse(IfElseContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitPerformInlineStatement(PerformInlineStatementContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitSentence(SentenceContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitIdentifier(IdentifierContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitEvaluateWhenOther(EvaluateWhenOtherContext ctx) {
    String wrongToken = ctx.getStart().getText();
    throwWarning(wrongToken, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    return visitChildren(ctx);
  }

  @Override
  public Class visitParagraphName(ParagraphNameContext ctx) {
    semanticContext.getParagraphs().define(ctx.getText().toUpperCase(), retrievePosition(ctx));
    return visitChildren(ctx);
  }

  @Override
  public Class visitDataDescriptionEntryFormat1(DataDescriptionEntryFormat1Context ctx) {

    String levelNumber = ctx.otherLevel().getText();
    ofNullable(ctx.dataName1())
        .ifPresent(
            variable ->
                defineVariable(levelNumber, variable.getText(), retrievePosition(variable)));
    return visitChildren(ctx);
  }

  @Override
  public Class visitDataDescriptionEntryFormat2(DataDescriptionEntryFormat2Context ctx) {

    String levelNumber = ctx.LEVEL_NUMBER_66().getText();
    ofNullable(ctx.dataName1())
        .ifPresent(
            variable ->
                defineVariable(levelNumber, variable.getText(), retrievePosition(variable)));
    return visitChildren(ctx);
  }

  @Override
  public Class visitDataDescriptionEntryFormat3(DataDescriptionEntryFormat3Context ctx) {
    String levelNumber = ctx.LEVEL_NUMBER_88().getText();
    ofNullable(ctx.dataName1())
        .ifPresent(
            variable ->
                defineVariable(levelNumber, variable.getText(), retrievePosition(variable)));
    return visitChildren(ctx);
  }

  private void defineVariable(String level, String name, Position position) {
    semanticContext.getVariables().define(new Variable(level, name), position);
    semanticContext.getVariables().createRelationBetweenVariables();
  }

  @Override
  public Class visitParagraphNameUsage(ParagraphNameUsageContext ctx) {
    Position position = retrievePosition(ctx);
    String name = ctx.getText().toUpperCase();
    addUsage(semanticContext.getParagraphs(), name, position);
    return visitChildren(ctx);
  }

  @Override
  public Class visitQualifiedDataNameFormat1(QualifiedDataNameFormat1Context ctx) {
    ofNullable(ctx.dataName())
        .map(it -> it.getText().toUpperCase())
        .ifPresent(variable -> checkForVariable(variable, ctx));
    return visitChildren(ctx);
  }

  private void checkForVariable(String variable, QualifiedDataNameFormat1Context ctx) {
    Position variablePosition = retrievePosition(ctx);
    if (!semanticContext.getVariables().contains(variable)) {
      reportVariableNotDefined(variable, variablePosition);
    }
    addUsage(semanticContext.getVariables(), variable, variablePosition);

    if (ctx.qualifiedInData() != null) {
      iterateOverQualifiedDataNames(ctx, variable, variablePosition);
    }
  }

  private void iterateOverQualifiedDataNames(
      QualifiedDataNameFormat1Context ctx, String variable, Position variablePosition) {
    for (QualifiedInDataContext node : ctx.qualifiedInData()) {
      DataName2Context context = getDataName2Context(node);

      String parent = context.getText().toUpperCase();
      checkVariableStructure(parent, variable, variablePosition);
      Position parentPosition = retrievePosition(context);
      variable = parent;
      addUsage(semanticContext.getVariables(), variable, parentPosition);
    }
  }

  private DataName2Context getDataName2Context(QualifiedInDataContext node) {
    return node.inData() == null
        ? node.inTable().tableCall().dataName2()
        : node.inData().dataName2();
  }

  private void checkVariableStructure(String parent, String child, Position position) {
    if (!semanticContext.getVariables().parentContainsSpecificChild(parent, child)) {
      reportVariableNotDefined(child, position);
    }
  }

  private void addUsage(SubContext<?> langContext, String name, Position position) {
    langContext.addUsage(name.toUpperCase(), position);
  }

  private void reportVariableNotDefined(String variable, Position position) {
    errors.add(
        SyntaxError.syntaxError()
            .position(position)
            .suggestion("Invalid definition for: " + variable)
            .severity(INFO_LEVEL)
            .build());
  }

  private void throwWarning(String wrongToken, int startLine, int charPositionInLine) {
    MisspelledKeywordDistance.calculateDistance(wrongToken.toUpperCase())
        .ifPresent(
            correctWord ->
                getSemanticError(wrongToken, startLine, charPositionInLine, correctWord));
  }

  private void getSemanticError(
      String wrongToken, int startLine, int charPositionInLine, String correctWord) {
    errors.add(
        SyntaxError.syntaxError()
            .position(
                new Position(
                    documentUri,
                    charPositionInLine,
                    getWrongTokenStopPosition(wrongToken, charPositionInLine),
                    startLine,
                    charPositionInLine))
            .suggestion("A misspelled word, maybe you want to put " + correctWord)
            .severity(WARNING_LEVEL)
            .build());
  }

  private static int getWrongTokenStopPosition(String wrongToken, int charPositionInLine) {
    return charPositionInLine + wrongToken.length() - 1;
  }
}
